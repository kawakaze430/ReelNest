# ReelNest Build 32 — Editor-reviewed scene/chapter explorer

**24 October 2026.** Source of truth: GitHub `kawakaze430/ReelNest`, `main`.

## Feature work
Original optional task `Q-09` receives its first substantial source implementation:
- Additive `V26__editor_reviewed_scene_markers.sql`: editor-owned scene labels, summaries and integer media time ranges bound to a specific processed asset; max 40 per asset, bounded fields, FK cascade and index. Old V1–V25 migrations are preserved.
- Admin-only `GET/PUT /api/v1/admin/titles/{titleId}/media/{assetId}/scenes` endpoints. Requires admin role and READY asset/title match, validates ordering/non-overlap and duration, atomically replaces annotations, audits edits. Review Console adds an editor.
- Authenticated viewer `GET /api/v1/playback/scenes?profileId=...&titleId=...` returns 403 until *existing* PlaybackService availability authorizes this exact profile/title for verified territory, current rights, approved creator, age rating and paid plan. GET sends Cache-Control: no-store; metadata never grants media access.
- React player scene explorer lets authorized viewers search the human-written labels and jump inside their active playback session. No general-purpose AI scene search or unlicensed transcript access.
- Added JUnit policy tests, source guard tests, and updated disposable clean/V9-upgrade Flyway expectations to V26.
- CI source builds and real migration tests must pass on latest main before considering the code compile-verified.

## What is NOT implemented
This is **human-reviewed scene indexing**, not automatic CV/NLP scene understanding. Machine-generated annotations, new personalized spoiler handling, cross-title semantic retrieval, actual browser playback and authorized editor session tests remain open. Q-09 is only **partially implemented**, not production accepted.

## Build 32 exact task counts
| Bucket | Count |
|---|---:|
| CI verified (narrow historical components) | 5 |
| Source implemented, runtime unverified | 67 |
| Partially implemented | 131 |
| Blocked, execution/testing | 31 |
| Blocked, vendor/account approval | 17 |
| Not started | 31 |
| **Total** | **282** |

**Production acceptance remains 0/282.** G1 creator-to-licensed-stream E2E, G2 payment acceptance, G3 AWS staging, G4 security/data recovery, and G5 licensed territory release remain OPEN. No AWS resources or live payments were created.

## Verification paths
- [Build checks on GitHub](https://github.com/kawakaze430/ReelNest/actions)
- Disposable PostgreSQL clean and upgrade [workflow](https://github.com/kawakaze430/ReelNest/actions/workflows/postgres-flyway.yml)
- Java unit: `mvn --batch-mode --no-transfer-progress -f apps/api/pom.xml verify`
- Next.js: `cd apps/web && npm ci && npm run typecheck && npm run build`
- Release: `python3 -m pytest tests/release/test_build32_scenes.py tests/release/test_build32_status.py -q`
- Manual G1: reviewer adds scene marker to approved READY asset, eligible signed-in profile sees scene and can jump to time; another profile/expired right/anonymous request must get 403; no media may be unlocked by scene metadata.

## Second feature — human subtitle-localization QA (Q-10)
Creator Studio now includes a browser-only source-versus-translated SRT comparison, using the existing strict SRT validator. This checks cue count, timeline differences over 250ms, unchanged text, and unsafe subtitle markup. No caption text is uploaded by the comparison operation. Creators still upload their manually approved translations through the existing private caption source API *before video encoding*. No generative translation, dubbing, audio mastering or editor language approval is implemented. Task Q-10 advances to partial implementation. `node scripts/test_build32_translation.mjs` performs the runtime QA algorithm checks in CI.
