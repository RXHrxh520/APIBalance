# Windows PowerShell 发布构建入口：
# powershell -NoProfile -ExecutionPolicy Bypass -File .\build-release.ps1
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$javaLine = (& java -version 2>&1 | Out-String)
if ($javaLine -notmatch 'version "([0-9]+)') {
    throw '找不到 Java，或 java -version 输出无法解析。需要 JDK 27+。'
}
$javaMajor = [int]$Matches[1]
if ($javaMajor -lt 27) {
    throw "需要 JDK 27 或更高版本，当前为 $javaMajor。"
}

$sdk = $env:ANDROID_HOME
if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
if ((-not $sdk) -and (Test-Path 'local.properties')) {
    $line = Get-Content 'local.properties' | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -Last 1
    if ($line) { $sdk = ($line -replace '^sdk\.dir=', '').Replace('\\','\').Replace('\:',':') }
}
if (-not $sdk) {
    throw '未找到 Android SDK：请设置 ANDROID_HOME/ANDROID_SDK_ROOT，或在 local.properties 写 sdk.dir。'
}
$platform = Join-Path $sdk 'platforms/android-37'
$buildTools = Join-Path $sdk 'build-tools/37.0.0'
if (-not (Test-Path $platform)) { throw "缺少 SDK Platform 37：$platform" }
if (-not (Test-Path $buildTools)) { throw "缺少 Build Tools 37.0.0：$buildTools" }

if (-not (Test-Path 'keystore.properties')) {
    throw '缺少 keystore.properties：请复制 keystore.properties.example 并填入签名信息。'
}
$props = Get-Content 'keystore.properties' -ErrorAction Stop
foreach ($key in @('RXH_STORE_PASS', 'RXH_KEY_PASS')) {
    if (-not ($props | Where-Object { $_ -match "^$key=." })) {
        throw "keystore.properties 缺少 $key=..."
    }
}
$storeLine = $props | Where-Object { $_ -match '^RXH_STORE_FILE=' } | Select-Object -Last 1
$storeFile = if ($storeLine) { $storeLine -replace '^RXH_STORE_FILE=', '' } else { 'keystore.jks' }
if (-not $storeFile) { $storeFile = 'keystore.jks' }
$storePath = if ([System.IO.Path]::IsPathRooted($storeFile)) { $storeFile } else { Join-Path $PSScriptRoot $storeFile }
if (-not (Test-Path $storePath)) { throw "找不到密钥库：$storePath" }

$gradleArgs = @('assembleRelease') + $args
# Windows ARM64 主机自动切换 aapt2，不需要手工加 -P 参数。
if ($env:PROCESSOR_ARCHITECTURE -eq 'ARM64') {
    $aapt2 = Join-Path $buildTools 'aapt2.exe'
    if (-not (Test-Path $aapt2)) { throw "ARM64 主机需要 aapt2：$aapt2" }
    $gradleArgs += "-Pandroid.aapt2FromMavenOverride=$aapt2"
}

& .\gradlew.bat @gradleArgs
exit $LASTEXITCODE
