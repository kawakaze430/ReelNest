package com.reelnest.discovery;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Operational guardrail: bounded personal viewing event retention by default. */
@Component
public class EventRetentionJob {
    private final JdbcTemplate jdbc;
    public EventRetentionJob(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Scheduled(cron="0 15 2 * * *",zone="UTC")
    public void purgeOldEvents() {
        // Delete in bounded batches; data-subject requests require a separate workflow.
        for (int batch=0;batch<10;batch++) {
            int removed=jdbc.update("""
                DELETE FROM viewer_events WHERE id IN (
                  SELECT e.id FROM viewer_events e WHERE e.occurred_at<NOW()-interval '180 days'
                  AND NOT EXISTS(SELECT 1 FROM retention_legal_holds h WHERE h.user_id=e.user_id
                    AND h.released_at IS NULL AND h.expires_at>NOW())
                  LIMIT 1000
                )
                """);
            if(removed<1000)break;
        }
        // Temporary staged CSVs contain creator metadata and must not accumulate forever.
        for(int batch=0;batch<5;batch++) {
            int removed=jdbc.update("""
                DELETE FROM catalog_import_batches WHERE id IN (
                  SELECT id FROM catalog_import_batches
                  WHERE expires_at<NOW()-interval '7 days' LIMIT 1000
                )
                """);
            if(removed<1000)break;
        }
        // QoE is shorter-lived (30 days) than basic viewing events (180 days).
        for (int batch=0;batch<10;batch++) {
            int removed=jdbc.update("""
                DELETE FROM playback_qoe_events WHERE id IN (
                  SELECT q.id FROM playback_qoe_events q JOIN profiles p ON p.id=q.profile_id
                  WHERE q.created_at<NOW()-interval '30 days'
                  AND NOT EXISTS(SELECT 1 FROM retention_legal_holds h WHERE h.user_id=p.user_id
                    AND h.released_at IS NULL AND h.expires_at>NOW())
                  LIMIT 1000
                )
                """);
            if(removed<1000)break;
        }
    }
}
