# ReelNest — Complete Master Implementation & QA Checklist

**Planning baseline:** Build 18 cumulative ZIP reconciled against Builds 13–17 (source inspected). **Release state:** NO-GO for public paid OTT.

**Vision:** A Netflix-class streaming experience with legally licensed content, worldwide distribution where rights permit, subscriptions, creator marketplace, accessible apps, multi-DRM and measurable reliability. 'All tasks' is a defined practical backlog rather than a guarantee of every future business integration.

**Priority:** P0 public paid launch blocker; P1 worldwide competitive quality; P2 platform/device expansion; P3 optional experiments. DRM, territory, privacy and regional tax requirements escalate to P0 whenever an intended market/licence requires them.

**Status baseline (2026-10-22):** Builds 14–18 add player controls, QoE, protected previews, subtitles, alternate embedded audio, and offline ML evaluation. Full Maven/Next.js compilation, complete Docker integration, real Razorpay verification, Terraform provider validation, AWS staging and content-rights review remain unverified. Native mobile/TV applications and commercial multi-DRM remain unimplemented. All 282 acceptance boxes are unchecked because source-only evidence is not acceptance sign-off.

Owner codes: ME = ChatGPT writes/reviews source + offline tests in an active turn; LOCAL = user runs commands on own PC; CODEX = Codex/Astra handles hard terminal/runtime work; YOU = user account/financial/product permission; PRO = qualified legal/security/payment specialist. Section owner denotes primary contributors; individual tasks may require more than one. A checkbox means proven complete only after the tests and evidence are attached; existing source is NOT a checked box.

## Gates and release order

- **G0 local build:** Maven verify, npm typecheck/build, Compose up, V1–V14 migrations, auth smoke; no external credentials.
- **G1 verified vertical slice:** original creator film uploaded, validated, HLS packaged, approved, found, authorized, played and resumed; denied cases tested.
- **G2 staging:** full AWS infrastructure applied with approval; sandbox payments; CDN/geo/DRM where needed; controlled E2E, backup restoration, observability.
- **G3 licensed country pilot:** legal rights, security review, monitored small rollout, support/refunds, app/browser/accessibility testing.
- **G4 worldwide eligible territories:** per-title rights and payment/tax matrix; CloudFront global edge, QoE country tests, accessibility/localization, regional resilience; add new countries in approved batches.
- **G5 premium features:** 4K/HDR, multiple native device apps, advanced ML, licensed offline DRM, optional live/advertising.

**Implementation policy:** Every iteration runs unit/static checks; relevant integration tests before merging; nightly browser+system checks in CI; staging release regression, performance, security, backup/restore and compliance before production. P0 incident = stop release/fix immediately, P1 = fix before affected milestone, P2/P3 = backlog and scheduled triage.

## A. Build, repository & local runtime — P0 — lead: ME/LOCAL/CODEX (16 tasks)

- [ ] **A-01** Inspect Build 13 branch and record immutable baseline commit
- [ ] **A-02** Create dependency inventory and supported-version matrix
- [ ] **A-03** Review licenses, CVEs and deprecated libraries
- [ ] **A-04** Regenerate and commit reviewed npm lockfile
- [ ] **A-05** Compile backend with real Maven dependencies
- [ ] **A-06** Run backend unit and integration tests
- [ ] **A-07** Typecheck and production-build Next.js
- [ ] **A-08** Build Docker images for API, frontend, worker and AI service
- [ ] **A-09** Validate Compose schema, profiles and dependencies
- [ ] **A-10** Start PostgreSQL, Keycloak, Redis, RabbitMQ, MinIO, API, frontend and ML services
- [ ] **A-11** Apply Flyway V1–V9 on clean database and test an upgrade database
- [ ] **A-12** Validate configuration/env contracts between containers
- [ ] **A-13** Fix first actual runtime errors with regression tests
- [ ] **A-14** Establish private Git repository, branches and protected PR workflow
- [ ] **A-15** Centralize environment examples without committing secrets
- [ ] **A-16** Produce reproducible clean-machine setup guide

## B. Identity, security & user accounts — P0 — lead: ME/CODEX (15 tasks)

