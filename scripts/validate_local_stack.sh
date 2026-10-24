#!/usr/bin/env bash
# Build 12 — explicit local acceptance run. Requires a fresh developer Docker stack.
# Does not delete volumes, reset user data, make charges, or deploy cloud resources.
set -Eeuo pipefail
cd "$(dirname "$0")/.."
MODE="${1:---smoke}"
if [[ "$MODE" != --smoke && "$MODE" != --full ]]; then
 echo 'Usage: scripts/validate_local_stack.sh [--smoke|--full]' >&2; exit 2
fi
for bin in docker python3 curl; do
 command -v "$bin" >/dev/null || { echo "BLOCKED: missing $bin" >&2; exit 3; }
done
if ! docker info >/dev/null 2>&1; then echo 'BLOCKED: Docker daemon not accessible' >&2; exit 3; fi
mkdir -p build-evidence
log="build-evidence/local-stack-$(date -u +%Y%m%dT%H%M%SZ).log"
exec > >(tee "$log") 2>&1
trap 'echo "FAILED: inspect docker compose logs --tail=120 api video-worker web recommendation-api"' ERR
if [[ "$MODE" == --full ]]; then
  # Full E2E uploads only this repository-owned synthetic media fixture.
  test -s apps/web/public/sample/demo.mp4 || {
    echo 'BLOCKED: original sample media missing; use scripts/generate_demo_media.sh on an isolated developer system' >&2
    exit 3
  }
fi
printf 'Checking Docker Compose...\n'
docker compose config -q
printf 'Building and starting services...\n'
docker compose up --build -d
printf 'Waiting for backend and ML health...\n'
for attempt in $(seq 1 90); do
 if curl -fsS http://127.0.0.1:8080/actuator/health >/dev/null && \
   curl -fsS http://127.0.0.1:3000 >/dev/null && \
   docker compose exec -T recommendation-api python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health',timeout=4)" >/dev/null; then break; fi
 if [[ "$attempt" -eq 90 ]]; then echo 'ERROR: startup health timeout' >&2;exit 1;fi
 sleep 3
done
printf 'Confirming media-worker modules inside the running non-root container...\n'
docker compose exec -T video-worker python -c 'import worker, pipeline, caption_packaging, timed_text, audio_packaging, preview_artwork'
printf 'Checking Flyway migrations...\n'
docker compose exec -T postgres psql -U reelnest -d reelnest -Atqc \
 "SELECT CASE WHEN COUNT(DISTINCT version) FILTER (WHERE success AND version IN ('1','2','3','4','5','6','7','8','9','10','11','12','13','14','15','16','17','18','19','20','21','22','23','24','25','26'))=26 AND COUNT(*) FILTER (WHERE NOT success)=0 THEN 'PASS' ELSE 'FAIL' END FROM flyway_schema_history" | grep '^PASS$'
printf 'Checking API smoke tests...\n'
python3 scripts/live_smoke.py
bash scripts/security_smoke_live.sh
if [[ "$MODE" == --full ]]; then
 echo 'Running original-asset ingestion-to-playback E2E. Use a clean dev DB for fixture assumptions.'
 python3 scripts/e2e_processing_live.py
 python3 scripts/e2e_playback_live.py
fi
printf 'PASS local %s validation; logs: %s\n' "$MODE" "$log"
