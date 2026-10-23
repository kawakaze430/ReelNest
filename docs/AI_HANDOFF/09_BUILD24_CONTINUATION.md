# Build 24 — For the next AI coding session

**Latest checkpoint:** Build 24 cumulative archive. Read `docs/BUILD24_IMPLEMENTATION.md` and `docs/BUILD24_TEST_EVIDENCE.json`; treat prior Build 23 sections as historical. `docs/BUILD24_282_TASKS.csv` is the authoritative task table; IDs and status counts are unchanged. Do not claim Maven/Docker/AWS approval based on helper checks.

## Added

- Fixed missing PIN-check method that prevented `ProfileController` compiling against its dependency.
- Preserved ownership + PIN verification and failed-attempt protection for adult-profile deletion.
- Added JUnit test source plus portable JDK regression assertions and frontend selection/PIN cleanup.

## Highest-priority continuation

1. **G0 compile now**: install reviewed Maven/npm dependencies, create/commit an audited npm lockfile, run `mvn verify` and Next.js `typecheck/build`. Run `ProfilePinControllerTest` against actual Spring/JUnit dependencies; fix any remaining compile or wiring errors with source regressions.
2. **G1 local identity/permissions**: disposable Postgres V1–V25 migration and upgrade, Keycloak real token, two-account isolation, profile PIN correct/wrong/lockout/403/404, concurrent wrong-attempt lock and commit persistence. Verify PIN semantics for profile switching and every endpoint; **Build 24 does not implement a profile-wide grant**.
3. **G1 video journey**: original/licensed test clip, owner-scoped upload → FFmpeg → approval → HLS → private media; negative tests for invalid media key, revoked rights, country/profile/subscription restrictions.
4. **G2 money**: Razorpay sandbox charges, out-of-order webhooks, verified-entitlement boundary, refund/reconciliation correctness; never execute live payments without permission.
5. **G3–G5 deployment**: Terraform provider validation/plan; private AWS staging, CDN trusted geolocation, security and rights; backups/load test and territory-specific legal release. No paid deployment without owner approval.

## Separation of duties

AI coding agent owns source fixes, tests and entire new cumulative ZIP; Codex/local execution environment owns genuine builds and Docker E2E; human owner owns external test-account approvals, licensing, legal review and any chargeable cloud actions.
