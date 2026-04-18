# Лицензирование: таблицы по ER, операции API, Ticket и ЭЦП

Подсистема реализована в пакете сервиса **`LicenseService`**, контроллер **`LicenseController`**, сущности в **`ru.rkjrth.adboard.entity`**, DTO — **`ru.rkjrth.adboard.dto.license`**.

---

## Соответствие ER и PostgreSQL

Таблицы создаёт Hibernate (`ddl-auto=update`). Связи на пользователей идут на существующую таблицу **`users`** (идентификатор **`BIGINT`**, не UUID как на абстрактной ER — это связка с уже существующей моделью `User`).

| Таблица | Назначение |
|---------|------------|
| `license_products` | Продукт (`name`, `is_blocked`). |
| `license_types` | Тип лицензии: имя, **`default_duration_in_days`**, описание. |
| `license_devices` | Устройство пользователя: `name`, **`mac_address`**, FK **`user_id`** → `users`. |
| `licenses` | Выданная лицензия: уникальный **`code`**, FK на продукт и тип, **`user_id`** (держатель), **`owner_id`**, период **`first_activation_date`** / **`ending_date`**, **`blocked`**, лимит **`device_count`**. |
| `device_license` | Факт активации: связь **`license_id`** ↔ **`device_id`**, **`activation_date`**. |
| `license_history` | Журнал: статусы **`CREATED`**, **`ACTIVATED`**, **`RENEWED`** и др. |

---

## Операции (бизнес-логика)

Логика соответствует типичным диаграммам последовательности «клиент — сервер — БД» без привязки к конкретному файлам UML в репозитории.

1. **Создание лицензии** — генерируется код, строка в `licenses`, запись в `license_history`.
2. **Активация** — проверка кода, пользователя и продукта; при необходимости создание **`license_devices`**; первая активация задаёт **`ending_date`** = сегодня + длительность из типа; связь в **`device_license`**.
3. **Проверка** — по коду и MAC находятся лицензия и устройство; должна существовать активация; формируется **`Ticket`** и **`TicketResponse`** с подписью.
4. **Продление** — сдвигается **`ending_date`** (от текущего окончания или от сегодня, если просрочена); запись в **`license_history`**.

Реализация: класс **`LicenseService`**.

---

## REST API

| Операция | Метод и путь | Авторизация |
|----------|----------------|-------------|
| Каталог продуктов и типов (UUID для создания лицензии) | `GET /api/licenses/catalog` | без JWT |
| Создание | `POST /api/licenses/admin/create` | JWT, роль **ADMIN** |
| Продление | `POST /api/licenses/admin/renew` | JWT, **ADMIN** |
| Активация | `POST /api/licenses/activate` | без JWT |
| Проверка | `POST /api/licenses/check` | без JWT |
| Публичный ключ RSA | `GET /api/licenses/signing-public-key` | без JWT |

Примеры тел запросов см. в коде классов **`CreateLicenseRequest`**, **`ActivateLicenseRequest`**, **`CheckLicenseRequest`**, **`RenewLicenseRequest`**.

Административные вызовы: после **`POST /auth/login`** передать заголовок **`Authorization: Bearer`** с access-токеном в той же строке.

---

## Класс Ticket и TicketResponse

**`Ticket`** (поля для клиента):

- **`serverCurrentAt`** — текущее время сервера (UTC, `Instant`).
- **`ticketLifetimeSeconds`** — допустимая «свежесть» ответа проверки для клиента (не путать с датой истечения лицензии).
- **`licenseActivationDate`** / **`licenseExpirationDate`** — период действия лицензии после активации (`LocalDate`).
- **`userId`** — держатель лицензии (`Long`, как в `users.id`).
- **`deviceId`** — UUID устройства из `license_devices`.
- **`licenseBlocked`** — признак блокировки лицензии в БД на момент проверки.

**`TicketResponse`** содержит **`ticket`** и **`signatureBase64`**.

Подпись: алгоритм **`SHA256withRSA`** по каноническому JSON объекта **`Ticket`** (Jackson, алфавитный порядок полей через **`@JsonPropertyOrder(alphabetic = true)`**). Компонент **`TicketSigner`**; проверка на клиенте — тем же DER SPKI, что выдаёт **`GET /api/licenses/signing-public-key`** (поле **`publicKeyDerBase64`**).

Если не заданы ключи в конфигурации, при старте генерируется временная пара RSA только для разработки (в лог пишется предупреждение).

---

## Конфигурация (`application.properties`)

```properties
license.ticket.lifetime-seconds=300
# PKCS#8 DER, Base64 без PEM-заголовков (опционально для стабильной ЭЦП между перезапусками):
# license.signature.private-key-base64=
# license.signature.public-key-base64=
```

Для продакшена рекомендуется задать ключевую пару через переменные окружения и секреты CI.

---

## Тестирование

Интеграционный сценарий: **`LicenseFlowIntegrationTest`** (создание → активация → проверка подписи → продление).

---

## HTTPS

Шифрование транспорта (**HTTPS**) обеспечивается на стороне прокси или хостинга; сам Spring Boot в репозитории по умолчанию слушает HTTP.
