# Current execution and integration status

## Blocker review: 25 October 2026

The latest pass continued from `c7a81eb2`. A real PostgreSQL regression reproduced a stale media worker changing a replacement worker's completed READY asset back to PROCESSING. The worker now validates and locks its exact live claim before changing media state or starting pipeline work. Nine actual PostgreSQL claim/lease/state tests passed after repair, with fixtures cleaned. A dedicated CI stage now runs these cases.

Native build configuration now uses a valid Swift language mode and typed generated URL-scheme plists. Android TV has the banner and optional-touchscreen declarations required by its lint rules. A new path-scoped workflow builds Android debug/unsigned-release APKs and runs lint; unsigned Apple simulator compilation is a manual opt-in. Nine native configuration/source checks and 32 change-selection checks passed. All five workflows pass actionlint. Native compilation itself remains unobserved; source/configuration checks are not SDK execution. Native-only changes no longer request unrelated full Docker G1.

The cloud draft was saved again with only the two currently required explicit hosts, `cgr.dev` and `api.github.com`, preserving package-manager presets, installation instructions and main-only startup guidance. The obsolete optional Go-fallback storage host was removed. The save returned `requires_publish=true`; it did not apply or publish the rules. **The next external action is review/save/publication in environment settings.** See [exact owner actions](OWNER_ACTIONS_TO_UNBLOCK.md) and [the latest repair evidence](BLOCKER_RESOLUTION_2026_10_25.md). Full G1 and the external release gates remain unaccepted.

## Continuation: 25 October 2026

The next execution pass continued from unchanged remote `main` at `4489fcf6`. It reproduced and repaired two additional backend defects on actual PostgreSQL: usable sessions with stale heartbeats bypassed the two-stream admission cap, and first-user provisioning could attempt an INSERT inside a read-only request transaction. All ten new PostgreSQL/Spring service tests passed after the repairs; the baseline had eight passes and two failures. Native Maven verification passed with 56 tests passed and 15 opt-in database/storage cases skipped. The ten focused PostgreSQL cases were run separately with no skips.

Real two-account browser/API/PostgreSQL/Redis checks also passed for profile/PIN ownership, persisted denied PIN attempts, correct-PIN reset/deletion and atomic one-time device handoff. Test-owned profiles were removed. CI now includes these account checks and the real playback database regressions. Upload acceptance no longer waits for removed UI text; it checks the matching backend upload completion. Playback acceptance now requires private caption delivery/denials, positive independent-account media access and actual encoded traversal denials. Those full media additions remain unexecuted until MinIO is available. There were 98 passing focused Python regressions, plus successful actionlint and shell syntax checks.

See [the continuation report](CONTINUATION_2026_10_25.md) for commands, before/after evidence and precise limitations. Existing frontend/Terraform/media component passes below were reused because their source did not change. No current remote Actions result or full G1 success is claimed. Network settings remain inactive for `cgr.dev` and `api.github.com`; G1–G5 and native device acceptance remain open.

## Previous execution pass

GitHub `kawakaze430/ReelNest`, branch `main`, remains canonical. This pass began on Build 28 commit `84a42f6abb3809926cae0ebce550b050f705fb76`, then safely reconciled newer upstream Build 37 `fcfd396` before pushing. New upstream rights/territory catalog eligibility, resumable uploads, advanced source features and V26/V27 migrations are preserved. Local evidence records that base SHA and the modified-worktree fingerprint. It is not evidence of a successful remote workflow on the subsequently pushed commit.

## Repairs

- **Catalog:** missing search filters caused PostgreSQL `lower(bytea)` and HTTP 500. Explicit SQL text casts preserve null/mixed filters, case-insensitive matching and pagination. Three real PostgreSQL regressions failed before the repair and passed afterward. CI now runs them on a dedicated disposable database.
- **Containers:** the recommendation image omitted a required semantic-search module. Python images now include their first-party module closure, verify imports and give their nonroot users readable modules. A read-only, network-disabled nonroot recommendation import/health check passed. Optional build-only CA mounts preserve TLS verification; build contexts exclude local secrets/caches.
- **Identity/UI:** synthetic Keycloak users now have required first/last names, preserving PKCE and profile requirements. Profiles are tagged to the current identity/token, private views reset at identity/profile boundaries, and the player mounts its session effects only after identity/profile resolution. Stale requests are cancelled. Lint rules were not disabled.
- **Browser behavior:** real tests verify two accounts' PKCE login, owned profile picker, persisted Arabic RTL/English LTR, and mounted player requests using owned profiles. Unprocessed metadata correctly returns 404. This is **not** decoded-media playback evidence.
- **Storage:** the final merge preserves upstream's Chainguard MinIO images and its existing multipart integration workflow. An earlier local official-source fallback could not fetch Go dependencies and was removed in favor of the newer upstream choice. Pulling the current images is policy-denied at `cgr.dev`; full-stack server/client startup remains unverified here. Exact image digests and maintained production storage still need review.
- **Terraform:** added the reviewed signed-provider lock, standard formatting and an explicit all-object lifecycle filter. Existing source guards tolerate formatter whitespace while retaining their security assertions. No plan/apply or cloud-account call occurred.

