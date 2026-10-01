# 愚者客户端与独立更新

客户端补丁 0.5.0 可通过同一个 Start 选择原好友生存服或愚者。两者使用不同实例和更新记录。愚者采用作者 0.3.0：Minecraft 1.20.1 / Forge 47.4.12 / Java 17。

## 玩家使用

1. 先退出 Minecraft 和启动器。补丁适用于已有的 Friends MC 0.4.1 完整客户端，不能单独在空目录运行。
2. 下载对应补丁并解压覆盖到原 Start 所在文件夹：
   - [Windows 补丁](https://friends-mc-downloads-1318356926.cos.ap-beijing.myqcloud.com/friends-mc/packs/fool/clients/0.5.0/FriendsMC-MultiPack-Update.zip)
   - [macOS 补丁](https://friends-mc-downloads-1318356926.cos.ap-beijing.myqcloud.com/friends-mc/packs/fool/clients/0.5.0/FriendsMC-macOS-MultiPack-Update.zip)
3. Windows 打开 Start.cmd，Mac 打开 Start.command，选择愚者。
4. 首次会准备 Java 17，并通过国内 packwiz 源同步模组、配置、配方、任务和资源，约 2GB。作者的 225 个模组保持完整，可选光影由玩家选择。
5. Windows 首次需确认 PCL 导入；Mac 在 Prism 中选择愚者实例。启动器负责安装 Minecraft 与 Forge，首次安装仍依赖启动器下载源。
6. 登录正版账号后启动，使用服主提供的游戏地址。客户端选择不会切换远端服务器，进服前确认服主当前开放的是哪个整合包。
7. 以后退出游戏后运行同一个 Start，只对齐有变化的托管文件，不必为每次模组更新重下补丁。

托管列表移除的旧模组会被删除；未托管的个人模组、截图、世界和账号不会做整目录覆盖。部分个人偏好配置只首次下发，任务、配方和内容配置随整合包同步。避免用个人文件覆盖同名托管文件，自装模组仍需确认兼容性。

客户端默认游戏内存 6GB，建议电脑总内存至少 16GB。以后可在启动器中自行调整，普通同步不会重置该选择。

## 版本发布原则

原好友服保留原 JSON 模组更新器。愚者使用原生 packwiz，公开目录 packs/fool 保存版本通道、文件清单和索引快照；完整下载源由 channel.json 指定。

维护者可用 tools/import-fool.py 从作者客户端的 modrinth.index.json 和 overrides 生成候选发布。当前支持已验证的 MC 1.20.1 / Forge 47.4.12；加载器或 Java 主版本变更需先评估兼容性。

每次更新都使用新发布号。先准备客户端和对应服务端，确认兼容后上传全部模组与资源，最后切换 channel.json。客户端 Start 不会自动部署或切换服务端。具体主机管理信息由服主在本地保管。

## 已验证与限制

已测试真实整合包同步、缺失模组下载、Java 17、个人文件保留和原好友服更新器兼容。尚未替玩家完成 PCL 登录进服；macOS 未做实机游戏测试。首次安装、光影及个人额外模组请以实机结果为准。

作者来源：[愚者 0.3.0](https://bbsmc.net/modpack/the-fool/version/0.3.0)。保留作者署名和包内许可，各模组许可不因本次部署而改变。

[packwiz 安装文档](https://packwiz.infra.link/tutorials/installing/packwiz-installer/)
