#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../apps/web"
node --version
npm --version
npm install --package-lock-only --no-audit --no-fund
npm ci --no-audit --no-fund
npm audit --omit=dev --audit-level=high
npm run typecheck
npm run build
printf '
Review package-lock.json diff and commit only after dependency audit.
'
