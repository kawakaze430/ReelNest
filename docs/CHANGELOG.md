## Build 18 (2026-10-22)

- Private alternate AAC HLS audio packaging for multilingual embedded source tracks, strict audio and caption manifest validation, session-scoped audio media path allowlist.
- Browser hls.js and feature-detected native Safari audio selection with existing per-profile language preference.
- Creator SRT structural/readability preflight and offline chronological rights-first ML evaluation and curation.
- Offline FFmpeg, Python, Java and TypeScript helper tests. **No production sign-off or live cloud verification.**

## Build 15 — 2026-10-22

- Added Flyway V10 for owned profile preferences, admin-reviewed media cues and opt-in QoE events.
- Added near-end resume safeguards, multi-session progress conflict rejection, authenticated teardown progress write.
- Added Skip Intro/Recap/Credits UI and admin cue editing for READY assets.
- Added bounded hls.js network/media recovery and explicit buffer/ABR tuning.
- Added per-profile server preference sync with device fallback; optional telemetry off by default.
- Added bounded, deduplicated QoE ingestion, 30-day retention and admin small-cohort-suppressed aggregate panel.
- Added public-only sitemap/robots/social metadata and noindex layouts for private routes.
- Added compiler-backed pure Java/TypeScript regression tests. Full application and provider/cloud integration unverified.

---

# Build 05 + Build 06 (2026-10-21)

- Added Flyway V6 subscription/billing tables and V7 viewing-event/analytics tables.
- Added Razorpay API adapter, server-side plans/pricing verification, signed raw-body webhooks, unique event/charge records, confirmed paid-through entitlement and cycle-end cancellation.
- Added creator title FREE/PREMIUM selection and admin reviewer visibility.
- Added paid plan UI, billing account UI and premium playback gating.
- Added rights/age-gated profile-aware discovery API, per-title event aggregation and ADMIN metrics, retention job and frontend feeds.
- Added tested Python TF-IDF + collaborative hybrid ML laboratory and standalone offline UI preview.
- Known blocker: no full Maven/Docker/Next.js or live Razorpay webhook integration; refunds/chargebacks and reconciliation pending.

---

# Build 04 (2026-10-21)

- V5 playback and watchlists schema, expiry and paid-tier guards.
- Session authorization against rights, territory, age, creator and media readiness.
- Private authenticated development HLS proxy with strict media paths; session lease renewal and resume.
- Personal watchlist, profile picker and Continue Watching including clear history.
- Browser-only offline playback demo and optional real auth/HLS integration test script.
- Corrected creator age classification normalization and admin expiry-day consistency.
- Documented unverified runtime integration and next Build05 payments work.

---

# ReelNest change log

## Build 03 — 2026-10-21

- Added durable SQL processing-job queue and Build 02 media backfill (Flyway V4).
- Added upload-completion database atomicity and S3 orphan retry/reconciliation.
- Added separate Docker video worker implementing FFprobe/FFmpeg and tested multi-rendition HLS.
- Added processed-private asset uploads, lease-protected READY transition, terminal failures and bounded retries.
- Added MinIO source-bucket CORS configuration for multipart ETag exposure.
- Added creator/admin media status APIs and reviewer retry action.
- Updated Creator Studio and Review Console to display processing lifecycle.
- Added processing pipeline tests, optional live E2E script, CI worker job, validation and continuation ledger.
- Kept creator videos private; authorized viewing is the next milestone.

## Build 02

- Creator applications, rights declarations, admin review, private multipart upload, studio UIs.

## Build 01

- Spring Boot/Next.js foundation, fictional demo catalog and original HLS sample media.

---

## Builds 09 and 10 — 2026-10-21

- Kept cumulative Builds 01–08 source; created a separate full-stack go-live blocker register.
- Added deployment-mode startup guard (forbids development media proxy, unsafe CDN/proxy settings and missing geographic review in production).
- Added one-country CloudFront geo whitelist matching backend configured territory, operator review flag off by default and lifecycle safeguard for enabling API service.
- Added explicit app runtime PostgreSQL role & secret scaffold, separation from privileged Flyway migration operator, and SQL provisioning guide.
- Added production Next.js image URL validation, safe build helper, TypeScript syntax parser and seven public URL test cases.
- Prevented privacy review from falsely marking an account deleted.
- Added read-only live smoke checks, machine-readable offline release evidence and 12 gated external release checks; CI uploads the offline report.
- Updated website status screen and docs to say NOT PRODUCTION READY until genuine integration, licensing, security and provider tests pass.


### Build 11 — Single ZIP & AWS deployment handoff
- Exported allowlist-only static OTT demonstration for Amplify Hosting manual deploy.
- Added AWS domain/DNS/certificate and staged cloud deployment runbook.
- Added deployment preflight JSON and packaging security tests; production remains blocked.
- Consolidated source into one release ZIP with a fresh SHA-256 manifest.


## Build 14 — Netflix-style playback control pass

- Implemented E-04/05/06/07/08/14/15/16/19/21 source: accessible transport, quality, audio/captions, styling, speed, shortcuts, mobile double-tap, picture-in-picture, errors/retry and data saver.
- Added per-profile device-local playback preferences with input validation and compiler-executed tests; E-22 cross-device syncing remains open.
- Updated obsolete Build 04 HLS lifecycle structural gate to recognize ref-based destruction.
- Kept all existing entitlements, signed-CDN/proxy rules, age/territory filters, and payment checks unchanged. No full-stack verification or paid launch approval.


## Build 16 — Series, previews and UX reliability
- Added series/episode mapping and default-off per-profile autoplay (V11), admin link endpoint, public season picker, entitled next-episode lookup and countdown cancellation.
- Generated private seek previews (bounded JPEG/WebVTT), protected their media paths and added spoiler-conscious authenticated timeline scrubbing.
- Added optional 1080p rendition, representative HLS segment decode validation and strict standalone SRT→VTT conversion.
- Fixed mobile navigation reachability, added title loading/network-error states and public Help & FAQ with optional monitored support contact.
- Added new offline tests and release evidence. No live integration, commercial launch or rights sign-off claimed.


## Build 17 — Protected subtitle pipeline and delivery improvements
- Added creator-scoped SRT ingest and HLS WebVTT rendition packaging, private posters and sprite atlas seek previews.
- Added Flyway V12–V14 for media progress, explicit SEO approval and caption sources.
- Added lease-protected processing milestones, creator polling, strict HLS/media allowlists and optional full-segment decode.
- Added consent-respecting in-memory QoE retry, server-side noindex default and rights-aware title SEO/sitemap.
- Added real FFmpeg caption/artwork tests and regression guards; full-stack deployment remains unverified.


## Build 18 recovery checkpoint — 2026-10-22
No application feature changes; consolidates Builds 01–18 from existing cumulative source, preserves all earlier files, and adds reconciled 282- and 117-task evidence tracking. Hourly automation disabled. Live production gates remain unverified.
