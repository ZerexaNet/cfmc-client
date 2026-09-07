#!/bin/bash
# ============================================================================
# CFMC Client — 本地全版本矩阵构建
# 用法:  ./scripts/build-all.sh          # 构建全部支持版本 × 两个加载器
#        ./scripts/build-all.sh fabric    # 只构建 fabric 全版本
# 产物:  dist/<loader>-<mc>-<modver>.jar
# ============================================================================
set -e
cd "$(dirname "$0")/.."

LOADERS="${1:-fabric neoforge}"
VERSIONS=$(grep '^supported_versions=' gradle.properties | cut -d= -f2 | tr ',' ' ')
MODVER=$(grep '^mod_version=' gradle.properties | cut -d= -f2)

mkdir -p dist
echo "================================================"
echo " CFMC Client 全版本矩阵构建"
echo " 版本: $VERSIONS"
echo " 加载器: $LOADERS"
echo "================================================"

for loader in $LOADERS; do
  for mc in $VERSIONS; do
    # 1.20.1 无 NeoForge → 跳过 (versions.gradle 里 neoforge=null)
    if [ "$loader" = "neoforge" ] && [ "$mc" = "1.20.1" ]; then
      echo "跳过 neoforge @ $mc (该版本无 NeoForge)"
      continue
    fi
    echo ""
    echo "▶ 构建 $loader @ MC $mc ..."
    if gradle ":$loader:build" -Pmc_version="$mc" --no-daemon -q; then
      cp "$loader/build/libs/${MODVER/ /}"*.jar dist/ 2>/dev/null || true
      cp "$loader/build/libs/"*.jar dist/ 2>/dev/null || true
      echo "✔ $loader @ $mc 完成"
    else
      echo "✘ $loader @ $mc 构建失败 (见上方日志)"
      FAILED=1
    fi
  done
done

# 去重 sources jar (保留 release jar)
rm -f dist/*-sources.jar dist/*-test.jar 2>/dev/null || true

echo ""
echo "================================================"
echo " 完成! 产物在 dist/:"
ls -1 dist/ 2>/dev/null || echo " (无产物)"
[ "${FAILED:-0}" = "1" ] && exit 1 || exit 0
