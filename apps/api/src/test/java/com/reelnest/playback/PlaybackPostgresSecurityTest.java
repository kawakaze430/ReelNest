package com.reelnest.playback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reelnest.billing.BillingService;
import com.reelnest.billing.SubscriptionPaymentProvider;
import com.reelnest.privacy.IdentityTombstone;
import com.reelnest.users.CurrentUserService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Actual PostgreSQL and Spring transaction proxies, with synthetic metadata only.
 * These tests do not verify HTTP JWT signatures, S3 objects, or decoded playback.
 * Committed fixtures let independent service transactions and admission threads
 * see the same rows; cleanup addresses only this test's generated identities.
 */
@DataJpaTest(showSql = false, properties = {
    "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.show-sql=false",
    "app.playback.dev-proxy-enabled=true", "app.playback.trusted-country=IN",
    "app.playback.cdn-base-url=", "app.billing.razorpay.standard-plan-id=",
    "app.billing.razorpay.premium-plan-id=", "spring.datasource.hikari.connection-timeout=10000",
    "spring.datasource.hikari.data-source-properties.options=-c statement_timeout=15000 -c lock_timeout=10000"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PlaybackService.class, CurrentUserService.class, BillingService.class,
    CdnCookieSigner.class, ObjectMapper.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@EnabledIfEnvironmentVariable(named = "REELNEST_DISPOSABLE_DB", matches = "true")
@EnabledIfEnvironmentVariable(named = "REELNEST_SECURITY_DB_URL",
    matches = "jdbc:postgresql://(?:127\\.0\\.0\\.1|localhost):[0-9]+/reelnest_security_test")
class PlaybackPostgresSecurityTest {
    @Autowired private PlaybackService playback;
    @Autowired private CurrentUserService users;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private SubscriptionPaymentProvider provider;

    private final List<String> subjects = new ArrayList<>();
    private UUID ownerId, creatorId, titleId, assetId, rightsId, adultProfile, childProfile;
    private Jwt owner, other;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("REELNEST_SECURITY_DB_URL"));
        properties.add("spring.datasource.username", () -> System.getenv().getOrDefault("DB_USER", "reelnest"));
        properties.add("spring.datasource.password", () -> System.getenv().getOrDefault("DB_PASSWORD", "local_dev_only"));
    }

    private Jwt identity() {
        String subject = "security-test-" + UUID.randomUUID();
        subjects.add(subject);
        return Jwt.withTokenValue("synthetic-local-service-test")
            .header("alg", "none").subject(subject)
            .claim("email", subject + "@example.invalid")
            .issuedAt(Instant.now().minusSeconds(30)).expiresAt(Instant.now().plusSeconds(600)).build();
    }

    @BeforeEach
    void fixtures() {
        assertTrue(AopUtils.isAopProxy(playback), "The real Spring transaction proxy is required");
        assertTrue(AopUtils.isAopProxy(users), "Identity resolution must use its transaction proxy");
        owner = identity();
        other = identity();
        ownerId = users.resolve(owner).getId();
        users.resolve(other);
        creatorId = UUID.randomUUID();
        titleId = UUID.randomUUID();
        assetId = UUID.randomUUID();
        rightsId = UUID.randomUUID();
        adultProfile = profile(false);
        childProfile = profile(true);
        jdbc.update("INSERT INTO creators(id,user_id,display_name,legal_name,contact_email,application_statement,status) VALUES(?,?,?,?,?,?,'APPROVED')",
            creatorId, ownerId, "Security Fixture", "Synthetic Fixture", "security@example.invalid", "No real film or transaction");
        jdbc.update("""
            INSERT INTO titles(id,slug,name,synopsis,type,genre,language,status,age_rating,
                runtime_minutes,release_year,art_tone,creator_id,access_tier)
            VALUES(?,?,?,'Synthetic SQL metadata','MOVIE','Drama','en','PUBLISHED','U',1,2026,'blue',?,'FREE')
            """, titleId, "security-" + titleId, "Security fixture " + titleId, creatorId);
        jdbc.update("""
            INSERT INTO content_rights(id,title_id,creator_id,rights_basis,proof_reference,
                agreement_notes,valid_from,valid_until,status)
            VALUES(?,?,?,'OWNER','Synthetic SQL fixture','No real media',CURRENT_DATE-10,CURRENT_DATE+10,'APPROVED')
            """, rightsId, titleId, creatorId);
        jdbc.update("INSERT INTO rights_territories(rights_id,country_code) VALUES(?,'IN')", rightsId);
        jdbc.update("""
            INSERT INTO media_assets(id,title_id,source_key,manifest_key,processing_status,duration_seconds)
            VALUES(?,?,?,?,'READY',60)
            """, assetId, titleId, "security/" + titleId + ".mp4", "processed/security/" + titleId + "/master.m3u8");
    }

    private UUID profile(boolean child) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO profiles(id,user_id,name,is_child) VALUES(?,?,?,?)", id, ownerId, child ? "Child" : "Adult", child);
        return id;
    }

    @AfterEach
    void cleanupOnlyOwnedFixtures() {
        try {
            verifyNoInteractions(provider); // Premium denial uses stored evidence, never a payment API.
        } finally {
            if (titleId != null) {
                jdbc.update("DELETE FROM watch_progress WHERE media_asset_id=?", assetId);
                jdbc.update("DELETE FROM playback_sessions WHERE title_id=?", titleId);
                jdbc.update("DELETE FROM media_assets WHERE title_id=?", titleId);
                jdbc.update("DELETE FROM titles WHERE id=?", titleId);
            }
            if (creatorId != null) jdbc.update("DELETE FROM creators WHERE id=?", creatorId);
            for (String subject : subjects) {
                jdbc.update("DELETE FROM users WHERE identity_subject=?", subject);
                jdbc.update("DELETE FROM identity_tombstones WHERE subject_digest=?", IdentityTombstone.digest(subject));
            }
        }
    }

    private PlaybackService.Session start() {
        return playback.start(owner, new PlaybackService.Start(adultProfile, titleId));
    }

    private static void denied(HttpStatus status, Runnable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action::run).getStatusCode());
    }

    @Test
    void firstReadOnlyRequestCreatesIdentityAndReturnsExpectedNotFound() {
        Jwt firstRequest = identity();
        denied(HttpStatus.NOT_FOUND, () -> playback.continueWatching(firstRequest, UUID.randomUUID()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE identity_subject=?", Integer.class, firstRequest.getSubject()));
    }

    @Test
    void staleButUnexpiredUsableSessionsStillCountTowardsStreamLimit() {
        var first = start();
        var second = start();
        jdbc.update("UPDATE playback_sessions SET last_heartbeat_at=NOW()-interval '3 minutes' WHERE id IN (?,?)", first.sessionId(), second.sessionId());
        assertNotNull(playback.authorizeMedia(owner, first.sessionId(), "master.m3u8"));
        assertNotNull(playback.authorizeMedia(owner, second.sessionId(), "master.m3u8"));
        denied(HttpStatus.CONFLICT, this::start);
    }

    @Test
    void simultaneousAdmissionsCannotExceedTwoUsableStreams() throws Exception {
        var gate = new CountDownLatch(1);
        try (var threads = Executors.newFixedThreadPool(3)) {
            var outcomes = new ArrayList<java.util.concurrent.Future<Integer>>();
            for (int i = 0; i < 3; i++) outcomes.add(threads.submit(() -> {
                assertTrue(gate.await(10, TimeUnit.SECONDS));
                try { start(); return 201; }
                catch (ResponseStatusException denied) { return denied.getStatusCode().value(); }
            }));
            gate.countDown();
            var statuses = new ArrayList<Integer>();
            for (var outcome : outcomes) statuses.add(outcome.get(15, TimeUnit.SECONDS));
            assertEquals(List.of(201, 201, 409), statuses.stream().sorted().toList());
        }
    }

    @Test
    void anotherAccountCannotStartOrReadOwnerSessions() {
        var session = start();
        denied(HttpStatus.NOT_FOUND, () -> playback.start(other, new PlaybackService.Start(adultProfile, titleId)));
        denied(HttpStatus.NOT_FOUND, () -> playback.authorizeMedia(other, session.sessionId(), "master.m3u8"));
        denied(HttpStatus.NOT_FOUND, () -> playback.continueWatching(other, adultProfile));
    }

    @Test
    void rightsStatusTerritoryAndValidityAreEnforced() {
        jdbc.update("UPDATE content_rights SET status='REJECTED' WHERE id=?", rightsId);
        denied(HttpStatus.NOT_FOUND, this::start);
        jdbc.update("UPDATE content_rights SET status='APPROVED' WHERE id=?", rightsId);
        jdbc.update("UPDATE rights_territories SET country_code='US' WHERE rights_id=?", rightsId);
        denied(HttpStatus.NOT_FOUND, this::start);
        jdbc.update("UPDATE rights_territories SET country_code='IN' WHERE rights_id=?", rightsId);
        jdbc.update("UPDATE content_rights SET valid_until=CURRENT_DATE-1 WHERE id=?", rightsId);
        denied(HttpStatus.NOT_FOUND, this::start);
    }

    @Test
    void childAgeAndUnpaidPremiumRemainDenied() {
        jdbc.update("UPDATE titles SET age_rating='A' WHERE id=?", titleId);
        denied(HttpStatus.FORBIDDEN, () -> playback.start(owner, new PlaybackService.Start(childProfile, titleId)));
        var adult = start();
        playback.stop(owner, adult.sessionId());
        jdbc.update("UPDATE titles SET access_tier='PREMIUM' WHERE id=?", titleId);
        denied(HttpStatus.PAYMENT_REQUIRED, this::start);
    }

    @Test
    void rightsRevocationDeniesAlreadyIssuedSessions() {
        var session = start();
        assertNotNull(playback.authorizeMedia(owner, session.sessionId(), "master.m3u8"));
        jdbc.update("UPDATE content_rights SET status='REJECTED' WHERE id=?", rightsId);
        denied(HttpStatus.NOT_FOUND, () -> playback.authorizeMedia(owner, session.sessionId(), "master.m3u8"));
        denied(HttpStatus.NOT_FOUND, () -> playback.heartbeat(owner, session.sessionId()));
        denied(HttpStatus.NOT_FOUND, () -> playback.saveProgress(owner, session.sessionId(), new PlaybackService.Progress(5, 1)));
    }

    @Test
    void progressOrderingAndStoppedSessionRevocationAreEnforced() {
        var session = start();
        assertEquals(10, playback.saveProgress(owner, session.sessionId(), new PlaybackService.Progress(10, 1)).positionSeconds());
        denied(HttpStatus.CONFLICT, () -> playback.saveProgress(owner, session.sessionId(), new PlaybackService.Progress(9, 1)));
        assertEquals(20, playback.saveProgress(owner, session.sessionId(), new PlaybackService.Progress(20, 2)).positionSeconds());
        assertEquals(20, playback.continueWatching(owner, adultProfile).getFirst().positionSeconds());
        playback.stop(owner, session.sessionId());
        denied(HttpStatus.FORBIDDEN, () -> playback.authorizeMedia(owner, session.sessionId(), "master.m3u8"));
        denied(HttpStatus.FORBIDDEN, () -> playback.saveProgress(owner, session.sessionId(), new PlaybackService.Progress(21, 3)));
    }

    @Test
    void actuallyExpiredSessionDoesNotConsumeAdmission() {
        var expired = start();
        start();
        jdbc.update("UPDATE playback_sessions SET created_at=NOW()-interval '20 minutes',expires_at=NOW()-interval '1 minute' WHERE id=?", expired.sessionId());
        assertNotNull(start());
        denied(HttpStatus.FORBIDDEN, () -> playback.authorizeMedia(owner, expired.sessionId(), "master.m3u8"));
    }

    @Test
    void identityBootstrapPreservesErasureSuspensionAndJwtRevocationChecks() {
        Jwt erased = identity();
        jdbc.update("INSERT INTO identity_tombstones(subject_digest) VALUES(?)", IdentityTombstone.digest(erased.getSubject()));
        denied(HttpStatus.FORBIDDEN, () -> playback.continueWatching(erased, UUID.randomUUID()));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE identity_subject=?", Integer.class, erased.getSubject()));
        jdbc.update("UPDATE users SET status='SUSPENDED' WHERE id=?", ownerId);
        denied(HttpStatus.FORBIDDEN, () -> playback.continueWatching(owner, adultProfile));
        jdbc.update("UPDATE users SET status='ACTIVE',token_valid_after=NOW() WHERE id=?", ownerId);
        denied(HttpStatus.UNAUTHORIZED, () -> playback.continueWatching(owner, adultProfile));
        denied(HttpStatus.NOT_FOUND, () -> playback.continueWatching(other, UUID.randomUUID()));
    }
}
