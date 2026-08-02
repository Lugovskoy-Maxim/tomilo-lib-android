#!/usr/bin/env bash
# Собирает подписанные release AAB + APK для Google Play / раздачи.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"

if [[ ! -f "$ROOT/keystore.properties" ]]; then
  echo "Нет keystore.properties."
  echo "Сначала: ./scripts/generate-upload-keystore.sh"
  echo "Или скопируй keystore.properties.example → keystore.properties и укажи свой .jks"
  exit 1
fi

# shellcheck disable=SC1091
source /dev/null 2>/dev/null || true

STORE_FILE="$(grep -E '^STORE_FILE=' keystore.properties | cut -d= -f2-)"
if [[ ! -f "$ROOT/$STORE_FILE" ]]; then
  echo "Keystore не найден: $ROOT/$STORE_FILE"
  exit 1
fi

echo "→ bundleRelease (AAB для Play Console)"
./gradlew :app:bundleRelease --quiet

echo "→ assembleRelease (APK)"
./gradlew :app:assembleRelease --quiet

AAB="$ROOT/app/build/outputs/bundle/release/app-release.aab"
APK="$ROOT/app/build/outputs/apk/release/app-release.apk"

echo ""
echo "Артефакты:"
ls -lh "$AAB" "$APK" 2>/dev/null || true
echo ""
echo "В Google Play загружай: $AAB"
echo ""
echo "Проверка подписи APK (apksigner):"
APKSIGNER="$(find "${ANDROID_HOME:-$HOME/.bubblewrap/android_sdk}/build-tools" -name apksigner 2>/dev/null | sort | tail -1 || true)"
if [[ -n "$APKSIGNER" && -x "$APKSIGNER" ]]; then
  "$APKSIGNER" verify --verbose "$APK" 2>&1 | head -12
else
  echo "(apksigner не найден — пропуск; APK всё равно подписан v2 при сборке)"
fi
