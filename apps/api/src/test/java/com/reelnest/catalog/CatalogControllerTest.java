package com.reelnest.catalog;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

class CatalogControllerTest {
    private final TitleRepository repository=mock(TitleRepository.class);
    private final CatalogController controller=new CatalogController(repository,"IN");

    @Test void invalidPagesAndCountriesRejected() {
        assertThrows(ResponseStatusException.class,()->controller.list(null,null,null,0,100));
        assertThrows(IllegalArgumentException.class,()->new CatalogController(repository,"IND"));
        verifyNoInteractions(repository);
    }
    @Test void missingOrRevokedTitleDoesNotFallBackToPublishedOnly() {
        UUID id=UUID.randomUUID();
        when(repository.findPublicById(id,"IN")).thenReturn(Optional.empty());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->controller.get(id)).getStatusCode().value());
        verify(repository).findPublicById(id,"IN");
        verify(repository,never()).findByIdAndStatus(any(),any());
    }
    @Test void slugIsAlsoSubjectToCurrentRights() {
        when(repository.findPublicBySlug("film-1","IN")).thenReturn(Optional.empty());
        assertEquals(404,assertThrows(ResponseStatusException.class,
            ()->controller.bySlug("film-1")).getStatusCode().value());
        verify(repository).findPublicBySlug("film-1","IN");
        assertEquals(400,assertThrows(ResponseStatusException.class,
            ()->controller.bySlug("../bad")).getStatusCode().value());
    }
    @Test void searchUsesServerCountryAndEscapesLiteralMetacharacters() {
        when(repository.searchPublic(isNull(),isNull(),eq("%100!%!_!!%"),isNull(),eq("IN"),any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
        assertEquals(0,controller.list(null,null,"100%_!",null,0,12).getTotalElements());
        verify(repository).searchPublic(isNull(),isNull(),eq("%100!%!_!!%"),isNull(),eq("IN"),any(Pageable.class));
        verify(repository,never()).findByIdAndStatus(any(),any());
    }
    @Test void typeFacetValidatesBeforeDatabaseAccess() {
        assertEquals(400,assertThrows(ResponseStatusException.class,()->
            controller.list(null,null,null,"UNLICENSED",0,12)).getStatusCode().value());
    }
    @Test void unfilteredCatalogIsPaginatedAndRightsBound() {
        when(repository.searchPublic(isNull(),isNull(),isNull(),isNull(),eq("IN"),any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
        assertEquals(0,controller.list(null,null,null,null,0,12).getTotalElements());
    }
}
