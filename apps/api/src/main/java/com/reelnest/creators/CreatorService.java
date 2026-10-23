package com.reelnest.creators;

import com.reelnest.users.CurrentUserService;
import com.reelnest.users.UserEntity;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreatorService {
    private final CreatorRepository creators;
    private final CurrentUserService currentUser;
    private final JdbcTemplate jdbc;

    public CreatorService(CreatorRepository creators, CurrentUserService currentUser, JdbcTemplate jdbc) {
        this.creators = creators; this.currentUser = currentUser; this.jdbc = jdbc;
    }

    public record Application(
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Size(max = 160) String legalName,
        @NotBlank @Email @Size(max = 320) String contactEmail,
        @Size(max = 500) @Pattern(regexp = "^(https://[^\\s]+)?$", message = "Use a public HTTPS URL") String portfolioUrl,
        @NotBlank @Size(min = 20, max = 2000) String statement
    ) {}

    public record CreatorView(UUID id, String displayName, String legalName, String contactEmail,
                              String portfolioUrl, String statement, String status, String reviewNote) {
        public static CreatorView from(CreatorEntity c) {
            return new CreatorView(c.getId(), c.getDisplayName(), c.getLegalName(),
                c.getContactEmail(), c.getPortfolioUrl(), c.getApplicationStatement(), c.getStatus(), c.getReviewNote());
        }
    }
    public record Review(@NotBlank @Pattern(regexp = "APPROVED|REJECTED") String decision,
                         @Size(max = 1000) String note) {}

    @Transactional
    public CreatorView apply(Jwt jwt, Application input) {
        UserEntity user = currentUser.resolve(jwt);
        if (creators.findByUserId(user.getId()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application already exists");
        CreatorEntity creator = creators.save(new CreatorEntity(user.getId(), input.displayName().trim(),
            input.legalName().trim(), input.contactEmail().trim(), input.portfolioUrl(), input.statement().trim()));
        audit(user.getId(), "CREATOR_APPLIED", "creator", creator.getId(), null);
        return CreatorView.from(creator);
    }
    @Transactional
    public CreatorView mine(Jwt jwt) {
        UUID userId = currentUser.resolve(jwt).getId();
        return creators.findByUserId(userId).map(CreatorView::from)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No creator application"));
    }
    @Transactional
    public CreatorEntity requireApproved(Jwt jwt) {
        UUID userId = currentUser.resolve(jwt).getId();
        CreatorEntity c = creators.findByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Apply as a creator first"));
        if (!"APPROVED".equals(c.getStatus()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Creator approval required");
        return c;
    }
    @Transactional
    public List<CreatorView> pending() {
        return creators.findAllByStatusOrderByCreatedAtAsc("PENDING").stream().map(CreatorView::from).toList();
    }
    @Transactional
    public CreatorView review(Jwt reviewer, UUID id, Review input) {
        UserEntity admin = currentUser.resolve(reviewer);
        CreatorEntity creator = creators.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Creator not found"));
        if (!"PENDING".equals(creator.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application already reviewed");
        creator.review(input.decision(), input.note());
        audit(admin.getId(), "CREATOR_" + input.decision(), "creator", creator.getId(), input.note());
        return CreatorView.from(creator);
    }
    public void audit(UUID actor, String action, String entityType, UUID entityId, String detail) {
        jdbc.update("INSERT INTO audit_logs(id, actor_user_id, action, entity_type, entity_id, detail) VALUES (?,?,?,?,?,?)",
            UUID.randomUUID(), actor, action, entityType, entityId, detail);
    }
}
