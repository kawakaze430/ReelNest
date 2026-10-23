# ReelNest Build 28 — verified Docker worker and real database upgrade

GitHub `main` is the sole source of truth. This checkpoint builds on Build 27 without overwriting V1–V25 or resetting historic data. These are **component/infrastructure proofs**, not acceptance of full OTT streaming or worldwide launch.

## Verified work and exact CI evidence
1. **Worker image:** [GitHub Actions 40012236201](https://github.com/kawakaze430/ReelNest/actions/runs/40012236201) passed `docker build` for `apps/video-worker` and executed inside an isolated read-only non-root container with no networking/capabilities: Python worker imports and `ffmpeg -version`. This covers part of A-08 and D-05. No source upload, database lease, or encoding job ran in that test.
2. **Flyway upgrade:** [GitHub Actions 40012433265](https://github.com/kawakaze430/ReelNest/actions/runs/40012433265) passed a real PostgreSQL 17 test: apply V1–V9, migrate the same isolated DB through V25 (16 additional migrations), verify all 25 history rows in order, and confirm repeat migration is idempotent. The job also booted the Spring Boot JAR against a separate clean database applying V1–V25. The upgrade test reported `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`.
3. **Ongoing existing verification:** Java Maven tests, Next.js TypeScript and production build, worker unit tests, static release checks, and Terraform schema validation are run by `.github/workflows/checks.yml`. Review the newest commit's results before declaring its CI green.

## Files changed
- `.github/workflows/checks.yml` — added `worker-image` CI job to build and test the real non-root container, checking Python imports and FFmpeg.
- `apps/api/src/test/java/com/reelnest/FlywayUpgradeTest.java` — real V1–V9 to V25 upgrade/idempotency test, opt-in gated to CI's disposable PostgreSQL database.
- `.github/workflows/postgres-flyway.yml` — provisions isolated `reelnest_upgrade` DB in disposable PostgreSQL 17 CI; runs real upgrade test after clean schema startup.
- `docs/BUILD28_282_TASKS.csv` — all 282 original task IDs, prior columns and acceptance flags retained; only A-08 and A-11 task classifications updated.
- `tests/release/test_build28_status.py` — audit check for exact 282-task status totals and unchanged historical columns.
- `docs/BUILD28_STATUS.md` and headers — latest cross-model checkpoint.

## Updated 282-task classification (none with full production acceptance)
| Status | Tasks |
|---|---:|
| Component CI verified — production acceptance pending | 5 |
| Source implemented — runtime unverified | 67 |
| Partially implemented | 122 |
| Blocked on external execution/operator verification | 32 |
| Blocked on external approvals/accounts | 17 |
| Not started/no audited implementation | 39 |
| **Total** | **282** |

A-11 is now **component verified** for its named database migration checks; production schema/data recovery is still open. A-08 moves to **partial** because only the worker image has been built in an actual Docker container; the other service images and full stack are not verified. D-05 and remaining G1 work are still partial. No acceptance flag has become `YES`.

## Remaining release gates
- **G1 full Docker OTT E2E: OPEN.** Actual Keycloak identity, Redis/RabbitMQ, MinIO/S3 multipart upload, queued FFmpeg transcode, rights review, private stream/segment authorization, revoked-session denial, and restore test. Run the existing `Full developer-stack acceptance (manual)` action only on a disposable GitHub-hosted environment with owner awareness of Action-minute costs.
- **G2 payment/identity: OPEN.** Real Razorpay TEST flows, verified webhooks, renewals, refunds, sessions, privacy erasure.
- **G3 AWS staging: OPEN.** No AWS resources provisioned.
- **G4 security/recovery/content clearance: OPEN.** Load/restore/security/legal checks.
- **G5 territory pilot/worldwide expansion: OPEN.** Legally approved market first, additional rights and compliance before each region.

**Release verdict:** No-go for paid production or public worldwide launch. GitHub CI and migration correctness are progress toward G1, not launch approval.

## Work allocation
ChatGPT maintains the repository and tracks tests; GitHub Actions performs automated build and scoped real infrastructure checks. Codex Cloud should only be used when GitHub-hosted integration checks expose an inaccessible execution blocker. Windows local installation and billable AWS work are last resorts, requiring user approval.
