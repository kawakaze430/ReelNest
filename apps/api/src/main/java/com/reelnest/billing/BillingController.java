package com.reelnest.billing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {
    private final BillingService billing;
    public BillingController(BillingService billing) { this.billing=billing; }
    public record Create(@Pattern(regexp="STANDARD|PREMIUM") String planCode) {}
    @GetMapping("/plans") public List<BillingService.Plan> plans() { return billing.plans(); }
    @GetMapping("/me") public BillingService.MyBilling me(@AuthenticationPrincipal Jwt jwt) { return billing.me(jwt); }
    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public BillingService.Checkout create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Create input) {
        return billing.create(jwt,input.planCode());
    }
    @PostMapping("/subscriptions/{id}/cancel")
    public BillingService.MyBilling cancel(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) {
        return billing.cancel(jwt,id);
    }
    @PostMapping("/webhooks/razorpay")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void webhook(@RequestBody byte[] body,
        @RequestHeader(value="X-Razorpay-Signature",required=false) String signature,
        @RequestHeader(value="x-razorpay-event-id",required=false) String eventId) {
        billing.processWebhook(body,signature,eventId);
    }
}
