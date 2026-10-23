package com.reelnest.uploads;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/creator/titles/{titleId}/uploads")
public class UploadController {
    private final UploadService uploads;
    public UploadController(UploadService uploads) { this.uploads = uploads; }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UploadService.Intent begin(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID titleId,
        @Valid @RequestBody UploadService.BeginRequest input) { return uploads.begin(jwt,titleId,input); }
    @GetMapping
    public List<UploadService.Intent> list(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID titleId) {
        return uploads.list(jwt,titleId);
    }
    @GetMapping("/{id}")
    public UploadService.Intent get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID titleId,@PathVariable UUID id) {
        return uploads.get(jwt,titleId,id);
    }
    @GetMapping("/{id}/parts/{partNumber}")
    public UploadService.PartUrl part(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID titleId,
        @PathVariable UUID id,@PathVariable int partNumber) { return uploads.part(jwt,titleId,id,partNumber); }
    @PostMapping("/{id}/complete")
    public UploadService.Intent complete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID titleId,
        @PathVariable UUID id,@Valid @RequestBody UploadService.CompleteRequest input) {
        return uploads.complete(jwt,titleId,id,input);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void abort(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID titleId,@PathVariable UUID id) {
        uploads.abort(jwt,titleId,id);
    }
}
