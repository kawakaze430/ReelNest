> **LATEST BUILD 31 (2026-10-24):** Real disposable MinIO S3 multipart resume + abort + size guard test passed [GitHub Actions 40064689818](https://github.com/kawakaze430/ReelNest/actions/runs/40064689818). Old Quay MinIO images replaced with Chainguard development images. Read `docs/BUILD31_MINIO_INTEGRATION.md` and `docs/BUILD31_282_TASKS.csv`. D-01 moved to partial; D-02 remains partial; full authenticated creator-to-HLS G1, billing, AWS and production are still open. Counts: 5 CI-verified, 67 source-only, 129 partial, 31 execution-blocked, 17 external-blocked, 33 not started (282 total, zero launch signoffs).\n\n> **BUILD 30 PAYMENT SPI UPDATE (2026-10-24):** G-14 has a server-only `SubscriptionPaymentProvider`; Razorpay remains the only active adapter. No international merchant, payout, payment flow or entitlement is enabled. All 39 advanced IDs audited: 6 partial, 33 not started. Overall 282: 5 CI verified, 67 source-only, 128 partial, 32 execution blocked, 17 external approvals blocked, 33 not started. Read `docs/BUILD30_ADVANCED39.md`. **No production acceptance.**

> **LATEST BUILD 30 (2026-10-24):** Audited all 39 advanced previously unstarted backlog IDs. Five have source/design progress (**F-12, L-17, L-19, P-02, P-10**), 34 remain honestly not started. Read `docs/BUILD30_ADVANCED39.md`, `docs/BUILD30_39_ADVANCED_AUDIT.json`, and `docs/BUILD30_282_TASKS.csv`. All 282 IDs remain cumulative; no production signoff. This is **not** completion of 39 advanced product features. G1–G5 remain open.\n\n> **LATEST BUILD 29 — RESUMABLE CREATOR UPLOADS (2026-10-24):** Continue from GitHub `main`. Source adds owner-scoped S3 multipart part inspection, safe refresh/resume, explicit cancellation and bounded retries. Read `docs/BUILD29_UPLOAD_RESUME.md` and `docs/BUILD29_282_TASKS.csv`. New Maven/Next.js tests are **NOT VERIFIED** because GitHub Actions jobs stopped before any steps executed. G1 full-stack, G2 payments and G3 AWS staging remain OPEN. Previous CI evidence pertains to Build 28.

> **LATEST CHECKPOINT — BUILD 28 (2026-10-24):** Verified [restricted Docker worker build + imports](https://github.com/kawakaze430/ReelNest/actions/runs/40012236201) and [real PostgreSQL Flyway V1–V9 → V25 upgrade](https://github.com/kawakaze430/ReelNest/actions/runs/40012433265). Read `docs/BUILD28_STATUS.md` and `docs/BUILD28_282_TASKS.csv`. **G1 full Docker creator-to-playback, G2 payments, G3 AWS staging, G4 security and G5 worldwide licensing remain OPEN.** All 282 original IDs retained; no production acceptance signed.

> **BUILD 27 DB VERIFIED (2026-10-24):** [Real PostgreSQL Flyway CI passed](https://github.com/kawakaze430/ReelNest/actions/runs/40010523079): Spring Boot Java 21 startup, V1–V25 migration history, 3 verification tests. [Build 27 G1 report](docs/BUILD27_G1_READINESS.md) and [282-task ledger](docs/BUILD27_282_TASKS.csv). Historical DB upgrade + complete video, auth, billing and AWS E2E **remain open**. Counts: 4 CI verified, 67 source-only, 122 partial, 33 execution blocked, 17 external blocked, 39 not started.\n\n> **LATEST SOURCE — BUILD 26 (2026-10-23):** Work from GitHub `main`. Read [Build 26 engineering report](docs/BUILD26_DELIVERY_REPORT.md) and `docs/BUILD26_282_TASKS.csv` before continuing. Exactly 20 original backlog items received evidence or engineering progress, with 4 component-CI-verified, 67 source-implemented/runtime-unverified, 121 partial, 34 execution-blocked, 17 external-approval-blocked and 39 not started. The older Build 25 headings below are historical. **No completed G1–G5 acceptance; no AWS/paid launch authorization.**

> **BUILD 25:** Latest cumulative checkpoint. New: scoped Codex Cloud G0/G1/TF evidence runner and Flyway V1–V25 guard. `docs/BUILD25_282_TASKS.csv` is authoritative; no production acceptance. Read `docs/BUILD25_IMPLEMENTATION.md` and the one-shot prompt at the repository root.

# CURRENT — ReelNest Build 24 cumulative source (2026-10-23)

**This supersedes the Build 23 handoff header below.** Read `docs/BUILD24_IMPLEMENTATION.md`, `docs/AI_HANDOFF/09_BUILD24_CONTINUATION.md`, and `docs/BUILD24_282_TASKS.csv` first. Build 24 repairs PIN-authorized profile deletion, adds backend tests and frontend cleanup, but does **not** pass production release gates. All 282 original task IDs and baseline status counts are preserved; no fully accepted production tasks.

The earlier Build 23 documentation below is retained for provenance, including its older claims about what was then the newest build. Current release decision: **NO-GO**.

---

# READ THIS FIRST — ReelNest Cross-AI Handoff (2026-10-22)

**This is the single cumulative ReelNest source through Build 23, plus a new cross-model handoff package.** It does NOT add a new functional Build 24. The project is NOT production-ready. Do not launch paid subscriptions or worldwide AWS deployment based on the source alone.

## One-minute orientation

- **Goal:** original Netflix-class OTT with verified creator catalog, adaptive secure streaming, payments, recommendations, mobile/TV expansion and eventual licensed worldwide AWS availability.
- **Existing:** `apps/web` Next.js, `apps/api` Spring Boot, `apps/video-worker` FFmpeg, `apps/recommendation-api`/`recommendation-lab`, 25 Flyway migrations, local Compose, AWS Terraform, significant UI/player/creator/security features. **Source does not imply integrated acceptance.**
- **282 source tasks:** 67 implemented (runtime unverified), 119 partial, 40 local/Codex blocked, 17 account/approval blocked, 39 no audited source evidence, 0 fully signed off.
- **Verification from THIS packaging turn:** 7 Build23 contract tests, 59 Python media/ML tests, 83 TypeScript syntax parses passed. Full Maven, npm Next production build, Docker, DB, Razorpay, AWS, CloudFront, security and rights compliance NOT run.
- **Current release decision: NO-GO.** A static AWS Amplify demonstration package exists separately under `deploy/`, but it does NOT offer full authenticated OTT service.
- **History:** cumulative Builds 01–23; old dated README headings are historical. **No scheduled/hourly ReelNest automation should be created without explicit permission.**

## Read in order

1. `docs/AI_HANDOFF/01_PROJECT_CHARTER.md` — exact final goal and core business journey.
2. `docs/AI_HANDOFF/02_CURRENT_SOURCE_AND_LIMITS.md` — what is present and real caveats.
3. `docs/AI_HANDOFF/03_CANONICAL_282_STATUS_AND_BACKLOG.md` — all workstreams and every unfinished task ID.
4. `docs/AI_HANDOFF/04_RELEASE_GATES_AND_REAL_TESTS.md` — exact AWS, payment, media and security release gate plan.
5. `docs/AI_HANDOFF/05_WORK_ALLOCATION_AND_CONTINUATION.md` — owner vs assistant vs Codex, ZIP/output expectations.
6. `docs/AI_HANDOFF/06_PROMPTS_FOR_OTHER_MODELS.md` — ready-to-paste master and scoped prompts.
7. `docs/AI_HANDOFF/07_NEXT_WORK_PACKAGES.md` — ordered batch recommendations.
8. `docs/BUILD23_282_TASKS.csv` — canonical 282 task IDs/statuses. `AI_HANDOFF_CONTEXT.json` and `docs/AI_HANDOFF/ALL_282_TASKS.jsonl` are machine-readable alternatives.

## First commands when terminal tools are available

```bash
python3 scripts/codex_preflight.py
python3 scripts/release_gate.py --mode offline
cd apps/api && mvn -B verify
cd ../web && npm ci && npm run typecheck && npm run build
cd ../..
docker compose config
docker compose up --build -d
bash scripts/validate_local_stack.sh --smoke
```

These are commands to **run**, not recorded passes. `npm ci` requires a reviewed `package-lock.json`; generate one in a connected environment if missing. Never use public developer Compose credentials.

## Mandatory output from next development turn

One **complete cumulative updated ZIP** if tools allow, an updated 282-task register (sum must be 282), exact before→after task IDs, changed file list, actual tests with PASS/FAIL/NOT RUN, remaining blockers by owner, evidence paths, release GO/NO-GO, and the next priorities. **Do not invent files/links or claim success without logs.**
