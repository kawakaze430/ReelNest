# ReelNest Build 17 — Protected captions, media artwork, progress, SEO and QoE replay

**Baseline:** cumulative Build 16. **Decision:** NOT PRODUCTION READY. This is source development with offline FFmpeg, Python, Java policy and TypeScript helper tests. No actual Maven/Next.js build, Flyway V12–V14 execution, Docker, browser playback, CDN, AWS or payment-provider test was performed here.

## Ten master-checklist tasks addressed

| ID | Actual code delivered | Completeness / remaining evidence |
|---|---|---|
| **D-10** | Private 1280px landscape poster, 10×10 sprite atlases and WebVTT `#xywh` indexes; thumbnail fallback remains. | **Partial:** no creator-selected/editorial poster pipeline or public catalog artwork yet; device/CDN tests pending. |
| **D-11** | Creator-owned draft SRT upload/list/delete, maximum eight language tracks; worker packages alternate HLS WebVTT subtitle renditions. | **Source implemented for SRT subtitles;** alternate audio, browser compatibility and real database tests pending. |
| **D-12** | Previously offline-only strict SRT conversion now connected to creator upload and worker HLS packaging; timing checked against actual video duration. | **Source implemented for SRT;** additional formats, caption QA and integration pending. |
| **D-14** | Worker progress stages, lease-guarded monotonic milestones, V12 migration, creator progress display and polling. | **Partial:** stage percentages are milestones, not measured ETA; real lease/retry/DB test pending. |
| **D-16** | Strict HLS master/variant URI and segment checks; optional full sequential FFmpeg decode of every segment. | **Partial:** all referenced files always validated; full decode requires `REELNEST_FULL_SEGMENT_DECODE=true`; CDN E2E pending. |
| **E-06** | Player's existing HLS subtitle selectors now have worker-produced alternate subtitle tracks to discover. | **Partial:** real Safari/hls.js caption selection pending; alternate audio not implemented. |
| **E-13** | Private sprite VTT parser, authenticated atlas loading, thumbnail fallback and spoiler-conscious scrub UI. | **Source implemented;** cross-browser rendering/CDN tests pending. |
| **E-19** | Replaced permissive `*.ts`/`*.m3u8` media proxy paths with exact generated-file allowlist. | **Security source fix**, Java policy assertions pass; negative API tests pending. |
| **I-09** | In-memory, opt-in QoE retry queue with stable event IDs, bounded size/backoff, permanent-error drop and opt-out clearing. Existing server-side dedupe remains. | **Source implemented for active-session replay**; durable offline cross-session queue intentionally NOT added for privacy. Browser/network tests pending. |
| **J-13** | Default-noindex per-title server metadata; admin SEO approval, rights/territory/READY filtering, bounded eligible-title sitemap and revocation. | **Source implemented**; Next.js SSR/SEO crawler, migration and actual licence workflow tests pending. |

## Database migrations (must run in order on clean AND upgraded PostgreSQL)

- **V12** adds progress stage and bounded percentage to `media_processing_jobs`, backfilling existing rows.
- **V13** adds `titles.seo_index_approved` default **FALSE**; the admin must explicitly approve indexing, and public endpoints independently require published, licensed, current, READY content for the configured market.
- **V14** adds `title_caption_sources`, scoped to title and lowercase language, with a 200 KB UTF-8 payload bound. Creator API never returns source text to a public caller.

## New API contracts

- `GET /api/v1/creator/titles/{titleId}/captions`: owner/approved creator; returns language + update time, not text.
- `PUT /api/v1/creator/titles/{titleId}/captions/{language}`: approved owner + DRAFT; JSON `{ "srt": "..." }`. Must precede source upload unless existing media failed; no more than 8 tracks. Strict Python duration/overlap validation occurs during encoding. An invalid track causes the worker to fail/retry; correct the caption and have an admin retry the FAILED job.
- `DELETE /api/v1/creator/titles/{titleId}/captions/{language}`: same ownership/draft/media guards.
- `GET /api/v1/catalog/{id}/seo`: public only when admin-approved, published, rights/creator-approved, currently licensed in configured country and READY media exists; otherwise 404.
- `GET /api/v1/catalog/seo-index?page=0&size=100`: bounded public metadata list with same rights checks.
- `GET /api/v1/admin/titles/seo-review`: admin-only published title review list.
- `POST /api/v1/admin/titles/{titleId}/seo-approval`: admin-only `{ "approved": true|false }`, audited; approval requires current rights and READY media, revocation always possible.
- Existing `GET /api/v1/creator/titles/{titleId}/media` now includes `progress_stage` and `progress_percent`.

