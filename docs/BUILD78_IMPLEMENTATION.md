# ReelNest Builds 07 + 08 — Implementation and evidence

This checkpoint is a **complete cumulative copy of Builds 01–08**, not a new repository. It adds real source code, Terraform staging infrastructure and quality gates. It has **NOT** been built/deployed end-to-end or shown suitable for public billing.

## Build 07: integration, payments, CDN, cloud

**Changed:**

- `V8__billing_disputes_and_privacy.sql`: per-transaction `entitled_until`, refund amounts, dispute holds, last reconciliation; previous paid-through values invalidated instead of inventing historical billing cycles. **After upgrading an existing database, old paid subscribers must be reviewed and reconciled before entitlement is restored.**
- `ChargePolicy`: a charge is eligible only while charged, unrefunded, undisputed and within a valid cycle. No premium entitlement is conferred by payment callback UI or unverified provider responses.
- `BillingService`: signed webhook ingestion supports `subscription.*`, `payment.refunded`, `refund.created`, `refund.processed`, and `payment.dispute.*` (subject to actual Razorpay payload tests). Captured charges are confirmed through provider payment GET before recording; reconciliation recomputes paid-through from valid transactions. A pending refund and any dispute conservatively hold access.
- `RazorpayGateway.getPayment()`, `BillingReconciliationJob` (operator-enabled, bounded scheduled polling). Event order, provider trial and discount semantics, duplicate webhooks and refund/dispute closure need provider sandbox acceptance tests and a policy review. No automated hold clearance after a dispute.
- `CdnCookieSigner`, `PlaybackController`, `PlaybackService`, `protected-player.tsx`: optional CloudFront custom-policy signed cookies with 120-second expiry and renewal at session heartbeats; path-scoped to the processed media rendition prefix. Web API sessions send credentials; hls.js sends cookies to CDN. Old bearer-auth local proxy continues for Docker **only**.
- **Territory safety:** CloudFront playback defaults to **disabled** until the operator explicitly attests to a trusted geo integration (`PLAYBACK_CDN_GEO_REVIEWED`), and that integration has **not been built** here. Merely setting the flag without a real verified edge signal would be unsafe. CloudFront signed cookies are authorization, not DRM.
- `StorageConfig`, Python media worker: AWS IAM task roles/default credential chain are supported when static MinIO credentials and custom S3 endpoints are absent.
- `apps/web/Dockerfile`, `next.config.ts`: production standalone Next.js Docker stage alongside local `dev` stage. Public Next.js environment variables must be baked at production image build time.
- `infrastructure/aws`: **unvalidated staging Terraform scaffold** for VPC, NAT, private subnets, RDS PostgreSQL, private S3 + OAC, CloudFront trusted key group, ECS/Fargate, ECR, ALB/ACM HTTPS, Route53, ElastiCache Redis, IAM, WAF basic IP rate rule, CloudWatch logs and 5xx alarm. Services default off; Terraform creation **still incurs costs** for other managed services if applied. TLS certs, custom domain, public key, container images and OIDC IdP must be supplied. Secrets Manager private key is created as an EMPTY secret and must be populated OUTSIDE Terraform.

## Build 08: security, privacy, testing and operations

- `PrivacyController`: authenticated application-data JSON export (scoped to self), duplicate-safe deletion review requests, admin-only review/decision API. **Deletion requests DO NOT delete anything**. Manual privacy, Keycloak, finance retention, creator-owned assets, and operator records are not implemented.
- `RequestSecurityHeaders`: request correlation ID (untrusted IDs validated), anti-sniff/frame/referrer headers and no-store policy for personal financial/account routes.
- `AdmissionRateLimiter`: Redis-backed, atomic, per-authenticated-principal one-minute quota for checkout creation, playback session creation and deletion requests. Fail-closed on cache failure. Public endpoints/large uploads have no comprehensive DDoS controls yet; WAF scaffold only.
- `SecurityConfig`: explicit CORS origin with credentials needed for CDN signed cookies; bearer-token protected APIs remain stateless.
- `apps/web/src/app/account/privacy/page.tsx`: account export + deletion review UI; navigation updated.
- GitHub Actions includes Maven verification, Next production build, worker/ML tests, static checks and Terraform provider validation. These GitHub jobs were **written**, not executed here.

## Tests performed in authoring container

- Historical `scripts/check_static.py`, `check_build02_security.py`, `check_build03.py`, `check_build04.py`, `check_build56.py`: pass.
- New `scripts/check_build08.py` incl compiled independent Java reversible-entitlement policy: pass.
- `scripts/check_payment_policy_java.sh`: pass.
- `pytest apps/video-worker/tests apps/recommendation-lab/tests -q`: 11 pass.
- YAML parse of Compose, Spring config and workflow: pass.
- TypeScript syntax parse using globally installed TypeScript: 37 app source files, 0 errors (only `apps/web/src` is parsed; MPEG-TS `.ts` files under `public/sample/hls` are binary video, not TypeScript).
- Java source lexical/syntax scan with `javac`: no Java parser diagnostics detected; missing Maven dependency classpath yielded missing-class errors. This is **not** backend compilation.

## Tests not performed and why

There is no working Maven installation, Docker engine, Terraform binary, Maven/Next dependencies cache or network DNS access for package registries in this authoring environment. Therefore these are UNVERIFIED: `mvn verify`, full `npm run typecheck && npm run build`, Terraform `init && validate`, actual Flyway V1-V8 upgrades against PostgreSQL, Compose Keycloak/startup, actual S3 uploads/CloudFront signing, Razorpay sandbox/refund events, live video playback, privacy persistence and public cloud deployment.

## Launch blockers (must resolve in next continuation)

1. **Full compile and E2E**: run `mvn verify`, `npm install && npm run typecheck && npm run build`, `terraform init -backend=false && terraform validate`, `docker compose up --build -d`, Keycloak, Flyway V1–V8, smoke and upload/encode/watch tests, fix EVERY failing integration.
2. **Payments**: Razorpay TEST subscription, delayed and duplicate webhooks, captured amount/currency, refunds, partial refunds, disputes, cancellation, renewal and reconciliation using actual sample provider payloads. Need explicit entitlement policy for coupons/proration, failures, billing invoices/tax, and revenue reconciliation. No live merchant money until certified.
3. **Territory**: implement a trusted edge-derived country/geo path with spoofing protection and legal restrictions; replace hardcoded country and review. CDN currently blocked by default. DRM and licence-holder contractual requirements may also be mandatory.
4. **AWS**: validate and review Terraform, stage deploy with real OIDC and domain/cert/image inputs, verify IAM least privilege, keep private S3, secrets outside state, enable alarms/backup recovery. RDS currently uses the managed **master user** for API; switch to least-privileged runtime DB role. Terraform is staging scaffolding, not production reviewed.
5. **Privacy/legal**: deletion automation/Keycloak coordination, identity export, retention policy, genuine grievance process, account billing compliance, tax/legal/OTT classification, rightsholder contracts, customer support.
6. **Scale**: CDN cookies across browsers/sessions, observability, concurrency load tests, regional egress, realistic pricing costs, edge abuse and DDoS protection, security testing, independently verify HLS protocol/DRM.

Full deployment instructions: `docs/DEPLOYMENT_AND_LAUNCH.md`, response playbooks: `docs/OPERATIONS_RUNBOOK.md`.
