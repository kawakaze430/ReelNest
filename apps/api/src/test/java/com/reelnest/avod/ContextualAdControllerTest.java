package com.reelnest.avod;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import com.reelnest.users.CurrentUserService;
import com.reelnest.users.UserEntity;
import com.reelnest.playback.PlaybackService;
import com.reelnest.billing.BillingService;

class ContextualAdControllerTest {
    private final Jwt jwt=Jwt.withTokenValue("x").header("alg","none").claim("sub","viewer").build();
    private final JdbcTemplate db=mock(JdbcTemplate.class);
    private final CurrentUserService users=mock(CurrentUserService.class);
    private final PlaybackService playback=mock(PlaybackService.class);
    private final BillingService billing=mock(BillingService.class);
    private final ContextualAdController controller=
        new ContextualAdController(db,users,playback,billing,"IN");

    @Test void wrongProfileIsNotFoundBeforeEvenCheckingPayment() {
        UUID id=UUID.randomUUID();
        when(users.resolve(jwt)).thenReturn(new UserEntity("viewer","viewer@example.invalid"));
        var forbidden=assertThrows(ResponseStatusException.class,
            ()->controller.preview(jwt,id,UUID.randomUUID()));
        assertEquals(HttpStatus.NOT_FOUND,forbidden.getStatusCode());
        verifyNoInteractions(playback,billing);
    }
    @Test void badDeploymentTerritoryFailClosed() {
        assertThrows(IllegalArgumentException.class,()->
            new ContextualAdController(db,users,playback,billing,"UNKNOWN"));
    }
    @Test void noUnverifiedAdOrRentalEntitlementsInPublicContract() {
        var example=new ContextualAdController.AccessOptions(false,false,false,false,false,
            "No checkout or streaming grants");
        assertFalse(example.actualAdInsertionAvailable());
        assertFalse(example.rentalPurchasesAvailable());
    }
}
