package com.reelnest.billing;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Bounded reconciliation; a failed lookup must not fabricate entitlement. */
@Component
public class BillingReconciliationJob {
    private static final Logger log = LoggerFactory.getLogger(BillingReconciliationJob.class);
    private final JdbcTemplate jdbc;
    private final BillingService billing;
    private final RazorpayGateway gateway;
    private final boolean enabled;

    public BillingReconciliationJob(JdbcTemplate jdbc, BillingService billing,
        RazorpayGateway gateway,
        @Value("${app.billing.reconciliation.enabled:false}") boolean enabled) {
        this.jdbc=jdbc;this.billing=billing;this.gateway=gateway;this.enabled=enabled;
    }
    @Scheduled(fixedDelayString="${app.billing.reconciliation.delay-ms:1800000}", initialDelay=120000)
    public void reconcileRecent() {
        if (!enabled || !gateway.configured()) return;
        // Limit provider load. Multiple replicas should use a distributed scheduler/lock.
        List<String> subscriptionIds=jdbc.query("""
            SELECT provider_subscription_id FROM subscriptions
            WHERE provider_subscription_id IS NOT NULL
              AND status IN ('ACTIVE','CANCEL_SCHEDULED','PAST_DUE')
              AND updated_at>NOW()-interval '90 days'
            ORDER BY updated_at ASC LIMIT 25
            """,(rs,i)->rs.getString(1));
        for(String id: subscriptionIds) {
            try { billing.reconcileSubscription(id); }
            catch (Exception ex) { log.warn("Billing reconciliation failed for one subscription",ex); }
        }
    }
}