- [ ] **B-01** Verify registration, login, email confirmation and account recovery
- [ ] **B-02** Verify OIDC PKCE, issuer, audience, JWKS rotation and token refresh
- [ ] **B-03** Remove seeded/demo accounts from staging and production
- [ ] **B-04** Configure production identity persistence, SMTP and secure TLS
- [ ] **B-05** Enable MFA for administrators and sensitive accounts
- [ ] **B-06** Enforce viewer, creator, moderator and admin roles server-side
- [ ] **B-07** Enforce per-resource ownership and IDOR protection
- [ ] **B-08** Add device/session inventory and revoke-all-sessions control
- [ ] **B-09** Verify lockout, abuse throttling and stolen-token handling
- [ ] **B-10** Add independent viewer profiles and PIN-protected adult profile actions
- [ ] **B-11** Implement child ages, parental controls and mature-title restrictions
- [ ] **B-12** Add accessibility for authentication and account recovery
- [ ] **B-13** Validate OIDC logout and cache/session invalidation
- [ ] **B-14** Test Keycloak outage, restart and identity recovery procedures
- [ ] **B-15** Review admin break-glass procedure and audit records

## C. Catalog, metadata, rights & moderation — P0 — lead: ME/YOU (15 tasks)

- [ ] **C-01** Normalize movies, series, seasons, episodes and trailers
- [ ] **C-02** Add creator-facing metadata validation and draft workflow
- [ ] **C-03** Add genre, language, cast, crew, artwork and release metadata
- [ ] **C-04** Add unique content identifiers, slugs and canonical URLs
- [ ] **C-05** Add posters, landscape art, banners and artwork rendition pipeline
- [ ] **C-06** Add editorial categories, collections, featured rails and scheduling
- [ ] **C-07** Add search visibility rules and availability windows
- [ ] **C-08** Persist rights holder identity and attached licence evidence
- [ ] **C-09** Model rights by country, region, platform, tier and date window
- [ ] **C-10** Add content ratings, content warnings and age-review process
- [ ] **C-11** Implement reviewer approval/rejection and audit trails
- [ ] **C-12** Enforce unpublish and rights revocation across search, CDN authorization and recommendations
- [ ] **C-13** Add takedown request and appeal workflows
- [ ] **C-14** Implement metadata import/export and bulk operations
- [ ] **C-15** Review content rights and classification with qualified professionals

## D. Creator upload & media processing — P0 — lead: ME/CODEX (18 tasks)

- [ ] **D-01** Verify end-to-end direct multipart upload to private MinIO/S3
- [ ] **D-02** Implement resumable interrupted uploads and safe retry
- [ ] **D-03** Enforce ownership, maximum size, format and quota constraints
- [ ] **D-04** Inspect input with ffprobe and reject malformed/disguised media
- [ ] **D-05** Isolate FFmpeg/worker execution and constrain resources
- [ ] **D-06** Implement queue-claim leases and duplicate-job idempotency
- [ ] **D-07** Generate aligned ABR renditions 360p/480p/720p/1080p
- [ ] **D-08** Generate HLS master/media manifests with valid MIME and paths
- [ ] **D-09** Generate optional DASH/CMAF renditions for multi-device playback
- [ ] **D-10** Create thumbnails, posters, preview strips and sprite/VTT metadata
- [ ] **D-11** Support alternate audio tracks and subtitle ingest
- [ ] **D-12** Add caption/transcript validation and timed-text conversion
- [ ] **D-13** Add 4K/HDR/HEVC/AV1 presets only for compatible devices/sources
- [ ] **D-14** Record processing progress, ETA, warnings and failure diagnosis
- [ ] **D-15** Implement retries, dead-letter queues, cancellation and cleanup
- [ ] **D-16** Validate package decoding, manifest correctness and all segments
- [ ] **D-17** Prove publish gate requires encoded media and approved rights
- [ ] **D-18** Add lifecycle policies for originals, renditions and failed uploads

## E. Netflix-like video player & viewing UX — P1 — lead: ME/CODEX (24 tasks)

