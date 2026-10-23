package com.reelnest.playback;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/playback/sessions/{id}/qoe")
public class PlaybackQoeController {
    private final PlaybackService service;
    public PlaybackQoeController(PlaybackService service) {this.service=service;}
    /** No URLs, text messages, IPs, device identifiers, raw analytics payloads or client user IDs. */
    public record Event(
        @NotNull UUID eventId,
        @NotNull @Pattern(regexp="STARTUP|STALL|QUALITY_SWITCH|PLAYER_ERROR|RECOVERY") String eventType,
        @Min(0) @Max(3600000) Integer elapsedMs,
        @Min(1) @Max(4320) Integer qualityHeight,
        @Pattern(regexp="[A-Z0-9_]{1,32}") String errorCode
    ) {}
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void track(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Event event) {
        service.recordQoe(jwt,id,event);
    }
}
