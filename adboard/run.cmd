@echo off
REM Запуск из cmd. Порт 8082 по умолчанию в application.properties — не конфликтует с занятым 8081.
REM Пароль к PostgreSQL Maven не подставит из .env — задайте перед запуском, например:
REM   set SPRING_DATASOURCE_PASSWORD=ВАШ_ПАРОЛЬ_ИЗ_DOCKER
REM Или используйте PowerShell:  cd ..  &&  .\scripts\run-dev.ps1

cd /d "%~dp0"
set SERVER_PORT=8082
call mvnw.cmd spring-boot:run
