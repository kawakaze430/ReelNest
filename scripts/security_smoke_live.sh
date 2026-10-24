#!/usr/bin/env bash
# Safe against LOCAL / staging only. No DDoS, no brute-force, no payment creation.
set -euo pipefail
API="${API_BASE_URL:-http://localhost:8080}"
python3 - "$API" <<'PY'
import sys
from urllib.parse import urlsplit
p=urlsplit(sys.argv[1])
local=p.scheme=='http' and p.hostname in ('localhost','127.0.0.1','::1')
staging=p.scheme=='https' and p.hostname and p.hostname.startswith('staging.')
if not (local or staging) or p.username or p.password or p.path not in ('','/') or p.query or p.fragment:
    raise SystemExit('Refusing non-local, non-staging API origin')
PY
for endpoint in \
  /api/v1/privacy/export \
  /api/v1/privacy/requests \
  /api/v1/billing/me \
  /api/v1/playback/continue-watching \
  /api/v1/discovery/home \
  /api/v1/admin/analytics; do
  status=$(curl --max-time 10 --connect-timeout 5 --silent --show-error --output /dev/null --write-out '%{http_code}' "$API$endpoint")
  if [[ "$status" != "401" ]]; then echo "Expected 401 but received $status for $endpoint" >&2; exit 1; fi
  echo "PASS unauthenticated $endpoint -> 401"
done
header=$(curl --max-time 10 --connect-timeout 5 -sSI "$API/actuator/health" | tr -d '\r' | grep -i '^x-content-type-options:' || :)
[[ "$header" == *nosniff* ]] || { echo 'Missing anti-sniff header' >&2; exit 1; }
echo 'PASS live security smoke checks'
