#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="$ROOT/gradle/wrapper/gradle-wrapper.jar"
URL="https://raw.githubusercontent.com/gradle/gradle/v9.3.1/gradle/wrapper/gradle-wrapper.jar"
EXPECTED="b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13"
mkdir -p "$(dirname "$JAR")"
if [ ! -f "$JAR" ]; then
  if command -v curl >/dev/null; then curl -L --fail "$URL" -o "$JAR";
  elif command -v wget >/dev/null; then wget -O "$JAR" "$URL";
  else echo "Necesitas curl o wget" >&2; exit 1; fi
fi
ACTUAL=$(sha256sum "$JAR" | awk '{print $1}')
[ "$ACTUAL" = "$EXPECTED" ] || { echo "Checksum wrapper inválido: $ACTUAL" >&2; exit 1; }
exec "$ROOT/gradlew" "$@"
