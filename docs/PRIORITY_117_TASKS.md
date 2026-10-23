# ReelNest — Reconciled 117-Task Priority Checklist

**Recovery checkpoint:** Build 19, 2026-10-22 · A/B security and runtime source updates added to Build 18 baseline.

**Important provenance:** The original project master list has **282 task IDs (A-01 through Q-13)**. No independent earlier 117-item list is present in the uploaded files or available project history. This document is a **new 117-task priority view selected from the original 282**, preserving every selected original ID; it is not a claim to recover some different 117-item list.

**Acceptance status:** 0/117 are marked verified complete. Build 19 changes improve source evidence for tasks A/B; none of the blocked external gates were silently promoted to complete. Source code can exist and pass offline tests while full Maven/Next.js, Flyway, Docker, real playback/payment/AWS/security/legal checks remain unverified. Check a box ONLY once item-level acceptance evidence has been captured.

**Source status terminology:** `SOURCE_PRESENT_UNVERIFIED` = release-specific code work claimed and present for that item, still requires integration/device/provider verification; `PARTIAL_SOURCE` = explicitly reported incomplete code path; `OFFLINE_ONLY` = model/lab tests, not live integration; `BLOCKED_EXTERNAL` = requires missing tools, accounts or external tests; `NOT_INDIVIDUALLY_AUDITED` = may have earlier code, but no current source-to-requirement verification. **This is not the same as 'not started'.**

**Fix order:** G0 real build+auth → G1 licensed sample upload-to-playback → billing/rights and security → AWS staging → public pilot; new mobile/TV/ML enhancements after blockers.

## Current 117-task source-evidence distribution

| Status | Tasks |
|---|---:|
| SOURCE_PRESENT_UNVERIFIED | 21 |
| PARTIAL_SOURCE | 17 |
| OFFLINE_ONLY | 2 |
| NOT_INDIVIDUALLY_AUDITED | 68 |
| BLOCKED_EXTERNAL | 9 |

**Build 19:** Source status reflects new A/B implementation evidence; all acceptance boxes stay unchecked pending real runtime verification.

## All 117 selected tasks (unchecked = acceptance not yet verified)

### A — Build, repository & local runtime — P0 — lead: ME/LOCAL/CODEX (16 tasks) (13)

| Acceptance | Task | Current source status | Implementation and verification requirement |
|---|---|---|---|
| ☐ | **A-01** — Inspect Build 13 branch and record immutable baseline commit | `PARTIAL_SOURCE` | Requires inspection of earlier source and live verification |
| ☐ | **A-02** — Create dependency inventory and supported-version matrix | `SOURCE_PRESENT_UNVERIFIED` | Requires inspection of earlier source and live verification |
| ☐ | **A-03** — Review licenses, CVEs and deprecated libraries | `PARTIAL_SOURCE` | Requires inspection of earlier source and live verification |
| ☐ | **A-04** — Regenerate and commit reviewed npm lockfile | `BLOCKED_EXTERNAL` | Requires inspection of earlier source and live verification |
| ☐ | **A-05** — Compile backend with real Maven dependencies | `BLOCKED_EXTERNAL` | Requires inspection of earlier source and live verification |
| ☐ | **A-06** — Run backend unit and integration tests | `PARTIAL_SOURCE` | Requires inspection of earlier source and live verification |
| ☐ | **A-07** — Typecheck and production-build Next.js | `BLOCKED_EXTERNAL` | Requires inspection of earlier source and live verification |
| ☐ | **A-08** — Build Docker images for API, frontend, worker and AI service | `BLOCKED_EXTERNAL` | Requires inspection of earlier source and live verification |
| ☐ | **A-09** — Validate Compose schema, profiles and dependencies | `PARTIAL_SOURCE` | Requires inspection of earlier source and live verification |
| ☐ | **A-10** — Start PostgreSQL, Keycloak, Redis, RabbitMQ, MinIO, API, frontend and ML services | `BLOCKED_EXTERNAL` | Requires inspection of earlier source and live verification |
| ☐ | **A-11** — Apply Flyway V1–V9 on clean database and test an upgrade database | `BLOCKED_EXTERNAL` | Requires inspection of earlier source and live verification |
| ☐ | **A-12** — Validate configuration/env contracts between containers | `SOURCE_PRESENT_UNVERIFIED` | Requires inspection of earlier source and live verification |
| ☐ | **A-13** — Fix first actual runtime errors with regression tests | `PARTIAL_SOURCE` | Requires inspection of earlier source and live verification |

