# Windows PowerShell 发布构建入口：powershell -ExecutionPolicy Bypass -File build-release.ps1
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
if (-not (Test-Path "keystore.properties")) {
    Write-Error "缺少 keystore.properties：请复制 keystore.properties.example 并填入签名信息。"
    exit 1
}
& .\gradlew.bat assembleRelease @args
exit $LASTEXITCODE
