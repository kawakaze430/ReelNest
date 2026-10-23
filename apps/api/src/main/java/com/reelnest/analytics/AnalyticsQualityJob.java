package com.reelnest.analytics;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
/** Monitors only aggregate schema integrity and freshness. No direct identification data. */
@Component
public class AnalyticsQualityJob {
    private final JdbcTemplate db;
    public AnalyticsQualityJob(JdbcTemplate db){this.db=db;}
    @Scheduled(cron="0 0 4 * * *",zone="UTC")
    @Transactional
    public void run(){
        var problems=db.queryForList("""
            SELECT COUNT(*) AS n FROM viewer_events e LEFT JOIN profiles p ON e.profile_id=p.id
            WHERE p.id IS NULL OR e.occurred_at>NOW()+interval '10 minutes'
            """);
        long count=((Number)problems.getFirst().get("n")).longValue();
        db.update("""
            INSERT INTO analytics_quality_checks(id,check_name,severity,observed_count)
            VALUES(?,'invalid_or_future_events',?,?)
            """,UUID.randomUUID(),count>0?"WARN":"INFO",count);
        db.update("DELETE FROM analytics_quality_checks WHERE checked_at<NOW()-interval '90 days'");
    }
}
