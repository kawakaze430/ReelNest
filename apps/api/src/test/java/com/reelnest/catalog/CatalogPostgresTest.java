package com.reelnest.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("REELNEST_CATALOG_DB_URL"));
        properties.add("spring.datasource.username", () -> System.getenv().getOrDefault("DB_USER", "reelnest"));
        properties.add("spring.datasource.password", () -> System.getenv().getOrDefault("DB_PASSWORD", "local_dev_only"));
    }

    @BeforeEach
    void createCatalogFixtures() {
        add("Aurora Harbor", "A hidden ORCHID among the tides", "Drama", "en", "PUBLISHED");
        add("ORCHID Orchard", "Daybreak in the valley", "Drama", "hi", "PUBLISHED");
        add("Signals at Night", "A distant transmission", "Sci-Fi", "en", "PUBLISHED");
        add("Orchid Draft", "A private unfinished film", "Drama", "en", "DRAFT");
        titles.flush();
    }

    private void add(String name, String synopsis, String genre, String language, String status) {
        var title = new TitleEntity(null, name, synopsis, "MOVIE", genre, language, "U", 1, 2026);
        title.setStatus(status);
        titles.save(title);
    }

    @Test
    void allNullFiltersReturnPublishedCatalogWithPaginationAndCount() {
        long expected = jdbc.queryForObject("SELECT COUNT(*) FROM titles WHERE status='PUBLISHED'", Long.class);
        var first = titles.search(null, null, null, PageRequest.of(0, 2, Sort.by("name")));
        var second = titles.search(null, null, null, PageRequest.of(1, 2, Sort.by("name")));
        assertEquals(expected, first.getTotalElements());
        assertEquals(expected, second.getTotalElements());
        assertEquals(2, first.getNumberOfElements());
        assertEquals(2, second.getNumberOfElements());
        assertTrue(first.getContent().stream().noneMatch(second.getContent()::contains));
        assertFalse(first.getContent().stream().anyMatch(t -> "DRAFT".equals(t.getStatus())));
    }

    @Test
    void nullGenreAndLanguageSearchCaseInsensitivelyAcrossNameAndSynopsis() {
        var page = titles.search(null, null, "oRcHiD", PageRequest.of(0, 1, Sort.by("name")));
        assertEquals(2, page.getTotalElements());
        assertEquals("Aurora Harbor", page.getContent().getFirst().getName());
        var next = titles.search(null, null, "oRcHiD", PageRequest.of(1, 1, Sort.by("name")));
        assertEquals("ORCHID Orchard", next.getContent().getFirst().getName());
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
        return titles.search(genre, language, query, PageRequest.of(0, 20, Sort.by("name")))
            .map(TitleEntity::getName).getContent();
    }
}
