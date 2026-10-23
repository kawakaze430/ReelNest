#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
javac -d "$TMP" apps/api/src/main/java/com/reelnest/support/CasePolicy.java tests/release/CasePolicySmoke.java
java -ea -cp "$TMP" CasePolicySmoke
