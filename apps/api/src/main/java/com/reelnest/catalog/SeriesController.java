package com.reelnest.catalog;

import com.reelnest.creators.CreatorService;
import com.reelnest.users.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Editorial episode ordering only; playback independently rechecks rights and subscriptions. */
@RestController
public class SeriesController {
    private final JdbcTemplate jdbc;
    private final TitleRepository titles;
    private final CreatorService creators;
    private final CurrentUserService users;
    public SeriesController(JdbcTemplate jdbc,TitleRepository titles,CreatorService creators,CurrentUserService users) {
        this.jdbc=jdbc;this.titles=titles;this.creators=creators;this.users=users;
    }
    public record Episode(UUID titleId,String name,String synopsis,int seasonNumber,int episodeNumber,
                          Integer runtimeMinutes,String ageRating,String artTone) {}
    public record EpisodeLink(@NotNull UUID episodeTitleId,@Min(1) @Max(100) int seasonNumber,
                              @Min(1) @Max(500) int episodeNumber) {}

    @GetMapping("/api/v1/catalog/{seriesId}/episodes")
    @Transactional(readOnly=true)
    public List<Episode> episodes(@PathVariable UUID seriesId) {
        TitleEntity series=titles.findByIdAndStatus(seriesId,"PUBLISHED").orElseThrow(
            ()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Series unavailable"));
        if(!"SERIES".equals(series.getType()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Not a series");
        return jdbc.query("""
            SELECT t.id,t.name,t.synopsis,se.season_number,se.episode_number,
                   t.runtime_minutes,t.age_rating,t.art_tone
            FROM series_episodes se JOIN titles t ON t.id=se.episode_title_id
            WHERE se.series_id=? AND t.status='PUBLISHED'
            ORDER BY se.season_number,se.episode_number LIMIT 500
            """,(r,i)->new Episode(r.getObject(1,UUID.class),r.getString(2),r.getString(3),
                r.getInt(4),r.getInt(5),(Integer)r.getObject(6),r.getString(7),r.getString(8)),seriesId);
    }

    @PutMapping("/api/v1/admin/series/{seriesId}/episodes")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public EpisodeLink link(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID seriesId,
                            @Valid @RequestBody EpisodeLink input) {
        TitleEntity series=titles.findById(seriesId).orElseThrow(
            ()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Series not found"));
        TitleEntity episode=titles.findById(input.episodeTitleId()).orElseThrow(
            ()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Episode title not found"));
        if(!"SERIES".equals(series.getType()) || !"MOVIE".equals(episode.getType()) ||
            !"PUBLISHED".equals(series.getStatus()) || !"PUBLISHED".equals(episode.getStatus()) ||
            series.getCreatorId()==null || !series.getCreatorId().equals(episode.getCreatorId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Published series and episode must have the same creator");
        // Prevent retargeting an existing episode to another series, or silent overwrites.
        Integer existing=jdbc.queryForObject("""
            SELECT COUNT(*) FROM series_episodes WHERE episode_title_id=?
              OR (series_id=? AND season_number=? AND episode_number=?)
            """,Integer.class,input.episodeTitleId(),seriesId,input.seasonNumber(),input.episodeNumber());
        if(existing!=null && existing>0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Episode already linked or position occupied");
        try {
            jdbc.update("""
                INSERT INTO series_episodes(series_id,episode_title_id,season_number,episode_number)
                VALUES(?,?,?,?)
                """,seriesId,input.episodeTitleId(),input.seasonNumber(),input.episodeNumber());
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Episode already linked or position occupied",ex);
        }
        creators.audit(users.resolve(jwt).getId(),"SERIES_EPISODE_LINKED","title",seriesId,input.episodeTitleId().toString());
        return input;
    }
}
