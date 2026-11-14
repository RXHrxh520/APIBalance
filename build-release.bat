@echo off
rem Windows 发布构建入口，双击或 cmd 运行。
setlocal
cd /d "%~dp0"
if not exist keystore.properties (
  echo 缺少 keystore.properties：请复制 keystore.properties.example 并填入签名信息。
  exit /b 1
)
call gradlew.bat assembleRelease %*
endlocal
