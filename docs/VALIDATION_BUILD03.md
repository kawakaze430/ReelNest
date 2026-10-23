# Build 03 validation record — 2026-10-21

## Actual checks executed

| Check | Result | Interpretation |
|---|---|---|
| Original generated FFmpeg 720p video -> 360/480/720 HLS | **PASS** | All three expected rendition playlists and segments generated |
| Decode sample segment for each HLS variant with FFmpeg | **PASS** | Generated chunks are media-decodable |
| Small 240p input | **PASS** | Encoder does not upscale smaller input |
| Malformed non-video upload | **PASS** | FFprobe validation rejects invalid source |
| Disguised malicious remote HLS playlist | **PASS** | Playlist/container rejection and protocol allowlisting |
| Silent video without audio track | **PASS** | Audio-optional HLS encoding works |
| `python -m unittest discover -s apps/video-worker/tests -v` | **PASS (5 tests)** | FFmpeg/FFprobe encoder path actually executed |
| `python scripts/check_static.py` | **PASS** | Build 01/02 structure, JSON, XML and YAML |
| `python scripts/check_build02_security.py` | **PASS** | Source-level role/ownership and multipart markers |
| `python scripts/check_build03.py` | **PASS** | Build 03 worker/job/CORS/config markers |
| `python scripts/check_media.py` | **PASS** | Build 01 original clip and its segment metadata |
| `python scripts/test_preview_browser.py` | **PASS** | Standalone catalog preview browser interactions |
| `python scripts/test_creator_preview.py` | **PASS** | Standalone simulated creator/admin preview interactions |
| Java AST syntax parse via javac JavaCompiler API | **PASS** | 30 Java files parsed, 0 syntax errors — not compiled with Spring dependencies |
| TS/TSX syntax parse via global TypeScript | **PASS** | 22 TS/TSX source files parsed, 0 syntax errors — not dependency typechecked |
| ZIP checksum and content integrity | pending during authoring, recorded in final handoff | Archive manifest checked during packaging |

## Not executed in this sandbox

- No `docker` executable; no running Docker Compose services. Consequently no real Keycloak, PostgreSQL, MinIO or video-worker integration.
- No Maven executable or available cached Spring dependencies: backend Maven compilation, JUnit tests and live Flyway migrations NOT run.
- No frontend `node_modules` or npm package registry access: Next.js build and TypeScript dependency-aware typecheck NOT run.
- No real browser-to-MinIO multipart upload with CORS/ETag, S3/SQL transactional failure injection or admin publication flow.
- Optional `scripts/e2e_processing_live.py` is authored but not executed. It mutates the local developer database; use it only with disposable fixtures.

## Validation commands for a developer machine

```bash
python3 scripts/check_static.py
python3 scripts/check_build02_security.py
python3 scripts/check_build03.py
python3 -m unittest discover -s apps/video-worker/tests -v
python3 scripts/check_media.py

# Docker-capable host with internet access
docker compose config
docker compose up --build -d
docker compose ps
docker compose logs -f video-worker api minio-init

# If Maven/npm installed on host
cd apps/api && mvn test && cd ../..
cd apps/web && npm install && npm run typecheck && npm run build && cd ../..

# Disposable stack, requires Python Playwright and Chromium
python3 scripts/e2e_processing_live.py
```

**Do not claim production readiness without successfully completing the pending integration tests and security hardening.**
