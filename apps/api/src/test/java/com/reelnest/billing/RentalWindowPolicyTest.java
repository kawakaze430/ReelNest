package com.reelnest.billing;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RentalWindowPolicyTest {
    private static final Instant PAID=Instant.parse("2026-10-24T00:00:00Z");
    @Test void rentalAndPlayWindowsHaveStrictEndBoundaries() {
        Instant licensed=PAID.plusSeconds(3600*200);
        assertTrue(RentalWindowPolicy.validWindow(PAID,null,PAID.plusSeconds(3600),licensed,48,24));
        assertTrue(RentalWindowPolicy.validWindow(PAID,PAID.plusSeconds(3600),
            PAID.plusSeconds(3600*2),licensed,48,24));
        assertFalse(RentalWindowPolicy.validWindow(PAID,null,PAID.plusSeconds(48*3600),licensed,48,24));
        assertFalse(RentalWindowPolicy.validWindow(PAID,PAID.plusSeconds(3600),
            PAID.plusSeconds(25*3600),licensed,48,24));
        assertFalse(RentalWindowPolicy.validWindow(PAID,null,PAID.plusSeconds(3600),
            PAID.plusSeconds(1800),48,24));
        assertFalse(RentalWindowPolicy.validWindow(PAID,null,PAID.plusSeconds(3600),licensed,0,24));
        assertFalse(RentalWindowPolicy.validWindow(null,null,PAID,licensed,48,24));
    }
}