## Actual checks

Final commands exited 0 unless stated otherwise.

| Check | Outcome |
| --- | --- |
| `cloud_evidence.py --phase g0` | PASS: Maven, frozen npm install, ESLint, TypeScript, Next.js build and Compose. Lock generation SKIPPED because the committed lock was preserved. |
| Native Maven/JUnit after Build 37 merge | 61 collected: 56 passed, 0 failures/errors, 5 opt-in PostgreSQL/MinIO cases skipped. |
| Dedicated PostgreSQL catalog regression | 3 passed, 0 skipped, after reproducing 3 failures before repair. |
| Dedicated V1–V9 → V27 upgrade/idempotency test | 1 passed, 0 skipped, against a separate actual PostgreSQL 17 database. |
| Clean API boot and strict migration verifier | Actual V1–V27 ordered history passed after the merged API applied the two preserved newer migrations. |
| Python media/ML/release/handoff/deployment suite | 300 passed after reconciling Build 37; one dependency deprecation warning. Initial pre-merge suite: 248 passed. |
| Offline release gate | 24/24 passed. Its 12 external production sign-offs remain unverified. |
| Actual API smoke | 8/8 passed after the initial catalog repair; post-merge real public catalog/ID/slug/alias revocation and facet checks also passed. |
| Additional security smoke | Six protected routes returned 401. |
| Real browser component regression | Both accounts passed identity, owned profile UI, locale persistence and denied player startup checks. |
| Cross-account negative probe | Reviewer access to the demo profile's history returned 404. |
| Actual logical PostgreSQL restore | Passed on V27: selected counts/history matched and only the owned scratch DB was removed. New upstream RTO/backup-age proxy instrumentation is preserved; actual production RPO remains unverified. |
| Real V27 optional-preview schema | Default OFF consent, distinct reviewer, non-billable deduplication and deletion cascade checks passed. No advertising transaction occurred. |
| TV client tests | Node test runner passed the public catalog/origin/navigation check. Native Android/iOS compilation and hardware playback were not performed. |
| npm production dependency audit | 0 vulnerabilities reported; this is not a license audit or security sign-off. |
| Terraform | Formatter, backend-disabled init, readonly-lock init and provider-schema validation passed without the prior lifecycle warning. |
| GitHub workflow static checks | Actionlint passed all four workflows; behavior tests execute the actual change selector/evidence runner. |
| Reusable cloud setup | Verified Maven/JDK activation, dynamic proxy/CA files, pinned Python requirements, frozen npm refresh and generated local Compose configuration passed. |

Reports/logs remain in ignored `build-evidence/`: `cloud-g0-latest.json`, `browser-identity-latest.json`, `auth-browser-latest.json`, `restore-latest.json` and `ownership-pass/`. Generated output, raw logs, credentials, dumps and dependency trees are not committed. Known automatic Next.js type/config edits are removed before committing.

## CI and remaining work

`Build checks` now includes lint, nonzero JUnit checks, image smoke tests and bounded/concurrent jobs. Relevant main changes automatically invoke full developer-stack acceptance after the required component jobs succeed. Documentation-only changes avoid duplicate heavy runs. Manual dispatch remains available when evidence is missing.

Full acceptance requires exact-source G0 proof and an isolated, uniquely named disposable project. Stages check readiness, strict migrations, browser identity, original film/caption upload, worker processing, rights review/publication, private HLS bytes, actual browser frames/time advancement, two-account/session/JWT denial and logical restore. Unique logs preserve original exit codes; credentials/signed URLs are redacted; timeout cleanup stops descendants. Artifact retention is bounded. Cleanup targets only the CI run's own volumes. CI does not rewrite source or automatically deploy/pay.

**Full G1 remains NOT ACCEPTED.** The current MinIO image registry is policy-denied at `cgr.dev`. Actual upload → worker → published private HLS → browser decoding has not passed here. Component proofs do not substitute for that workflow.

**Remote CI observation is BLOCKED:** `gh run list` cannot connect to `api.github.com` through the current proxy. Git fetch/push can work independently. A pushed workflow is configured to execute, not a claimed successful remote run.

The cloud configuration draft preserves existing network presets, includes the required `api.github.com` and `cgr.dev` access, and saves tested install/start instructions. The later blocker pass removed the unused optional `storage.googleapis.com` request. Review/save those environment settings and publish; a draft save does not apply networking or publish. Retry the denied operations after the observed runtime changes. GitHub-hosted CI has its own network policy and may independently complete the automatic G1 job.

G2 payment/provider verification, G3 AWS staging/CDN, G4 independent security/load/recovery/legal approval and G5 licensed territorial launch remain open. No financial transaction, deployment, DNS change or production database operation was performed. **Production readiness is not established.**

All 282 original task IDs and historical acceptance flags are preserved; no tracker status was promoted merely from source/component checks. Continue with [the Codex testing prompt](CODEX_NEXT_TESTING_PROMPT.md): existing main only, no new branches/worktrees/PRs, safe reconciliation and normal direct-to-main pushes.
