package com.reelnest.profiles;

import com.reelnest.users.CurrentUserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Independent viewer control: personalization OFF by default; no hidden opt-in. */
@RestController
@RequestMapping("/api/v1/profiles/{profileId}/personalization")
public class PersonalizationController {
 private final JdbcTemplate db;private final CurrentUserService users;
 public PersonalizationController(JdbcTemplate db,CurrentUserService users){this.db=db;this.users=users;}
 public record Preference(boolean enabled){}
 @GetMapping
 @Transactional(readOnly=true)
 public Preference get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID profileId){
  UUID user=users.resolve(jwt).getId();
  return db.query("SELECT personalization_enabled FROM profiles WHERE id=? AND user_id=?",
    rs->{if(!rs.next())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Profile not found");
      return new Preference(rs.getBoolean(1));},profileId,user);
 }
 @PutMapping
 @Transactional
 public Preference update(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID profileId,@Valid @RequestBody Preference request){
  UUID user=users.resolve(jwt).getId();
  int n=db.update("""
    UPDATE profiles SET personalization_enabled=?,personalization_updated_at=NOW()
    WHERE id=? AND user_id=?
    """,request.enabled(),profileId,user);
  if(n!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Profile not found");
  // Clear stored personalization interactions if consent is withdrawn; progress/watchlist remain as explicit user features.
  if(!request.enabled()) db.update("DELETE FROM viewer_events WHERE profile_id=?",profileId);
  return request;
 }
}
