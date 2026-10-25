package com.reelnest.admin;

import com.reelnest.catalog.TitleEntity;
import com.reelnest.catalog.TitleRepository;
import com.reelnest.creators.CreatorService;
import com.reelnest.rights.RightsService;
import com.reelnest.users.CurrentUserService;
import com.reelnest.users.UserEntity;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real MVC response/status contract; role filtering and database transactions
 * remain covered separately by the full authenticated integration workflow. */
class AdminPublicationHttpContractTest {
    private final CreatorService creators = mock(CreatorService.class);
    private final RightsService rights = mock(RightsService.class);
    private final TitleRepository titles = mock(TitleRepository.class);
    private final CurrentUserService users = mock(CurrentUserService.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final Jwt jwt = Jwt.withTokenValue("synthetic").header("alg", "none").subject("reviewer").build();
    private final UserEntity reviewer = new UserEntity("reviewer", "reviewer@example.invalid");
    private final TitleEntity title = new TitleEntity(UUID.randomUUID(), "Original integration film", "Synthetic only",
        "FILM", "Drama", "en", "U", 1, 2026);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        mvc = MockMvcBuilders.standaloneSetup(new AdminController(creators, rights, titles, users, jdbc))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
        title.setStatus("IN_REVIEW");
        when(titles.findById(title.getId())).thenReturn(Optional.of(title));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void rights(String status) {
        var today = LocalDate.now(ZoneOffset.UTC);
        when(rights.viewForTitle(title.getId())).thenReturn(new RightsService.RightsView(UUID.randomUUID(),
            title.getId(), "OWNER", "Original generated test film", "Internal software testing only",
            today.minusDays(1), today.plusDays(1), List.of("IN"), status, "Reviewed synthetic media"));
    }

    private void readyMedia(long count) {
        when(jdbc.queryForObject(contains("processing_status='READY'"), eq(Long.class), eq(title.getId())))
            .thenReturn(count);
    }

    @Test
    void successfulPublicationReturnsNoContentAndPublishesOnlyRequestedTitle() throws Exception {
        rights("APPROVED");
        readyMedia(1L);
        when(users.resolve(jwt)).thenReturn(reviewer);
        mvc.perform(post("/api/v1/admin/titles/{titleId}/publish", title.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNoContent()).andExpect(content().string(""));
        assertEquals("PUBLISHED", title.getStatus());
        verify(titles).findById(title.getId());
        verify(creators).audit(reviewer.getId(), "TITLE_PUBLISHED", "title", title.getId(), null);
    }

    @Test
    void unapprovedRightsRemainAConflictWithoutPublication() throws Exception {
        rights("PENDING");
        mvc.perform(post("/api/v1/admin/titles/{titleId}/publish", title.getId()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isConflict());
        assertEquals("IN_REVIEW", title.getStatus());
        verifyNoInteractions(creators, users, jdbc);
    }

    @Test
    void missingReadyMediaRemainsAConflictWithoutPublication() throws Exception {
        rights("APPROVED");
        readyMedia(0L);
        mvc.perform(post("/api/v1/admin/titles/{titleId}/publish", title.getId()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isConflict());
        assertEquals("IN_REVIEW", title.getStatus());
        verifyNoInteractions(creators, users);
    }

    @Test
    void draftCannotBePublishedBeforeReview() throws Exception {
        title.setStatus("DRAFT");
        mvc.perform(post("/api/v1/admin/titles/{titleId}/publish", title.getId()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isConflict());
        assertEquals("DRAFT", title.getStatus());
        verifyNoInteractions(creators, users, rights, jdbc);
    }

    @Test
    void unknownTitleRemainsNotFound() throws Exception {
        UUID missing = UUID.randomUUID();
        mvc.perform(post("/api/v1/admin/titles/{titleId}/publish", missing).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNotFound());
        verify(titles).findById(missing);
        verifyNoInteractions(creators, users, rights, jdbc);
    }

    @Test
    void approvedSeoReturnsNoContentAfterEligibilityAndScopedUpdate() throws Exception {
        when(jdbc.queryForObject(contains("JOIN creators c"), eq(Integer.class), eq(title.getId()))).thenReturn(1);
        when(jdbc.update("UPDATE titles SET seo_index_approved=? WHERE id=?", true, title.getId())).thenReturn(1);
        when(users.resolve(jwt)).thenReturn(reviewer);
        mvc.perform(post("/api/v1/admin/titles/{titleId}/seo-approval", title.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}"))
            .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(jdbc).update("UPDATE titles SET seo_index_approved=? WHERE id=?", true, title.getId());
        verify(creators).audit(reviewer.getId(), "SEO_APPROVED", "title", title.getId(), null);
    }

    @Test
    void ineligibleSeoRemainsAConflictWithoutMutation() throws Exception {
        when(jdbc.queryForObject(contains("JOIN creators c"), eq(Integer.class), eq(title.getId()))).thenReturn(0);
        mvc.perform(post("/api/v1/admin/titles/{titleId}/seo-approval", title.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}"))
            .andExpect(status().isConflict());
        verify(jdbc, never()).update(anyString(), any(), any());
        verifyNoInteractions(creators, users);
    }

    @Test
    void failedOwnedMediaRetryReturnsNoContentAndQueuesTheSameAsset() throws Exception {
        UUID asset = UUID.randomUUID();
        when(jdbc.update(contains("UPDATE media_processing_jobs AS j"), eq(asset), eq(title.getId()))).thenReturn(1);
        when(users.resolve(jwt)).thenReturn(reviewer);
        mvc.perform(post("/api/v1/admin/titles/{titleId}/media/{assetId}/retry", title.getId(), asset)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(jdbc).update(contains("m.title_id=?"), eq(asset), eq(title.getId()));
        verify(jdbc).update(contains("UPDATE media_assets SET processing_status='QUEUED'"), eq(asset));
        verify(creators).audit(reviewer.getId(), "MEDIA_RETRY_REQUESTED", "title", title.getId(), asset.toString());
    }

    @Test
    void nonfailedOrWrongTitleMediaRetryRemainsAConflict() throws Exception {
        UUID asset = UUID.randomUUID();
        mvc.perform(post("/api/v1/admin/titles/{titleId}/media/{assetId}/retry", title.getId(), asset)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isConflict());
        verify(jdbc).update(contains("j.state='FAILED'"), eq(asset), eq(title.getId()));
        verify(jdbc, never()).update(contains("UPDATE media_assets"), eq(asset));
        verifyNoInteractions(creators, users);
    }

    @Test
    void rejectionReturnsNoContentAndKeepsRequiredAuditReason() throws Exception {
        when(users.resolve(jwt)).thenReturn(reviewer);
        mvc.perform(post("/api/v1/admin/titles/{titleId}/reject", title.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Synthetic rejection evidence\"}"))
            .andExpect(status().isNoContent()).andExpect(content().string(""));
        assertEquals("REJECTED", title.getStatus());
        verify(creators).audit(reviewer.getId(), "TITLE_REJECTED", "title", title.getId(), "Synthetic rejection evidence");
    }

    @Test
    void rejectionOfUnreviewedDraftRemainsAConflict() throws Exception {
        title.setStatus("DRAFT");
        mvc.perform(post("/api/v1/admin/titles/{titleId}/reject", title.getId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Synthetic rejection evidence\"}"))
            .andExpect(status().isConflict());
        assertEquals("DRAFT", title.getStatus());
        verifyNoInteractions(creators, users, rights, jdbc);
    }
}
