# ReelNest: continue testing and fixing GitHub main

Use this prompt for the next Codex execution session. GitHub `kawakaze430/ReelNest`, branch `main`, is the canonical source. Read `AGENTS.md`, `docs/EXECUTION_STATUS.md`, `docs/OWNER_ACTIONS_TO_UNBLOCK.md`, `docs/BLOCKER_RESOLUTION_2026_10_25.md`, `docs/CONTINUATION_2026_10_25.md`, the latest numbered status/task ledger, `docs/REELNEST_MASTER_282_CHECKLIST.md`, `docs/REELNEST_TEST_EVIDENCE_AND_GATES.md`, and the current workflows before acting.

Take ownership of the requested execution checks and their concrete failures. Finish independent work before reporting an external blocker. Do not infer production readiness from a successful build.

## Git workflow: main only

- **Do not create branches, worktrees, or pull requests. Work directly on `main` and push completed fixes directly to `origin/main`.** The cloud task already has an isolated checkout.
- Inspect the working tree first and preserve existing changes. Fetch `origin/main`, switch to the existing canonical `main`, and fast-forward where possible. Read the actual latest SHA; do not work from an older ZIP or this document's historical results.
- Before pushing, fetch again. If another writer advanced `main`, reconcile without discarding either writer's changes, rerun affected checks, then push normally. Never force-push, reset shared history, or bypass branch protections. Report a rejected push precisely.
- Commit reviewed source, tests, dependency locks and sanitized written findings only. Never commit credentials, `.env` files, database dumps, dependency trees, raw logs, generated build output or Terraform state. Preserve all 282 original task IDs and applied Flyway migrations.

## Inspect existing evidence first

1. Fetch `main` and record its full SHA.
2. Inspect GitHub Actions for that exact SHA, including individual job conclusions and artifacts. Use the existing GitHub authentication; do not request or expose token values merely because the CLI fails.
3. `Build checks` now runs full disposable integration automatically after successful component jobs when relevant application/integration files change. The standalone PostgreSQL workflow proves clean migrations, an existing V9 upgrade, and real catalog queries. Neither workflow proves payments, AWS or legal approval.
4. Reuse passing same-source results. Run a failed or missing check; avoid duplicate builds and duplicate manual integration runs. If an exact-SHA run is already active, inspect it rather than dispatching another.
5. API access now works in runtime revision 2. If `gh` reports an invalid inherited token, test a normal HTTPS API request using platform-injected authentication before requesting credentials. Detailed job logs and artifacts use additional Actions/Azure destinations; MinIO blobs use its observed R2 host. Read `OWNER_ACTIONS_TO_UNBLOCK.md` and check current policy. Capture download errors privately and sanitize signed URL parameters before any output. Do not treat inaccessible logs as a source diagnosis or bypass the network policy.
6. Failed evidence phases now publish sanitized GitHub check annotations, including matching local-stack stage metadata and scoped container diagnostics. Inspect those annotations through the API as well as downloading artifacts when permitted. An annotation identifies a diagnosis only when its actual content establishes the cause.

## G0: compilation, dependencies and regression checks

When not already established for the current source, run:

```bash
python3 scripts/cloud_evidence.py --phase g0
```

Use Java **JDK** 21, Maven, the committed npm lockfile, and the repository's Python requirements. A Java runtime alone is insufficient. Keep package integrity, Go checksum-database verification and TLS verification enabled. For managed-cloud builds, the Dockerfiles accept optional build-only CA secrets; never copy session trust material or credentials into image layers.

Confirm actual JUnit test counts, TypeScript typecheck, Next.js production build and Compose validation. Run relevant worker/recommendation/evidence-runner regressions and the offline release gate for changes affecting them. Run backend-disabled Terraform validation if it is not already verified. Distinguish skipped opt-in database tests from passing tests.

Fix demonstrated causes with focused regression coverage. Do not weaken assertions, ownership, media rights, entitlement, age, territory or signature checks to obtain a green result.

## G1: the next required full-stack acceptance

Prefer the automatic GitHub-hosted acceptance job and inspect its failing stage. If a new manual run is actually needed, run `Build checks` on `main`, or use `Full developer-stack acceptance` for its G0 → G1 fallback. Do not start duplicate jobs.

For a local run, first verify that Docker is the intended isolated local daemon and use a new disposable Compose project:

```bash
export REELNEST_DISPOSABLE_STACK=true
export COMPOSE_PROJECT_NAME="reelnest-test-$(date -u +%Y%m%d%H%M%S)"
python3 scripts/cloud_evidence.py --phase g1 --disposable-confirmed \
  --g0-evidence build-evidence/cloud-g0-latest.json
bash scripts/test_restore_disposable.sh
```

