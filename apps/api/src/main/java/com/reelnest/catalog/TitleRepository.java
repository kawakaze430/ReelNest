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

    // PostgreSQL needs text-typed optional parameters even when their value is null.
    @Query("""
      select t from TitleEntity t
      where t.status = 'PUBLISHED'
      and (cast(:genre as string) is null or lower(t.genre) = lower(cast(:genre as string)))
      and (cast(:language as string) is null or lower(t.language) = lower(cast(:language as string)))
      and (cast(:query as string) is null or lower(t.name) like lower(concat('%', cast(:query as string), '%'))
           or lower(t.synopsis) like lower(concat('%', cast(:query as string), '%')))
      """)
    Page<TitleEntity> search(
        @Param("genre") String genre,
        @Param("language") String language,
        @Param("query") String query,
        Pageable pageable
    );
}
