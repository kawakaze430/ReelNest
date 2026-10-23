# ReelNest Build 18 — Multilingual HLS audio, caption QA, rights-first ML evaluation

**Baseline:** Build 17 cumulative. **Status:** SOURCE IMPLEMENTED / NOT PRODUCTION READY. No Maven/Next.js dependency build, Docker, Flyway, live authenticated browser playback, CloudFront, AWS, payments or film-rights verification occurred in this authoring environment.

## Ten master-checklist tasks addressed (implementation progress, NOT release sign-off)

| Task | Delivered source change | Completeness and acceptance still required |
|---|---|---|
| **D-11** | Multi-audio MP4/MKV/MOV with 2–8 embedded audio tracks now produces protected per-track AAC HLS renditions and master `EXT-X-MEDIA:TYPE=AUDIO` group; existing SRT tracks coexist. | **Partial:** separately uploaded dubbed audio, licence review, Safari/TV/DRM and live processing remain. |
| **D-12** | Added in-browser strict SRT syntax/overlap/markup/size preflight and nonblocking readability warnings before creator upload. Worker retains final duration-aware validation. | **Incremental:** WebVTT/TTML input and human QA remain. |
| **D-16** | Strictly validate audio rendition tags, fixed numeric paths, sequential segment references and decode representative/all audio segments under FFmpeg timeouts. | **Incremental:** production-scale long-film QA and CDN E2E pending. |
| **E-06** | Existing hls.js audio selector can now select worker-generated alternate audio. Safari native `AudioTrackList` is feature-detected and optionally selectable. | **Partial:** browser/device verification and track-switch timing needed. |
| **E-22** | Native audio selection applies stored per-profile preferred language and saves explicit selection through existing profile preferences. | **Incremental:** actual multi-device sync and browser event behavior unverified. |
| **F-09** | Exact Java media proxy allowlist now covers only generated `audio/a00..a07` playlists and MPEG-TS segments; missing caption group and malicious audio URI rejection added. | **Source fix:** live authenticated/unauthorized API and CDN tests pending. |
| **K-07** | Creator Studio shows cue count, last cue time, structural errors, and reading-speed/line-length warnings; blocks invalid SRT upload client-side. | **Partial:** human subtitle QA, editing tools and actual upload integration unverified. |
| **H-07** | Offline `TimedInteraction` and global timezone-aware chronological train/test cutoff; future events excluded from training. | **Offline research:** consented real dataset, repeatability, deployment not completed. |
| **H-08** | Aggregated Recall@K, NDCG@K, eligible catalog coverage, genre diversity, and per-language catalog coverage. | **Offline metrics:** baseline/holdout business metrics pending. |
| **H-15** | Rights-first candidate curation supports editorial pins, exclusions and soft per-genre cap without admitting IDs outside the already-eligible ranked set. | **Offline helper:** not wired into live serving; admin review UI pending. |

## Engineering details

- **Source audio:** `apps/video-worker/audio_packaging.py`, `pipeline.py`, `caption_packaging.py`. Source languages are sanitized; malformed/oversized/remote HLS playlists are rejected. Audio group appears only when >=2 source tracks. Mono/single-audio assets keep their original layout. Up to 8 tracks; HLS audio renditions are AAC stereo in MPEG-TS. Video variants retain their muxed primary audio for legacy compatibility. The worker fails an attempt if a multilingual source cannot be packaged correctly, rather than silently dropping a language.
- **Private access:** `PlaybackPolicy.safeRelativeMediaPath` explicitly authorizes only `audio/a00..a07/index.m3u8` and `seg_00000.ts`-style files after existing session authorization. This is not DRM or CloudFront proof.
- **Playback:** `native-audio.ts` safely feature-detects Safari native audio tracks, and `protected-player.tsx` reuses the existing profile preferences and hls.js selector. Unsupported browsers retain the default track.
- **Caption QA:** `caption-qa.ts` parses in the browser, never uploads text during preflight, and emits a bounded warning list. Actual server/worker caption rules remain authoritative. A file >200 KB is not read for preview.
- **ML:** `apps/recommendation-lab/evaluation.py` uses no live production data, and curation cannot override eligibility. It does not prove recommendation uplift.

## Tests and source checks

1. `PYTEST_DISABLE_PLUGIN_AUTOLOAD=1 python3 -m pytest apps/video-worker/tests apps/recommendation-api/tests apps/recommendation-lab/tests tests/release -q` — **75 passed**, including real local FFmpeg two-audio-track HLS generation, full segment decoding and injection tests.
2. `bash scripts/test_build16_java.sh` — compiles `PlaybackPolicy` with `javac`; **36 assertions passed**.
3. `node scripts/test_build18_frontend.mjs` — actual transpiled native audio and SRT QA helpers; **18 assertions passed**.
4. `node scripts/check_ts_syntax.mjs` — **63 TS/TSX files parsed**, no syntax errors; **not** semantic typecheck.
5. `python3 scripts/release_gate.py --mode offline --output docs/BUILD18_OFFLINE_RESULTS.json` — **19/19 offline groups passed**, **12/12 external release gates unverified**.
6. Archive manifest SHA-256 and ZIP CRC verification, after packaging.

## Next local/Codex acceptance checklist (not run here)

1. **P0:** `mvn -B verify` in `apps/api`; `npm ci && npm run typecheck && npm run build` in `apps/web`. Fix any actual compile errors without weakening policy.
2. **P0:** Start Docker Compose, run Flyway V1–V14 on fresh and upgraded PostgreSQL, check worker and Keycloak startup.
3. **P1:** Upload an **owned** 2-audio-track MP4 and two SRT files. Verify READY processing, protected master, audio rendition playlists and subtitle selection on Chrome/hls.js and Safari native HLS; verify default audio and language persistence across profiles/devices.
4. **P0:** With an expired/revoked/wrong-profile session, request `/audio/a00/index.m3u8`, an audio segment, a subtitle and a poster: all must be denied. Test malformed playlist tags, missing segments and revoked territory rights.
5. **P1:** Test malformed SRT, oversized SRT, captions exceeding actual film duration, and editing lock after upload. Confirm creator warnings are nonblocking but structural errors block save.
6. **P1:** Run the offline ML evaluation on an authorized, consented, pseudonymous dataset with a fixed cutoff. Audit eligibility snapshots and temporal leakage; compare popularity and existing model baselines. Do not deploy the curation helper to live API before a rights and ownership review.

## Rollback

- Disable multilingual alternate-audio generation by reverting `package_audio(...)` invocation in `pipeline.encode` and removing the associated new paths from the Java allowlist; already-produced packages remain readable if the policy is preserved until migrated.
- Remove `native-audio.ts` imports and the creator SRT preview component to revert UI changes. Existing profile-preference schema is unchanged; **no new Flyway migration** in Build 18.
- The new offline ML evaluation module does not alter the serving model or any database state.

**Release decision: NO-GO.** External production sign-offs remain outstanding. All ten IDs above denote source progress, not closed acceptance checkboxes.
