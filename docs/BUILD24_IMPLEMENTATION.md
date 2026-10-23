# ReelNest Build 24 — Targeted security/runtime source repair (2026-10-23)

## Release decision: NO-GO (source changes tested offline; runtime still unverified)

Build 24 is a complete cumulative source release built directly on the uploaded Build 23 handoff. All previous modules, 25 Flyway migrations, and 282 canonical task IDs are preserved. No paid AWS actions, Razorpay payments, real accounts, or production content were touched.

## Reproduced source defect and repair

`ProfileController.remove(...)` invoked `ProfilePinController.requireForSensitiveAction(userId, profileId, suppliedPin)`, but that method was absent in the uploaded Java class. A normal Maven compilation would fail resolving the symbol. Instead of removing the authorization call, Build 24 adds the missing guard:

- Select the **user-owned** profile with the existing `SELECT ... FOR UPDATE` lookup.
- For profiles with a configured PIN, require a matching PIN; refuse missing, incorrect and temporarily locked PINs with HTTP 403.
- Increment failed-attempt and lockout counters on incorrect PINs. The caller already uses `@Transactional(noRollbackFor=ResponseStatusException.class)` so failed attempts can persist despite HTTP 403. This must still be proven in real PostgreSQL.
- Continue to allow an authenticated account owner to delete an **unprotected** profile. This does **not** create a reusable verified-profile grant or lock every playback endpoint.

The profile manager also now displays error messages for failed deletion attempts, clears submitted PIN state, and selects a remaining profile (or removes the selection) when deleting the active profile. These are source changes, not browser E2E evidence.

## Files added or modified

- `apps/api/src/main/java/com/reelnest/profiles/ProfilePinController.java` — implement previously missing ownership- and PIN-gated sensitive-action method.
- `apps/api/src/test/java/com/reelnest/profiles/ProfilePinControllerTest.java` — five JUnit/Mockito test cases for correct, incorrect, missing, locked PIN and wrong profile ownership; **written but not executed without Maven dependencies**.
- `apps/web/src/components/profile-manager.tsx` — error handling, PIN-state clearing, deleted-profile selection cleanup.
- `tests/release/test_build24_profile_pin.py` — portable source contracts plus executed JDK cryptographic policy test, and task-register preservation checks.
- `docs/BUILD24_282_TASKS.csv` — current authoritative 282-task copy including unchanged classifications and two marked areas of progress.
- `docs/BUILD24_TEST_EVIDENCE.json` — exact outcomes and missing execution capabilities.
- `docs/AI_HANDOFF/09_BUILD24_CONTINUATION.md` — next development and Codex handoff notes.
- `docs/BUILD24_SHA256SUMS.txt` — checksum inventory for the new cumulative package.
- `AI_START_HERE.md`, `AGENTS.md`, `README.md`, `AI_HANDOFF_CONTEXT.json`, `docs/BUILD_VERSION` — current checkpoint pointers.

## Checklist: cautious classification, no unsupported promotions

| Classification | Before | After |
|---|---:|---:|
| Source implemented — runtime unverified | 67 | 67 |
| Partially implemented | 119 | 119 |
| Blocked — external execution / operator verification | 40 | 40 |
| Blocked — external approval / account | 17 | 17 |
| Not started / no audited implementation evidence | 39 | 39 |
| **Total** | **282** | **282** |

Source progress addressed **A-13** (known code defect fixed, backend still uncompiled) and **B-10** (protected profile deletion path and UI repaired, full selection/playback PIN grants and parental controls still partial). These remain *Partially implemented* until more evidence; no task has complete production acceptance.

## Actual evidence and limitations

See `docs/BUILD24_TEST_EVIDENCE.json`. JDK helper, source contracts, selected Python tests, static checks and TS syntax parsing executed successfully. **Not executed**: Maven `verify` or new JUnit tests; frontend `npm ci`, semantic typecheck or production build; Docker Compose; PostgreSQL migration execution; Keycloak, media E2E, real Razorpay TEST, Terraform providers or AWS.

The initial larger FFmpeg/ML combined test command exceeded its execution timeout. Selected tests passed on a separate invocation, but we make no claim that the original combined suite passed in this session.

## Immediate Codex acceptance commands (disposable development environment)

```bash
cd apps/api && mvn -B -ntp verify
cd ../web && npm ci --no-audit --no-fund && npm run typecheck && npm run build
cd ../.. && docker compose config -q
# Review dev secrets and storage volumes before starting; do not target production.
docker compose up --build -d
bash scripts/validate_local_stack.sh --smoke
```

`npm ci` will not work until a reviewed `apps/web/package-lock.json` is generated/committed in a connected environment. For this reason **A-04 and A-07 remain blocked**. Validate fresh and upgraded V1–V25 PostgreSQL migrations; use disposable DBs only. Follow full G0–G5 release gates before considering a live launch.
