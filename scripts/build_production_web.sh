#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
: "${NEXT_PUBLIC_API_BASE_URL:?Define your HTTPS API origin}"
: "${NEXT_PUBLIC_AUTHORITY:?Define your HTTPS OIDC realm}"
: "${NEXT_PUBLIC_APP_ORIGIN:?Define your HTTPS application origin}"
: "${NEXT_PUBLIC_OIDC_CLIENT_ID:?Define your OIDC client ID}"
: "${REELNEST_WEB_IMAGE_TAG:?Define your immutable-tag destination}"
node "$ROOT/apps/web/scripts/validate-public-env.mjs"
command -v docker >/dev/null || { echo 'Docker required for production images' >&2; exit 2; }
docker build --target production \
  --build-arg "NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL" \
  --build-arg "NEXT_PUBLIC_AUTHORITY=$NEXT_PUBLIC_AUTHORITY" \
  --build-arg "NEXT_PUBLIC_APP_ORIGIN=$NEXT_PUBLIC_APP_ORIGIN" \
  --build-arg "NEXT_PUBLIC_OIDC_CLIENT_ID=$NEXT_PUBLIC_OIDC_CLIENT_ID" \
  -t "$REELNEST_WEB_IMAGE_TAG" "$ROOT/apps/web"
echo "Built $REELNEST_WEB_IMAGE_TAG — smoke-test and scan before pushing."
