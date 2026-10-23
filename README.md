> **LATEST CHECKPOINT — BUILD 27 (2026-10-24):** GitHub `main` is canonical. Read `docs/BUILD27_G1_READINESS.md` for fixed media-worker Docker import graph and disposable GitHub Actions integration workflow. Real G1 acceptance, billing, AWS staging and worldwide rights remain **OPEN**. The original 282-task classifications remain in `docs/BUILD26_282_TASKS.csv`, unchanged by source-only checks.

> **LATEST SOURCE — BUILD 26 (2026-10-23):** Work from GitHub `main`. Read [Build 26 engineering report](docs/BUILD26_DELIVERY_REPORT.md) and `docs/BUILD26_282_TASKS.csv` before continuing. Exactly 20 original backlog items received evidence or engineering progress, with 4 component-CI-verified, 67 source-implemented/runtime-unverified, 121 partial, 34 execution-blocked, 17 external-approval-blocked and 39 not started. The older Build 25 headings below are historical. **No completed G1–G5 acceptance; no AWS/paid launch authorization.**

> **BUILD 25 (2026-10-23):** This package adds minimal Codex Cloud verification workflow; see `REELNEST_CODEX_CLOUD_ONE_SHOT_PROMPT.md`, `scripts/cloud_evidence.py`, and `docs/BUILD25_IMPLEMENTATION.md`. Status counts remain unverified; no paid AWS deployment.

> **CURRENT CHECKPOINT — BUILD 24 (2026-10-23):** [Implementation, limitations and evidence](docs/BUILD24_IMPLEMENTATION.md), [282-task tracker](docs/BUILD24_282_TASKS.csv), [handoff](docs/AI_HANDOFF/09_BUILD24_CONTINUATION.md). Full compilation, integrated runtime and worldwide deployment are **not verified**. Earlier Build 23 text below is preserved as historical context.

> **CROSS-MODEL HANDOFF (2026-10-22):** Start with [AI_START_HERE.md](AI_START_HERE.md) and the [AI handoff directory](docs/AI_HANDOFF/). Current cumulative code is Build 23; no production release sign-off. Older headings below may refer to historical checkpoints.

# ReelNest — Build 23 cumulative source checkpoint (2026-10-22)

**This is the newest cumulative development version (Builds 01–23), not an approved production release.**
Start with [`docs/BUILD23_IMPLEMENTATION.md`](docs/BUILD23_IMPLEMENTATION.md),
[`docs/BUILD23_STATUS_SUMMARY.md`](docs/BUILD23_STATUS_SUMMARY.md), and
[`docs/BUILD23_282_TASKS.csv`](docs/BUILD23_282_TASKS.csv) for the latest changes, evidence and outstanding work.

Build 23 adds curated collections, scheduled EPG channels, invitation-only watch-party state,
optional FFmpeg DASH/fMP4 packaging, a privacy-safe offline PWA shell,
English/Hindi/Arabic navigation translations, a canonical slug route, focus accessibility,
and a manual browser DRM capability diagnostic. **These additions do not grant playback rights,
create a working FAST live stream, license DRM, or deploy the platform.**

The 57 existing local/Codex/provider blockers are unchanged. Acceptance sign-offs remain at zero.

---

# ReelNest — Builds 01–18 Reconciled Source (2026-10-22)

**Current source release: Build 18.** This is the single cumulative repository including earlier builds. The previous automated hourly ReelNest task has been disabled at the owner's request. **No full production launch approval.**

Begin at [`docs/RECOVERY_AUDIT_2026-10-22.md`](docs/RECOVERY_AUDIT_2026-10-22.md) for exact provenance and verification, [`docs/PRIORITY_117_TASKS.md`](docs/PRIORITY_117_TASKS.md) for the NEW 117-item priority selection, and [`docs/MASTER_IMPLEMENTATION_CHECKLIST.md`](docs/MASTER_IMPLEMENTATION_CHECKLIST.md) for the original full 282-item checklist. Original IDs and acceptance boxes are retained; no task has been marked verified solely from code presence or offline tests.

