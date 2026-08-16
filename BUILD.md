# Сборка TOMILO LIB

JDK 17+, Android SDK 35.

## Debug

```bash
./gradlew :app:assembleRustoreDebug
```

APK: `app/build/outputs/apk/rustore/debug/`

## Релиз GitHub

Тег `v*` должен совпадать с `versionName` в `app/build.gradle.kts`. Сейчас **1.2.1**.

```bash
git tag v1.2.1
git push origin v1.2.1
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
