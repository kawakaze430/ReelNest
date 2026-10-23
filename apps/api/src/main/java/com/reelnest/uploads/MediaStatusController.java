package com.reelnest.uploads;

import com.reelnest.creators.CreatorService;
import com.reelnest.catalog.TitleRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/creator/titles/{titleId}/media")
public class MediaStatusController {
    private final CreatorService creators;
    private final TitleRepository titles;
    private final JdbcTemplate jdbc;
    public MediaStatusController(CreatorService creators, TitleRepository titles, JdbcTemplate jdbc) {
        this.creators=creators;this.titles=titles;this.jdbc=jdbc;
    }
    @GetMapping
    public List<Map<String,Object>> list(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID titleId) {
        var creator=creators.requireApproved(jwt);
        titles.findByIdAndCreatorId(titleId,creator.getId()).orElseThrow(
            ()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Title not found"));
        // IMPORTANT: never return private S3 source/manifest keys to creators.
        return jdbc.queryForList("""
            SELECT m.id,m.processing_status,m.duration_seconds,m.created_at,
                   j.state AS job_state,j.attempts,j.max_attempts,j.last_error,j.updated_at,
                   j.progress_stage,j.progress_percent
            FROM media_assets m LEFT JOIN media_processing_jobs j ON j.media_asset_id=m.id
            WHERE m.title_id=? ORDER BY m.created_at DESC
            """,titleId);
    }
}
