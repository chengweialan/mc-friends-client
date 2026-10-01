# Friends MC 客户端 0.5.0 — 好友服与愚者独立更新

Start 新增整合包选择：原好友生存服保持 Minecraft 26.3 / NeoForge；愚者使用作者 0.3.0 的 Minecraft 1.20.1 / Forge 47.4.12，分别保存实例、模组、配置和更新记录。

- Windows 保留 PCL，Mac 保留 Prism。愚者自动准备 Java 17，使用原生 packwiz 从北京 COS 同步模组、资源、任务和配置。
- 愚者的作者模组保持完整，可选光影由玩家选择。后续同步移除已退役的托管文件，保留未托管的个人模组及个人文件；自装模组仍需自行确认兼容性。
- Windows 已有 0.4.1 完整客户端的玩家：退出游戏和启动器，将 `FriendsMC-MultiPack-Update.zip` 解压覆盖到 Start.cmd 所在目录。
- Mac 已有完整客户端的玩家：退出游戏和启动器，将 `FriendsMC-macOS-MultiPack-Update.zip` 解压覆盖到 Start.command 所在目录。macOS 未完成实机游戏测试。
- 补丁不能在空目录单独运行。Windows 首次选择愚者需确认 PCL 导入；登录正版账号后启动。游戏本体和 Forge 由启动器安装，首次安装仍依赖启动器下载源。
- 客户端选择不会远程切换服务器；服务器一次只开启一个整合包。服主使用 `mc-pack fool` / `mc-pack friends` 切换，世界保持独立。

国内补丁下载：

- [Windows](https://friends-mc-downloads-1318356926.cos.ap-beijing.myqcloud.com/friends-mc/packs/fool/clients/0.5.0/FriendsMC-MultiPack-Update.zip)
- [macOS](https://friends-mc-downloads-1318356926.cos.ap-beijing.myqcloud.com/friends-mc/packs/fool/clients/0.5.0/FriendsMC-macOS-MultiPack-Update.zip)

后续新增或升级愚者模组，服主发布新 packwiz 清单后，玩家运行同一个 Start 即可对齐，无需为每次模组更新重下补丁。加载器或 Java 主版本变更需先确认兼容性。

详见 [愚者部署与维护说明](FOOL-SETUP.md)。原好友服仍使用原更新通道。
