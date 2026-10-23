# Build 05 + 06 — verification ledger

## Executed with observed passes

| Check | Outcome |
|---|---|
| Build 01 basic source checks | PASS |
| Build 02 ownership & rights source checks | PASS |
| Build 03 media worker source checks | PASS |
| Build 04 playback/source regression | PASS |
| Build 05/06 signature, entitlement, age/territory, provider config, analytics source checks | PASS |
| Standalone Java 21 HMAC and premium policy compilation/assertions | PASS |
| Python media pipeline tests | 5/5 passed |
| Python hybrid ML recommender tests | 6/6 passed |
| TypeScript syntax parsing via global TypeScript compiler | 36 source files, 0 syntax errors |
| Offline preview inline JavaScript parsing via Node | PASS |
| Compose YAML and environment secret placeholders | PASS |
| Archive entry checksums and integrity | At packaging |

## Not executed or not established

- Full `mvn test` and Spring Boot build (no Maven and dependencies in authoring runtime).
- Next.js `npm install`, typecheck or application build (package registry unavailable).
- Docker Compose execution and live PostgreSQL migration, Keycloak login, provider webhook, live HLS player (Docker unavailable).
- Chromium preview interaction tests for this new preview (browser navigation blocked by administrator/environment policy). The HTML preview is usable in an ordinary browser but not verified interactively here.
- Paid checkout/Razorpay API integration requires user-provided TEST merchant keys and webhook setup.

Passing source checks and standalone policy tests do not establish production readiness.
