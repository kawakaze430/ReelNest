package com.reelnest.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Real PostgreSQL coverage: H2 and a mocked repository cannot detect null/bytea bindings. */
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "REELNEST_DISPOSABLE_DB", matches = "true")
@EnabledIfEnvironmentVariable(named = "REELNEST_CATALOG_DB_URL",
    matches = "jdbc:postgresql://(?:127\\.0\\.0\\.1|localhost):[0-9]+/reelnest_catalog_test")
class CatalogPostgresTest {
    @Autowired private TitleRepository titles;
    @Autowired private JdbcTemplate jdbc;
    private UUID creatorId;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("REELNEST_CATALOG_DB_URL"));
        properties.add("spring.datasource.username", () -> System.getenv().getOrDefault("DB_USER", "reelnest"));
        properties.add("spring.datasource.password", () -> System.getenv().getOrDefault("DB_PASSWORD", "local_dev_only"));
    }

    @BeforeEach
    void createCatalogFixtures() {
        UUID owner=UUID.randomUUID();
        creatorId=UUID.randomUUID();
        jdbc.update("INSERT INTO users(id,identity_subject,email) VALUES(?,?,?)",owner,"catalog-test-"+owner,"catalog@example.invalid");
        jdbc.update("INSERT INTO creators(id,user_id,display_name,legal_name,contact_email,application_statement,status) VALUES(?,?,?,?,?,?,'APPROVED')",
            creatorId,owner,"Regression Studio","Regression Studio","catalog@example.invalid","Synthetic disposable SQL fixture");
        add("Aurora Harbor", "A hidden ORCHID among the tides", "Drama", "en", "PUBLISHED");
        add("ORCHID Orchard", "Daybreak in the valley", "Drama", "hi", "PUBLISHED");
        add("Signals at Night", "A distant transmission", "Sci-Fi", "en", "PUBLISHED");
        add("Orchid Draft", "A private unfinished film", "Drama", "en", "DRAFT");
        titles.flush();
    }

    private void add(String name, String synopsis, String genre, String language, String status) {
        var title = new TitleEntity(creatorId, name, synopsis, "MOVIE", genre, language, "U", 1, 2026);
        title.setStatus(status);
        titles.saveAndFlush(title);
        UUID rights=UUID.randomUUID();
        jdbc.update("INSERT INTO content_rights(id,title_id,creator_id,rights_basis,proof_reference,agreement_notes,valid_from,valid_until,status) VALUES(?,?,?,'OWNER','SQL fixture','No actual media claim',CURRENT_DATE-1,CURRENT_DATE+10,'APPROVED')",rights,title.getId(),creatorId);
        jdbc.update("INSERT INTO rights_territories(rights_id,country_code) VALUES(?,'IN')",rights);
        jdbc.update("INSERT INTO media_assets(id,title_id,source_key,manifest_key,processing_status,duration_seconds) VALUES(?,?,?,?,'READY',60)",UUID.randomUUID(),title.getId(),"regression/"+title.getId()+".mp4","regression/"+title.getId()+"/master.m3u8");
    }

    @Test
    void allNullFiltersReturnPublishedCatalogWithPaginationAndCount() {
        long expected = 3;
        var first = titles.searchPublic(null, null, null, null, "IN", PageRequest.of(0, 2));
        var second = titles.searchPublic(null, null, null, null, "IN", PageRequest.of(1, 2));
        assertEquals(expected, first.getTotalElements());
        assertEquals(expected, second.getTotalElements());
        assertEquals(2, first.getNumberOfElements());
        assertEquals(1, second.getNumberOfElements());
        assertTrue(first.getContent().stream().noneMatch(second.getContent()::contains));
        assertFalse(first.getContent().stream().anyMatch(t -> "DRAFT".equals(t.getStatus())));
    }

    @Test
    void nullGenreAndLanguageSearchCaseInsensitivelyAcrossNameAndSynopsis() {
        var page = titles.searchPublic(null, null, SearchTextPolicy.likeLiteral("oRcHiD"), null, "IN", PageRequest.of(0, 1));
        assertEquals(2, page.getTotalElements());
        var next = titles.searchPublic(null, null, SearchTextPolicy.likeLiteral("oRcHiD"), null, "IN", PageRequest.of(1, 1));
        assertEquals(Set.of("Aurora Harbor","ORCHID Orchard"),Set.of(page.getContent().getFirst().getName(),next.getContent().getFirst().getName()));
    }

    @Test
    void mixedNullFiltersAndNonemptyFiltersKeepTheirMeaning() {
        assertEquals(List.of("Aurora Harbor", "ORCHID Orchard"), names("dRaMa", null, "orchid"));
        assertEquals(List.of("Aurora Harbor"), names(null, "EN", "orchid"));
        assertEquals(List.of("Aurora Harbor"), names("DRAMA", "en", null));
        assertEquals(List.of("ORCHID Orchard"), names("drama", "HI", "ORCHID"));
        assertTrue(names("thriller", "hi", "orchid").isEmpty());
    }

    private List<String> names(String genre, String language, String query) {
        return titles.searchPublic(genre, language, query==null?null:SearchTextPolicy.likeLiteral(query), null, "IN", PageRequest.of(0, 20))
            .map(TitleEntity::getName).getContent().stream().sorted().toList();
    }
}