### B — Identity, security & user accounts — P0 — lead: ME/CODEX (15 tasks) (9)

| Acceptance | Task | Current source status | Implementation and verification requirement |
|---|---|---|---|
| ☐ | **B-01** — Verify registration, login, email confirmation and account recovery | `BLOCKED_EXTERNAL` | Requires inspection of earlier source and live verification |
| ☐ | **B-02** — Verify OIDC PKCE, issuer, audience, JWKS rotation and token refresh | `PARTIAL_SOURCE` | Requires inspection of earlier source and live verification |
| ☐ | **B-03** — Remove seeded/demo accounts from staging and production | `PARTIAL_SOURCE` | Requires inspection of earlier source and live verification |
| ☐ | **B-04** — Configure production identity persistence, SMTP and secure TLS | `BLOCKED_EXTERNAL` | Requires inspection of earlier source and live verification |
| ☐ | **B-05** — Enable MFA for administrators and sensitive accounts | `PARTIAL_SOURCE` | Requires inspection of earlier source and live verification |
| ☐ | **B-06** — Enforce viewer, creator, moderator and admin roles server-side | `PARTIAL_SOURCE` | Requires inspection of earlier source and live verification |
| ☐ | **B-07** — Enforce per-resource ownership and IDOR protection | `PARTIAL_SOURCE` | Requires inspection of earlier source and live verification |
| ☐ | **B-10** — Add independent viewer profiles and PIN-protected adult profile actions | `BLOCKED_EXTERNAL` | Requires inspection of earlier source and live verification |
| ☐ | **B-11** — Implement child ages, parental controls and mature-title restrictions | `PARTIAL_SOURCE` | Requires inspection of earlier source and live verification |

### C — Catalog, metadata, rights & moderation — P0 — lead: ME/YOU (15 tasks) (9)

| Acceptance | Task | Current source status | Implementation and verification requirement |
|---|---|---|---|
| ☐ | **C-01** — Normalize movies, series, seasons, episodes and trailers | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **C-02** — Add creator-facing metadata validation and draft workflow | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **C-03** — Add genre, language, cast, crew, artwork and release metadata | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **C-07** — Add search visibility rules and availability windows | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **C-08** — Persist rights holder identity and attached licence evidence | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **C-09** — Model rights by country, region, platform, tier and date window | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **C-10** — Add content ratings, content warnings and age-review process | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **C-11** — Implement reviewer approval/rejection and audit trails | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **C-12** — Enforce unpublish and rights revocation across search, CDN authorization and recommendations | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |

### D — Creator upload & media processing — P0 — lead: ME/CODEX (18 tasks) (12)

| Acceptance | Task | Current source status | Implementation and verification requirement |
|---|---|---|---|
| ☐ | **D-01** — Verify end-to-end direct multipart upload to private MinIO/S3 | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **D-02** — Implement resumable interrupted uploads and safe retry | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **D-03** — Enforce ownership, maximum size, format and quota constraints | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **D-04** — Inspect input with ffprobe and reject malformed/disguised media | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **D-06** — Implement queue-claim leases and duplicate-job idempotency | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **D-07** — Generate aligned ABR renditions 360p/480p/720p/1080p | `SOURCE_PRESENT_UNVERIFIED` | Build 16 (implemented) |
| ☐ | **D-08** — Generate HLS master/media manifests with valid MIME and paths | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **D-10** — Create thumbnails, posters, preview strips and sprite/VTT metadata | `PARTIAL_SOURCE` | Build 16 (partial), Build 17 (partial) |
| ☐ | **D-11** — Support alternate audio tracks and subtitle ingest | `PARTIAL_SOURCE` | Build 17 (partial), Build 18 (partial) |
| ☐ | **D-12** — Add caption/transcript validation and timed-text conversion | `SOURCE_PRESENT_UNVERIFIED` | Build 16 (partial), Build 17 (implemented), Build 18 (partial) |
| ☐ | **D-16** — Validate package decoding, manifest correctness and all segments | `PARTIAL_SOURCE` | Build 16 (partial), Build 17 (partial), Build 18 (partial) |
| ☐ | **D-17** — Prove publish gate requires encoded media and approved rights | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |

