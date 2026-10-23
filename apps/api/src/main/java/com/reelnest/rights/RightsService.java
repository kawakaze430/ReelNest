package com.reelnest.rights;

import com.reelnest.catalog.TitleRepository;
import com.reelnest.creators.CreatorService;
import jakarta.validation.constraints.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RightsService {
    private final JdbcTemplate jdbc;
    private final CreatorService creators;
    private final TitleRepository titles;
    private final com.reelnest.users.CurrentUserService currentUser;
    public RightsService(JdbcTemplate jdbc, CreatorService creators, TitleRepository titles, com.reelnest.users.CurrentUserService currentUser) {
        this.jdbc = jdbc; this.creators = creators; this.titles = titles; this.currentUser = currentUser;
    }

    public record RightsInput(
        @NotBlank @Pattern(regexp = "OWNER|LICENSED_DISTRIBUTOR") String rightsBasis,
        @NotBlank @Size(max = 1000) String proofReference,
        @NotBlank @Size(min = 20, max = 2000) String agreementNotes,
        @NotNull LocalDate validFrom,
        @NotNull LocalDate validUntil,
        @NotEmpty @Size(max = 100) Set<@Pattern(regexp = "[A-Z]{2}") String> territories
    ) {}
    public record RightsView(UUID id, UUID titleId, String rightsBasis, String proofReference,
                             String agreementNotes, LocalDate validFrom, LocalDate validUntil,
                             List<String> territories, String status, String reviewNote) {}
    public record Review(@NotBlank @Pattern(regexp = "APPROVED|REJECTED") String decision,
                         @Size(max = 1000) String note) {}

    @Transactional
    public RightsView save(Jwt jwt, UUID titleId, RightsInput input) {
        var creator = creators.requireApproved(jwt);
        var title = titles.findByIdAndCreatorId(titleId, creator.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Title not found"));
        if (!"DRAFT".equals(title.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rights may be edited only while the title is a draft");
        if (!input.validUntil().isAfter(input.validFrom()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must follow start date");
        Set<String> countries = Set.of(Locale.getISOCountries());
        if (!countries.containsAll(input.territories()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use valid ISO 3166-1 alpha-2 territory codes");
        UUID requested = UUID.randomUUID();
        UUID rightsId = jdbc.queryForObject("""
            INSERT INTO content_rights(id,title_id,creator_id,rights_basis,proof_reference,agreement_notes,valid_from,valid_until,status)
            VALUES (?,?,?,?,?,?,?,?, 'PENDING')
            ON CONFLICT (title_id) DO UPDATE SET
                rights_basis = EXCLUDED.rights_basis,
                proof_reference = EXCLUDED.proof_reference,
                agreement_notes = EXCLUDED.agreement_notes,
                valid_from = EXCLUDED.valid_from,
                valid_until = EXCLUDED.valid_until,
                status = 'PENDING',
                review_note = NULL,
                reviewed_at = NULL,
                updated_at = NOW()
            RETURNING id
            """, UUID.class, requested, titleId, creator.getId(), input.rightsBasis(),
            input.proofReference().trim(), input.agreementNotes().trim(),
            Date.valueOf(input.validFrom()), Date.valueOf(input.validUntil()));
        jdbc.update("DELETE FROM rights_territories WHERE rights_id=?", rightsId);
        input.territories().stream().sorted().forEach(code -> jdbc.update(
            "INSERT INTO rights_territories(rights_id,country_code) VALUES (?,?)", rightsId, code));
        creators.audit(creator.getUserId(), "RIGHTS_SUBMITTED", "title", titleId, null);
        return viewForTitle(titleId);
    }
    @Transactional
    public RightsView mine(Jwt jwt, UUID titleId) {
        var creator = creators.requireApproved(jwt);
        titles.findByIdAndCreatorId(titleId, creator.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Title not found"));
        return viewForTitle(titleId);
    }
    @Transactional
    public RightsView viewForTitle(UUID titleId) {
        List<RightsView> rights = jdbc.query("""
            SELECT id,title_id,rights_basis,proof_reference,agreement_notes,valid_from,valid_until,status,review_note
            FROM content_rights WHERE title_id=?
            """, (rs, row) -> {
            UUID id = rs.getObject("id", UUID.class);
            List<String> territories = jdbc.queryForList(
                "SELECT TRIM(country_code) FROM rights_territories WHERE rights_id=? ORDER BY country_code",
                String.class, id);
            return new RightsView(id, rs.getObject("title_id", UUID.class),rs.getString("rights_basis"),
                rs.getString("proof_reference"),rs.getString("agreement_notes"),
                rs.getDate("valid_from").toLocalDate(),rs.getDate("valid_until").toLocalDate(),
                territories, rs.getString("status"),rs.getString("review_note"));
        }, titleId);
        return rights.stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rights not submitted"));
    }
    @Transactional
    public RightsView review(Jwt admin, UUID titleId, Review input) {
        var actor = currentUser.resolve(admin).getId();
        RightsView current = viewForTitle(titleId);
        if (!"PENDING".equals(current.status()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rights already reviewed");
        jdbc.update("UPDATE content_rights SET status=?, review_note=?, reviewed_at=NOW() WHERE id=?",
            input.decision(), input.note(), current.id());
        if ("REJECTED".equals(input.decision())) {
            // Give the creator an explicit correction path rather than leaving the title frozen IN_REVIEW.
            jdbc.update("UPDATE titles SET status='REJECTED' WHERE id=? AND status='IN_REVIEW'", titleId);
        }
        creators.audit(actor, "RIGHTS_" + input.decision(), "title", titleId, input.note());
        return viewForTitle(titleId);
    }
}
