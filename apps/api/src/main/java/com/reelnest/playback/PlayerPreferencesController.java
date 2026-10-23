package com.reelnest.playback;

import com.reelnest.users.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Server-owned profile settings: same values on each signed-in device; no browser tokens stored. */
@RestController
@RequestMapping("/api/v1/profiles/{profileId}/player-preferences")
public class PlayerPreferencesController {
    private final JdbcTemplate jdbc;
    private final CurrentUserService users;
    public PlayerPreferencesController(JdbcTemplate jdbc,CurrentUserService users) {this.jdbc=jdbc;this.users=users;}

    public record Settings(
        @NotNull Double speed,
        @NotNull @Pattern(regexp="small|normal|large") String captionSize,
        @NotNull @Pattern(regexp="standard|high") String captionContrast,
        @NotNull @Pattern(regexp="|[a-z]{2,3}(-[a-z]{2,8})?") String preferredAudio,
        @NotNull @Pattern(regexp="|[a-z]{2,3}(-[a-z]{2,8})?") String preferredSubtitles,
        boolean dataSaver,
        boolean qoeConsent,
        boolean autoplayNext
    ) {
        public boolean validSpeed() {return speed!=null &&
            (speed==0.5 || speed==0.75 || speed==1.0 || speed==1.25 || speed==1.5 || speed==2.0);}
        public static Settings defaults() {return new Settings(1.0,"normal","standard","","",false,false,false);}
    }
    private void requireOwner(Jwt jwt,UUID profileId) {
        UUID user=users.resolve(jwt).getId();
        Integer owned=jdbc.queryForObject("SELECT COUNT(*) FROM profiles WHERE id=? AND user_id=?",Integer.class,profileId,user);
        if(owned==null || owned!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Profile not found");
    }
    @GetMapping
    @Transactional(readOnly=true)
    public Settings get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID profileId) {
        requireOwner(jwt,profileId);
        return jdbc.query("""
            SELECT speed,caption_size,caption_contrast,preferred_audio,preferred_subtitles,data_saver,qoe_consent,autoplay_next
            FROM player_preferences WHERE profile_id=?
            """,(r,i)->new Settings(r.getDouble(1),r.getString(2),r.getString(3),r.getString(4),
                r.getString(5),r.getBoolean(6),r.getBoolean(7),r.getBoolean(8)),profileId).stream().findFirst()
            .orElse(Settings.defaults());
    }
    @PutMapping
    @Transactional
    public Settings update(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID profileId,
                           @Valid @RequestBody Settings input) {
        requireOwner(jwt,profileId);
        if(!input.validSpeed()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported playback speed");
        jdbc.update("""
            INSERT INTO player_preferences(profile_id,speed,caption_size,caption_contrast,
                preferred_audio,preferred_subtitles,data_saver,qoe_consent,autoplay_next)
            VALUES(?,?,?,?,?,?,?,?,?)
            ON CONFLICT(profile_id) DO UPDATE SET speed=EXCLUDED.speed,
                caption_size=EXCLUDED.caption_size,caption_contrast=EXCLUDED.caption_contrast,
                preferred_audio=EXCLUDED.preferred_audio,preferred_subtitles=EXCLUDED.preferred_subtitles,
                data_saver=EXCLUDED.data_saver,qoe_consent=EXCLUDED.qoe_consent,
                autoplay_next=EXCLUDED.autoplay_next,updated_at=NOW()
            """,profileId,input.speed(),input.captionSize(),input.captionContrast(),input.preferredAudio(),
            input.preferredSubtitles(),input.dataSaver(),input.qoeConsent(),input.autoplayNext());
        // Revocation is stronger than simply stopping future collection: purge this profile's
        // prior opt-in QoE events. The telemetry writer locks this row FOR SHARE so an event
        // cannot be committed after this transaction's purge.
        if(!input.qoeConsent()) jdbc.update("DELETE FROM playback_qoe_events WHERE profile_id=?",profileId);
        return input;
    }
}
