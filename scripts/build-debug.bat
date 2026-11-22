@echo off
chcp 65001 >nul
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-debug.ps1" %*
set "RC=%ERRORLEVEL%"
endlocal & exit /b %RC%
