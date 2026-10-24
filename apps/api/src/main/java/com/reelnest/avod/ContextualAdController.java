package com.reelnest.avod;

import com.reelnest.billing.BillingService;
import com.reelnest.playback.PlaybackService;
import com.reelnest.users.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Preview-only ad decision engine. Not an ad insertion service, no tracking pixels,
 * third-party requests, royalties, invoice, monetization, cookies or paid inventory.
 */
@RestController
@RequestMapping("/api/v1/avod")
public class ContextualAdController {
  private final JdbcTemplate db;
  private final CurrentUserService users;
  private final PlaybackService playback;
  private final BillingService billing;
  private final String country;
  public ContextualAdController(JdbcTemplate db,CurrentUserService users,
      PlaybackService playback,BillingService billing,
      @Value("${app.playback.trusted-country:IN}") String country) {
    this.db=db;this.users=users;this.playback=playback;this.billing=billing;
    if(!country.matches("[A-Z]{2}"))throw new IllegalArgumentException("Invalid ad-preview territory");
    this.country=country;
  }
  public record Preferences(boolean contextualPreviewsOptIn,boolean anonymousMeasurementOptIn) {}
  public record PreferencesUpdate(@NotNull Boolean contextualPreviewsOptIn,
                                  @NotNull Boolean anonymousMeasurementOptIn) {}
  public record Creative(UUID id,String advertiser,String message) {}
  public record Proposal(@Size(max=100) String advertiser,@Size(max=180) String message,
      @NotNull Instant startsAt,@NotNull Instant endsAt,String genre,String countryCode) {}
  public record Impression(@NotNull UUID profileId,@NotNull UUID titleId,
                           @NotNull UUID creativeId,@NotNull UUID eventId) {}
  public record Receipt(boolean nonBillable,boolean duplicate,String note) {}

