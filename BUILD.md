# Сборка TOMILO LIB

JDK 17+, Android SDK 35.

## Debug

```bash
./gradlew :app:assembleRustoreDebug
```

APK: `app/build/outputs/apk/rustore/debug/`

## Релиз GitHub

Тег `v*` должен совпадать с `versionName` в `app/build.gradle.kts`. Сейчас **1.2.2**.

```bash
git tag v1.2.2
git push origin v1.2.2
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

Один keystore на RuStore и Play (`applicationId` общий).  
`keystores/tomilo-upload.jks` и `keystore.properties` в git не класть.

## Push-уведомления

1. Firebase Console → создать проект → Android-приложение с package `ru.tomilo.lib.mobile`.
2. Скачать `google-services.json`, положить в `app/google-services.json` (без него плагин не применяется, сборка идёт как раньше — уведомления только через фоновый polling).
3. Project Settings → Service Accounts → Generate new private key → JSON одной строкой в `FIREBASE_SERVICE_ACCOUNT_JSON` на сервере (`server/.env`).

### RuStore Push

1. В RuStore Консоль создать Push-проект и добавить его идентификатор в `keystore.properties` или в переменную окружения сборки:

   ```properties
   RUSTORE_PUSH_PROJECT_ID=ваш-id-проекта
   ```

2. На сервере задать `RUSTORE_PROJECT_ID` и `RUSTORE_SERVICE_TOKEN` из этого же проекта.

Оба транспорта работают независимо: Play-устройства используют FCM, а устройства без Google Play Services могут получить RuStore Push. Если соответствующий SDK или сервис недоступен, приложение сохраняет фоновую сверку через `NotificationsPollWorker`.
