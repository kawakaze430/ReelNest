package com.reelnest.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reelnest.users.CurrentUserService;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BillingService {
    private final JdbcTemplate jdbc;
    private final CurrentUserService current;
    private final SubscriptionPaymentProvider provider;
    private final ObjectMapper mapper;
    private final String webhookSecret, standardPlan, premiumPlan;

    public BillingService(JdbcTemplate jdbc, CurrentUserService current,SubscriptionPaymentProvider provider,ObjectMapper mapper,
            @Value("${app.billing.razorpay.webhook-secret:}") String webhookSecret,
            @Value("${app.billing.razorpay.standard-plan-id:}") String standardPlan,
            @Value("${app.billing.razorpay.premium-plan-id:}") String premiumPlan) {
        this.jdbc=jdbc;this.current=current;this.provider=provider;this.mapper=mapper;
        this.webhookSecret=webhookSecret;this.standardPlan=standardPlan;this.premiumPlan=premiumPlan;
    }
    @PostConstruct
    public void configureProviderPlans() {
        configurePlan("STANDARD",standardPlan);
        configurePlan("PREMIUM",premiumPlan);
    }
    private void configurePlan(String code,String providerPlanId) {
        if (providerPlanId.isBlank()) return;
        if (!providerPlanId.matches("plan_[A-Za-z0-9]{8,100}"))
            throw new IllegalArgumentException("Invalid Razorpay plan ID for "+code);
        int changed=jdbc.update("""
            UPDATE subscription_plans SET provider_plan_id=?
            WHERE code=? AND (provider_plan_id IS NULL OR provider_plan_id=?)
            """,providerPlanId,code,providerPlanId);
        if (changed!=1) throw new IllegalStateException("Existing provider plan for "+code+" differs; reconcile subscriptions before changing plans");
    }

    public record Plan(String code,String label,int amountPaise,String currency,boolean active) {}
    public record Subscription(String id,String planCode,String status,Instant periodEnd,
                               Instant paidThrough,boolean cancelAtCycleEnd,String providerSubscriptionId) {}
    public record Checkout(String subscriptionId,String keyId,String planCode,String status) {}
    public record MyBilling(boolean premiumAccess,List<Subscription> subscriptions) {}

    public List<Plan> plans() {
        return jdbc.query("SELECT code,label,amount_paise,currency,active FROM subscription_plans WHERE active=TRUE ORDER BY amount_paise",
            (r,i)->new Plan(r.getString(1),r.getString(2),r.getInt(3),r.getString(4),r.getBoolean(5)));
    }
    private UUID id(Jwt jwt) { return current.resolve(jwt).getId(); }

    @Transactional(readOnly=true)
    public boolean hasPremium(UUID userId) {
        Integer count=jdbc.queryForObject("""
            SELECT COUNT(*) FROM subscriptions s
            WHERE s.user_id=? AND s.status IN ('ACTIVE','CANCEL_SCHEDULED')
              AND s.plan_code IN ('STANDARD','PREMIUM') AND s.paid_through>NOW()
              AND EXISTS (SELECT 1 FROM billing_transactions b
                  WHERE b.subscription_id=s.id AND b.status='CHARGED'
                    AND b.refunded_paise=0 AND b.dispute_open=FALSE
                    AND b.entitled_until>NOW())
            """,Integer.class,userId);
        return count!=null && count>0;
    }

    @Transactional
    public MyBilling me(Jwt jwt) {
        UUID user=id(jwt);
        List<Subscription> subscriptions=jdbc.query("""
            SELECT id,plan_code,status,current_period_end,paid_through,cancel_at_cycle_end,provider_subscription_id
            FROM subscriptions WHERE user_id=? ORDER BY created_at DESC LIMIT 25
            """,(r,i)->new Subscription(r.getString(1),r.getString(2),r.getString(3),
                r.getTimestamp(4)==null?null:r.getTimestamp(4).toInstant(),
                r.getTimestamp(5)==null?null:r.getTimestamp(5).toInstant(),r.getBoolean(6),r.getString(7)),user);
        return new MyBilling(hasPremium(user),subscriptions);
    }

    @Transactional
    public Checkout create(Jwt jwt,String code) {
        UUID user=id(jwt);
        if (!"STANDARD".equals(code) && !"PREMIUM".equals(code))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a paid plan");
        String planId="STANDARD".equals(code)?standardPlan:premiumPlan;
        if (!planId.matches("plan_[A-Za-z0-9]{8,100}"))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Configure the Razorpay plan ID first");
        // Serialize checkout admissions for this user, including requests on multiple devices.
        jdbc.queryForList("SELECT id FROM users WHERE id=? FOR UPDATE",UUID.class,user);
        if (hasPremium(user)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Already subscribed");
        Integer pending=jdbc.queryForObject("""
            SELECT COUNT(*) FROM subscriptions WHERE user_id=? AND status IN ('CREATED','PENDING')
              AND created_at>NOW()-interval '30 minutes'
            """,Integer.class,user);
        if (pending!=null && pending>0) throw new ResponseStatusException(HttpStatus.CONFLICT,
            "Checkout already pending. Refresh billing details before trying again.");
        // Compare the merchant's configured provider plan to displayed product pricing.
        JsonNode providerPlan=provider.getPlan(planId);
        Integer expectedPrice=jdbc.queryForObject(
            "SELECT amount_paise FROM subscription_plans WHERE code=? AND active=TRUE",Integer.class,code);
        if (!planId.equals(providerPlan.path("id").asText())
                || !"monthly".equals(providerPlan.path("period").asText())
                || providerPlan.path("interval").asInt()!=1
                || providerPlan.path("item").path("amount").asInt(-1)!=expectedPrice
                || !"INR".equals(providerPlan.path("item").path("currency").asText()))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Merchant billing plan amount/frequency mismatch. Contact support.");
        JsonNode remote=provider.create(planId,user.toString());
        String remoteId=remote.path("id").asText();
        if (!remoteId.matches("sub_[A-Za-z0-9]{8,100}") || !planId.equals(remote.path("plan_id").asText()))
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Unexpected subscription response");
        jdbc.update("""
            INSERT INTO subscriptions(id,user_id,plan_code,provider_subscription_id,status,provider_status)
            VALUES(?,?,?,?,'PENDING',?)
            """,UUID.randomUUID(),user,code,remoteId,remote.path("status").asText());
        return new Checkout(remoteId,provider.publicKey(),code,"PENDING");
    }

    @Transactional
    public MyBilling cancel(Jwt jwt,UUID subscriptionId) {
        UUID user=id(jwt);
        var records=jdbc.query("""
            SELECT provider_subscription_id,cancel_at_cycle_end,status FROM subscriptions
            WHERE id=? AND user_id=? FOR UPDATE
            """,(r,i)->new Object[]{r.getString(1),r.getBoolean(2),r.getString(3)},subscriptionId,user);
        if (records.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Subscription not found");
        Object[] record=records.getFirst();
        if ((boolean)record[1] || "CANCELLED".equals(record[2]) || "EXPIRED".equals(record[2]))
            return me(jwt); // Idempotent retries do not ask provider to cancel twice.
        // Request cancel at cycle end; provider state is authoritative, NOT this request.
        String providerId=(String)record[0];
        JsonNode result=provider.cancelAtEnd(providerId);
        syncProviderEntity(result,providerId);
        return me(jwt);
    }

    private static Instant end(JsonNode subscription) {
        long seconds=subscription.path("current_end").asLong(0);
        // Defensive bounds on provider time; never invent a paid period.
        if (seconds<=0 || seconds>4102444800L) return null;
        return Instant.ofEpochSecond(seconds);
    }
    private void syncProviderEntity(JsonNode remote,String expectedId) {
        if (!expectedId.equals(remote.path("id").asText()))
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Subscription identity mismatch");
        String status=remote.path("status").asText("");
        boolean cancelAtEnd=remote.path("cancel_at_cycle_end").asInt(0)==1;
        Instant expires=end(remote);
        // Grace periods and delayed charges must be consciously modeled, not guessed.
        String mapped=EntitlementPolicy.mappedProviderStatus(status,cancelAtEnd);
        String remotePlan=remote.path("plan_id").asText();
        int changed=jdbc.update("""
            UPDATE subscriptions s SET status=?,provider_status=?,current_period_end=?,
                  cancel_at_cycle_end=?,updated_at=NOW()
            FROM subscription_plans p WHERE s.provider_subscription_id=?
                  AND p.code=s.plan_code AND p.provider_plan_id=?
            """,mapped,status,expires==null?null:java.sql.Timestamp.from(expires),cancelAtEnd,expectedId,remotePlan);
        if(changed!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"Provider plan mismatch or subscription not registered");
    }

    /** Only the signature-authenticated webhook can cause entitlement changes; GETs never do. */
    @Transactional
    public void processWebhook(byte[] raw,String signature,String eventId) {
        if (raw.length>262144) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Webhook too large");
        if (!WebhookVerifier.verify(raw,signature,webhookSecret))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid payment webhook signature");
        if (eventId==null || !eventId.matches("[A-Za-z0-9_-]{8,140}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Missing event identifier");
        try {
            JsonNode event=mapper.readTree(raw);
            String type=event.path("event").asText("");
            boolean subscriptionEvent=type.matches("subscription\\.[a-z_]+");
            boolean reversalEvent=type.equals("payment.refunded") || type.equals("refund.processed")
                || type.equals("refund.created") || type.equals("payment.dispute.created")
                || type.equals("payment.dispute.closed");
            if (!subscriptionEvent && !reversalEvent) return;
            String subId = subscriptionEvent
                ? event.path("payload").path("subscription").path("entity").path("id").asText("")
                : null;
            String paymentId = reversalEvent ? paymentReference(event,type) : null;
            if (subscriptionEvent && !subId.matches("sub_[A-Za-z0-9]{8,100}"))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Missing provider subscription");
            if (reversalEvent && !paymentId.matches("pay_[A-Za-z0-9]{8,100}"))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Missing provider payment reference");
            int inserted=jdbc.update("""
                INSERT INTO payment_webhook_events(provider_event_id,event_name,provider_subscription_id)
                VALUES(?,?,?) ON CONFLICT DO NOTHING
                """,eventId,type,subId);
            if(inserted==0) return;
            if (subscriptionEvent) {
                // Fetch CURRENT provider state; webhook delivery can be out of order.
                JsonNode authoritative=provider.get(subId);
                syncProviderEntity(authoritative,subId);
                if("subscription.charged".equals(type)) recordCharge(event,subId);
            } else {
                // Reversals immediately remove the entitlement when the affected payment
                // belongs to this application; unknown external payments are ignored.
                reconcilePayment(paymentId,type.startsWith("payment.dispute.") || type.equals("refund.created"));
            }
            jdbc.update("UPDATE payment_webhook_events SET processed_at=NOW() WHERE provider_event_id=?",eventId);
        } catch (ResponseStatusException ex) { throw ex; }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Malformed webhook",ex); }
    }

    private void recordCharge(JsonNode event,String subId) {
        JsonNode payment=event.path("payload").path("payment").path("entity");
        String paymentId=payment.path("id").asText("");
        if (!paymentId.matches("pay_[A-Za-z0-9]{8,100}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Charged event missing payment");
        int amount=payment.path("amount").asInt(-1);
        String currency=payment.path("currency").asText("");
        String paymentStatus=payment.path("status").asText("");
        // Fail closed on incomplete, ambiguous, or discounted payment records.
        // Discounts and prorated cycles require a separate reviewed billing policy.
        if (amount<0 || !"INR".equals(currency) || !"captured".equals(paymentStatus))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Payment not verified as captured");
        Integer expected=jdbc.queryForObject("""
            SELECT p.amount_paise FROM subscriptions s
              JOIN subscription_plans p ON p.code=s.plan_code
            WHERE s.provider_subscription_id=?
            """,Integer.class,subId);
        if(expected==null || expected!=amount)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Charged amount does not match configured plan");
        // Validate captured status and refund state against provider, not only webhook body.
        JsonNode verified=provider.getPayment(paymentId);
        if (!paymentId.equals(verified.path("id").asText())
                || !"captured".equals(verified.path("status").asText())
                || verified.path("amount").asInt(-1)!=amount
                || !"INR".equals(verified.path("currency").asText())
                || verified.path("amount_refunded").asInt(0)!=0
                || (verified.hasNonNull("subscription_id")
                    && !subId.equals(verified.path("subscription_id").asText())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Payment failed provider verification");
        // Entitled period is bound at the time this particular provider charge is recorded.
        // A late historic event must not grant a speculative future billing cycle.
        int inserted=jdbc.update("""
            INSERT INTO billing_transactions(id,subscription_id,provider_payment_id,
                amount_paise,currency,status,entitled_until,last_reconciled_at)
            SELECT ?,s.id,?,?,?,'CHARGED',s.current_period_end,NOW()
            FROM subscriptions s WHERE s.provider_subscription_id=? AND s.current_period_end>NOW()
            ON CONFLICT(provider_payment_id) DO NOTHING
            """,UUID.randomUUID(),paymentId,amount,currency,subId);
        if (inserted==1) recomputePaidThrough(subId);
    }
    private static String paymentReference(JsonNode event, String type) {
        JsonNode payload=event.path("payload");
        if (type.startsWith("refund."))
            return payload.path("refund").path("entity").path("payment_id").asText("");
        if (type.startsWith("payment.dispute."))
            return payload.path("dispute").path("entity").path("payment_id").asText("");
        return payload.path("payment").path("entity").path("id").asText("");
    }

    /** Persist reversible charge state; a partial refund also denies access pending review. */
    @Transactional
    public void reconcilePayment(String paymentId, boolean disputeNotice) {
        var rows=jdbc.query("""
            SELECT b.id,s.provider_subscription_id,b.amount_paise,b.dispute_open
            FROM billing_transactions b JOIN subscriptions s ON s.id=b.subscription_id
            WHERE b.provider_payment_id=? FOR UPDATE OF b
            """,(r,i)->new Object[]{r.getObject(1,UUID.class),r.getString(2),r.getInt(3),r.getBoolean(4)},paymentId);
        if(rows.isEmpty()) return; // Other merchants' payment references are never imported.
        Object[] row=rows.getFirst();
        JsonNode remote=provider.getPayment(paymentId);
        if(!paymentId.equals(remote.path("id").asText()))
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Provider payment mismatch");
        int amount=(int)row[2], refunded=remote.path("amount_refunded").asInt(-1);
        if(amount!=remote.path("amount").asInt(-1) || refunded<0 || refunded>amount)
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Provider payment amount mismatch");
        // Dispute closure is NOT proof of a win. An opened dispute stays blocked until
        // a documented review. Never silently regrant access just because it was closed.
        boolean disputed=(boolean)row[3] || disputeNotice;
        String state=ChargePolicy.transactionState(refunded,amount,disputed);
        jdbc.update("""
            UPDATE billing_transactions SET refunded_paise=?, dispute_open=?,status=?,last_reconciled_at=NOW()
            WHERE id=?
            """,refunded,disputed,state,row[0]);
        recomputePaidThrough((String)row[1]);
    }

    private void recomputePaidThrough(String providerSubscriptionId) {
        jdbc.update("""
            UPDATE subscriptions s SET paid_through=(
               SELECT MAX(b.entitled_until) FROM billing_transactions b
               WHERE b.subscription_id=s.id AND b.status='CHARGED'
                 AND b.refunded_paise=0 AND b.dispute_open=FALSE
                 AND b.entitled_until>NOW()),updated_at=NOW()
            WHERE s.provider_subscription_id=?
            """,providerSubscriptionId);
    }

    /** Manual/admin reconciliation of missed events. Never grants access on API request alone. */
    @Transactional
    public void reconcileSubscription(String providerSubscriptionId) {
        JsonNode remote=provider.get(providerSubscriptionId);
        syncProviderEntity(remote,providerSubscriptionId);
        List<String> payments=jdbc.query("""
            SELECT b.provider_payment_id FROM billing_transactions b
            JOIN subscriptions s ON s.id=b.subscription_id
            WHERE s.provider_subscription_id=?
            ORDER BY b.recorded_at DESC LIMIT 100
            """,(r,i)->r.getString(1),providerSubscriptionId);
        for(String paymentId:payments) reconcilePayment(paymentId,false);
        recomputePaidThrough(providerSubscriptionId);
    }

}
