# 构建环境自检（Windows）：powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\check-env.ps1 --release
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$mode = if ($args.Count -gt 0) { $args[0] } else { '' }

$javaLine = (& java -version 2>&1 | Out-String)
if ($javaLine -notmatch 'version "([0-9]+)') { throw '找不到 Java，或 java -version 输出无法解析' }
$javaMajor = [int]$Matches[1]
if ($javaMajor -lt 27) { throw "需要 JDK 27+，当前为 $javaMajor" }
Write-Host "OK  JDK $javaMajor"

$sdk = $env:ANDROID_HOME
if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
if ((-not $sdk) -and (Test-Path (Join-Path $root 'local.properties'))) {
    $line = Get-Content (Join-Path $root 'local.properties') | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -Last 1
    if ($line) { $sdk = ($line -replace '^sdk\.dir=', '').Replace('\\','\').Replace('\:',':') }
}
if (-not $sdk) { throw '未设置 ANDROID_HOME/ANDROID_SDK_ROOT，也没有 local.properties sdk.dir' }
$platform = Join-Path $sdk 'platforms/android-37'
$buildTools = Join-Path $sdk 'build-tools/37.0.0'
if (-not (Test-Path $platform)) { throw "缺少 $platform" }
if (-not (Test-Path $buildTools)) { throw "缺少 $buildTools" }
Write-Host "OK  SDK $sdk"

$aapt2 = Join-Path $buildTools 'aapt2.exe'
if (-not (Test-Path $aapt2)) { throw "aapt2 不存在：$aapt2" }
Write-Host "OK  aapt2 $aapt2"

if ($mode -eq '--release') {
    $propsPath = Join-Path $root 'keystore.properties'
    if (-not (Test-Path $propsPath)) { throw "缺少 $propsPath" }
    $props = Get-Content $propsPath
    foreach ($key in @('RXH_STORE_PASS', 'RXH_KEY_PASS')) {
        if (-not ($props | Where-Object { $_ -match "^$key=." })) { throw "keystore.properties 缺少 $key" }
    }
    $storeLine = $props | Where-Object { $_ -match '^RXH_STORE_FILE=' } | Select-Object -Last 1
    $storeFile = if ($storeLine) { $storeLine -replace '^RXH_STORE_FILE=', '' } else { 'keystore.jks' }
    if (-not $storeFile) { $storeFile = 'keystore.jks' }
    $storePath = if ([System.IO.Path]::IsPathRooted($storeFile)) { $storeFile } else { Join-Path $root $storeFile }
    if (-not (Test-Path $storePath)) { throw "密钥库不存在：$storePath" }
    Write-Host "OK  keystore $storePath"
}

Write-Host '环境检查通过'
