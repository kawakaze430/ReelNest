# Build 04 — Protected Playback Technical Notes

## Required runtime

- Docker Compose with Keycloak, PostgreSQL (Flyway V1-V5), Spring Boot, MinIO source/processed buckets and Python video-worker.
- Public viewer web at `http://localhost:3000` and local API `http://localhost:8080`.
- In the **local development-only** Compose file, `PLAYBACK_DEV_PROXY_ENABLED: "true"`, `PLAYBACK_TRUSTED_COUNTRY: IN`, `S3_PROCESSED_BUCKET: reelnest-processed-private`.
- A viewer account has a profile. A creator has been approved, film submitted, HLS transcoded to READY, rights approved including IN, title PUBLISHED. Fictional seeded movies have **no** media/rights and cannot play.

## Authentication and media flow

1. Browser signs in via Keycloak and selects own viewer profile (profile ID cached in namespaced browser storage; trusted only after API verifies account ownership).
2. Title detail calls `GET /api/v1/playback/availability?profileId=&titleId=` — informational, not a capability token.
3. Browser calls `POST /api/v1/playback/sessions` `{ "profileId": "UUID", "titleId": "UUID" }` with bearer token.
4. Spring locks the user row for admission; requires active account, profile ownership, rights APPROVED + valid UTC dates and territory IN, creator APPROVED, title PUBLISHED, media READY, and permitted age. FREE tier only until billing. Two recent active sessions max. Inserts expiring DB session.
5. Response has session UUID, root-relative manifest path `/api/v1/playback/sessions/{sessionId}/files/master.m3u8`, resume position and heartbeat interval.
6. hls.js sends the bearer token on every playlist and segment request. Java validates session owner, expiry, current profile and media, approved rights, territory, age, and access tier, then streams an allowed `.m3u8` or `.ts` object from a **private** MinIO bucket. Relative HLS paths resolve under the same session URL.
7. Browser periodically sends `PUT /sessions/{id}/progress` with an increasing sequence and position. Server rejects stale/replayed sequence and out-of-range video positions; progress persists per profile and media asset.
8. Every 30 seconds `POST /sessions/{id}/heartbeat` extends expiry after re-authorization; teardown calls `DELETE /sessions/{id}`. Expired sessions cease to work if tab is abandoned.

## REST endpoints

| Method | Path | Response |
|---|---|---|
| GET | `/api/v1/playback/availability?profileId=&titleId=` | `{playable,code,message}` |
| POST | `/api/v1/playback/sessions` | session + manifest URL + resume duration |
| GET | `/api/v1/playback/sessions/{id}/files/master.m3u8` | authenticated HLS manifest |
| GET | `/api/v1/playback/sessions/{id}/files/360p/index.m3u8` | authenticated variant |
| GET | `/api/v1/playback/sessions/{id}/files/360p/seg_00000.ts` | authenticated video segment |
| POST | `/api/v1/playback/sessions/{id}/heartbeat` | refreshed expiry |
| PUT | `/api/v1/playback/sessions/{id}/progress` | saved position/completion |
| DELETE | `/api/v1/playback/sessions/{id}` | 204, session ended |
| GET | `/api/v1/playback/continue-watching?profileId=` | available noncompleted progress |
| DELETE | `/api/v1/playback/continue-watching/{titleId}?profileId=` | 204, remove progress |
| GET | `/api/v1/watchlist?profileId=` | title list scoped by viewer profile |
| PUT | `/api/v1/watchlist/{titleId}?profileId=` | 204, save a title |
| DELETE | `/api/v1/watchlist/{titleId}?profileId=` | 204, remove a title |

## Security boundaries

- The `source` and `processed` MinIO buckets have no anonymous read grants. There is no client-side signing secret; auth and S3 credentials stay server-side.
- No `Range` streaming in the local segment proxy. FFmpeg TS segments are fetched as entire objects. Production CDN requires separate byte-range/subtitle/DRM design.
- The media proxy strictly whitelists `[A-Za-z0-9_./-]` plus allowed `.m3u8` / `.ts` suffix and rejects `..`, percent-encoding, and absolute URLs.
- A session ID alone does not authorize a segment: a valid bearer token for the session account is also required.
- A media asset becoming unpublished, rights expiring, account deactivation, or age restriction causes new segment/heartbeat requests to fail.
- Premium requests are HTTP 402 pending Build 05; do not fake subscriptions on the frontend.
- Under a proxy crash/disconnect, abandoned sessions expire automatically. This code is not a production traffic CDN.

## Smoke and validation commands

```bash
# Offline, dependency-free/locally installed utilities
python3 scripts/check_build04.py
python3 scripts/test_playback_preview.py  # Chromium + Playwright; only UI simulation
python3 -m unittest discover -s apps/video-worker/tests -v

# Real stack (must have Docker and internet access for first dependency pull)
docker compose config
docker compose up --build -d
cd apps/api && mvn test && cd ../..
cd apps/web && npm install && npm run typecheck && npm run build && cd ../..
python3 scripts/e2e_processing_live.py # create approved generated-video fixture
python3 scripts/e2e_playback_live.py # test real signed-in viewer and private HLS
```

## Remaining engineering

- CDN signed cookies/authorizer and managed production DRM instead of proxy; Safari/native HLS authentication.
- Verified server-side geolocation; Keycloak refresh/rotation and long-running playback tokens.
- Billing and PREMIUM entitlements, fraud control and per-plan concurrency rules.
- Resilience and concurrency integration tests, user data retention/deletion, streaming analytics and customer-facing error recovery.
