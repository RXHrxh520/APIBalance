#!/usr/bin/env sh
# 生成 release keystore 并打印 SHA-256（用于 EnvChecker 的官方证书常量）。
# 用法：scripts/generate-keystore.sh [alias] [keystore路径] [口令]
set -eu
ALIAS="${1:-key0}"
STORE="${2:-keystore.jks}"
PASS="${3:?用法: scripts/generate-keystore.sh [alias] [keystore] [口令]}"

keytool -genkeypair -v \
  -keystore "$STORE" -alias "$ALIAS" \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$PASS" -keypass "$PASS" \
  -dname "CN=APIBalance, OU=Release, O=APIBalance, L=Unknown, ST=Unknown, C=CN"

echo
echo "证书 SHA-256："
keytool -list -v -keystore "$STORE" -alias "$ALIAS" -storepass "$PASS" \
  | sed -n 's/.*SHA256: //p' | head -1
echo
echo "下一步：把上面的指纹填到 EnvChecker.OFFICIAL_CERT_SHA256，并在根目录创建 keystore.properties。"
