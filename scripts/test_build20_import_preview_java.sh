#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
javac -d "$TMP" apps/api/src/main/java/com/reelnest/admin/CatalogImportPreview.java tests/release/CatalogImportPreviewSmoke.java
java -ea -cp "$TMP" CatalogImportPreviewSmoke