### E — Netflix-like video player & viewing UX — P1 — lead: ME/CODEX (24 tasks) (15)

| Acceptance | Task | Current source status | Implementation and verification requirement |
|---|---|---|---|
| ☐ | **E-01** — Test initial playback in Chrome, Edge, Firefox and Safari | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **E-02** — Test Android Chrome and iOS Safari native HLS behavior | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **E-03** — Create responsive desktop, phone, tablet and TV player layouts | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **E-04** — Improve play, pause, seek, jump 10s, volume, mute and fullscreen | `SOURCE_PRESENT_UNVERIFIED` | Build 14 (implemented) |
| ☐ | **E-05** — Implement adaptive quality auto-switching and manual quality selector | `SOURCE_PRESENT_UNVERIFIED` | Build 14 (implemented) |
| ☐ | **E-06** — Implement audio track and subtitle/closed-caption selectors | `PARTIAL_SOURCE` | Build 14 (implemented), Build 17 (partial), Build 18 (partial) |
| ☐ | **E-07** — Implement subtitle styling: language, font size, contrast and positioning | `SOURCE_PRESENT_UNVERIFIED` | Build 14 (implemented) |
| ☐ | **E-09** — Implement accurate Continue Watching and multi-device resume | `SOURCE_PRESENT_UNVERIFIED` | Build 15 (implemented) |
| ☐ | **E-10** — Add Next Episode, episode picker and series season navigation | `PARTIAL_SOURCE` | Build 16 (partial) |
| ☐ | **E-11** — Add Skip Intro, Skip Recap and Skip Credits with metadata cues | `SOURCE_PRESENT_UNVERIFIED` | Build 15 (implemented) |
| ☐ | **E-12** — Implement Up Next countdown and disable-autoplay accessibility preference | `SOURCE_PRESENT_UNVERIFIED` | Build 16 (implemented) |
| ☐ | **E-13** — Add spoiler-safe timeline previews and thumbnail scrubbing | `SOURCE_PRESENT_UNVERIFIED` | Build 16 (implemented), Build 17 (implemented) |
| ☐ | **E-18** — Gracefully recover network loss, buffering and expired authorization | `SOURCE_PRESENT_UNVERIFIED` | Build 15 (implemented) |
| ☐ | **E-19** — Provide playback error codes, retry action and user-facing diagnostics | `SOURCE_PRESENT_UNVERIFIED` | Build 14 (implemented), Build 17 (implemented) |
| ☐ | **E-22** — Handle subtitle/audio language preference per profile | `SOURCE_PRESENT_UNVERIFIED` | Build 14 (partial), Build 15 (implemented), Build 18 (implemented) |

### F — DRM, playback entitlement & anti-abuse — P0/P1 — lead: ME/CODEX/YOU (19 tasks) (10)

| Acceptance | Task | Current source status | Implementation and verification requirement |
|---|---|---|---|
| ☐ | **F-01** — Verify every playback API checks profile ownership | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **F-02** — Verify subscription, free tier, rentals and trials when configured | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **F-03** — Verify publication, age, territory and rights dates on every session | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **F-04** — Implement trusted country decision from edge/network signal, not client input | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **F-05** — Generate short-lived scoped CloudFront signed cookies/URLs | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **F-06** — Prove direct S3 and unauthorized CDN requests return denial | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **F-07** — Verify session renewal and revoked subscription/rights handling | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **F-09** — Prevent manifest and segment authorization bypass | `SOURCE_PRESENT_UNVERIFIED` | Build 18 (implemented) |
| ☐ | **F-10** — Support concurrent stream leases and shared account device policy | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **F-12** — Design DRM packaging and SPEKE/license-server integration | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |

