package com.reelnest.analytics;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/engagement")
@PreAuthorize("hasRole('ADMIN')")
public class EngagementAnalyticsController {
    private final JdbcTemplate db;
    public EngagementAnalyticsController(JdbcTemplate db){this.db=db;}
    public record TimeBucket(String bucket,long profiles,long events,long completed) {}
    public record Cohort(String cohort,long profiles,long events,long completed) {}
    private int checked(int days) {
        try{return AggregatePrivacyPolicy.days(days);}catch(IllegalArgumentException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage());
        }
    }
    /** Strictly aggregate, opt-in view events. Suppress buckets with <5 distinct profiles. */
    @GetMapping("/activity-by-hour")
    public List<TimeBucket> hours(@RequestParam(defaultValue="30") int days){
        return db.query("""
            SELECT to_char(date_trunc('hour',e.occurred_at AT TIME ZONE 'UTC'),'YYYY-MM-DD HH24:00') bucket,
                   COUNT(DISTINCT e.profile_id),COUNT(*),COUNT(*) FILTER(WHERE e.event_type='PLAY_COMPLETE')
            FROM viewer_events e JOIN profiles p ON p.id=e.profile_id
            WHERE p.personalization_enabled AND e.occurred_at>=NOW()-(? * interval '1 day')
            GROUP BY date_trunc('hour',e.occurred_at AT TIME ZONE 'UTC')
            HAVING COUNT(DISTINCT e.profile_id)>=5
            ORDER BY bucket DESC LIMIT 500
            """,(r,i)->new TimeBucket(r.getString(1),r.getLong(2),r.getLong(3),r.getLong(4)),checked(days));
    }
    /** Only cohort-level aggregate; no viewer identifiers or identifying country/IP fields. */
    @GetMapping("/signup-cohorts")
    public List<Cohort> cohorts(@RequestParam(defaultValue="90") int days) {
        return db.query("""
            SELECT to_char(date_trunc('week',u.created_at AT TIME ZONE 'UTC'),'IYYY-"W"IW') cohort,
                   COUNT(DISTINCT e.profile_id),COUNT(*),COUNT(*) FILTER(WHERE e.event_type='PLAY_COMPLETE')
            FROM viewer_events e JOIN users u ON u.id=e.user_id
              JOIN profiles p ON p.id=e.profile_id
            WHERE p.personalization_enabled AND e.occurred_at>=NOW()-(? * interval '1 day')
            GROUP BY date_trunc('week',u.created_at AT TIME ZONE 'UTC')
            HAVING COUNT(DISTINCT e.profile_id)>=5
            ORDER BY cohort DESC LIMIT 26
            """,(r,i)->new Cohort(r.getString(1),r.getLong(2),r.getLong(3),r.getLong(4)),checked(days));
    }
    /** Per-title viewing heatmap, suppressed for fewer than five consenting profiles. */
    public record ContentHeat(UUIDKey title,String utcHour,long profiles,long starts,long completions) {}
    public record UUIDKey(java.util.UUID id) {}
    @GetMapping("/content-heatmap")
    public List<ContentHeat> contentHeatmap(@RequestParam(defaultValue="30") int days) {
        return db.query("""
            SELECT e.title_id,to_char(date_trunc('hour',e.occurred_at AT TIME ZONE 'UTC'),'YYYY-MM-DD HH24:00'),
                   COUNT(DISTINCT e.profile_id),
                   COUNT(*) FILTER(WHERE e.event_type='PLAY_START'),
                   COUNT(*) FILTER(WHERE e.event_type='PLAY_COMPLETE')
            FROM viewer_events e JOIN profiles p ON p.id=e.profile_id
            WHERE p.personalization_enabled AND e.title_id IS NOT NULL
              AND e.occurred_at>=NOW()-(? * interval '1 day')
            GROUP BY e.title_id,date_trunc('hour',e.occurred_at AT TIME ZONE 'UTC')
            HAVING COUNT(DISTINCT e.profile_id)>=5
            ORDER BY 2 DESC LIMIT 500
            """,(r,i)->new ContentHeat(new UUIDKey(r.getObject(1,java.util.UUID.class)),r.getString(2),
                  r.getLong(3),r.getLong(4),r.getLong(5)),checked(days));
    }
    /** Episode performance, grouped by series and season; no individual viewer results. */
    public record EpisodeAggregate(java.util.UUID seriesId,int season,int episode,long profiles,long completions) {}
    @GetMapping("/episode-aggregates")
    public List<EpisodeAggregate> episodeStats(@RequestParam(defaultValue="30") int days) {
        return db.query("""
            SELECT se.series_id,se.season_number,se.episode_number,
                 COUNT(DISTINCT e.profile_id),COUNT(*) FILTER(WHERE e.event_type='PLAY_COMPLETE')
            FROM series_episodes se JOIN viewer_events e ON e.title_id=se.episode_title_id
              JOIN profiles p ON p.id=e.profile_id
            WHERE p.personalization_enabled AND e.occurred_at>=NOW()-(? * interval '1 day')
            GROUP BY se.series_id,se.season_number,se.episode_number
            HAVING COUNT(DISTINCT e.profile_id)>=5
            ORDER BY se.series_id,se.season_number,se.episode_number LIMIT 500
            """,(r,i)->new EpisodeAggregate(r.getObject(1,java.util.UUID.class),r.getInt(2),r.getInt(3),
                r.getLong(4),r.getLong(5)),checked(days));
    }
    /** Country-level QoE, only when enough opt-in profiles contribute; never expose ISP or device IDs. */
    public record CountryQoe(String country,long profiles,long startupSamples,Double meanStartupMs,long stalls) {}
    @GetMapping("/qoe-by-country")
    public List<CountryQoe> countries(@RequestParam(defaultValue="30") int days) {
        return db.query("""
            SELECT s.country_code,COUNT(DISTINCT q.profile_id),
              COUNT(*) FILTER(WHERE q.event_type='STARTUP'),
              AVG(q.elapsed_ms) FILTER(WHERE q.event_type='STARTUP'),
              COUNT(*) FILTER(WHERE q.event_type='STALL')
            FROM playback_qoe_events q JOIN playback_sessions s ON s.id=q.session_id
              JOIN player_preferences pref ON pref.profile_id=q.profile_id
            WHERE pref.qoe_consent AND q.created_at>=NOW()-(? * interval '1 day')
            GROUP BY s.country_code HAVING COUNT(DISTINCT q.profile_id)>=5
            ORDER BY s.country_code LIMIT 250
            """,(r,i)->new CountryQoe(r.getString(1),r.getLong(2),r.getLong(3),
                 r.getBigDecimal(4)==null?null:r.getBigDecimal(4).doubleValue(),r.getLong(5)),checked(days));
    }
    @GetMapping("/quality")
    public List<Map<String,Object>> quality(){
        return db.queryForList("""
            SELECT check_name,severity,observed_count,checked_at FROM analytics_quality_checks
            WHERE checked_at>=NOW()-interval '30 days' ORDER BY checked_at DESC LIMIT 200
            """);
    }
}
