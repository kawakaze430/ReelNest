package com.reelnest.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "titles")
public class TitleEntity {
    @Id private UUID id;
    @Column(name = "creator_id") private UUID creatorId;
    @Column(nullable = false, unique = true) private String slug;
    @Column(nullable = false) private String name;
    @Column(columnDefinition = "TEXT") private String synopsis;
    @Column(nullable = false) private String type;
    @Column(nullable = false) private String genre;
    @Column(nullable = false) private String language;
    @Column(nullable = false) private String status;
    @Column(name = "age_rating", nullable = false) private String ageRating;
    @Column(name = "runtime_minutes") private Integer runtimeMinutes;
    @Column(name = "release_year") private Integer releaseYear;
    @Column(name = "art_tone", nullable = false) private String artTone;
    @Column(nullable = false) private boolean featured;
    @Column(name = "access_tier", nullable = false) private String accessTier;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected TitleEntity() {}
    public TitleEntity(UUID creatorId, String name, String synopsis, String type, String genre,
                       String language, String ageRating, Integer runtimeMinutes, Integer releaseYear) {
        this.id = UUID.randomUUID();
        this.creatorId = creatorId;
        this.slug = "creator-" + this.id;
        this.name = name;
        this.synopsis = synopsis;
        this.type = type;
        this.genre = genre;
        this.language = language;
        this.ageRating = AgeRating.canonical(ageRating);
        this.runtimeMinutes = runtimeMinutes;
        this.releaseYear = releaseYear;
        this.status = "DRAFT";
        this.artTone = "copper";
        this.featured = false;
        this.accessTier = "FREE";
        this.createdAt = Instant.now();
    }
    public void updateDraft(String name, String synopsis, String genre, String language,
                            String ageRating, Integer runtimeMinutes, Integer releaseYear) {
        this.name = name;
        this.synopsis = synopsis;
        this.genre = genre;
        this.language = language;
        this.ageRating = AgeRating.canonical(ageRating);
        this.runtimeMinutes = runtimeMinutes;
        this.releaseYear = releaseYear;
    }
    public void setStatus(String status) { this.status = status; }
    public void setAccessTier(String tier) {
        if (!"FREE".equals(tier) && !"PREMIUM".equals(tier))
            throw new IllegalArgumentException("Unsupported access tier");
        if (!"DRAFT".equals(status))
            throw new IllegalStateException("Access tier may only be edited on drafts");
        this.accessTier=tier;
    }
    public UUID getCreatorId() { return creatorId; }
    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getSynopsis() { return synopsis; }
    public String getType() { return type; }
    public String getGenre() { return genre; }
    public String getLanguage() { return language; }
    public String getStatus() { return status; }
    public String getAgeRating() { return ageRating; }
    public Integer getRuntimeMinutes() { return runtimeMinutes; }
    public Integer getReleaseYear() { return releaseYear; }
    public String getArtTone() { return artTone; }
    public boolean isFeatured() { return featured; }
    public String getAccessTier() { return accessTier; }
    public Instant getCreatedAt() { return createdAt; }
}
