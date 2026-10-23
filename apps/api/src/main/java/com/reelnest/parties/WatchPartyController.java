package com.reelnest.parties;

import com.reelnest.users.CurrentUserService;
import com.reelnest.playback.PlaybackService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** A watch party shares only playhead state. Every member separately passes PlaybackService authorization. */
@RestController
@RequestMapping("/api/v1/watch-parties")
public class WatchPartyController {
 private final JdbcTemplate db;private final PlaybackService playback;private final CurrentUserService users;
 public WatchPartyController(JdbcTemplate db,PlaybackService playback,CurrentUserService users){
   this.db=db;this.playback=playback;this.users=users;
 }
 public record Create(@NotNull UUID profileId,@NotNull UUID titleId){}
 public record Join(@NotNull UUID profileId,@NotBlank String invitation){}
 public record Sync(@NotNull @Min(0) Long expectedVersion,@NotNull @Min(0) @Max(86400) Integer positionSeconds,@NotNull Boolean paused){}
 public record Room(UUID id,UUID titleId,UUID hostUserId,long version,int positionSeconds,boolean paused,
                    Instant updatedAt,Instant expiresAt,int members){}
 public record Created(Room room,String invitation){}
 private UUID user(Jwt jwt){return users.resolve(jwt).getId();}
 private record Raw(UUID titleId,UUID hostId,long version,int position,boolean paused,Instant updated,Instant expiry,String state){}
 private Raw load(UUID id){return db.query("""
    SELECT title_id,host_user_id,version,position_seconds,paused,updated_at,expires_at,state
    FROM watch_parties WHERE id=?
    """,(r,i)->new Raw(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getLong(3),r.getInt(4),
       r.getBoolean(5),r.getTimestamp(6).toInstant(),r.getTimestamp(7).toInstant(),r.getString(8)),id)
       .stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Room not found"));}
 private void authorize(Jwt jwt,UUID profile,UUID title){
   PlaybackService.Availability check=playback.availability(jwt,profile,title);
   if(!check.playable()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Independent viewing entitlement required");
 }
 private UUID member(UUID room,UUID user){return db.query("""
   SELECT profile_id FROM watch_party_members WHERE party_id=? AND user_id=?
   """,(r,i)->r.getObject(1,UUID.class),room,user).stream().findFirst().orElseThrow(
     ()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Room not found"));}
 private Room view(UUID id,Raw r){Integer count=db.queryForObject("SELECT COUNT(*) FROM watch_party_members WHERE party_id=?",Integer.class,id);
  return new Room(id,r.titleId(),r.hostId(),r.version(),r.position(),r.paused(),r.updated(),r.expiry(),count==null?0:count);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
 public Created create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Create req){
   UUID host=user(jwt);authorize(jwt,req.profileId(),req.titleId());
   String code=PartyPolicy.invitation(),digest=PartyPolicy.digest(code);UUID id=UUID.randomUUID();
   db.update("""
     INSERT INTO watch_parties(id,host_user_id,host_profile_id,title_id,invitation_digest,expires_at)
     VALUES(?,?,?,?,?,NOW()+interval '4 hours')
     """,id,host,req.profileId(),req.titleId(),digest);
   db.update("INSERT INTO watch_party_members(party_id,user_id,profile_id) VALUES(?,?,?)",id,host,req.profileId());
   return new Created(view(id,load(id)),code);
 }
 @PostMapping("/join") @Transactional
 public Room join(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Join req){
   String digest;try{digest=PartyPolicy.digest(req.invitation());}
   catch(IllegalArgumentException ex){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Invitation unavailable");}
   List<UUID> ids=db.query("""
     SELECT id FROM watch_parties WHERE invitation_digest=? AND state='ACTIVE' AND expires_at>NOW()
     FOR UPDATE
     """,(r,i)->r.getObject(1,UUID.class),digest);
   if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Invitation unavailable");
   UUID id=ids.get(0),viewer=user(jwt);Raw room=load(id);
   authorize(jwt,req.profileId(),room.titleId());
   Integer count=db.queryForObject("SELECT COUNT(*) FROM watch_party_members WHERE party_id=?",Integer.class,id);
   Integer already=db.queryForObject("SELECT COUNT(*) FROM watch_party_members WHERE party_id=? AND user_id=?",
       Integer.class,id,viewer);
   if((already==null||already==0)&&count!=null&&count>=10)
       throw new ResponseStatusException(HttpStatus.CONFLICT,"Room full");
   db.update("""
      INSERT INTO watch_party_members(party_id,user_id,profile_id) VALUES(?,?,?)
      ON CONFLICT(party_id,user_id) DO UPDATE SET profile_id=EXCLUDED.profile_id
      """,id,viewer,req.profileId());
   return view(id,room);
 }
 @GetMapping("/{id}") @Transactional(readOnly=true)
 public Room get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
   UUID profile=member(id,user(jwt));Raw room=load(id);
   if(!PartyPolicy.active(room.state(),room.expiry(),Instant.now()))
       throw new ResponseStatusException(HttpStatus.GONE,"Room expired");
   authorize(jwt,profile,room.titleId());return view(id,room);
 }
 @PutMapping("/{id}/sync") @Transactional
 public Room sync(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Sync req){
   UUID current=user(jwt);UUID profile=member(id,current);
   Raw room=load(id);if(!room.hostId().equals(current))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only host can synchronize");
   if(!PartyPolicy.active(room.state(),room.expiry(),Instant.now()))
      throw new ResponseStatusException(HttpStatus.GONE,"Room expired");
   authorize(jwt,profile,room.titleId());
   int changed=db.update("""
     UPDATE watch_parties SET position_seconds=?,paused=?,version=version+1,updated_at=NOW()
     WHERE id=? AND version=? AND state='ACTIVE' AND expires_at>NOW()
     """,PartyPolicy.boundedPosition(req.positionSeconds()),req.paused(),id,req.expectedVersion());
   if(changed!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Room state changed; fetch latest version");
   return view(id,load(id));
 }
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
 public void leave(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
   UUID current=user(jwt);member(id,current);Raw room=load(id);
   if(room.hostId().equals(current))db.update("UPDATE watch_parties SET state='CLOSED' WHERE id=?",id);
   else db.update("DELETE FROM watch_party_members WHERE party_id=? AND user_id=?",id,current);
 }
}
