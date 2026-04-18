# ЗИоВПО — серверная часть

Spring Boot **3.5**, Java **17**. Модуль приложения: каталог **`adboard/`** (Maven).

## Что внутри

| Область | Описание |
|---------|----------|
| Аутентификация | JWT **access** / **refresh**, таблица `user_sessions` |
| Авторизация | Роли **USER** / **ADMIN**, правила в `SecurityConfig` |
| БД | **PostgreSQL** (локально или через `docker-compose`) |
| Лицензирование | Таблицы по ER, API создания/активации/проверки/продления, **Ticket** и ЭЦП — см. **`docs/Лицензирование-API-и-БД.md`** |
| CI | **`.github/workflows/maven.yml`** — шаги **Test** и **Build** |

Предметная модель «доски объявлений» из учебного репозитория **не включена** — только инфраструктура безопасности, пользователи и модуль лицензий.

## Требования

- JDK 17+
- PostgreSQL (или контейнер из `docker-compose.yml`)

## Запуск

```powershell
cd adboard
.\mvnw.cmd spring-boot:run
```

Linux/macOS: `./mvnw spring-boot:run`

Порт по умолчанию: **8082**. Переопределение: переменная **`SERVER_PORT`**.

Скопируйте **`.env.example`** в **`.env`** в корне репозитория и задайте **`SPRING_DATASOURCE_PASSWORD`**, **`JWT_SECRET`** (≥ 32 символов в проде), при необходимости ключи **`LICENSE_SIGNATURE_*`** (см. файл).

## Полезные HTTP-пути

| Назначение | Метод и путь |
|------------|----------------|
| Проверка access JWT (любая роль) | `GET /api/ping` + заголовок `Authorization: Bearer …` |
| Вход | `POST /auth/login` |
| Новая пара токенов | `POST /auth/refresh` |
| Регистрация | `POST /api/auth/register` |
| Инфо | `GET /info` |
| Публичный ключ для проверки ЭЦП | `GET /api/licenses/signing-public-key` |

Подробности по лицензиям — в **`docs/`**.

## Тесты

```powershell
cd adboard
.\mvnw.cmd test
```

## HTTPS

Шифрование транспорта на проде: обычно TLS на **reverse-proxy** / Ingress; приложение по умолчанию слушает HTTP.
