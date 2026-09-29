# Friends MC — Windows / macOS

当前发布：0.4.0。Minecraft 26.3，NeoForge 26.3.0.26-beta，Java 25。

- Windows：PCL2 + Start.cmd；已有用户下载 UI 更新补丁覆盖。
- macOS：Prism Launcher + Start.command；分别提供 Apple Silicon 和 Intel 包。
- 两端共用 `pack/mods.lock.json`，固定模组文件与 SHA-256；`build-pack.py` 生成 packwiz 元数据。
- 必装与可选模组清单见 [MOD-STATUS.md](MOD-STATUS.md)，Mac 操作见 [MAC-GUIDE.md](MAC-GUIDE.md)。
- 更新前关闭游戏与启动器；更新窗口可重新选择辅助模组。个人配置和非托管模组保留。
- 当前仓库不发布 Minecraft 游戏本体、账号、世界存档或私钥。游戏由启动器从正规源下载。

## 维护流程

1. 更新 mods.lock.json 中经过测试的版本、下载 URL、哈希和依赖，执行 `python3 build-pack.py`。
2. 对服务端组合做隔离启动测试，确认 Done；测试客户端的可选项增删与前置依赖。
3. 先提交清单，再让 channel.json 的 packUrl 指向该不可变 commit，同时更新 catalogSha256 与 release。不要让 packUrl 指向 main。
4. 修改 channel.json 会触发同一工作流，构建 Windows、macOS arm64 和 macOS x64；统一发布到一个 GitHub Release。
5. 若更新器本身改变，旧用户安装 UI 更新补丁；日常模组更新只需运行 Start。

Java 更新器源码位于 updater/，构建时使用 javac --release 17 编译，运行时使用包内 Java 25。只管理清单中的 JAR；删除/替换前备份，下载失败不进入提交阶段，中断的事务下次启动恢复。备份位于实例 .friends-sync/backup-*，可在确认更新稳定后手工归档。

最低启动器协议 schema=2，旧版 0.3.0 会拒绝新渠道并提示升级，避免漏装必装模组。

