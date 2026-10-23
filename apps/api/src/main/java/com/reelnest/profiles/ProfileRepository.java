package com.reelnest.profiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<ProfileEntity, UUID> {
    List<ProfileEntity> findAllByUserIdOrderByCreatedAtAsc(UUID userId);
    long countByUserId(UUID userId);
    Optional<ProfileEntity> findByIdAndUserId(UUID id, UUID userId);
}
