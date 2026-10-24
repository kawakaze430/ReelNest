package com.reelnest.billing;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Country capability disclosure, not a trusted geolocation or payment signal. */
@RestController
@RequestMapping("/api/v1/billing")
public class MarketEligibilityController {
    public record Market(String country,String currency,boolean checkoutAvailable,
                         String notice,List<String> visiblePlans) {}
    @GetMapping("/markets/{country}")
    public Market market(@PathVariable String country) {
        if(country==null || !country.matches("[A-Z]{2}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Two-letter uppercase country code required");
        // International merchants, tax, FX and territorial rights are unapproved.
        if(!"IN".equals(country))
            return new Market(country,"",false,"Billing is not enabled in this country",List.of());
        // Informational only. Real checkout still validates the configured Razorpay plan.
        return new Market("IN","INR",true,"Indian-market checkout only; provider payment verification required",
            List.of("STANDARD","PREMIUM"));
    }
}
