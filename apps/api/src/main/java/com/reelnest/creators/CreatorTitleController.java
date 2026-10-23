package com.reelnest.creators;

import com.reelnest.catalog.*;
import com.reelnest.rights.RightsService;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/creator/titles")
public class CreatorTitleController {
    private final CreatorService creators;
    private final TitleRepository titles;
    private final RightsService rights;
    private final JdbcTemplate jdbc;
    public CreatorTitleController(CreatorService creators, TitleRepository titles, RightsService rights, JdbcTemplate jdbc) {
        this.creators = creators; this.titles = titles; this.rights = rights; this.jdbc = jdbc;
    }
    public record TitleDraft(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 3000) String synopsis,
        @NotBlank @Pattern(regexp = "MOVIE|SERIES") String type,
        @NotBlank @Size(max = 80) String genre,
        @NotBlank @Pattern(regexp = "[a-z]{2,3}") String language,
        @NotBlank @Pattern(regexp = "U|UA7|UA13|UA16|A") String ageRating,
        @Min(1) @Max(2000) Integer runtimeMinutes,
        @Min(1888) @Max(2100) Integer releaseYear,
        @Pattern(regexp="FREE|PREMIUM") String accessTier
    ) {}
    public record DraftView(UUID id, String name, String synopsis, String type, String genre,
        String language, String ageRating, Integer runtimeMinutes, Integer releaseYear, String status, String accessTier) {
        public static DraftView from(TitleEntity t) {
            return new DraftView(t.getId(), t.getName(), t.getSynopsis(), t.getType(),
                t.getGenre(), t.getLanguage(), t.getAgeRating(), t.getRuntimeMinutes(), t.getReleaseYear(), t.getStatus(),t.getAccessTier());
        }
    }
    @GetMapping
    public List<DraftView> list(@AuthenticationPrincipal Jwt jwt) {
        var creator = creators.requireApproved(jwt);
        return titles.findAllByCreatorIdOrderByCreatedAtDesc(creator.getId()).stream().map(DraftView::from).toList();
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public DraftView create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TitleDraft input) {
        var creator = creators.requireApproved(jwt);
        TitleEntity draft = new TitleEntity(creator.getId(), input.name().trim(), input.synopsis().trim(),
            input.type(), input.genre().trim(), input.language(), input.ageRating(), input.runtimeMinutes(), input.releaseYear());
        draft.setAccessTier(input.accessTier()==null?"FREE":input.accessTier());
        TitleEntity created = titles.save(draft);
        creators.audit(creator.getUserId(), "DRAFT_CREATED", "title", created.getId(), null);
        return DraftView.from(created);
    }
    @PutMapping("/{id}")
    @Transactional
    public DraftView update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                            @Valid @RequestBody TitleDraft input) {
        var creator = creators.requireApproved(jwt);
        TitleEntity title = titles.findByIdAndCreatorId(id, creator.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Draft not found"));
        if (!"DRAFT".equals(title.getStatus()) || !title.getType().equals(input.type()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only drafts can be edited; title type cannot change");
        title.updateDraft(input.name().trim(), input.synopsis().trim(), input.genre().trim(),
            input.language(), input.ageRating(), input.runtimeMinutes(), input.releaseYear());
        title.setAccessTier(input.accessTier()==null?"FREE":input.accessTier());
        creators.audit(creator.getUserId(), "DRAFT_EDITED", "title", id, null);
        return DraftView.from(title);
    }
    @PostMapping("/{id}/submit")
    @Transactional
    public DraftView submit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        var creator = creators.requireApproved(jwt);
        var title = titles.findByIdAndCreatorId(id, creator.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Title not found"));
        if (!"DRAFT".equals(title.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Only drafts may be submitted");
        var rights = this.rights.viewForTitle(id);
        if (!("PENDING".equals(rights.status()) || "APPROVED".equals(rights.status())))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Submit valid distribution rights first");
        Long uploaded = jdbc.queryForObject(
            "SELECT COUNT(*) FROM upload_intents WHERE title_id=? AND status='COMPLETED'", Long.class,id);
        if (uploaded == null || uploaded == 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Complete a video upload first");
        title.setStatus("IN_REVIEW");
        creators.audit(creator.getUserId(),"TITLE_SUBMITTED","title",id,null);
        return DraftView.from(title);
    }
    @PostMapping("/{id}/reopen")
    @Transactional
    public DraftView reopen(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        var creator = creators.requireApproved(jwt);
        var title = titles.findByIdAndCreatorId(id, creator.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Title not found"));
        if (!"REJECTED".equals(title.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Only rejected titles can be reopened");
        title.setStatus("DRAFT");
        creators.audit(creator.getUserId(),"DRAFT_REOPENED","title",id,null);
        return DraftView.from(title);
    }
}
