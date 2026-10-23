# ReelNest Build 23 — Incomplete source tasks

There are **158** task IDs still classified as partial or without evidence. This file is a queue, not a claim that a task has no code at all.

## A: 4 unresolved

- [ ] **A-01** Inspect Build 13 branch and record immutable baseline commit — *Partially implemented*
- [ ] **A-03** Review licenses, CVEs and deprecated libraries — *Partially implemented*
- [ ] **A-13** Fix first actual runtime errors with regression tests — *Partially implemented*
- [ ] **A-14** Establish private Git repository, branches and protected PR workflow — *Partially implemented*

## B: 8 unresolved

- [ ] **B-02** Verify OIDC PKCE, issuer, audience, JWKS rotation and token refresh — *Partially implemented*
- [ ] **B-06** Enforce viewer, creator, moderator and admin roles server-side — *Partially implemented*
- [ ] **B-08** Add device/session inventory and revoke-all-sessions control — *Partially implemented*
- [ ] **B-09** Verify lockout, abuse throttling and stolen-token handling — *Partially implemented*
- [ ] **B-10** Add independent viewer profiles and PIN-protected adult profile actions — *Partially implemented*
- [ ] **B-11** Implement child ages, parental controls and mature-title restrictions — *Partially implemented*
- [ ] **B-12** Add accessibility for authentication and account recovery — *Partially implemented*
- [ ] **B-13** Validate OIDC logout and cache/session invalidation — *Partially implemented*

## C: 10 unresolved

- [ ] **C-01** Normalize movies, series, seasons, episodes and trailers — *Partially implemented*
- [ ] **C-03** Add genre, language, cast, crew, artwork and release metadata — *Partially implemented*
- [ ] **C-04** Add unique content identifiers, slugs and canonical URLs — *Partially implemented* — Canonical /title/slug/[slug] server redirect added; unique slug remains creator-id by default; editorial slugs unresolved
- [ ] **C-05** Add posters, landscape art, banners and artwork rendition pipeline — *Partially implemented*
- [ ] **C-06** Add editorial categories, collections, featured rails and scheduling — *Partially implemented* — Flyway V23 editorial collections and admin-controlled rails added; full category/home integration remains
- [ ] **C-08** Persist rights holder identity and attached licence evidence — *Partially implemented*
- [ ] **C-09** Model rights by country, region, platform, tier and date window — *Partially implemented*
- [ ] **C-10** Add content ratings, content warnings and age-review process — *Partially implemented*
- [ ] **C-12** Enforce unpublish and rights revocation across search, CDN authorization and recommendations — *Partially implemented*
- [ ] **C-13** Add takedown request and appeal workflows — *Partially implemented*

## D: 11 unresolved

- [ ] **D-02** Implement resumable interrupted uploads and safe retry — *Partially implemented*
- [ ] **D-03** Enforce ownership, maximum size, format and quota constraints — *Partially implemented*
- [ ] **D-05** Isolate FFmpeg/worker execution and constrain resources — *Partially implemented*
- [ ] **D-09** Generate optional DASH/CMAF renditions for multi-device playback — *Partially implemented* — Offline-tested experimental DASH/fMP4 packager; production HLS worker/CDN integration pending
- [ ] **D-10** Create thumbnails, posters, preview strips and sprite/VTT metadata — *Partially implemented*
- [ ] **D-11** Support alternate audio tracks and subtitle ingest — *Partially implemented*
- [ ] **D-13** Add 4K/HDR/HEVC/AV1 presets only for compatible devices/sources — *Not started / no audited implementation evidence*
- [ ] **D-14** Record processing progress, ETA, warnings and failure diagnosis — *Partially implemented*
- [ ] **D-15** Implement retries, dead-letter queues, cancellation and cleanup — *Partially implemented*
- [ ] **D-16** Validate package decoding, manifest correctness and all segments — *Partially implemented*
- [ ] **D-18** Add lifecycle policies for originals, renditions and failed uploads — *Partially implemented*

## E: 6 unresolved

- [ ] **E-03** Create responsive desktop, phone, tablet and TV player layouts — *Partially implemented*
- [ ] **E-06** Implement audio track and subtitle/closed-caption selectors — *Partially implemented*
- [ ] **E-10** Add Next Episode, episode picker and series season navigation — *Partially implemented*
- [ ] **E-17** Add Chromecast/AirPlay casting when platforms permit — *Not started / no audited implementation evidence*
- [ ] **E-20** Tune startup latency, buffer size, ABR ladder and rebuffering controls — *Partially implemented*
- [ ] **E-21** Support data-saver and optional audio-only accessibility mode — *Partially implemented*

