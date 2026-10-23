package com.reelnest.billing;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChargePolicyTest {
    private final Instant now = Instant.parse("2026-10-21T12:00:00Z");
    @Test void refundsAndDisputesRevokeUntilReconciled() {
        Instant later=now.plusSeconds(86400);
        assertTrue(ChargePolicy.counts(new ChargePolicy.Charge("CHARGED",later,14900,0,false),now));
        assertFalse(ChargePolicy.counts(new ChargePolicy.Charge("REFUNDED",later,14900,14900,false),now));
        assertFalse(ChargePolicy.counts(new ChargePolicy.Charge("PARTIALLY_REFUNDED",later,14900,100,false),now));
        assertFalse(ChargePolicy.counts(new ChargePolicy.Charge("CHARGED",later,14900,0,true),now));
        assertEquals("DISPUTED",ChargePolicy.transactionState(0,14900,true));
        assertEquals("REFUNDED",ChargePolicy.transactionState(14900,14900,false));
        assertEquals("PARTIALLY_REFUNDED",ChargePolicy.transactionState(100,14900,false));
    }
    @Test void onlyRemainingPaidCyclesCanGrantAccess() {
        Instant tomorrow=now.plusSeconds(86400);
        Instant nextWeek=now.plusSeconds(604800);
        assertEquals(tomorrow, ChargePolicy.paidThrough(List.of(
           new ChargePolicy.Charge("CHARGED",tomorrow,14900,0,false),
           new ChargePolicy.Charge("DISPUTED",nextWeek,14900,0,true)),now));
        assertNull(ChargePolicy.paidThrough(List.of(
           new ChargePolicy.Charge("REFUNDED",nextWeek,14900,14900,false)),now));
    }
}