Older build summaries below are historical and may use language such as "latest" referring to their own generation date. For current status, use the three documents above and [`docs/PROJECT_STATE.md`](docs/PROJECT_STATE.md).

---

**Historical Build 17 checkpoint (not production-ready).** Protected creator subtitles, private sprite previews, processing progress, opt-in QoE replay and rights-aware SEO were added. See [`docs/BUILD17_IMPLEMENTATION.md`](docs/BUILD17_IMPLEMENTATION.md). Full Maven/Next.js compilation, V12–V14 Flyway execution, Docker, payment and cloud gates remain unverified.

**Historical Build 16 checkpoint.** See `docs/BUILD16_IMPLEMENTATION.md` for the ten tasks addressed, tests, partial work and local/Codex follow-up. **Not production-ready.**

# ReelNest — Release 12 cumulative source (Builds 01–12)

**Status: Offline-tested development checkpoint; NOT cleared for public paid OTT.** See [the complete readiness and 12-task blocker register](docs/RELEASE12_PRODUCTION_READINESS.md). All prior source remains in this ZIP. Build 12 adds the internal ML recommendation HTTP service, rights-preserving Java fallback, guarded free-viewer Keycloak + database erasure path, PWA/mobile manifest, Safari native HLS via CDN cookies, privacy admin page, staging realm generator, local Docker acceptance runner, disposable PostgreSQL restore test, and CI verification workflows.

## Validated in authoring environment
- 14 offline release checks (including Java helper assertions, media processing tests, and ML HTTP tests).
- Browser-only viewer, creator and playback previews exercised with Chromium.
- Python automated tests, Docker Compose YAML parser and TypeScript syntax parser passed.
- Maven build, full Next.js dependency install, Docker service startup, actual Razorpay payments, Terraform provider validation, DRM and cloud deployment **were NOT run**.

## Start here
- Preview-only AWS hosting: `docs/AWS_DOMAIN_AND_DEPLOYMENT.md`, `deploy/reelnest-amplify-preview.zip`.
- Real local backend: `bash scripts/validate_local_stack.sh --smoke` (requires Docker and external image download).
- Integration on clean local stack: `bash scripts/validate_local_stack.sh --full`.
- Staging identity/Keycloak configuration: `docs/RELEASE12_PRODUCTION_READINESS.md`.
- One command for verified offline checks: `python3 scripts/release_gate.py --mode offline`.

---

# START HERE — AWS deployment

**Quick public preview:** `deploy/reelnest-amplify-preview.zip` is a ready-to-upload static showcase for AWS Amplify manual deployment. **Full OTT application remains unverified and must not accept real payments.** Read `docs/AWS_DOMAIN_AND_DEPLOYMENT.md` before creating cloud resources.

# ReelNest — cumulative OTT source project (Builds 01–10)

**This is a development checkpoint, NOT a verified, deployed, licensed or production-ready video service.** Builds 09–10 harden deployment configuration and add release evidence. The cumulative repository retains previous source for catalog, creators, rights, uploads, HLS, protected playback, subscriptions, recommendations and analytics.

## Project modules

| Area | Path | Reality |
|---|---|---|
| Next.js viewer, creator and admin frontend | `apps/web` | Source implemented; full npm build pending |
| Java 21 Spring Boot API and PostgreSQL Flyway V1–V8 | `apps/api` | Source implemented; Maven/full schema integration pending |
| FFmpeg/HLS background worker | `apps/video-worker` | Offline media tests passed; live MinIO/DB integration pending |
| Offline TF-IDF/collaborative model | `apps/recommendation-lab` | Offline tests pass; not connected to live serving |
| Local developer stack | `compose.yaml` | Requires Docker, not run in this environment |
| AWS staging infrastructure | `infrastructure/aws` | Terraform source; provider validation/apply pending |
| Release gate | `scripts/release_gate.py` | 12 offline checks pass, 12 external release items blocked |

## First-time local setup (Docker and internet required)

