package com.reelnest.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {
    @Mock TitleRepository repository;
    @InjectMocks CatalogController controller;

    @Test
    void invalidPageSizeRejected() {
        assertThrows(ResponseStatusException.class,
            () -> controller.list(null, null, null, 0, 100));
    }

    @Test
    void missingPublishedTitleIsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndStatus(id, "PUBLISHED")).thenReturn(Optional.empty());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> controller.get(id));
        assertEquals(404, error.getStatusCode().value());
    }

    @Test
    void emptyCatalogUsesPagination() {
        when(repository.search(isNull(), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
        assertEquals(0, controller.list(null, null, null, 0, 12).getTotalElements());
    }
}
