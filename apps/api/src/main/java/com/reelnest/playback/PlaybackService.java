package com.reelnest.playback;

import com.reelnest.users.CurrentUserService;
import com.reelnest.billing.BillingService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlaybackService {
    private static final int MAX_SESSIONS = 2;
    private static final int SESSION_MINUTES = 10;
    private final JdbcTemplate jdbc;
    private final CurrentUserService currentUser;
    private final String country;
    private final BillingService billing;
    private final CdnCookieSigner cdn;
    private final boolean devProxy;
    private final boolean cdnGeoReviewed;

    public PlaybackService(JdbcTemplate jdbc, CurrentUserService currentUser, BillingService billing,
            CdnCookieSigner cdn,
            @Value("${app.playback.dev-proxy-enabled:false}") boolean devProxy,
            @Value("${app.playback.cdn-geo-reviewed:false}") boolean cdnGeoReviewed,
            @Value("${app.playback.trusted-country:IN}") String country) {
        this.jdbc = jdbc; this.currentUser = currentUser; this.billing=billing;
        this.cdn=cdn; this.devProxy=devProxy; this.cdnGeoReviewed=cdnGeoReviewed;
        if (!country.matches("[A-Z]{2}")) throw new IllegalArgumentException("Invalid trusted playback country");
        this.country = country;
    }

    public record Start(@NotNull UUID profileId, @NotNull UUID titleId) {}
    public record Session(UUID sessionId, UUID titleId, String manifestUrl,
                          int resumePositionSeconds, int durationSeconds,
                          Instant expiresAt, int heartbeatIntervalSeconds, PlaybackCues cues) {}
    public record Heartbeat(Instant expiresAt) {}
    public record Progress(@Min(0) @Max(86400) int positionSeconds,
                           @Min(1) long sequence) {}
    public record ProgressResult(int positionSeconds, boolean completed) {}
    public record ContinueItem(UUID titleId, String name, String genre, String artTone,
                               String ageRating, UUID mediaAssetId, int positionSeconds,
                               int durationSeconds, Instant updatedAt) {}
    public record MediaAccess(String manifestKey, String objectKey, String contentType) {}
    public record Availability(boolean playable, String code, String message) {}

    private record Profile(UUID userId, boolean child) {}
    private record Asset(UUID id, UUID titleId, String manifestKey, int duration, String age,
                         String tier) {}
    private record StoredSession(UUID userId, UUID profileId, UUID titleId, UUID assetId,
                                 String status, Instant expiresAt, Instant heartbeat) {}

    private UUID userId(Jwt jwt) { return currentUser.resolve(jwt).getId(); }

    private Profile requireProfile(UUID user, UUID profileId) {
        return jdbc.query("SELECT user_id,is_child FROM profiles WHERE id=? AND user_id=?",
            (rs,i) -> new Profile(rs.getObject(1,UUID.class),rs.getBoolean(2)), profileId,user)
            .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Profile not found"));
    }

    private Asset eligibleAsset(UUID titleId, boolean child, UUID userId) {
        List<Asset> assets = jdbc.query("""
            SELECT m.id,m.title_id,m.manifest_key,m.duration_seconds,t.age_rating,t.access_tier
            FROM titles t
            JOIN creators c ON c.id=t.creator_id AND c.status='APPROVED'
            JOIN content_rights r ON r.title_id=t.id AND r.status='APPROVED'
            JOIN rights_territories rt ON rt.rights_id=r.id AND rt.country_code=?
            JOIN media_assets m ON m.title_id=t.id AND m.processing_status='READY'
            WHERE t.id=? AND t.status='PUBLISHED'
              AND r.valid_from<=? AND r.valid_until>=?
              AND m.manifest_key IS NOT NULL
            ORDER BY m.created_at DESC LIMIT 1
            """, (rs,i) -> new Asset(rs.getObject(1,UUID.class), rs.getObject(2,UUID.class),
                rs.getString(3),rs.getInt(4),rs.getString(5),rs.getString(6)),
                country,titleId,java.sql.Date.valueOf(LocalDate.now(ZoneOffset.UTC)),
                java.sql.Date.valueOf(LocalDate.now(ZoneOffset.UTC)));
        Asset a = assets.stream().findFirst().orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND,"No licensed playable media for this title in your territory"));
        if(a.duration()<=0)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Processed media duration unavailable");
        if (!PlaybackPolicy.allowedAge(child,a.age()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Profile age restriction");
        // Premium entitlements are derived solely from verified subscription records, never frontend state.
        if (!PlaybackPolicy.isFree(a.tier()) && !billing.hasPremium(userId))
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,"Active subscription required");
        return a;
    }

    /** Viewer-friendly status; does not grant video access. All checks repeat at start and on every segment. */
    @Transactional(readOnly=true)
    public Availability availability(Jwt jwt, UUID profileId, UUID titleId) {
        UUID user=userId(jwt);
        Profile profile=requireProfile(user,profileId);
        try {
            eligibleAsset(titleId,profile.child(),user);
            return new Availability(true,"READY","Video available for this profile");
        } catch (ResponseStatusException ex) {
            String code=switch(ex.getStatusCode().value()) {
                case 402 -> "SUBSCRIPTION_REQUIRED";
                case 403 -> "PROFILE_RESTRICTED";
                default -> "NOT_AVAILABLE";
            };
            String message=switch(code) {
                case "SUBSCRIPTION_REQUIRED" -> "Choose a paid plan to watch this title";
                case "PROFILE_RESTRICTED" -> "This profile cannot watch this title";
                default -> "No currently licensed and processed video is available";
            };
            return new Availability(false,code,message);
        }
    }

    @Transactional
    public Session start(Jwt jwt, Start request) {
        if (!devProxy && !cdn.enabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "No secure media delivery mechanism configured");
        if (cdn.enabled() && !cdnGeoReviewed) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "CDN playback remains disabled until trusted-geolocation integration is reviewed");
        UUID user = userId(jwt);
        // Serialize session admission even under two simultaneous clicks on separate devices.
        jdbc.queryForList("SELECT id FROM users WHERE id=? FOR UPDATE",UUID.class,user);
        Profile profile = requireProfile(user,request.profileId());
        Asset asset = eligibleAsset(request.titleId(),profile.child(),user);
        Integer active = jdbc.queryForObject("""
            SELECT COUNT(*) FROM playback_sessions
            WHERE user_id=? AND status='ACTIVE' AND expires_at>NOW()
              AND last_heartbeat_at>NOW()-interval '2 minutes'
            """,Integer.class,user);
        if (active != null && active >= MAX_SESSIONS)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Two active streams already running");
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO playback_sessions(id,user_id,profile_id,title_id,media_asset_id,country_code,expires_at)
            VALUES (?,?,?,?,?,?,NOW()+interval '10 minutes')
            """,id,user,request.profileId(),request.titleId(),asset.id(),country);
        // Server-observed play starts are stronger analytics signals than unaudited client events.
        jdbc.update("""
            INSERT INTO viewer_events(id,user_id,profile_id,title_id,event_type)
            VALUES(?,?,?,?,'PLAY_START')
            """,UUID.randomUUID(),user,request.profileId(),request.titleId());
        int position = jdbc.query("""
            SELECT position_seconds,completed FROM watch_progress WHERE profile_id=? AND media_asset_id=?
            """,rs -> rs.next() && !rs.getBoolean(2) ? rs.getInt(1) : 0,request.profileId(),asset.id());
        PlaybackCues cues=jdbc.query("""
            SELECT intro_start_seconds,intro_end_seconds,recap_start_seconds,recap_end_seconds,credits_start_seconds
            FROM media_cues WHERE media_asset_id=?
            """,(r,i)->new PlaybackCues((Integer)r.getObject(1),(Integer)r.getObject(2),
                (Integer)r.getObject(3),(Integer)r.getObject(4),(Integer)r.getObject(5)),asset.id())
            .stream().findFirst().orElse(PlaybackCues.EMPTY);
        return new Session(id,request.titleId(),cdn.enabled()?cdn.manifestUrl(asset.manifestKey()):mediaUrl(id,"master.m3u8"),
            ResumePolicy.resumePosition(position,asset.duration()),
            asset.duration(),Instant.now().plusSeconds(SESSION_MINUTES*60),30,cues);
    }

    public record NextEpisode(UUID titleId,String name,int seasonNumber,int episodeNumber) {}

    /** Return only the immediate next episode, and only if it is currently playable by this profile.
     * No autoplay bypass: the next video must still create a fresh authorized session.
     */
    @Transactional(readOnly=true)
    public NextEpisode nextEpisode(Jwt jwt,UUID profileId,UUID currentTitleId) {
        UUID user=userId(jwt);
        Profile profile=requireProfile(user,profileId);
        List<NextEpisode> candidates=jdbc.query("""
            SELECT t.id,t.name,n.season_number,n.episode_number
            FROM series_episodes current
            JOIN series_episodes n ON n.series_id=current.series_id
            JOIN titles t ON t.id=n.episode_title_id AND t.status='PUBLISHED'
            WHERE current.episode_title_id=?
              AND (n.season_number>current.season_number OR
                  (n.season_number=current.season_number AND n.episode_number>current.episode_number))
            ORDER BY n.season_number,n.episode_number LIMIT 1
            """,(r,i)->new NextEpisode(r.getObject(1,UUID.class),r.getString(2),r.getInt(3),r.getInt(4)),currentTitleId);
        if(candidates.isEmpty())return null;
        NextEpisode next=candidates.get(0);
        try { eligibleAsset(next.titleId(),profile.child(),user); }
        catch (ResponseStatusException ex) { return null; }
        return next;
    }

    public static String mediaUrl(UUID session, String relative) {
        return "/api/v1/playback/sessions/"+session+"/files/"+relative;
    }

    private StoredSession requireActiveSession(UUID user, UUID sessionId) {
        StoredSession s = jdbc.query("""
            SELECT user_id,profile_id,title_id,media_asset_id,status,expires_at,last_heartbeat_at
            FROM playback_sessions WHERE id=? AND user_id=?
            """,(rs,i) -> new StoredSession(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),
                rs.getObject(3,UUID.class),rs.getObject(4,UUID.class),rs.getString(5),
                rs.getTimestamp(6).toInstant(),rs.getTimestamp(7).toInstant()),sessionId,user)
            .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Playback session not found"));
        if (!"ACTIVE".equals(s.status()) || !s.expiresAt().isAfter(Instant.now()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Playback session expired or ended");
        // Explicitly re-evaluate on each manifest, segment, heartbeat or progress request.
        Profile profile = requireProfile(user,s.profileId());
        Asset current = eligibleAsset(s.titleId(),profile.child(),user);
        if (!current.id().equals(s.assetId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Media has been replaced or unpublished");
        return s;
    }

    @Transactional
    public Heartbeat heartbeat(Jwt jwt, UUID sessionId) {
        UUID user = userId(jwt);
        requireActiveSession(user,sessionId);
        int refreshed=jdbc.update("""
            UPDATE playback_sessions SET last_heartbeat_at=NOW(), expires_at=NOW()+interval '10 minutes'
            WHERE id=? AND user_id=? AND status='ACTIVE' AND expires_at>NOW()
            """,sessionId,user);
        if (refreshed==0) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Session no longer active");
        return new Heartbeat(Instant.now().plusSeconds(SESSION_MINUTES*60));
    }

    @Transactional
    public ProgressResult saveProgress(Jwt jwt, UUID sessionId, Progress request) {
        UUID user = userId(jwt);
        StoredSession s = requireActiveSession(user,sessionId);
        Integer duration = jdbc.queryForObject("SELECT duration_seconds FROM media_assets WHERE id=?",Integer.class,s.assetId());
        int bounded;
        try { bounded=ResumePolicy.boundedProgress(request.positionSeconds(),duration==null?0:duration); }
        catch(IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid progress or duration"); }
        int changed = jdbc.update("""
            UPDATE playback_sessions SET last_progress_seq=?,last_heartbeat_at=NOW(),
                expires_at=NOW()+interval '10 minutes'
            WHERE id=? AND user_id=? AND status='ACTIVE' AND expires_at>NOW()
              AND last_progress_seq < ?
            """,request.sequence(),sessionId,user,request.sequence());
        if (changed == 0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Stale progress event or expired session");
        boolean completed = bounded>=Math.max(1,(int)(duration*.95));
        Boolean wasCompleted=jdbc.query("""
            SELECT completed FROM watch_progress WHERE profile_id=? AND media_asset_id=?
            """,rs->rs.next()?rs.getBoolean(1):false,s.profileId(),s.assetId());
        int saved=jdbc.update("""
            INSERT INTO watch_progress(profile_id,media_asset_id,position_seconds,completed,updated_at,last_session_id)
            VALUES (?,?,?,?,NOW(),?)
            ON CONFLICT(profile_id,media_asset_id)
            DO UPDATE SET position_seconds=EXCLUDED.position_seconds,
                completed=EXCLUDED.completed,updated_at=NOW(),last_session_id=EXCLUDED.last_session_id
            WHERE watch_progress.last_session_id=EXCLUDED.last_session_id
               OR watch_progress.last_session_id IS NULL
               OR (SELECT created_at FROM playback_sessions WHERE id=watch_progress.last_session_id)
                  <= (SELECT created_at FROM playback_sessions WHERE id=EXCLUDED.last_session_id)
            """,s.profileId(),s.assetId(),bounded,completed,sessionId);
        if(saved==0) throw new ResponseStatusException(HttpStatus.CONFLICT,"A newer playback session owns progress");
        if (completed && !Boolean.TRUE.equals(wasCompleted)) {
            jdbc.update("""
                INSERT INTO viewer_events(id,user_id,profile_id,title_id,event_type)
                VALUES(?,?,?,?,'PLAY_COMPLETE')
                """,UUID.randomUUID(),user,s.profileId(),s.titleId());
        }
        return new ProgressResult(bounded,completed);
    }

    /** Opt-in only, bounded and idempotent. Event identity is supplied by the client,
     * but profile identity and active entitlement are always derived on the server.
     */
    @Transactional
    public void recordQoe(Jwt jwt,UUID sessionId,PlaybackQoeController.Event event) {
        UUID user=userId(jwt);
        StoredSession session=requireActiveSession(user,sessionId);
        Boolean consent=jdbc.query("""
            SELECT qoe_consent FROM player_preferences WHERE profile_id=? FOR SHARE
            """,r->r.next() && r.getBoolean(1),session.profileId());
        if(!Boolean.TRUE.equals(consent))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Playback telemetry requires opt-in");
        // Serialize per-session ingestion so the 200-event cap is not bypassed by concurrent requests.
        jdbc.queryForList("SELECT id FROM playback_sessions WHERE id=? FOR UPDATE",UUID.class,sessionId);
        UUID existing=jdbc.query("SELECT session_id FROM playback_qoe_events WHERE id=?",
            r->r.next()?r.getObject(1,UUID.class):null,event.eventId());
        if(existing!=null) {
            if(!existing.equals(sessionId))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Event ID belongs to another session");
            return; // exact retry is idempotent, even when the session is at its event cap.
        }
        Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM playback_qoe_events WHERE session_id=?",Integer.class,sessionId);
        if(count!=null && count>=200)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Telemetry limit reached");
        jdbc.update("""
            INSERT INTO playback_qoe_events(id,session_id,profile_id,event_type,elapsed_ms,quality_height,error_code)
            VALUES(?,?,?,?,?,?,?) ON CONFLICT(id) DO NOTHING
            """,event.eventId(),sessionId,session.profileId(),event.eventType(),event.elapsedMs(),
            event.qualityHeight(),event.errorCode());
    }

    @Transactional
    public void stop(Jwt jwt, UUID sessionId) {
        UUID user=userId(jwt);
        jdbc.update("UPDATE playback_sessions SET status='ENDED',expires_at=NOW() WHERE id=? AND user_id=? AND status='ACTIVE'",
            sessionId,user);
    }

    @Transactional
    public void clearProgress(Jwt jwt, UUID profileId, UUID titleId) {
        UUID user=userId(jwt);
        requireProfile(user,profileId);
        jdbc.update("""
            DELETE FROM watch_progress AS wp USING media_assets AS m
            WHERE wp.media_asset_id=m.id AND wp.profile_id=? AND m.title_id=?
            """,profileId,titleId);
    }

    @Transactional(readOnly=true)
    public String authorizedCdnManifest(Jwt jwt,UUID sessionId) {
        UUID user=userId(jwt);
        StoredSession s=requireActiveSession(user,sessionId);
        return jdbc.queryForObject("SELECT manifest_key FROM media_assets WHERE id=?",String.class,s.assetId());
    }

    /** All media is fetched through the *development-only* API proxy, never direct public S3. */
    @Transactional(readOnly=true)
    public MediaAccess authorizeMedia(Jwt jwt, UUID sessionId, String relative) {
        if (!PlaybackPolicy.safeRelativeMediaPath(relative))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid media path");
        UUID user=userId(jwt);
        StoredSession session=requireActiveSession(user,sessionId);
        String manifest=jdbc.queryForObject(
            "SELECT manifest_key FROM media_assets WHERE id=?",String.class,session.assetId());
        if (manifest==null || !manifest.endsWith("/master.m3u8"))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Manifest unavailable");
        String prefix=manifest.substring(0,manifest.length()-"master.m3u8".length());
        String contentType=relative.endsWith(".m3u8") ? "application/vnd.apple.mpegurl"
            : relative.endsWith(".ts") ? "video/mp2t"
            : relative.endsWith(".vtt") ? "text/vtt" : "image/jpeg";
        return new MediaAccess(manifest,prefix+relative,contentType);
    }

    @Transactional(readOnly=true)
    public List<ContinueItem> continueWatching(Jwt jwt,UUID profileId) {
        UUID user=userId(jwt);
        Profile profile=requireProfile(user,profileId);
        // Return only titles whose current access rights and age rules remain valid.
        return jdbc.query("""
            SELECT t.id,t.name,t.genre,t.art_tone,t.age_rating,m.id,
                   wp.position_seconds,m.duration_seconds,wp.updated_at
            FROM watch_progress wp
            JOIN media_assets m ON m.id=wp.media_asset_id AND m.processing_status='READY'
            JOIN titles t ON t.id=m.title_id AND t.status='PUBLISHED'
            JOIN creators c ON c.id=t.creator_id AND c.status='APPROVED'
            JOIN content_rights r ON r.title_id=t.id AND r.status='APPROVED'
            JOIN rights_territories rt ON rt.rights_id=r.id AND rt.country_code=?
            WHERE wp.profile_id=? AND wp.completed=FALSE AND wp.position_seconds>0
              AND r.valid_from<=? AND r.valid_until>=?
            ORDER BY wp.updated_at DESC LIMIT 30
            """,(rs,i)->new ContinueItem(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),
                rs.getString(4),rs.getString(5),rs.getObject(6,UUID.class),rs.getInt(7),rs.getInt(8),
                rs.getTimestamp(9).toInstant()),country,profileId,
                java.sql.Date.valueOf(LocalDate.now(ZoneOffset.UTC)),
                java.sql.Date.valueOf(LocalDate.now(ZoneOffset.UTC))).stream()
            .filter(i->PlaybackPolicy.allowedAge(profile.child(),i.ageRating()))
            .filter(i -> {
                String tier=jdbc.queryForObject("SELECT access_tier FROM titles WHERE id=?",String.class,i.titleId());
                return PlaybackPolicy.isFree(tier) || billing.hasPremium(user);
            }).distinct().toList();
    }
}
