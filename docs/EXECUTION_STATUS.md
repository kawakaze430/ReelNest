# Current execution and integration status

GitHub `kawakaze430/ReelNest`, branch `main`, remains canonical. This pass starts from Build 28 commit `84a42f6abb3809926cae0ebce550b050f705fb76`. Local evidence records that base SHA and the modified-worktree fingerprint. It is not evidence of a successful remote workflow on the subsequently pushed commit.

## Repairs

- **Catalog:** missing search filters caused PostgreSQL `lower(bytea)` and HTTP 500. Explicit HQL string casts preserve null/mixed filters, case-insensitive matching and pagination. Three real PostgreSQL regressions failed before the repair and passed afterward. CI now runs them on a dedicated disposable database.
- **Containers:** the recommendation image omitted a required semantic-search module. Python images now include their first-party module closure, verify imports and give their nonroot users readable modules. A read-only, network-disabled nonroot recommendation import/health check passed. Optional build-only CA mounts preserve TLS verification; build contexts exclude local secrets/caches.
- **Identity/UI:** synthetic Keycloak users now have required first/last names, preserving PKCE and profile requirements. Profiles are tagged to the current identity/token, private views reset at identity/profile boundaries, and the player mounts its session effects only after identity/profile resolution. Stale requests are cancelled. Lint rules were not disabled.
- **Browser behavior:** real tests verify two accounts' PKCE login, owned profile picker, persisted Arabic RTL/English LTR, and mounted player requests using owned profiles. Unprocessed metadata correctly returns 404. This is **not** decoded-media playback evidence.
- **Storage:** unavailable community MinIO images are replaced with development builds from immutable official Go commits, normal TLS/checksum verification, nonroot runtimes and shared build caches. These new images have **not** completed a build here: required Go downloads redirect to denied `storage.googleapis.com`. Full G1 must verify them. Maintained production storage remains a separate decision.
- **Terraform:** added the reviewed signed-provider lock, standard formatting and an explicit all-object lifecycle filter. Existing source guards tolerate formatter whitespace while retaining their security assertions. No plan/apply or cloud-account call occurred.

## Actual checks

Final commands exited 0 unless stated otherwise.

| Check | Outcome |
| --- | --- |
| `cloud_evidence.py --phase g0` | PASS: Maven, frozen npm install, ESLint, TypeScript, Next.js build and Compose. Lock generation SKIPPED because the committed lock was preserved. |
| Native Maven/JUnit | 28 collected: 24 passed, 0 failures/errors, 4 opt-in PostgreSQL cases skipped. |
| Dedicated PostgreSQL catalog regression | 3 passed, 0 skipped, after reproducing 3 failures before repair. |
| Dedicated V1–V9 → V25 upgrade/idempotency test | 1 passed, 0 skipped, against a separate actual PostgreSQL 17 database. |
| Clean API boot and strict migration verifier | Actual V1–V25 successful and ordered. |
| Python media/ML/release/handoff/deployment suite | 248 passed; one dependency deprecation warning. |
| Offline release gate | 24/24 passed. Its 12 external production sign-offs remain unverified. |
| Actual API smoke | 8/8 passed after catalog repair; protected unauthenticated paths denied. |
| Additional security smoke | Six protected routes returned 401. |
| Real browser component regression | Both accounts passed identity, owned profile UI, locale persistence and denied player startup checks. |
| Cross-account negative probe | Reviewer access to the demo profile's history returned 404. |
| Actual logical PostgreSQL restore | Selected application-table counts/migration history matched; only this run's uniquely created scratch database was removed. |
| npm production dependency audit | 0 vulnerabilities reported; this is not a license audit or security sign-off. |
| Terraform | Formatter, backend-disabled init, readonly-lock init and provider-schema validation passed without the prior lifecycle warning. |
| GitHub workflow static checks | Actionlint passed all three workflows; behavior tests execute the actual change selector/evidence runner. |
| Reusable cloud setup | Verified Maven/JDK activation, dynamic proxy/CA files, pinned Python requirements, frozen npm refresh and generated local Compose configuration passed. |

Reports/logs remain in ignored `build-evidence/`: `cloud-g0-latest.json`, `browser-identity-latest.json`, `auth-browser-latest.json`, `restore-latest.json` and `ownership-pass/`. Generated output, raw logs, credentials, dumps and dependency trees are not committed. Known automatic Next.js type/config edits are removed before committing.

## CI and remaining work

`Build checks` now includes lint, nonzero JUnit checks, image smoke tests and bounded/concurrent jobs. Relevant main changes automatically invoke full developer-stack acceptance after the required component jobs succeed. Documentation-only changes avoid duplicate heavy runs. Manual dispatch remains available when evidence is missing.

Full acceptance requires exact-source G0 proof and an isolated, uniquely named disposable project. Stages check readiness, strict migrations, browser identity, original film/caption upload, worker processing, rights review/publication, private HLS bytes, actual browser frames/time advancement, two-account/session/JWT denial and logical restore. Unique logs preserve original exit codes; credentials/signed URLs are redacted; timeout cleanup stops descendants. Artifact retention is bounded. Cleanup targets only the CI run's own volumes. CI does not rewrite source or automatically deploy/pay.

**Full G1 remains NOT ACCEPTED.** MinIO's required downloads are policy-denied at `storage.googleapis.com`. Actual upload → worker → published private HLS → browser decoding has not passed here. Component proofs do not substitute for that workflow.

**Remote CI observation is BLOCKED:** `gh run list` cannot connect to `api.github.com` through the current proxy. Git fetch/push can work independently. A pushed workflow is configured to execute, not a claimed successful remote run.

The cloud configuration draft preserves existing network presets, adds only `api.github.com` and `storage.googleapis.com`, and saves tested install/start instructions. Review/save those environment settings and publish to activate them; a draft save does not apply networking or publish. Retry the denied operations after activation. GitHub-hosted CI has its own network policy and may independently complete the automatic G1 job.

G2 payment/provider verification, G3 AWS staging/CDN, G4 independent security/load/recovery/legal approval and G5 licensed territorial launch remain open. No financial transaction, deployment, DNS change or production database operation was performed. **Production readiness is not established.**

All 282 original task IDs and historical acceptance flags are preserved; no tracker status was promoted merely from source/component checks. Continue with [the Codex testing prompt](CODEX_NEXT_TESTING_PROMPT.md): existing main only, no new branches/worktrees/PRs, safe reconciliation and normal direct-to-main pushes.
