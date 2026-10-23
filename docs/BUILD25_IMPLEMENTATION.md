# ReelNest Build 25 — Codex-minimal real-testing handoff

**Date:** 2026-10-23. **Release: NO-GO** for staging/pilot/production. This is a complete cumulative release based on Build 24; no previous application file or Flyway migration was removed. The purpose is to conserve limited Codex Cloud access while the browser ChatGPT engineer retains responsibility for development and ZIP packaging.

## Actual source changes

- `scripts/cloud_evidence.py` (new): one-command real G0 Maven + Next.js compilation, npm lockfile generation, Docker Compose validation; optional explicitly authorized disposable G1 full-stack validator and Terraform backend-disabled validation. Returns per-step PASS/FAIL/BLOCKED/TIMEOUT, raw logs in ignored `build-evidence/`, JSON and Markdown summaries. It never runs `terraform plan/apply`, starts paid cloud services, or resets volumes.
- `scripts/validate_local_stack.sh`: corrected stale migration check requiring only 15 successful versions. It now checks for **all successful Flyway V1–V25 versions** and no failed migrations, rather than accepting incomplete upgrades. This new SQL check is source-tested but **not executed against PostgreSQL** in the browser environment.
- `tests/handoff/test_cloud_evidence.py` (new): offline tests for G1 safety confirmation, sanitization, no explicitly prohibited destructive calls, 25-migration guard, and unique 282 task IDs.
- `REELNEST_CODEX_CLOUD_ONE_SHOT_PROMPT.md` (new): a short complete prompt to paste into Codex Cloud. 
- `docs/AI_HANDOFF/10_BUILD25_CLOUD_TEST_DELEGATION.md` (new): complete task-routing and fallback matrix for ChatGPT, Codex Cloud, Antigravity, Google Colab, GitHub Actions, AWS and local Windows.
- `docs/BUILD25_282_TASKS.csv`: all original 282 task IDs preserved, with unchanged classifications and Build 25 progress notes.
- `AI_START_HERE.md`, `AGENTS.md`, `README.md`, `AI_HANDOFF_CONTEXT.json`, `docs/BUILD_VERSION`: Build 25 pointers.
- `docs/BUILD25_TEST_EVIDENCE.json`, `docs/BUILD25_SHA256SUMS.txt`: local test report and package integrity manifest.

## Actually executed in the authoring environment

1. `python -m pytest tests/handoff/test_cloud_evidence.py tests/release/test_ai_handoff.py tests/release/test_build24_profile_pin.py -q --disable-warnings` → **15 passed** after repairing an initial new-test fixture error (recorded as a fixed test failure, not hidden).
2. `PYTEST_DISABLE_PLUGIN_AUTOLOAD=1 python -m pytest apps/recommendation-api/tests apps/recommendation-lab/tests apps/video-worker/tests/test_timed_text.py apps/video-worker/tests/test_caption_packaging.py -q --disable-warnings` → **37 passed**.
3. `bash -n scripts/validate_local_stack.sh` → **PASS** (shell syntax only).
4. `python -m py_compile scripts/cloud_evidence.py` → **PASS**.
5. `python scripts/check_env_contracts.py` → **PASS** static checks.
6. `python scripts/check_static.py` → **PASS** static checks.
7. `node scripts/check_ts_syntax.mjs` → **PASS**, 83 TS/TSX files syntactically parsed; not semantic compilation.

**NOT RUN**: Maven build/JUnit, Next.js npm ci/typecheck/production build, Docker/Keycloak/PostgreSQL V1–V25, FFmpeg upload/playback E2E, Terraform provider init/validate, Razorpay TEST, any AWS deployment. Do not count source checks as acceptance.

## Status counts preserved

| Classification | Build 24 | Build 25 |
|---|---:|---:|
| Source implemented — runtime unverified | 67 | 67 |
| Partially implemented | 119 | 119 |
| Blocked — external execution / operator verification | 40 | 40 |
| Blocked — external approval / account | 17 | 17 |
| Not started / no audited implementation evidence | 39 | 39 |
| Total | 282 | 282 |

## Codex acceptance contract

Use the one-shot prompt. Execute `python3 scripts/cloud_evidence.py --phase g0` from the extracted repo in a prepared cloud environment. Never assume Docker/internet. G1 requires explicit disposable-container confirmation; `--phase tf` is backend-disabled validation only. Codex reports actual log evidence and one minimal compile fix per failing component, then returns to ChatGPT/browser to integrate and package. Do not assign feature development to Codex.

## After Codex evidence returns

The browser ChatGPT engineer must inspect the actual test reports, merge any targeted patches, fix source issues here where technically possible, preserve all 282 task IDs, and issue a new complete cumulative ZIP. Remaining gating work includes real payment sandbox workflows, cloud staging security and legally cleared media rights. No independent work occurs between user messages.
