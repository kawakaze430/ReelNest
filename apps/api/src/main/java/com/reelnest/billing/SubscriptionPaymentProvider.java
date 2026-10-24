package com.reelnest.billing;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Server-only, testable boundary for subscription-provider operations.
 * No provider implementation grants entitlement based on checkout or client data.
 * All charge, refund and dispute reconciliation remains in BillingService.
 *
 * Razorpay is the ONLY production-configured implementation in Build 30.
 * International payment methods are disabled pending compliance and tests.
 */
public interface SubscriptionPaymentProvider {
    boolean configured();
    String publicKey();
    JsonNode getPlan(String planId);
    JsonNode create(String planId, String userReference);
    JsonNode getPayment(String paymentId);
    JsonNode get(String subscriptionId);
    JsonNode cancelAtEnd(String subscriptionId);
}
