package com.reelnest.analytics;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Read-only account anomaly review. Never revoke access or call a user fraudulent automatically. */
@RestController
@RequestMapping("/api/v1/admin/playback-risk")
@PreAuthorize("hasRole('ADMIN')")
public class PlaybackRiskController {
    private final JdbcTemplate db;
    public PlaybackRiskController(JdbcTemplate db){this.db=db;}
    public record Summary(String signal,long affectedAccounts,long totalSessions) {}
    @GetMapping("/summary")
    public List<Summary> summary(){
        // Aggregate only: no raw IPs, account IDs, devices or profile IDs in API response.
        return db.query("""
            WITH active_accounts AS (
              SELECT user_id,COUNT(*) AS sessions FROM playback_sessions
              WHERE status='ACTIVE' AND expires_at>NOW() AND last_heartbeat_at>NOW()-interval '5 minutes'
              GROUP BY user_id HAVING COUNT(*)>2
            ) SELECT COUNT(*),COALESCE(SUM(sessions),0) FROM active_accounts
            """,(r,i)->{
                long accounts=r.getLong(1), sessions=r.getLong(2);
                if(accounts<AggregatePrivacyPolicy.MIN_PROFILES)return new Summary("concurrent_session_excess",0,0);
                return new Summary("concurrent_session_excess",accounts,sessions);
            }).stream().toList();
    }
}
