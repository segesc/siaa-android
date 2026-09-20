#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${TMPDIR:-/tmp}/siaa-kotlin-smoke"
rm -rf "$OUT"; mkdir -p "$OUT"
MODEL=$(find "$ROOT/core/model/src/main/kotlin" -name '*.kt')
ALGO=$(find "$ROOT/core/algorithm/src/main/kotlin" -name '*.kt')
kotlinc $MODEL $ALGO "$ROOT/tools/algorithm_smoke.kt" -include-runtime -d "$OUT/smoke.jar"
java -jar "$OUT/smoke.jar"
