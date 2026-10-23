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

/** Editorial rails contain catalog cards only; video starts always require PlaybackService. */
@RestController
public class EditorialCollectionsController {
 private final JdbcTemplate db;
 private final CurrentUserService users;
 private final String country;
 public EditorialCollectionsController(JdbcTemplate db,CurrentUserService users,
     @Value("${app.playback.trusted-country:IN}") String country){this.db=db;this.users=users;
     if(!country.matches("[A-Z]{2}"))throw new IllegalArgumentException("Invalid catalog territory");this.country=country;}
 public record Collection(UUID id,String slug,String heading,String description,String language,int position,
                          String status,Instant startsAt,Instant endsAt,List<TitleResponse> titles) {}
 public record Create(@NotBlank @Size(max=90) String slug,@NotBlank @Size(max=100) String heading,
   @Size(max=500) String description,@Pattern(regexp="[a-z]{2}(-[A-Z]{2})?") String language,
   @Min(0) @Max(10000) int position,Instant startsAt,Instant endsAt){}
 public record Add(@NotNull UUID titleId,@Min(0) @Max(10000) int position){}
 public record Change(@NotBlank @Pattern(regexp="DRAFT|PUBLISHED|ARCHIVED") String status){}
 private final org.springframework.jdbc.core.RowMapper<Collection> mapper=(rs,i)->
   new Collection(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),
   rs.getInt(6),rs.getString(7),rs.getTimestamp(8)==null?null:rs.getTimestamp(8).toInstant(),
   rs.getTimestamp(9)==null?null:rs.getTimestamp(9).toInstant(),List.of());
 private List<Collection> list(boolean admin){
   String q="""
     SELECT id,slug,heading,description,language,sort_position,status,starts_at,ends_at
     FROM editorial_collections
     """+(admin?"": " WHERE status='PUBLISHED' AND (starts_at IS NULL OR starts_at<=NOW()) AND (ends_at IS NULL OR ends_at>NOW())")+
     " ORDER BY sort_position,heading LIMIT 100";
   return db.query(q,mapper).stream().map(c->new Collection(c.id(),c.slug(),c.heading(),c.description(),c.language(),
     c.position(),c.status(),c.startsAt(),c.endsAt(),cards(c.id(),admin))).toList();
 }
 private List<TitleResponse> cards(UUID collection,boolean admin){
   // Single query, bounded results. The public rail excludes unpublished, unlicensed and unprocessed media.
   // This endpoint never returns a media URL or grants playback entitlements.
   return db.query("""
     SELECT t.id,t.slug,t.name,t.synopsis,t.type,t.genre,t.language,t.age_rating,
            t.runtime_minutes,t.release_year,t.art_tone,t.featured,t.access_tier
     FROM editorial_collection_items i JOIN titles t ON t.id=i.title_id
     WHERE i.collection_id=? AND (? OR (t.status='PUBLISHED'
       AND t.creator_id IS NOT NULL
       AND EXISTS (SELECT 1 FROM creators c WHERE c.id=t.creator_id AND c.status='APPROVED')
       AND EXISTS (SELECT 1 FROM content_rights r JOIN rights_territories rt ON rt.rights_id=r.id
          WHERE r.title_id=t.id AND rt.country_code=? AND r.status='APPROVED' AND r.valid_from<=CURRENT_DATE
          AND r.valid_until>=CURRENT_DATE)
       AND EXISTS (SELECT 1 FROM media_assets m WHERE m.title_id=t.id
           AND m.processing_status='READY' AND m.manifest_key IS NOT NULL)))
     ORDER BY i.sort_position,t.name LIMIT 50
     """,(r,i)->new TitleResponse(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getString(4),
       r.getString(5),r.getString(6),r.getString(7),r.getString(8),(Integer)r.getObject(9),
       (Integer)r.getObject(10),r.getString(11),r.getBoolean(12),r.getString(13)),collection,admin,country);
 }
 @GetMapping("/api/v1/catalog/collections")
 public List<Collection> published(){return list(false);}
 @GetMapping("/api/v1/admin/collections") @PreAuthorize("hasRole('ADMIN')")
 public List<Collection> all(){return list(true);}
 @PostMapping("/api/v1/admin/collections") @PreAuthorize("hasRole('ADMIN')")
 @ResponseStatus(HttpStatus.CREATED) @Transactional
 public UUID create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Create request){
   if(!EditorialPolicy.validSlug(request.slug()) || !EditorialPolicy.validDates(request.startsAt(),request.endsAt()))
     throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid slug or date window");
   UUID id=UUID.randomUUID();UUID actor=users.resolve(jwt).getId();
   db.update("""
     INSERT INTO editorial_collections(id,slug,heading,description,language,sort_position,starts_at,ends_at,created_by)
     VALUES(?,?,?,?,?,?,?,?,?)
     """,id,request.slug(),request.heading().trim(),request.description()==null?"":request.description().trim(),
     request.language(),request.position(),request.startsAt(),request.endsAt(),actor);
   audit(actor,"COLLECTION_CREATED",id);return id;
 }
 @PutMapping("/api/v1/admin/collections/{id}/items") @PreAuthorize("hasRole('ADMIN')")
 @Transactional
 public void add(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Add item){
   requireDraft(id);
   Integer available=db.queryForObject("SELECT COUNT(*) FROM titles WHERE id=? AND status='PUBLISHED'",Integer.class,item.titleId());
   if(available==null || available==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Only published titles can be curated");
   db.update("""
     INSERT INTO editorial_collection_items(collection_id,title_id,sort_position) VALUES(?,?,?)
     ON CONFLICT(collection_id,title_id) DO UPDATE SET sort_position=EXCLUDED.sort_position
     """,id,item.titleId(),item.position());
   audit(users.resolve(jwt).getId(),"COLLECTION_ITEM_CHANGED",id);
 }
 @DeleteMapping("/api/v1/admin/collections/{id}/items/{titleId}") @PreAuthorize("hasRole('ADMIN')")
 @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
 public void remove(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@PathVariable UUID titleId){
   requireDraft(id);db.update("DELETE FROM editorial_collection_items WHERE collection_id=? AND title_id=?",id,titleId);
   audit(users.resolve(jwt).getId(),"COLLECTION_ITEM_REMOVED",id);
 }
 @PutMapping("/api/v1/admin/collections/{id}/state") @PreAuthorize("hasRole('ADMIN')")
 @Transactional
 public void change(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Change requested){
   String current=db.query("SELECT status FROM editorial_collections WHERE id=? FOR UPDATE",rs->rs.next()?rs.getString(1):null,id);
   if(current==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Collection not found");
   if("ARCHIVED".equals(current) || "PUBLISHED".equals(requested.status()) && !"DRAFT".equals(current))
      throw new ResponseStatusException(HttpStatus.CONFLICT,"Only drafts can be published; archived rails cannot be restored");
   if("PUBLISHED".equals(requested.status())){
     Integer count=db.queryForObject("SELECT COUNT(*) FROM editorial_collection_items WHERE collection_id=?",Integer.class,id);
     if(count==null || count<1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Collection must contain a published title");
   }
   db.update("UPDATE editorial_collections SET status=?,updated_at=NOW() WHERE id=?",requested.status(),id);
   audit(users.resolve(jwt).getId(),"COLLECTION_"+requested.status(),id);
 }
 private void requireDraft(UUID id){
   String state=db.query("SELECT status FROM editorial_collections WHERE id=? FOR UPDATE",rs->rs.next()?rs.getString(1):null,id);
   if(state==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Collection not found");
   if(!"DRAFT".equals(state))throw new ResponseStatusException(HttpStatus.CONFLICT,"Edit only drafts; archive and recreate published rails");
 }
 private void audit(UUID actor,String action,UUID id){db.update("""
   INSERT INTO audit_logs(id,actor_user_id,action,entity_type,entity_id,detail)
   VALUES(?,?,?,'editorial_collection',?,NULL)
   """,UUID.randomUUID(),actor,action,id);
 }
}
