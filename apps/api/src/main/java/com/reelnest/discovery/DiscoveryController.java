package com.reelnest.discovery;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/discovery")
public class DiscoveryController {
    private final DiscoveryService service;
    public DiscoveryController(DiscoveryService service) { this.service=service; }
    public record ViewEvent(@NotNull UUID eventId,@NotNull UUID profileId,UUID titleId,@NotNull String eventType) {}
    @GetMapping("/home")
    public List<DiscoveryService.Item> home(@AuthenticationPrincipal Jwt jwt,
        @RequestParam UUID profileId,@RequestParam(defaultValue="24") int limit) {
        return service.home(jwt,profileId,limit);
    }
    @GetMapping("/search/semantic")
    public List<DiscoveryService.Item> semantic(@AuthenticationPrincipal Jwt jwt,
        @RequestParam UUID profileId,@RequestParam String q,@RequestParam(defaultValue="12") int limit) {
        return service.semantic(jwt,profileId,q,limit);
    }
    @PostMapping("/events")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void track(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody ViewEvent event) {
        service.ingest(jwt,event.eventId(),event.profileId(),event.titleId(),event.eventType());
    }
    @GetMapping("/admin/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public DiscoveryService.Overview overview() { return service.overview(); }
    @GetMapping("/admin/top-titles")
    @PreAuthorize("hasRole('ADMIN')")
    public List<DiscoveryService.PopularTitle> top() { return service.topTitles(); }
}
