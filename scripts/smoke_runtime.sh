#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${TMPDIR:-/tmp}/siaa-runtime-smoke"
rm -rf "$OUT"; mkdir -p "$OUT"
CP="/root/.sdkman/candidates/kotlin/current/lib/kotlinx-coroutines-core-jvm.jar"
MODEL=$(find "$ROOT/core/model/src/main/kotlin" -name '*.kt')
ALGO=$(find "$ROOT/core/algorithm/src/main/kotlin" -name '*.kt')
RUNTIME=$(find "$ROOT/core/runtime/src/main/kotlin" -name '*.kt')
kotlinc -cp "$CP" $MODEL $ALGO $RUNTIME "$ROOT/tools/runtime_smoke.kt" -include-runtime -d "$OUT/runtime-smoke.jar"
java -cp "$OUT/runtime-smoke.jar:$CP" Runtime_smokeKt
