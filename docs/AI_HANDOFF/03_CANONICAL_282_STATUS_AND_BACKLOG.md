# 03 — Canonical 282-task status and remaining work

**Canonical tracker:** `docs/BUILD23_282_TASKS.csv`. No historical numbers are to be silently reclassified. Every item is unique.

| Status | Count | Meaning |
|---|---:|---|
| Source implemented — runtime unverified | 67 | Code exists; no complete acceptance proof |
| Partially implemented | 119 | Some code exists, substantial behavior/QA remains |
| Blocked — local/Codex verification | 40 | Needs tools, true runtime, integration or device lab |
| Blocked — accounts/approvals | 17 | Needs AWS/provider account, licensed content, user decision or qualified reviewer |
| Not started / no audited evidence | 39 | No clear implementation proof; may require further review or development |
| **TOTAL** | **282** | **Fully acceptance-verified: 0** |

## Workstream breakdown (all 17 categories)

| Workstream | Total | Implemented | Partial | Local blocked | External blocked | No evidence |
|---|---:|---:|---:|---:|---:|---:|
| A. Build, repository & local runtime | 16 | 4 | 4 | 8 | 0 | 0 |
| B. Identity, security & user accounts | 15 | 0 | 8 | 6 | 1 | 0 |
| C. Catalog, metadata, rights & moderation | 15 | 4 | 10 | 0 | 1 | 0 |
| D. Creator upload & media processing | 18 | 6 | 10 | 1 | 0 | 1 |
| E. Netflix-style player & playback | 24 | 15 | 5 | 3 | 0 | 1 |
| F. DRM, entitlement & streaming security | 19 | 3 | 7 | 2 | 1 | 6 |
| G. Subscriptions & international payments | 17 | 3 | 7 | 1 | 2 | 4 |
| H. Search, recommendations & AI | 17 | 6 | 11 | 0 | 0 | 0 |
| I. Analytics & quality of experience | 13 | 5 | 8 | 0 | 0 | 0 |
| J. UI, accessibility & localization | 14 | 6 | 7 | 1 | 0 | 0 |
| K. Creator Studio & administration | 13 | 4 | 9 | 0 | 0 | 0 |
| L. AWS & worldwide distribution | 25 | 4 | 10 | 5 | 2 | 4 |
| M. Mobile, TV & offline viewing | 15 | 1 | 1 | 1 | 1 | 11 |
| N. Privacy, legal & compliance | 15 | 1 | 6 | 0 | 8 | 0 |
| O. Security & engineering quality | 18 | 4 | 8 | 5 | 1 | 0 |
| P. Reliability, performance & recovery | 15 | 1 | 5 | 7 | 0 | 2 |
| Q. Advanced innovation & monetization | 13 | 0 | 3 | 0 | 0 | 10 |

## How to work the remaining backlog

1. **Resolve blocking foundations first**: compiler/type errors, dependency lock, Docker, Flyway, authentication, rights, upload/playback, sandbox payment and staging. Never assume these passed merely because a script exists.
2. **Finish 119 partials** by meeting each task's complete functional scope and writing tests. Reclassification to "source implemented" still does not mean accepted.
3. **Inspect 39 no-evidence tasks** before implementation; do not assume no source exists just because the audit did not identify a matching file.
4. **Track the 57 blocked tasks separately** with named owners and external evidence requirements. Blocked tasks are not done.
5. **Every task** has `task_id`, owner, status before/after, files changed, expected acceptance, exact tests/evidence and unfinished items. Leave other task statuses unchanged.

## Full itemized remaining task list (all not source-implemented)

Below, descriptions and statuses are read from the original canonical Build 23 CSV, not invented.

### Partially implemented — 119 tasks

