#!/usr/bin/env bash
# Private developer-stack acceptance. Never deletes volumes or performs payments/deployments.
set -Eeuo pipefail
cd "$(dirname "$0")/.."
mkdir -p build-evidence
rm -f build-evidence/local-stack-latest.json
MODE="${1:---smoke}"
if [[ "$MODE" != --smoke && "$MODE" != --full ]]; then
 echo 'Usage: scripts/validate_local_stack.sh [--smoke|--full]' >&2; exit 2
fi
if [[ "$MODE" == --full ]]; then
  if [[ "${REELNEST_DISPOSABLE_STACK:-}" != true || ! "${COMPOSE_PROJECT_NAME:-}" =~ ^reelnest-(ci|test)-[a-z0-9][a-z0-9_-]*$ ]]; then
    echo 'BLOCKED: --full requires REELNEST_DISPOSABLE_STACK=true and a unique reelnest-ci-/reelnest-test- COMPOSE_PROJECT_NAME' >&2
    exit 3
  fi
fi
for bin in docker python3 curl; do
 command -v "$bin" >/dev/null || { echo "BLOCKED: missing $bin" >&2; exit 3; }
done
if ! docker info >/dev/null 2>&1; then echo 'BLOCKED: Docker daemon not accessible' >&2; exit 3; fi
mkdir -p build-evidence
log="build-evidence/local-stack-$(date -u +%Y%m%dT%H%M%SZ)-$$.log"
exec > >(tee "$log") 2>&1
export REELNEST_STACK_EVIDENCE=build-evidence/local-stack-latest.json
python3 - <<'PY'
import datetime as dt,json,os,subprocess
from pathlib import Path
Path(os.environ['REELNEST_STACK_EVIDENCE']).write_text(json.dumps({
 'startedAt':dt.datetime.now(dt.timezone.utc).isoformat(),
 'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),
 'project':os.getenv('COMPOSE_PROJECT_NAME','reelnest'), 'status':'RUNNING','steps':[]
},indent=2)+'\n')
PY
record_step() {
 python3 - "$1" "$2" <<'PY'
import json,os,sys
from pathlib import Path
p=Path(os.environ['REELNEST_STACK_EVIDENCE']);data=json.loads(p.read_text())
code=int(sys.argv[2]); data['steps'].append({'step':sys.argv[1], 'exitCode':code,'status':'PASS' if code==0 else 'BLOCKED' if code==3 else 'FAIL'})
p.write_text(json.dumps(data,indent=2)+'\n')
PY
}
finish() {
 local code=$?
 trap - EXIT
 python3 - "$code" <<'PY'
import datetime as dt,json,os,sys
from pathlib import Path
p=Path(os.environ['REELNEST_STACK_EVIDENCE']);data=json.loads(p.read_text())
code=int(sys.argv[1]);data.update(status='PASS' if code==0 else 'BLOCKED' if code==3 else 'FAIL',exitCode=code,finishedAt=dt.datetime.now(dt.timezone.utc).isoformat())
p.write_text(json.dumps(data,indent=2)+'\n')
PY
 if [[ "$code" != 0 ]]; then echo 'FAILED/BLOCKED: inspect private Docker diagnostics and local-stack-latest.json'; fi
 exit "$code"
}
trap finish EXIT
run_step() {
 local label=$1 result
 shift
 printf '\nChecking %s...\n' "$label"
 # Run in a separate errexit shell: failures within shell functions must not be
 # masked by the conditional used to capture their underlying exit status.
 set +e
 (set -Eeuo pipefail; "$@")
 result=$?
 set -e
 record_step "$label" "$result"
 if [[ "$result" != 0 ]]; then exit "$result"; fi
}
preflight() {
 if [[ "$MODE" == --full ]]; then
  test -s apps/web/public/sample/demo.mp4 || { echo 'BLOCKED: repository-owned original sample media missing' >&2; exit 3; }
  python3 - <<'PY' || { echo 'BLOCKED: install Playwright and its Chromium browser for full acceptance' >&2; exit 3; }
from playwright.sync_api import sync_playwright
from scripts.live_e2e_helpers import chromium_launch_options
with sync_playwright() as playwright:
    browser=playwright.chromium.launch(**chromium_launch_options())
    browser.close()
PY
 fi
}
wait_for_services() {
 local timeout="${REELNEST_STARTUP_TIMEOUT_SECONDS:-360}" deadline
 [[ "$timeout" =~ ^[1-9][0-9]*$ ]] || { echo 'Invalid startup timeout' >&2; exit 2; }
 deadline=$((SECONDS + timeout))
 until curl --max-time 5 -fsS http://127.0.0.1:8080/actuator/health >/dev/null && \
       curl --max-time 5 -fsS http://127.0.0.1:3000 >/dev/null && \
       curl --max-time 5 -fsS http://localhost:8180/realms/reelnest/.well-known/openid-configuration >/dev/null && \
       docker compose exec -T recommendation-api python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health',timeout=4)" >/dev/null; do
  if (( SECONDS >= deadline )); then echo 'ERROR: startup health timeout' >&2; exit 1; fi
  sleep 3
 done
}
verify_migrations() {
 # Strictly validate ordered, successful history rows, including rejecting
 # unexpected baselines/duplicates (a COUNT(DISTINCT version)=25 alone cannot).
 docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U reelnest -d reelnest -AtF '|' -c \
   'SELECT version,success FROM flyway_schema_history ORDER BY installed_rank' | \
 python3 -c 'import sys;from scripts.verify_flyway_postgres import validate_history;rows=[(v,s=="t") for v,s in (line.rstrip("\n").split("|") for line in sys.stdin)];print("PASS actual ordered Flyway migrations:",validate_history(rows))'
}
run_step 'full-test prerequisites' preflight
run_step 'Docker Compose configuration' docker compose config -q
run_step 'service image build and startup' docker compose up --build -d
run_step 'API, web, Keycloak and ML readiness' wait_for_services
run_step 'non-root media worker imports' docker compose exec -T video-worker python -c 'import worker, pipeline, caption_packaging, timed_text, audio_packaging, preview_artwork'
run_step 'ordered Flyway V1-V25 history' verify_migrations
run_step 'read-only API smoke' python3 scripts/live_smoke.py --output build-evidence/live-smoke-latest.json
run_step 'unauthenticated security smoke' bash scripts/security_smoke_live.sh
if [[ "$MODE" == --full ]]; then
 echo 'Running original-asset browser ingestion-to-playback E2E on the confirmed disposable stack.'
 run_step 'browser identity, owned profile picker, locale and player mounting' python3 scripts/e2e_browser_identity_live.py
 run_step 'creator browser upload, captions, worker and rights publication' python3 scripts/e2e_processing_live.py
 run_step 'browser decoded playback, two-account isolation and revoked media' python3 scripts/e2e_playback_live.py
fi
printf 'PASS local %s validation; logs: %s\n' "$MODE" "$log"
