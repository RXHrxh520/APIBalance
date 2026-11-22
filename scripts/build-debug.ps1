# 调试构建入口（Windows）：不需要 release keystore。
# powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-debug.ps1
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
& (Join-Path $PSScriptRoot 'check-env.ps1')

$gradleArgs = @('assembleDebug') + $args
if ($env:PROCESSOR_ARCHITECTURE -eq 'ARM64') {
    $sdk = $env:ANDROID_HOME
    if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
    if ((-not $sdk) -and (Test-Path 'local.properties')) {
        $line = Get-Content 'local.properties' | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -Last 1
        if ($line) { $sdk = ($line -replace '^sdk\.dir=', '').Replace('\\','\').Replace('\:',':') }
    }
    $aapt2 = Join-Path $sdk 'build-tools/37.0.0/aapt2.exe'
    if (-not (Test-Path $aapt2)) { throw "ARM64 主机需要 aapt2：$aapt2" }
    $gradleArgs += "-Pandroid.aapt2FromMavenOverride=$aapt2"
}
& .\gradlew.bat @gradleArgs
exit $LASTEXITCODE
