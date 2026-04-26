-- Полная очистка демо-данных PostgreSQL (локальный docker-compose).
-- После выполнения перезапустите приложение: поднимутся снова admin (DataInitializer),
-- продукт и тип лицензии (LicenseMetadataBootstrap).
--
-- Запуск из корня репозитория (PowerShell):
--   Get-Content .\scripts\reset-postgres-demo.sql -Raw | docker compose exec -T postgres psql -U postgres -d adboard
--
-- bash:
--   docker compose exec -T postgres psql -U postgres -d adboard < scripts/reset-postgres-demo.sql

BEGIN;

TRUNCATE TABLE
    device_license,
    license_history,
    license,
    device,
    signatures_audit,
    signatures_history,
    signatures,
    user_sessions,
    users,
    product,
    license_type
RESTART IDENTITY CASCADE;

COMMIT;