## F: 13 unresolved

- [ ] **F-02** Verify subscription, free tier, rentals and trials when configured — *Partially implemented*
- [ ] **F-04** Implement trusted country decision from edge/network signal, not client input — *Partially implemented*
- [ ] **F-07** Verify session renewal and revoked subscription/rights handling — *Partially implemented*
- [ ] **F-08** Implement CDN edge/token key rotation and private key custody — *Partially implemented*
- [ ] **F-10** Support concurrent stream leases and shared account device policy — *Partially implemented*
- [ ] **F-11** Add optional session anomaly detection and suspicious activity reviews — *Partially implemented*
- [ ] **F-12** Design DRM packaging and SPEKE/license-server integration — *Not started / no audited implementation evidence*
- [ ] **F-13** Integrate Widevine on supported browser/Android clients — *Not started / no audited implementation evidence*
- [ ] **F-14** Integrate FairPlay for Safari/iOS/tvOS clients — *Not started / no audited implementation evidence*
- [ ] **F-15** Integrate PlayReady for Windows/selected TV clients — *Not started / no audited implementation evidence*
- [ ] **F-16** Add device capabilities/fallbacks and verify license renewals — *Partially implemented* — Client-side EME capability probe and device diagnostic UI; real DRM licences/renewals unsupported
- [ ] **F-17** Implement rights-driven watermarking or forensic watermark vendor integration — *Not started / no audited implementation evidence*
- [ ] **F-18** Test offline DRM licences where offline downloads are permitted — *Not started / no audited implementation evidence*

## G: 11 unresolved

- [ ] **G-04** Handle captured, failed, pending, duplicate and out-of-order events — *Partially implemented*
- [ ] **G-06** Verify trial, renewal, grace period and expired subscriptions — *Partially implemented*
- [ ] **G-07** Verify cancellation now versus period-end behavior — *Partially implemented*
- [ ] **G-09** Reconcile provider events against database and report discrepancies — *Partially implemented*
- [ ] **G-10** Add upgrade, downgrade, prorating and plan-change UX — *Not started / no audited implementation evidence*
- [ ] **G-11** Define trials, coupons, promotional credits and abuse prevention — *Partially implemented*
- [ ] **G-12** Add invoices, tax information and payment failure communication — *Partially implemented*
- [ ] **G-13** Support subscriptions per country/currency only where provider allows — *Not started / no audited implementation evidence*
- [ ] **G-14** Implement international payment method/provider abstraction — *Not started / no audited implementation evidence*
- [ ] **G-16** Define paid film rentals/TVOD and purchase windows (optional) — *Not started / no audited implementation evidence*
- [ ] **G-17** Verify PCI/payment scope; never store sensitive card credentials — *Partially implemented*

## H: 11 unresolved

- [ ] **H-01** Verify baseline keyword search, facets and synonyms — *Partially implemented*
- [ ] **H-02** Improve multilingual and transliterated title discovery — *Partially implemented*
- [ ] **H-05** Connect private recommendation service to live API with feature flag — *Partially implemented*
- [ ] **H-06** Ensure unauthorized content never appears from ML fallback — *Partially implemented*
- [ ] **H-09** Add semantic search with title/synopsis embeddings — *Partially implemented*
- [ ] **H-10** Add collaborative filtering candidate generator after sufficient data — *Partially implemented*
- [ ] **H-11** Add hybrid retrieval, reranking and exploration strategies — *Partially implemented*
- [ ] **H-13** Train/update models with versioned features and drift monitoring — *Partially implemented*
- [ ] **H-14** Set up online experiments, holdouts and unbiased metrics — *Partially implemented*
- [ ] **H-16** Optional AI scene search and natural-language content discovery — *Partially implemented*
- [ ] **H-17** Optional assisted subtitles/transcripts/dubbing with human QA — *Partially implemented*

## I: 8 unresolved

- [ ] **I-04** Implement active-users, conversions, retention, churn and watch time — *Partially implemented*
- [ ] **I-05** Add movie/episode completion and drop-off funnels — *Partially implemented*
- [ ] **I-07** Build creator dashboard for views, audience and revenue share — *Partially implemented*
- [ ] **I-09** Add event deduplication, ingestion durability and replay — *Partially implemented*
- [ ] **I-10** Implement exportable admin dashboards with RBAC — *Partially implemented*
- [ ] **I-11** Add cohort analysis and acquisition attribution with consent — *Partially implemented*
- [ ] **I-12** Set up data quality alerts, anomalies and schema-version handling — *Partially implemented*
- [ ] **I-13** Benchmark streaming QoE by country/ISP/device — *Partially implemented*

