# Friends MC — Windows / macOS

客户端工具 0.4.1；整合包 0.4.1。Minecraft 26.3，NeoForge 26.3.0.26-beta，Java 25。

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
4. 修改 channel.json 只发布国内更新清单和获准镜像的模组，不构建客户端。修改更新器时才提高 CLIENT-VERSION，手动运行 Build Windows and macOS clients 发布完整包及两端补丁。
5. 若更新器本身改变，旧用户安装 UI 更新补丁；日常模组更新只需运行 Start。

Java 更新器源码位于 updater/，构建时使用 javac --release 17 编译，运行时使用包内 Java 25。只管理清单中的 JAR；删除/替换前备份，下载失败不进入提交阶段，中断的事务下次启动恢复。备份位于实例 .friends-sync/backup-*，可在确认更新稳定后手工归档。

最低启动器协议 schema=2，旧版 0.3.0 会拒绝新渠道并提示升级，避免漏装必装模组。


## 完整服主管理手册

见 [服务器与模组管理指南](SERVER-MANAGEMENT.md)。添加、升级、删除模组可使用 `python tools/manage-pack.py --help`。工具准备清单，不自动推送或部署服务器。

当前 Start 只同步托管 JAR，不同步 config、KubeJS 或任务脚本，也不自动升级自身。导入大型整合包前须按指南扩展相应能力。完整客户端只从 GitHub 下载，不再上传完整 ZIP 到 COS。