- [ ] **E-01** Test initial playback in Chrome, Edge, Firefox and Safari
- [ ] **E-02** Test Android Chrome and iOS Safari native HLS behavior
- [ ] **E-03** Create responsive desktop, phone, tablet and TV player layouts
- [ ] **E-04** Improve play, pause, seek, jump 10s, volume, mute and fullscreen
- [ ] **E-05** Implement adaptive quality auto-switching and manual quality selector
- [ ] **E-06** Implement audio track and subtitle/closed-caption selectors
- [ ] **E-07** Implement subtitle styling: language, font size, contrast and positioning
- [ ] **E-08** Add persistent playback speed controls
- [ ] **E-09** Implement accurate Continue Watching and multi-device resume
- [ ] **E-10** Add Next Episode, episode picker and series season navigation
- [ ] **E-11** Add Skip Intro, Skip Recap and Skip Credits with metadata cues
- [ ] **E-12** Implement Up Next countdown and disable-autoplay accessibility preference
- [ ] **E-13** Add spoiler-safe timeline previews and thumbnail scrubbing
- [ ] **E-14** Add keyboard controls, focus rings and screen-reader descriptions
- [ ] **E-15** Add touch gestures and orientation/rotation handling
- [ ] **E-16** Implement picture-in-picture where supported
- [ ] **E-17** Add Chromecast/AirPlay casting when platforms permit
- [ ] **E-18** Gracefully recover network loss, buffering and expired authorization
- [ ] **E-19** Provide playback error codes, retry action and user-facing diagnostics
- [ ] **E-20** Tune startup latency, buffer size, ABR ladder and rebuffering controls
- [ ] **E-21** Support data-saver and optional audio-only accessibility mode
- [ ] **E-22** Handle subtitle/audio language preference per profile
- [ ] **E-23** Capture playback QoE events with consent and privacy limits
- [ ] **E-24** Verify stream session limits across browsers/devices

## F. DRM, playback entitlement & anti-abuse — P0/P1 — lead: ME/CODEX/YOU (19 tasks)

- [ ] **F-01** Verify every playback API checks profile ownership
- [ ] **F-02** Verify subscription, free tier, rentals and trials when configured
- [ ] **F-03** Verify publication, age, territory and rights dates on every session
- [ ] **F-04** Implement trusted country decision from edge/network signal, not client input
- [ ] **F-05** Generate short-lived scoped CloudFront signed cookies/URLs
- [ ] **F-06** Prove direct S3 and unauthorized CDN requests return denial
- [ ] **F-07** Verify session renewal and revoked subscription/rights handling
- [ ] **F-08** Implement CDN edge/token key rotation and private key custody
- [ ] **F-09** Prevent manifest and segment authorization bypass
- [ ] **F-10** Support concurrent stream leases and shared account device policy
- [ ] **F-11** Add optional session anomaly detection and suspicious activity reviews
- [ ] **F-12** Design DRM packaging and SPEKE/license-server integration
- [ ] **F-13** Integrate Widevine on supported browser/Android clients
- [ ] **F-14** Integrate FairPlay for Safari/iOS/tvOS clients
- [ ] **F-15** Integrate PlayReady for Windows/selected TV clients
- [ ] **F-16** Add device capabilities/fallbacks and verify license renewals
- [ ] **F-17** Implement rights-driven watermarking or forensic watermark vendor integration
- [ ] **F-18** Test offline DRM licences where offline downloads are permitted
- [ ] **F-19** Define DRM compliance and security commitments with licensors

## G. Subscriptions, global payments & commerce — P0/P1 — lead: ME/CODEX/YOU (17 tasks)

- [ ] **G-01** Compile and run billing service against local database
- [ ] **G-02** Verify Razorpay TEST plan creation and checkout
- [ ] **G-03** Validate raw-body webhook signatures and event IDs
- [ ] **G-04** Handle captured, failed, pending, duplicate and out-of-order events
- [ ] **G-05** Use verified charges and paid-through dates for premium entitlement
- [ ] **G-06** Verify trial, renewal, grace period and expired subscriptions
- [ ] **G-07** Verify cancellation now versus period-end behavior
- [ ] **G-08** Handle full and partial refunds and dispute/chargeback reversals
- [ ] **G-09** Reconcile provider events against database and report discrepancies
- [ ] **G-10** Add upgrade, downgrade, prorating and plan-change UX
- [ ] **G-11** Define trials, coupons, promotional credits and abuse prevention
- [ ] **G-12** Add invoices, tax information and payment failure communication
- [ ] **G-13** Support subscriptions per country/currency only where provider allows
- [ ] **G-14** Implement international payment method/provider abstraction
- [ ] **G-15** Calculate appropriate local taxes, VAT/GST and refund rules with advisor
- [ ] **G-16** Define paid film rentals/TVOD and purchase windows (optional)
- [ ] **G-17** Verify PCI/payment scope; never store sensitive card credentials