The runner requires successful G0 evidence for the exact source, not a historical pass. Use an existing system Chromium only through the supported `REELNEST_CHROMIUM_EXECUTABLE` override; otherwise install Playwright's browser normally. Set runtime proxy exclusions for local service DNS names in managed environments while preserving the configured proxy for external traffic.

Verify all of the following with actual responses and browser behavior:

- Every required image builds, each service starts, and Keycloak discovery, API, web and recommendation readiness succeed.
- PostgreSQL has exactly V1–V27 in successful order. Verify clean migration and V9 → V27 upgrade/idempotency separately against disposable databases.
- Real PKCE browser login works for two independent test accounts. Authenticated profiles work; anonymous requests, tampered JWTs and cross-account profile access are denied.
- `e2e_account_security_live.py` proves profile/PIN ownership, persisted denial counters and correct-PIN reset/deletion against PostgreSQL, plus wrong-account/replay/deleted-profile denial and concurrent exactly-once Redis handoff. It must clean only its own test profiles. PIN protection here covers sensitive profile actions, not a universal playback challenge.
- A uniquely named repository-owned synthetic film completes creator approval, rights declaration, private upload, caption upload, worker encoding, administrator rights review and publication. No copyrighted commercial asset or production data is used.
- The newly processed title serves real private HLS playlists, video segments and the uploaded original caption cue in WebVTT. Anonymous/cross-account caption access and encoded traversal probes are denied. The second account independently gets legal FREE media using its own profile. The shipped browser player decodes frames and advances playback time; downloaded bytes or an open port alone are insufficient.
- Anonymous and cross-account media access, revoked sessions, stale progress writes and old application JWTs after global logout are denied. Resume/watchlist behavior and independent-account access still work.
- Private source/processed buckets remain private. Caption, child-age, territory, entitlement, rights-expiry and path-traversal cases need explicit positive/negative evidence before broader security acceptance.
- A logical backup restores into a uniquely owned scratch database and its selected row counts/migration history match. Cleanup must never drop a database that this run did not create.

Development Compose retains the latest upstream Chainguard MinIO server/client images. Registry manifests now work at `cgr.dev`; actual image blobs redirect to the additional R2 host in the owner action document. A policy denial is an external prerequisite, not permission to bypass the proxy or disable verification. Verify exact image digests and both server/client startup; development floating tags do not establish production storage approval.

The standalone PostgreSQL workflow also runs `PlaybackPostgresSecurityTest` against its dedicated loopback `reelnest_security_test` database. Preserve the fixes for first-user read-only identity provisioning and the two-stream cap. Real concurrent admissions, stale heartbeat versus expiry, ownership, rights/territory/age, unpaid premium, progress ordering, erasure/suspension and JWT-revocation cutoffs must continue to pass. Synthetic service metadata and a constructed JWT do not establish signature or media delivery acceptance.

The same workflow separately runs `test_worker_postgres.py` with `REELNEST_DISPOSABLE_DB=true` and loopback `WORKER_TEST_DB_DSN` naming only `reelnest_worker_test`. Preserve the initial handle lease fence: a stale worker must not change newer READY media or invoke the pipeline. Concurrent claims, progress/failure/success ownership and retry behavior require actual PostgreSQL checks. Stubbing the pipeline in a lease test does not clear actual encoding/storage acceptance.

## Additional client checks

Inspect the path-scoped `Native catalog client compilation` job for the exact SHA. Android debug/unsigned-release compilation and real lint run automatically. Apple unsigned simulator compilation requires explicit manual `apple_simulators=true`; it consumes additional macOS CI minutes and should not be started automatically. Both Apple simulator builds/plist checks already passed at `d4ae145`, Android at `c55dea46`; reuse these results while their component source is unchanged. Read `apps/mobile/README.md` for exact tool versions and commands. Native source guards do not prove SDK compilation, signed installation, DRM or device playback. Reuse unchanged passing TV Node/packaging results and preserve historical acceptance flags.

## Remaining release gates

G2 real payment/provider flows, G3 AWS staging/CDN, G4 independent security/load/recovery/legal review and G5 territory-specific launch require their own evidence and authorizations. Do not run financial transactions, provision billable resources, change DNS, use production databases or declare a public release under this prompt. Offline payment-policy tests and Terraform schema validation are permitted; `terraform plan`/`apply` are not authorized here.

## Required handoff

Report the final remote `main` SHA, changed files, commits actually pushed, exact commands/exit codes/test counts, same-SHA CI links, and PASS/FAIL/BLOCKED/SKIPPED/NOT RUN outcomes. Preserve underlying failures and sanitized artifacts in ignored `build-evidence/`; write a concise reviewed status to GitHub when it adds useful evidence. Distinguish component compilation, database verification, full Docker/browser integration, financial verification, staging and production acceptance. State the precise next missing check, then stop.

**Reminder: no new branch or PR. Commit and push approved fixes directly to `origin/main`, without force-pushing.**
