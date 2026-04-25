# ЗИоВПО — серверная часть (задания 1–3)

Репозиторий серверного приложения на **Spring Boot 3.5** и **Java 17**. Исходники — в каталоге **`adboard/`**.

---

## Задание 1. Подготовка репозитория

### Что требуется по методичке

| Требование | Что сделано в проекте |
|------------|------------------------|
| Git-репозиторий | История коммитов; удалённый репозиторий подключается как **`origin`** |
| Аутентификация: JWT **access** и **refresh** | `JwtTokenProvider`, `AuthTokenService`, `AuthJwtController` — **`POST /auth/login`**, **`POST /auth/refresh`**; сессии refresh в таблице **`user_sessions`** (`UserSession`) |
| Авторизация: роли и правила доступа | Сущность **`User`** с ролями **USER** / **ADMIN**; **`SecurityConfig`** + **`JwtAuthenticationFilter`**; админ-маршруты (**`/api/users/**`**, **`/api/licenses/admin/**`**) только с ролью **ADMIN** |
| Шифрование (HTTPS) | Приложение по умолчанию слушает **HTTP**; **TLS** описывается как слой **reverse-proxy / Ingress** перед сервисом (типичная схема деплоя) |
| Подключение к **PostgreSQL** | `spring.datasource.*` в **`adboard/src/main/resources/application.properties`**, пароль и URL через переменные окружения **`SPRING_DATASOURCE_*`** |
| Переменные и секреты | Шаблон **`.env.example`** в корне; секреты не коммитить. Основное: **`JWT_SECRET`**, **`SPRING_DATASOURCE_PASSWORD`**, для ЭЦП см. ниже (задание 3) |
| Pipeline: шаги **test** и **build** | **`.github/workflows/maven.yml`** — последовательно **`./mvnw test`** и **`./mvnw package -DskipTests`** в каталоге **`adboard/`** |
| Теория **UML** и **ER** | Изучение по курсу; в коде можно приложить диаграммы по требованию преподавателя |

### Быстрый старт

```powershell
cd adboard
.\mvnw.cmd spring-boot:run
```

Linux/macOS: `./mvnw spring-boot:run`. Порт по умолчанию **8082** (`SERVER_PORT`).

Тесты:

```powershell
cd adboard
.\mvnw.cmd test
```

Проверка работы JWT для пользователя с ролью USER: после логина вызвать **`GET /api/ping`** с заголовком **`Authorization: Bearer <access>`**.

### Локальная PostgreSQL и Postman

**База в Docker** (из корня репозитория):

```powershell
docker compose up -d
```

На хосте по умолчанию порт **`5433`** → в **`.env`** или переменных IDE задайте **`SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/adboard`** и **`SPRING_DATASOURCE_PASSWORD=adboard123`** (см. актуальный **`.env.example`**). Шаблон **`.env`** скопируйте из **`.env.example`**.

При пустых таблицах продуктов приложение один раз создаёт демо-продукт и тип лицензии; UUID для запросов можно взять из **`GET /api/licenses/catalog`** (без авторизации).

**Коллекция Postman:** файл **`postman/ZIoVPO-server.postman_collection.json`**. Импорт в Postman → **Import**. В коллекции переменная **`baseUrl`** (`http://localhost:8082`), скрипты сохраняют токены и поля после логина, регистрации пользователя и **`GET /api/licenses/catalog`**. Перед сценарием лицензии выполните вручную или через Collection Runner по порядку: логин админа → регистрация пользователя **`license_holder_demo`** (при конфликте имени смените username в теле запроса) → каталог → создание лицензии → остальное.

---

## Задание 2. Модуль управления лицензиями

### Структура данных (PostgreSQL по ER)

Таблицы создаёт **Hibernate** (`spring.jpa.hibernate.ddl-auto=update`). Связь с пользователями — через **`users.id`** (**BIGINT**, как в текущей модели `User`, не UUID с абстрактной ER).

