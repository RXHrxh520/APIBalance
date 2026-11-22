@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0bump-version.ps1" %*
set "RC=%ERRORLEVEL%"
endlocal & exit /b %RC%
