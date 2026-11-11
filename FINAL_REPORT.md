# Итоговый отчёт — tomilo-lib-android

Дата: 2026-11-10 (Europe/Moscow). Ветка: `cursor/android-audit-optimize-36db`.

## Вердикт

Приложение уже было зрелым (поиск, фильтры, закладки, история, комментарии, рейтинги, auth, пуши, офлайн, режимы чтения, темы). Сделан свежий аудит, закрыты оставшиеся средние/высокие дефекты старта и ошибок, ускорен discovery-кеш, перенесены нативные **Топ** и **Подборки** с сайта.

Сборка: `:app:assembleRustoreDebug` и `:app:testRustoreDebugUnitTest` — **успешно**.

## Этап 1. Аудит

Файл: [`AUDIT.md`](AUDIT.md).

Стек: Kotlin/Compose, Retrofit/OkHttp, Room + DataStore, Coil, manual `AppContainer`, WorkManager, R8 уже включён.

Главный вывод: большая часть «недостающих» функций сайта уже в клиенте; реальные пробелы — топ тайтлов, нативные подборки, регистрация с CAPTCHA, часть настроек профиля.

## Этап 2. Баги и стабильность

| Что | Как проверить |
|---|---|
| Холодный старт без `runBlocking` на main | После установки cold start: короткий spinner → лента; logcat без долгой блокировки Application |
| Ошибки ленты/логина через `toUserFacingError` | Airplane mode → главная/вход: понятный текст + retry, без stack/HTTP JSON |
| Неполная офлайн-глава не считается «скачанной» | Частично скачанная глава: `isDownloaded=false`, но открывается с баннером |

## Этап 3. Скорость и оптимизация

| Что | Как проверить |
|---|---|
| Auth hydrate + фоновые воркеры после sessionReady | Cold start быстрее до первого кадра; poll/update не стартуют до готовности сессии |
| Coil: memory 20%, disk 150 MB | Меньше давления на RAM/диск рядом с OkHttp media cache 200 MB |
| HTTP cache для `/titles/top/*`, `/titles/collections`, `/collections/{id}` | Повторный вход на Топ/Подборки: быстрее, в debug — cache hit |
| R8/ProGuard + shrinkResources | Уже были в release — без изменений политики |

Читалка: плитки ~1 экран, prefetch, fallback Coil, отмена `loadChapter` — уже были; не ломались.

## Этап 4. Функции с tomilo-lib.ru

**Добавлено**

- Экран **Топ тайтлов** (день/неделя/месяц) — Hub → «Топ тайтлов», deep link `/top`
- Экран **Подборки** + детали — Hub → «Подборки», deep link `/collections`
- Фильтр «Топ недели» на главной берёт серверный `/titles/top/week`

**Уже было (не дублировали)**

Поиск/фильтры, каталог/сортировки, закладки и статусы, история, комментарии, рейтинги, авторизация+синк, уведомления, избранное (категория закладок).

**Пропущено / ограничено API или CAPTCHA**

| Функция | Почему |
|---|---|
| Регистрация / send-registration-code | Нужен Turnstile CAPTCHA — без web-токена нативно нельзя |
| Forgot/reset password | OpenAPI-схемы пустые/неполные; безопаснее deep-link на сайт |
| Avatar upload / privacy settings UI | API есть, UI не переносили в этом проходе |
| Новости / гайды / FAQ | Контентные HTML-страницы — остаются в браузере |

## Этап 5. Новые функции для читателей

Большинство уже реализовано ранее: вертикаль/пейджер/RTL, тёмная/светлая/кофе тема, офлайн-скачивание, яркость/автоскролл/fullscreen, навигация по главам.

**Добавлено сейчас:** кнопка «Докачать» на баннере неполной офлайн-главы.

## Что дальше

1. Нативная регистрация через WebView/Turnstile или server-side mobile CAPTCHA exemption.
2. Forgot-password UI после уточнения контракта API.
3. Privacy/display settings и загрузка аватара.
4. Instrumentation-тесты: auth hydrate, download resume, search race.
5. Замер cold start / reader jank на слабом устройстве.
6. Разнести polling worker и добить process-death для загрузок на API 35–36.

## Коммиты

1. `docs: add AUDIT.md…`
2. `fix: unblock cold start and normalize error/offline checks`
3. `perf: cache top titles and collections discovery GETs`
4. `feat: native top titles and editorial collections`
