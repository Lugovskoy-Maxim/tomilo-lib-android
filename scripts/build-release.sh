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

echo "→ bundlePlayRelease (AAB для Play Console)"
./gradlew :app:bundlePlayRelease --quiet

echo "→ assemblePlayRelease (APK)"
./gradlew :app:assemblePlayRelease --quiet

AAB="$(find "$ROOT/app/build/outputs/bundle/playRelease" -name '*.aab' 2>/dev/null | head -1 || true)"
APK="$(find "$ROOT/app/build/outputs/apk/play/release" -name '*.apk' ! -name '*.apk.idsig' 2>/dev/null | head -1 || true)"

echo ""
echo "Артефакты:"
ls -lh "$AAB" "$APK" 2>/dev/null || true
echo ""
echo "В Google Play загружай AAB: $AAB"
echo "Для RuStore используй: ./scripts/build-rustore.sh"
echo ""
echo "Проверка подписи APK (apksigner):"
APKSIGNER="$(find "${ANDROID_HOME:-$HOME/.bubblewrap/android_sdk}/build-tools" -name apksigner 2>/dev/null | sort | tail -1 || true)"
if [[ -n "$APKSIGNER" && -x "$APKSIGNER" ]]; then
  "$APKSIGNER" verify --verbose "$APK" 2>&1 | head -12
else
  echo "(apksigner не найден — пропуск; APK всё равно подписан v2 при сборке)"
fi
