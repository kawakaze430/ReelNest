package com.reelnest.admin;

import com.reelnest.users.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/v1/admin/publication-schedules")
@PreAuthorize("hasRole('ADMIN')")
public class PublicationScheduleController {
 private final JdbcTemplate db;private final CurrentUserService users;
 public PublicationScheduleController(JdbcTemplate db,CurrentUserService users){this.db=db;this.users=users;}
 public record Input(@NotNull Instant publishAt){}
 public record Schedule(UUID titleId,Instant publishAt,String state,String lastError){}
 @GetMapping
 public List<Schedule> list(){return db.query("""
    SELECT title_id,publish_at,state,last_error FROM title_publication_schedules
    ORDER BY publish_at ASC LIMIT 250
    """,(r,i)->new Schedule(r.getObject(1,UUID.class),r.getTimestamp(2).toInstant(),r.getString(3),r.getString(4)));}
 @PostMapping("/{titleId}")
 @Transactional
 public Schedule schedule(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID titleId,@Valid @RequestBody Input input){
    String state=db.query("SELECT status FROM titles WHERE id=? FOR UPDATE",rs->rs.next()?rs.getString(1):null,titleId);
    if(!PublicationSchedulePolicy.canSchedule(state,input.publishAt(),Instant.now()))
       throw new ResponseStatusException(HttpStatus.CONFLICT,"Schedule requires IN_REVIEW title and a date 1 min–1 year in future");
    // Fail before staging if rights and playable media have not been reviewed.
    if(!PublicationScheduler.eligible(db,titleId))
       throw new ResponseStatusException(HttpStatus.CONFLICT,"Approved active rights, approved creator and READY media required");
    UUID actor=users.resolve(jwt).getId();
    db.update("""
      INSERT INTO title_publication_schedules(title_id,requested_by,publish_at,state)
      VALUES(?,?,?,'SCHEDULED') ON CONFLICT(title_id) DO UPDATE SET
      requested_by=EXCLUDED.requested_by,publish_at=EXCLUDED.publish_at,
      state='SCHEDULED',last_error=NULL,updated_at=NOW()
      """,titleId,actor,java.sql.Timestamp.from(input.publishAt()));
    return new Schedule(titleId,input.publishAt(),"SCHEDULED",null);
 }
 @DeleteMapping("/{titleId}")
 @Transactional
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void cancel(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID titleId){
    int updated=db.update("""
      UPDATE title_publication_schedules SET state='CANCELLED',updated_at=NOW()
      WHERE title_id=? AND state='SCHEDULED'
      """,titleId);
    if(updated!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"No pending schedule");
    db.update("""
      INSERT INTO audit_logs(id,actor_user_id,action,entity_type,entity_id,detail)
      VALUES(?,?,'PUBLICATION_CANCELLED','title',?,NULL)
      """,UUID.randomUUID(),users.resolve(jwt).getId(),titleId);
 }
}
