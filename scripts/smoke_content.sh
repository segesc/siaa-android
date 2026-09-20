#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${TMPDIR:-/tmp}/siaa-content-smoke"
rm -rf "$OUT"; mkdir -p "$OUT"
MODEL=$(find "$ROOT/core/model/src/main/kotlin" -name '*.kt')
CONTENT=$(find "$ROOT/core/content/src/main/kotlin" -name '*.kt')
kotlinc $MODEL $CONTENT "$ROOT/tools/content_smoke.kt" -include-runtime -d "$OUT/content-smoke.jar"
java -jar "$OUT/content-smoke.jar"
