package com.reelnest.playback;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** Local development only. At scale use CDN signed cookies/DRM, not Spring Boot as a media CDN. */
@RestController
@RequestMapping("/api/v1/playback")
public class PrivateMediaController {
    private final PlaybackService playback;
    private final S3Client s3;
    private final String bucket;
    private final boolean devProxy;

    public PrivateMediaController(PlaybackService playback, S3Client s3,
        @Value("${app.playback.processed-bucket:reelnest-processed-private}") String bucket,
        @Value("${app.playback.dev-proxy-enabled:false}") boolean devProxy) {
        this.playback=playback;this.s3=s3;this.bucket=bucket;this.devProxy=devProxy;
    }

    @GetMapping("/sessions/{sessionId}/files/**")
    public ResponseEntity<StreamingResponseBody> media(@AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID sessionId, HttpServletRequest request) {
        if (!devProxy) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "Local development media proxy disabled; production CDN signer not configured");
        // Explicit allowlist avoids servlet path normalization and encoded traversal bypasses.
        String prefix="/api/v1/playback/sessions/"+sessionId+"/files/";
        String requestPath=request.getRequestURI();
        if (!requestPath.startsWith(prefix) || requestPath.indexOf('%')>=0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid media URL");
        String relative=requestPath.substring(prefix.length());
        PlaybackService.MediaAccess authorized=playback.authorizeMedia(jwt,sessionId,relative);
        final ResponseInputStream<GetObjectResponse> object;
        try {
            object=s3.getObject(GetObjectRequest.builder().bucket(bucket).key(authorized.objectKey()).build());
        } catch (NoSuchKeyException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Video segment unavailable");
        } catch (S3Exception ex) {
            if (ex.statusCode()==404) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Video segment unavailable");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Private media storage unavailable");
        }
        StreamingResponseBody body=output -> {
            try (object) { object.transferTo(output); }
            catch (IOException e) { /* streaming clients can disconnect; container closes response */ throw e; }
        };
        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL,"private, no-store, max-age=0")
            .header("X-Content-Type-Options","nosniff")
            .contentType(MediaType.parseMediaType(authorized.contentType()))
            .contentLength(object.response().contentLength())
            .body(body);
    }
}
