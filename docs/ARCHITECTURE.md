# Architecture — ReelNest Build 03

```text
Browser -> Next.js viewer / Creator Studio / Admin Review
      |           |                       |
      +---- OIDC -> Keycloak              |
      +--- JSON + JWT -> Spring Boot API <-+
                          |       |
                  PostgreSQL     S3-compatible MinIO (source bucket private)
                      |                       ^
             media_processing_jobs            | multipart signed PUT from browser
                      |
           separate worker container
              /            \
     FFprobe/FFmpeg     worker job leases, retries, dead-job recovery
              \
               -> processed-private MinIO: HLS master, variant playlists, segments
                        |
                  READY media asset
                        |
             Admin-reviewed rights + valid term -> published catalog entry

Viewer streaming of private creator assets still blocked until Build 04.
```

## State machines

Upload intent: `INITIATED -> COMPLETED | ABORTED`; delayed S3/DB repair in `reconcile_uploads`. An expired unfinished multipart upload becomes `ABORTED`.

Media asset: `QUEUED -> PROCESSING -> READY`; transient failures `PROCESSING -> RETRY_WAIT -> PROCESSING` (3 attempts max), terminal `FAILED` or `REJECTED` (corrupt media). A failed asset can be administratively requeued.

Processing job: `QUEUED -> RUNNING -> SUCCEEDED`, or `RUNNING -> RETRY_WAIT -> RUNNING`, or `RUNNING -> FAILED | REJECTED`. Lost leases can be reclaimed with `SKIP LOCKED`; expired final attempts become `FAILED` on the next claim cycle.

Publication: `DRAFT -> IN_REVIEW -> PUBLISHED` only after admin approves rights and at least one media asset is `READY` with a manifest key; uploaded bytes cannot be streamed anonymously. Rights must still be verified and valid on the publication date.

## Transaction boundary and idempotency

1. Browser uploads parts to a randomized, private S3 key. Spring validates S3-observed ETags and per-part byte sizes.
2. S3 `CompleteMultipartUpload` occurs **outside** the SQL transaction. Retrying completion first HEAD-checks for an already-assembled object of the expected size, making a prior S3-success / DB-failure repairable.
3. Single PostgreSQL transaction locks the intent row, commits `COMPLETED`, creates the unique `media_assets` row, creates a unique processing job, and writes an audit action. A second completion skips the already-completed row.
4. Worker claims a job using `FOR UPDATE SKIP LOCKED` and sets a short lease. Worker heartbeat renews it; stale workers cannot set `READY` after losing the lease.
5. Outputs are uploaded into a unique prefix per attempt. Only a complete output whose S3 master manifest can be HEAD-confirmed is recorded as READY.
6. Reconciliation polls for aged upload intents with completed S3 objects, and aborts incomplete expired multipart upload sessions. This guards against abandoned originals, while the object-storage lifecycle policy remains an outstanding production control.

## Encoding approach

- Inspects source using FFprobe, rejects missing video tracks, non-finite/excessive durations, extreme dimensions, and excessively many tracks.
- Uses FFmpeg to create MPEG-TS segments and VOD HLS playlists; levels are capped at 360p, 480p and 720p. A smaller input gets only one non-upscaled rendition.
- Validates each generated playlist, segment existence and format, then uploads every file privately. `master.m3u8` exposes relative variant playlists; it is not publicly served in this build.
- This is functional engineering scaffolding, not secure studio-grade video DRM. Further container isolation, malware scanning, video/audio compliance checks, subtitles, CMAF, encryption, signed CDN playback, and extensive load testing remain.

## Security boundary

- Keycloak JWT and database-approved creators gate creator APIs; creator media status lookups verify title ownership.
- Admin APIs require `ROLE_ADMIN` and avoid returning raw storage keys in UI responses.
- MinIO source+processed buckets have NO public anonymous grants. Bucket CORS allows only local web origin for uploading parts and exposing ETag.
- Worker credentials are limited by the local MinIO fixture credentials; **fine-grained IAM** must be added for production.
- Uploaded creator media cannot be publicly viewed merely because a title has a catalog listing.

## Schema and upgrade

- V1: account, catalog, media, progress.
- V2: fictional demo metadata.
- V3: creator/rights/upload intent/audit state.
- **V4:** unique source-object key, JSON rendition details, processing-job queue table, and backfill to queue legacy `AWAITING_INSPECTION` media from prior Build 02 installations.

## Future Build 04

Authenticated playback sessions, territorial/age/rights/subscription entitlements, short-lived signed media authorization, player integration, resume progress, and content rights expiry during streaming. No unprotected public-read bucket shortcuts.

## Build 04 — authorized FREE playback

User and profile → `POST /api/v1/playback/sessions` → Java checks account/profile ownership,
creator ACTIVE/APPROVED, published title, rights approved/date/territory, child rating,
media READY and free tier → admission lock and expiring PostgreSQL session. Browser
hls.js supplies bearer JWT per media request; local Spring proxy re-evaluates session
and rights for every manifest/variant/segment and streams only allowed private MinIO
processed `.m3u8`/`.ts` files. Every 30s heartbeat refreshes lease; progress events
with increasing sequence update PostgreSQL resume position; watchlist is per-profile.

**Local development path only.** Real launch requires replacing proxy with private
CloudFront distribution plus short-lived signed cookie policy/DRM appropriate to content,
trusted-edge territory verification and operational performance testing.

See `docs/BUILD04_PLAYBACK_DESIGN.md`, `docs/PROJECT_STATE.md`.
