package com.reelnest.creators;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "creators")
public class CreatorEntity {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false, unique = true) private UUID userId;
    @Column(name = "display_name", nullable = false) private String displayName;
    @Column(name = "legal_name", nullable = false) private String legalName;
    @Column(name = "contact_email", nullable = false) private String contactEmail;
    @Column(name = "portfolio_url") private String portfolioUrl;
    @Column(name = "application_statement", nullable = false) private String applicationStatement;
    @Column(nullable = false) private String status;
    @Column(name = "review_note") private String reviewNote;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "reviewed_at") private Instant reviewedAt;

    protected CreatorEntity() {}
    public CreatorEntity(UUID userId, String displayName, String legalName, String contactEmail,
                         String portfolioUrl, String statement) {
        this.id = UUID.randomUUID(); this.userId = userId; this.displayName = displayName;
        this.legalName = legalName; this.contactEmail = contactEmail;
        this.portfolioUrl = portfolioUrl; this.applicationStatement = statement;
        this.status = "PENDING"; this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getLegalName() { return legalName; }
    public String getContactEmail() { return contactEmail; }
    public String getPortfolioUrl() { return portfolioUrl; }
    public String getApplicationStatement() { return applicationStatement; }
    public String getStatus() { return status; }
    public String getReviewNote() { return reviewNote; }
    public Instant getCreatedAt() { return createdAt; }
    public void review(String decision, String note) {
        this.status = decision; this.reviewNote = note; this.reviewedAt = Instant.now();
    }
}
