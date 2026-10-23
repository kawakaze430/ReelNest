# ReelNest — detailed unfinished work after Builds 09/10

**Release state: NOT PRODUCTION READY.** This document is intentionally detailed so no source-code milestone is mistaken for a tested, licensed or secure public launch.

## P0 — Must complete before even a real-money pilot

### 1. Run genuine builds and fix compilation errors (BLOCKED)
- CI host with network access and Java 21: `cd apps/api && mvn --batch-mode verify`; repair all Java/Spring dependency, integration, JPA entity, Liquibase/Flyway or API contract failures.
- Node 22 + npm dependency install: `cd apps/web && npm install && npm run typecheck && npm run build`; fix any React/Next/TypeScript errors. Generate and commit a reviewed lockfile, then use `npm ci` in CI/Docker.
- `docker compose up --build -d` then `scripts/live_smoke.py`; check keycloak login, PostgreSQL V1–V8 migrations, MinIO permissions and FFmpeg worker job claims. Verify clean start and upgrade from Build 08 DB backup.
**Accept when:** fresh deploy and upgrade complete, no failed migrations, 8/8 read-only smoke checks, CI Maven and Next jobs green.

### 2. Creator → video → publication → viewer E2E (BLOCKED)
- Test multipart upload from browser; interrupted/resumed upload, 5GB max, checksums, corrupted file and ownership checks.
- Confirm worker acquires and releases leases, decodes 360/480/720p HLS, produces correct Content-Type and playlist paths in private S3.
- Verify admin content review, rights document evidence, territory expiry and revocation; only processed/approved film can appear in catalog and play.
- Test viewer profiles, age restrictions, auth on every private media segment, watch progress and 2-stream limit under concurrent requests.
**Accept when:** an original licensed sample file runs through the whole workflow in a real Docker stack; negative authorization cases denied.

### 3. Razorpay TEST payments (BLOCKED)
- Add TEST merchant keys, plan IDs, webhook secret and registered local/public callback endpoints; do not paste secrets into repository.
- Test payment captured, failed, pending, renewal, paused, cancel-at-cycle-end, refunded, partial refund, dispute opened/closed, duplicate/out-of-order webhook, network timeout, provider retries and reconciliation against live TEST provider records.
- Review subscription accounting and plan benefit design. Validate exact webhook event fields and when captured charge is linked to a subscription period. Build a support flow to resolve disputes and responsibly backfill historical charges affected by V8 entitlement reset.
- Tax treatment, refunds, consumer cancellation policy, payment fees and invoicing require a specialist review.
**Accept when:** independently verified TEST payment grants only correct entitlement and all other scenarios fail closed, without manual DB edits.

### 4. Production identity and administrator controls (BLOCKED)
- Provision externally reachable HTTPS OIDC/Keycloak; persistent realm, client registration, identity recovery, secure MFA and admin break-glass. Verify issuer/JWKS consistency, token refresh, logout, revoked/suspended users, session theft and browser login flows.
- Verify role/ownership authorization for creators/admin/viewers and proper parental protections and age verification for mature content.
**Accept when:** penetration and E2E tests prove role isolation; no Keycloak development passwords in any production configuration.

### 5. AWS staging deployment + geography (BLOCKED)
- Run `terraform init -backend=false && terraform validate` then remote-state `terraform plan` and manual peer/security/cost review on real provider. Verify networking, region, ACM, IAM policies, ECR images, RDS connectivity, DNS, HTTPS, CloudFront OAC and keys, S3 buckets, WAF and logging.
- Runtime DB secret and role must be provisioned (`scripts/sql/provision_runtime_role.sql`) after migrations; no database master secret to API/worker. Explicitly test application cannot issue DDL.
- Compile Next.js with real build-time HTTPS URLs using `scripts/build_production_web.sh` (and attach image digest); never deploy a localhost-baked image.
- Test CloudFront single-country whitelist and bypass cases. IP-based geofencing is not a trusted location assertion and doesn't eliminate VPN tunneling. Multi-territory rights require authenticated trusted edge signalling or equivalent enforcement. Do not set `reviewed_geo_enforcement=true` until real behavior reviewed.
- Verify signed cookies, short TTL, renewal, revoked rights/payment expiry, direct-S3 denial, CDN cache rules and differences on Safari/iOS. Add DRM if licensor requires it.
**Accept when:** staging works fully with real cloud providers, geo/access and revocation negatives passed, and Terraform plan reviewed.