## H. Search, recommendations & AI features — P1/P2 — lead: ME/CODEX (17 tasks)

- [ ] **H-01** Verify baseline keyword search, facets and synonyms
- [ ] **H-02** Improve multilingual and transliterated title discovery
- [ ] **H-03** Verify cold-start onboarding and trending recommendations
- [ ] **H-04** Validate personalized ranking only on eligible titles
- [ ] **H-05** Connect private recommendation service to live API with feature flag
- [ ] **H-06** Ensure unauthorized content never appears from ML fallback
- [ ] **H-07** Create offline chronological evaluation and reproducible datasets
- [ ] **H-08** Measure Recall@K, NDCG@K, coverage, diversity and fairness
- [ ] **H-09** Add semantic search with title/synopsis embeddings
- [ ] **H-10** Add collaborative filtering candidate generator after sufficient data
- [ ] **H-11** Add hybrid retrieval, reranking and exploration strategies
- [ ] **H-12** Add per-user control for personalization and privacy
- [ ] **H-13** Train/update models with versioned features and drift monitoring
- [ ] **H-14** Set up online experiments, holdouts and unbiased metrics
- [ ] **H-15** Build editorial overrides, safety exclusion and diversity constraints
- [ ] **H-16** Optional AI scene search and natural-language content discovery
- [ ] **H-17** Optional assisted subtitles/transcripts/dubbing with human QA

## I. Analytics, playback QoE & creator insights — P1 — lead: ME/CODEX (13 tasks)

- [ ] **I-01** Validate view events and event retention controls
- [ ] **I-02** Instrument request tracing and correlation IDs
- [ ] **I-03** Capture startup time, bitrate switches, stalls and player errors
- [ ] **I-04** Implement active-users, conversions, retention, churn and watch time
- [ ] **I-05** Add movie/episode completion and drop-off funnels
- [ ] **I-06** Build content viewing heatmaps and episodic analytics
- [ ] **I-07** Build creator dashboard for views, audience and revenue share
- [ ] **I-08** Design privacy-safe event schema and identity resolution
- [ ] **I-09** Add event deduplication, ingestion durability and replay
- [ ] **I-10** Implement exportable admin dashboards with RBAC
- [ ] **I-11** Add cohort analysis and acquisition attribution with consent
- [ ] **I-12** Set up data quality alerts, anomalies and schema-version handling
- [ ] **I-13** Benchmark streaming QoE by country/ISP/device

## J. UI, accessibility, localization & product polish — P1 — lead: ME (14 tasks)

- [ ] **J-01** Verify every existing route and error state in the real app
- [ ] **J-02** Unify original ReelNest brand identity and design tokens
- [ ] **J-03** Build hero banners, collections, titles and episode detail UX
- [ ] **J-04** Add responsive navigation, profile picker, watchlist and search
- [ ] **J-05** Add loading skeletons, empty states and safe fallbacks
- [ ] **J-06** Provide screen-reader labels, keyboard nav and WCAG audits
- [ ] **J-07** Provide Hindi/English localizations with translation architecture
- [ ] **J-08** Design RTL layout support for applicable languages
- [ ] **J-09** Add profile-specific recommendations and personalized home rails
- [ ] **J-10** Add themed posters, trailers, preview thumbnails and editorial art
- [ ] **J-11** Implement support/contact, FAQ and account settings pages
- [ ] **J-12** Add notifications, parental settings and device management UX
- [ ] **J-13** Implement SEO, social previews, sitemap and metadata for public content
- [ ] **J-14** Add opt-in email/push notifications respecting consent

## K. Creator studio, admin operations & support — P1 — lead: ME/YOU (13 tasks)

