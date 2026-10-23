package com.reelnest.creators;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/creators")
public class CreatorController {
    private final CreatorService creators;
    public CreatorController(CreatorService creators) { this.creators = creators; }

    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatorService.CreatorView apply(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody CreatorService.Application input) {
        return creators.apply(jwt, input);
    }
    @GetMapping("/me")
    public CreatorService.CreatorView mine(@AuthenticationPrincipal Jwt jwt) {
        return creators.mine(jwt);
    }
}
