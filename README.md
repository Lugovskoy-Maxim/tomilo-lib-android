# Tomilo Android

Самостоятельное нативное Android-приложение для [tomilo-lib.ru](https://tomilo-lib.ru).

**Стек:** Kotlin · Jetpack Compose · Material 3 · Retrofit · Room · DataStore · Coil  

**Принципы:** быстрое, лёгкое, минималистичное. Те же production API, что и у сайта. Офлайн-чтение глав — только для Premium.

## Возможности (v0.1)

| Экран | Что делает |
|--------|------------|
| Главная | Лента обновлений + популярное |
| Поиск | Autocomplete/search API, точные имена выше за счёт серверного ранга |
| Тайтл | Карточка, список глав, онлайн-чтение |
| Читалка | Вертикальный скролл страниц (CDN) |
| Профиль | Вход по email/паролю (JWT), статус Premium |
| Офлайн | Скачивание глав (Premium), локальная библиотека, чтение без сети |

## API

Базовый URL: `https://tomilo-lib.ru/api/`

- `POST /auth/login`
- `GET /users/profile`
- `GET /titles/latest-updates`, `/titles/popular`, `/titles/{id}`, `/titles/slug/{slug}`
- `GET /chapters/title/{titleId}`, `/chapters/{id}`
- `GET /search/autocomplete`, `/search`

Медиа: `https://cdn.tomilo-lib.ru` (+ нормализация путей `/uploads`, S3 → CDN).

## Premium / offline

1. Пользователь входит в аккаунт.
2. `subscriptionExpiresAt` в будущем → Premium активен.
3. На экране тайтла — иконка загрузки у главы.
4. Глава и страницы сохраняются в **app-private storage** (`files/offline/...`) + Room-индекс.
5. Без Premium скачивание отклоняется с понятным сообщением.

## Сборка

Требования: JDK 17+, Android SDK 35, Android Studio Ladybug+ или CLI.

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

Release:

```bash
./gradlew :app:assembleRelease
```

(нужен signing config — по умолчанию debug-keystore для debug-сборки).

## Структура

```
app/src/main/java/ru/tomilo/lib/mobile/
  core/          # MediaUrl, Premium
  data/api/      # Retrofit + DTO
  data/local/    # DataStore auth, Room offline
  data/repo/     # Auth, Catalog, Offline
  ui/            # Compose screens + theme
```

Package: `ru.tomilo.lib.mobile`  
ApplicationId: `ru.tomilo.lib.mobile`

## Что не входит в v0.1 (осознанно)

Комментарии, игры, магазин, OAuth VK/Yandex, пуши, закладки/история — чтобы держать APK лёгким. Можно нарастить поверх того же API.

## Лицензия

Приватный клиент к TOMILO LIB. Не публиковать чужой контент вне правил сервиса.
