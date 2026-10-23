package com.reelnest.discovery;

import com.reelnest.billing.BillingService;
import com.reelnest.catalog.TitleResponse;
import com.reelnest.catalog.TitleRepository;
import com.reelnest.playback.PlaybackPolicy;
import com.reelnest.users.CurrentUserService;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DiscoveryService {
    private final JdbcTemplate jdbc;
    private final CurrentUserService users;
    private final BillingService billing;
    private final TitleRepository titles;
    private final String territory;
    private final PrivateRecommendationClient ml;
    public DiscoveryService(JdbcTemplate jdbc,CurrentUserService users,BillingService billing,
            TitleRepository titles,PrivateRecommendationClient ml,
            @Value("${app.playback.trusted-country:IN}") String territory) {
        this.jdbc=jdbc;this.users=users;this.billing=billing;this.titles=titles;this.ml=ml;this.territory=territory;
    }
    public record Item(TitleResponse title,int score,String reason) {}
    private record Profile(boolean child,String language,boolean personalized) {}
    private record Candidate(UUID id,String genre,String language,boolean featured,int year,long interactions,String tier) {}
    private record Preferences(Map<String,Integer> genreWeights,Set<UUID> seen) {}

    private Profile owner(Jwt jwt,UUID profileId) {
        UUID user=users.resolve(jwt).getId();
        return jdbc.query("SELECT is_child,preferred_language,personalization_enabled FROM profiles WHERE id=? AND user_id=?",
            (r,i)->new Profile(r.getBoolean(1),r.getString(2),r.getBoolean(3)),profileId,user).stream().findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Profile not found"));
    }
    @Transactional
    public List<Item> home(Jwt jwt,UUID profileId,int limit) {
        if(limit<1||limit>48) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Limit must be 1-48");
        Profile profile=owner(jwt,profileId);
        boolean premium=billing.hasPremium(users.resolve(jwt).getId());
        Preferences preferences=profile.personalized()?preferences(profileId):new Preferences(Map.of(),Set.of());
        // Rights-gated: no recommendation for unlicensed, unprocessed, or expired films.
        List<Candidate> candidates=jdbc.query("""
            SELECT t.id,t.genre,t.language,t.featured,COALESCE(t.release_year,0),
             (SELECT COUNT(*) FROM viewer_events e WHERE e.title_id=t.id
               AND e.event_type IN ('PLAY_START','PLAY_COMPLETE','DETAIL_VIEW')
               AND e.occurred_at>NOW()-interval '30 days') AS engagement,
             t.access_tier
            FROM titles t WHERE t.status='PUBLISHED'
              AND EXISTS(SELECT 1 FROM creators c WHERE c.id=t.creator_id AND c.status='APPROVED')
              AND EXISTS(SELECT 1 FROM content_rights r JOIN rights_territories rt
                         ON rt.rights_id=r.id AND rt.country_code=?
                         WHERE r.title_id=t.id AND r.status='APPROVED'
                           AND r.valid_from<=CURRENT_DATE AND r.valid_until>=CURRENT_DATE)
              AND EXISTS(SELECT 1 FROM media_assets m WHERE m.title_id=t.id
                         AND m.processing_status='READY' AND m.manifest_key IS NOT NULL)
            LIMIT 1000
            """,(r,i)->new Candidate(r.getObject(1,UUID.class),r.getString(2),r.getString(3),
                r.getBoolean(4),r.getInt(5),r.getLong(6),r.getString(7)),territory);
        List<Item> ranking=new ArrayList<>();
        for(Candidate c:candidates) {
            var entity=titles.findByIdAndStatus(c.id(),"PUBLISHED");
            if(entity.isEmpty() || !PlaybackPolicy.allowedAge(profile.child(),entity.get().getAgeRating()))continue;
            boolean liked=preferences.genreWeights().getOrDefault(c.genre(),0)>0;
            boolean familiar=preferences.seen().contains(c.id());
            int score=15;
            score+=Math.min(35,preferences.genreWeights().getOrDefault(c.genre(),0)*9);
            if(Objects.equals(c.language(),profile.language()))score+=15;
            score+=Math.min(20,(int)c.interactions()*2);
            if(c.featured())score+=5;
            if(c.year()>=2025)score+=5;
            if(familiar)score-=22; // Re-rank titles already played lower, but do not hide them.
            if(!premium && !"FREE".equals(c.tier()))score-=3; // Paid films can still be discovered.
            String reason=liked?"Because you enjoy "+c.genre() :
                c.interactions()>0?"Popular with viewers":
                Objects.equals(c.language(),profile.language())?"In your preferred language":"Explore something new";
            ranking.add(new Item(TitleResponse.from(entity.get()),Math.max(0,score),reason));
        }
        ranking.sort(Comparator.comparingInt(Item::score).reversed()
            .thenComparing(i->i.title().name()));
        return profile.personalized()?ml.rerank(ranking,preferences.seen(),profile.language(),limit)
            : ranking.stream().limit(limit).toList();
    }
    /** Search only authorized catalog candidates for this viewer; ML cannot introduce new titles. */
    public List<Item> semantic(Jwt jwt,UUID profileId,String query,int limit) {
        if(query==null || query.isBlank() || query.length()>200 || limit<1 || limit>24)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid query or limit");
        return ml.semanticSearch(home(jwt,profileId,48),query,limit);
    }
    private Preferences preferences(UUID profileId) {
        Map<String,Integer> weights=new HashMap<>();
        Set<UUID> seen=new HashSet<>();
        jdbc.query("""
            SELECT t.id,t.genre,4 AS weight FROM watchlists w
              JOIN titles t ON t.id=w.title_id WHERE w.profile_id=?
            UNION ALL
            SELECT t.id,t.genre,2 AS weight FROM watch_progress wp
              JOIN media_assets m ON m.id=wp.media_asset_id
              JOIN titles t ON t.id=m.title_id WHERE wp.profile_id=?
            """,r->{
                UUID id=r.getObject(1,UUID.class);
                weights.merge(r.getString(2),r.getInt(3),Integer::sum);
                seen.add(id);
            },profileId,profileId);
        return new Preferences(weights,seen);
    }
    @Transactional
    public void ingest(Jwt jwt,UUID eventId,UUID profileId,UUID titleId,String type) {
        if(!Set.of("IMPRESSION","DETAIL_VIEW","PLAY_START","PLAY_COMPLETE","SEARCH","WATCHLIST_ADD").contains(type))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported event");
        Profile p=owner(jwt,profileId);
        UUID user=users.resolve(jwt).getId();
        if(!p.personalized())return; // Viewing analytics ingestion requires explicit preference.
        if(titleId!=null) {
            var t=titles.findByIdAndStatus(titleId,"PUBLISHED").orElseThrow(
                ()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Title not found"));
            if(!PlaybackPolicy.allowedAge(p.child(),t.getAgeRating()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Profile restriction");
        } else if(!"SEARCH".equals(type)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Title required");
        jdbc.update("""
            INSERT INTO viewer_events(id,user_id,profile_id,title_id,event_type)
            VALUES(?,?,?,?,?) ON CONFLICT(id) DO NOTHING
            """,eventId,user,profileId,titleId,type);
    }
    public record Overview(long viewers,long titles,long paidSubscribers,long playbackStarts,long watchlistAdds) {}
    public record PopularTitle(UUID titleId,String name,long plays) {}
    @Transactional(readOnly=true)
    public Overview overview() {
        return new Overview(count("SELECT COUNT(*) FROM users"),
            count("SELECT COUNT(*) FROM titles WHERE status='PUBLISHED'"),
            count("SELECT COUNT(DISTINCT user_id) FROM subscriptions WHERE status IN ('ACTIVE','CANCEL_SCHEDULED') AND paid_through>NOW()"),
            count("SELECT COUNT(*) FROM viewer_events WHERE event_type='PLAY_START' AND occurred_at>NOW()-interval '30 days'"),
            count("SELECT COUNT(*) FROM viewer_events WHERE event_type='WATCHLIST_ADD' AND occurred_at>NOW()-interval '30 days'"));
    }
    private long count(String sql) { Long n=jdbc.queryForObject(sql,Long.class);return n==null?0:n; }
    @Transactional(readOnly=true)
    public List<PopularTitle> topTitles() {
        return jdbc.query("""
            SELECT t.id,t.name,COUNT(e.id) plays FROM titles t JOIN viewer_events e ON e.title_id=t.id
            WHERE e.event_type='PLAY_START' AND e.occurred_at>NOW()-interval '30 days'
            GROUP BY t.id,t.name ORDER BY plays DESC,t.name ASC LIMIT 10
            """,(r,i)->new PopularTitle(r.getObject(1,UUID.class),r.getString(2),r.getLong(3)));
    }
}
