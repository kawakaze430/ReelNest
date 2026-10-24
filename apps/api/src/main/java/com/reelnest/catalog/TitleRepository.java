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

    // Reuse this exact eligibility predicate for every public catalog entry point.
    // Public metadata is *not* a media entitlement. Actual access is still
    // checked by PlaybackService on every session and media request.
    String PUBLIC_PREDICATE = """
      FROM titles t
      WHERE t.status='PUBLISHED'
        AND EXISTS (SELECT 1 FROM creators c WHERE c.id=t.creator_id AND c.status='APPROVED')
        AND EXISTS (SELECT 1 FROM content_rights r
                    JOIN rights_territories rt ON rt.rights_id=r.id
                    WHERE r.title_id=t.id AND r.status='APPROVED'
                      AND r.valid_from<=CURRENT_DATE AND r.valid_until>=CURRENT_DATE
                      AND rt.country_code=:country)
        AND EXISTS (SELECT 1 FROM media_assets m WHERE m.title_id=t.id
                    AND m.processing_status='READY' AND m.manifest_key IS NOT NULL)
      """;

    String FILTERS = """
        AND (CAST(:genre AS text) IS NULL OR lower(t.genre)=lower(CAST(:genre AS text)))
        AND (CAST(:language AS text) IS NULL OR lower(t.language)=lower(CAST(:language AS text)))
        AND (CAST(:contentType AS text) IS NULL OR t.type=CAST(:contentType AS text))
        AND (CAST(:query AS text) IS NULL OR t.name ILIKE CAST(:query AS text) ESCAPE '!'
             OR t.synopsis ILIKE CAST(:query AS text) ESCAPE '!'
             OR EXISTS (SELECT 1 FROM title_search_aliases a WHERE a.title_id=t.id
                        AND a.alias ILIKE CAST(:query AS text) ESCAPE '!'))
        """;

    @Query(value="SELECT t.* "+PUBLIC_PREDICATE+FILTERS+
             " ORDER BY t.featured DESC,t.created_at DESC,t.id DESC",
           countQuery="SELECT COUNT(*) "+PUBLIC_PREDICATE+FILTERS,
           nativeQuery=true)
    Page<TitleEntity> searchPublic(
        @Param("genre") String genre,
        @Param("language") String language,
        @Param("query") String query,
        @Param("contentType") String contentType,
        @Param("country") String country,
        Pageable pageable
    );

    @Query(value="SELECT t.* "+PUBLIC_PREDICATE+" AND t.id=:id LIMIT 1",nativeQuery=true)
    Optional<TitleEntity> findPublicById(@Param("id") UUID id,@Param("country") String country);

    @Query(value="SELECT t.* "+PUBLIC_PREDICATE+" AND t.slug=:slug LIMIT 1",nativeQuery=true)
    Optional<TitleEntity> findPublicBySlug(@Param("slug") String slug,@Param("country") String country);
}
