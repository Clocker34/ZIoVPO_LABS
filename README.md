# Adboard — учебный проект (Spring Boot)

Кратко о репозитории по этапам **заданий 1–5**: подготовка проекта, REST API, база данных, базовая безопасность, **JWT (access/refresh) и сессии в БД**. Дополнительно: **лицензирование ПО** по ER (таблицы в PostgreSQL, операции создание/активация/проверка/продление, **Ticket** и **ЭЦП**): см. [раздел ниже](#licensing) и файл **`docs/Лицензирование-API-и-БД.md`**.

---

## Задание 1. Подготовка репозитория

### Что сделано

- **Java-проект на Spring Boot** версии **3.5.5** (см. `adboard/pom.xml`, родитель `spring-boot-starter-parent`).
- **Код опубликован на GitHub** — клонируйте репозиторий и открывайте каталог `adboard` как Maven-модуль.
- **Простые контроллеры** для проверки работы Spring MVC:
  - `GET /hello` — текстовый ответ (`HelloController`);
  - `GET /info` — JSON с названием сервиса и версией (`InfoController`).

### Запуск (после клонирования)

```text
cd adboard
.\mvnw.cmd spring-boot:run
```

На Linux/macOS: `./mvnw spring-boot:run`. **Обёртка Maven лежит в каталоге `adboard` внутри репозитория** (не в корне репозитория).

Порт по умолчанию: **8082** (`server.port` в `adboard/src/main/resources/application.properties`). Другой порт: переменная окружения **`SERVER_PORT`** (например `8081`).

**PostgreSQL в Docker на `localhost:5432`** (как контейнер `postgres` в Docker Desktop): скопируйте `.env.example` в `.env`, укажите **`SPRING_DATASOURCE_PASSWORD`**, оставьте **`ADBOARD_SKIP_DOCKER_COMPOSE=1`**, затем из корня репозитория: `.\scripts\run-dev.ps1` — второй контейнер Postgres из `docker-compose.yml` не поднимается.

---

## Задание 2. Работа с REST

### Тема

**Онлайн-доска объявлений (adboard):** пользователи, категории, объявления, переписка по объявлению, жалобы на объявления.

### Сущности и CRUD

Для **каждой** сущности реализованы методы контроллера: **создание**, **получение** (список и по id), **изменение**, **удаление**.

| Сущность | Базовый путь API |
|----------|------------------|
| Пользователь | `/api/users` |
| Категория | `/api/categories` |
| Объявление | `/api/listings` |
| Сообщение | `/api/messages` |
| Жалоба | `/api/reports` |

Дополнительно к заданию 1: эндпоинты `GET /hello`, `GET /info`.

### Что сервис может развивать дальше (идеи)

- Аутентификация и роли (владелец объявления / модератор).
- Поиск и фильтры объявлений (категория, цена, текст).
- Загрузка изображений к объявлениям.
- Уведомления о новых сообщениях.
- Очередь жалоб для модерации и аналитика.

### Postman

Коллекция с парами запросов (CRUD и сценарии): файл **`adboard/adboard-postman-collection.json`**.  
Импорт: Postman → **Import** → выбрать JSON. Переменная **`baseUrl`**: `http://localhost:8082` (или ваш `SERVER_PORT`); при необходимости задайте id сущностей (`userId`, `categoryId`, и т.д.).

---

## Задание 3. Базы данных и бизнес-операции

### База данных

- **Реляционная СУБД: PostgreSQL** (драйвер в `pom.xml`, настройки в `adboard/src/main/resources/application.properties`).
- **Чувствительные данные** (пароль и при необходимости URL/логин) для продакшена задавайте через **переменные окружения**, а не храните в открытом виде в репозитории:
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`

### Таблицы, связи, ограничения

Таблицы соответствуют JPA-сущностям в `adboard/src/main/java/ru/rkjrth/adboard/entity/`. Схема обновляется Hibernate (`spring.jpa.hibernate.ddl-auto=update`).

| Сущность | Связи | Примеры ограничений |
|----------|--------|---------------------|
| **User** | Владелец объявлений, отправитель/получатель сообщений, автор жалоб | Уникальный **email** |
| **Category** | Категория у многих объявлений | Уникальное **name** |
| **Listing** | Принадлежит User и Category | FK на `owner_id`, `category_id`; статусы и даты |
| **Message** | Отправитель, получатель (User), объявление (Listing) | FK на пользователей и объявление |
| **Report** | Автор (User), объявление (Listing) | FK на автора и объявление |

### Тестовые данные

Набор тестовых записей удобно создать **через сценарий в Postman** (создать пользователей → категорию → объявление → сообщения и жалобы). При необходимости для проверки можно добавить SQL-скрипт с `INSERT` или использовать `data.sql` в профиле Spring — в репозитории основной путь проверки — API и коллекция.

### CRUD и БД

Контроллеры из задания 2 **читают и пишут в PostgreSQL** через сервисы и **Spring Data JPA** (репозитории в `repository/`), а не в память.

### Бизнес-операции (не меньше 5)

Реализованы **6** операций с предметной логикой. Операция **поиска/выборки** учитывается, если затрагивает **не менее двух сущностей** (например, сообщения по объявлению).

| Операция | HTTP |
|----------|------|
| Публикация объявления | `POST /api/listings/{id}/publish` |
| Закрытие объявления | `POST /api/listings/{id}/close` |
| Сообщение по объявлению (связь пользователей и лота) | `POST /api/messages?senderId=...&receiverId=...&listingId=...` |
| Переписка по объявлению | `GET /api/messages/listing/{listingId}` |
| Жалоба в рассмотрение | `POST /api/reports/{id}/in-review` |
| Решение жалобы и закрытие объявления **в одной транзакции** | `POST /api/reports/{id}/resolve-and-close-listing` |

Реализация: `ListingService`, `MessageService`, `ReportService` и соответствующие контроллеры.

### Postman (задание 3)

В коллекции **`adboard/adboard-postman-collection.json`** есть **все** запросы: полный CRUD по каждой сущности, примеры тел **JSON**, переменные (`baseUrl`, id), а также бизнес-операции.  
Сценарий «создать данные → выполнить операцию → увидеть результат» описан ниже.

### Сценарий проверки

1. Создать пользователей — `POST /api/users`  
2. Создать категорию — `POST /api/categories`  
3. Создать объявление — `POST /api/listings?ownerId=...&categoryId=...`  
4. Опубликовать — `POST /api/listings/{id}/publish`  
5. Отправить сообщение — `POST /api/messages?...`  
6. Посмотреть переписку — `GET /api/messages/listing/{listingId}`  
7. Подать жалобу — `POST /api/reports?...`  
8. В рассмотрение — `POST /api/reports/{id}/in-review`  
9. Решить жалобу и закрыть лот — `POST /api/reports/{id}/resolve-and-close-listing`, затем `GET` по объявлению и жалобе.

---

## Коротко: тема, сущности, возможности сервиса

| | |
|--|--|
| **Тема** | Онлайн-доска объявлений |
| **Сущности** | User, Category, Listing, Message, Report |
| **Операции** | Полный REST CRUD по пяти сущностям; публикация и закрытие объявления; переписка и просмотр диалога по объявлению; модерация жалоб, в том числе атомарное закрытие жалобы и связанного объявления |

---

## Задание 4. Базовая безопасность API

**Актуальная конфигурация:** на этапе **задания 5** вместо HTTP Basic и cookie-CSRF для REST используется **JWT** (stateless API). Ниже — что остаётся общим с этапом 4; детали входа и защиты запросов — в [задании 5](#assignment-5).

### Spring Security (текущее поведение)

- Подключён **`spring-boot-starter-security`**: **сессии stateless**, **CSRF отключён** (типично для JWT API).
- Аутентификация по **заголовку** `Authorization: Bearer <access JWT>` (фильтр `JwtAuthenticationFilter`).
- Публично без токена: `GET /`, `/hello`, `/info`, **`GET /api/csrf`**, **`POST /api/auth/register`**, **`POST /auth/login`**, **`POST /auth/refresh`**, а также **`POST /api/licenses/activate`**, **`POST /api/licenses/check`**, **`GET /api/licenses/signing-public-key`** (лицензирование).
- Остальные **`/api/**`** (кроме перечисленного) — с валидным **access**-токеном.
- **`/api/users/**`** и **`/api/reports/**`** — роль **ADMIN**.

### Пользователи и пароли

- Учётные записи в **PostgreSQL** (сущность `User`: `username`, `passwordHash` BCrypt, роль `USER` / `ADMIN`).
- **`DatabaseUserDetailsService`** используется для интеграции с Spring Security (в т.ч. при проверке учётных данных при логине).
- **Регистрация:** `POST /api/auth/register` с телом JSON: `username`, `email`, `password`, `name`. Пароль: не короче 8 символов, хотя бы одна цифра и спецсимвол из `!@#$%^&*`.
- **Первый администратор:** переменные `ADBOARD_BOOTSTRAP_ADMIN_USERNAME`, `ADBOARD_BOOTSTRAP_ADMIN_PASSWORD`.
- Администрирование пользователей: `POST/PUT /api/users` с телом **`AdminUserRequest`**.

### Тесты

Профиль **`test`** использует **H2** (`adboard/src/test/resources/application-test.properties`). Для корректной работы **`Instant`** с H2 в тестах задан явный **`Hibernate H2Dialect`** (иначе возможны ошибки при проверке срока refresh-сессии).

Запуск тестов из каталога **`adboard`** (где лежит `mvnw.cmd`):

```text
cd adboard
.\mvnw.cmd test
```

---

<a id="assignment-5"></a>

## Задание 5. JWT Access/Refresh и управление сессиями

### Идея

- **Access JWT** — короткоживущий, передаётся в **`Authorization: Bearer`**, содержит claims (`typ=access`, роль, `uid` и т.д.).
- **Refresh JWT** — долгоживущий, хранит **`jti`**, сопоставляется с записью в таблице **`user_sessions`**; при обмене на новую пару токенов старая сессия помечается **`REPLACED`** (повторное использование старого refresh отклоняется).

### Модель данных

| Элемент | Описание |
|--------|----------|
| Таблица **`user_sessions`** | Сущность `UserSession`: связь с пользователем, **`refresh_jti`**, **`status`** (`ACTIVE` / `REPLACED` / `REVOKED`), срок **`expires_at`**. |
| Репозиторий | `UserSessionRepository` (поиск сессии по `refresh_jti`). |

### Конфигурация (`application.properties`)

| Переменная / свойство | Назначение |
|----------------------|------------|
| **`JWT_SECRET`** (или `jwt.secret`) | Секрет HMAC; **не короче 32 символов** для HS256/совместимых алгоритмов в текущей реализации. |
| **`JWT_ACCESS_MS`** / `jwt.access-expiration-ms` | TTL access-токена (мс). |
| **`JWT_REFRESH_MS`** / `jwt.refresh-expiration-ms` | TTL refresh-токена (мс). |

### API

| Метод | Путь | Тело | Ответ |
|-------|------|------|--------|
| `POST` | **`/auth/login`** | JSON: `username`, `password` | `accessToken`, `refreshToken`, `tokenType` |
| `POST` | **`/auth/refresh`** | JSON: `refreshToken` | новая пара `accessToken` / `refreshToken` |

Защищённые запросы: заголовок `Authorization: Bearer` и строка access-токена.

### Как проверить вручную (сценарий курса)

1. **`POST /auth/login`** — сохранить `accessToken` и `refreshToken`.
2. Запрос к защищённому API, например **`GET /api/categories`**, с заголовком **`Authorization: Bearer`** и access-токеном из шага 1 — ожидается успех (**200**).
3. **`POST /auth/refresh`** с текущим `refreshToken` — ожидается **200** и **новая** пара токенов.
4. Повторить **`POST /auth/refresh`** со **старым** refresh из шага 1 — ожидается **401**, новая пара не выдаётся.
5. В PostgreSQL: **`SELECT * FROM user_sessions`** — у старой сессии статус **`REPLACED`**, у новой — **`ACTIVE`**.

### Автотесты

- `JwtTokenProviderTest` — корректность claims refresh-токена (`jti`).
- `AuthTokenIntegrationTest` — полный сценарий login → доступ по access → refresh → отказ при reuse старого refresh → проверка статусов в БД.

---

<a id="licensing"></a>

## Лицензирование (ER, операции, Ticket и ЭЦП)

Реализованы таблицы **`license_products`**, **`license_types`**, **`license_devices`**, **`licenses`**, **`device_license`**, **`license_history`** и связи с **`users`**. Операции: **создание** и **продление** лицензии (админ, JWT), **активация** на устройстве и **проверка** с выдачей подписанного тикета (публичные эндпоинты без JWT).

| Действие | API |
|----------|-----|
| Создать лицензию | `POST /api/licenses/admin/create` |
| Продлить | `POST /api/licenses/admin/renew` |
| Активировать | `POST /api/licenses/activate` |
| Проверить (ответ: `Ticket` + `signatureBase64`) | `POST /api/licenses/check` |
| Ключ проверки ЭЦП | `GET /api/licenses/signing-public-key` |

Подробности по полям **`Ticket`** / **`TicketResponse`**, подписи RSA и конфигурации — в **`docs/Лицензирование-API-и-БД.md`**. Автотест сценария: **`LicenseFlowIntegrationTest`**.

---

*Java 17, Spring Boot 3.5.5, порт по умолчанию **8082**.*
