package com.reelnest.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id private UUID id;
    @Column(name = "identity_subject", nullable = false, unique = true) private String identitySubject;
    @Column(nullable = false) private String email;
    @Column(nullable = false) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected UserEntity() {}
    public UserEntity(String subject, String email) {
        this.id = UUID.randomUUID();
        this.identitySubject = subject;
        this.email = email;
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getIdentitySubject() { return identitySubject; }
    public String getStatus() { return status; }
}
