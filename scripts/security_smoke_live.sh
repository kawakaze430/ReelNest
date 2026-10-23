#!/usr/bin/env bash
# Safe against LOCAL / staging only. No DDoS, no brute-force, no payment creation.
set -euo pipefail
API="${API_BASE_URL:-http://localhost:8080}"
case "$API" in
  http://localhost:*|http://127.0.0.1:*|https://staging.*) ;;
  *) echo "Refusing non-local, non-staging target $API" >&2; exit 2;;
esac
for endpoint in \
  /api/v1/privacy/export \
  /api/v1/privacy/requests \
  /api/v1/billing/me \
  /api/v1/playback/continue-watching \
  /api/v1/discovery/home \
  /api/v1/admin/analytics; do
  status=$(curl --silent --output /dev/null --write-out '%{http_code}' "$API$endpoint")
  if [[ "$status" != "401" ]]; then echo "Expected 401 but received $status for $endpoint" >&2; exit 1; fi
  echo "PASS unauthenticated $endpoint -> 401"
done
header=$(curl -sSI "$API/actuator/health" | tr -d '\r' | grep -i '^x-content-type-options:' || :)
[[ "$header" == *nosniff* ]] || { echo 'Missing anti-sniff header' >&2; exit 1; }
echo 'PASS live security smoke checks'