- [ ] **K-01** Creator verification and contract upload review
- [ ] **K-02** Creator content submission, resubmission and versioning
- [ ] **K-03** Admin role separation, two-person approval for sensitive actions
- [ ] **K-04** Bulk catalog updates and metadata import/export
- [ ] **K-05** Rights calendar, expiration alerts and takedown controls
- [ ] **K-06** Admin complaints, moderation decisions and appeal case management
- [ ] **K-07** Creator processing dashboard, subtitles and poster editing
- [ ] **K-08** Creator earnings statements and configurable revenue share
- [ ] **K-09** Support tickets, transaction investigations and viewer complaints
- [ ] **K-10** Refund-support tools with auditable approval workflow
- [ ] **K-11** Operations dashboard for worker failures and playback errors
- [ ] **K-12** Content publishing schedules, embargoes and staged rollouts
- [ ] **K-13** Rights-holder reports and platform audit exports

## L. AWS deployment & worldwide delivery — P0/P1 — lead: ME/LOCAL/CODEX/YOU (25 tasks)

- [ ] **L-01** Verify local Docker integration before AWS deployment
- [ ] **L-02** Validate Terraform fmt/init/validate with real providers
- [ ] **L-03** Review Terraform plan, resource costs and IAM policies
- [ ] **L-04** Configure separate dev/staging/prod AWS accounts or roles
- [ ] **L-05** Configure private VPC, subnets, NAT, security groups and endpoints
- [ ] **L-06** Provision RDS PostgreSQL with backup and runtime least privilege
- [ ] **L-07** Provision Redis, message broker, ECS services and job workers
- [ ] **L-08** Configure IAM roles, Secrets Manager and key rotation
- [ ] **L-09** Publish reproducible ECR container images and digest pins
- [ ] **L-10** Configure managed Keycloak or supported OIDC runtime
- [ ] **L-11** Deploy frontend, API, ML service and worker to staging
- [ ] **L-12** Configure Route 53 DNS, TLS/ACM and custom app/API/stream hostnames
- [ ] **L-13** Configure private S3 storage and CloudFront OAC origins
- [ ] **L-14** Configure signed-cookies distribution and verified georestrictions
- [ ] **L-15** Deploy CloudFront global distribution and verify real regional clients
- [ ] **L-16** Implement country-specific catalog, tier and entitlement policies
- [ ] **L-17** Measure CDN cache hit rate, bandwidth, misses and origin egress
- [ ] **L-18** Set cost budgets, quotas, alerts and encoder spend safeguards
- [ ] **L-19** Configure origin shield/multi-region origins only after measured need
- [ ] **L-20** Design global API latency and disaster recovery strategy
- [ ] **L-21** Add regional API deployment, Route53 latency routing when justified
- [ ] **L-22** Plan database cross-region replication and consistency/failover
- [ ] **L-23** Implement progressive delivery, rollback and environment promotion
- [ ] **L-24** Set up streaming DNS, cache invalidation and emergency block controls
- [ ] **L-25** Perform canary pilot in one licensed territory before worldwide rollout

## M. Mobile apps, smart TV & offline viewing — P2 — lead: ME/CODEX/YOU (15 tasks)

- [ ] **M-01** Harden installable PWA manifest, service worker and app shell
- [ ] **M-02** Create Android app with compatible playback/DRM framework
- [ ] **M-03** Create iOS app with AVPlayer/FairPlay compatibility
- [ ] **M-04** Test Android/iOS auth flows and deep links
- [ ] **M-05** Implement mobile download manager only for authorized offline titles
- [ ] **M-06** Handle expiration, key renewal and delete downloaded media
- [ ] **M-07** Build Android TV / Google TV focus-based player UX
- [ ] **M-08** Build Apple tvOS player and focus/navigation UX
- [ ] **M-09** Add Samsung Tizen client where market warrants
- [ ] **M-10** Add LG webOS client where market warrants
- [ ] **M-11** Implement remote control focus map, D-pad and accessibility
- [ ] **M-12** Implement casting/deep-link handoff and device pairing
- [ ] **M-13** Set up push notifications, privacy prompts and crash analytics
- [ ] **M-14** Set up app signing, store listings, review and staged rollouts
- [ ] **M-15** Create a device/browser/DRM capability matrix and test lab

## N. Privacy, legal, compliance & abuse — P0/P1 — lead: ME/YOU/PRO (15 tasks)