## J: 7 unresolved

- [ ] **J-02** Unify original ReelNest brand identity and design tokens — *Partially implemented*
- [ ] **J-06** Provide screen-reader labels, keyboard nav and WCAG audits — *Partially implemented* — Skip link, focus outline, reduced motion; full screen-reader and WCAG audit pending
- [ ] **J-07** Provide Hindi/English localizations with translation architecture — *Partially implemented* — English/Hindi/Arabic navigation translations and locale picker; full route localization pending
- [ ] **J-08** Design RTL layout support for applicable languages — *Partially implemented* — Basic Arabic RTL document direction & CSS; comprehensive RTL/TV QA pending
- [ ] **J-10** Add themed posters, trailers, preview thumbnails and editorial art — *Partially implemented* — Rights-filtered collection rails; creator posters/editorial banners remain
- [ ] **J-12** Add notifications, parental settings and device management UX — *Partially implemented*
- [ ] **J-14** Add opt-in email/push notifications respecting consent — *Partially implemented*

## K: 9 unresolved

- [ ] **K-01** Creator verification and contract upload review — *Partially implemented*
- [ ] **K-02** Creator content submission, resubmission and versioning — *Partially implemented*
- [ ] **K-03** Admin role separation, two-person approval for sensitive actions — *Partially implemented*
- [ ] **K-05** Rights calendar, expiration alerts and takedown controls — *Partially implemented*
- [ ] **K-06** Admin complaints, moderation decisions and appeal case management — *Partially implemented*
- [ ] **K-07** Creator processing dashboard, subtitles and poster editing — *Partially implemented*
- [ ] **K-08** Creator earnings statements and configurable revenue share — *Partially implemented*
- [ ] **K-10** Refund-support tools with auditable approval workflow — *Partially implemented*
- [ ] **K-11** Operations dashboard for worker failures and playback errors — *Partially implemented*

## L: 14 unresolved

- [ ] **L-04** Configure separate dev/staging/prod AWS accounts or roles — *Partially implemented*
- [ ] **L-08** Configure IAM roles, Secrets Manager and key rotation — *Partially implemented*
- [ ] **L-09** Publish reproducible ECR container images and digest pins — *Partially implemented*
- [ ] **L-10** Configure managed Keycloak or supported OIDC runtime — *Partially implemented*
- [ ] **L-12** Configure Route 53 DNS, TLS/ACM and custom app/API/stream hostnames — *Partially implemented*
- [ ] **L-16** Implement country-specific catalog, tier and entitlement policies — *Partially implemented*
- [ ] **L-17** Measure CDN cache hit rate, bandwidth, misses and origin egress — *Not started / no audited implementation evidence*
- [ ] **L-18** Set cost budgets, quotas, alerts and encoder spend safeguards — *Partially implemented*
- [ ] **L-19** Configure origin shield/multi-region origins only after measured need — *Not started / no audited implementation evidence*
- [ ] **L-20** Design global API latency and disaster recovery strategy — *Partially implemented*
- [ ] **L-21** Add regional API deployment, Route53 latency routing when justified — *Not started / no audited implementation evidence*
- [ ] **L-22** Plan database cross-region replication and consistency/failover — *Not started / no audited implementation evidence*
- [ ] **L-23** Implement progressive delivery, rollback and environment promotion — *Partially implemented*
- [ ] **L-24** Set up streaming DNS, cache invalidation and emergency block controls — *Partially implemented*

## M: 12 unresolved

- [ ] **M-02** Create Android app with compatible playback/DRM framework — *Not started / no audited implementation evidence*
- [ ] **M-03** Create iOS app with AVPlayer/FairPlay compatibility — *Not started / no audited implementation evidence*
- [ ] **M-04** Test Android/iOS auth flows and deep links — *Not started / no audited implementation evidence*
- [ ] **M-05** Implement mobile download manager only for authorized offline titles — *Not started / no audited implementation evidence*
- [ ] **M-06** Handle expiration, key renewal and delete downloaded media — *Not started / no audited implementation evidence*
- [ ] **M-07** Build Android TV / Google TV focus-based player UX — *Not started / no audited implementation evidence*
- [ ] **M-08** Build Apple tvOS player and focus/navigation UX — *Not started / no audited implementation evidence*
- [ ] **M-09** Add Samsung Tizen client where market warrants — *Not started / no audited implementation evidence*
- [ ] **M-10** Add LG webOS client where market warrants — *Not started / no audited implementation evidence*
- [ ] **M-11** Implement remote control focus map, D-pad and accessibility — *Partially implemented*
- [ ] **M-12** Implement casting/deep-link handoff and device pairing — *Not started / no audited implementation evidence*
- [ ] **M-13** Set up push notifications, privacy prompts and crash analytics — *Not started / no audited implementation evidence*