## Security and media behavior

- Subtitle files (`captions/{language}/index.m3u8` and `captions/{language}/captions.vtt`), sprite atlas and poster are stored **under the same private processed prefix** as the movie. The development proxy only serves allowlisted paths after reauthorizing an active playback session. Production signed-CDN behavior must be independently tested.
- `validate_package` rejects untrusted playlist URLs, extra directives, missing/nonsequential segments and invalid caption media playlists. Every segment is checked for presence and size; representative segments are decoded by default. `REELNEST_FULL_SEGMENT_DECODE=true` enables all-segment decoding at additional CPU/time cost.
- QoE retry events are held in **memory only** and discarded on session teardown or consent withdrawal. No token, IP, raw URL or viewer ID is added to event payloads. Retried events reuse their original UUID.
- Robots no longer blanket-disallows `/title/`: disallowing it prevents crawlers from reading a page's **noindex** directive. Instead, server-rendered title metadata defaults to noindex and only explicitly eligible titles are indexable. No indexing occurs from a static demo catalog or when the rights API fails.

## Tests actually executed in authoring environment

- `python3 -m pytest apps/video-worker/tests apps/recommendation-api/tests apps/recommendation-lab/tests tests/release -q`: **60 passed**, including actual local FFmpeg video generation, poster/sprite decode, caption HLS packaging, full decode of test segments, and malicious playlist URI tests.
- `bash scripts/test_build16_java.sh`: **29 pure Java allowlist assertions passed**.
- `node scripts/test_build16_frontend.mjs`: **11 runtime helper assertions passed**.
- `node scripts/test_build17_frontend.mjs`: **21 runtime QoE replay and sprite assertions passed**.
- `node scripts/check_ts_syntax.mjs`: **61 TS/TSX files parsed, zero syntax errors**. Not a semantic typecheck.
- `python3 scripts/release_gate.py --mode offline`: **18/18 offline groups passed**, **12/12 external gates remain unverified**.
- Python worker `compileall`: passed.

## Priority local/Codex acceptance matrix (not executed here)

1. Run `mvn -B verify` in `apps/api`, `npm ci && npm run typecheck && npm run build` in `apps/web`; fix real compilation failures.
2. Run Flyway V12–V14 on both a fresh database and a database upgraded from V11; verify backfill and constraints.
3. With an approved creator and a draft title, upload SRT in two languages, then a short **owned** sample film. Verify alternate subtitle HLS master, authorized subtitle segments, browser track selection and poster/sprite rendering. Test malformed SRT, >8 tracks, wrong-owner and published-title denial.
4. Test processing retries and lease loss; verify percentage resets per attempt, 100% only after READY, and no leaked object keys in creator APIs.
5. Test public SEO in three states: unapproved (noindex/404), approved current rights and READY (indexable), expired/revoked rights (immediate 404 and sitemap exclusion). Verify crawler output on deployed Next.js server.
6. Test QoE opt-out during pending network retries, expired session, 403 and 503, and consent purge in PostgreSQL.
7. Verify private S3/CloudFront origin access, cookie/session scope, territorial denial, and DRM as required by the content licence.

**Known limitations:** Caption editing after source upload is intentionally restricted to avoid stale packages; failed media can be corrected and requeued by an admin. No native mobile/TV apps, DRM licences, commercial rights, production payment validation or worldwide deployment are claimed. A future release should add creator artwork selection, native TV episode picker, alternate audio, subtitle QA UI, support tickets and further reliability work.
