# Friends MC — Windows / macOS

客户端工具 0.5.0 新增整合包选择：原好友服（26.3 / NeoForge / Java 25）与愚者（1.20.1 / Forge 47.4.12 / Java 17）。

已有 0.4.1 完整客户端安装一次 [多整合包补丁](FOOL-SETUP.md#玩家使用)，之后通过同一个 Start 选择和更新。愚者使用原生 packwiz，同步模组、配置、任务、配方和资源；服务端需要服主另行切换。

- Windows：PCL2 + Start.cmd；已有用户下载 UI 更新补丁覆盖。
- macOS：Prism Launcher + Start.command；分别提供 Apple Silicon 和 Intel 包。
- 原好友服两端共用 `pack/mods.lock.json`；愚者使用独立的 `packs/fool/channel.json` 和国内原生 packwiz 发布目录。
- 必装与可选模组清单见 [MOD-STATUS.md](MOD-STATUS.md)，Mac 操作见 [MAC-GUIDE.md](MAC-GUIDE.md)。
- 更新前关闭游戏与启动器；更新窗口可重新选择辅助模组。个人配置和非托管模组保留。
- 当前仓库不发布 Minecraft 游戏本体、账号、世界存档或私钥。游戏由启动器从正规源下载。

## 原好友服维护流程

1. 更新 mods.lock.json 中经过测试的版本、下载 URL、哈希和依赖，执行 `python3 build-pack.py`。
2. 对服务端组合做隔离启动测试，确认 Done；测试客户端的可选项增删与前置依赖。
3. 先提交清单，再让 channel.json 的 packUrl 指向该不可变 commit，同时更新 catalogSha256 与 release。不要让 packUrl 指向 main。
4. 修改 channel.json 只发布国内更新清单和获准镜像的模组，不构建客户端。修改更新器时才提高 CLIENT-VERSION，手动运行 Build Windows and macOS clients 发布完整包及两端补丁。
5. 若更新器本身改变，旧用户安装 UI 更新补丁；日常模组更新只需运行 Start。

原好友服 Java 更新器源码位于 updater/，构建时使用 javac --release 17 编译，运行时使用包内 Java 25。只管理清单中的 JAR；删除/替换前备份，下载失败不进入提交阶段，中断的事务下次启动恢复。备份位于实例 .friends-sync/backup-*，可在确认更新稳定后手工归档。

最低启动器协议 schema=2，旧版 0.3.0 会拒绝新渠道并提示升级，避免漏装必装模组。


## 完整服主管理手册

见 [服务器与模组管理指南](SERVER-MANAGEMENT.md)。添加、升级、删除模组可使用 `python tools/manage-pack.py --help`。工具准备清单，不自动推送或部署服务器。

原好友服分支继续只同步托管 JAR；新增愚者分支同步完整 packwiz 清单（含配置、任务、资源）。两者使用独立实例与更新状态。更新器自身升级仍需安装补丁；日常整合包更新只运行 Start。完整客户端仍从 GitHub 下载，本次 COS 仅新增愚者更新内容与小补丁。

愚者安装与发布原则见 [FOOL-SETUP.md](FOOL-SETUP.md)；主机操作说明保留在服主本地。
