# tomilo-lib Android

Самостоятельное нативное Android-приложение для [tomilo-lib.ru](https://tomilo-lib.ru).

**Стек:** Kotlin · Jetpack Compose · Material 3 · Retrofit · Room · DataStore · Coil  

**Принципы:** быстрое, лёгкое, минималистичное. Те же production API, что и у сайта. Офлайн-чтение глав — только для Premium.

## Возможности (v1.1)

| Экран | Что делает |
|--------|------------|
| Главная | Продолжение чтения, свежие обновления, популярное и быстрый каталог |
| Поиск и каталог | Быстрый поиск, сортировка, фильтры по типу, статусу и жанрам |
| Тайтл | Карточка, главы, категории закладок, оценка, комментарии и онлайн-чтение |
| Читалка | Вертикальный, постраничный и RTL-режимы, масштаб, яркость, автоскролл, предзагрузка и переходы между главами |
| Закладки | Категории reading / planned / completed / favorites / dropped |
| Обновления | Отдельная пагинируемая лента новых глав с возрастным фильтром |
| Чаты | Личные диалоги, поддержка, админский inbox, ответы, удаление и статусы прочтения |
| Друзья | Список друзей, входящие/исходящие заявки, поиск пользователей и быстрый чат |
| Задания | Ежедневный бонус, прогресс заданий, получение одной или всех наград |
| Колесо судьбы | Серверные призы, обычный и мгновенный спин, cooldown, баланс и лента победителей |
| Лидеры | Топ‑3, категории и периоды рейтинга, профили участников |
| Профили | Публичная статистика, управление дружбой и личный чат |
| Уведомления | Прочтение/удаление, переход по событию + системные уведомления через WorkManager |
| Профиль | Аккаунт, Premium, офлайн, история, друзья, задания, обновления и настройки контента |
| Офлайн | Premium-загрузки, пауза/повтор, контроль места, проверка файлов и локальная история |
| Мир Tomilo | Подборки, новости, гайды, магазин, игры и справочные разделы оригинального сайта |

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

### Debug

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew :app:assembleRustoreDebug
```

APK: `app/build/outputs/apk/rustore/debug/`

### Release — RuStore (обычные пользователи)

Подписанный **consumer**-APK для консоли [RuStore](https://console.rustore.ru/):

```bash
./scripts/generate-upload-keystore.sh   # один раз, если ещё нет keystore
./scripts/build-rustore.sh
```

Артефакты: `dist/rustore/tomilo-rustore-v1.1.2-14-….apk` и `.aab`

Карточка магазина (текст, возраст, чеклист): [`store/rustore/listing.md`](store/rustore/listing.md)

Flavor: `rustore` · package: `ru.tomilo.lib.mobile` · minify/shrink · production API.

### Release — Google Play

```bash
./scripts/build-release.sh
```

AAB: `app/build/outputs/bundle/playRelease/…aab`

**Важно:** сохрани `keystores/tomilo-upload.jks` и `keystore.properties` в офлайн-бэкапе.  
Один и тот же keystore для RuStore и Play (один `applicationId`).

### GitHub Releases

Каждый тег `v*` собирает подписанный APK и публикует его в
[Releases](https://github.com/Lugovskoy-Maxim/tomilo-lib-android/releases).

```bash
# versionName в app/build.gradle.kts должен совпадать с тегом без «v»
git tag v1.1.2
git push origin v1.1.2
```

Либо Actions → **Release** → Run workflow. Подробности: [`.github/RELEASE.md`](.github/RELEASE.md).

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

## Пуши

Сервер сейчас принимает **Web Push** (сайт). В приложении:

1. Список уведомлений `GET /notifications`
2. Фоновый polling раз в 15 минут (WorkManager) → локальный system notification при росте unread
3. Запрос `POST_NOTIFICATIONS` на Android 13+

Полноценный FCM потребует доработки бэкенда.

## Версия

Текущий релиз: **1.1.2** (`versionCode 14`). Изменения релиза описаны в
[`store/rustore/whats-new-1.1.2-2026-08-15.md`](store/rustore/whats-new-1.1.2-2026-08-15.md).

## Лицензия

Проприетарное ПО. Все права принадлежат Луговскому Максиму Юрьевичу.

Копирование, изменение, распространение и использование исходного кода
или его частей в других проектах **запрещены** без письменного разрешения.
Официальные сборки из GitHub Releases / RuStore / Google Play можно
устанавливать как конечному пользователю.

Полный текст: [`LICENSE`](LICENSE). Запросы: lugovskou.myu@yandex.ru
