package com.reelnest.privacy;

import com.reelnest.users.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
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

/** Data subject workflows. Deletion is a reviewed request, NOT a pretend completed deletion. */
@RestController
@RequestMapping("/api/v1/privacy")
public class PrivacyController {
    private final JdbcTemplate jdbc;
    private final CurrentUserService users;
    private final PrivacyErasureService erasure;
    public PrivacyController(JdbcTemplate jdbc, CurrentUserService users, PrivacyErasureService erasure) {
        this.jdbc=jdbc;this.users=users;this.erasure=erasure;
    }
    public record Request(UUID id,String type,String status,Instant createdAt) {}
    public record Decision(@NotBlank @Pattern(regexp="IN_REVIEW|REJECTED") String status,
                           @Size(max=500) String note) {}
    private UUID user(Jwt jwt) { return users.resolve(jwt).getId(); }

    @GetMapping("/export")
    @Transactional(readOnly=true)
    public Map<String,Object> export(@AuthenticationPrincipal Jwt jwt) {
        UUID u=user(jwt);
        // No other user's records and no identity-provider password/token material.
        return Map.of(
            "account", jdbc.queryForMap("SELECT id,email,status,created_at FROM users WHERE id=?",u),
            "profiles", jdbc.queryForList("SELECT id,name,preferred_language,is_child FROM profiles WHERE user_id=?",u),
            "watchlist",jdbc.queryForList("""
                SELECT w.profile_id,w.title_id,w.created_at FROM watchlists w
                JOIN profiles p ON p.id=w.profile_id WHERE p.user_id=?
                ORDER BY w.created_at DESC LIMIT 10000
                """,u),
            "progress", jdbc.queryForList("""
                SELECT wp.profile_id,wp.media_asset_id,wp.position_seconds,wp.completed,wp.updated_at
                FROM watch_progress wp JOIN profiles p ON p.id=wp.profile_id
                WHERE p.user_id=? ORDER BY wp.updated_at DESC LIMIT 10000
                """,u),
            "supportCases",jdbc.queryForList("""
                SELECT id,title_id,parent_case_id,category,subject,description,status,resolution,created_at,updated_at
                FROM support_cases WHERE user_id=? ORDER BY created_at DESC LIMIT 1000
                """,u),
            "subscriptions",jdbc.queryForList("""
                SELECT id,plan_code,status,paid_through,cancel_at_cycle_end,created_at
                FROM subscriptions WHERE user_id=? ORDER BY created_at DESC LIMIT 1000
                """,u),
            "metadata", Map.of("scope","application data only; identity provider and legal billing records require separate export")
        );
    }

    @GetMapping("/requests")
    @Transactional(readOnly=true)
    public List<Request> requests(@AuthenticationPrincipal Jwt jwt) {
        UUID u=user(jwt);
        return jdbc.query("""
            SELECT id,request_type,status,requested_at FROM privacy_requests
            WHERE user_id=? ORDER BY requested_at DESC LIMIT 100
            """,(r,i)->new Request(r.getObject(1,UUID.class),r.getString(2),r.getString(3),
            r.getTimestamp(4).toInstant()),u);
    }

    @PostMapping("/deletion-request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Transactional
    public Request requestDeletion(@AuthenticationPrincipal Jwt jwt) {
        UUID u=user(jwt);
        UUID id=UUID.randomUUID();
        jdbc.update("""
            INSERT INTO privacy_requests(id,user_id,request_type)
            VALUES(?,?,'DELETE') ON CONFLICT DO NOTHING
            """,id,u);
        return jdbc.query("""
            SELECT id,request_type,status,requested_at FROM privacy_requests
            WHERE user_id=? AND request_type='DELETE'
              AND status IN ('REQUESTED','IN_REVIEW')
            ORDER BY requested_at DESC LIMIT 1
            """,(r,i)->new Request(r.getObject(1,UUID.class),r.getString(2),r.getString(3),
            r.getTimestamp(4).toInstant()),u).stream().findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"No pending deletion request"));
    }

    @PostMapping("/admin/requests/{id}/erase-free-viewer")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eraseFreeViewer(@PathVariable UUID id) {
        erasure.eraseFreeViewer(id);
    }

    @GetMapping("/admin/requests")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly=true)
    public List<Map<String,Object>> adminRequests() {
        return jdbc.queryForList("""
            SELECT pr.id,pr.user_id,pr.request_type,pr.status,pr.requested_at
            FROM privacy_requests pr ORDER BY pr.requested_at DESC LIMIT 100
            """);
    }
    @PostMapping("/admin/requests/{id}/decision")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void decision(@PathVariable UUID id,@Valid @RequestBody Decision decision) {
        int changed=jdbc.update("""
            UPDATE privacy_requests SET status=?,admin_note=?,resolved_at=CASE
                WHEN ? = 'REJECTED' THEN NOW() ELSE NULL END
            WHERE id=? AND status IN ('REQUESTED','IN_REVIEW')
            """,decision.status(),decision.note(),decision.status(),id);
        if(changed==0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Request not pending");
        // Intentionally cannot mark FULFILLED: a verified cross-system deletion
        // workflow is not implemented, and falsely confirming deletion is unsafe.
    }
}
