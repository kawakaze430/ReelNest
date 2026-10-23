package com.reelnest.catalog;

import com.reelnest.users.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

/** Editorial aliases for regional scripts/transliteration. Public results still require active rights and ready media. */
@RestController
public class TitleAliasController {
 private final JdbcTemplate db;private final CurrentUserService users;private final String trustedCountry;
 public TitleAliasController(JdbcTemplate db,CurrentUserService users,
     @org.springframework.beans.factory.annotation.Value("${app.playback.trusted-country:IN}") String trustedCountry){
     this.db=db;this.users=users;this.trustedCountry=trustedCountry;
 }
 public record AliasInput(@NotBlank @Pattern(regexp="[a-zA-Z]{2,3}(-[a-zA-Z]{2,4})?") String language,
                           @NotBlank @Size(min=2,max=160) String alias){}
 public record Suggestion(UUID titleId,String name,String language,String slug){}
 @PostMapping("/api/v1/admin/titles/{titleId}/aliases")
 @PreAuthorize("hasRole('ADMIN')")
 @Transactional
 @ResponseStatus(HttpStatus.CREATED)
 public void add(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID titleId,@Valid @RequestBody AliasInput input){
    Integer n=db.queryForObject("SELECT COUNT(*) FROM titles WHERE id=?",Integer.class,titleId);
    if(n==null||n==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Title not found");
    String alias=input.alias().strip();
    if(alias.chars().anyMatch(Character::isISOControl))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid alias");
    db.update("""
      INSERT INTO title_search_aliases(title_id,language,alias) VALUES(?,?,?)
      ON CONFLICT DO NOTHING
      """,titleId,input.language().toLowerCase(java.util.Locale.ROOT),alias);
    db.update("""
      INSERT INTO audit_logs(id,actor_user_id,action,entity_type,entity_id,detail)
      VALUES(?,?,'SEARCH_ALIAS_ADDED','title',?,?)
      """,UUID.randomUUID(),users.resolve(jwt).getId(),titleId,input.language());
 }
 @GetMapping("/api/v1/catalog/suggest")
 public List<Suggestion> suggest(@RequestParam String q,@RequestParam(defaultValue="8") int limit){
    if(q==null||q.strip().length()<2||q.strip().length()>80||limit<1||limit>15)
       throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid query or limit");
    String literal=SearchTextPolicy.likeLiteral(q.strip());
    return db.query("""
      SELECT DISTINCT t.id,t.name,t.language,t.slug FROM titles t
      WHERE t.status='PUBLISHED' AND (
        t.name ILIKE ? ESCAPE '!' OR t.slug ILIKE ? ESCAPE '!'
        OR EXISTS(SELECT 1 FROM title_search_aliases a WHERE a.title_id=t.id AND a.alias ILIKE ? ESCAPE '!'))
      AND EXISTS(SELECT 1 FROM creators c WHERE c.id=t.creator_id AND c.status='APPROVED')
      AND EXISTS(SELECT 1 FROM content_rights r WHERE r.title_id=t.id AND r.status='APPROVED'
        AND r.valid_from<=CURRENT_DATE AND r.valid_until>=CURRENT_DATE
        AND EXISTS(SELECT 1 FROM rights_territories rt WHERE rt.rights_id=r.id AND rt.country_code=?))
      AND EXISTS(SELECT 1 FROM media_assets m WHERE m.title_id=t.id
        AND m.processing_status='READY' AND m.manifest_key IS NOT NULL)
      ORDER BY t.name ASC LIMIT ?
      """,(r,i)->new Suggestion(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getString(4)),
        literal,literal,literal,trustedCountry,limit);
 }
}
