#!/usr/bin/env bash
# Экспорт ключей для RuStore «Загрузка подписи приложения» (PEPK + PEM).
#
# В консоли RuStore скопируйте команду с вашим encryptionkey, либо:
#   ./scripts/rustore-pepk-export.sh 'ВАШ_ENCRYPTION_KEY_ИЗ_КОНСОЛИ'
#
# Результат (оба < 100 KB):
#   dist/rustore-signing/pepk_out.zip          ← шаг «Загрузите созданный ZIP-архив»
#   dist/rustore-signing/upload_certificate.pem ← шаг «Загрузите сертификат загрузки»
#
# AAB/APK сюда НЕ загружаются — только в «Новая версия» приложения.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
if [[ -d "$JAVA_HOME" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

OUT_DIR="$ROOT/dist/rustore-signing"
mkdir -p "$OUT_DIR"

PEPK_JAR="$OUT_DIR/pepk.jar"
if [[ ! -f "$PEPK_JAR" ]]; then
  echo "→ Скачиваю pepk.jar…"
  curl -fsSL -o "$PEPK_JAR" \
    "https://www.gstatic.com/play-apps-publisher-rapid/signing-tool/prod/pepk.jar"
fi

if [[ ! -f "$ROOT/keystore.properties" ]]; then
  echo "Нет keystore.properties"
  exit 1
fi

STORE_FILE="$(grep -E '^STORE_FILE=' keystore.properties | cut -d= -f2- | tr -d '\r')"
KEY_ALIAS="$(grep -E '^KEY_ALIAS=' keystore.properties | cut -d= -f2- | tr -d '\r')"
STORE_PASSWORD="$(grep -E '^STORE_PASSWORD=' keystore.properties | cut -d= -f2- | tr -d '\r')"
KEY_PASSWORD="$(grep -E '^KEY_PASSWORD=' keystore.properties | cut -d= -f2- | tr -d '\r')"

if [[ ! -f "$ROOT/$STORE_FILE" ]]; then
  echo "Keystore не найден: $ROOT/$STORE_FILE"
  exit 1
fi

# ── PEM (сертификат загрузки) ────────────────────────────────────
PEM="$OUT_DIR/upload_certificate.pem"
keytool -exportcert -rfc \
  -keystore "$ROOT/$STORE_FILE" \
  -alias "$KEY_ALIAS" \
  -storepass "$STORE_PASSWORD" \
  -keypass "$KEY_PASSWORD" \
  -file "$PEM" >/dev/null
echo "✓ PEM: $PEM ($(wc -c < "$PEM" | tr -d ' ') bytes)"

# ── ZIP через PEPK ───────────────────────────────────────────────
ENC_KEY="${1:-${RUSTORE_ENCRYPTION_KEY:-}}"
if [[ -z "$ENC_KEY" ]]; then
  echo ""
  echo "PEM уже готов. Для ZIP нужен encryptionkey из консоли RuStore:"
  echo "  1) Откройте «Загрузка подписи приложения»"
  echo "  2) Скопируйте длинный encryptionkey из команды (после --encryptionkey=)"
  echo "  3) Запустите:"
  echo "       ./scripts/rustore-pepk-export.sh 'ВСТАВЬТЕ_КЛЮЧ_СЮДА'"
  echo ""
  echo "Или: export RUSTORE_ENCRYPTION_KEY='...' && ./scripts/rustore-pepk-export.sh"
  exit 0
fi

ZIP_OUT="$OUT_DIR/pepk_out.zip"
rm -f "$ZIP_OUT"

echo "→ pepk.jar (export + encrypt private key)…"
java -jar "$PEPK_JAR" \
  --keystore="$ROOT/$STORE_FILE" \
  --alias="$KEY_ALIAS" \
  --output="$ZIP_OUT" \
  --encryptionkey="$ENC_KEY" \
  --include-cert \
  --keystore-pass="$STORE_PASSWORD" \
  --key-pass="$KEY_PASSWORD"

if [[ ! -f "$ZIP_OUT" ]]; then
  echo "PEPK не создал ZIP. Проверьте encryptionkey (должен быть из консоли RuStore, целиком)."
  exit 1
fi

echo "✓ ZIP: $ZIP_OUT ($(wc -c < "$ZIP_OUT" | tr -d ' ') bytes)"
echo ""
echo "=== Что загружать в RuStore (окно подписи, лимит 100 KB) ==="
echo "  ZIP:  $ZIP_OUT"
echo "  PEM:  $PEM"
echo ""
echo "=== AAB/APK — НЕ сюда ==="
echo "  APK/AAB грузятся в разделе «Версии» / «Новая версия», не в форму ключей."
echo "  Файл: dist/rustore/tomilo-rustore-v*.apk"
ls -lh "$ZIP_OUT" "$PEM"
