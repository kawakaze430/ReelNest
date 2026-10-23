package com.reelnest.billing;

import java.time.Instant;

/** Pure paid-access decision for offline assertion tests. */
public final class EntitlementPolicy {
    private EntitlementPolicy() {}
    public static boolean hasPremium(String status, String planCode, Instant periodEnd, Instant now) {
        return ("ACTIVE".equals(status) || "CANCEL_SCHEDULED".equals(status))
            && ("STANDARD".equals(planCode) || "PREMIUM".equals(planCode))
            && periodEnd != null && periodEnd.isAfter(now);
    }
    public static String mappedProviderStatus(String status, boolean cancelAtEnd) {
        if ("active".equalsIgnoreCase(status)) return cancelAtEnd ? "CANCEL_SCHEDULED" : "ACTIVE";
        return switch (status == null ? "" : status.toLowerCase(java.util.Locale.ROOT)) {
            case "created","authenticated" -> "PENDING";
            case "pending","halted","paused" -> "PAST_DUE";
            case "cancelled" -> "CANCELLED";
            case "completed","expired" -> "EXPIRED";
            default -> "PENDING";
        };
    }
}
