# Очистка всех таблиц приложения в PostgreSQL из docker-compose (корень репозитория).
# После выполнения перезапустите Spring Boot — создадутся admin, продукт и тип лицензии.

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$sql = Join-Path $root "scripts\reset-postgres-demo.sql"

Set-Location $root
Get-Content -Raw $sql | docker compose exec -T postgres psql -U postgres -d adboard
Write-Host "Готово. Перезапустите приложение (adboard)."
