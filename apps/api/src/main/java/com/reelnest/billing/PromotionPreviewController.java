package com.reelnest.billing;

import com.reelnest.users.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Quote preview only; does not create a subscription, transaction or entitlement. */
@RestController
@RequestMapping("/api/v1/billing/promotions")
public class PromotionPreviewController {
    private final JdbcTemplate db;
    private final CurrentUserService users;
    public PromotionPreviewController(JdbcTemplate db,CurrentUserService users){this.db=db;this.users=users;}
    public record QuoteRequest(@NotBlank String code,@NotBlank String planCode) {}
    public record Quote(String planCode,String currency,int originalMinor,int previewMinor,
                        String disclosure) {}
    @PostMapping("/preview")
    public Quote preview(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody QuoteRequest input){
        users.resolve(jwt);
        final String code;
        try {code=PromotionPolicy.checkedCode(input.code());}
        catch(IllegalArgumentException ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid promotion");}
        var plans=db.query("""
            SELECT p.amount_paise,p.currency,q.percent_off,q.starts_at,q.expires_at
            FROM subscription_plans p JOIN promotional_quotes q ON q.plan_code=p.code
            WHERE q.code=? AND p.code=? AND p.active AND q.active
            """,(r,i)->new Object[]{r.getInt(1),r.getString(2),r.getInt(3),
                r.getTimestamp(4).toInstant(),r.getTimestamp(5).toInstant()},code,input.planCode());
        if(plans.isEmpty() || !PromotionPolicy.active(Instant.now(),(Instant)plans.getFirst()[3],(Instant)plans.getFirst()[4]))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Promotion not available");
        Object[] row=plans.getFirst();int original=(int)row[0];
        return new Quote(input.planCode(),(String)row[1],original,PromotionPolicy.quoteMinor(original,(int)row[2]),
            "Illustrative quote ONLY. Current Razorpay checkout does NOT apply promotion codes; do not pay expecting this discount.");
    }
}
