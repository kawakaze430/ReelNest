package com.reelnest.catalog;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TitleRepository extends JpaRepository<TitleEntity, UUID> {
    List<TitleEntity> findAllByCreatorIdOrderByCreatedAtDesc(UUID creatorId);
    Optional<TitleEntity> findByIdAndCreatorId(UUID id, UUID creatorId);
    Optional<TitleEntity> findByIdAndStatus(UUID id, String status);
    Optional<TitleEntity> findBySlugAndStatus(String slug,String status);

    @Query("""
      select t from TitleEntity t
      where t.status = 'PUBLISHED'
      and (:genre is null or lower(t.genre) = lower(:genre))
      and (:language is null or lower(t.language) = lower(:language))
      and (:query is null or lower(t.name) like lower(concat('%', :query, '%'))
           or lower(t.synopsis) like lower(concat('%', :query, '%')))
      """)
    Page<TitleEntity> search(
        @Param("genre") String genre,
        @Param("language") String language,
        @Param("query") String query,
        Pageable pageable
    );
}