- **A-01** (Build, repository & local runtime): Inspect Build 13 branch and record immutable baseline commit
- **A-03** (Build, repository & local runtime): Review licenses, CVEs and deprecated libraries
- **A-13** (Build, repository & local runtime): Fix first actual runtime errors with regression tests
- **A-14** (Build, repository & local runtime): Establish private Git repository, branches and protected PR workflow
- **B-02** (Identity, security & user accounts): Verify OIDC PKCE, issuer, audience, JWKS rotation and token refresh
- **B-06** (Identity, security & user accounts): Enforce viewer, creator, moderator and admin roles server-side
- **B-08** (Identity, security & user accounts): Add device/session inventory and revoke-all-sessions control
- **B-09** (Identity, security & user accounts): Verify lockout, abuse throttling and stolen-token handling
- **B-10** (Identity, security & user accounts): Add independent viewer profiles and PIN-protected adult profile actions
- **B-11** (Identity, security & user accounts): Implement child ages, parental controls and mature-title restrictions
- **B-12** (Identity, security & user accounts): Add accessibility for authentication and account recovery
- **B-13** (Identity, security & user accounts): Validate OIDC logout and cache/session invalidation
- **C-01** (Catalog, metadata, rights & moderation): Normalize movies, series, seasons, episodes and trailers
- **C-03** (Catalog, metadata, rights & moderation): Add genre, language, cast, crew, artwork and release metadata
- **C-04** (Catalog, metadata, rights & moderation): Add unique content identifiers, slugs and canonical URLs
- **C-05** (Catalog, metadata, rights & moderation): Add posters, landscape art, banners and artwork rendition pipeline
- **C-06** (Catalog, metadata, rights & moderation): Add editorial categories, collections, featured rails and scheduling
- **C-08** (Catalog, metadata, rights & moderation): Persist rights holder identity and attached licence evidence
- **C-09** (Catalog, metadata, rights & moderation): Model rights by country, region, platform, tier and date window
- **C-10** (Catalog, metadata, rights & moderation): Add content ratings, content warnings and age-review process
- **C-12** (Catalog, metadata, rights & moderation): Enforce unpublish and rights revocation across search, CDN authorization and recommendations
- **C-13** (Catalog, metadata, rights & moderation): Add takedown request and appeal workflows
- **D-02** (Creator upload & media processing): Implement resumable interrupted uploads and safe retry
- **D-03** (Creator upload & media processing): Enforce ownership, maximum size, format and quota constraints
- **D-05** (Creator upload & media processing): Isolate FFmpeg/worker execution and constrain resources
- **D-09** (Creator upload & media processing): Generate optional DASH/CMAF renditions for multi-device playback
- **D-10** (Creator upload & media processing): Create thumbnails, posters, preview strips and sprite/VTT metadata
- **D-11** (Creator upload & media processing): Support alternate audio tracks and subtitle ingest
- **D-14** (Creator upload & media processing): Record processing progress, ETA, warnings and failure diagnosis
- **D-15** (Creator upload & media processing): Implement retries, dead-letter queues, cancellation and cleanup
- **D-16** (Creator upload & media processing): Validate package decoding, manifest correctness and all segments
- **D-18** (Creator upload & media processing): Add lifecycle policies for originals, renditions and failed uploads
- **E-03** (Netflix-style player & playback): Create responsive desktop, phone, tablet and TV player layouts
- **E-06** (Netflix-style player & playback): Implement audio track and subtitle/closed-caption selectors
- **E-10** (Netflix-style player & playback): Add Next Episode, episode picker and series season navigation
- **E-20** (Netflix-style player & playback): Tune startup latency, buffer size, ABR ladder and rebuffering controls
- **E-21** (Netflix-style player & playback): Support data-saver and optional audio-only accessibility mode
- **F-02** (DRM, entitlement & streaming security): Verify subscription, free tier, rentals and trials when configured
- **F-04** (DRM, entitlement & streaming security): Implement trusted country decision from edge/network signal, not client input
- **F-07** (DRM, entitlement & streaming security): Verify session renewal and revoked subscription/rights handling
- **F-08** (DRM, entitlement & streaming security): Implement CDN edge/token key rotation and private key custody
- **F-10** (DRM, entitlement & streaming security): Support concurrent stream leases and shared account device policy
- **F-11** (DRM, entitlement & streaming security): Add optional session anomaly detection and suspicious activity reviews
- **F-16** (DRM, entitlement & streaming security): Add device capabilities/fallbacks and verify license renewals
- **G-04** (Subscriptions & international payments): Handle captured, failed, pending, duplicate and out-of-order events
- **G-06** (Subscriptions & international payments): Verify trial, renewal, grace period and expired subscriptions
- **G-07** (Subscriptions & international payments): Verify cancellation now versus period-end behavior
- **G-09** (Subscriptions & international payments): Reconcile provider events against database and report discrepancies
- **G-11** (Subscriptions & international payments): Define trials, coupons, promotional credits and abuse prevention
- **G-12** (Subscriptions & international payments): Add invoices, tax information and payment failure communication
- **G-17** (Subscriptions & international payments): Verify PCI/payment scope; never store sensitive card credentials
- **H-01** (Search, recommendations & AI): Verify baseline keyword search, facets and synonyms
- **H-02** (Search, recommendations & AI): Improve multilingual and transliterated title discovery
- **H-05** (Search, recommendations & AI): Connect private recommendation service to live API with feature flag
- **H-06** (Search, recommendations & AI): Ensure unauthorized content never appears from ML fallback
- **H-09** (Search, recommendations & AI): Add semantic search with title/synopsis embeddings
- **H-10** (Search, recommendations & AI): Add collaborative filtering candidate generator after sufficient data
- **H-11** (Search, recommendations & AI): Add hybrid retrieval, reranking and exploration strategies
- **H-13** (Search, recommendations & AI): Train/update models with versioned features and drift monitoring
- **H-14** (Search, recommendations & AI): Set up online experiments, holdouts and unbiased metrics
- **H-16** (Search, recommendations & AI): Optional AI scene search and natural-language content discovery
- **H-17** (Search, recommendations & AI): Optional assisted subtitles/transcripts/dubbing with human QA
- **I-04** (Analytics & quality of experience): Implement active-users, conversions, retention, churn and watch time
- **I-05** (Analytics & quality of experience): Add movie/episode completion and drop-off funnels
- **I-07** (Analytics & quality of experience): Build creator dashboard for views, audience and revenue share
- **I-09** (Analytics & quality of experience): Add event deduplication, ingestion durability and replay
- **I-10** (Analytics & quality of experience): Implement exportable admin dashboards with RBAC
- **I-11** (Analytics & quality of experience): Add cohort analysis and acquisition attribution with consent
- **I-12** (Analytics & quality of experience): Set up data quality alerts, anomalies and schema-version handling
- **I-13** (Analytics & quality of experience): Benchmark streaming QoE by country/ISP/device
- **J-02** (UI, accessibility & localization): Unify original ReelNest brand identity and design tokens
- **J-06** (UI, accessibility & localization): Provide screen-reader labels, keyboard nav and WCAG audits
- **J-07** (UI, accessibility & localization): Provide Hindi/English localizations with translation architecture
- **J-08** (UI, accessibility & localization): Design RTL layout support for applicable languages
- **J-10** (UI, accessibility & localization): Add themed posters, trailers, preview thumbnails and editorial art
- **J-12** (UI, accessibility & localization): Add notifications, parental settings and device management UX
- **J-14** (UI, accessibility & localization): Add opt-in email/push notifications respecting consent
- **K-01** (Creator Studio & administration): Creator verification and contract upload review
- **K-02** (Creator Studio & administration): Creator content submission, resubmission and versioning
- **K-03** (Creator Studio & administration): Admin role separation, two-person approval for sensitive actions
- **K-05** (Creator Studio & administration): Rights calendar, expiration alerts and takedown controls
- **K-06** (Creator Studio & administration): Admin complaints, moderation decisions and appeal case management
- **K-07** (Creator Studio & administration): Creator processing dashboard, subtitles and poster editing
- **K-08** (Creator Studio & administration): Creator earnings statements and configurable revenue share
- **K-10** (Creator Studio & administration): Refund-support tools with auditable approval workflow
- **K-11** (Creator Studio & administration): Operations dashboard for worker failures and playback errors
- **L-04** (AWS & worldwide distribution): Configure separate dev/staging/prod AWS accounts or roles
- **L-08** (AWS & worldwide distribution): Configure IAM roles, Secrets Manager and key rotation
- **L-09** (AWS & worldwide distribution): Publish reproducible ECR container images and digest pins
- **L-10** (AWS & worldwide distribution): Configure managed Keycloak or supported OIDC runtime
- **L-12** (AWS & worldwide distribution): Configure Route 53 DNS, TLS/ACM and custom app/API/stream hostnames
- **L-16** (AWS & worldwide distribution): Implement country-specific catalog, tier and entitlement policies
- **L-18** (AWS & worldwide distribution): Set cost budgets, quotas, alerts and encoder spend safeguards
- **L-20** (AWS & worldwide distribution): Design global API latency and disaster recovery strategy
- **L-23** (AWS & worldwide distribution): Implement progressive delivery, rollback and environment promotion
- **L-24** (AWS & worldwide distribution): Set up streaming DNS, cache invalidation and emergency block controls
- **M-11** (Mobile, TV & offline viewing): Implement remote control focus map, D-pad and accessibility
- **N-02** (Privacy, legal & compliance): Complete account export and deletion across identity and database
- **N-03** (Privacy, legal & compliance): Respect billing/creator/audit record retention exemptions
- **N-04** (Privacy, legal & compliance): Implement deletion propagation to analytics, caches and object storage
- **N-05** (Privacy, legal & compliance): Define data retention schedule and backup deletion/expiry procedure
- **N-06** (Privacy, legal & compliance): Add consent, cookie tracking preferences and policy versioning
- **N-12** (Privacy, legal & compliance): Maintain security incident, legal hold and deletion audit logs
- **O-01** (Security & engineering quality): Create system threat model, assets inventory and trust boundaries
- **O-02** (Security & engineering quality): Enforce authentication on all private API/media routes
- **O-04** (Security & engineering quality): Test CSRF/CORS/cookie scope and browser sandboxing
- **O-05** (Security & engineering quality): Perform input validation, injection and XSS regression tests
- **O-07** (Security & engineering quality): Add SAST/SCA/dependency and secret scanning to CI
- **O-10** (Security & engineering quality): Review least-privilege IAM, SQL runtime privileges and admin MFA
- **O-17** (Security & engineering quality): Establish performance budgets for API, player and web app
- **O-18** (Security & engineering quality): Add regression tests for each fixed P0/P1 bug
- **P-03** (Reliability, performance & recovery): Verify durable worker retry/dead-letter/crash recovery
- **P-07** (Reliability, performance & recovery): Verify monitoring dashboards, on-call alerts and logging
- **P-09** (Reliability, performance & recovery): Implement readiness/liveness probes and graceful shutdown
- **P-12** (Reliability, performance & recovery): Configure retention and cost alarms for logs, storage and CDN
- **P-15** (Reliability, performance & recovery): Create reproducible launch audit/evidence package
- **Q-04** (Advanced innovation & monetization): Implement FAST-style scheduled linear channels
- **Q-05** (Advanced innovation & monetization): Implement watch parties with realtime synchronization and moderation
- **Q-08** (Advanced innovation & monetization): Provide AI search assistant grounded in licensed catalog

