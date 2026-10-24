package com.reelnest.avod;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Expiry and consent revocation sweep for untrusted, non-billable preview signals. */
@Component
public class AvodPreviewRetentionJob {
  private final JdbcTemplate db;
  public AvodPreviewRetentionJob(JdbcTemplate db) {this.db=db;}

  @Scheduled(cron="0 40 2 * * *",zone="UTC")
  @Transactional
  public void purge() {
    db.update("DELETE FROM avod_preview_events WHERE created_at<NOW()-interval '7 days'");
    db.update("""
      DELETE FROM avod_preview_events e WHERE NOT EXISTS(
        SELECT 1 FROM avod_profile_preferences p JOIN profiles v ON v.id=p.profile_id
        WHERE p.profile_id=e.profile_id AND p.contextual_previews_opt_in
          AND p.anonymous_measurement_opt_in AND NOT v.is_child
      )
      """);
  }
}
