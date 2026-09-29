# Friends MC · macOS

Apple Silicon（M1/M2/M3/M4 等）下载 arm64，Intel Mac 下载 x64。包内带对应 Java 25 和原版 Prism Launcher 11.1.1，不需要安装 Windows 的 PCL、Python、Git 或系统 Java。

1. 将 ZIP 完整解压到可写的普通文件夹；不要直接在压缩包内运行。
2. 双击 `Start.command`。首次若 macOS 阻止打开，请在“系统设置 → 隐私与安全性”检查来源后允许此文件；Prism 首次打开也可能需要确认。无需关闭系统安全保护。
3. 在窗口中确认必装模组，勾选想要的可选模组，点确定。
4. 更新中心下载并校验模组，之后打开 Prism。首次按启动器指引完成设置并登录拥有 Minecraft Java 版的微软账号。
5. 启动 FriendsMC 实例。Prism 自动下载对应 Minecraft / NeoForge，进度在 Prism 显示。游戏里加入 `wze.rainplay.cn:21250`。

以后更新：关闭游戏及此包的 Prism，再双击 `Start.command`。Windows 的 `Start.cmd` 和 macOS 的 `Start.command` 读取同一个发布渠道、同一套模组版本。每次都会显示选项并记住上次选择；取消勾选会备份移出该可选模组。

首次联网下载和账号登录不可省略。macOS 必须支持包内 Java 与 Prism；旧系统若无法启动，请根据错误升级系统，不使用 x64 模拟作为默认配置。此包未做 Apple 开发者签名/公证。

语音：首次使用需允许麦克风权限；按 V 设置音频设备与说话键，进入同一语音群组可无视距离聊天。

更新日志：`logs/latest-update.log`。个人地图、按键和账号保留在此文件夹，不要上传到公共仓库。个人 mods 可放入 `launcher-data/instances/FriendsMC-26.3-26.3.0.26-beta/.minecraft/mods`；若与整合包同 ID 冲突，更新器会提示具体文件。

网络下载失败会中止启动；再次运行会重试。安装事务中断时，下次启动会从 `.friends-sync` 内的备份恢复。不要在游戏运行时手工替换模组。

不适配的模组与功能限制见仓库 MOD-STATUS.md。macOS 启动器界面与 Windows PCL 不同，但版本对齐、模组选择、下载进度、个人文件保留功能共用同一套更新代码。
