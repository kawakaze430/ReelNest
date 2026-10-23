package com.reelnest.profiles;

import com.reelnest.users.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {
    private final ProfileRepository profiles;
    private final CurrentUserService currentUser;
    private final ProfilePinController pinController;
    public ProfileController(ProfileRepository profiles, CurrentUserService currentUser, ProfilePinController pinController) {
        this.pinController = pinController;
        this.profiles = profiles;
        this.currentUser = currentUser;
    }

    public record CreateProfile(
        @NotBlank @Size(max = 40) String name,
        @NotBlank @Pattern(regexp = "en|hi", message = "Supported languages are en and hi") String preferredLanguage,
        boolean child
    ) {}
    public record ProfileResponse(UUID id, String name, String preferredLanguage, boolean child) {
        static ProfileResponse from(ProfileEntity entity) {
            return new ProfileResponse(entity.getId(), entity.getName(),
                entity.getPreferredLanguage(), entity.isChild());
        }
    }

    @GetMapping
    public List<ProfileResponse> list(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = currentUser.resolve(jwt).getId();
        return profiles.findAllByUserIdOrderByCreatedAtAsc(userId)
            .stream().map(ProfileResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ProfileResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateProfile input) {
        UUID userId = currentUser.resolve(jwt).getId();
        if (profiles.countByUserId(userId) >= 5) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Maximum of five profiles reached");
        }
        return ProfileResponse.from(profiles.save(new ProfileEntity(
            userId, input.name().trim(), input.preferredLanguage(), input.child())));
    }

    @DeleteMapping("/{profileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID profileId,
                       @RequestHeader(value="X-Profile-Pin",required=false) String suppliedPin) {
        UUID userId = currentUser.resolve(jwt).getId();
        ProfileEntity profile = profiles.findByIdAndUserId(profileId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        pinController.requireForSensitiveAction(userId,profileId,suppliedPin);
        profiles.delete(profile);
    }
}
