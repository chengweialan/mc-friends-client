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
choice=$(/usr/bin/osascript -e 'set chosen to choose from list {"好友生存服 · 26.3", "愚者 · 1.20.1"} with title "Friends MC · 选择世界" with prompt "选择后同步对应整合包。远程服务器由服主切换。" default items {"好友生存服 · 26.3"}' -e 'if chosen is false then return "cancel"' -e 'return item 1 of chosen')
if [[ "$choice" == 'cancel' ]]; then exit 0; fi
if [[ "$choice" == '愚者 · 1.20.1' ]]; then
  exec "$JAVA" -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Xdock:name='Friends MC · 愚者' -cp "$ROOT/tools/friends-updater.jar:$ROOT/tools/fool-updater.jar" FoolUpdater mac "$ROOT"
fi
exec "$JAVA" -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Xdock:name='Friends MC' -jar "$ROOT/tools/friends-updater.jar" mac "$ROOT"
