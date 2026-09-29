# Friends MC 客户端 0.4.1 国内源补丁

游戏整合包仍为 0.4.0：Minecraft 26.3 / NeoForge 26.3.0.26-beta，模组版本不变。

退出游戏和启动器，将补丁内文件合并覆盖到原 FriendsMC 目录（与 Start.cmd 或 Start.command 同级）。不要删除原文件夹，也不要把整个 tools 文件夹替换为空文件夹。Windows 使用 FriendsMC-UI-Update.zip；Mac 使用 FriendsMC-macOS-Update.zip，Mac 不要替换 Start.command 的执行权限。

这次必须覆盖 tools/friends-updater.jar 和 download-sources.json，Windows 还要覆盖 scripts 内的脚本。只下载旧版完整包不能自动升级更新器本身。

再次运行 Start：版本清单使用北京 COS，GitHub 为备用；8 个已核对再分发条件的模组使用 COS，其他模组仍通过作者 Modrinth CDN 下载。完整名单及作者链接见 MOD-CREDITS.md。下载错误自动重试，哈希不匹配自动尝试备用地址。保留个人模组、地图、按键和账号。

首次安装 Minecraft 和 NeoForge 由 PCL / Prism 负责，微软登录也需要网络；不能据此保证所有地区所有网络都不需要代理。
