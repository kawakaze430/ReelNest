package com.reelnest.users;

import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {
    private final CurrentUserService currentUser;
    public MeController(CurrentUserService currentUser) { this.currentUser = currentUser; }
    public record MeResponse(UUID id, String email, String status) {}

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        UserEntity user = currentUser.resolve(jwt);
        return new MeResponse(user.getId(), user.getEmail(), user.getStatus());
    }
}
