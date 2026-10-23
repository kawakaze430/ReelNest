# ReelNest Build 15 — Playback continuity, editorial cues, profile sync and opt-in QoE

**Base:** cumulative Build 14. **Release decision:** NOT PRODUCTION READY. This is source implementation plus offline evidence, **not** an attestation that Java/Next.js/Docker/AWS/Razorpay/browser integrations passed.

## Ten checklist IDs addressed

| ID | Source delivered | Remaining validation / limitation |
|---|---|---|
| **E-09** | Server-side resume policy resets completed films and near-end positions; progress writes are duration-bounded, sequence-guarded and reject an older session overwriting a newer session. Final page teardown attempts an authenticated keepalive write. | Live two-device concurrency, seek, background-tab and resume tests. |
| **E-11** | Optional admin-reviewed intro/recap/credits markers per encoded asset (Flyway V10, admin GET/PUT, admin cue editor, skip buttons only inside valid intervals). | Verify on a real licensed film, browser seek and multi-device playback; no automatic scene detection. |
| **E-18** | Bounded hls.js network/media recovery (max two attempts), online-event recovery, no automatic retry of HTTP 401/402/403/404, manual reauthorization remains available. | Throttled network/browser tests, Safari/native HLS and CDN expiry. |
| **E-20** | Explicit hls.js startup/forward/back-buffer bounds, ABR cap to player dimensions, auto-start level; resume seek delayed until media metadata. | Actual ABR QoE/bandwidth measurements and device tuning. |
| **E-22** | Per-profile settings GET/PUT, ownership checks, server-backed speed/caption/audio/subtitle/data-saver/consent sync with device-local fallback. | Real Keycloak/DB sync, simultaneous-device last-write-wins behavior, offline conflict resolution. |
| **E-23** | Optional QoE event emission only after explicit per-profile opt-in, with server-side consent enforcement. | Live opt-in/opt-out tests and privacy notice review. |
| **I-03** | Startup latency, stall duration, rendition switch, fatal error and recovery events; admin-only 7-day aggregate QoE panel. | Browser events and aggregate SQL results on PostgreSQL; metrics are untrusted client observations. |
| **I-08** | Minimal structured QoE schema, server-derived session/profile ownership, 30-day retention, immediate profile-level QoE purge on consent revocation, profile/session deletion cascade, no raw URLs/IPs/device fingerprints. Small-cohort admin summaries suppressed (<5 sessions). | Legal/privacy review and DB retention job test. |
| **I-09** | Event UUID deduplication, PostgreSQL persistence, per-session serialized ingestion and 200-event cap. **Partial:** durable event replay/export pipeline is not implemented. | Concurrent event/idempotency and DB outage tests; replay tooling remains open. |
| **J-13** | Public-only `robots.txt`, sitemap, metadataBase, social preview metadata and noindex private route layouts. **Partial:** rights-aware canonical metadata/sitemap for individual licensed titles is not implemented. | Production Next.js build and search-engine validation. |

## New API contracts

### Player preferences (authenticated, owned profile)

`GET /api/v1/profiles/{profileId}/player-preferences`

`PUT /api/v1/profiles/{profileId}/player-preferences`

```json
{"speed":1,"captionSize":"normal","captionContrast":"standard","preferredAudio":"en","preferredSubtitles":"hi","dataSaver":false,"qoeConsent":false}
```

Only the signed-in owner can access the settings. `qoeConsent` is **false by default** and must be explicitly enabled. Switching it off deletes previously collected QoE for that profile. The client retains local settings as a fallback when the API is unreachable, but the server wins on reconnect. Concurrent devices currently use last-write-wins; do not call conflict resolution complete.

### Editorial cues (ADMIN only)

`GET /api/v1/admin/titles/{titleId}/media/{assetId}/cues`

`PUT /api/v1/admin/titles/{titleId}/media/{assetId}/cues`

```json
{"introStartSeconds":0,"introEndSeconds":48,"recapStartSeconds":null,"recapEndSeconds":null,"creditsStartSeconds":1580}
```

The asset must be READY and belong to the title. Cue intervals must be valid and within its actual duration. The viewer never trusts client-supplied cue metadata for access control.

### Playback QoE (authenticated, active entitled session, explicit opt-in)

`POST /api/v1/playback/sessions/{id}/qoe`

```json
{"eventId":"00000000-0000-4000-8000-000000000001","eventType":"STARTUP","elapsedMs":1250,"qualityHeight":null,"errorCode":null}
```

Supported events: `STARTUP`, `STALL`, `QUALITY_SWITCH`, `PLAYER_ERROR`, `RECOVERY`. The server derives the profile from the active playback session, rejects unconsented events, enforces a per-session cap, and deduplicates UUIDs. No free-form messages or client-provided user IDs are accepted. Events are pseudonymous, **not anonymous**, because session/profile references exist until deletion/retention.

`GET /api/v1/admin/qoe/summary` returns aggregated seven-day metrics only for at least five distinct opted-in viewer profiles. This is not a public endpoint.

## Offline tests actually run

- `python3 -m pytest apps/video-worker/tests apps/recommendation-lab/tests apps/recommendation-api/tests tests/release tests/handoff -q`
- `node scripts/check_ts_syntax.mjs` — TypeScript **syntax** only.
- `python3 scripts/release_gate.py --mode offline --output docs/BUILD15_OFFLINE_RESULTS.json`
- `python3 scripts/verify_release.py` after refreshing the cumulative SHA-256 manifest.
- Pure Java policy tests compiled and executed with Java 21; pure TypeScript helpers compiled with `tsc` and executed in Node.

The full Spring Boot compile, Next.js semantic typecheck/build, Flyway V10 on PostgreSQL, Docker service startup and real browser/device playback **were not run** here.

## Required local/Codex handoff (do not mark these passed without logs)

1. `cd apps/api && mvn -B verify`; fix real Java compiler/test errors.
2. `cd apps/web && npm install --no-audit --no-fund && npm run typecheck && npm run build`.
3. Start a **disposable** development PostgreSQL with Docker; apply V1–V10 with Flyway and test upgrade from V9. Do not edit V1–V9 in place.
4. Test profile settings ownership (profile A must not read/write profile B), defaults, persistence and simultaneous devices.
5. Test admin cue GET/PUT: unauthorized role denied, wrong title/asset denied, invalid interval denied, valid marker appears in authorized session only.
6. Test QoE with consent disabled (403), enabled (204), duplicate event (204, one row), same UUID in another session (409), >200 events (429), and cross-user session access (404).
7. Test full creator upload→READY→admin approval→viewer HLS with a **rights-owned test clip**, then skip buttons, resume, buffering, network recovery and session expiry.
8. Test admin QoE summary small-cohort suppression, scheduled 30-day retention and profile deletion cascade.
9. Verify sitemap, robots and noindex routes in a real production Next.js build.
10. Do not deploy AWS or run Razorpay charges without separate approval and appropriate TEST credentials.

## Known limitations and rollback

- The Java and TypeScript source changes may still have integration/compiler issues until the above environment-backed tests run. These must be corrected before merge to a release branch.
- A production deployment must migrate PostgreSQL with V10 **before** serving the new API; rollback requires application rollback and retention of V10 schema (do not destructively reverse the migration on production data).
- Consent controls are an engineering safeguard, not a substitute for reviewed privacy disclosures and regional legal requirements.
- Player QoE does not measure the native OS decoder's internal bitrate choices on Safari; client metrics are approximate.
- A title's catalogue appearance, rights or paid entitlement is **never** inferred from skip markers, player settings or QoE events.
