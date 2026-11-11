# AUDIT — tomilo-lib-android

Дата: 2026-11-10 (Europe/Moscow). Область: исходники `app/`, сборка, публичный API `tomilo-lib.ru`, сравнение с сайтом.

## Стек

| Слой | Реализация |
|---|---|
| Язык | Kotlin 2.0.21, JVM 17, AGP 8.10.1, minSdk 26, target/compileSdk 36 |
| UI | Jetpack Compose + Material3 |
| Архитектура | Manual service locator (`AppContainer` / `TomiloApp`), экраны с локальным state без ViewModel |
| Сеть | Retrofit 2.11 + OkHttp 4.12 + kotlinx.serialization |
| БД | Room 2.6.1 (офлайн-главы), DataStore Preferences (сессия/тема/чтение) |
| DI | Нет Hilt/Koin — `AppContainer` |
| Картинки | Coil 2.7 + отдельный media OkHttp-клиент |
| Фон | WorkManager (уведомления, докачка, обновления), FGS загрузок |
| Релиз | R8 + shrinkResources уже включены |

Версия в дереве: **1.5.2** (`versionCode` 54).

## Что уже есть (сайт ↔ приложение)

| Функция сайта | В приложении |
|---|---|
| Каталог + фильтры (жанры, тип, статус, год, возраст) | Есть (`CatalogScreen`) |
| Поиск / autocomplete | Есть |
| Закладки (Читаю / В планах / Прочитано / Избранное / Брошено) + группы | Есть |
| История и продолжение с последней страницы | Есть |
| Комментарии и рейтинги | Есть |
| Авторизация (email, Яндекс, VK) + синхронизация | Есть (регистрация/сброс пароля — через сайт) |
| Уведомления о новых главах | Есть (FCM / RuStore Push / polling) |
| Режимы чтения (вертикаль / страницы / RTL), тема, офлайн, настройки читалки | Есть |
| Топ тайтлов (день/неделя/месяц) | Нет нативно (на сайте `/top`) |
| Подборки / коллекции | Только WebView/браузер из хаба |
| Новости / гайды / FAQ | Только браузер |
| Регистрация / forgot-password / avatar upload / privacy settings | API есть, в клиенте нет UI |

## Таблица проблем

| Проблема | Место | Серьёзность | Решение |
|---|---|---|---|
| Холодный старт блокирует main thread через `runBlocking` (encrypt + hydrate токенов) | `TomiloApp.kt` | Высокая | Асинхронный hydrate + короткий readiness-gate до первого API; не блокировать `Application.onCreate` |
| Ошибки ленты иногда показывают сырой `Throwable.message` | `HomeScreen.kt` | Средняя | Всегда `toUserFacingError(...)` |
| Ошибки входа показывают сырой message | `LoginScreen.kt` | Средняя | `toUserFacingError` |
| `isDownloaded` считает неполную главу «скачанной» | `OfflineRepository.kt` | Средняя | Считать скачанной только `complete == true` |
| Гигантские Compose-экраны без ViewModel — риск потери UI-state при process death | `ReaderScreen`, `CatalogScreen`, … | Средняя | Не переписывать архитектуру целиком; точечно saveable для критичного UI |
| Бюджет polling (уведомления + закладки + чаты) | `NotificationsPollWorker` | Средняя | Разнести/урезать частоту, уважать 429 (частично сделано) |
| Подборки/топ тайтлов открываются только как сайт | Hub / отсутствие экранов | Средняя | Нативные экраны поверх `/titles/collections` и `/titles/top/{day\|week\|month}` |
| Регистрация / сброс пароля | Auth | Низкая–средняя | API есть; регистрация требует CAPTCHA — нативно добавить forgot-password + deep-link на сайт для регистрации, либо пропустить CAPTCHA-flow |
| Нет instrumentation-тестов | `androidTest` отсутствует | Средняя | Добавлять точечно на регрессии; unit-тесты уже покрывают helpers |
| Двойной disk cache изображений (Coil 250 MB + OkHttp media 200 MB) | `TomiloApp` / `NetworkModule` | Низкая | Снизить Coil disk cache, документировать бюджет |
| Визуальный/доступность бэклог | `todo.md` | Низкая | Постепенная доводка вне критического пути |

## Уже закрыто ранее (не повторять)

Из `docs/reader-page-load-audit.md` и `todo.md` в текущем коде уже есть:

- плитки ~1 экран (`maxTileHeightFor`), спиннер сверху, fallback в Coil (`PageDecodeFallback`);
- отмена параллельных `loadChapter` через `loadJob`/`loadToken`;
- честные дыры офлайн-главы (`readLocalChapter` / `offlineIncomplete`);
- докачка без повторного списания ad-credit;
- R8/ProGuard + resource shrink в release;
- HTTP cache isolation для JWT, coalescing обложек/каталога, таймауты media 8 с.

## План работ по этапам

1. **Аудит** — этот файл.
2. **Баги** — cold start, user-facing errors, `isDownloaded`.
3. **Оптимизация** — отложенный старт фоновых задач, бюджет кэша Coil, проверка сборки.
4. **Функции сайта** — топ тайтлов + подборки; registration/CAPTCHA — отметить ограничения.
5. **Новые функции** — большинство уже есть; точечно усилить UX (повтор скачивания неполной главы, навигация на топ/подборки).
