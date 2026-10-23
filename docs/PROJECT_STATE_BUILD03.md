# ReelNest continuation ledger — Build 03

**Checkpoint:** 2026-10-21, `reelnest-build-03-hls-worker`  
**Distribution:** entire cumulative repository, not a patch  
**Status:** Python HLS pipeline media-tested offline, Java/TypeScript syntax parsed; Java/Next.js/Docker full stack NOT runtime verified.

## Finished/implemented in this turn

| Task ID | Deliverable | Status / evidence |
|---|---|---|
| M5.01 | SQL V4 durable media_processing_jobs with unique asset ID, lease, retry, backoff, failure and backfill | Code present; PostgreSQL migration NOT executed |
| M5.02 | Atomic upload completion -> media QUEUED + job QUEUED | Java code present; no live Spring compile/integration |
| M5.03 | S3-success/DB-failure retry reconciliation with HEAD | Java + Python recovery path authored; live MinIO not tested |
| M5.04 | Separate Python worker with `SKIP LOCKED` and lease renewal | Code + Dockerfile present; no DB integration tested |
| M5.05 | FFprobe input checks, bounded FFmpeg HLS transcoding | **Executed: original generated clips, 5 passing tests** |
| M5.06 | 360/480/720 adaptive HLS playlists with safe no-upscale fallback | **Executed: decoded generated segments** |
| M5.07 | Private processed-bucket upload with unique attempt prefix | Code present; no MinIO live test |
| M5.08 | READY transition only after all uploaded files + retained lease | Code present; no live PG/S3 test |
| M5.09 | Retry delay, terminal rejection/failure and crashed-worker expiry | Code + structural check present; DB not live verified |
| M5.10 | Periodic multipart expiry cleanup + orphan reconciliation | Code present; worker runtime not live verified |
| M5.11 | MinIO source-bucket ETag CORS configuration | XML + Compose mount present; live MinIO not verified |
| M5.12 | Creator/admin media status APIs and retry action | Java source present; Java not compiled |
| M5.13 | Creator Studio/Admin Review show media state and retry count | TS syntax parsed; Next.js type/build not run |
| M5.14 | Admin publication becomes possible only with media READY + approved current rights | Backend guard retained; no live publication test |
| M5.15 | Optional end-to-end real browser integration script | Authored NOT executed |
| M5.16 | Offline original demo and Build 02 preview remain functional | **Executed: existing media and previews passed** |
| M5.17 | CI media-worker test job + cumulative ZIP manifest and notes | Authored/validated statically |

## Milestone checklist

### Build 01 / Foundation
- [x] Spring Boot/Next.js/compose source, sample catalog, user profiles and original HLS demo.
- [x] Offline storefront preview and original sample segment validation.
- [ ] Verify Maven, npm and Docker services end-to-end in a machine with network/Docker.

### Build 02 / Creator rights & ingestion
- [x] Creator application, admin approval, drafts, rights declaration and upload intent source.
- [x] MinIO S3-compatible private multipart protocol and creator/admin UI source.
- [ ] Complete real multipart upload against live MinIO; verify header CORS, cookies, Keycloak claims, live Flyway V3.
- [ ] Signed rights-evidence upload and real document/legal verification workflow.

### Build 03 / Processing
- [x] Durable V4 PostgreSQL worker-job scheme with Build 02 migration backfill.
- [x] FFprobe input inspection and tested FFmpeg 360/480/720 HLS encoder.
- [x] Worker claiming, leases, retries, private output, processing statuses, eventual repair source.
- [x] Admin publish READY+approved-rights guards with live state/status UIs.
- [ ] **Must run** live Docker integration: build images, apply Flyway V4, connect Keycloak, complete multipart upload, worker encode in MinIO, review/publish.
- [ ] Stronger multipart upload resume, malware scanning, disk quotas, failure injection, lifecycle deletion, secure untrusted codec execution.
- [ ] Subtitle/audio tracks, adaptive bitrate quality tuning, CMAF, encryption and production DRM.

### Build 04 / Protected playback (NEXT)
- [ ] Design playback entitlement including rights territories, term, profile age, subscription/free access and concurrent streams.
- [ ] Implement playback sessions and an authorization endpoint; no private S3 raw access.
- [ ] Local private HLS proxy/temporary signed access; CloudFront signer for production.
- [ ] Shaka/hls.js protected player and profile watch progress.
- [ ] Rights expiry and revocation during active sessions.
- [ ] First repair any runtime issues identified in Build 03 full-stack checks.

### Build 05 / Payments
- [ ] Verified Razorpay test checkout, subscriptions, webhook signatures, deduplication and idempotency.

### Build 06 / Discovery AI
- [ ] Ranking and offline evaluation, personalization, analytics, semantic retrieval.

### Build 07 / Infrastructure
- [ ] AWS infrastructure-as-code, CI/CD deployment, monitoring and operational playbooks.

### Build 08 / Launch hardening
- [ ] Actual media-rights agreements, OTT age classification, regulatory/privacy compliance, security audit, E2E load tests.

## Core decisions

1. **SQL polling as durable work queue** for media processing to avoid double writes in the source-upload transaction. RabbitMQ remains available for future event fan-out.
2. Private S3/MinIO buckets only. Public catalog **never implies public playback**.
3. Inputs can be corrupt/hostile; worker is nonroot with memory/CPU limits and validation; security hardening remains a prerequisite for public untrusted uploads.
4. Creator can only see media status for their own approved-creator title; admin sees all processing statuses but no raw S3 keys.
5. Code exists but integration is **not verified** until the user's Docker/Maven/npm-enabled environment executes the test plan.

## Next handoff

For Build 04, attach `reelnest-build-03-hls-worker.zip` if another session doesn't have current files. Ask: **"Continue ReelNest Build 04 from docs/PROJECT_STATE.md. Preserve the entire existing source; first fix any integration problems; implement protected entitlement-based playback and resume. Return a new cumulative ZIP, validation report and remaining task list."**

Next ZIP suggested filename: `reelnest-build-04-protected-playback.zip`.
