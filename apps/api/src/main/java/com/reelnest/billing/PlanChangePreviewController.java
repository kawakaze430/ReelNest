package com.reelnest.billing;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Read-only planning UI: no provider mutation, proration guess or entitlement grant. */
@RestController
@RequestMapping("/api/v1/billing")
public class PlanChangePreviewController {
    private final BillingService billing;
    public PlanChangePreviewController(BillingService billing) {this.billing=billing;}
    public record Preview(String currentPlan,String targetPlan,
                          int currentAmountPaise,int targetAmountPaise,String direction,
                          boolean automaticChangeAvailable,Integer proratedAmountPaise,
                          String notice) {}
    @GetMapping("/plan-change-preview")
    public Preview preview(@AuthenticationPrincipal Jwt jwt,@RequestParam String targetPlan) {
        if(!"STANDARD".equals(targetPlan)&&!"PREMIUM".equals(targetPlan))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a valid paid plan");
        var my=billing.me(jwt);
        if(!my.premiumAccess())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"A verified paid subscription is required");
        var confirmed=my.subscriptions().stream().filter(s->
            ("ACTIVE".equals(s.status())||"CANCEL_SCHEDULED".equals(s.status())) &&
            s.paidThrough()!=null && s.paidThrough().isAfter(Instant.now())).toList();
        if(confirmed.size()!=1)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Reconcile active subscriptions before a plan change");
        var current=confirmed.getFirst();
        if(current.planCode().equals(targetPlan))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Already on this plan");
        List<BillingService.Plan> plans=billing.plans();
        var previous=plans.stream().filter(p->p.code().equals(current.planCode())&&p.active())
            .findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Current plan is not active"));
        var requested=plans.stream().filter(p->p.code().equals(targetPlan)&&p.active())
            .findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Target plan unavailable"));
        if(!"INR".equals(previous.currency())||!"INR".equals(requested.currency()))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Unsupported currency transition");
        String direction=requested.amountPaise()>previous.amountPaise()?"UPGRADE":
            requested.amountPaise()<previous.amountPaise()?"DOWNGRADE":"SAME_PRICE";
        return new Preview(current.planCode(),requested.code(),previous.amountPaise(),
            requested.amountPaise(),direction,false,null,
            "Preview only. Contact support about changing plans. Automatic changes and proration " +
            "are disabled until the merchant confirms billing-cycle, refund, and webhook rules.");
    }
}
