#!/usr/bin/env bash
# Build 12 — non-destructive PostgreSQL logical-backup/restore smoke test.
# Creates a new scratch DB, tests row counts and Flyway schema, then deletes ONLY scratch DB.
set -Eeuo pipefail
cd "$(dirname "$0")/.."
command -v docker >/dev/null || { echo 'Docker required' >&2; exit 3; }
docker compose exec -T postgres pg_isready -U reelnest -d reelnest >/dev/null
DB="reelnest_restore_check_$(date +%s)"
if ! [[ "$DB" =~ ^reelnest_restore_check_[0-9]+$ ]]; then exit 2;fi
ARCHIVE=$(mktemp /tmp/reelnest-logical-backup-XXXXXXXX.dump)
trap 'docker compose exec -T postgres dropdb --if-exists -U reelnest "$DB" 2>/dev/null || :; rm -f "$ARCHIVE"' EXIT
# The archive contains real development records. Never share it; keep temporary files private.
umask 077
docker compose exec -T postgres pg_dump -U reelnest -Fc reelnest > "$ARCHIVE"
docker compose exec -T postgres createdb -U reelnest "$DB"
cat "$ARCHIVE" | docker compose exec -T postgres pg_restore -U reelnest --no-owner -d "$DB"
LIVE=$(docker compose exec -T postgres psql -U reelnest -d reelnest -Atqc 'SELECT COUNT(*) FROM flyway_schema_history')
COPY=$(docker compose exec -T postgres psql -U reelnest -d "$DB" -Atqc 'SELECT COUNT(*) FROM flyway_schema_history')
[[ "$LIVE" == "$COPY" ]] || { echo "Flyway mismatch: source=$LIVE restore=$COPY" >&2; exit 1; }
for table in users titles profiles privacy_requests; do
 LIVE=$(docker compose exec -T postgres psql -U reelnest -d reelnest -Atqc "SELECT COUNT(*) FROM $table")
 COPY=$(docker compose exec -T postgres psql -U reelnest -d "$DB" -Atqc "SELECT COUNT(*) FROM $table")
 [[ "$LIVE" == "$COPY" ]] || { echo "Mismatch $table: $LIVE vs $COPY" >&2;exit 1; }
done
printf 'PASS isolated logical restore: migrated schema and selected row counts match\n'
