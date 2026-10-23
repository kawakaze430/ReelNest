# ReelNest Builds 05 + 06 — Implementation reference

## Important truth boundary

Source has been implemented and offline structural/media/policy/ML tests run. **The complete runtime has not been built or started in this authoring environment** because Docker, Maven, dependency-registry network access and runnable frontend dependencies are unavailable. Razorpay subscriptions have **not** been exercised against a real TEST merchant. This is a developer code handoff, **not a production OTT deployment**.

## Build 05: payment and subscription system

### Included

- Flyway V6 tables `subscription_plans`, `subscriptions`, `payment_webhook_events` and `billing_transactions`; amount in integer paise, unique provider events and transaction IDs. Default demonstration tiers: FREE ₹0, STANDARD ₹149/mo, PREMIUM ₹299/mo. Both paid tiers currently unlock the same PREMIUM catalog, **not** differentiated benefits.
- Razorpay gateway in Java with HTTP timeouts, server-only Basic authentication and fixed HTTPS host; creates a merchant subscription (12 monthly cycles), verifies configured Razorpay plan ID, period, currency and amount before checkout, reads current subscription status from provider, and requests cancellation at cycle end.
- Frontend checkout at `/pricing` and `/account/billing`; uses Razorpay checkout.js (hosted payment UI) and polls/refreshes backend billing state. **Checkout success callbacks alone do not grant premium access**.
- Razorpay webhook `POST /api/v1/billing/webhooks/razorpay` is deliberately unauthenticated with OIDC, but verifies raw-body HMAC-SHA256 using a separate webhook secret. Dedupe through unique provider event ID, fetch provider-authoritative subscription state, and record unique confirmed charges.
- A paid entitlement additionally requires `paid_through > NOW()`; only a signed `subscription.charged` event containing the correct provider plan, captured INR payment, expected amount and known subscription can extend paid-through time. `subscription.activated` alone is not enough.
- Playback API rechecks subscriber entitlement on session start and media GETs. Subscription cancellation is checked with the provider. Creator drafts can specify `FREE` or `PREMIUM`, and admin reviewers see the designation.
- Fail closed with missing test keys or plan IDs.

### Configuration before using TEST checkout

1. Open Razorpay **TEST Mode** with an approved/eligible merchant account. Create recurring monthly Standard/Premium plans matching the displayed local amounts and currency exactly.
2. Copy `.env.billing.example` to `.env` in the repository root. Add **TEST** values for `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`, `RAZORPAY_STANDARD_PLAN_ID`, `RAZORPAY_PREMIUM_PLAN_ID`. Never commit `.env`.
3. Confirm Razorpay subscription APIs and recurring payments are enabled for your merchant account.
4. Set a dedicated Razorpay **TEST webhook** to `https://<your-staging-host>/api/v1/billing/webhooks/razorpay`. A local-only localhost URL cannot receive Razorpay internet callbacks. Use a secure tunnel only for controlled tests.
5. Subscribe to relevant subscription events, especially `subscription.charged`, `subscription.activated`, `subscription.pending`, `subscription.halted`, `subscription.cancelled`, and `subscription.completed`. Ensure raw request bytes reach Spring unmodified.
6. Use a fresh local database or migrate with Flyway V6/V7. Run checkout, verify status remains pending until the signed captured payment event arrives, verify event deduplication, test cancellation and expiry, and verify a premium film rejects an unpaid account.

### Billing APIs

| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/v1/billing/plans` | Public |
| GET | `/api/v1/billing/me` | Authenticated account |
| POST | `/api/v1/billing/subscriptions` | Authenticated account |
| POST | `/api/v1/billing/subscriptions/{id}/cancel` | Subscription owner |
| POST | `/api/v1/billing/webhooks/razorpay` | Provider signature verified |

### Important billing gaps / **do not launch paid production until resolved**

- Refunds, chargebacks, disputes, partial/prorated billing, plan upgrades and proration are **not implemented**. In particular, a refund does not currently revoke a previously recorded `paid_through` entitlement. This needs handling before a public paid launch.
- Full provider reconciliation/alerting of missed webhooks, an outbox for subscription creation across HTTP/DB failures, invoices and tax handling, and customer support tooling are pending.
- `subscription.charged` verification uses the signed event and a current provider subscription fetch, but the payment itself is not independently fetched; add explicit payment status reconciliation and address unusual timing/out-of-order cycle changes before production.
- The `paid_through` field is the *confirmed* access date, distinct from `current_period_end` (provider's reported cycle). Never set it from client data.
- The higher-priced PREMIUM demonstration plan currently has the same access as STANDARD. Do not sell two prices without clear differentiated benefits.

## Build 06: discovery and analytics

### Included

- Flyway V7 creates deduplicated `viewer_events` and `recommendation_runs` experiment metadata.
- `GET /api/v1/discovery/home?profileId=<UUID>&limit=24` returns actual media-ready and rights-approved current-territory titles only, filtered for selected child profile, scored from saved genres, viewed genres, preferred language, previous interactions, content freshness, and recent popularity. Reasons accompany recommendations. Paid titles remain discoverable but require a subscription for playback.
- Authenticated `POST /api/v1/discovery/events` accepts only allowlisted event names, UUID-dedupes, checks profile ownership and profile-appropriate content. Play-start/complete and watchlist-add signals are also emitted from trusted backend operations.
- `GET /api/v1/discovery/admin/overview` and `/admin/top-titles` require `ADMIN` role. UI is at `/admin/analytics`; viewer recommendations at `/discover` and on `/browse`.
- `EventRetentionJob` purges limited batches of viewing events older than 180 days (UTC scheduler). This is only an initial operational default; formal privacy compliance workflows are pending.
- `apps/recommendation-lab/` contains an *actual offline* scikit-learn TF-IDF content similarity + collaborative co-interaction model, cold-start ranking, eligible-ID filtering, Recall@K / NDCG@K, and six unit tests.

### Recommendation caveats

The active Java endpoint is a rule-weighted hybrid recommender, **not** a trained neural ranking model. The Python model is a tested offline lab and is not connected to the production request path. No uplift or measured recommendation performance is claimed. Behavior and sample data are not representative of paying subscribers.

Viewer events are not abuse-proof; rate limiting, consent preferences, stronger events validation, cookie and privacy controls, and analytics scaling are future hardening tasks. The platform only returns authorized profiles and legal content; personalized ranking never itself grants playback entitlements.

## Exact implementation/testing commands

```bash
# From repository root
python3 scripts/check_static.py
python3 scripts/check_build02_security.py
python3 scripts/check_build03.py
python3 scripts/check_build04.py
python3 scripts/check_build56.py
bash scripts/check_payment_policy_java.sh
python3 -m pytest apps/video-worker/tests apps/recommendation-lab/tests -q

# Requires Docker + access to container/dependency registries:
docker compose config
docker compose up --build -d
cd apps/api && mvn test && cd ../..
cd apps/web && npm install && npm run typecheck && npm run build && cd ../..
python3 scripts/e2e_processing_live.py
python3 scripts/e2e_playback_live.py
python3 scripts/e2e_billing_discovery_live.py
```

**No Docker/Maven/npm full build or real Razorpay webhook delivery was executed when producing this archive.**

## Next build 07

1. Run the entire Build 01–06 stack with real dependency builds and correct any migrations, auth, and media integration defects.
2. Implement billing refunds, chargebacks, provider reconciliation, true paid-cycle handling and cancellation idempotency, plus comprehensive integration tests.
3. Connect trained Python recommendation inference behind the rights/age-gated Java API with offline evaluation and feature flags (optional).
4. Establish AWS IAM/VPC/ECS/RDS/private S3, signed CloudFront cookies and trustable territory policy; turn off local media proxy in production.
5. Add accessibility, privacy/delete/export requests, abuse protections, operational dashboards, disaster recovery and licensed-content launch review.
