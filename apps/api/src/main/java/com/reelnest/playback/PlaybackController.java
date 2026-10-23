package com.reelnest.playback;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/playback")
public class PlaybackController {
    private final PlaybackService service;
    private final CdnCookieSigner cdn;
    public PlaybackController(PlaybackService service,CdnCookieSigner cdn) { this.service=service;this.cdn=cdn; }

    @GetMapping("/next-episode")
    public org.springframework.http.ResponseEntity<PlaybackService.NextEpisode> nextEpisode(@AuthenticationPrincipal Jwt jwt,
        @RequestParam UUID profileId,@RequestParam UUID titleId) {
        PlaybackService.NextEpisode next=service.nextEpisode(jwt,profileId,titleId);
        if(next==null)return org.springframework.http.ResponseEntity.noContent().build();
        return org.springframework.http.ResponseEntity.ok(next);
    }

    @GetMapping("/availability")
    public PlaybackService.Availability availability(@AuthenticationPrincipal Jwt jwt,
        @RequestParam UUID profileId,@RequestParam UUID titleId) {
        return service.availability(jwt,profileId,titleId);
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public PlaybackService.Session create(@AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody PlaybackService.Start request,HttpServletResponse response) {
        PlaybackService.Session session=service.start(jwt,request);
        if(cdn.enabled()) cdn.grant(response,service.authorizedCdnManifest(jwt,session.sessionId()));
        return session;
    }

    @PostMapping("/sessions/{id}/heartbeat")
    public PlaybackService.Heartbeat heartbeat(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,
        HttpServletResponse response) {
        PlaybackService.Heartbeat h=service.heartbeat(jwt,id);
        if(cdn.enabled()) cdn.grant(response,service.authorizedCdnManifest(jwt,id));
        return h;
    }

    @PutMapping("/sessions/{id}/progress")
    public PlaybackService.ProgressResult progress(@AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id,@Valid @RequestBody PlaybackService.Progress progress) {
        return service.saveProgress(jwt,id,progress);
    }

    @DeleteMapping("/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stop(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) { service.stop(jwt,id); }

    @DeleteMapping("/continue-watching/{titleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearProgress(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID profileId,
        @PathVariable UUID titleId) { service.clearProgress(jwt,profileId,titleId); }

    @GetMapping("/continue-watching")
    public List<PlaybackService.ContinueItem> resume(@AuthenticationPrincipal Jwt jwt,
        @RequestParam UUID profileId) { return service.continueWatching(jwt,profileId); }
}
