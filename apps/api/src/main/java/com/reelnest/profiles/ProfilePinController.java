package com.reelnest.profiles;

import com.reelnest.users.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Server-side additional PIN challenge for protected adult-profile management.
 * Does not yet gate playback: that requires integrating grants into ALL profile-scoped APIs.
 */
@RestController
@RequestMapping("/api/v1/profiles")
public class ProfilePinController {
    private final JdbcTemplate db;
    private final CurrentUserService users;
    public ProfilePinController(JdbcTemplate db,CurrentUserService users){this.db=db;this.users=users;}
    public record PinInput(@NotBlank String pin) {}
    public record ChangePin(@NotBlank String newPin,String currentPin) {}
    public record PinStatus(boolean configured,boolean locked) {}
    private UUID owner(Jwt jwt){return users.resolve(jwt).getId();}
    private record PinRow(boolean child,byte[] salt,byte[] hash,int failures,Instant lockedUntil) {}
    private PinRow get(UUID user,UUID id){
        List<PinRow> rows=db.query("""
          SELECT is_child,pin_salt,pin_hash,pin_failed_attempts,pin_locked_until FROM profiles
          WHERE id=? AND user_id=? FOR UPDATE
          """,(r,i)->new PinRow(r.getBoolean(1),r.getBytes(2),r.getBytes(3),r.getInt(4),
          r.getTimestamp(5)==null?null:r.getTimestamp(5).toInstant()),id,user);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Profile not found");
        return rows.get(0);
    }
    @GetMapping("/{profileId}/pin")
    @Transactional(readOnly=true)
    public PinStatus status(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID profileId){
        UUID user=owner(jwt);
        return db.query("SELECT pin_hash,pin_locked_until FROM profiles WHERE id=? AND user_id=?",
          rs->{if(!rs.next())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Profile not found");
            var t=rs.getTimestamp(2);return new PinStatus(rs.getBytes(1)!=null,t!=null && t.toInstant().isAfter(Instant.now()));},profileId,user);
    }
    @PutMapping("/{profileId}/pin")
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public PinStatus change(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID profileId,
                             @Valid @RequestBody ChangePin input){
        UUID user=owner(jwt);PinRow row=get(user,profileId);
        if(row.child())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Child profiles cannot have adult PINs");
        if(!ProfilePinPolicy.valid(input.newPin()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use 6–12 non-trivial digits");
        if(row.hash()!=null && !verify(user,profileId,row,input.currentPin()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"PIN verification required");
        byte[] salt=ProfilePinPolicy.salt();byte[] hash=ProfilePinPolicy.derive(input.newPin(),salt);
        db.update("UPDATE profiles SET pin_salt=?,pin_hash=?,pin_failed_attempts=0,pin_locked_until=NULL WHERE id=? AND user_id=?",
            salt,hash,profileId,user);
        return new PinStatus(true,false);
    }
    private boolean verify(UUID user,UUID id,PinRow row,String pin){
        Instant now=Instant.now();
        if(ProfilePinPolicy.locked(now,row.lockedUntil()))return false;
        if(ProfilePinPolicy.matches(pin,row.salt(),row.hash())){
            db.update("UPDATE profiles SET pin_failed_attempts=0,pin_locked_until=NULL WHERE id=? AND user_id=?",id,user);
            return true;
        }
        int failures=row.failures()+1;Instant lock=ProfilePinPolicy.nextLock(now,failures);
        db.update("UPDATE profiles SET pin_failed_attempts=?,pin_locked_until=? WHERE id=? AND user_id=?",
            failures,lock==null?null:java.sql.Timestamp.from(lock),id,user);
        return false;
    }
    /**
     * Protect an owner-scoped sensitive operation, such as deleting an adult profile.
     * Must run inside the caller's write transaction: get() takes a row lock, and
     * failed attempts must commit even when the request is rejected (403).
     *
     * A profile without a configured PIN remains accessible to its authenticated
     * account owner. This method does not create a reusable playback/profile grant.
     */
    public void requireForSensitiveAction(UUID user, UUID profileId, String suppliedPin) {
        PinRow row=get(user,profileId); // Includes account ownership check and SELECT ... FOR UPDATE.
        if(row.hash()==null)return;
        if(!verify(user,profileId,row,suppliedPin))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Invalid or temporarily locked PIN");
    }

    @PostMapping("/{profileId}/pin/check")
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public PinStatus check(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID profileId,
                           @Valid @RequestBody PinInput input){
        UUID user=owner(jwt);PinRow row=get(user,profileId);
        if(row.hash()==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"PIN not configured");
        if(!verify(user,profileId,row,input.pin()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Invalid or temporarily locked PIN");
        return new PinStatus(true,false);
    }
}