| Таблица | Назначение |
|---------|------------|
| `license_products` | Продукт (название, блокировка) |
| `license_types` | Тип: длительность по умолчанию (**`default_duration_in_days`**), описание |
| `licenses` | Выданная лицензия: код, продукт, тип, держатель (**`user_id`**), владелец (**`owner_id`**), даты, **`device_count`**, **`blocked`** |
| `license_devices` | Устройство: **`mac_address`**, привязка к пользователю |
| `device_license` | Активация лицензии на устройстве (**`activation_date`**) |
| `license_history` | Журнал статусов (**CREATED**, **ACTIVATED**, **RENEWED** и др.) |

Подробнее и нюансы полей — в **`docs/Лицензирование-API-и-БД.md`**.

### Операции (по логике диаграмм последовательности «клиент — сервер — БД»)

Реализация: **`LicenseService`**.

1. **Создание** — генерация уникального кода, запись лицензии и строки истории  
2. **Активация** — проверки кода/пользователя/продукта; при необходимости создание устройства; первая активация задаёт период действия; связь **`device_license`**  
3. **Проверка** — по коду лицензии и MAC устройства; возврат **`Ticket`** и **`TicketResponse`** с подписью (задание 3)  
4. **Продление** — обновление **`ending_date`**, запись в истории  

### REST API лицензирования

| Операция | Запрос | Авторизация |
|----------|--------|-------------|
| Создание | `POST /api/licenses/admin/create` | JWT (**ADMIN**) |
| Продление | `POST /api/licenses/admin/renew` | JWT (**ADMIN**) |
| Активация | `POST /api/licenses/activate` | без JWT |
| Проверка (ответ — тикет + подпись) | `POST /api/licenses/check` | без JWT |

Тела запросов — классы **`CreateLicenseRequest`**, **`ActivateLicenseRequest`**, **`CheckLicenseRequest`**, **`RenewLicenseRequest`** в коде.

### Ticket и TicketResponse

**`Ticket`** содержит: время сервера (**`serverCurrentAt`**), время жизни ответа проверки (**`ticketLifetimeSeconds`**), даты активации и окончания лицензии, **`userId`**, **`deviceId`** (UUID устройства), признак блокировки (**`licenseBlocked`**).

**`TicketResponse`** включает **`ticket`** и **`signatureBase64`** (ЭЦП полезной нагрузки тикета).

Интеграционный тест сценария: **`LicenseFlowIntegrationTest`**.

---

## Задание 3. Электронная цифровая подпись (ЭЦП)

### Хранилище ключей и конфигурация

- Компонент **`TicketSigner`**: алгоритм **`SHA256withRSA`**, канонический JSON **`Ticket`** (поля в алфавитном порядке через **`@JsonPropertyOrder`**).
- Ключевая пара: **RSA**, приватный ключ в формате **PKCS#8 DER, Base64** без PEM-заголовков.
- Если ключи не заданы, при старте генерируется **временная** пара только для разработки (подпись между перезапусками не сохраняется).

Переменные окружения (см. **`application.properties`**):

| Переменная | Назначение |
|------------|------------|
| **`LICENSE_SIGNATURE_PRIVATE_KEY_BASE64`** | Приватный ключ (секрет; в GitHub/GitLab — **Secrets**) |
| **`LICENSE_SIGNATURE_PUBLIC_KEY_BASE64`** | Публичный ключ (можно как **Variable**, если допустимо по политике) |
| **`LICENSE_TICKET_LIFETIME_SECONDS`** | TTL «свежести» тикета проверки (по умолчанию **300**) |

Клиент получает материал для проверки подписи через **`GET /api/licenses/signing-public-key`** (поле **`publicKeyDerBase64`** — DER **SubjectPublicKeyInfo** в Base64).

Детали формата подписи и проверки на клиенте — в **`docs/Лицензирование-API-и-БД.md`**.

---

## Справочно: основные эндпоинты

| Назначение | Метод и путь |
|------------|----------------|
| Проверка access JWT | `GET /api/ping` + `Authorization: Bearer …` |
| Логин | `POST /auth/login` |
| Обновление пары токенов | `POST /auth/refresh` |
| Регистрация | `POST /api/auth/register` |
| Служебная информация | `GET /info` |

---

## Что намеренно отсутствует в коде

Удалены сущности и API учебной «онлайн-доски объявлений»: **Category**, **Listing**, **Message**, **Report** и связанный код — в соответствии с формулировкой переноса только инфраструктуры безопасности и БД без лишних предметных сущностей.