### Not started / no audited implementation evidence — 39 tasks

- **D-13** (Creator upload & media processing): Add 4K/HDR/HEVC/AV1 presets only for compatible devices/sources
- **E-17** (Netflix-style player & playback): Add Chromecast/AirPlay casting when platforms permit
- **F-12** (DRM, entitlement & streaming security): Design DRM packaging and SPEKE/license-server integration
- **F-13** (DRM, entitlement & streaming security): Integrate Widevine on supported browser/Android clients
- **F-14** (DRM, entitlement & streaming security): Integrate FairPlay for Safari/iOS/tvOS clients
- **F-15** (DRM, entitlement & streaming security): Integrate PlayReady for Windows/selected TV clients
- **F-17** (DRM, entitlement & streaming security): Implement rights-driven watermarking or forensic watermark vendor integration
- **F-18** (DRM, entitlement & streaming security): Test offline DRM licences where offline downloads are permitted
- **G-10** (Subscriptions & international payments): Add upgrade, downgrade, prorating and plan-change UX
- **G-13** (Subscriptions & international payments): Support subscriptions per country/currency only where provider allows
- **G-14** (Subscriptions & international payments): Implement international payment method/provider abstraction
- **G-16** (Subscriptions & international payments): Define paid film rentals/TVOD and purchase windows (optional)
- **L-17** (AWS & worldwide distribution): Measure CDN cache hit rate, bandwidth, misses and origin egress
- **L-19** (AWS & worldwide distribution): Configure origin shield/multi-region origins only after measured need
- **L-21** (AWS & worldwide distribution): Add regional API deployment, Route53 latency routing when justified
- **L-22** (AWS & worldwide distribution): Plan database cross-region replication and consistency/failover
- **M-02** (Mobile, TV & offline viewing): Create Android app with compatible playback/DRM framework
- **M-03** (Mobile, TV & offline viewing): Create iOS app with AVPlayer/FairPlay compatibility
- **M-04** (Mobile, TV & offline viewing): Test Android/iOS auth flows and deep links
- **M-05** (Mobile, TV & offline viewing): Implement mobile download manager only for authorized offline titles
- **M-06** (Mobile, TV & offline viewing): Handle expiration, key renewal and delete downloaded media
- **M-07** (Mobile, TV & offline viewing): Build Android TV / Google TV focus-based player UX
- **M-08** (Mobile, TV & offline viewing): Build Apple tvOS player and focus/navigation UX
- **M-09** (Mobile, TV & offline viewing): Add Samsung Tizen client where market warrants
- **M-10** (Mobile, TV & offline viewing): Add LG webOS client where market warrants
- **M-12** (Mobile, TV & offline viewing): Implement casting/deep-link handoff and device pairing
- **M-13** (Mobile, TV & offline viewing): Set up push notifications, privacy prompts and crash analytics
- **P-02** (Reliability, performance & recovery): Measure backup recovery-point and recovery-time objectives
- **P-10** (Reliability, performance & recovery): Use auto-scaling only after testing predictable thresholds
- **Q-01** (Advanced innovation & monetization): Introduce AVOD advertising with server/client insertion where permitted
- **Q-02** (Advanced innovation & monetization): Offer AVOD/SVOD/TVOD hybrid packages and experiments
- **Q-03** (Advanced innovation & monetization): Implement live events with separate ingest and low-latency pipeline
- **Q-06** (Advanced innovation & monetization): Add loyalty/rewards and safe personalization controls
- **Q-07** (Advanced innovation & monetization): Add multi-tenant white-label OTT capabilities if commercially justified
- **Q-09** (Advanced innovation & monetization): Add scene-level discovery and recommendation exploration
- **Q-10** (Advanced innovation & monetization): Add localization-quality translation workflows and assisted dubbing
- **Q-11** (Advanced innovation & monetization): Add 4K HDR/Dolby formats as device/licence support grows
- **Q-12** (Advanced innovation & monetization): Add ad measurement, fraud detection and privacy controls
- **Q-13** (Advanced innovation & monetization): Introduce advanced creator licensing marketplace and royalties

