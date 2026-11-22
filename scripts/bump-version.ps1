# 版本号升位工具（Windows PowerShell）
# 用法：powershell -ExecutionPolicy Bypass -File scripts/bump-version.ps1 [-Type patch|minor|major] [-GradleFile app/build.gradle]
param(
    [ValidateSet('patch', 'minor', 'major')]
    [string]$Type = 'patch',
    [string]$GradleFile = 'app/build.gradle'
)

$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
if (-not (Test-Path $GradleFile)) {
    throw "找不到 $GradleFile，请在仓库根目录执行本脚本"
}

$text = Get-Content -LiteralPath $GradleFile -Raw -Encoding UTF8
$mCode = [regex]::Match($text, 'versionCode\s+(\d+)')
$mName = [regex]::Match($text, 'versionName\s+([\x27\"])([^\x27\"]+)\1')
if (-not $mCode.Success -or -not $mName.Success) {
    throw "未能在 $GradleFile 中找到 versionCode / versionName"
}

$oldCode = [int]$mCode.Groups[1].Value
$quote = $mName.Groups[1].Value
$oldName = $mName.Groups[2].Value
$parts = @($oldName.Split('.') | ForEach-Object { if ($_ -match '^\d+$') { [int]$_ } else { 0 } })
while ($parts.Count -lt 3) { $parts += 0 }
switch ($Type) {
    'major' { $parts = @($parts[0] + 1, 0, 0) }
    'minor' { $parts = @($parts[0], $parts[1] + 1, 0) }
    default { $parts = @($parts[0], $parts[1], $parts[2] + 1) }
}
$newCode = $oldCode + 1
$newName = ($parts -join '.')

$text = $text.Substring(0, $mCode.Index) + "versionCode $newCode" + $text.Substring($mCode.Index + $mCode.Length)
$text = $text.Substring(0, $mName.Index) + "versionName $quote$newName$quote" + $text.Substring($mName.Index + $mName.Length)
[System.IO.File]::WriteAllText((Resolve-Path $GradleFile), $text, (New-Object System.Text.UTF8Encoding($false))

Write-Host "versionCode : $oldCode -> $newCode"
Write-Host "versionName : $oldName -> $newName"
Write-Host ''
Write-Host '下一步：.\build-release.bat，然后把 APK 与签名信息发到 Release'
