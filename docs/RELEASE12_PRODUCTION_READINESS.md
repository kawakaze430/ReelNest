# ReelNest Release 12 — engineering evidence & production readiness

## Release verdict: **NO-GO for public paid OTT**

This archive includes the prior cumulative builds and the incremental implementation listed here. Passing offline tests **does not** establish that the service compiles, runs under Docker, is deployed on AWS, meets DRM licences or satisfies content-licensing/Indian regulatory requirements. No sandbox Razorpay payment, provider refund, actual AWS account, CloudFront distribution, Keycloak admin identity removal or genuine content rights have been tested here.

### Implemented in Release 12

1. **Optional live-API ML reranking**: a private Python FastAPI service accepts only Java-authorized candidate records, holds per-request ephemeral interaction context, requires a strong shared secret, returns IDs/scores, and exposes `/health`; the Java backend checks returned IDs against the authorized candidate set, rejects duplicates, falls back to the existing ranking if ML fails, and preserves Java playback authorization. This is a first live ranking integration, **not** trained long-term collaborative personalization or measured accuracy improvement.
2. **Eligibility-constrained viewer erasure**: administrator-triggered free-viewer deletion locks the account, refuses accounts with creator, billing, or audit obligations, calls Keycloak's admin API using explicitly configured client credentials, erases local viewing data, records a SHA-256 identity tombstone, and marks the deletion request fulfilled only after both operations succeed. If the remote call or transaction fails the account remains locked for supervised recovery. Migration V9 changes privacy request FK semantics. **This has not been end-to-end tested with a real Keycloak and PostgreSQL instance.** Retention requirements and additional external data holders must be reviewed.
3. **Mobile-friendly web install manifest and Safari CDN playback**: mobile/PWA manifest and original SVG mark; native HLS is allowed only on the cookie-authorized CDN path (never the local bearer-token media proxy). Video keyboard shortcuts support arrows, k/space, and m. This is **not** an iOS/Android or TV-native application, offline download implementation, or licensed DRM.
4. **Repeatable verification automation**: `scripts/validate_local_stack.sh --smoke|--full`, `scripts/test_restore_disposable.sh`, manual GitHub Actions integration workflow, Python unit/contract tests, and the updated offline release gate.
5. **Staging Keycloak realm generator**: removes bundled demo users, switches on email verification and brute-force protection, restricts redirect origins to HTTPS and disables public registration until SMTP is configured. This is a template, **not** a deployed/secure production realm; admin MFA, email and security audit remain outstanding.

## Task-by-task remaining work (user-requested scope)