- [ ] **N-01** Review privacy policy, terms of use and applicable data laws
- [ ] **N-02** Complete account export and deletion across identity and database
- [ ] **N-03** Respect billing/creator/audit record retention exemptions
- [ ] **N-04** Implement deletion propagation to analytics, caches and object storage
- [ ] **N-05** Define data retention schedule and backup deletion/expiry procedure
- [ ] **N-06** Add consent, cookie tracking preferences and policy versioning
- [ ] **N-07** Perform child data/age verification risk review
- [ ] **N-08** Review OTT content classifications, grievance redressal and takedown
- [ ] **N-09** Validate rights contracts for every country and streaming platform
- [ ] **N-10** Review international taxes, trade/sanctions and payment availability
- [ ] **N-11** Review accessibility commitments in target countries
- [ ] **N-12** Maintain security incident, legal hold and deletion audit logs
- [ ] **N-13** Run independent IP/trademark clearance for brand name and artwork
- [ ] **N-14** Define creator payout and intellectual-property dispute process
- [ ] **N-15** Document media licensing, DRM and geoblocking limitations

## O. Security & engineering quality — P0/P1 — lead: ME/CODEX (18 tasks)

- [ ] **O-01** Create system threat model, assets inventory and trust boundaries
- [ ] **O-02** Enforce authentication on all private API/media routes
- [ ] **O-03** Test BOLA/IDOR against user/creator/admin account boundaries
- [ ] **O-04** Test CSRF/CORS/cookie scope and browser sandboxing
- [ ] **O-05** Perform input validation, injection and XSS regression tests
- [ ] **O-06** Secure multipart upload, MIME sniffing and archive/path traversal
- [ ] **O-07** Add SAST/SCA/dependency and secret scanning to CI
- [ ] **O-08** Apply restrictive CSP, HSTS, secure headers and TLS settings
- [ ] **O-09** Run DAST and external dependency/vulnerability review
- [ ] **O-10** Review least-privilege IAM, SQL runtime privileges and admin MFA
- [ ] **O-11** Implement webhook rate limiting, request signing and replay defense
- [ ] **O-12** Test CDN signed-cookie expiry, geo bypass and authorization revocation
- [ ] **O-13** Add pentest and independent security review before paid launch
- [ ] **O-14** Implement deterministic fixtures and contract/unit/integration tests
- [ ] **O-15** Add Playwright happy-path and negative browser E2E flows
- [ ] **O-16** Add visual, keyboard, browser matrix and accessibility regressions
- [ ] **O-17** Establish performance budgets for API, player and web app
- [ ] **O-18** Add regression tests for each fixed P0/P1 bug

## P. Resilience, performance, backup & operations — P1 — lead: ME/LOCAL/CODEX (15 tasks)

- [ ] **P-01** Run backup/restore in disposable PostgreSQL environment
- [ ] **P-02** Measure backup recovery-point and recovery-time objectives
- [ ] **P-03** Verify durable worker retry/dead-letter/crash recovery
- [ ] **P-04** Run read-only and controlled-write load tests in staging
- [ ] **P-05** Test playback startup time and rebuffer ratios across networks
- [ ] **P-06** Load test simultaneous sessions and subscription gate
- [ ] **P-07** Verify monitoring dashboards, on-call alerts and logging
- [ ] **P-08** Test disk/storage, DB, broker and upstream outage behavior
- [ ] **P-09** Implement readiness/liveness probes and graceful shutdown
- [ ] **P-10** Use auto-scaling only after testing predictable thresholds
- [ ] **P-11** Test blue-green/canary deploy and safe rollback
- [ ] **P-12** Configure retention and cost alarms for logs, storage and CDN
- [ ] **P-13** Document incident response, rollback and on-call runbooks
- [ ] **P-14** Perform chaos/failure injection in staging (not production)
- [ ] **P-15** Create reproducible launch audit/evidence package

## Q. Optional advanced innovation & monetization — P2/P3 — lead: ME/CODEX/YOU (13 tasks)

- [ ] **Q-01** Introduce AVOD advertising with server/client insertion where permitted
- [ ] **Q-02** Offer AVOD/SVOD/TVOD hybrid packages and experiments
- [ ] **Q-03** Implement live events with separate ingest and low-latency pipeline
- [ ] **Q-04** Implement FAST-style scheduled linear channels
- [ ] **Q-05** Implement watch parties with realtime synchronization and moderation
- [ ] **Q-06** Add loyalty/rewards and safe personalization controls
- [ ] **Q-07** Add multi-tenant white-label OTT capabilities if commercially justified
- [ ] **Q-08** Provide AI search assistant grounded in licensed catalog
- [ ] **Q-09** Add scene-level discovery and recommendation exploration
- [ ] **Q-10** Add localization-quality translation workflows and assisted dubbing
- [ ] **Q-11** Add 4K HDR/Dolby formats as device/licence support grows
- [ ] **Q-12** Add ad measurement, fraud detection and privacy controls
- [ ] **Q-13** Introduce advanced creator licensing marketplace and royalties

