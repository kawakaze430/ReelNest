# Build 04 validation record — 2026-10-21

## Actually executed in authoring environment

| Check | Result | Meaning |
|---|---|---|
| `javac` JavaParser / AST parse of entire backend | **PASS — 38 files / 0 errors** | Java syntax verified, not dependency-aware Maven compilation |
| Standalone compiled Java policy assertions (`AgeRating`, `PlaybackPolicy`) | **PASS** | Creator short-code normalization, child limits, path traversal, PREMIUM default denial |
| TypeScript compiler AST parse | **PASS — 29 TS/TSX files / 0 errors** | Source syntax only; no `node_modules`, no dependency-aware typecheck |
| `python3 scripts/check_static.py` | **PASS** | Prior build source, catalog/config checks |
| `python3 scripts/check_build02_security.py` | **PASS** | Creator/rights ownership and private-upload structure |
| `python3 scripts/check_build03.py` | **PASS** | Worker-job/queue/private bucket/CORS architecture markers |
| `python3 scripts/check_build04.py` | **PASS** | Playback gates, migration, private media path and frontend state markers |
| `python3 scripts/check_media.py` | **PASS** | Original video & HLS sample assets |
| Python FFmpeg encoder unit tests | **PASS — 5 tests** | Original generated video processed, adaptive ladder/silent/invalid input checks |
| Original catalog preview via Chromium | **PASS** | Offline browser search/favorite/details interactions |
| Creator flow preview via Chromium | **PASS** | Offline simulated 7-step workflow/mobile layout |
| Build 04 playback-flow preview via Chromium | **PASS** | Offline UI profile isolation, local favorites, progress/reset, mobile layout |
| ZIP SHA-256 file manifest and archive integrity | **Run during packaging** | Confirms ZIP files were included uncorrupted; see release log |

## Not verified here

1. No Docker daemon/executable, thus no Compose service startup and **no PostgreSQL Flyway V5 execution**, Redis/Keycloak/MinIO connectivity, real S3 private HLS reads or browser-to-API playback.
2. No Maven executable/cached Spring dependencies: JUnit/Spring compilation, Hibernate schema validation and Java runtime integration **not executed**.
3. No frontend `node_modules`, and npm registry DNS returned `EAI_AGAIN`: Next.js compilation and full TypeScript typecheck **not executed**.
4. Payment entitlements, CDN authorization, DRM, verified country geolocation and production security tests are deliberately not implemented in Build 04.
5. Python scripts `scripts/e2e_processing_live.py` and `scripts/e2e_playback_live.py` have been authored and syntax checked but require a **disposable full Docker environment**.

### To reproduce on a development workstation

```bash
cd reelnest
python3 scripts/check_build04.py
python3 -m unittest discover -s apps/video-worker/tests -v

docker compose config
docker compose up --build -d
cd apps/api && mvn test && cd ../..
cd apps/web && npm install --no-audit && npm run typecheck && npm run build && cd ../..
python3 scripts/e2e_processing_live.py
python3 scripts/e2e_playback_live.py
```

**The product is not production-ready until these live checks pass and the remaining threat model items are addressed.**
