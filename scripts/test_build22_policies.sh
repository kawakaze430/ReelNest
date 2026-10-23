#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
DIR="$(mktemp -d)"; trap 'rm -rf "$DIR"' EXIT
javac -d "$DIR" \
 apps/api/src/main/java/com/reelnest/notifications/NotificationPolicy.java \
 apps/api/src/main/java/com/reelnest/analytics/AggregatePrivacyPolicy.java \
 apps/api/src/main/java/com/reelnest/creators/RoyaltyEstimatePolicy.java \
 apps/api/src/main/java/com/reelnest/billing/PromotionPolicy.java \
 tests/release/Build22PolicySmoke.java
java -ea -cp "$DIR" Build22PolicySmoke
