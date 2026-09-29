#!/bin/bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
ROOT="$PWD"
JAVA="$ROOT/runtime/Contents/Home/bin/java"
if [[ ! -x "$JAVA" ]]; then
  echo "内置 Java 缺失或没有执行权限，请重新解压完整 macOS 客户端。"
  read -r -p '按回车关闭' ignored
  exit 1
fi
exec "$JAVA" -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Xdock:name='Friends MC' -jar "$ROOT/tools/friends-updater.jar" mac "$ROOT"
