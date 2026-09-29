# Friends MC 0.4.0

Minecraft 26.3 / NeoForge 26.3.0.26-beta / Java 25。

- Windows 保留 PCL2，新增必装/可选模组选择与记忆功能。
- 新增 macOS Apple Silicon 与 Intel 客户端，内置 Prism 和对应 Java，双击 Start.command。
- 两个平台使用同一固定版本清单，下载带 SHA-256 校验，失败中止，未完成事务下次恢复。
- 保留个人模组、地图、按键与账号，检测重复 mod ID。
- 3 个共同玩法模组必装，12 个辅助模组可选，3 个前置库自动跟随安装。
- 语音对外地址 wze.rainplay.cn:51612（UDP），游戏地址仍为 wze.rainplay.cn:21250。

## 旧 Windows 用户

下载 FriendsMC-UI-Update.zip，退出游戏和 PCL，解压覆盖原 FriendsMC 文件夹（Start.cmd、scripts、tools）。不删除原来的游戏和账号。随后运行 Start.cmd。旧更新器会提示需要升级，避免绕过新的模组选择流程。

## 暂缓的模组

垃圾槽的 Balm 依赖在当前 NeoForge 启动测试崩溃，暂不安装。旅人标题、流畅加载重置版、更多箱子（Iron Chests / Sophisticated Storage）、自动汉化更新未找到 26.3 NeoForge 文件。ModernFix 使用已标明的 mVUS 社区维护分支。详情见 MOD-STATUS.md。

macOS 首次可能需要在隐私与安全性中允许打开，并授予麦克风权限；首次游戏安装仍需联网、登录正版微软账号。
