package com.reelnest.rights;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/creator/titles/{titleId}/rights")
public class RightsController {
    private final RightsService rights;
    public RightsController(RightsService rights) { this.rights = rights; }
    @PutMapping
    public RightsService.RightsView save(@AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID titleId, @Valid @RequestBody RightsService.RightsInput input) {
        return rights.save(jwt, titleId, input);
    }
    @GetMapping
    public RightsService.RightsView get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID titleId) {
        return rights.mine(jwt, titleId);
    }
}
