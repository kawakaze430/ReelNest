package com.reelnest.users;

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

/** Lists and revokes APP playback sessions and JWTs. Does not pretend to
 * enumerate external Keycloak browser sessions or revoke Keycloak refresh tokens.
 */
@RestController
@RequestMapping("/api/v1/me/sessions")
public class AccountSessionsController {
    private final CurrentUserService users;
    private final JdbcTemplate jdbc;
    public AccountSessionsController(CurrentUserService users,JdbcTemplate jdbc){
        this.users=users;this.jdbc=jdbc;
    }
    public record SessionView(UUID id,UUID profileId,Instant createdAt,Instant lastHeartbeat,
                              Instant expiresAt,String status) {}
    public record Revoked(int affected,String scope) {}

    @GetMapping
    public List<SessionView> list(@AuthenticationPrincipal Jwt jwt) {
        UUID user=users.resolve(jwt).getId();
        return jdbc.query("""
            SELECT id,profile_id,created_at,last_heartbeat_at,expires_at,status
            FROM playback_sessions WHERE user_id=?
            ORDER BY created_at DESC LIMIT 50
            """,(rs,i)->new SessionView(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),
                rs.getTimestamp(3).toInstant(),rs.getTimestamp(4).toInstant(),
                rs.getTimestamp(5).toInstant(),rs.getString(6)),user);
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void revokePlayback(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID sessionId) {
        UUID user=users.resolve(jwt).getId();
        int changed=jdbc.update("""
            UPDATE playback_sessions SET status='REVOKED',expires_at=NOW()
            WHERE id=? AND user_id=? AND status='ACTIVE'
            """,sessionId,user);
        if(changed!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Active playback session not found");
    }

    /** Revokes all APP bearer JWTs issued before this database cutoff plus active
     * playback sessions. Frontend must also sign out through OIDC to clear SSO.
     */
    @PostMapping("/revoke-all")
    @Transactional
    public Revoked revokeAll(@AuthenticationPrincipal Jwt jwt) {
        UUID user=users.resolve(jwt).getId();
        jdbc.queryForList("SELECT id FROM users WHERE id=? FOR UPDATE",UUID.class,user);
        int count=jdbc.update("""
            UPDATE playback_sessions SET status='REVOKED',expires_at=NOW()
            WHERE user_id=? AND status='ACTIVE'
            """,user);
        jdbc.update("UPDATE users SET token_valid_after=NOW() WHERE id=?",user);
        return new Revoked(count,"API_JWTS_AND_PLAYBACK_ONLY");
    }
}
