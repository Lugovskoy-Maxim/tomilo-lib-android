# TOMILO LIB 1.4.6 / 51

Локальный RuStore-релиз, 11 октября 2026.

Исправлены отмена редактирования категории, возврат из настроек профиля и со страницы книги. Добавлены управление порядком категорий и удаление с подтверждением, компактные переключатели, Dragon-тема, переходы профиля и ссылка регистрации и отправка письма восстановления через API. Унифицирована скорость прокрутки, исправлено отключение автоматического перехода глав.

Полный аудит и незавершённые пункты: [FIGMA-MANHWA-2026-10-11.md](FIGMA-MANHWA-2026-10-11.md).

Проверка:

```
ANDROID_HOME=/tmp/tomilo-android-sdk ./gradlew :app:testRustoreDebugUnitTest :app:lintRustoreDebug :app:assembleRustoreRelease :app:bundleRustoreRelease :app:compilePlayDebugKotlin --console=plain
```

Подпись использует существующий ключ проекта. Ключ и конфигурация находятся в ignored-файлах. Устройства для визуальной проверки не подключены. Релиз не опубликован в магазины.

Результат финальной команды: BUILD SUCCESSFUL. Unit tests: {'tests': 116, 'failures': 0, 'errors': 0, 'skipped': 0}. Lint: 0 errors, 91 warnings, 3 hints. Эти предупреждения не объявлены исправленными. Компиляция Play прошла.

APK: `dist/rustore/tomilo-rustore-v1.4.6-51.apk`. AAB: `dist/rustore/tomilo-rustore-v1.4.6-51.aab`. Контрольные суммы: `dist/rustore/SHA256SUMS`. APK проверен apksigner; package `ru.tomilo.lib.mobile`, версия 1.4.6, код 51.
