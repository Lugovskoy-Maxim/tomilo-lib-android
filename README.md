# TOMILO LIB

Android-приложение [tomilo-lib.ru](https://tomilo-lib.ru): каталог, читалка, полка, офлайн (Premium).

Стек: Kotlin, Jetpack Compose, Material 3.

## Сборка

JDK 17+, Android SDK 35.

```bash
./gradlew :app:assembleRustoreDebug
```

Релиз в GitHub: тег `v*` = `versionName`. Сейчас **1.2.1**.

```bash
git tag v1.2.1
git push origin v1.2.1
```

Подробности: [`.github/RELEASE.md`](.github/RELEASE.md).  
RuStore / Play: `./scripts/build-rustore.sh` и `./scripts/build-release.sh`.

## Лицензия

Проприетарное ПО. © Луговской Максим Юрьевич.  
Исходники без письменного разрешения копировать нельзя.  
APK из [Releases](https://github.com/Lugovskoy-Maxim/tomilo-lib-android/releases), RuStore и Play ставить можно.

Полный текст: [`LICENSE`](LICENSE) · lugovskou.myu@yandex.ru