## N: 6 unresolved

- [ ] **N-02** Complete account export and deletion across identity and database — *Partially implemented*
- [ ] **N-03** Respect billing/creator/audit record retention exemptions — *Partially implemented*
- [ ] **N-04** Implement deletion propagation to analytics, caches and object storage — *Partially implemented*
- [ ] **N-05** Define data retention schedule and backup deletion/expiry procedure — *Partially implemented*
- [ ] **N-06** Add consent, cookie tracking preferences and policy versioning — *Partially implemented*
- [ ] **N-12** Maintain security incident, legal hold and deletion audit logs — *Partially implemented*

## O: 8 unresolved

- [ ] **O-01** Create system threat model, assets inventory and trust boundaries — *Partially implemented*
- [ ] **O-02** Enforce authentication on all private API/media routes — *Partially implemented*
- [ ] **O-04** Test CSRF/CORS/cookie scope and browser sandboxing — *Partially implemented*
- [ ] **O-05** Perform input validation, injection and XSS regression tests — *Partially implemented*
- [ ] **O-07** Add SAST/SCA/dependency and secret scanning to CI — *Partially implemented*
- [ ] **O-10** Review least-privilege IAM, SQL runtime privileges and admin MFA — *Partially implemented*
- [ ] **O-17** Establish performance budgets for API, player and web app — *Partially implemented*
- [ ] **O-18** Add regression tests for each fixed P0/P1 bug — *Partially implemented*

## P: 7 unresolved

- [ ] **P-02** Measure backup recovery-point and recovery-time objectives — *Not started / no audited implementation evidence*
- [ ] **P-03** Verify durable worker retry/dead-letter/crash recovery — *Partially implemented*
- [ ] **P-07** Verify monitoring dashboards, on-call alerts and logging — *Partially implemented*
- [ ] **P-09** Implement readiness/liveness probes and graceful shutdown — *Partially implemented*
- [ ] **P-10** Use auto-scaling only after testing predictable thresholds — *Not started / no audited implementation evidence*
- [ ] **P-12** Configure retention and cost alarms for logs, storage and CDN — *Partially implemented*
- [ ] **P-15** Create reproducible launch audit/evidence package — *Partially implemented*

## Q: 13 unresolved

- [ ] **Q-01** Introduce AVOD advertising with server/client insertion where permitted — *Not started / no audited implementation evidence*
- [ ] **Q-02** Offer AVOD/SVOD/TVOD hybrid packages and experiments — *Not started / no audited implementation evidence*
- [ ] **Q-03** Implement live events with separate ingest and low-latency pipeline — *Not started / no audited implementation evidence*
- [ ] **Q-04** Implement FAST-style scheduled linear channels — *Partially implemented* — Admin scheduled channels/48h EPG; no true continuous FAST/live streaming
- [ ] **Q-05** Implement watch parties with realtime synchronization and moderation — *Partially implemented* — Membership/entitlement-checked invite-only watch-party state & UI; five-second polling, no chat/moderation or precise synchronization
- [ ] **Q-06** Add loyalty/rewards and safe personalization controls — *Not started / no audited implementation evidence*
- [ ] **Q-07** Add multi-tenant white-label OTT capabilities if commercially justified — *Not started / no audited implementation evidence*
- [ ] **Q-08** Provide AI search assistant grounded in licensed catalog — *Partially implemented*
- [ ] **Q-09** Add scene-level discovery and recommendation exploration — *Not started / no audited implementation evidence*
- [ ] **Q-10** Add localization-quality translation workflows and assisted dubbing — *Not started / no audited implementation evidence*
- [ ] **Q-11** Add 4K HDR/Dolby formats as device/licence support grows — *Not started / no audited implementation evidence*
- [ ] **Q-12** Add ad measurement, fraud detection and privacy controls — *Not started / no audited implementation evidence*
- [ ] **Q-13** Introduce advanced creator licensing marketplace and royalties — *Not started / no audited implementation evidence*