### Blocked — external execution / operator verification — 40 tasks

- **A-04** (Build, repository & local runtime): Regenerate and commit reviewed npm lockfile
- **A-05** (Build, repository & local runtime): Compile backend with real Maven dependencies
- **A-06** (Build, repository & local runtime): Run backend unit and integration tests
- **A-07** (Build, repository & local runtime): Typecheck and production-build Next.js
- **A-08** (Build, repository & local runtime): Build Docker images for API, frontend, worker and AI service
- **A-09** (Build, repository & local runtime): Validate Compose schema, profiles and dependencies
- **A-10** (Build, repository & local runtime): Start PostgreSQL, Keycloak, Redis, RabbitMQ, MinIO, API, frontend and ML services
- **A-11** (Build, repository & local runtime): Apply Flyway V1–V9 on clean database and test an upgrade database
- **B-01** (Identity, security & user accounts): Verify registration, login, email confirmation and account recovery
- **B-03** (Identity, security & user accounts): Remove seeded/demo accounts from staging and production
- **B-05** (Identity, security & user accounts): Enable MFA for administrators and sensitive accounts
- **B-07** (Identity, security & user accounts): Enforce per-resource ownership and IDOR protection
- **B-14** (Identity, security & user accounts): Test Keycloak outage, restart and identity recovery procedures
- **B-15** (Identity, security & user accounts): Review admin break-glass procedure and audit records
- **D-01** (Creator upload & media processing): Verify end-to-end direct multipart upload to private MinIO/S3
- **E-01** (Netflix-style player & playback): Test initial playback in Chrome, Edge, Firefox and Safari
- **E-02** (Netflix-style player & playback): Test Android Chrome and iOS Safari native HLS behavior
- **E-24** (Netflix-style player & playback): Verify stream session limits across browsers/devices
- **F-01** (DRM, entitlement & streaming security): Verify every playback API checks profile ownership
- **F-06** (DRM, entitlement & streaming security): Prove direct S3 and unauthorized CDN requests return denial
- **G-01** (Subscriptions & international payments): Compile and run billing service against local database
- **J-01** (UI, accessibility & localization): Verify every existing route and error state in the real app
- **L-01** (AWS & worldwide distribution): Verify local Docker integration before AWS deployment
- **L-02** (AWS & worldwide distribution): Validate Terraform fmt/init/validate with real providers
- **L-03** (AWS & worldwide distribution): Review Terraform plan, resource costs and IAM policies
- **L-11** (AWS & worldwide distribution): Deploy frontend, API, ML service and worker to staging
- **L-14** (AWS & worldwide distribution): Configure signed-cookies distribution and verified georestrictions
- **M-15** (Mobile, TV & offline viewing): Create a device/browser/DRM capability matrix and test lab
- **O-03** (Security & engineering quality): Test BOLA/IDOR against user/creator/admin account boundaries
- **O-09** (Security & engineering quality): Run DAST and external dependency/vulnerability review
- **O-12** (Security & engineering quality): Test CDN signed-cookie expiry, geo bypass and authorization revocation
- **O-15** (Security & engineering quality): Add Playwright happy-path and negative browser E2E flows
- **O-16** (Security & engineering quality): Add visual, keyboard, browser matrix and accessibility regressions
- **P-01** (Reliability, performance & recovery): Run backup/restore in disposable PostgreSQL environment
- **P-04** (Reliability, performance & recovery): Run read-only and controlled-write load tests in staging
- **P-05** (Reliability, performance & recovery): Test playback startup time and rebuffer ratios across networks
- **P-06** (Reliability, performance & recovery): Load test simultaneous sessions and subscription gate
- **P-08** (Reliability, performance & recovery): Test disk/storage, DB, broker and upstream outage behavior
- **P-11** (Reliability, performance & recovery): Test blue-green/canary deploy and safe rollback
- **P-14** (Reliability, performance & recovery): Perform chaos/failure injection in staging (not production)

