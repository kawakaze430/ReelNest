#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
javac -d "$TMP" "$ROOT/apps/api/src/main/java/com/reelnest/security/TokenClaimsPolicy.java" "$ROOT/tests/release/TokenClaimsPolicySmoke.java"
java -cp "$TMP" TokenClaimsPolicySmoke
