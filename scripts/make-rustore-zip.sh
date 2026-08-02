#!/usr/bin/env bash
# Простой мастер: создаёт pepk_out.zip для RuStore.
# Вам нужен только длинный ключ из консоли (encryptionkey).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
if [[ -d "$JAVA_HOME/bin" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

OUT_DIR="$ROOT/dist/rustore-signing"
mkdir -p "$OUT_DIR"

# pepk: из Downloads или скачать
if [[ -f "$HOME/Downloads/pepk.jar" ]]; then
  PEPK="$HOME/Downloads/pepk.jar"
elif [[ -f "$OUT_DIR/pepk.jar" ]]; then
  PEPK="$OUT_DIR/pepk.jar"
else
  echo "Скачиваю pepk.jar…"
  curl -fsSL -o "$OUT_DIR/pepk.jar" \
    "https://www.gstatic.com/play-apps-publisher-rapid/signing-tool/prod/pepk.jar"
  PEPK="$OUT_DIR/pepk.jar"
fi

if [[ ! -f keystore.properties ]]; then
  echo "Ошибка: нет keystore.properties в $ROOT"
  exit 1
fi

STORE_FILE="$(grep -E '^STORE_FILE=' keystore.properties | cut -d= -f2- | tr -d '\r')"
KEY_ALIAS="$(grep -E '^KEY_ALIAS=' keystore.properties | cut -d= -f2- | tr -d '\r')"
STORE_PASSWORD="$(grep -E '^STORE_PASSWORD=' keystore.properties | cut -d= -f2- | tr -d '\r')"
KEY_PASSWORD="$(grep -E '^KEY_PASSWORD=' keystore.properties | cut -d= -f2- | tr -d '\r')"

clear 2>/dev/null || true
cat <<'TXT'
╔══════════════════════════════════════════════════════╗
║  RuStore: создать ZIP для подписи приложения         ║
╚══════════════════════════════════════════════════════╝

1. Откройте в браузере консоль RuStore:
   https://console.rustore.ru/

2. Приложение Tomilo → «Подпись» / «Загрузка подписи приложения»

3. Найдите команду, похожую на:
   java -jar pepk.jar --keystore ... --encryptionkey=XXXXX ...

4. Скопируйте ТОЛЬКО то, что после --encryptionkey=
   (длинная строка из букв и цифр, иногда с = на конце)

5. Вставьте её ниже и нажмите Enter.
   (можно с кавычками — скрипт их уберёт)

TXT

if [[ -n "${1:-}" ]]; then
  ENC_KEY="$1"
else
  echo -n "Вставьте encryptionkey и Enter: "
  read -r ENC_KEY
fi

# убрать кавычки/пробелы/переносы
ENC_KEY="$(printf '%s' "$ENC_KEY" | tr -d '\r\n' | sed -E "s/^['\"]//; s/['\"]$//; s/^--encryptionkey=//")"

if [[ ${#ENC_KEY} -lt 20 ]]; then
  echo ""
  echo "Ключ слишком короткий. Нужен полный encryptionkey из консоли RuStore."
  echo "Пример длины: обычно 50–200+ символов."
  exit 1
fi

ZIP_OUT="$OUT_DIR/pepk_out.zip"
rm -f "$ZIP_OUT"

echo ""
echo "Делаю ZIP… (пароли берутся из keystore.properties)"
java -jar "$PEPK" \
  --keystore="$ROOT/$STORE_FILE" \
  --alias="$KEY_ALIAS" \
  --output="$ZIP_OUT" \
  --encryptionkey="$ENC_KEY" \
  --include-cert \
  --keystore-pass="$STORE_PASSWORD" \
  --key-pass="$KEY_PASSWORD"

# PEM на всякий случай
PEM="$OUT_DIR/upload_certificate.pem"
keytool -exportcert -rfc \
  -keystore "$ROOT/$STORE_FILE" \
  -alias "$KEY_ALIAS" \
  -storepass "$STORE_PASSWORD" \
  -keypass "$KEY_PASSWORD" \
  -file "$PEM" >/dev/null 2>&1 || true

echo ""
echo "════════════════════════════════════════════════════"
echo " ГОТОВО. Откройте Finder и загрузите в RuStore:"
echo ""
echo "  1) ZIP (поле «Загрузите созданный ZIP-архив»):"
echo "     $ZIP_OUT"
echo "     размер: $(wc -c < "$ZIP_OUT" | tr -d ' ') байт"
echo ""
echo "  2) PEM (поле «Загрузите сертификат загрузки»):"
echo "     $PEM"
echo ""
echo " AAB/APK в эти поля НЕ кладите."
echo "════════════════════════════════════════════════════"
echo ""
# открыть папку в Finder (macOS)
open "$OUT_DIR" 2>/dev/null || true
