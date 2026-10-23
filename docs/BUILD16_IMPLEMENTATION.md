# ReelNest Build 16 — Series navigation, private seek previews and UX reliability

**Source baseline:** cumulative Build 15. **Release decision: NO-GO.** This release contains source changes and offline tests. It does **not** attest a Maven/Next.js production build, Flyway V11, real device playback, Docker integration, AWS, Razorpay or content rights.

## Ten master checklist tasks addressed

| Task | Source change | Completeness and required follow-up |
|---|---|---|
| **D-07** | Added conditional 1080p H.264/AAC rendition to FFmpeg ladder, never upscale small sources. | Implemented; only pure ladder policy tested for 1080p, full 1080p encoding needs a suitable source and resources. |
| **D-10** | Bounded JPEG preview generation (max 120), WebVTT timeline index and worker private-object upload. | **Partial:** seek previews implemented; posters, sprite atlases and editorial poster pipeline remain. |
| **D-12** | Strict offline SRT→WebVTT parser/converter, bounded size/count/duration, overlap and markup rejection. | **Partial:** creator subtitle upload/storage and HLS alternate subtitle track wiring not yet implemented. |
| **D-16** | Validate all playlist-referenced segments exist and are nonempty; decode first/middle/last segment per variant before READY. | **Partial:** full decode of every segment and CDN delivery E2E still outstanding. |
| **E-10** | V11 series/episode mapping, admin-only episode linking with same-creator/published checks, public series season picker, entitled next-episode lookup. | **Partial:** episode media uses separately published MOVIE titles; richer normalized episode metadata, TV player picker and live DB tests pending. |
| **E-12** | Per-profile `autoplayNext` (default false), opt-in Up Next countdown, cancellation and manual Play Next. | Implemented in source; requires browser/Keycloak/DB tests. |
| **E-13** | Authenticated private JPEG/VTT seek previews, safe media allowlist, bounded object-URL cache, spoiler-hidden future frames. | Implemented in source; needs browser, Safari, signed-CDN and network tests. |
| **J-04** | Compact desktop navigation, reachable mobile menu, accessible toggle and close-on-route-change. | Implemented in source; responsive browser/a11y tests pending. |
| **J-05** | Title loading skeleton, distinguish 404 from temporary API failure, retry button, reduced-motion shimmer. | Implemented in source; real API error tests pending. |
| **J-11** | Public Help & FAQ covering rights, buffering, captions, billing, creator and privacy; optional monitored support email. | **Partial:** live support ticket backend/SLAs and operational mailbox are not configured. |

**Additional C-01 progress:** the series-episode association is explicit, but complete normalized season/episode/trailer domain modeling is not done.

## Database migration

`apps/api/src/main/resources/db/migration/V11__series_and_autoplay.sql` creates `series_episodes` and adds `player_preferences.autoplay_next` default false. It does not grant any rights or entitlements. Episodes are independently published playable titles. The admin linking endpoint checks published state and common creator; uniqueness is enforced by PostgreSQL and duplicates return conflict.

## New API contracts

- `GET /api/v1/catalog/{seriesId}/episodes` — public metadata for **published** episodes of a published series.
- `PUT /api/v1/admin/series/{seriesId}/episodes` — ADMIN only, JSON `{"episodeTitleId":"UUID","seasonNumber":1,"episodeNumber":1}`. No silent overwrite.
- `GET /api/v1/playback/next-episode?profileId=UUID&titleId=UUID` — authenticated and owned profile; **204** if no immediate next episode is currently playable. Uses existing creator approval, READY media, country rights, age and premium entitlement policy. The frontend still requests a fresh authorized playback session for the next episode.
- `GET/PUT /api/v1/profiles/{profileId}/player-preferences` — adds `autoplayNext` boolean; false by default. Existing settings continue to work when updated clients send this field.
- Private media proxy allows only `previews/preview.vtt` and `previews/thumb_####.jpg` in addition to HLS. CDN path remains signed and restricted to the specific media prefix.

## Media pipeline behavior

FFmpeg generates 320px-wide JPEG frames and `previews/preview.vtt` (up to 120 images). The worker uploads them to the existing private processed bucket with appropriate `text/vtt` / `image/jpeg` types. Preview extraction is **best effort**; if it fails, its partial directory is removed and valid HLS processing continues. Representative HLS segments are decoded before READY. Generated previews contain frames from the video and therefore inherit the **same title rights and protection** as HLS. The client loads previews only through a valid playback session, uses an authenticated fetch or signed CDN cookies, never puts bearer tokens in image URLs, and hides future thumbnails while scrubbing.

The SRT conversion helper is intentionally not yet connected to creator uploads. It cannot be advertised as a finished subtitle-ingest service.

## Tests actually executed

- `python3 -m pytest apps/video-worker/tests apps/recommendation-api/tests apps/recommendation-lab/tests tests/release -q` — **46 passed** (includes Build 16 source contracts and real FFmpeg package generation/decode tests).
- `node scripts/test_build16_frontend.mjs` — **11 runtime assertions passed** for preview parsing/URL policy and default-off autoplay preference.
- `bash scripts/test_build16_java.sh` — **14 compiled Java policy assertions passed** for private preview path allowlisting.
- `node scripts/check_ts_syntax.mjs` — **59 TS/TSX files parsed, zero syntax errors**; NOT a semantic typecheck.
- `python3 scripts/release_gate.py --mode offline` — **16/16 groups passed**; **12/12 external production gates unverified**.

## Codex / local runtime follow-up (do not mark PASS without actual evidence)

1. Run `mvn -B verify` in `apps/api`; fix Java compilation errors and run V1–V11 Flyway migration on clean and upgraded PostgreSQL.
2. Run `npm install`, `npm run typecheck`, `npm run build` in `apps/web`; fix semantic/type and hydration errors.
3. Start Docker Compose; publish one original rights-approved SERIES plus at least two independently licensed READY episode titles, link as ADMIN and verify 403/404/409 paths.
4. Test next episode with adult/child profiles, different subscriptions and rights territories, and expired licences; verify no cross-profile data leak or autoplay bypass.
5. Encode a licensed 1080p source; verify 360/480/720/1080 renditions, previews and caption tracks (when later implemented).
6. Test authenticated private preview fetch, missing preview graceful fallback, CloudFront signed-cookie behavior, Safari native HLS, autoplay countdown cancellation, mobile menu, and screen readers.
7. Ensure no private preview URLs or tokens are exposed through logs, HTML metadata, or public S3 policies.

**Known outstanding work:** D-10 posters/sprites, D-12 ingest and alternate subtitle packaging, D-16 full-segment validation, E-10 native TV episode picker, I-09 telemetry replay, J-13 rights-aware per-title SEO, plus all previously unverified P0 integration gates.
