package com.reelnest.users;

import org.springframework.security.oauth2.jwt.Jwt;
import com.reelnest.privacy.IdentityTombstone;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {
    private final UserRepository users;
    private final JdbcTemplate jdbc;
    public CurrentUserService(UserRepository users, JdbcTemplate jdbc) {
        this.users = users;
        this.jdbc = jdbc;
    }

    @Transactional
    public UserEntity resolve(Jwt jwt) {
        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) email = subject + "@identity.invalid";
        var user = users.findByIdentitySubject(subject);
        if (user.isPresent()) return requireActive(user.get(), jwt);
        Integer erased=jdbc.queryForObject(
            "SELECT COUNT(*) FROM identity_tombstones WHERE subject_digest=?",Integer.class,
            IdentityTombstone.digest(subject));
        if(erased!=null && erased>0)throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.FORBIDDEN,"Account erased; cannot recreate from a cached token");
        // Atomic insert avoids duplicate-key failures on simultaneous first requests.
        jdbc.update("""
            INSERT INTO users(id, identity_subject, email, status, created_at)
            VALUES (?, ?, ?, 'ACTIVE', NOW())
            ON CONFLICT (identity_subject) DO NOTHING
            """, UUID.randomUUID(), subject, email);
        return requireActive(users.findByIdentitySubject(subject).orElseThrow(), jwt);
    }

    private UserEntity requireActive(UserEntity user, Jwt jwt) {
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "Account suspended");
        }
        // Revocation cutoff is read from the database on every resolved request, so
        // an earlier signed bearer JWT cannot be reused after global application logout.
        var cutoff=jdbc.query("SELECT token_valid_after FROM users WHERE id=?",
            rs -> rs.next() && rs.getTimestamp(1)!=null ? rs.getTimestamp(1).toInstant() : null,user.getId());
        if(cutoff!=null && !com.reelnest.security.TokenClaimsPolicy.issuedAfterRevocation(jwt.getIssuedAt(),cutoff))
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED,"Session revoked; sign in again");
        return user;
    }
}
