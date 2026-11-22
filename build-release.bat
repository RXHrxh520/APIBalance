@echo off
rem Windows 发布构建入口：统一交给 PowerShell 脚本做 JDK/SDK/签名/ARM64 检查。
chcp 65001 >nul
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-release.ps1" %*
set "RC=%ERRORLEVEL%"
endlocal & exit /b %RC%
