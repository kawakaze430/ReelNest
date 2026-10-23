# ReelNest Build 19 — A/B task-by-task delivery and blockers

**Scope:** the 16 A-series tasks and 15 B-series tasks requested in the October 22 development turn. Every status is **source / evidence**, not production certification.

Legend: **Implemented** = code or repeatable artifact added, not necessarily deployed; **Partial** = portion addressed; **Blocked** = missing runtime/account/external evidence.

## A — repository and runtime

| ID | Status | Evidence / work left |
|---|---|---|
| A-01 | Partial | Build 18 uploaded archive SHA-256 and baseline Git commit recorded in `BASELINE_PROVENANCE.json` / bundle; operator must push to private repo |
| A-02 | Implemented | `DEPENDENCY_AND_VERSION_MATRIX.md` inventories runtime and dependencies; real installed versions unverified |
| A-03 | Partial | Next.js patched minimum 16.4.1; GitHub Dependabot and CI npm audit wired; live npm/Maven advisory scans not executed |
| A-04 | Blocked | No npm registry available to generate/review reliable lockfile; `scripts/generate_reviewed_lockfile.sh` provides command; image temporarily bootstraps when missing |
| A-05 | Blocked | No `mvn` installed, no Maven Central reachability; `mvn verify` remains external |
| A-06 | Partial | Existing offline unit tests plus new JWT policy tests run; Maven unit/integration pending |
| A-07 | Blocked | No frontend dependencies installed; TS parse only, typecheck and `next build` pending |
| A-08 | Blocked | Docker daemon/executable unavailable; Dockerfiles retained and reviewed statically |
| A-09 | Partial | `compose.yaml` parsed with PyYAML and contract checker; real `docker compose config` pending |
| A-10 | Blocked | No Docker; run clean-machine validation |
| A-11 | Blocked | Flyway V1–V15 migrations present; PostgreSQL clean + upgrade execution pending |
| A-12 | Implemented | Explicit JWT audience/client env contract, `check_env_contracts.py`, compose and realm checks |
| A-13 | Partial | Fixed a nullable JDBC cutoff read, added revocation tests; real runtime failures still unknown |
| A-14 | Partial | CODEOWNERS, PR template, initial Git provenance; private GitHub repo and branch protection require owner |
| A-15 | Implemented | Root `.env.example`, `.gitignore`, secrets policy, no real credentials |
| A-16 | Implemented | `CLEAN_MACHINE_SETUP.md` with prerequisites, install/build/test/debug |

## B — identity, security, accounts

| ID | Status | Evidence / work left |
|---|---|---|
| B-01 | Blocked | Browser Keycloak registration, confirmation, reset and SMTP test require running identity environment |
| B-02 | Partial | Audience `reelnest-api`, `azp` web client validator, issuer/JWKS and PKCE; live Keycloak token verification pending |
| B-03 | Partial | Production/staging generator strips demo accounts; live realm import must be verified |
| B-04 | Blocked | Persisted Keycloak DB exists in Compose for dev, prod SMTP and TLS require real environment |
| B-05 | Partial | Staging generator enforces OTP enrollment in realm configuration; admin MFA live test pending |
| B-06 | Partial | Keycloak realm roles include VIEWER/CREATOR/MODERATOR/ADMIN; admin API gates existed; all role paths not fully tested |
| B-07 | Partial | Scoped playback and profile checks already exist; multi-account negative end-to-end tests pending |
| B-08 | Partial | New list/revoke API playback sessions + `token_valid_after` invalidates API JWTs; Keycloak SSO session inventory/revocation pending |
| B-09 | Partial | Added admission limits for profile creation and global revoke; Keycloak brute-force config, live lockout test pending |
| B-10 | Blocked | Profile PIN actions not implemented: do not claim UI-only protection as a security barrier |
| B-11 | Partial | Child-profile checks already restrict U/U-A7 media; finer child ages and PIN parental approvals not implemented |
| B-12 | Partial | Existing accessible sign-in interface; screen reader and recovery testing pending |
| B-13 | Partial | OIDC logout UI exists and DB cutoff added; must terminate IdP SSO and verify refresh-token invalidation |
| B-14 | Blocked | Keycloak restart/outage/recovery require live environment |
| B-15 | Partial | `IDENTITY_OPERATIONS.md` documents break-glass and audit controls; manual drill pending |

## New source changes

- `ApiJwtDecoderConfig.java`, `TokenClaimsPolicy.java`: issuer, JWKS, access-token audience/authorized-party defense.
- `AccountSessionsController.java`, the `/account/sessions` page, Flyway V15: account playback session inventory, revoke-by-ID, revoke all application JWTs.
- Staging realm generator: no demo users, email verification and OTP enrollment configuration (live import not tested).
- Next.js minimum 16.4.1 after October 2026 security advisories (lockfile still must be generated online).
- Scoped admission throttling, CI checks, Dependabot, CODEOWNERS, environment contract tests.

## Critical missing release evidence

No Maven/Next.js compilation, Docker, PostgreSQL migrations, Keycloak issuer/JWKS, sandbox billing or AWS staging was executed in the offline environment. Do not put this build on a public paid domain until the missing P0 gates are independently passed.
