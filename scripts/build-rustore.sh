#!/usr/bin/env bash
# Сборка подписанной consumer-версии для RuStore (обычные пользователи).
# Артефакты: dist/rustore/ — APK (основной для RuStore) + AAB (опционально).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"

if [[ ! -f "$ROOT/keystore.properties" ]]; then
  echo "Нет keystore.properties."
  echo "Сначала: ./scripts/generate-upload-keystore.sh"
  exit 1
fi

STORE_FILE="$(grep -E '^STORE_FILE=' keystore.properties | cut -d= -f2- | tr -d '\r')"
if [[ ! -f "$ROOT/$STORE_FILE" ]]; then
  echo "Keystore не найден: $ROOT/$STORE_FILE"
  exit 1
fi

OUT_DIR="$ROOT/dist/rustore"
mkdir -p "$OUT_DIR"

echo "→ assembleRustoreRelease (подписанный APK для RuStore)"
./gradlew :app:assembleRustoreRelease --quiet

echo "→ bundleRustoreRelease (AAB, если RuStore примет bundle)"
./gradlew :app:bundleRustoreRelease --quiet

# Gradle / AGP может класть APK с outputFileName или стандартным именем
APK_SRC="$(find "$ROOT/app/build/outputs/apk/rustore/release" -name '*.apk' ! -name '*.apk.idsig' 2>/dev/null | head -1 || true)"
AAB_SRC="$(find "$ROOT/app/build/outputs/bundle/rustoreRelease" -name '*.aab' 2>/dev/null | head -1 || true)"

if [[ -z "$APK_SRC" || ! -f "$APK_SRC" ]]; then
  echo "APK не найден в app/build/outputs/apk/rustore/release"
  ls -la "$ROOT/app/build/outputs/apk/rustore/release" 2>/dev/null || true
  exit 1
fi

VER="$(grep -E 'versionName\s*=' "$ROOT/app/build.gradle.kts" | head -1 | sed -E 's/.*"([^"]+)".*/\1/')"
CODE="$(grep -E 'versionCode\s*=' "$ROOT/app/build.gradle.kts" | head -1 | sed -E 's/[^0-9]//g')"
STAMP="$(date +%Y%m%d)"

APK_DST="$OUT_DIR/tomilo-rustore-v${VER}-${CODE}-${STAMP}.apk"
cp -f "$APK_SRC" "$APK_DST"

if [[ -n "$AAB_SRC" && -f "$AAB_SRC" ]]; then
  AAB_DST="$OUT_DIR/tomilo-rustore-v${VER}-${CODE}-${STAMP}.aab"
  cp -f "$AAB_SRC" "$AAB_DST"
fi

CHECKSUM_DST="$OUT_DIR/tomilo-rustore-v${VER}-${CODE}-${STAMP}.sha256"
(
  cd "$OUT_DIR"
  shasum -a 256 "$(basename "$APK_DST")" > "$(basename "$CHECKSUM_DST")"
  if [[ -n "${AAB_DST:-}" && -f "$(basename "$AAB_DST")" ]]; then
    shasum -a 256 "$(basename "$AAB_DST")" >> "$(basename "$CHECKSUM_DST")"
  fi
)

# Короткий README рядом с билдом
cat > "$OUT_DIR/README.txt" <<EOF
tomilo-lib — сборка для RuStore (обычные пользователи)
======================================================
versionName: $VER
versionCode: $CODE
package:     ru.tomilo.lib.mobile
flavor:      rustore
buildType:   release (minify + shrink + signed)

Файл для загрузки в консоль RuStore:
  $(basename "$APK_DST")

Чеклист публикации:
  1. https://console.rustore.ru/ — приложение Tomilo
  2. Новая версия → загрузить APK
  3. Возраст 16+ (контент 18+ скрыт по умолчанию + age gate)
  4. Политика: https://tomilo-lib.ru (или отдельная /privacy)
  5. Категория: Книги и справочники / Развлечения
  6. Скриншоты 2–8 шт. (телефон)
  7. Краткое и полное описание — store/rustore/listing.md

Важно: тот же keystore, что и для Play — иначе пользователи
не смогут обновляться между магазинами с одним package name.
EOF

echo ""
echo "=== RuStore build ready ==="
ls -lh "$OUT_DIR"
echo ""
echo "Загрузите в RuStore Console:"
echo "  $APK_DST"
echo "  SHA-256: $CHECKSUM_DST"
echo ""

# verify signature if possible
APKSIGNER="$(find "${ANDROID_HOME:-$HOME/Library/Android/sdk}/build-tools" -name apksigner 2>/dev/null | sort | tail -1 || true)"
if [[ -z "$APKSIGNER" ]]; then
  APKSIGNER="$(find "$HOME/.bubblewrap/android_sdk/build-tools" -name apksigner 2>/dev/null | sort | tail -1 || true)"
fi
if [[ -n "$APKSIGNER" && -x "$APKSIGNER" ]]; then
  echo "Проверка подписи:"
  "$APKSIGNER" verify --print-certs "$APK_DST" 2>&1 | head -20
fi