  private UUID user(Jwt jwt){return users.resolve(jwt).getId();}
  private record OwnedProfile(boolean child,boolean consent,boolean measurement) {}
  private OwnedProfile profile(UUID user,UUID profileId){
    return db.query("""
      SELECT p.is_child,COALESCE(a.contextual_previews_opt_in,FALSE),
             COALESCE(a.anonymous_measurement_opt_in,FALSE)
      FROM profiles p LEFT JOIN avod_profile_preferences a ON a.profile_id=p.id
      WHERE p.id=? AND p.user_id=?
      """,(rs,i)->new OwnedProfile(rs.getBoolean(1),rs.getBoolean(2),rs.getBoolean(3)),profileId,user)
      .stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Profile not found"));
  }
  @GetMapping("/preferences")
  @Transactional(readOnly=true)
  public ResponseEntity<Preferences> preferences(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID profileId){
    OwnedProfile p=profile(user(jwt),profileId);
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(new Preferences(!p.child()&&p.consent(),!p.child()&&p.measurement()));
  }
  @PutMapping("/preferences")
  @Transactional
  public ResponseEntity<Preferences> update(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID profileId,
                                            @Valid @RequestBody PreferencesUpdate update) {
    UUID owner=user(jwt);
    OwnedProfile p=profile(owner,profileId);
    if(p.child() && (update.contextualPreviewsOptIn()||update.anonymousMeasurementOptIn()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Ads are unavailable for child profiles");
    boolean consent=!p.child()&&update.contextualPreviewsOptIn();
    boolean measure=consent&&update.anonymousMeasurementOptIn();
    db.update("""
      INSERT INTO avod_profile_preferences(profile_id,contextual_previews_opt_in,
                                           anonymous_measurement_opt_in)
      VALUES(?,?,?)
      ON CONFLICT(profile_id) DO UPDATE SET
        contextual_previews_opt_in=EXCLUDED.contextual_previews_opt_in,
        anonymous_measurement_opt_in=EXCLUDED.anonymous_measurement_opt_in,
        updated_at=NOW()
      """,profileId,consent,measure);
    if(!measure)db.update("DELETE FROM avod_preview_events WHERE profile_id=?",profileId);
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Preferences(consent,measure));
  }
  private record Eligible(String genre,boolean free) {}
  private Eligible eligible(Jwt jwt,UUID profileId,UUID titleId,UUID owner){
    OwnedProfile p=profile(owner,profileId);
    var right=playback.availability(jwt,profileId,titleId);
    if(!right.playable() || p.child() || !p.consent() || billing.hasPremium(owner))
      return null;
    List<Eligible> titles=db.query("""
      SELECT t.genre,t.access_tier='FREE' FROM titles t WHERE t.id=? AND t.status='PUBLISHED'
      """,(rs,i)->new Eligible(rs.getString(1),rs.getBoolean(2)),titleId);
    if(titles.isEmpty()||!titles.getFirst().free())return null;
    return titles.getFirst();
  }
  private List<Creative> creatives(String genre){
    return db.query("""
      SELECT id,advertiser,message FROM avod_creatives
      WHERE status='REVIEWED' AND reviewed_by IS NOT NULL
        AND starts_at<=NOW() AND ends_at>NOW()
        AND country_code=? AND (genre IS NULL OR lower(genre)=lower(?))
      ORDER BY created_at,id LIMIT 1
      """,(rs,i)->new Creative(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3)),
      country,genre);
  }
  @GetMapping("/preview")
  @Transactional(readOnly=true)
  public ResponseEntity<List<Creative>> preview(@AuthenticationPrincipal Jwt jwt,
                                          @RequestParam UUID profileId,@RequestParam UUID titleId){
    UUID owner=user(jwt);
    Eligible info=eligible(jwt,profileId,titleId,owner);
    List<Creative> preview=info==null?List.of():creatives(info.genre());
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(preview);
  }
  public record AccessOptions(boolean currentlyPlayable,boolean currentPremiumSubscription,
       boolean contextualSponsorPreviewAvailable,boolean rentalPurchasesAvailable,
       boolean actualAdInsertionAvailable,String notice) {}
  @GetMapping("/access-options")
  @Transactional(readOnly=true)
  public ResponseEntity<AccessOptions> accessOptions(@AuthenticationPrincipal Jwt jwt,
                                        @RequestParam UUID profileId,@RequestParam UUID titleId){
    UUID owner=user(jwt);
    boolean premium=billing.hasPremium(owner);
    boolean canPlay=playback.availability(jwt,profileId,titleId).playable();
    Eligible info=eligible(jwt,profileId,titleId,owner);
    boolean adPreview=info!=null&&!creatives(info.genre()).isEmpty();
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
      new AccessOptions(canPlay,premium,adPreview,false,false,
        "No TVOD checkout or advertising insertion is enabled. Playback always uses verified entitlements."));
  }

  @PostMapping("/preview-events")
  @Transactional
  public ResponseEntity<Receipt> observe(@AuthenticationPrincipal Jwt jwt,
                                   @Valid @RequestBody Impression request){
    UUID owner=user(jwt);
    OwnedProfile p=profile(owner,request.profileId());
    Eligible info=eligible(jwt,request.profileId(),request.titleId(),owner);
    if(info==null||!ContextualAdPolicy.measureAllowed(p.consent(),p.child(),
       p.measurement(),true))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,"No measurement consent or current permission");
    if(creatives(info.genre()).stream().noneMatch(c->c.id().equals(request.creativeId())))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,"No eligible preview creative");
    int inserted=db.update("""
      INSERT INTO avod_preview_events(id,profile_id,creative_id)
      VALUES(?,?,?) ON CONFLICT DO NOTHING
      """,request.eventId(),request.profileId(),request.creativeId());
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
      .body(new Receipt(true,inserted==0,
        "Untrusted client preview signal. Never bill advertisers or infer a payable impression."));
  }

  private void audit(UUID actor,String action,UUID id){
    db.update("""
       INSERT INTO audit_logs(id,actor_user_id,action,entity_type,entity_id,detail)
       VALUES(?,?,?,'avod_preview_creative',?,NULL)
       """,UUID.randomUUID(),actor,action,id);
  }

  @PostMapping("/admin/creatives")
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public UUID propose(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Proposal input){
    if(!ContextualAdPolicy.safeText(input.advertiser(),100)
       ||!ContextualAdPolicy.safeText(input.message(),180)
       ||input.countryCode()==null || !input.countryCode().matches("[A-Z]{2}")
       ||input.startsAt()==null || input.endsAt()==null
       ||!input.endsAt().isAfter(input.startsAt())
       ||input.endsAt().isAfter(input.startsAt().plusSeconds(90L*86400))
       ||input.genre()!=null && !ContextualAdPolicy.safeText(input.genre(),80))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid ad preview proposal");
    UUID id=UUID.randomUUID();
    db.update("""
      INSERT INTO avod_creatives(id,advertiser,message,genre,country_code,
                                 starts_at,ends_at,submitted_by)
      VALUES(?,?,?,?,?,?,?,?)
      """,id,input.advertiser().strip(),input.message().strip(),input.genre(),input.countryCode(),
      input.startsAt(),input.endsAt(),user(jwt));
    audit(user(jwt),"AVOD_DRAFT_CREATED",id);
    return id;
  }
  @PostMapping("/admin/creatives/{id}/review")
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public void review(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
    UUID actor=user(jwt);
    int count=db.update("""
      UPDATE avod_creatives SET status='REVIEWED',reviewed_by=?
      WHERE id=? AND status='DRAFT' AND submitted_by<>? AND ends_at>NOW()
      """,actor,id,actor);
    if(count!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,
       "Second administrator required; review only unexpired draft previews");
    audit(actor,"AVOD_REVIEWED",id);
  }
  @PostMapping("/admin/creatives/{id}/archive")
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public void archive(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
    if(db.update("UPDATE avod_creatives SET status='ARCHIVED' WHERE id=? AND status<>'ARCHIVED'",id)!=1)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Creative not active");
    audit(user(jwt),"AVOD_ARCHIVED",id);
  }
}
