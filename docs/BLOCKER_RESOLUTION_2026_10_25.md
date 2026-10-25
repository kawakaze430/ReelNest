# Blocker review and additional repairs: 25 October 2026

Started on unchanged remote `main`, `c7a81eb27c20fa960635d1b24550ebc188999a51`. Continued directly on `main`, without branches, worktrees or PRs. No historical task IDs, tracker CSVs or migrations were edited.

## Additional worker defect reproduced and repaired

`worker.handle()` previously set its media asset to PROCESSING without checking whether its claim still owned the job. A delayed old worker could resume after a replacement had already completed the asset and overwrite READY. The later progress/failure fences correctly refused the old worker, leaving the successful asset incorrectly stuck in PROCESSING and hidden from legal playback/catalog eligibility.

The initial transition now locks the job and checks the job ID, asset ID, attempt, worker identity, RUNNING state and live lease before changing the asset or invoking the pipeline. The job-before-asset lock order matches success/failure transitions. A positive case proves the fence still allows a valid worker to proceed.

Tests use actual PostgreSQL 17 in a newly owned loopback `reelnest_worker_test` database. Schema/constraints are copied without data from the already migrated V27 test schema. This is not an independent migration-history assertion. Handle tests stub the encoding/storage callback; READY is synthetic metadata, not an encoded film.

| Command/check | Actual result | Exit |
| --- | --- | --- |
| Original `pytest apps/video-worker/tests/test_worker_postgres.py -q` before repair | 7 passed, 1 failed; stale handle regressed READY | 1 |
| Same original eight cases after repair | 8 passed, no skips/failures | 0 |
| Added `test_live_handle_starts_pipeline_and_can_publish_after_fenced_transition` | 1 passed | 0 |
| Native configuration and existing mobile source checks | 9 passed | 0 |
| `pytest tests/handoff/test_ci_change_selection.py -q` | 32 passed | 0 |
| `actionlint -shellcheck= -pyflakes=` | All five workflows passed; optional external shellcheck/pyflakes not available | 0 |
| Prior backend/frontend/Terraform/full Python component results | Reused for unchanged source, not claimed as new exact-SHA remote CI | n/a |

The nine PostgreSQL worker cases cover concurrent SKIP LOCKED claims while transactions hold their row locks, replacement fencing, monotonic progress, expired final attempts, retry delay/exhaustion, permanent rejection and atomic state transitions. Cleanup verified zero synthetic users, titles and jobs. No S3 or FFmpeg operation occurred in these tests. Existing media encoder tests remain separate.

The PostgreSQL CI workflow now provisions a separate worker-test database, copies schema only using the service's matching PostgreSQL tools, installs pinned worker dependencies, and retains JUnit XML. Ordinary worker test runs skip these opt-in cases unless the strict disposable/loopback guard is satisfied.

## Native compiler inputs and hosted verification

- XcodeGen targets used `SWIFT_VERSION=5.9`, a compiler release rather than a supported language-mode value. They now use 5.0. Generated Info.plists contain actual typed URL-scheme arrays rather than stringified JSON build settings; iOS/tvOS use separate generated paths.
- Android TV lint rules `MissingTvBanner` and `ImpliedTouchscreenHardware` are errors in the authoritative AGP 8.7.3 lint-checks 31.7.3 source. Added an original landscape vector banner and `android.hardware.touchscreen` required=false while preserving the TV launcher. No lint suppression was introduced.
- `Native catalog client compilation` automatically runs Android debug, unsigned release and lint on relevant main changes. JDK 17, Gradle 8.9, platform 35 and build tools 34.0.0 are explicit. Debug signing is an ephemeral development signature, not production signing. The placeholder API origin disables network use by these preview clients.
- Apple simulator compilation is workflow_dispatch-only with `apple_simulators=true`, default false. It uses checksum-pinned XcodeGen 2.44.1, records installed Xcode/SDK versions, builds both unsigned simulators and checks actual built plist values. No certificates, device install or store upload are used. The macOS job consumes extra private CI minutes and was not dispatched here.
- Native-only and TV-shell changes no longer select full backend Docker G1; runtime service/integration changes still do. Behavior tests execute the actual selector.

**Native builds are configured, not verified here.** No Android SDK, Gradle, Xcode or Swift compiler is installed in this managed Linux environment, and API policy still prevents observing hosted job results. Device interaction, OIDC, protected media, DRM and store acceptance remain open.

## Current external blockers and saved configuration

Observed runtime remains specification revision 1 with no configured explicit destination grants. The draft was confirmed saved with the required `api.github.com` and `cgr.dev` rules and updated startup guidance. Existing package-manager presets and installation script remain; the obsolete optional `storage.googleapis.com` fallback rule was removed. The result returned `requires_publish=true`.

An agent draft save does not apply or publish configuration. The user must review/save the prepared settings and publish through the product. Recheck observed runtime access after that meaningful change before retrying MinIO or Actions. No unchanged denied network request was looped or bypassed.

See [owner actions](OWNER_ACTIONS_TO_UNBLOCK.md) for the immediate network step and the named payment, AWS, DRM, device and independent acceptance prerequisites. Actual creator upload → processing → licensed private HLS/captions → decoded browser playback remains BLOCKED. Financial transactions, paid deployments, production databases and Terraform plan/apply remain outside current scope. No completion flags were promoted from narrow tests.

Ignored evidence: `build-evidence/blocker-pass/worker-postgres-summary.json`, before/after/live-handle logs, `ci-change-selection.log` and native configuration diagnostics. Reports identify the tested dirty worktree/base and source hashes; they are not exact-SHA remote workflow attestations. Generated native projects/APKs, dumps, credentials and raw logs are not committed.