## Inventory: **282 actionable checklist items across 17 workstreams**

## Repeatable test/bug-fix checklist (apply to EVERY meaningful change)

- [ ] Record ticket ID, severity (P0/P1/P2/P3), owner, environment and reproducible steps.
- [ ] Write or update one failing reproduction/regression test.
- [ ] Implement the minimum safe patch; do not bypass auth/rights/payment policy.
- [ ] Run affected unit, contract and integration tests; record exact commands and exit codes.
- [ ] Run TypeScript/Maven/Docker checks where available (do not substitute syntax parses).
- [ ] Scan for secrets, sensitive logs, CVEs, access regressions and Flyway changes.
- [ ] Validate user-facing error, accessibility and mobile browser regression if applicable.
- [ ] Add staging evidence where a cloud/provider/device system is involved.
- [ ] Update docs/PROJECT_STATE.md, this checklist, the issue register and rollback notes.
- [ ] Confirm no unverified item is reported as complete.

## Issue register schema

| ID | Severity | Feature/Gate | Reproduction | Expected | Actual | Owner | First failing command | Patch | Tests/evidence | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| BUG-001 | P0 | G0 compilation | Fill when run | Build succeeds | To be measured | LOCAL/CODEX | `mvn -B verify` | Pending | None yet | OPEN |

Statuses: `OPEN`, `REPRODUCED`, `FIX_IN_PROGRESS`, `FIXED_UNVERIFIED`, `VERIFIED`, `BLOCKED_EXTERNAL`, `DEFERRED`. Never mark `VERIFIED` without executed evidence.

## Codex usage savings

1. LOCAL runs free prerequisite commands and gathers sanitized logs.
2. ME handles architecture/code changes, offline media+Python tests, targeted patches, this backlog and code reviews.
3. CODEX only gets a narrow failing runtime scenario with exact command and logs.
4. YOU authorizes paid infrastructure, domain changes, sensitive credentials, licences and final launches.

## Top-level acceptance evidence required per gate

| Gate | Evidence | Decision authority |
|---|---|---|
| G0 | Java and frontend build logs, Compose service health, migrations | Engineering |
| G1 | Local E2E original-film video, negative-auth tests and rights expiry | Engineering |
| G2 | Terraform plan/apply logs, sandbox webhook IDs (redacted), CDN tests, backup restore | Engineering + owner |
| G3 | Rights contracts, content classification, security review, privacy terms, monitored pilot | Owner + relevant professionals |
| G4 | Territory rights matrix, country/QoE/browser measurements, payment and tax review | Owner + operations |
| G5 | Multi-device matrices, DRM licence conformance, measured AI/player quality | Engineering + licensors |


## Build 14 source implementation ledger (not production validation)

E-04, E-05, E-06, E-07, E-08, E-14, E-15, E-16, E-19, E-21: UI/logic source implemented. Pure TypeScript helper tests and source contracts pass. Device/CDN/Next.js runtime integration remains unverified; boxes intentionally remain unchecked. E-22 partially implemented via device-local preferences only. See `docs/BUILD14_PLAYER_IMPLEMENTATION.md`.

## Build 15 source implementation ledger (NOT end-to-end verification)

E-09, E-11, E-18, E-20, E-22, E-23, I-03 and I-08 received source implementations and offline policy tests. I-09 (deduplication and durable ingestion, replay still missing) and J-13 (public sitemap/metadata, rights-aware per-title SEO still missing) received partial implementations. Boxes remain unchecked until the appropriate live integration and acceptance evidence exists. See `docs/BUILD15_IMPLEMENTATION.md` for API contracts and local/Codex test matrix. Existing Build 14 player features are preserved and remain device/CDN-unverified.


## Build 16 source progress ledger (NOT acceptance sign-off)

