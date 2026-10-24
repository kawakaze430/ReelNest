#!/usr/bin/env bash
# Non-destructive logical-backup/restore: drop only a scratch DB created by this run.
set -Eeuo pipefail
cd "$(dirname "$0")/.."
mkdir -p build-evidence
rm -f build-evidence/restore-latest.json
command -v docker >/dev/null || { echo 'Docker required' >&2; exit 3; }
if [[ "${REELNEST_DISPOSABLE_STACK:-}" != true || ! "${COMPOSE_PROJECT_NAME:-}" =~ ^reelnest-(ci|test)-[a-z0-9][a-z0-9_-]*$ ]]; then
 echo 'BLOCKED: restore test requires a confirmed disposable reelnest-ci-/reelnest-test- project' >&2; exit 3
fi
docker compose exec -T postgres pg_isready -U reelnest -d reelnest >/dev/null
umask 077
scratch_db="reelnest_restore_check_$(python3 -c 'import uuid;print(uuid.uuid4().hex)')"
[[ "$scratch_db" =~ ^reelnest_restore_check_[0-9a-f]{32}$ ]] || exit 2
backup_archive=$(mktemp /tmp/reelnest-logical-backup-XXXXXXXX.dump)
scratch_created=false
restore_verified=false
cleanup() {
 local code=$? cleanup_state=not_created
 trap - EXIT
 if [[ "$scratch_created" == true ]]; then
  cleanup_state=removed
  if ! docker compose exec -T postgres dropdb --if-exists -U reelnest "$scratch_db" >/dev/null; then
   echo 'Failed to remove owned scratch restore database' >&2
   cleanup_state=failed
   [[ "$code" != 0 ]] || code=1
  fi
 fi
 rm -f "$backup_archive"
 python3 - "$code" "$restore_verified" "$cleanup_state" <<'PY'
import datetime as dt,json,sys
from pathlib import Path
code=int(sys.argv[1]);Path('build-evidence/restore-latest.json').write_text(json.dumps({
 'status':'PASS' if code==0 else 'FAIL','exitCode':code,
 'finishedAt':dt.datetime.now(dt.timezone.utc).isoformat(),
 'checksVerified':sys.argv[2]=='true','scratchCleanup':sys.argv[3],
 'checks':'Flyway history and selected application row counts'
},indent=2)+'\n')
PY
 exit "$code"
}
trap cleanup EXIT
# Archives are private temporary files and are always removed, including failures.
docker compose exec -T postgres pg_dump -U reelnest -Fc reelnest > "$backup_archive"
docker compose exec -T postgres createdb -U reelnest "$scratch_db"
scratch_created=true
docker compose exec -T postgres pg_restore -U reelnest --exit-on-error --no-owner -d "$scratch_db" < "$backup_archive"
for table in flyway_schema_history users titles profiles privacy_requests; do
 source_count=$(docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U reelnest -d reelnest -Atqc "SELECT COUNT(*) FROM $table")
 restored_count=$(docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U reelnest -d "$scratch_db" -Atqc "SELECT COUNT(*) FROM $table")
 [[ "$source_count" == "$restored_count" ]] || { echo "Restore row-count mismatch in $table" >&2; exit 1; }
done
restore_verified=true
printf 'PASS isolated logical restore: migrated schema and selected row counts match\n'
