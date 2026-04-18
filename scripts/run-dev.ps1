# Loads .env, optional docker-compose Postgres, then mvnw spring-boot:run
# Run from repo root:  powershell -File .\scripts\run-dev.ps1

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot

Set-Location $RepoRoot
Write-Host "Project root: $RepoRoot"

$envFile = Join-Path $RepoRoot ".env"
$example = Join-Path $RepoRoot ".env.example"
if (-not (Test-Path $envFile) -and (Test-Path $example)) {
    Copy-Item $example $envFile
    Write-Host "Created .env from .env.example - set SPRING_DATASOURCE_PASSWORD and SERVER_PORT if needed."
}

if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
            $n = $matches[1]
            $v = $matches[2].Trim()
            if ($v.StartsWith('"') -and $v.EndsWith('"')) { $v = $v.Substring(1, $v.Length - 2) }
            Set-Item -Path "Env:$n" -Value $v
        }
    }
}

$skipDocker = $env:ADBOARD_SKIP_DOCKER_COMPOSE
if ($skipDocker -eq "1" -or $skipDocker -eq "true") { $skipDocker = $true } else { $skipDocker = $false }

if (-not $skipDocker) {
    $pgPass = $env:POSTGRES_PASSWORD
    if (-not $pgPass) { $pgPass = "adboard_local" }

    Write-Host "Starting docker compose (Postgres)..."
    docker compose up -d
    if ($LASTEXITCODE -ne 0) {
        Write-Error "docker compose failed. Is Docker Desktop running?"
        exit 1
    }

    Write-Host "Waiting for Postgres..."
    $deadline = (Get-Date).AddSeconds(45)
    do {
        docker exec adboard-postgres pg_isready -U postgres -d adboard 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { break }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $deadline)

    $hostPort = $env:POSTGRES_HOST_PORT
    if (-not $hostPort) { $hostPort = "5433" }
    $env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:${hostPort}/adboard"
    $env:SPRING_DATASOURCE_USERNAME = "postgres"
    $env:SPRING_DATASOURCE_PASSWORD = $pgPass
} else {
    Write-Host "Skipping docker compose (ADBOARD_SKIP_DOCKER_COMPOSE=1). Using SPRING_DATASOURCE_* from .env"
    if (-not $env:SPRING_DATASOURCE_URL) {
        $env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/adboard"
    }
}

if (-not $env:SPRING_DATASOURCE_USERNAME) { $env:SPRING_DATASOURCE_USERNAME = "postgres" }

if (-not $env:ADBOARD_BOOTSTRAP_ADMIN_USERNAME) { $env:ADBOARD_BOOTSTRAP_ADMIN_USERNAME = "admin" }
if (-not $env:ADBOARD_BOOTSTRAP_ADMIN_PASSWORD) {
    Write-Warning "ADBOARD_BOOTSTRAP_ADMIN_PASSWORD is empty - bootstrap admin will not be created."
}

$port = $env:SERVER_PORT
if (-not $port) { $port = "8082" }

$adboardDir = Join-Path $RepoRoot "adboard"
Set-Location $adboardDir
Write-Host "Starting Spring Boot (SERVER_PORT=$port)..."
& .\mvnw.cmd spring-boot:run
