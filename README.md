# ЗИоВПО — серверная часть (задания 1–5)

Репозиторий серверного приложения на **Spring Boot 3.5** и **Java 17**. Исходники — в каталоге **`adboard/`**.

---

## Задание 1. Подготовка репозитория

### Что требуется по методичке

| Требование | Что сделано в проекте |
|------------|------------------------|
| Git-репозиторий | История коммитов; удалённый репозиторий подключается как **`origin`** |
| Аутентификация: JWT **access** и **refresh** | `JwtTokenProvider`, `AuthTokenService`, `AuthJwtController` — **`POST /auth/login`**, **`POST /auth/refresh`**; сессии refresh в таблице **`user_sessions`** (`UserSession`) |
| Авторизация: роли и правила доступа | Сущность **`User`** с ролями **USER** / **ADMIN**; **`SecurityConfig`** + **`JwtAuthenticationFilter`**; админ-маршруты (**`/api/users/**`**, **`/api/licenses/admin/**`**, **`/api/signatures/admin/**`**) только с ролью **ADMIN** |
| Шифрование (HTTPS) | Приложение по умолчанию слушает **HTTP**; **TLS** описывается как слой **reverse-proxy / Ingress** перед сервисом (типичная схема деплоя) |
| Подключение к **PostgreSQL** | `spring.datasource.*` в **`adboard/src/main/resources/application.properties`** |
| Pipeline: шаги **test** и **build** | **`.github/workflows/maven.yml`** — последовательно **`./mvnw test`** и **`./mvnw package -DskipTests`** в каталоге **`adboard/`** |

### Быстрый старт

**База в Docker** (из корня репозитория):

```powershell
docker compose up -d
```

Порт PostgreSQL на хосте по умолчанию: **`54333`** (см. `docker-compose.yml` и переменную `POSTGRES_HOST_PORT`).

Запуск сервера:

```powershell
cd adboard
.\mvnw.cmd spring-boot:run
```

**Коллекция Postman:** файл **`postman/ZIoVPO-server.postman_collection.json`**.

Рекомендуемый порядок: авторизация админа → регистрация пользователя → каталог и выдача лицензии → сигнатуры → **папка «05 - Binary signatures»** для `multipart/mixed`.

### Сброс БД для демонстрации

Полная очистка таблиц приложения (Docker Postgres), затем **перезапуск Spring** — снова создаются учётная запись администратора (`DataInitializer`), продукт и тип лицензии (`LicenseMetadataBootstrap`).

PowerShell из корня репозитория:

```powershell
Get-Content -Path ".\scripts\reset-postgres-demo.sql" -Raw | docker compose exec -T postgres psql -U postgres -d adboard
```

Или:

```powershell
.\scripts\reset-demo-db.ps1
```

---

## Задание 2. Модуль управления лицензиями

Все сущности и таблицы строго соответствуют ER-диаграмме. Первичные ключи (в том числе у пользователей) переведены на **`UUID`**.

| Таблица | Назначение |
|---------|------------|
| `product` | Продукт (название, блокировка) |
| `license_type` | Тип: длительность по умолчанию (**`default_duration_in_days`**), описание |
| `license` | Выданная лицензия: код, продукт, тип, держатель, владелец, даты, лимиты, статус |
| `device` | Устройство: **`mac_address`**, привязка к пользователю |
| `device_license` | Активация лицензии на устройстве (**`activation_date`**) |
| `license_history` | Журнал статусов (**CREATED**, **ACTIVATED**, **RENEWED** и др.) |

Операции реализованы в `LicenseService` (создание, активация, проверка с выдачей `Ticket` + ЭЦП, продление).

---

## Задание 3. Электронная цифровая подпись (ЭЦП)

Архитектура разделена на компоненты в пакете `ru.mfa.signature`:

1. **Key Provider (`SignatureKeyStoreService`)**: загрузка ключей RSA из **`signing.jks`**.
2. **Canonicalization (`JsonCanonicalizer`)**: детерминированный JSON по **RFC 8785** для объектов (`Ticket`, `SignaturePayload` и др.).
3. **Signing Service (`SigningService`)**: **`sign(Object)`** — канонизация + **SHA256withRSA** + Base64; **`signBytes(byte[])`** / **`verifyBytes`** — подпись и проверка **готового байтового документа** (манифест бинарного пакета).

Публичный ключ для проверки на клиенте: **`GET /api/licenses/signing-public-key`**.

---

## Задание 4. Модуль управления антивирусными сигнатурами

Реализованы хранение, инкрементальные обновления, аудит и ЭЦП записей (`MalwareSignatureService`).

### Структура БД

- **`signatures` (`MalwareSignature`)**: текущее состояние, статус `ACTUAL` / `DELETED`, поле **`digitalSignatureBase64`**.
- **`signatures_history`**: версии до обновления или удаления.
- **`signatures_audit`**: кто и что менял.

### Восемь операций JSON API

Полная база (только `ACTUAL`), инкремент (включая `DELETED`), выборка по ID, CRUD админом, история и аудит — см. **`MalwareSignatureController`** (`/api/signatures/**`).

---

## Задание 5. Бинарный API выдачи сигнатур (`multipart/mixed`)

Транспортный слой отделён от CRUD: базовый путь **`/api/binary/signatures`**, доступ с любой аутентификацией (**`SecurityConfig`**).

| Метод | Назначение |
|--------|------------|
| **`GET /api/binary/signatures/full`** | Полная выгрузка только **`ACTUAL`** |
| **`GET /api/binary/signatures/increment?since=...`** | Записи с **`updatedAt > since`**, включая **`DELETED`**; **`since`** — epoch millis (только цифры) или ISO-8601 |
| **`POST /api/binary/signatures/by-ids`** | Тело `{"ids":["uuid",...]}` — только найденные записи |

Ответ: **`multipart/mixed`**, части по порядку **`manifest.bin`**, **`data.bin`** (`MultipartMixedBodyBuilder`). В манифест попадают уже сохранённые подписи записей; **подписывается заново только манифест** (`SigningService.signBytes`).

Формат бинарных файлов: **BigEndian** для чисел; magic UTF-8: **`MF-Парамонов`** / **`DB-Парамонов`** (префикс методички + фамилия). Сборка: **`SignatureBinaryExportService`**, вспомогательные классы в **`ru.rkjrth.adboard.binary`**.

Тесты: **`BinarySignatureExportIntegrationTest`**.

---

## Что намеренно отсутствует в коде

Удалены сущности и API учебной «онлайн-доски объявлений»: **Category**, **Listing**, **Message**, **Report** и связанный код — в соответствии с формулировкой переноса только инфраструктуры безопасности и БД без лишних предметных сущностей.
