package com.reelnest.billing;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PaymentSecurityTest {
    @Test void webhookMustUseRawPayloadAndCorrectSecret() {
        byte[] data="{\"event\":\"subscription.charged\"}".getBytes(StandardCharsets.UTF_8);
        // Test signature calculated with a fixed fixture secret; do not put production keys here.
        String actual=sign(data,"test-only-secret");
        assertTrue(WebhookVerifier.verify(data,actual,"test-only-secret"));
        assertFalse(WebhookVerifier.verify(data,actual,"wrong-secret"));
        assertFalse(WebhookVerifier.verify("{}".getBytes(StandardCharsets.UTF_8),actual,"test-only-secret"));
        assertFalse(WebhookVerifier.verify(data,"nothex","test-only-secret"));
        assertFalse(WebhookVerifier.verify(data,actual,""));
    }
    private static String sign(byte[] bytes,String secret) {
        try {
            var hmac=javax.crypto.Mac.getInstance("HmacSHA256");
            hmac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            return java.util.HexFormat.of().formatHex(hmac.doFinal(bytes));
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }
    @Test void neverGrantPremiumWithoutVerifiedAndUnexpiredPayment() {
        Instant now=Instant.parse("2026-10-21T12:00:00Z");
        assertTrue(EntitlementPolicy.hasPremium("ACTIVE","STANDARD",now.plusSeconds(60),now));
        assertTrue(EntitlementPolicy.hasPremium("CANCEL_SCHEDULED","PREMIUM",now.plusSeconds(60),now));
        for(String state:new String[]{"PENDING","PAST_DUE","CANCELLED","EXPIRED"})
            assertFalse(EntitlementPolicy.hasPremium(state,"PREMIUM",now.plusSeconds(60),now));
        assertFalse(EntitlementPolicy.hasPremium("ACTIVE","PREMIUM",null,now));
        assertFalse(EntitlementPolicy.hasPremium("ACTIVE","FREE",now.plusSeconds(60),now));
        assertFalse(EntitlementPolicy.hasPremium("ACTIVE","STANDARD",now,now));
    }
    @Test void providerStatusMappingIsFailClosed() {
        assertEquals("ACTIVE",EntitlementPolicy.mappedProviderStatus("active",false));
        assertEquals("CANCEL_SCHEDULED",EntitlementPolicy.mappedProviderStatus("active",true));
        assertEquals("PAST_DUE",EntitlementPolicy.mappedProviderStatus("halted",false));
        assertEquals("PENDING",EntitlementPolicy.mappedProviderStatus("unknown",false));
    }
}
