package com.reelnest.billing;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;

/** Pure, independently testable fail-closed payment reversal / entitlement policy. */
public final class ChargePolicy {
    private ChargePolicy() {}
    public record Charge(String status, Instant entitledUntil, int amount, int refundedPaise,
                         boolean disputeOpen) {}
    public static boolean counts(Charge charge, Instant now) {
        return charge != null && "CHARGED".equals(charge.status())
            && !charge.disputeOpen() && charge.refundedPaise() == 0
            && charge.amount() > 0 && charge.entitledUntil() != null
            && charge.entitledUntil().isAfter(now);
    }
    public static Instant paidThrough(Collection<Charge> charges, Instant now) {
        return charges.stream().filter(c -> counts(c, now))
            .map(Charge::entitledUntil).filter(Objects::nonNull)
            .max(Instant::compareTo).orElse(null);
    }
    public static String transactionState(int refundPaise, int totalPaise, boolean disputed) {
        if (disputed) return "DISPUTED";
        if (refundPaise > 0 && refundPaise < totalPaise) return "PARTIALLY_REFUNDED";
        if (refundPaise >= totalPaise && totalPaise > 0) return "REFUNDED";
        return "CHARGED";
    }
}
