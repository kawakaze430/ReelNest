package com.reelnest.billing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

class PlanChangePreviewControllerTest {
    private final BillingService billing=mock(BillingService.class);
    private final Jwt jwt=Jwt.withTokenValue("test").header("alg","none").claim("sub","viewer").build();
    private final PlanChangePreviewController controller=new PlanChangePreviewController(billing);
    @Test void doesNotInventProrationOrChangeEntitlement(){
        when(billing.me(jwt)).thenReturn(new BillingService.MyBilling(true,List.of(
            new BillingService.Subscription("local","STANDARD","ACTIVE",null,
                Instant.now().plusSeconds(3600),"false".equals("true"),"sub_provider"))));
        when(billing.plans()).thenReturn(List.of(
            new BillingService.Plan("STANDARD","Standard",14900,"INR",true),
            new BillingService.Plan("PREMIUM","Premium",29900,"INR",true)));
        var result=controller.preview(jwt,"PREMIUM");
        assertEquals("UPGRADE",result.direction());
        assertFalse(result.automaticChangeAvailable());
        assertNull(result.proratedAmountPaise());
        assertEquals(29900,result.targetAmountPaise());
        verify(billing).me(jwt);
        verify(billing).plans();
        verifyNoMoreInteractions(billing);
    }
    @Test void refusesNonPaidUsersAndUnsupportedTarget(){
        when(billing.me(jwt)).thenReturn(new BillingService.MyBilling(false,List.of()));
        var denied=assertThrows(ResponseStatusException.class,()->controller.preview(jwt,"PREMIUM"));
        assertEquals(HttpStatus.CONFLICT,denied.getStatusCode());
        assertThrows(ResponseStatusException.class,()->controller.preview(jwt,"BASIC"));
    }
}
