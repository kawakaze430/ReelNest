package com.reelnest.support;

import com.reelnest.users.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

/** Authenticated case management. Case descriptions are private and never publicly indexed. */
@RestController
@RequestMapping("/api/v1/support")
public class SupportCaseController {
    private final JdbcTemplate db;
    private final CurrentUserService users;
    public SupportCaseController(JdbcTemplate db, CurrentUserService users) {
        this.db=db; this.users=users;
    }
    public record CaseView(UUID id,UUID titleId,UUID parentCaseId,String category,
         String subject,String description,String status,String resolution,Instant createdAt,Instant updatedAt) {}
    public record NewCase(@NotBlank @Size(max=160) String subject,
       @NotBlank @Size(min=15,max=4000) String description,
       @NotBlank String category,UUID titleId) {}
    public record Appeal(@NotBlank @Size(min=15,max=4000) String reason) {}
    public record CaseEvent(String eventType,String note,Instant createdAt) {}

    private static final org.springframework.jdbc.core.RowMapper<CaseView> MAPPER=(rs,i)->new CaseView(
      rs.getObject("id",UUID.class),rs.getObject("title_id",UUID.class),
      rs.getObject("parent_case_id",UUID.class),rs.getString("category"),rs.getString("subject"),
      rs.getString("description"),rs.getString("status"),rs.getString("resolution"),
      rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("updated_at").toInstant());
    private UUID owner(Jwt jwt){return users.resolve(jwt).getId();}
    private static String safe(String s){return s==null?"":s.trim();}
    private static boolean normalCategory(String c) {
        return List.of("TECHNICAL","BILLING","ACCOUNT","CREATOR","RIGHTS_COMPLAINT","OTHER").contains(c);
    }
    private CaseView owned(UUID user,UUID id) {
        return db.query("SELECT * FROM support_cases WHERE id=? AND user_id=?",MAPPER,id,user).stream()
           .findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Support case not found"));
    }
    private static void event(JdbcTemplate db,UUID id,UUID actor,String type,String note){
        db.update("INSERT INTO support_case_events(id,case_id,actor_user_id,event_type,note) VALUES(?,?,?,?,?)",
          UUID.randomUUID(),id,actor,type,note);
    }
    @GetMapping("/cases")
    public List<CaseView> mine(@AuthenticationPrincipal Jwt jwt){
        return db.query("SELECT * FROM support_cases WHERE user_id=? ORDER BY created_at DESC LIMIT 100",MAPPER,owner(jwt));
    }
    @GetMapping("/cases/{id}")
    public CaseView detail(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return owned(owner(jwt),id);}
    @GetMapping("/cases/{id}/events")
    public List<CaseEvent> history(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
        owned(owner(jwt),id);
        return db.query("SELECT event_type,note,created_at FROM support_case_events WHERE case_id=? ORDER BY created_at,id",
           (rs,i)->new CaseEvent(rs.getString(1),rs.getString(2),rs.getTimestamp(3).toInstant()),id);
    }
    @PostMapping("/cases")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CaseView create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody NewCase input){
        UUID user=owner(jwt);
        if(input.subject().trim().isEmpty() || input.description().trim().length()<15)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A meaningful description is required");
        if(!normalCategory(input.category()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported case category");
        if("RIGHTS_COMPLAINT".equals(input.category()) && input.titleId()==null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Select the title for a rights complaint");
        if(input.titleId()!=null){
            Integer exists=db.queryForObject("SELECT COUNT(*) FROM titles WHERE id=?",Integer.class,input.titleId());
            if(exists==null || exists==0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown title");
        }
        // The user row serializes quota admissions across concurrent POST requests.
        db.queryForList("SELECT id FROM users WHERE id=? FOR UPDATE",UUID.class,user);
        Integer daily=db.queryForObject("SELECT COUNT(*) FROM support_cases WHERE user_id=? AND created_at>=NOW()-interval '24 hours'",
            Integer.class,user);
        if(daily!=null && daily>=5)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Daily case limit reached");
        UUID id=UUID.randomUUID();
        db.update("""
            INSERT INTO support_cases(id,user_id,title_id,category,subject,description)
            VALUES(?,?,?,?,?,?)
            """,id,user,input.titleId(),input.category(),safe(input.subject()),safe(input.description()));
        event(db,id,user,"CREATED",null);
        return owned(user,id);
    }
    @PostMapping("/cases/{id}/appeal")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CaseView appeal(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Appeal input){
        UUID user=owner(jwt);
        if(input.reason().trim().length()<15)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Appeal reason is too short");
        db.queryForList("SELECT id FROM users WHERE id=? FOR UPDATE",UUID.class,user);
        CaseView parent=owned(user,id);
        Integer n=db.queryForObject("SELECT COUNT(*) FROM support_cases WHERE parent_case_id=?",Integer.class,id);
        if(!CasePolicy.canAppeal(parent.category(),parent.status(),true,n!=null && n>0))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Case cannot be appealed");
        UUID appeal=UUID.randomUUID();
        db.update("""
            INSERT INTO support_cases(id,user_id,title_id,parent_case_id,category,subject,description)
            VALUES(?,?,?,?,'APPEAL',?,?)
            """,appeal,user,parent.titleId(),id,"Appeal: "+parent.subject().substring(0,Math.min(150,parent.subject().length())),safe(input.reason()));
        event(db,appeal,user,"CREATED","Appeal of case "+id);
        return owned(user,appeal);
    }
}
