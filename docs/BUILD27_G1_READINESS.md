# ReelNest Build 27 — Real-stack G1 preparation (2026-10-24)

Canonical source: private GitHub `kawakaze430/ReelNest`, branch `main`.
Baseline: Build 26, commit `dc99dd914c53246c9f10191241b66d56a12c73d9`.

## Engineering goal
Move from source-only confidence to a disposable, reproducible runtime proof that creator application, admin approval, private upload, PostgreSQL V1–V25 migrations, durable FFmpeg worker, rights review, authenticated HLS playback and revoked-access denial work as a single system.

## Concrete blocker repaired
The existing video-worker image previously copied only `pipeline.py` and `worker.py`. These import `caption_packaging.py`, `timed_text.py`, `audio_packaging.py`, and `preview_artwork.py`, which were absent from the container. An image could be built yet its worker process crash at import.

Build 27 now copies all first-party root-level Python modules and requires them to import successfully **during the Docker image build**. In the G1 acceptance script, a second import runs inside the **actual non-root live worker container**.

## Other changes
- Full G1 workflow now checks that the original generated sample MP4 exists **before** building the stack. It never downloads copyrighted material.
- The existing manual full-stack workflow uses a `COMPOSE_PROJECT_NAME` containing GitHub run and attempt IDs; its final cleanup targets only that isolated project and its volumes.
- Added four focused source-contract tests in `tests/release/test_build27_g1_contracts.py`, runnable without Docker.
- Kept G1 opt-in through GitHub Actions **workflow_dispatch**; no automatic full Docker invocation on every push, no AWS resource creation, no payment activity.

## Verified versus open
New test file exercised in the authoring environment: **4 passed**. Bash syntax of updated acceptance script: **passed**. This is not proof of Docker or Flyway execution. The regular GitHub workflow will still run after commit; results must be inspected before promoting new checks.

The complete `G1` test remains `NOT RUN / NOT ACCEPTED` until the manually triggered GitHub workflow completes and captures:
1. Docker images start (especially the worker).
2. Flyway V1–V25 confirmed in PostgreSQL.
3. Keycloak and viewer/creator identities function.
4. Original video upload -> worker HLS -> admin rights -> published media.
5. Authenticated streaming permits eligible requests, denies anonymous/revoked sessions.
6. Disposable logical PostgreSQL backup restore succeeds.

## 282-task status unchanged until real acceptance
The authoritative cumulative per-task list remains [Build 26 tracker](BUILD26_282_TASKS.csv):
- Component CI verified, production acceptance pending: **4**
- Source implemented, runtime unverified: **67**
- Partially implemented: **121**
- Blocked on execution/operator verification: **34**
- Blocked on accounts/external approval: **17**
- Not started/no audited implementation: **39**
- **Total: 282**; production acceptance signed: **0/282**.

Build 27 advances G1 preparation, not production task sign-off. No paid AWS, merchant payments or changes to content rights have been performed.

## Next authorized action
Open GitHub Actions -> **Full developer-stack acceptance (manual)** -> Run workflow on latest `main`, **only on a disposable GitHub-hosted runner** and after confirming Actions minutes/costs. If it fails, inspect step logs/artifacts and fix the first genuine integration defect. Do **not** run this against a shared local database or use `docker compose down -v` outside its unique CI project.

Latest release decision: **NO-GO for AWS staging/pilot/production** until G1–G5 are satisfied.
