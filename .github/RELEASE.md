# GitHub Releases

Локальная сборка: [`BUILD.md`](../BUILD.md).

Подписанный APK публикуется в [Releases](https://github.com/Lugovskoy-Maxim/tomilo-lib-android/releases).

В релиз попадают только APK и его SHA-256. R8 mapping, keystore и пароли в репозиторий и в Releases не публикуются.

## Как выпустить версию

1. Поднимите `versionName` и `versionCode` в `app/build.gradle.kts`.
2. Добавьте `store/rustore/whats-new-{version}-{date}.md` — текст попадёт в описание релиза.
3. Закоммитьте и запушьте `main`.
4. Поставьте тег, совпадающий с `versionName`:

```bash
git tag v1.2.3
git push origin v1.2.3
```

Либо Actions → **Release** → Run workflow.

## Подпись (не коммитить)

Release-сборка берёт keystore и пароли только из GitHub Actions secrets. Локальные `keystore.properties` и `*.jks` должны оставаться в `.gitignore`.

Нужные секреты репозитория: `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

Mapping после локальной release-сборки лежит в `app/build/outputs/mapping/` (каталог в git не попадает). Его нельзя выкладывать в публичный релиз — по нему снимается обфускация.
