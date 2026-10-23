package com.reelnest.admin;

import com.reelnest.catalog.TitleRepository;
import com.reelnest.playback.PlaybackCues;
import com.reelnest.creators.CreatorService;
import com.reelnest.rights.RightsService;
import com.reelnest.users.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final CreatorService creators;
    private final RightsService rights;
    private final TitleRepository titles;
    private final CurrentUserService users;
    private final JdbcTemplate jdbc;
    public AdminController(CreatorService creators, RightsService rights, TitleRepository titles,
                           CurrentUserService users, JdbcTemplate jdbc) {
        this.creators = creators; this.rights = rights; this.titles = titles;
        this.users = users; this.jdbc = jdbc;
    }
    /** Aggregate only. Do not expose viewer, profile, session, IP or individual event data. */
    public record QoeSummary(long consentingProfiles,long startupSamples,long stalls,
                             Double meanStartupMs,Double meanStallMs,boolean enoughSamples) {}
    @GetMapping("/qoe/summary")
    public QoeSummary qoeSummary() {
        return jdbc.query("""
            SELECT COUNT(DISTINCT profile_id),
                   COUNT(*) FILTER(WHERE event_type='STARTUP'),
                   COUNT(*) FILTER(WHERE event_type='STALL'),
                   AVG(elapsed_ms) FILTER(WHERE event_type='STARTUP'),
                   AVG(elapsed_ms) FILTER(WHERE event_type='STALL')
            FROM playback_qoe_events WHERE created_at>=NOW()-interval '7 days'
            """,r->{
                if(!r.next())return new QoeSummary(0,0,0,null,null,false);
                long sessions=r.getLong(1);
                // Suppress small cohorts. This is pseudonymous telemetry, not anonymous data.
                if(sessions<5)return new QoeSummary(0,0,0,null,null,false);
                return new QoeSummary(sessions,r.getLong(2),r.getLong(3),
                    r.getBigDecimal(4)==null?null:r.getBigDecimal(4).doubleValue(),
                    r.getBigDecimal(5)==null?null:r.getBigDecimal(5).doubleValue(),true);
            });
    }

    @GetMapping("/titles/seo-review")
    public List<Map<String,Object>> seoReview() {
        return jdbc.queryForList("""
            SELECT id,name,seo_index_approved FROM titles
            WHERE status='PUBLISHED' ORDER BY created_at DESC LIMIT 100
            """);
    }

    /** Explicit SEO approval never bypasses the live rights/READY checks in PublicSeoController. */
    public record SeoApproval(boolean approved) {}
    @PostMapping("/titles/{titleId}/seo-approval")
    @Transactional
    public void approveSeo(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID titleId,
                           @Valid @RequestBody SeoApproval input) {
        if(input.approved()) {
            Integer eligible=jdbc.queryForObject("""
                SELECT COUNT(*) FROM titles t
                JOIN creators c ON c.id=t.creator_id AND c.status='APPROVED'
                JOIN content_rights r ON r.title_id=t.id AND r.status='APPROVED'
                WHERE t.id=? AND t.status='PUBLISHED'
                  AND r.valid_from<=CURRENT_DATE AND r.valid_until>=CURRENT_DATE
                  AND EXISTS(SELECT 1 FROM rights_territories rt WHERE rt.rights_id=r.id)
                  AND EXISTS(SELECT 1 FROM media_assets m WHERE m.title_id=t.id
                    AND m.processing_status='READY' AND m.manifest_key IS NOT NULL)
                """,Integer.class,titleId);
            if(eligible==null || eligible!=1)
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Only licensed, READY published titles may be indexed");
        }
        int changed=jdbc.update("UPDATE titles SET seo_index_approved=? WHERE id=?",input.approved(),titleId);
        if(changed!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Title not found");
        creators.audit(users.resolve(jwt).getId(),input.approved()?"SEO_APPROVED":"SEO_REVOKED",
            "title",titleId,null);
    }

    @GetMapping("/creators/pending")
    public List<CreatorService.CreatorView> pendingCreators() { return creators.pending(); }

    @PostMapping("/creators/{id}/review")
    public CreatorService.CreatorView reviewCreator(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
        @Valid @RequestBody CreatorService.Review review) { return creators.review(jwt, id, review); }

    @GetMapping("/titles/review")
    public List<Map<String, Object>> pendingTitles() {
        return jdbc.queryForList("""
            SELECT t.id, t.name, t.status, t.access_tier, c.display_name AS creator_name,
                   r.status AS rights_status, r.valid_from, r.valid_until,
                   (SELECT COUNT(*) FROM upload_intents u WHERE u.title_id=t.id AND u.status='COMPLETED') AS uploaded_files,
                   (SELECT COUNT(*) FROM media_assets m WHERE m.title_id=t.id AND m.processing_status='READY' AND m.manifest_key IS NOT NULL) AS ready_assets
            FROM titles t JOIN creators c ON c.id=t.creator_id
            LEFT JOIN content_rights r ON r.title_id=t.id
            WHERE t.status='IN_REVIEW'
            ORDER BY t.created_at ASC
            """);
    }
    @GetMapping("/titles/{titleId}/media")
    public List<Map<String,Object>> titleMedia(@PathVariable UUID titleId) {
        if (!titles.existsById(titleId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Title not found");
        return jdbc.queryForList("""
            SELECT m.id,m.processing_status,m.duration_seconds,m.processing_details,
                   j.state AS job_state,j.attempts,j.max_attempts,j.last_error,j.updated_at,
                   j.progress_stage,j.progress_percent
            FROM media_assets m LEFT JOIN media_processing_jobs j ON m.id=j.media_asset_id
            WHERE m.title_id=? ORDER BY m.created_at DESC
            """,titleId);
    }
    @PostMapping("/titles/{titleId}/media/{assetId}/retry")
    @Transactional
    public void retryMedia(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID titleId,
                           @PathVariable UUID assetId) {
        var title = titles.findById(titleId).orElseThrow(
            ()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Title not found"));
        if (!"IN_REVIEW".equals(title.getStatus()) && !"DRAFT".equals(title.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Reviewable title required");
        int changed = jdbc.update("""
            UPDATE media_processing_jobs AS j SET state='QUEUED',progress_stage='QUEUED',progress_percent=0,attempts=0,worker_id=NULL,
                lease_until=NULL,available_at=NOW(),last_error=NULL,finished_at=NULL,updated_at=NOW()
            WHERE j.media_asset_id=? AND j.state='FAILED'
              AND EXISTS(SELECT 1 FROM media_assets m WHERE m.id=j.media_asset_id AND m.title_id=?)
            """,assetId,titleId);
        if (changed!=1)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Only FAILED media for this title can be retried");
        jdbc.update("UPDATE media_assets SET processing_status='QUEUED',manifest_key=NULL,processing_details=NULL WHERE id=?",assetId);
        creators.audit(users.resolve(jwt).getId(),"MEDIA_RETRY_REQUESTED","title",titleId,assetId.toString());
    }

    @GetMapping("/titles/{titleId}/media/{assetId}/cues")
    public PlaybackCues getCues(@PathVariable UUID titleId,@PathVariable UUID assetId) {
        Integer exists=jdbc.queryForObject("""
            SELECT COUNT(*) FROM media_assets WHERE id=? AND title_id=?
            """,Integer.class,assetId,titleId);
        if(exists==null || exists!=1)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Media asset not found");
        return jdbc.query("""
            SELECT intro_start_seconds,intro_end_seconds,recap_start_seconds,recap_end_seconds,credits_start_seconds
            FROM media_cues WHERE media_asset_id=?
            """,(r,i)->new PlaybackCues((Integer)r.getObject(1),(Integer)r.getObject(2),
                (Integer)r.getObject(3),(Integer)r.getObject(4),(Integer)r.getObject(5)),assetId)
            .stream().findFirst().orElse(PlaybackCues.EMPTY);
    }

    /** Admin-reviewed timeline markers, scoped to a READY asset and its actual duration. */
    @PutMapping("/titles/{titleId}/media/{assetId}/cues")
    @Transactional
    public PlaybackCues saveCues(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID titleId,
                                 @PathVariable UUID assetId,@RequestBody PlaybackCues cues) {
        Integer duration=jdbc.query("""
            SELECT duration_seconds FROM media_assets
            WHERE id=? AND title_id=? AND processing_status='READY'
            """,r->r.next()?(Integer)r.getObject(1):null,assetId,titleId);
        if(duration==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"READY media asset not found");
        if(cues==null || !cues.validFor(duration))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid cue positions for media duration");
        jdbc.update("""
            INSERT INTO media_cues(media_asset_id,intro_start_seconds,intro_end_seconds,
                recap_start_seconds,recap_end_seconds,credits_start_seconds)
            VALUES(?,?,?,?,?,?)
            ON CONFLICT(media_asset_id) DO UPDATE SET
                intro_start_seconds=EXCLUDED.intro_start_seconds,intro_end_seconds=EXCLUDED.intro_end_seconds,
                recap_start_seconds=EXCLUDED.recap_start_seconds,recap_end_seconds=EXCLUDED.recap_end_seconds,
                credits_start_seconds=EXCLUDED.credits_start_seconds,updated_at=NOW()
            """,assetId,cues.introStartSeconds(),cues.introEndSeconds(),cues.recapStartSeconds(),
            cues.recapEndSeconds(),cues.creditsStartSeconds());
        creators.audit(users.resolve(jwt).getId(),"MEDIA_CUES_UPDATED","title",titleId,assetId.toString());
        return cues;
    }

    @GetMapping("/titles/{titleId}/rights")
    public RightsService.RightsView titleRights(@PathVariable UUID titleId) {
        return rights.viewForTitle(titleId);
    }
    @PostMapping("/titles/{titleId}/rights-review")
    public RightsService.RightsView reviewRights(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID titleId,
        @Valid @RequestBody RightsService.Review review) { return rights.review(jwt, titleId, review); }

    public record Reject(@NotBlank @Size(max = 1000) String note) {}
    @PostMapping("/titles/{titleId}/reject")
    @Transactional
    public void reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID titleId,
                       @Valid @RequestBody Reject input) {
        var title = titles.findById(titleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Title not found"));
        if (!"IN_REVIEW".equals(title.getStatus()) || title.getCreatorId() == null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Title is not under review");
        title.setStatus("REJECTED");
        creators.audit(users.resolve(jwt).getId(), "TITLE_REJECTED", "title", titleId, input.note());
    }

    @PostMapping("/titles/{titleId}/publish")
    @Transactional
    public void publish(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID titleId) {
        var title = titles.findById(titleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Title not found"));
        if (!"IN_REVIEW".equals(title.getStatus()) || title.getCreatorId() == null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Title is not under review");
        RightsService.RightsView r = rights.viewForTitle(titleId);
        if (!"APPROVED".equals(r.status()) || r.validFrom().isAfter(LocalDate.now(java.time.ZoneOffset.UTC)) ||
            r.validUntil().isBefore(LocalDate.now(java.time.ZoneOffset.UTC)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Approved, currently valid streaming rights required");
        Long ready = jdbc.queryForObject("SELECT COUNT(*) FROM media_assets WHERE title_id=? AND processing_status='READY' AND manifest_key IS NOT NULL", Long.class, titleId);
        if (ready == null || ready < 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transcoded READY asset required; processing may still be running");
        title.setStatus("PUBLISHED");
        creators.audit(users.resolve(jwt).getId(), "TITLE_PUBLISHED", "title", titleId, null);
    }
}