### G — Subscriptions, global payments & commerce — P0/P1 — lead: ME/CODEX/YOU (17 tasks) (9)

| Acceptance | Task | Current source status | Implementation and verification requirement |
|---|---|---|---|
| ☐ | **G-01** — Compile and run billing service against local database | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **G-02** — Verify Razorpay TEST plan creation and checkout | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **G-03** — Validate raw-body webhook signatures and event IDs | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **G-04** — Handle captured, failed, pending, duplicate and out-of-order events | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **G-05** — Use verified charges and paid-through dates for premium entitlement | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **G-06** — Verify trial, renewal, grace period and expired subscriptions | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **G-07** — Verify cancellation now versus period-end behavior | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **G-08** — Handle full and partial refunds and dispute/chargeback reversals | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **G-09** — Reconcile provider events against database and report discrepancies | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |

### H — Search, recommendations & AI features — P1/P2 — lead: ME/CODEX (17 tasks) (5)

| Acceptance | Task | Current source status | Implementation and verification requirement |
|---|---|---|---|
| ☐ | **H-03** — Verify cold-start onboarding and trending recommendations | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **H-04** — Validate personalized ranking only on eligible titles | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **H-05** — Connect private recommendation service to live API with feature flag | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **H-07** — Create offline chronological evaluation and reproducible datasets | `OFFLINE_ONLY` | Build 18 (offline_only) |
| ☐ | **H-08** — Measure Recall@K, NDCG@K, coverage, diversity and fairness | `OFFLINE_ONLY` | Build 18 (offline_only) |

### I — Analytics, playback QoE & creator insights — P1 — lead: ME/CODEX (13 tasks) (5)

| Acceptance | Task | Current source status | Implementation and verification requirement |
|---|---|---|---|
| ☐ | **I-01** — Validate view events and event retention controls | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **I-03** — Capture startup time, bitrate switches, stalls and player errors | `SOURCE_PRESENT_UNVERIFIED` | Build 15 (implemented) |
| ☐ | **I-08** — Design privacy-safe event schema and identity resolution | `SOURCE_PRESENT_UNVERIFIED` | Build 15 (implemented) |
| ☐ | **I-09** — Add event deduplication, ingestion durability and replay | `SOURCE_PRESENT_UNVERIFIED` | Build 15 (partial), Build 17 (implemented) |
| ☐ | **I-13** — Benchmark streaming QoE by country/ISP/device | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |

### J — UI, accessibility, localization & product polish — P1 — lead: ME (14 tasks) (6)

| Acceptance | Task | Current source status | Implementation and verification requirement |
|---|---|---|---|
| ☐ | **J-01** — Verify every existing route and error state in the real app | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **J-04** — Add responsive navigation, profile picker, watchlist and search | `SOURCE_PRESENT_UNVERIFIED` | Build 16 (implemented) |
| ☐ | **J-05** — Add loading skeletons, empty states and safe fallbacks | `SOURCE_PRESENT_UNVERIFIED` | Build 16 (implemented) |
| ☐ | **J-06** — Provide screen-reader labels, keyboard nav and WCAG audits | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **J-07** — Provide Hindi/English localizations with translation architecture | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **J-13** — Implement SEO, social previews, sitemap and metadata for public content | `SOURCE_PRESENT_UNVERIFIED` | Build 15 (partial), Build 17 (implemented) |

### K — Creator studio, admin operations & support — P1 — lead: ME/YOU (13 tasks) (5)