```bash
cd reelnest
docker compose config
docker compose up --build -d
docker compose ps
python3 scripts/live_smoke.py
```

- Web: `http://localhost:3000`
- API health: `http://localhost:8080/actuator/health`
- Keycloak: `http://localhost:8180`
- MinIO API: `http://localhost:9000`

**Do not expose the local stack to the internet.** The developer credentials appear only in the local Compose file. Fictional catalog content is not licensed streaming media; upload an original/authorized film for an integration test.

## Release testing

```bash
python3 scripts/release_gate.py --mode offline --output docs/BUILD10_CHECK_RESULTS.json
node scripts/check_ts_syntax.mjs
bash scripts/test_deployment_policy_java.sh
python3 -m pytest apps/video-worker/tests apps/recommendation-lab/tests tests/release -q
# Must fail until externally reviewed evidence exists:
python3 scripts/release_gate.py --mode production
```

**External tests not run by the authoring environment:** `mvn verify`, `npm install`/`npm run typecheck`/`npm run build`, live Docker, Flyway PostgreSQL startup, Razorpay TEST, Terraform provider validation, CloudFront, security/backup/legal review. See `docs/BUILD10_CHECK_RESULTS.json` and `docs/REMAINING_WORK_DETAILED.md`.

For real deployment, use **separate** database migration/runtime roles, secure Secrets Manager entries and externally provisioned HTTPS OIDC. `scripts/build_production_web.sh` validates all embedded public URLs. CloudFront is India-allowlisted by default, with operational geo review disabled until tested. The account deletion request remains an admin-review request, not completed erasure.

## Continuation guide

Begin with `docs/PROJECT_STATE.md`, `docs/BUILD09_10_IMPLEMENTATION.md`, `docs/REMAINING_WORK_DETAILED.md` and `docs/DEPLOYMENT_AND_LAUNCH.md`. Do not enter production payment keys until provider sandbox and security tests are complete.

## Build 13 handoff: using Codex efficiently

For an actual runtime compile/test session, see [`docs/CODEX_SETUP_AND_HANDOFF.md`](docs/CODEX_SETUP_AND_HANDOFF.md) and read [`AGENTS.md`](AGENTS.md) before invoking Codex. Run `python3 scripts/codex_preflight.py` first. This handoff adds developer instructions and prerequisite tests; **it does not certify full Docker integration, paid subscriptions, AWS staging, DRM or legal readiness**. Do not use the developer stack or demo credentials for public hosting.

### Build 14 — Expanded player controls

The authenticated playback page now has transport buttons, accessible shortcuts, adaptive/manual quality selection, available HLS audio/subtitles, per-profile device preferences, caption styling, picture-in-picture where supported, data saver, and clearer retry/error controls. See `docs/BUILD14_PLAYER_IMPLEMENTATION.md` for complete feature IDs, tests and unverified device limitations. This does not grant additional film rights or bypass premium billing.


## Build 20 — private support + rights cases (source checkpoint)

Authenticated ticket/appeal APIs, admin moderation queue, rights-driven unpublish safeguards, admin-only CSV export, and a reclassified 282-task board were added. **These modules are not yet Maven/Docker/integration verified.** See `docs/BUILD20_IMPLEMENTATION.md` and `docs/BUILD20_282_STATUS_REPORT.md`.


## Build 20 — private support + rights cases (source checkpoint)

Authenticated ticket/appeal APIs, admin moderation queue, rights-driven unpublish safeguards, admin-only CSV export, and a reclassified 282-task board were added. **These modules are not yet Maven/Docker/integration verified.** See `docs/BUILD20_IMPLEMENTATION.md` and `docs/BUILD20_282_STATUS_REPORT.md`.


## Build 20 — private support + rights cases (source checkpoint)

Authenticated ticket/appeal APIs, admin moderation queue, rights-driven unpublish safeguards, admin-only CSV export, and a reclassified 282-task board were added. **These modules are not yet Maven/Docker/integration verified.** See `docs/BUILD20_IMPLEMENTATION.md` and `docs/BUILD20_282_STATUS_REPORT.md`.
