package com.reelnest.playback;

import com.reelnest.creators.CreatorService;
import com.reelnest.users.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Human-editor scene markers. GET does not grant playback, only metadata for eligible viewers. */
@RestController
@RequestMapping("/api/v1")
public class SceneMarkerController {
    private final JdbcTemplate jdbc;
    private final PlaybackService playback;
    private final CreatorService creators;
    private final CurrentUserService users;
    public SceneMarkerController(JdbcTemplate jdbc,PlaybackService playback,
                                 CreatorService creators,CurrentUserService users) {
        this.jdbc=jdbc;this.playback=playback;this.creators=creators;this.users=users;
    }
    public record Scene(int ordinal,String label,String summary,int startSeconds,int endSeconds) {}
    public record SceneUpdate(@NotNull @Size(max=40) List<SceneMarkerPolicy.Entry> scenes) {}

    @GetMapping("/admin/titles/{titleId}/media/{assetId}/scenes")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly=true)
    public List<Scene> adminList(@PathVariable UUID titleId,@PathVariable UUID assetId) {
        readyDuration(titleId,assetId,false);
        return readScenes(assetId);
    }

    @PutMapping("/admin/titles/{titleId}/media/{assetId}/scenes")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public List<Scene> replace(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID titleId,
                               @PathVariable UUID assetId,@Valid @RequestBody SceneUpdate update) {
        var duration=readyDuration(titleId,assetId,true);
        var entries=SceneMarkerPolicy.validate(update.scenes(),duration);
        // The DB transaction makes replacement atomic: never expose half an editorial update.
        jdbc.update("DELETE FROM media_scene_markers WHERE media_asset_id=?",assetId);
        int ordinal=0;
        for(var scene:entries) {
            jdbc.update("""
                INSERT INTO media_scene_markers(media_asset_id,ordinal,label,summary,start_seconds,end_seconds)
                VALUES(?,?,?,?,?,?)
                """,assetId,++ordinal,scene.label().strip(),scene.summary().strip(),
                scene.startSeconds(),scene.endSeconds());
        }
        creators.audit(users.resolve(jwt).getId(),"SCENES_REVIEWED","title",titleId,assetId.toString());
        return readScenes(assetId);
    }

    @GetMapping("/playback/scenes")
    @Transactional(readOnly=true)
    public ResponseEntity<List<Scene>> viewer(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam UUID titleId,@RequestParam UUID profileId,
                                               @RequestParam(required=false) String q) {
        // This check includes verified premium subscription, licensed territory, age
        // rules, published title, approved creator/rights and READY processed media.
        var allowed=playback.availability(jwt,profileId,titleId);
        if(!allowed.playable())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Scenes unavailable for this profile");
        if(q!=null && (q.length()>80 || (!q.isBlank() && q.strip().length()<2)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Search must be 2-80 characters");
        UUID media=jdbc.query("""
            SELECT m.id FROM media_assets m
            WHERE m.title_id=? AND m.processing_status='READY' AND m.manifest_key IS NOT NULL
            ORDER BY m.created_at DESC LIMIT 1
            """,rs->rs.next()?rs.getObject(1,UUID.class):null,titleId);
        if(media==null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Playable media not found");
        var scenes=readScenes(media);
        if(q!=null && !q.isBlank()) {
            String needle=q.strip().toLowerCase(java.util.Locale.ROOT);
            scenes=scenes.stream().filter(s->
                s.label().toLowerCase(java.util.Locale.ROOT).contains(needle) ||
                s.summary().toLowerCase(java.util.Locale.ROOT).contains(needle)).toList();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(scenes);
    }

    private int readyDuration(UUID titleId,UUID assetId,boolean lock) {
        Integer duration=jdbc.query("""
            SELECT duration_seconds FROM media_assets
            WHERE id=? AND title_id=? AND processing_status='READY'
            """+(lock?" FOR UPDATE":""),rs->rs.next()?(Integer)rs.getObject(1):null,assetId,titleId);
        if(duration==null || duration<1)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"READY media asset not found");
        return duration;
    }

    private List<Scene> readScenes(UUID assetId) {
        return jdbc.query("""
            SELECT ordinal,label,summary,start_seconds,end_seconds
            FROM media_scene_markers WHERE media_asset_id=? ORDER BY ordinal LIMIT 40
            """,(rs,n)->new Scene(rs.getInt(1),rs.getString(2),rs.getString(3),
                rs.getInt(4),rs.getInt(5)),assetId);
    }
}
