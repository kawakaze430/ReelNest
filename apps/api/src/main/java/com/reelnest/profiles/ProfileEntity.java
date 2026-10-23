package com.reelnest.profiles;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profiles")
public class ProfileEntity {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false) private String name;
    @Column(name = "preferred_language", nullable = false) private String preferredLanguage;
    @Column(name = "is_child", nullable = false) private boolean child;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected ProfileEntity() {}
    public ProfileEntity(UUID userId, String name, String language, boolean child) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.name = name;
        this.preferredLanguage = language;
        this.child = child;
        this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public boolean isChild() { return child; }
}
