package com.reelnest.creators;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorRepository extends JpaRepository<CreatorEntity, UUID> {
    Optional<CreatorEntity> findByUserId(UUID userId);
    List<CreatorEntity> findAllByStatusOrderByCreatedAtAsc(String status);
}
