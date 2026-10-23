package com.reelnest.notifications;

import com.reelnest.users.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/me/notifications")
public class NotificationController {
    private final JdbcTemplate db;
    private final CurrentUserService users;
    public NotificationController(JdbcTemplate db,CurrentUserService users){this.db=db;this.users=users;}
    public record Preferences(boolean inAppEnabled,boolean emailOptIn,boolean productUpdatesOptIn) {}
    public record Notice(UUID id,String kind,String title,String body,Instant createdAt,Instant readAt) {}
    public record PreferenceUpdate(@NotNull Boolean inAppEnabled,@NotNull Boolean emailOptIn,
                                   @NotNull Boolean productUpdatesOptIn) {}
    private UUID owner(Jwt jwt){return users.resolve(jwt).getId();}

    @GetMapping("/preferences")
    public Preferences preferences(@AuthenticationPrincipal Jwt jwt) {
        return db.query("SELECT in_app_enabled,email_opt_in,product_updates_opt_in FROM notification_preferences WHERE user_id=?",
            (r,i)->new Preferences(r.getBoolean(1),r.getBoolean(2),r.getBoolean(3)),owner(jwt))
            .stream().findFirst().orElse(new Preferences(true,false,false));
    }
    @PutMapping("/preferences")
    @Transactional
    public Preferences update(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody PreferenceUpdate req) {
        UUID user=owner(jwt);
        db.update("""
            INSERT INTO notification_preferences(user_id,in_app_enabled,email_opt_in,product_updates_opt_in)
            VALUES(?,?,?,?) ON CONFLICT(user_id) DO UPDATE SET
              in_app_enabled=EXCLUDED.in_app_enabled,email_opt_in=EXCLUDED.email_opt_in,
              product_updates_opt_in=EXCLUDED.product_updates_opt_in,changed_at=NOW()
            """,user,req.inAppEnabled(),req.emailOptIn(),req.productUpdatesOptIn());
        // Opting out also clears notifications that were not requested by the user.
        if(!req.productUpdatesOptIn()) db.update("DELETE FROM user_notifications WHERE user_id=? AND kind='SYSTEM'",user);
        return new Preferences(req.inAppEnabled(),req.emailOptIn(),req.productUpdatesOptIn());
    }
    @GetMapping
    public List<Notice> inbox(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="30") int limit) {
        if(limit<1||limit>100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"limit must be 1-100");
        return db.query("""
            SELECT id,kind,title,body,created_at,read_at FROM user_notifications
            WHERE user_id=? ORDER BY created_at DESC,id LIMIT ?
            """,(r,i)->new Notice(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getString(4),
                r.getTimestamp(5).toInstant(),r.getTimestamp(6)==null?null:r.getTimestamp(6).toInstant()),owner(jwt),limit);
    }
    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) {
        if(db.update("UPDATE user_notifications SET read_at=COALESCE(read_at,NOW()) WHERE id=? AND user_id=?",id,owner(jwt))!=1)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found");
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) {
        if(db.update("DELETE FROM user_notifications WHERE id=? AND user_id=?",id,owner(jwt))!=1)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found");
    }
}
