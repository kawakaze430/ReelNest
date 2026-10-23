# 02 — Current implementation: what exists and how certain we are

**Authoritative source:** Build 23 cumulative archive (Builds 01–23), inspected 2026-10-22. This handoff revision adds documentation/tracking only; it does **not** imply a new functional Build 24 or deploy a service.

## Source inventory

| Area | Existing source | Missing evidence / known limits |
|---|---|---|
| Next.js UI (`apps/web`) | Viewer browsing, profiles, creator/admin areas, search/discovery, billing screens, player, collections, channels guide, watch parties, PWA, three-language navigation groundwork | Full dependency-based `tsc`/`next build`, cross-browser E2E and accessibility QA not proven |
| Spring Boot (`apps/api`) | Modular catalog, identity/roles, creator rights, multipart uploads, playback sessions, watchlists, payment state, subscriptions, recommendations, admin analytics, privacy, support, content review, scheduled publishing, editorial/channel/watch-party APIs | Real Maven compile and servlet/runtime tests unverified; authorization policy needs live adversarial tests |
| Flyway (`apps/api/src/main/resources/db/migration`) | **V1 through V25** SQL migration files | Must apply to BOTH fresh and upgraded PostgreSQL; no real migration proof yet |
| Video worker (`apps/video-worker`) | FFmpeg adaptive HLS, captions/SRT→WebVTT, thumbnail sprites, multiple audio tracks, processing leases/retries, optional standalone DASH packager | Media-focused Python tests available; actual authenticated upload→worker→private CDN→player E2E unverified. DASH not integrated as secure production media path |
| AI (`apps/recommendation-api`, `apps/recommendation-lab`) | Internal FastAPI model endpoint, TF-IDF/collaborative evaluation, search/experiment experiments, rights-filtered Java candidate handoff/fallback | In-service model quality, latency, privacy and A/B performance not verified on representative data |
| Local stack (`compose.yaml`) | PostgreSQL, Keycloak, Redis, RabbitMQ, MinIO, workers, web, API, ML | Docker start/image builds and clean database apply not proven in this environment; contains LOCAL-ONLY credentials |
| AWS (`infrastructure/aws`) | Terraform for networking, ECS, S3/CloudFront, RDS, Redis, DNS/TLS etc. | Provider `init/validate/plan`, service integration, budgets and security review unverified; **nothing deployed** |
| Payments (`billing`) | Razorpay sandbox integration source, verified-charge entitlement design, webhook idempotency/reversal policies, promotions quote, refund-review workflow | No provider-connected charge/refund/webhook/reconciliation sign-off; NO live merchant launch |
| Protection (`playback`, privacy) | Server-side playback checks, demo proxy, signed-cookie signing, geofencing scaffolding, child filters, account-erasure restricted path, token revocation | True edge/territory enforcement, production DRM, cross-system erasure and mobile/device testing outstanding |
| Operations | CI workflows, smoke scripts, offline tests, archive/checksum reports, on-call/incident and legal-readiness docs | Full monitoring alerts, restore drill, load/chaos tests and qualified reviews not established |

## Recent increment history

- **Builds 01–04:** baseline frontend/backend, identity/profile/catalog, creator rights/uploads, HLS, protected player/Continue Watching.
- **05–08:** Razorpay subscription logic, ML/analytics, AWS Terraform, security/privacy operations.
- **09–13:** runtime/production guards, release gates, ML serving integration, erasure source, Codex handoff.
- **14–18:** Netflix-style player UI, QoE, skip cues, Next Episode, sprites/captions, multilingual audio, ML evaluation.
- **19–22:** OIDC audience/token revocation; support and rights cases; profile PIN, reviewed imports, scheduling, notices, reporting, experiments/semantic search.
- **23:** editorial rails, scheduled channel EPG (not FAST), invite-only watch-party coordination (polling, not frame sync), standalone optional DASH packager, privacy-safe PWA shell, EN/HI/AR navigation, keyboard/accessibility groundwork and DRM capability diagnostic (not DRM itself).

**What has NOT happened:** fully integrated Java/Next build + PostgreSQL + MinIO + worker + authenticated browser playback; real Razorpay sandbox acceptance; live AWS deployment; international rights compliance; independent security audit; commercial DRM licensing; a complete native app ecosystem. Therefore **production sign-off is NO-GO**.

**Caution:** The historical README contains dated status headings. For subsequent work, prefer the latest `docs/AI_HANDOFF/*` and the `docs/BUILD23_282_TASKS.csv` tracker over older build reports.