| Priority | Scope | Code/verification done | External blocker / completion definition |
|---|---|---|---|
| P0 | Compile Java and Next.js with real dependencies | Existing compilation CI job; TS **syntax** checks only | Run `mvn verify`, `npm install`, `npm run typecheck`, `npm run build` on connected runner. Fix any actual failures; retain machine logs. |
| P0 | Start all Docker services and run V1–V9 migrations | Compose now includes internal ML service; YAML parsed | Execute `bash scripts/validate_local_stack.sh --smoke` with Docker; confirm Flyway V1–V9 and readiness. |
| P0 | Full creator upload → HLS → playback | Existing Playwright integration scripts & original owned sample; standalone media tests | Run `bash scripts/validate_local_stack.sh --full` on a **fresh disposable** stack; fix issues and capture proof of browser uploads, publication, session/authz & playback. |
| P0 | Identity | Restricted staging realm generator; guarded viewer erasure implemented | Configure DNS/TLS, Keycloak service identity, verified email/SMTP, admin MFA, secure secrets. Prove OIDC login/logout and erasure with live Keycloak/PostgreSQL; remove dev identities. |
| P0 | Razorpay TEST billing | Prior code handles verified webhook/entitlements, reversal and dispute states | Supply merchant TEST keys, correct Razorpay plans/webhook URL. Capture actual provider sandbox transaction/webhooks/refund/dispute/renewal/out-of-order tests. Never switch to live payments before provider verification. |
| P0 | Validate and deploy AWS staging | Terraform source, deployment preflight, budget/runbooks present | With AWS account, Route53 zone, two ACM certificates, secrets and ECR images: run `terraform init -backend=false`, `terraform validate`, `terraform plan` and reviewed `apply`; validate ECS, RDS, CloudFront, VPC, rollback; never assume code is applied. |
| P0 | CloudFront signed access & rights | RSA signer tested offline; single-country CloudFront georestriction Terraform scaffold | Test signed cookie expiry, media path scoping, origin OAC deny-public, bypass attempts and country restriction **in actual CloudFront**. Server-side trusted country source and multi-territory support still require design/security review. |
| P0 | Privacy deletion | Limited free-viewer workflow and audit-preserving migration added | Live end-to-end test; exception/retention procedure for subscribers/creators/admins; confirm all external processors/storage and backups follow retention policy. |
| P0 | Security & content legal review | Earlier security and rights approval guard source; static checks | Independent threat model, dependency/SAST/DAST and manual auth review, licensed content agreements, DRM requirements, Indian OTT/consumer/DPDP/privacy review by qualified professionals. |
| P1 | Backup restore & performance | Added disposable PostgreSQL dump/restore test; existing safe read-only k6 scenario | Execute tests against isolated staging, record RTO/RPO, video QoE and load, alert/paging verification, autoscaling and rollback. |
| P2 | Live ML model | Internal HTTP ranker integrated with rights-filtering and API tests | Validate on actual staging traffic; offline metrics & privacy review, model lifecycle training/evaluation, canary/feature flag and production resource sizing. |
| P2 | Mobile/TV apps, advanced DRM | Installable web manifest, keyboard accessibility and Safari native CDN HLS branch | Android/iOS/TV native clients **not built**. Licensed Widevine/FairPlay/PlayReady packaging/license service, device entitlement and compliance tests **not implemented**. |

### Local reproducible commands

Run from extracted `reelnest/` root on an internet-connected computer with Java 21, Node 22, Python, Docker Compose, Playwright/Chromium, FFmpeg and (for AWS) Terraform CLI:

```bash
python3 scripts/release_gate.py --mode offline --output docs/BUILD12_OFFLINE_REPORT.json
cd apps/api && mvn verify && cd ../..
cd apps/web && npm install --no-audit --no-fund && npm run typecheck && npm run build && cd ../..
bash scripts/validate_local_stack.sh --smoke
# Only on an unused/disposable local database: e2e fixtures assume a fresh creator.
bash scripts/validate_local_stack.sh --full
bash scripts/test_restore_disposable.sh
```

Run `terraform init -backend=false && terraform validate` in `infrastructure/aws` after installing the Terraform CLI and having network access. Review any provider and billable-resource errors, do not blindly apply. The existing `docs/AWS_DOMAIN_AND_DEPLOYMENT.md` contains the domain, TLS and ECR steps.

### Staging identity template

```bash
python3 scripts/generate_keycloak_staging_realm.py \
  --web-origin https://app.YOURDOMAIN.com \
  --output build-evidence/keycloak-staging-realm.json
```

Import into a **separate staging Keycloak realm**, not the demo realm. Do not copy demo users. The `PRIVACY_KEYCLOAK_ADMIN_URL`, `PRIVACY_KEYCLOAK_REALM`, `PRIVACY_KEYCLOAK_CLIENT_ID`, and `PRIVACY_KEYCLOAK_CLIENT_SECRET` variables are mandatory to enable the free-viewer erasure operation. Provision a tightly scoped Keycloak service account with only the necessary user-management permissions. Do not put credentials in Terraform state or the frontend.

### Proof not available from this environment

- Docker daemon, Maven, Terraform, live PostgreSQL/MinIO/Keycloak cluster and cloud credentials were unavailable. npm registry DNS was unavailable, so dependency installation and real `next build` were not possible.
- No Razorpay TEST merchant key or real payment event was available.
- No rights grant for third-party movies, mobile/TV platform credentials, DRM provider licenses, or legal sign-off was supplied.

**Deployment choice:** the static preview inside `deploy/reelnest-amplify-preview.zip` can be made public independently. Keep the real subscription and streaming APIs behind staging access controls until the above P0 release gates pass.