- **D-07:** Conditional 1080p ladder source; verify full 1080p transcode.
- **D-10 (partial):** Private JPEG/WebVTT seek thumbnails; posters and sprites remain.
- **D-12 (partial):** SRT→VTT conversion library; upload/storage/HLS wiring remains.
- **D-16 (partial):** Playlist file validation and representative segment decode; every-segment decode remains.
- **E-10 (partial):** Series/episode mapping, public season picker and entitlement-filtered next lookup; TV episode picker/normalized metadata remain.
- **E-12:** Opt-in autoplay, countdown and cancellation; live browser test pending.
- **E-13:** Private spoiler-conscious seek preview; live CDN/browser tests pending.
- **J-04:** Accessible mobile/desktop navigation; device testing pending.
- **J-05:** Loading, network-error and 404 distinction; browser tests pending.
- **J-11 (partial):** Help & FAQ, optional support mailbox; staffed support and ticketing pending.

These source-progress notes intentionally do not check the acceptance boxes above. See `docs/BUILD16_IMPLEMENTATION.md` for files, evidence and Codex handoff.


## Build 17 source progress ledger (NOT acceptance sign-off)

- **D-10 (partial):** private poster and sprite atlas, preview WebVTT and authenticated player crop; editorial artwork/public catalog pipeline remains.
- **D-11 (partial):** draft creator SRT upload and HLS WebVTT subtitle renditions; alternate audio not implemented.
- **D-12:** SRT conversion integrated with creator ingest and actual-duration validation; live browser/DB tests pending.
- **D-14 (partial):** lease-guarded progress milestones, V12 migration, creator progress polling; no measured ETA.
- **D-16 (partial):** strict URI/segment verification and opt-in full decode; live CDN/long-video tests pending.
- **E-06 (partial):** subtitle tracks now generated for existing player selectors; browser/Safari and alternate audio pending.
- **E-13:** protected sprite seek previews with thumbnail fallback; device testing pending.
- **E-19:** strict private media path allowlist; API integration negative tests pending.
- **I-09:** bounded opt-in session QoE replay with stable IDs; cross-session persistence intentionally excluded for privacy.
- **J-13:** explicit admin-approved, territory/rights/READY-filtered title SEO and sitemap; live Next/crawler tests pending.

Source work is recorded separately from the 282 acceptance checkboxes; **no unchecked box is marked complete** until its required integration/security evidence exists. See `docs/BUILD17_IMPLEMENTATION.md`.

## Build 18 source progress ledger (NOT acceptance sign-off)

- **D-11 (partial):** worker now packages up to eight *embedded* alternate audio streams into private HLS; separate dubbed audio uploads remain.
- **D-12 (incremental):** creator browser SRT preflight checks timestamps/overlap, size, unsafe markup and provides readability warnings; worker duration QA still authoritative.
- **D-16 (incremental):** alternate audio manifest/segment integrity, sampled and optional full decode added; real long-film/CDN QA pending.
- **E-06 (partial):** hls.js alternate audio backed by actual worker output, Safari native audio track feature detection added; real device tests pending.
- **E-22 (incremental):** existing profile audio language preference applied to native tracks and stored on selection; live multi-device tests pending.
- **F-09 (source security fix):** strict numeric audio paths and fail-closed subtitle/audio group validation; negative authenticated/CDN tests pending.
- **K-07 (partial):** creator caption QA preview and structural upload guard; editor/human QA remains.
- **H-07 (offline source):** reproducible chronological global-cutoff evaluation function, no real dataset.
- **H-08 (offline source):** aggregate ranking/coverage/diversity/language metrics, no business outcome claims.
- **H-15 (offline source):** rights-first editorial candidate curation; NOT connected to live ranking.

No acceptance checkbox is checked merely from offline source tests. See `docs/BUILD18_IMPLEMENTATION.md` and `docs/BUILD18_OFFLINE_RESULTS.json`.


## 2026-10-22 Recovery checkpoint

The owner's hourly automation has been disabled. Build 18 retains all files in Builds 13–17; its source has been consolidated without starting a fresh unrelated project. Source evidence is summarized separately at `docs/MASTER_TASK_STATUS_282.csv` and `docs/PRIORITY_117_TASKS.md`. No acceptance boxes were marked solely on reported offline test passes. Build 18 offline release checks passed 19/19 in this reconciliation environment, but runtime/provider/legal gates remain unverified.
