package com.reelnest.creators;

import com.reelnest.catalog.TitleEntity;
import com.reelnest.catalog.TitleRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exercises the actual creator save path; worker timing validation is separate. */
class CreatorCaptionControllerTest {
    private static final String ORIGINAL =
        "1\n00:00:00,000 --> 00:00:01,000\nOriginal ReelNest integration fixture.\n";
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final CreatorService creators = mock(CreatorService.class);
    private final TitleRepository titles = mock(TitleRepository.class);
    private final CreatorCaptionController controller = new CreatorCaptionController(jdbc, creators, titles);
    private final Jwt jwt = Jwt.withTokenValue("synthetic").header("alg", "none").subject("test-creator").build();
    private final UUID titleId = UUID.randomUUID();
    private final CreatorEntity creator = new CreatorEntity(UUID.randomUUID(), "Test Studio", "Test Person",
        "creator@example.invalid", null, "Repository-owned generated media only.");

    private void approvedOwner() {
        creator.review("APPROVED", "Synthetic test approval");
        when(creators.requireApproved(jwt)).thenReturn(creator);
        when(titles.findByIdAndCreatorId(titleId, creator.getId())).thenReturn(Optional.of(mock(TitleEntity.class)));
    }

    private void editableDraft() {
        approvedOwner();
        when(jdbc.queryForObject(contains("FOR UPDATE"), eq(String.class), eq(titleId))).thenReturn("DRAFT");
        when(jdbc.queryForObject(contains("COUNT(*) FROM media_assets"), eq(Integer.class), eq(titleId)))
            .thenReturn(0);
    }

    static Stream<String> originalCaptions() {
        return Stream.of(ORIGINAL, ORIGINAL.replace("\n", "\r\n"));
    }

    @ParameterizedTest
    @MethodSource("originalCaptions")
    @SuppressWarnings("unchecked")
    void savesOriginalTimingArrowsAndNormalizesLineEndings(String input) {
        editableDraft();
        var expected = new CreatorCaptionController.CaptionView("en", Instant.EPOCH);
        when(jdbc.queryForObject(contains("SELECT language,updated_at"), any(RowMapper.class), eq(titleId), eq("en")))
            .thenReturn(expected);

        var saved = controller.save(jwt, titleId, "en", new CreatorCaptionController.CaptionInput(input));

        assertEquals(expected, saved);
        verify(jdbc).update(contains("INSERT INTO title_caption_sources"), eq(titleId), eq("en"), eq(ORIGINAL));
        verify(creators).audit(creator.getUserId(), "CAPTION_UPSERTED", "title", titleId, "en");
    }

    @ParameterizedTest
    @ValueSource(strings = {"<b>unsafe markup</b>", "x > y", "unclosed < tag", ">"})
    void timingArrowsDoNotPermitMarkupOrOtherAngleBrackets(String cue) {
        editableDraft();
        var denied = assertThrows(ResponseStatusException.class, () -> controller.save(jwt, titleId, "en",
            new CreatorCaptionController.CaptionInput("1\n00:00:00,000 --> 00:00:01,000\n" + cue + "\n")));
        assertEquals(HttpStatus.BAD_REQUEST, denied.getStatusCode());
        verify(jdbc, never()).update(anyString(), any(), any(), any());
    }

    @Test
    void anotherCreatorsTitleIsNotFoundBeforeDatabaseChanges() {
        approvedOwner();
        when(titles.findByIdAndCreatorId(titleId, creator.getId())).thenReturn(Optional.empty());
        var denied = assertThrows(ResponseStatusException.class, () -> controller.save(jwt, titleId, "en",
            new CreatorCaptionController.CaptionInput(ORIGINAL)));
        assertEquals(HttpStatus.NOT_FOUND, denied.getStatusCode());
        verifyNoInteractions(jdbc);
    }

    @Test
    void creatorApprovalRemainsRequired() {
        when(creators.requireApproved(jwt)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        var denied = assertThrows(ResponseStatusException.class, () -> controller.save(jwt, titleId, "en",
            new CreatorCaptionController.CaptionInput(ORIGINAL)));
        assertEquals(HttpStatus.FORBIDDEN, denied.getStatusCode());
        verifyNoInteractions(titles, jdbc);
    }

    @Test
    void publishedTitleCannotChangeCaptions() {
        approvedOwner();
        when(jdbc.queryForObject(contains("FOR UPDATE"), eq(String.class), eq(titleId))).thenReturn("PUBLISHED");
        var denied = assertThrows(ResponseStatusException.class, () -> controller.save(jwt, titleId, "en",
            new CreatorCaptionController.CaptionInput(ORIGINAL)));
        assertEquals(HttpStatus.CONFLICT, denied.getStatusCode());
        verify(jdbc, never()).update(anyString(), any(), any(), any());
    }

    @Test
    void sourceAlreadyUploadedCannotReceiveStaleCaptionChanges() {
        editableDraft();
        when(jdbc.queryForObject(contains("COUNT(*) FROM media_assets"), eq(Integer.class), eq(titleId)))
            .thenReturn(1);
        var denied = assertThrows(ResponseStatusException.class, () -> controller.save(jwt, titleId, "en",
            new CreatorCaptionController.CaptionInput(ORIGINAL)));
        assertEquals(HttpStatus.CONFLICT, denied.getStatusCode());
        verify(jdbc, never()).update(anyString(), any(), any(), any());
    }
}
