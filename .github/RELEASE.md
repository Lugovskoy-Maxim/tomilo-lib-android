# GitHub Releases

Подписанный APK публикуется в [Releases](https://github.com/Lugovskoy-Maxim/tomilo-lib-android/releases).

## Как выпустить версию

1. Поднимите `versionName` и `versionCode` в `app/build.gradle.kts`.
2. Добавьте `store/rustore/whats-new-{version}-{date}.md` — текст попадёт в описание релиза.
3. Закоммитьте и запушьте `main`.
4. Поставьте тег, совпадающий с `versionName`:

```bash
git tag v1.1.1
git push origin v1.1.1
```

Либо в GitHub Actions запустите workflow **Release** вручную (`workflow_dispatch`). Он соберёт APK и создаст тег `v{versionName}`, если его ещё нет.

## Секреты репозитория

Нужны для подписи тем же upload-ключом, что RuStore / Play:

| Secret            | Значение                                      |
|-------------------|-----------------------------------------------|
| `KEYSTORE_BASE64` | `base64` без переносов от `tomilo-upload.jks` |
| `STORE_PASSWORD`  | пароль хранилища                              |
| `KEY_ALIAS`       | `tomilo-upload`                               |
| `KEY_PASSWORD`    | пароль ключа                                  |

Keystore в git не коммитится. CI debug-сборку подписывать не нужно.

## Артефакты релиза

- `tomilo-lib-{version}.apk` — установка с GitHub
- `tomilo-lib-{version}.sha256` — контрольная сумма
- `mapping-{version}.txt` — mapping R8 для разбора крэшей
