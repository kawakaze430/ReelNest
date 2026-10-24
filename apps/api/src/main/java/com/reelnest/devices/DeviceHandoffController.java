package com.reelnest.devices;

import com.reelnest.profiles.ProfileRepository;
import com.reelnest.users.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Signed-in same-account profile handoff; NOT TV activation or session transfer.
 * Never puts OIDC tokens or signed streaming URLs into the ephemeral code.
 */
@RestController
@RequestMapping("/api/v1/device-handoff")
public class DeviceHandoffController {
    private static final Duration TTL=Duration.ofMinutes(5);
    private static final DefaultRedisScript<String> REDEEM=new DefaultRedisScript<>("""
        local v=redis.call('GET',KEYS[1])
        if not v or string.sub(v,1,string.len(ARGV[1]))~=ARGV[1] then return nil end
        redis.call('DEL',KEYS[1])
        return v
        """,String.class);

    private final StringRedisTemplate redis;
    private final CurrentUserService users;
    private final ProfileRepository profiles;
    public DeviceHandoffController(StringRedisTemplate redis, CurrentUserService users,
                                   ProfileRepository profiles) {
        this.redis=redis;this.users=users;this.profiles=profiles;
    }
    public record Create(@NotNull UUID profileId) {}
    public record Redeem(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{32}") String code) {}
    public record Invitation(String code,Instant expiresAt) {}
    public record Selection(UUID profileId,String profileName) {}

    @PostMapping
    public ResponseEntity<Invitation> create(@AuthenticationPrincipal Jwt jwt,
                                              @Valid @RequestBody Create input) {
        UUID owner=users.resolve(jwt).getId();
        var profile=profiles.findByIdAndUserId(input.profileId(),owner).orElseThrow(
            ()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Profile not found"));
        // Codes are sampled from 192 cryptographic random bits; Redis stores only their digest.
        for(int i=0;i<3;i++){
            String code=DeviceHandoffPolicy.newCode();
            try{
                Boolean stored=redis.opsForValue().setIfAbsent("reelnest:device-handoff:"+
                    DeviceHandoffPolicy.digest(code),owner+"|"+profile.getId(),TTL);
                if(Boolean.TRUE.equals(stored))return ResponseEntity.status(HttpStatus.CREATED)
                    .cacheControl(CacheControl.noStore())
                    .body(new Invitation(code,Instant.now().plus(TTL)));
            }catch(RuntimeException unavailable){
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Pairing unavailable");
            }
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Pairing unavailable");
    }

    @PostMapping("/redeem")
    public ResponseEntity<Selection> redeem(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody Redeem input) {
        UUID owner=users.resolve(jwt).getId();
        if(!DeviceHandoffPolicy.validCode(input.code()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid pairing code");
        String result;
        try{
            // Compare owner and delete atomically. Wrong-account requests never consume the code.
            result=redis.execute(REDEEM,List.of("reelnest:device-handoff:"+
                DeviceHandoffPolicy.digest(input.code())),owner+"|");
        }catch(RuntimeException unavailable){
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Pairing unavailable");
        }
        if(result==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Invalid or expired pairing code");
        UUID id;
        try{id=UUID.fromString(result.substring((owner+"|").length()));}
        catch(RuntimeException corrupted){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Invalid pairing code");}
        var profile=profiles.findByIdAndUserId(id,owner).orElseThrow(
            ()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Profile no longer exists"));
        // No entitlement, OIDC credential, playback link, or media lease is returned.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(new Selection(profile.getId(),profile.getName()));
    }
}
