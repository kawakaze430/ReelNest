# Execution continuation: 25 October 2026

Continued directly on GitHub `main`, starting at `4489fcf6e94f89dae05617d2af6629d64868c771`. No new branch, worktree or PR. All 282 historical task IDs, tracker files and applied migrations remain unchanged. This is focused verification and repair, not production acceptance.

## Demonstrated backend defects and repairs

1. **Stream admission:** `PlaybackService.start()` ignored heartbeats older than two minutes when counting sessions, while media authorization accepted those sessions until their ten-minute expiration. Two still-usable sessions could therefore admit additional streams. Admission now counts all ACTIVE, unexpired leases. Tests prove the stale-heartbeat case, three simultaneous admissions (two succeed, one conflicts), and replacement of actually expired leases.
2. **First request:** a new authenticated subject reaching a read-only playback method caused PostgreSQL to reject the local identity INSERT, returning a server error. Only missing identities reached through a read-only caller now use a short independent write transaction. Existing users and normal write callers keep their transaction behavior. Tombstone, suspension and application JWT revocation checks remain enforced. The bootstrap can commit the local identity even if the outer operation subsequently returns 404; it needs a second pooled connection only for that first-read case.

`PlaybackPostgresSecurityTest` uses actual PostgreSQL 17, repository beans and Spring transaction proxies. Its synthetic metadata permits service authorization tests without storing or decoding a film. It does not validate JWT signatures, S3 or browser playback. No payment-provider mock calls are allowed.

## Actual commands and outcomes

Java commands used the prepared JDK 21/Maven environment and its verified proxy trust/settings. PostgreSQL tests require `REELNEST_DISPOSABLE_DB=true` and dedicated loopback URL `REELNEST_SECURITY_DB_URL=jdbc:postgresql://127.0.0.1:5432/reelnest_security_test`.

| Command/check | Actual outcome | Exit |
| --- | --- | --- |
| `mvn -f apps/api/pom.xml -B -ntp -Dtest=PlaybackPostgresSecurityTest test` before repairs | 8 passed, 2 failed, 0 errors/skips; reproduced both defects | 1 |
| Same focused PostgreSQL command after repairs | 10 passed, 0 failures/errors/skips | 0 |
| `mvn -B -ntp -DfailIfNoTests=true verify` in `apps/api` | 71 collected: 56 passed, 15 opt-in cases skipped, 0 failures/errors; packaged real Spring Boot jar | 0 |
| `python scripts/e2e_account_security_live.py` on confirmed disposable local services with system Chromium | All ten account checks passed; owned fixtures cleaned | 0 |
| `pytest tests/handoff/test_processing_browser_contract.py -q` | 7 passed | 0 |
| `pytest tests/handoff/test_playback_browser_contract.py -q` | 26 passed | 0 |
| `pytest tests/handoff/test_account_security_safety.py -q` | 5 passed | 0 |
| `pytest tests/handoff/test_ci_change_selection.py tests/handoff/test_live_stack_safety.py tests/release/test_build27_g1_contracts.py -q` | 48 passed | 0 |
| `build-evidence/actionlint/actionlint -shellcheck= -pyflakes=` | All four workflows parsed/validated; optional shellcheck/pyflakes unavailable | 0 |
| `bash -n scripts/validate_local_stack.sh`; `git diff --check` | Passed | 0 each |
| Existing frontend G0, Terraform, media/ML and V1–V27 migration evidence | Reused for unchanged sources; not rerun or claimed as new exact-SHA CI | n/a |

The account check uses real PKCE login for the separate demo and reviewer identities and actual API requests. It proves wrong-account PIN/profile actions fail, child adult-PIN setup is rejected, rejected PIN requests commit their attempt counters, correct verification resets them, and protected deletion persists. Actual Redis redemption proves wrong-account requests do not consume a code, concurrent redemption succeeds exactly once, replay/malformed codes fail, and deleted profiles cannot be selected. Invitations/selections are no-store and return no authentication credentials. SQL access in that check is read-only; profiles are created/deleted only through their authenticated API.

Initial account attempts stopped before authentication because the native Next.js standalone server lacked current copied static assets and had cached its startup file list. Copying the documented standalone static/public assets and restarting only the task's server restored the JavaScript responses. No application change or relaxed authentication was used for that correction.

## Integration acceptance repairs

- Creator upload acceptance previously waited for `Source uploaded.`, a removed UI message, and would time out after a successful upload. It now observes the shipped browser form's matching completion response and checks HTTP status, COMPLETED state, title, filename and original byte count before polling worker readiness.
- Playback acceptance parses subtitle URIs from `EXT-X-MEDIA`, verifies the uploaded original cue in timed private WebVTT, and checks anonymous/cross-account caption denial.
- The independent second account must also start the FREE fixture using its own profile, fetch real media bytes and end that session. Merely denying foreign-account access is insufficient.
- Four fixed traversal probes stay beneath the validated local session URL, disable redirects, require an explicit rejection and record actual statuses. Positive media URLs still use the confined resolver.
- PostgreSQL CI gets a dedicated security-test database. Full acceptance CI executes the new account check and retains its sanitized report. Failed upload/playback scripts mark their reports FAIL while preserving their command's failure.

The focused Python tests exercise controlled responses and guard behavior. They are not evidence that these added media scenarios passed against actual storage. Full upload/processing/caption/media/browser acceptance remains blocked.

## Evidence and remaining prerequisites

Ignored evidence: `build-evidence/continuation/playback-postgres-security-summary.json`, `playback-postgres-before.log`, `playback-postgres-after.log`, `maven-verify.log`, `integration-regressions.log`, and `build-evidence/account-security-latest.json`. Local reports record their tested worktree/base commit; they are not remote exact-commit Actions attestations. Raw logs, generated artifacts, dependency trees and secrets are not committed.

The managed environment still has no active destination grants for `cgr.dev` (current MinIO server/client registry) or `api.github.com` (Actions observation). The already-saved environment draft contains those additions and tested setup/start instructions; saving a draft does not apply or publish it. No unchanged denied network operation was repeatedly retried. Git HTTPS remains functional independently.

Native Android/iOS/tvOS builds are NOT RUN: no Android SDK, Gradle/adb, Swift or Xcode is available. The remaining twelve ledger items require DRM/vendor contracts, native devices/SDKs, commercial decisions or authorized infrastructure. No payment transaction, cloud deployment, production database, DNS change or Terraform plan/apply occurred. G1–G5 remain unaccepted; no tracker flags were promoted from these narrow tests.

Next: activate the reviewed network settings, inspect current-SHA Actions before dispatching anything, and follow [the main-only testing prompt](CODEX_NEXT_TESTING_PROMPT.md). Actual original upload → worker → licensed publication → private HLS/captions → browser decoding is still the first full integration gate.
