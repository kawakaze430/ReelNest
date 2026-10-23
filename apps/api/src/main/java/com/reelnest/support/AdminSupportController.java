package com.reelnest.support;

import com.reelnest.users.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/support")
@PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
public class AdminSupportController {
    private final JdbcTemplate db;
    private final CurrentUserService users;
    public AdminSupportController(JdbcTemplate db,CurrentUserService users){this.db=db;this.users=users;}
    public record AdminCase(UUID id,UUID ownerId,UUID titleId,String category,String subject,
        String description,String status,String resolution,Instant createdAt) {}
    public record Decision(@NotBlank String status,@NotBlank @Size(min=10,max=2000) String reason,
                            boolean unpublishTitle) {}
    private static final org.springframework.jdbc.core.RowMapper<AdminCase> MAPPER=(rs,i)->new AdminCase(
        rs.getObject("id",UUID.class),rs.getObject("user_id",UUID.class),rs.getObject("title_id",UUID.class),
        rs.getString("category"),rs.getString("subject"),rs.getString("description"),rs.getString("status"),
        rs.getString("resolution"),rs.getTimestamp("created_at").toInstant());
    @GetMapping("/cases")
    public List<AdminCase> list(@RequestParam(defaultValue="OPEN") String status){
        if(!List.of("OPEN","IN_REVIEW","RESOLVED","REJECTED").contains(status))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid filter");
        return db.query("SELECT * FROM support_cases WHERE status=? ORDER BY created_at ASC LIMIT 100",MAPPER,status);
    }
    @PostMapping("/cases/{id}/decision")
    @Transactional
    public AdminCase review(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,
                            @Valid @RequestBody Decision decision){
        UUID admin=users.resolve(jwt).getId();
        List<AdminCase> rows=db.query("SELECT * FROM support_cases WHERE id=? FOR UPDATE",MAPPER,id);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Case not found");
        AdminCase c=rows.get(0);
        if(!CasePolicy.allowed(c.status(),decision.status()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Invalid case state transition");
        if(decision.unpublishTitle() && !CasePolicy.mayUnpublish(c.category(),c.status(),decision.status(),c.titleId()!=null))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unpublish requires resolved rights complaint");
        // Only administrators can unpublish titles; moderators can review/resolve cases.
        if(decision.unpublishTitle() && (jwt.getClaim("realm_access")==null ||
                !hasAdminRole(jwt))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Administrator role required");
        String reason=decision.reason().trim();
        if(reason.length()<10)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Give a meaningful review reason");
        db.update("""
            UPDATE support_cases SET status=?,resolution=?,updated_at=NOW(),
              closed_at=CASE WHEN ? IN ('RESOLVED','REJECTED') THEN NOW() ELSE NULL END
            WHERE id=?
            """,decision.status(),reason,decision.status(),id);
        String type=switch(decision.status()) {
            case "IN_REVIEW" -> "REVIEW_STARTED";
            case "RESOLVED" -> "RESOLVED";
            default -> "REJECTED";
        };
        db.update("INSERT INTO support_case_events(id,case_id,actor_user_id,event_type,note) VALUES(?,?,?,?,?)",
          UUID.randomUUID(),id,admin,type,reason);
        if(decision.unpublishTitle()){
            db.update("UPDATE titles SET status='UNPUBLISHED',seo_index_approved=FALSE WHERE id=?",c.titleId());
            db.update("UPDATE playback_sessions SET status='REVOKED',expires_at=NOW() WHERE title_id=? AND status='ACTIVE'",c.titleId());
            db.update("INSERT INTO support_case_events(id,case_id,actor_user_id,event_type,note) VALUES(?,?,?,?,?)",
                UUID.randomUUID(),id,admin,"TITLE_UNPUBLISHED",reason);
            db.update("""
                INSERT INTO audit_logs(id,actor_user_id,action,entity_type,entity_id,detail)
                VALUES(?,?,'TITLE_UNPUBLISHED_RIGHTS_CASE','title',?,?)
                """,UUID.randomUUID(),admin,c.titleId(),"Case "+id+": "+reason.substring(0,Math.min(700,reason.length())));
        }
        return db.query("SELECT * FROM support_cases WHERE id=?",MAPPER,id).get(0);
    }
    private boolean hasAdminRole(Jwt jwt){
        Object access=jwt.getClaim("realm_access");
        if(!(access instanceof java.util.Map<?,?> map))return false;
        Object roles=map.get("roles");
        return roles instanceof java.util.List<?> list && list.contains("ADMIN");
    }
}