### Blocked — external approval / account — 17 tasks

- **B-04** (Identity, security & user accounts): Configure production identity persistence, SMTP and secure TLS
- **C-15** (Catalog, metadata, rights & moderation): Review content rights and classification with qualified professionals
- **F-19** (DRM, entitlement & streaming security): Define DRM compliance and security commitments with licensors
- **G-02** (Subscriptions & international payments): Verify Razorpay TEST plan creation and checkout
- **G-15** (Subscriptions & international payments): Calculate appropriate local taxes, VAT/GST and refund rules with advisor
- **L-15** (AWS & worldwide distribution): Deploy CloudFront global distribution and verify real regional clients
- **L-25** (AWS & worldwide distribution): Perform canary pilot in one licensed territory before worldwide rollout
- **M-14** (Mobile, TV & offline viewing): Set up app signing, store listings, review and staged rollouts
- **N-01** (Privacy, legal & compliance): Review privacy policy, terms of use and applicable data laws
- **N-07** (Privacy, legal & compliance): Perform child data/age verification risk review
- **N-08** (Privacy, legal & compliance): Review OTT content classifications, grievance redressal and takedown
- **N-09** (Privacy, legal & compliance): Validate rights contracts for every country and streaming platform
- **N-10** (Privacy, legal & compliance): Review international taxes, trade/sanctions and payment availability
- **N-11** (Privacy, legal & compliance): Review accessibility commitments in target countries
- **N-13** (Privacy, legal & compliance): Run independent IP/trademark clearance for brand name and artwork
- **N-14** (Privacy, legal & compliance): Define creator payout and intellectual-property dispute process
- **O-13** (Security & engineering quality): Add pentest and independent security review before paid launch

