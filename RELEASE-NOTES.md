# Friends MC 客户端 0.4.1 — 国内下载源

客户端更新器升级；游戏整合包仍为 0.4.0，Minecraft 26.3 / NeoForge 26.3.0.26-beta，模组版本保持不变。

- Windows 保留 PCL，Mac 保留 Prism；两端优先通过北京 COS 获取版本和模组清单，GitHub 备用。
- 三轮重试，下载文件哈希不符也会换源；仍失败时停止启动，保留原有文件。
- 已核对许可的 8 个模组国内镜像，其余 10 个（含依赖）仍从作者 Modrinth CDN 获取，完整名单和作者链接见 MOD-CREDITS.md。
- Windows 旧用户下载 FriendsMC-UI-Update.zip；Mac 旧用户下载 FriendsMC-macOS-Update.zip。退出游戏及启动器，将补丁内容合并覆盖到原 FriendsMC 文件夹。不要删除原游戏、地图、账号文件。
- 国内补丁地址：https://friends-mc-downloads-1318356926.cos.ap-beijing.myqcloud.com/friends-mc/clients/0.4.1/FriendsMC-UI-Update.zip
- 游戏首次下载、NeoForge 安装和微软账号登录仍由启动器处理，需单独验证当地网络。此更新不承诺所有网络都能无代理完成首次安装。

新用户按系统下载完整包。国内源与 GitHub Release 文件一致，校验值见 SHA256SUMS.txt。
