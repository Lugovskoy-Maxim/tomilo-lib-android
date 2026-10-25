# Сборка TOMILO LIB

JDK 17+, Android SDK 36.

## Debug

```bash
./gradlew :app:assembleRustoreDebug
```

APK: `app/build/outputs/apk/rustore/debug/`

## Релиз GitHub

Тег `v*` должен совпадать с `versionName` в `app/build.gradle.kts`. Сейчас **1.5.1** (`versionCode 53`).

```bash
git tag v1.5.1
git push origin v1.5.1
```

Либо Actions → **Release**. Подробности: [`.github/RELEASE.md`](.github/RELEASE.md).

В релиз попадает подписанный `tomilo-lib-{version}.apk`.

## RuStore

```bash
./scripts/generate-upload-keystore.sh   # один раз
./scripts/build-rustore.sh
```

Карточка магазина: [`store/rustore/listing.md`](store/rustore/listing.md)

## Google Play

```bash
./scripts/build-release.sh
```

Для Google Play используется package `ru.tomilolib.mobile`; для существующего RuStore
остался `ru.tomilo.lib.mobile`. Один upload keystore можно использовать для обоих.
`keystores/tomilo-upload.jks` и `keystore.properties` в git не класть.

У Play отдельные рекламные блоки РСЯ. Перед сборкой добавьте выданные в кабинете ID
в `~/.gradle/gradle.properties` или задайте их переменными окружения:

```properties
PLAY_YANDEX_REWARDED_AD_UNIT_ID=...
PLAY_YANDEX_REWARDED_AD_UNIT_ID_2=...
PLAY_YANDEX_INTERSTITIAL_AD_UNIT_ID=...
PLAY_YANDEX_NATIVE_CATALOG_AD_UNIT_ID=...
```

Пустые значения отключают соответствующий формат рекламы в Play-сборке. Блоки
RuStore остаются отдельными и не подставляются в Play. GitHub Actions читает те же
значения из одноимённых repository secrets при сборке Play AAB. Без секрета
соответствующий Play-блок останется выключенным.

## Push-уведомления

1. Firebase Console → создать отдельное Android-приложение для Google Play с package `ru.tomilolib.mobile`.
2. Скачать `google-services.json`, положить в `app/google-services.json` (без него плагин не применяется, сборка идёт как раньше — уведомления только через фоновый polling).
3. Project Settings → Service Accounts → Generate new private key → JSON одной строкой в `FIREBASE_SERVICE_ACCOUNT_JSON` на сервере (`server/.env`).

### RuStore Push

1. В RuStore Консоль создать Push-проект и добавить его идентификатор в `keystore.properties` или в переменную окружения сборки:

   ```properties
   RUSTORE_PUSH_PROJECT_ID=ваш-id-проекта
   ```

2. На сервере задать `RUSTORE_PROJECT_ID` и `RUSTORE_SERVICE_TOKEN` из этого же проекта.

Оба транспорта работают независимо: Play-устройства используют FCM, а устройства без Google Play Services могут получить RuStore Push. Если соответствующий SDK или сервис недоступен, приложение сохраняет фоновую сверку через `NotificationsPollWorker`.
