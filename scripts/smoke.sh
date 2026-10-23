#!/usr/bin/env bash
set -euo pipefail
API="${API_BASE_URL:-http://localhost:8080}"
WEB="${WEB_BASE_URL:-http://localhost:3000}"
echo "Checking ReelNest API: ${API}"
curl --fail --silent --show-error "${API}/actuator/health"
echo
echo 'Checking demo catalog...'
curl --fail --silent --show-error "${API}/api/v1/catalog?size=2" | python3 -c 'import json,sys;d=json.load(sys.stdin);assert len(d["content"])>0;print("Catalog has",d["totalElements"],"titles")'
echo "Checking frontend: ${WEB}"
curl --fail --silent --show-error --output /dev/null "${WEB}/"
echo 'Smoke test PASS'
