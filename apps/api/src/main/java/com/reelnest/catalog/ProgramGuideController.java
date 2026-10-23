package com.reelnest.catalog;

import com.reelnest.users.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Guide only: a program never returns a signed movie URL. Use playback sessions for viewing. */
@RestController
public class ProgramGuideController {
 private final JdbcTemplate db;private final CurrentUserService users;private final String country;
 public ProgramGuideController(JdbcTemplate db,CurrentUserService users,
    @Value("${app.playback.trusted-country:IN}") String country){this.db=db;this.users=users;
    if(!country.matches("[A-Z]{2}"))throw new IllegalArgumentException("Invalid catalog territory");this.country=country;}
 public record Channel(UUID id,String slug,String name,String description,String status) {}
 public record Program(UUID id,UUID titleId,String title,Instant startsAt,Instant endsAt){}
 public record Guide(Channel channel,List<Program> programs){}
 public record Create(@NotBlank @Size(max=90) String slug,@NotBlank @Size(max=120) String name,
                      @Size(max=500) String description){}
 public record Entry(@NotNull UUID titleId,@NotNull Instant startsAt,@NotNull Instant endsAt){}
 public record State(@NotBlank @Pattern(regexp="DRAFT|PUBLISHED|ARCHIVED") String status){}
 private List<Channel> channels(boolean admin){return db.query("""
   SELECT id,slug,name,description,status FROM programmed_channels
   """+(admin?"":" WHERE status='PUBLISHED'")+" ORDER BY name LIMIT 100",
   (r,i)->new Channel(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getString(4),r.getString(5)));
 }
 private Guide guide(Channel c,Instant from,Instant until,boolean admin){
   return new Guide(c,db.query("""
     SELECT p.id,t.id,t.name,p.starts_at,p.ends_at FROM channel_programs p
     JOIN titles t ON t.id=p.title_id
     WHERE p.channel_id=? AND p.ends_at>? AND p.starts_at<?
       AND (? OR (t.status='PUBLISHED' AND t.creator_id IS NOT NULL
         AND EXISTS(SELECT 1 FROM creators creator WHERE creator.id=t.creator_id AND creator.status='APPROVED')
         AND EXISTS(SELECT 1 FROM content_rights r JOIN rights_territories rt ON rt.rights_id=r.id
           WHERE r.title_id=t.id AND rt.country_code=? AND r.status='APPROVED' AND r.valid_from<=CURRENT_DATE AND r.valid_until>=CURRENT_DATE)
         AND EXISTS(SELECT 1 FROM media_assets a WHERE a.title_id=t.id AND a.processing_status='READY' AND a.manifest_key IS NOT NULL)))
     ORDER BY p.starts_at LIMIT 250
     """,(r,i)->new Program(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getString(3),
       r.getTimestamp(4).toInstant(),r.getTimestamp(5).toInstant()),c.id(),from,until,admin,country));
 }
 @GetMapping("/api/v1/catalog/channels")
 public List<Guide> publicGuide(@RequestParam(required=false) Instant from){
   Instant start=from==null?Instant.now():from;
   if(start.isBefore(Instant.now().minusSeconds(3600))||start.isAfter(Instant.now().plusSeconds(30*86400)))
     throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Guide start outside permitted window");
   return channels(false).stream().map(c->guide(c,start,start.plusSeconds(48*3600),false)).toList();
 }
 @GetMapping("/api/v1/admin/channels") @PreAuthorize("hasRole('ADMIN')")
 public List<Guide> adminGuide(){Instant start=Instant.now().minusSeconds(86400);
   return channels(true).stream().map(c->guide(c,start,start.plusSeconds(15*86400),true)).toList();}
 @PostMapping("/api/v1/admin/channels") @ResponseStatus(HttpStatus.CREATED)
 @PreAuthorize("hasRole('ADMIN')") @Transactional
 public UUID create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Create req){
   if(!EditorialPolicy.validSlug(req.slug()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid channel slug");
   UUID id=UUID.randomUUID();db.update("""
     INSERT INTO programmed_channels(id,slug,name,description,created_by) VALUES(?,?,?,?,?)
     """,id,req.slug(),req.name().trim(),req.description()==null?"":req.description().trim(),users.resolve(jwt).getId());
   return id;
 }
 @PostMapping("/api/v1/admin/channels/{id}/programs") @ResponseStatus(HttpStatus.CREATED)
 @PreAuthorize("hasRole('ADMIN')") @Transactional
 public UUID schedule(@PathVariable UUID id,@Valid @RequestBody Entry entry){
   if(!ChannelSchedulePolicy.validWindow(entry.startsAt(),entry.endsAt()))
     throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Program must be 1 second to 24 hours");
   String state=db.query("SELECT status FROM programmed_channels WHERE id=? FOR UPDATE",rs->rs.next()?rs.getString(1):null,id);
   if(!"DRAFT".equals(state))throw new ResponseStatusException(HttpStatus.CONFLICT,"Only draft channels are editable");
   Integer titles=db.queryForObject("SELECT COUNT(*) FROM titles WHERE id=? AND status='PUBLISHED'",Integer.class,entry.titleId());
   if(titles==null||titles==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Published film required");
   Integer overlaps=db.queryForObject("""
     SELECT COUNT(*) FROM channel_programs WHERE channel_id=? AND starts_at<? AND ends_at>?
     """,Integer.class,id,entry.endsAt(),entry.startsAt());
   if(overlaps!=null&&overlaps>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Program overlaps existing schedule");
   UUID program=UUID.randomUUID();db.update("""
     INSERT INTO channel_programs(id,channel_id,title_id,starts_at,ends_at) VALUES(?,?,?,?,?)
     """,program,id,entry.titleId(),entry.startsAt(),entry.endsAt());return program;
 }
 @PutMapping("/api/v1/admin/channels/{id}/state") @PreAuthorize("hasRole('ADMIN')") @Transactional
 public void change(@PathVariable UUID id,@Valid @RequestBody State req){
   String previous=db.query("SELECT status FROM programmed_channels WHERE id=? FOR UPDATE",rs->rs.next()?rs.getString(1):null,id);
   if(previous==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Channel not found");
   if("ARCHIVED".equals(previous)||"PUBLISHED".equals(req.status())&&!"DRAFT".equals(previous))
     throw new ResponseStatusException(HttpStatus.CONFLICT,"Invalid channel transition");
   if("PUBLISHED".equals(req.status())){
     Integer count=db.queryForObject("SELECT COUNT(*) FROM channel_programs WHERE channel_id=?",Integer.class,id);
     if(count==null||count==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Channel requires scheduled content");
   }
   db.update("UPDATE programmed_channels SET status=? WHERE id=?",req.status(),id);
 }
}
