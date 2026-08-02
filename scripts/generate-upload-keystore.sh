#!/usr/bin/env bash
# Создаёт upload-keystore для Google Play (applicationId: ru.tomilo.lib.mobile).
# Запускать один раз. Бэкапь .jks и keystore.properties в надёжное место.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KEYSTORES="$ROOT/keystores"
PROPS="$ROOT/keystore.properties"
JKS="$KEYSTORES/tomilo-upload.jks"
ALIAS="tomilo-upload"

if [[ -f "$JKS" ]]; then
  echo "Keystore уже есть: $JKS"
  echo "Не перезаписываем. Удали вручную, если нужен новый (осторожно: Play не примет другой upload key без reset)."
  exit 1
fi

mkdir -p "$KEYSTORES"

# Пароли: из env или случайные
STORE_PASSWORD="${STORE_PASSWORD:-$(openssl rand -base64 24 | tr -d '/+=' | head -c 24)}"
KEY_PASSWORD="${KEY_PASSWORD:-$STORE_PASSWORD}"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"

keytool -genkeypair \
  -v \
  -storetype JKS \
  -keystore "$JKS" \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass "$STORE_PASSWORD" \
  -keypass "$KEY_PASSWORD" \
  -dname "CN=Tomilo Mobile, OU=Mobile, O=Tomilo Lib, L=Moscow, ST=Moscow, C=RU"

cat > "$PROPS" <<EOF
STORE_FILE=keystores/tomilo-upload.jks
STORE_PASSWORD=$STORE_PASSWORD
KEY_ALIAS=$ALIAS
KEY_PASSWORD=$KEY_PASSWORD
EOF

chmod 600 "$PROPS" "$JKS"

echo ""
echo "Готово."
echo "  Keystore: $JKS"
echo "  Config:   $PROPS"
echo ""
echo "Сохрани keystore.properties и .jks в менеджере паролей / офлайн-бэкапе."
echo "Без этого нельзя обновлять приложение в Play с тем же ключом."
echo ""
echo "Сборка AAB:"
echo "  ./scripts/build-release.sh"
