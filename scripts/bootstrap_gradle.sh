#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$ROOT/gradle/wrapper/gradle-wrapper.jar"
URL="https://raw.githubusercontent.com/gradle/gradle/v9.6.0/gradle/wrapper/gradle-wrapper.jar"
EXPECTED="497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7"
mkdir -p "$(dirname "$JAR")"
if [ ! -f "$JAR" ]; then
  if command -v curl >/dev/null; then curl -L --fail "$URL" -o "$JAR";
  elif command -v wget >/dev/null; then wget -O "$JAR" "$URL";
  else echo "Necesitas curl o wget" >&2; exit 1; fi
fi
ACTUAL=$(sha256sum "$JAR" | awk '{print $1}')
[ "$ACTUAL" = "$EXPECTED" ] || { echo "Checksum wrapper inválido: $ACTUAL" >&2; exit 1; }
exec "$ROOT/gradlew" "$@"
