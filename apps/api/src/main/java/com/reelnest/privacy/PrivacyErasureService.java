package com.reelnest.privacy;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Deliberately conservative: no payment, creator, or admin/audit record deletion.
 * It locks the account before calling Keycloak, then erases relational viewer data
 * and writes a tombstone. If Keycloak fails, account remains locked for retry.
 */
@Service
public class PrivacyErasureService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final RestClient keycloak;
    private final String realm, clientId, secret;
    public record Token(@JsonProperty("access_token") String accessToken) {}
    private record Subject(UUID userId,String subject) {}

    public PrivacyErasureService(JdbcTemplate jdbc, PlatformTransactionManager manager,
            @Value("${app.privacy.keycloak-admin-url:}") String adminUrl,
            @Value("${app.privacy.keycloak-realm:reelnest}") String realm,
            @Value("${app.privacy.client-id:}") String clientId,
            @Value("${app.privacy.client-secret:}") String secret) {
        this.jdbc=jdbc;
        this.tx=new TransactionTemplate(manager);
        this.realm=realm;
        this.clientId=clientId;
        this.secret=secret;
        this.keycloak=RestClient.create(adminUrl==null || adminUrl.isBlank()?"http://localhost:8180":adminUrl);
    }

    public void eraseFreeViewer(UUID requestId) {
        if(clientId.isBlank() || secret.length()<16 || !realm.matches("[A-Za-z0-9_-]{1,64}"))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Keycloak erasure client must be explicitly configured");
        Subject selected=tx.execute(status -> {
            var requests=jdbc.query("""
                SELECT pr.user_id,u.identity_subject FROM privacy_requests pr
                JOIN users u ON u.id=pr.user_id
                WHERE pr.id=? AND pr.request_type='DELETE' AND pr.status IN ('REQUESTED','IN_REVIEW')
                FOR UPDATE OF pr,u
                """,(r,i)->new Subject(r.getObject(1,UUID.class),r.getString(2)),requestId);
            if(requests.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Deletion request not pending");
            Subject sub=requests.getFirst();
            // Even old, cancelled, refunded, or failed payments may trigger record-retention law.
            int other=jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM subscriptions WHERE user_id=?)
                     + (SELECT COUNT(*) FROM creators WHERE user_id=?)
                     + (SELECT COUNT(*) FROM audit_logs WHERE actor_user_id=?)
                     + (SELECT COUNT(*) FROM support_cases WHERE user_id=?)
                     + (SELECT COUNT(*) FROM support_case_events WHERE actor_user_id=?)
                     + (SELECT COUNT(*) FROM retention_legal_holds WHERE user_id=? OR created_by=?)
                     + (SELECT COUNT(*) FROM refund_review_requests WHERE requested_by=? OR approved_by=?)
                     + (SELECT COUNT(*) FROM catalog_import_batches WHERE requested_by=? OR approved_by=?)
                """,Integer.class,sub.userId(),sub.userId(),sub.userId(),sub.userId(),sub.userId(),
                sub.userId(),sub.userId(),sub.userId(),sub.userId(),sub.userId(),sub.userId());
            if(other>0) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Creator, billing, support, legal-hold or audit records require individual retention review");
            jdbc.update("UPDATE users SET status='ERASURE_LOCKED' WHERE id=?",sub.userId());
            jdbc.update("UPDATE privacy_requests SET status='IN_REVIEW' WHERE id=?",requestId);
            return sub;
        });
        if(selected==null) throw new IllegalStateException("Missing locked account");
        // Keycloak admin URL must be private, backed by a restricted confidential service client.
        // A 404 is idempotent success; all other failures keep ERASURE_LOCKED for manual retry.
        var data=new LinkedMultiValueMap<String,String>();
        data.add("grant_type","client_credentials");data.add("client_id",clientId);data.add("client_secret",secret);
        Token auth=keycloak.post().uri("/realms/{realm}/protocol/openid-connect/token",realm)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(data).retrieve().body(Token.class);
        if(auth==null || auth.accessToken()==null || auth.accessToken().isBlank())
            throw new IllegalStateException("Identity deletion token unavailable");
        try {
            keycloak.delete().uri("/admin/realms/{realm}/users/{subject}",realm,selected.subject())
                .headers(h->h.setBearerAuth(auth.accessToken())).retrieve().toBodilessEntity();
        } catch(HttpClientErrorException.NotFound ignored) {
            // Already removed; continue with local erasure.
        }
        tx.executeWithoutResult(status -> {
            // A failed DB transaction leaves the account locked. Retrying is safe.
            jdbc.update("INSERT INTO identity_tombstones(subject_digest) VALUES (?) ON CONFLICT DO NOTHING",
                IdentityTombstone.digest(selected.subject()));
            jdbc.update("DELETE FROM viewer_events WHERE user_id=?",selected.userId());
            jdbc.update("DELETE FROM playback_sessions WHERE user_id=?",selected.userId());
            jdbc.update("DELETE FROM profiles WHERE user_id=?",selected.userId());
            jdbc.update("DELETE FROM users WHERE id=?",selected.userId());
            int complete=jdbc.update("""
                UPDATE privacy_requests SET status='FULFILLED',resolved_at=NOW(),
                admin_note='Verified free-viewer identity removal and database erasure'
                WHERE id=? AND status='IN_REVIEW'
                """,requestId);
            if(complete!=1)throw new IllegalStateException("Erasure status not committed");
        });
    }
}
