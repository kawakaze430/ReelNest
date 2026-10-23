#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIR="$(mktemp -d)"
trap 'rm -rf "$DIR"' EXIT
javac -d "$DIR" "$ROOT/apps/api/src/main/java/com/reelnest/security/DeploymentPolicy.java" "$ROOT/tests/release/DeploymentPolicyCheck.java"
java -cp "$DIR" DeploymentPolicyCheck
