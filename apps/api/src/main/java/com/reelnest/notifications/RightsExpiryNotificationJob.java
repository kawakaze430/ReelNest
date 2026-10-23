package com.reelnest.notifications;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Idempotent in-app creator notices; never sends email or authorizes publication. */
@Component
public class RightsExpiryNotificationJob {
    private final JdbcTemplate db;
    public RightsExpiryNotificationJob(JdbcTemplate db){this.db=db;}
    private record Expiring(UUID rightsId,UUID userId,String title,LocalDate until) {}
    @Scheduled(cron="0 15 7 * * *",zone="UTC")
    @Transactional
    public void run() {
        for(int days: NotificationPolicy.EXPIRY_THRESHOLDS) {
            var rows=db.query("""
                SELECT r.id,c.user_id,t.name,r.valid_until FROM content_rights r
                JOIN titles t ON t.id=r.title_id
                JOIN creators c ON c.id=r.creator_id
                WHERE r.status='APPROVED' AND c.status='APPROVED'
                  AND r.valid_until=CURRENT_DATE+? AND t.status IN ('PUBLISHED','IN_REVIEW')
                ORDER BY r.id LIMIT 1000
                """,(rs,i)->new Expiring(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),
                    rs.getString(3),rs.getDate(4).toLocalDate()),days);
            for(var r: rows){
                String name=r.title().length()>110?r.title().substring(0,110):r.title();
                db.update("""
                    INSERT INTO user_notifications(id,user_id,kind,title,body,dedupe_key)
                    SELECT ?,?,'RIGHTS_EXPIRY',?,?,?
                    WHERE COALESCE((SELECT in_app_enabled FROM notification_preferences WHERE user_id=?),TRUE)
                    ON CONFLICT(user_id,dedupe_key) DO NOTHING
                    """,UUID.randomUUID(),r.userId(),"Streaming rights expiring",
                    name+" — expires "+r.until()+" ("+days+" days)",
                    NotificationPolicy.expiryKey(r.rightsId(),r.until(),days),r.userId());
            }
        }
        db.update("DELETE FROM user_notifications WHERE created_at<NOW()-interval '180 days'");
    }
}