### 6. Legal and data-protection clearance (BLOCKED)
- Secure signed licences for **every** film/poster/trailer/audio/subtitle plus rights term and territory. Check classification, parental controls, takedown and complaints under applicable Indian laws.
- Have counsel review Terms, Privacy Notice, DPDP compliance timeline, retention schedule, consumer refunds, grievance redressal, tax/invoices, creator agreements and music rights.
- Implement actual cross-system deletion (PostgreSQL, Keycloak, object storage, queues, exports, logs, backups subject to lawful retention), and verifiable completion receipts. Current DELETE request is review-only; it **cannot be marked fulfilled** via API.
**Accept when:** verified licences and counsel review documented, privacy deletion/export complete without a false-completion claim.

## P1 — Reliability and operational readiness before scaled rollout

### 7. Observability and on-call
- Add playback startup/buffering/error QoE events validated server-side, user privacy limits, dashboards, tracing and managed log retention.
- Build measurable alerts for 5xx, video processing stall/dead-letter, failed webhook, subscription drift, CloudFront errors, expiring rights, budget and backup health. Test pager delivery and runbooks.

### 8. Capacity, failure and disaster-recovery tests
- Run realistic k6 API/load and concurrency tests, creator large-upload tests, CDN transfer checks, DB pool saturation, queue crashes, worker lease expiry and geographic access tests.
- Restore RDS to staging, document recovery point/time, verify upload bucket recovery and encrypted backups, test rollback/canary deployments. No measured SLA or DAU capacity claim without evidence.

### 9. Supply-chain hardening
- Commit actual npm lockfile and pin npm Docker dependencies, review container base image digests, scanning, SBOM, dependency/security updates, and GitHub branch protection.
- Implement isolated CI staging with real Postgres/MinIO and Playwright for login/upload/playback. Pin Terraform/AWS-provider versions and add a CI plan policy.

## P2 — Product enhancements (after safe foundation)

### 10. Recommendation ML online serving
- Current backend ranking is rules-based; the separately tested Python TF-IDF/collaborative lab is **not** connected to the live API. Add versioned trained model, retraining pipeline, feature quality, offline evaluation and A/B monitoring, rights-aware inference and fallback.

### 11. OTT product completeness
- Multi-episode series organization, multi-language subtitle workflows, trailers, notifications, accessibility captions and TV/mobile screen testing; dedicated Android, iOS and smart-TV clients later.
- Creator reporting/payout agreements, revenue share ledger, refunds and invoice export, subscription upgrades/proration and legally compliant creator tax reporting.
- DRM, content watermarking, forensic watermarking or offline rights-controlled playback where rights owner requires it.

### 12. Real production launch & operational ownership
- Register domain and trademarks, finalized branding/assets, signed platform policies, admin MFA, trusted support process and privacy contacts. Set financial budgets and staffed escalation.
- Controlled pilot with licensed originals, monitor player failures and cancellation rates, then staged increase in traffic. Define rollback threshold and launch signoff.

## Clear overall accounting

- **Source development**: modules for catalog, creator, rights, upload, HLS worker, playback, billing, basic recommendations, analytics, cloud scaffold and security/privacy review exist across Builds 01–10.
- **Proved in this execution environment**: independent policy tests, FFmpeg/media tests, offline ML tests, TypeScript syntax parse, static source checks, URL validation tests and archive integrity.
- **Not proved**: complete Java + Next.js builds, live migrations and Docker, Razorpay provider, CDN, Terraform provider, account deletion, real identity provider and traffic/load/legal readiness.
- **Launch verdict**: NO-GO. An offline PASS is never evidence that services are integrated or production-safe.