| Acceptance | Task | Current source status | Implementation and verification requirement |
|---|---|---|---|
| ☐ | **K-01** — Creator verification and contract upload review | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **K-02** — Creator content submission, resubmission and versioning | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **K-05** — Rights calendar, expiration alerts and takedown controls | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **K-07** — Creator processing dashboard, subtitles and poster editing | `PARTIAL_SOURCE` | Build 18 (partial) |
| ☐ | **K-11** — Operations dashboard for worker failures and playback errors | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |

### L — AWS deployment & worldwide delivery — P0/P1 — lead: ME/LOCAL/CODEX/YOU (25 tasks) (12)

| Acceptance | Task | Current source status | Implementation and verification requirement |
|---|---|---|---|
| ☐ | **L-01** — Verify local Docker integration before AWS deployment | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **L-02** — Validate Terraform fmt/init/validate with real providers | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **L-03** — Review Terraform plan, resource costs and IAM policies | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **L-06** — Provision RDS PostgreSQL with backup and runtime least privilege | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **L-07** — Provision Redis, message broker, ECS services and job workers | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **L-08** — Configure IAM roles, Secrets Manager and key rotation | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **L-10** — Configure managed Keycloak or supported OIDC runtime | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **L-11** — Deploy frontend, API, ML service and worker to staging | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **L-12** — Configure Route 53 DNS, TLS/ACM and custom app/API/stream hostnames | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **L-13** — Configure private S3 storage and CloudFront OAC origins | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **L-14** — Configure signed-cookies distribution and verified georestrictions | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **L-15** — Deploy CloudFront global distribution and verify real regional clients | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |

### M — Mobile apps, smart TV & offline viewing — P2 — lead: ME/CODEX/YOU (15 tasks) (1)

| Acceptance | Task | Current source status | Implementation and verification requirement |
|---|---|---|---|
| ☐ | **M-01** — Harden installable PWA manifest, service worker and app shell | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |

### N — Privacy, legal, compliance & abuse — P0/P1 — lead: ME/YOU/PRO (15 tasks) (3)

| Acceptance | Task | Current source status | Implementation and verification requirement |
|---|---|---|---|
| ☐ | **N-02** — Complete account export and deletion across identity and database | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **N-04** — Implement deletion propagation to analytics, caches and object storage | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **N-09** — Validate rights contracts for every country and streaming platform | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |

### O — Security & engineering quality — P0/P1 — lead: ME/CODEX (18 tasks) (2)

| Acceptance | Task | Current source status | Implementation and verification requirement |
|---|---|---|---|
| ☐ | **O-02** — Enforce authentication on all private API/media routes | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |
| ☐ | **O-03** — Test BOLA/IDOR against user/creator/admin account boundaries | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |

### P — Resilience, performance, backup & operations — P1 — lead: ME/LOCAL/CODEX (15 tasks) (1)

| Acceptance | Task | Current source status | Implementation and verification requirement |
|---|---|---|---|
| ☐ | **P-01** — Run backup/restore in disposable PostgreSQL environment | `NOT_INDIVIDUALLY_AUDITED` | Requires inspection of earlier source and live verification |

## Release evidence required before closing tasks

- `mvn -B verify` with dependencies, actual Java tests, Keycloak/JPA/Flyway boot checks.
- `npm ci` then typecheck/build and authenticated Playwright across public and protected pages.
- `docker compose up --build` on clean and migrated databases; run creator-upload → FFmpeg HLS → admin rights review → authorized playback/continue-watching.
- Razorpay TEST captured/failed/refund/dispute event results and signed-webhook replay tests.
- Terraform fmt/init/validate/plan, approved AWS staging, private CDN/geo negative tests, and managed identity settings.
- Data export/deletion across systems, accessibility/privacy/security checks, backup restore and load tests.
- Content rights agreements, classification, and actual target-country licensing sign-off before public paid release.

## Change control

The hourly ReelNest build automation was **disabled on 2026-10-22** at the owner's request. Do not create or enable a recurrence unless the user explicitly requests a new schedule. Preserve the ZIP/checklist as a manually reviewed checkpoint and merge future Codex branches using Git, never blindly overwrite files.
