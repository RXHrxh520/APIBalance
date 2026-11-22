#!/usr/bin/env bash
# 为 GitHub Release 生成 SHA256SUMS.txt 并上传。
# 用法：
#   scripts/release-checksum.sh [tag]
# 不带 tag 时扫描最近 100 个已发布 Release，只处理「有描述 + 有 APK + 还没有校验文件」的。
set -euo pipefail

command -v gh >/dev/null 2>&1 || { echo "需要 GitHub CLI (gh)" >&2; exit 1; }
if [ -z "${GH_TOKEN:-}" ] && ! gh auth status >/dev/null 2>&1; then
  echo "需要 GH_TOKEN 或已登录 gh" >&2
  exit 1
fi

if command -v sha256sum >/dev/null 2>&1; then
  hash_file() { sha256sum "$1" | awk '{print $1}'; }
else
  hash_file() { shasum -a 256 "$1" | awk '{print $1}'; }
fi

process_release() {
  local tag="$1"
  local body apk_count has_sum
  body=$(gh release view "$tag" --json body --jq '.body // ""')
  [ -n "$(printf '%s' "$body" | tr -d '[:space:]')" ] || { echo "[skip] $tag：Release 描述为空"; return 0; }
  apk_count=$(gh release view "$tag" --json assets --jq '[.assets[].name | select(endswith(".apk"))] | length')
  [ "$apk_count" -gt 0 ] || { echo "[skip] $tag：没有 APK 资产"; return 0; }
  has_sum=$(gh release view "$tag" --json assets --jq '[.assets[].name | select(. == "SHA256SUMS.txt")] | length')
  [ "$has_sum" -eq 0 ] || { echo "[skip] $tag：已存在 SHA256SUMS.txt"; return 0; }

  tmp=$(mktemp -d)
  trap 'rm -rf "$tmp"' EXIT
  gh release download "$tag" --dir "$tmp" --clobber
  # GitHub 自动生成的源码压缩包不在 assets 列表里，单独下载。
  gh release download "$tag" --archive=zip --dir "$tmp" --clobber || {
    echo "[fail] $tag：源码 zip 下载失败" >&2; return 1;
  }
  gh release download "$tag" --archive=tar.gz --dir "$tmp" --clobber || {
    echo "[fail] $tag：源码 tar.gz 下载失败" >&2; return 1;
  }

  : > "$tmp/SHA256SUMS.txt"
  local file name hash
  while IFS= read -r file; do
    name=$(basename "$file")
    hash=$(hash_file "$file")
    printf '%s  %s\n' "$hash" "$name" >> "$tmp/SHA256SUMS.txt"
  done < <(find "$tmp" -maxdepth 1 -type f ! -name 'SHA256SUMS.txt' -print | LC_ALL=C sort)

  gh release upload "$tag" "$tmp/SHA256SUMS.txt" --clobber
  rm -rf "$tmp"
  tmp=""
  echo "[ok] $tag：已上传 SHA256SUMS.txt"
}

if [ "${1:-}" ]; then
  process_release "$1"
else
  while IFS= read -r tag; do
    [ -n "$tag" ] && process_release "$tag"
  done < <(gh release list --limit 100 --json tagName,isDraft,isPrerelease \
    --jq '.[] | select(.isDraft == false) | .tagName')
fi
