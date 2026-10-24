package com.reelnest.billing;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;

/** Type-level contract: billing cannot depend on an unapproved extra gateway. */
class SubscriptionPaymentProviderContractTest {
    @Test void razorpayImplementsTheOnlyConfiguredSubscriptionAdapter() {
        assertTrue(SubscriptionPaymentProvider.class.isAssignableFrom(RazorpayGateway.class));
        assertTrue(java.lang.reflect.Modifier.isInterface(SubscriptionPaymentProvider.class.getModifiers()));
        assertEquals(7,SubscriptionPaymentProvider.class.getDeclaredMethods().length);
    }
    @Test void serviceDependsOnTheProviderInterface() throws Exception {
        var provider=BillingService.class.getDeclaredField("provider");
        assertEquals(SubscriptionPaymentProvider.class,provider.getType());
    }
}
