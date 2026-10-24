> **LATEST BUILD 31 (2026-10-24):** Real disposable MinIO S3 multipart resume + abort + size guard test passed [GitHub Actions 40064689818](https://github.com/kawakaze430/ReelNest/actions/runs/40064689818). Old Quay MinIO images replaced with Chainguard development images. Read `docs/BUILD31_MINIO_INTEGRATION.md` and `docs/BUILD31_282_TASKS.csv`. D-01 moved to partial; D-02 remains partial; full authenticated creator-to-HLS G1, billing, AWS and production are still open. Counts: 5 CI-verified, 67 source-only, 129 partial, 31 execution-blocked, 17 external-blocked, 33 not started (282 total, zero launch signoffs).\n\n> **BUILD 30 PAYMENT SPI UPDATE (2026-10-24):** G-14 has a server-only `SubscriptionPaymentProvider`; Razorpay remains the only active adapter. No international merchant, payout, payment flow or entitlement is enabled. All 39 advanced IDs audited: 6 partial, 33 not started. Overall 282: 5 CI verified, 67 source-only, 128 partial, 32 execution blocked, 17 external approvals blocked, 33 not started. Read `docs/BUILD30_ADVANCED39.md`. **No production acceptance.**

> **LATEST BUILD 30 (2026-10-24):** Audited all 39 advanced previously unstarted backlog IDs. Five have source/design progress (**F-12, L-17, L-19, P-02, P-10**), 34 remain honestly not started. Read `docs/BUILD30_ADVANCED39.md`, `docs/BUILD30_39_ADVANCED_AUDIT.json`, and `docs/BUILD30_282_TASKS.csv`. All 282 IDs remain cumulative; no production signoff. This is **not** completion of 39 advanced product features. G1–G5 remain open.\n\n> **LATEST BUILD 29 — RESUMABLE CREATOR UPLOADS (2026-10-24):** Continue from GitHub `main`. Source adds owner-scoped S3 multipart part inspection, safe refresh/resume, explicit cancellation and bounded retries. Read `docs/BUILD29_UPLOAD_RESUME.md` and `docs/BUILD29_282_TASKS.csv`. New Maven/Next.js tests are **NOT VERIFIED** because GitHub Actions jobs stopped before any steps executed. G1 full-stack, G2 payments and G3 AWS staging remain OPEN. Previous CI evidence pertains to Build 28.

> **LATEST CHECKPOINT — BUILD 28 (2026-10-24):** Verified [restricted Docker worker build + imports](https://github.com/kawakaze430/ReelNest/actions/runs/40012236201) and [real PostgreSQL Flyway V1–V9 → V25 upgrade](https://github.com/kawakaze430/ReelNest/actions/runs/40012433265). Read `docs/BUILD28_STATUS.md` and `docs/BUILD28_282_TASKS.csv`. **G1 full Docker creator-to-playback, G2 payments, G3 AWS staging, G4 security and G5 worldwide licensing remain OPEN.** All 282 original IDs retained; no production acceptance signed.

> **BUILD 27 DB VERIFIED (2026-10-24):** [Real PostgreSQL Flyway CI passed](https://github.com/kawakaze430/ReelNest/actions/runs/40010523079): Spring Boot Java 21 startup, V1–V25 migration history, 3 verification tests. [Build 27 G1 report](docs/BUILD27_G1_READINESS.md) and [282-task ledger](docs/BUILD27_282_TASKS.csv). Historical DB upgrade + complete video, auth, billing and AWS E2E **remain open**. Counts: 4 CI verified, 67 source-only, 122 partial, 33 execution blocked, 17 external blocked, 39 not started.\n\n> **LATEST SOURCE — BUILD 26 (2026-10-23):** Work from GitHub `main`. Read [Build 26 engineering report](docs/BUILD26_DELIVERY_REPORT.md) and `docs/BUILD26_282_TASKS.csv` before continuing. Exactly 20 original backlog items received evidence or engineering progress, with 4 component-CI-verified, 67 source-implemented/runtime-unverified, 121 partial, 34 execution-blocked, 17 external-approval-blocked and 39 not started. The older Build 25 headings below are historical. **No completed G1–G5 acceptance; no AWS/paid launch authorization.**

> **GITHUB SHARED WORKFLOW (2026-10-23):** The private GitHub repository `kawakaze430/ReelNest`, branch `main`, is now the canonical cumulative source. New ZIP exports are optional, not mandatory. ChatGPT owns feature development and targeted repairs; Codex Cloud should perform only missing runtime/integration verification and narrowly scoped fixes. Read the *latest* `main` before work, not an old handoff ZIP. **For routine work, commit and push directly to `main` when the environment permits and the user has authorized it; don't create task branches or pull requests by default.** Never force-push, reset shared history, overwrite newer commits, edit applied migrations in place, or bypass required repository protections. If `main` advances or a push is rejected, fetch/reconcile safely; if unclear, stop and report the conflict. If Codex Cloud cannot push to `main` due to platform restrictions, report that limitation and return the exact committed SHA or patch; never claim a push succeeded. Never commit secrets, build artifacts, dependency trees or production credentials. When handing work back, supply the final `main` SHA, changed files, actual CI/test evidence, and unresolved blockers. Parallel agents must not edit the same files simultaneously. G0 CI success is not G1 Docker/media integration, payment testing, or AWS deployment acceptance.\n\n> **LATEST BUILD 25 (2026-10-23):** Browser ChatGPT owns development. Codex Cloud is LIMITED to real build/integration evidence. For its one-shot test task read `REELNEST_CODEX_CLOUD_ONE_SHOT_PROMPT.md` and `docs/AI_HANDOFF/10_BUILD25_CLOUD_TEST_DELEGATION.md`. Execute `python3 scripts/cloud_evidence.py --phase g0` first. Never deploy or use paid cloud resources.

> **LATEST BUILD 24 (2026-10-23):** Read `docs/BUILD24_IMPLEMENTATION.md`, `docs/AI_HANDOFF/09_BUILD24_CONTINUATION.md` and `docs/BUILD24_282_TASKS.csv` first. No Maven/Docker/AWS production acceptance. Older guidance below is historical.

> **CURRENT CONTEXT:** Read `AI_START_HERE.md`, `docs/AI_HANDOFF/04_RELEASE_GATES_AND_REAL_TESTS.md`, and `docs/BUILD23_282_TASKS.csv` before making changes. These supersede historical dated release claims.

# ReelNest instructions for coding agents (Build 17+)

## Mission
Convert the existing OTT monorepo into a **verified** local end-to-end application. Do not rewrite it or claim a launch before the release gates pass. The main user goal is working film upload -> HLS encoding -> licensed publication -> authorized playback, then TEST-only billing and AWS staging.

## Repo map
- `apps/api`: Java 21, Spring Boot, Flyway, Keycloak resource-server auth, rights, billing, playback.
- `apps/web`: Next.js 16 viewer, creator, admin UIs.
- `apps/video-worker`: FFmpeg worker for private source -> private HLS media.
- `apps/recommendation-api`: private FastAPI ML scoring, must not change entitlements.
- `compose.yaml`: **development-only**, hardcoded demo credentials, bound to loopback.
- `infrastructure/aws`: unverified Terraform skeleton. **Do not apply by default**.
- `scripts/release_gate.py`: offline tests and external-blocker ledger, NOT live evidence.
- `docs/RELEASE12_PRODUCTION_READINESS.md`: known limitations and all unverified gates.

## Safety boundaries
1. Never store/print real passwords, API keys, Razorpay keys, AWS credentials, Keycloak admin tokens, or customer data in code, commits, logs, test outputs or chat.
2. Use **test mode only** for Razorpay. Never initiate live charges, refunds, or subscriptions. Do not provision billable AWS resources, change DNS, push production images or change a production DB without the human owner's explicit approval.
3. Never disable ownership checks, media rights, age and territory controls to make E2E tests pass. Use an original test asset, never copyrighted commercial films.
4. Do not treat syntax parsing as `mvn verify`, `npm run build`, or an actual Docker integration run. Explicitly report `not run` when appropriate.
5. Make small targeted patches, add a regression test for a reproduced defect, and preserve existing modules/migrations. Never edit already-applied Flyway migrations in-place: add a new numbered migration.
6. Keep the local dev stack local. Never expose `compose.yaml` demo passwords or databases to the internet.
7. Keep artifacts and run logs under `build-evidence/` (gitignored). Never commit env files, generated credentials, backups, keys, Terraform state or production deployment attestations.

## Validation order (inside repo root)
```bash
python3 scripts/codex_preflight.py
python3 scripts/release_gate.py --mode offline
cd apps/api && mvn -B verify && cd ../..
cd apps/web && npm install --no-audit --no-fund && npm run typecheck && npm run build && cd ../..
docker compose config -q
bash scripts/validate_local_stack.sh --smoke
# --full only when using a disposable development database / test accounts
bash scripts/validate_local_stack.sh --full
```
Run each available step, record command, exit code, relevant error, fix, then rerun. Do not destroy Docker volumes or reset persistent state without asking.

## Efficient work policy
- LOCAL/CODEX track: prioritize the **first failing real compilation/integration check** and fix it before declaring a release. Separate ChatGPT feature batches may continue in parallel as requested by the owner, but source-only changes never clear launch gates.
- Builds 15–17 add Flyway **V10–V14**. Read `docs/BUILD17_IMPLEMENTATION.md` and preserve every prior migration; test both clean and upgraded databases.
- Build 17 adds creator SRT caption upload/HLS alternate subtitles, V12 processing progress, and V13 explicit SEO approval. Verify caption owner/draft guards, 403/404 denial, WebVTT packaging, noindex defaults and active-rights filtering. Never make title indexing opt-out.
- A standalone Python FFmpeg test passing is not evidence that Safari, hls.js, CloudFront or signed cookies work together.
- When blocked by missing credentials, permissions, external resources or a human decision, do not invent a result; implement safe local tests and describe the exact handoff.
- Finish each task with: `Files changed`, `Tests actually run`, `Remaining blockers`, `Next command`; write concise findings to `build-evidence/CODEX_SESSION.md`.
- Ask for authorization only for destructive or chargeable actions, not ordinary repo edits or tests.
