package com.reelnest.creators;

import com.reelnest.catalog.TitleRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Creator-owned SRT sidecars; the media worker validates timing against real duration.
 * Caption uploads must precede the source-video upload to avoid stale HLS packages.
 */
@RestController
@RequestMapping("/api/v1/creator/titles/{titleId}/captions")
public class CreatorCaptionController {
    private final JdbcTemplate jdbc;
    private final CreatorService creators;
    private final TitleRepository titles;
    public CreatorCaptionController(JdbcTemplate jdbc,CreatorService creators,TitleRepository titles) {
        this.jdbc=jdbc;this.creators=creators;this.titles=titles;
    }
    public record CaptionInput(@NotBlank @Size(max=200000) String srt) {}
    public record CaptionView(String language,java.time.Instant updatedAt) {}

    private void owned(Jwt jwt,UUID titleId) {
        var creator=creators.requireApproved(jwt);
        titles.findByIdAndCreatorId(titleId,creator.getId()).orElseThrow(
            ()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Title not found"));
    }
    private void editable(UUID titleId) {
        String status=jdbc.queryForObject("SELECT status FROM titles WHERE id=? FOR UPDATE",String.class,titleId);
        if(!"DRAFT".equals(status))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Only draft captions may be changed");
        Integer media=jdbc.queryForObject("""
            SELECT COUNT(*) FROM media_assets WHERE title_id=? AND processing_status NOT IN ('FAILED','REJECTED')
            """,Integer.class,titleId);
        if(media!=null && media>0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Upload captions before the video source; existing media must be resolved first");
    }
    private static void language(String language) {
        if(language==null || !language.matches("[a-z]{2,3}(-[a-z]{2,8})?"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use a supported lowercase language tag");
    }
    @GetMapping
    @Transactional(readOnly=true)
    public List<CaptionView> list(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID titleId) {
        owned(jwt,titleId);
        return jdbc.query("""
            SELECT language,updated_at FROM title_caption_sources WHERE title_id=? ORDER BY language
            """,(rs,n)->new CaptionView(rs.getString(1),rs.getTimestamp(2).toInstant()),titleId);
    }
    @PutMapping("/{language}")
    @Transactional
    public CaptionView save(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID titleId,
                            @PathVariable String language,@Valid @RequestBody CaptionInput input) {
        owned(jwt,titleId);language(language);editable(titleId);
        String srt=input.srt().replace("\r\n","\n").replace('\r','\n');
        if(srt.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>200000 ||
           srt.indexOf('\u0000')>=0 || srt.contains("<") || srt.replace("-->","").contains(">") ||
           !srt.contains("-->") || srt.lines().count()>15000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid or oversized SRT captions");
        Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM title_caption_sources WHERE title_id=?",Integer.class,titleId);
        Integer exists=jdbc.queryForObject("SELECT COUNT(*) FROM title_caption_sources WHERE title_id=? AND language=?",
            Integer.class,titleId,language);
        if(count!=null && count>=8 && (exists==null || exists==0))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"At most eight caption languages per title");
        jdbc.update("""
            INSERT INTO title_caption_sources(title_id,language,srt) VALUES(?,?,?)
            ON CONFLICT(title_id,language) DO UPDATE SET srt=EXCLUDED.srt,updated_at=NOW()
            """,titleId,language,srt);
        creators.audit(creators.requireApproved(jwt).getUserId(),"CAPTION_UPSERTED","title",titleId,language);
        return jdbc.queryForObject("""
            SELECT language,updated_at FROM title_caption_sources WHERE title_id=? AND language=?
            """,(rs,n)->new CaptionView(rs.getString(1),rs.getTimestamp(2).toInstant()),titleId,language);
    }
    @DeleteMapping("/{language}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID titleId,@PathVariable String language) {
        owned(jwt,titleId);language(language);editable(titleId);
        jdbc.update("DELETE FROM title_caption_sources WHERE title_id=? AND language=?",titleId,language);
        creators.audit(creators.requireApproved(jwt).getUserId(),"CAPTION_DELETED","title",titleId,language);
    }
}
