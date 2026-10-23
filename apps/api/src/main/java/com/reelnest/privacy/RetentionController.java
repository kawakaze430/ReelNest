package com.reelnest.privacy;

import com.reelnest.users.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
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

/** Retention preview and scoped legal holds. Does not erase legal billing records or backups. */
@RestController
@RequestMapping("/api/v1/admin/retention")
@PreAuthorize("hasRole('ADMIN')")
public class RetentionController {
 private final JdbcTemplate db;private final CurrentUserService users;
 public RetentionController(JdbcTemplate db,CurrentUserService users){this.db=db;this.users=users;}
 public record HoldRequest(@NotBlank @Size(min=20,max=1000) String reason,Instant expiresAt){}
 public record Hold(UUID id,UUID userId,Instant expiresAt){}
 @GetMapping("/preview")
 @Transactional(readOnly=true)
 public Map<String,Object> preview(){
   Long events=db.queryForObject("""
     SELECT COUNT(*) FROM viewer_events e WHERE e.occurred_at<NOW()-interval '180 days'
     AND NOT EXISTS(SELECT 1 FROM retention_legal_holds h
       WHERE h.user_id=e.user_id AND h.released_at IS NULL AND h.expires_at>NOW())
     """,Long.class);
   Long qoe=db.queryForObject("""
     SELECT COUNT(*) FROM playback_qoe_events e JOIN profiles p ON p.id=e.profile_id
     WHERE e.created_at<NOW()-interval '30 days'
     AND NOT EXISTS(SELECT 1 FROM retention_legal_holds h
       WHERE h.user_id=p.user_id AND h.released_at IS NULL AND h.expires_at>NOW())
     """,Long.class);
   Long holds=db.queryForObject("""
     SELECT COUNT(*) FROM retention_legal_holds WHERE released_at IS NULL AND expires_at>NOW()
     """,Long.class);
   return Map.of("expiredViewerEventsEligible",events==null?0:events,
     "expiredQoeEligible",qoe==null?0:qoe,"activeLegalHolds",holds==null?0:holds,
     "policy","Viewer events: 180 days; QoE: 30 days; holds exclude users. Manual review for billing and backups.");
 }
 @PostMapping("/holds/{userId}")
 @Transactional
 @ResponseStatus(HttpStatus.CREATED)
 public Hold hold(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID userId,@Valid @RequestBody HoldRequest input){
   if(input.expiresAt()==null || !input.expiresAt().isAfter(Instant.now().plusSeconds(60)) ||
       input.expiresAt().isAfter(Instant.now().plusSeconds(365L*24*3600)))
       throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Hold expiry must be within one year");
   Integer exists=db.queryForObject("SELECT COUNT(*) FROM users WHERE id=?",Integer.class,userId);
   if(exists==null||exists==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found");
   UUID id=UUID.randomUUID();UUID actor=users.resolve(jwt).getId();
   db.update("""
     INSERT INTO retention_legal_holds(id,user_id,created_by,reason,expires_at)
     VALUES(?,?,?,?,?)
     """,id,userId,actor,input.reason(),java.sql.Timestamp.from(input.expiresAt()));
   db.update("""
     INSERT INTO audit_logs(id,actor_user_id,action,entity_type,entity_id,detail)
     VALUES(?,?,'LEGAL_HOLD_CREATED','retention_hold',?,?)
     """,UUID.randomUUID(),actor,id,"Expiry: "+input.expiresAt());
   return new Hold(id,userId,input.expiresAt());
 }
 @DeleteMapping("/holds/{id}")
 @Transactional
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void release(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
    int changed=db.update("""
      UPDATE retention_legal_holds SET released_at=NOW()
      WHERE id=? AND released_at IS NULL
      """,id);
    if(changed!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Active hold not found");
    db.update("""
       INSERT INTO audit_logs(id,actor_user_id,action,entity_type,entity_id,detail)
       VALUES(?,?,'LEGAL_HOLD_RELEASED','retention_hold',?,NULL)
       """,UUID.randomUUID(),users.resolve(jwt).getId(),id);
 }
}
