> **LATEST BUILD 25 (2026-10-23):** Browser ChatGPT owns development. Codex Cloud is LIMITED to real build/integration evidence. For its one-shot test task read `REELNEST_CODEX_CLOUD_ONE_SHOT_PROMPT.md` and `docs/AI_HANDOFF/10_BUILD25_CLOUD_TEST_DELEGATION.md`. Execute `python3 scripts/cloud_evidence.py --phase g0` first. Never deploy or use paid cloud resources.

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
