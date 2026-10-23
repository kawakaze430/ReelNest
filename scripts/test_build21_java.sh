#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
javac -d "$TMP" \
 apps/api/src/main/java/com/reelnest/profiles/ProfilePinPolicy.java \
 apps/api/src/main/java/com/reelnest/admin/CatalogBatchPolicy.java \
 apps/api/src/main/java/com/reelnest/admin/PublicationSchedulePolicy.java \
 apps/api/src/main/java/com/reelnest/support/RefundReviewPolicy.java \
 apps/api/src/main/java/com/reelnest/catalog/SearchTextPolicy.java \
 tests/release/Build21PolicySmoke.java
java -ea -cp "$TMP" Build21PolicySmoke
