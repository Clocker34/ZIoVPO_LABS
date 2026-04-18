@echo off
cd /d "%~dp0"
echo Running scripts\run-dev.ps1 ...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\run-dev.ps1"
pause
