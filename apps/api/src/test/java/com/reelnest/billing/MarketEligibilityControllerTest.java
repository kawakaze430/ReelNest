package com.reelnest.billing;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class MarketEligibilityControllerTest {
    @Test void internationalMarketsFailClosed() {
        var c=new MarketEligibilityController();
        for(String country:new String[]{"US","GB","AE","CA"}) {
            var market=c.market(country);
            assertFalse(market.checkoutAvailable());
            assertTrue(market.visiblePlans().isEmpty());
            assertEquals("",market.currency());
        }
        assertEquals("INR",c.market("IN").currency());
    }
    @Test void inputMustNotPretendToBeGeolocation() {
        var c=new MarketEligibilityController();
        for(String country:new String[]{"","INR","in","!N"})
            assertThrows(ResponseStatusException.class,()->c.market(country));
    }
}
