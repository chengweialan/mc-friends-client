# 0.4.0 模组与兼容性

Minecraft 26.3 / NeoForge 26.3.0.26-beta / Java 25。Windows 和 macOS 使用同一份 `pack/mods.lock.json`；`build-pack.py` 从该清单生成 packwiz 索引。

## 必装

- Corpse 1.1.19+26.3：死亡尸体。
- Carry On 2.12.0：搬运方块与生物。
- Trade Cycling 1.0.22+26.3：刷新未锁定的村民交易。

必装用于保持共同玩法和网络协议一致，更新器不能取消勾选。

## 客户端可选（默认勾选）

Simple Voice Chat 2.6.24+26.3、Xaero Minimap 26.5.3、Xaero World Map 1.46.4、JEI 31.7.0.47、Jade 26.3.1、Mouse Tweaks 2.31、Inventory Profiles Next 2.3.8、LambDynamicLights 4.13.0+26.3、AppleSkin 3.0.10、Better Climbing 6、Enchantment Descriptions 26.3.0.2、ModernFix-mVUS 5.27.23+mc26.3。

依赖自动随选项安装：JEI → MezzConfig 0.6.5；IPN → libIPN 6.9.0（含 Kotlin）；附魔描述 → Prickle 26.3.0.2。动态光源和 Xaero 的公共组件已内置，不额外放入 Fabric API。

服务端安装 Corpse、Carry On、Trade Cycling、Simple Voice Chat、Jade、AppleSkin、ModernFix-mVUS；客户端可以关闭后四项中的辅助功能。

## 本次不安装

| 请求 | 结果 |
|---|---|
| 垃圾槽 TrashSlot | 找到 26.3.0.1，但依赖 Balm 26.3.0.2 在当前 NeoForge 隔离启动测试触发 `BalmBiomeModifier AbstractMethodError`，整组暂缓 |
| 旅人标题 Traveler's Titles | 未找到 26.3 NeoForge 适配文件 |
| 流畅加载重置版 Smooth Boot (Reloaded) | 未找到 26.3 NeoForge 适配文件 |
| 现代化修复 ModernFix 原项目 | 无对应版本，使用明确标注的 mVUS 社区维护分支 |
| 更多箱子 | 按 Iron Chests 和 Sophisticated Storage 检查，未找到 26.3 NeoForge 版本 |
| 自动汉化更新 I18nUpdateMod | 未找到 26.3 NeoForge 适配文件；使用模组内置中文 |

## 额外操作

- 简单语音：雨云外部 `wze.rainplay.cn:51612/UDP` → 内部 `24454/UDP`。客户端首次选择麦克风/扬声器并允许系统麦克风权限。按 V 打开语音，加入同一群组可跨距离聊天。
- Carry On：默认空手 Shift + 右键，受模组的搬运限制控制；不可保证所有方块/生物都能搬运。
- Trade Cycling：用于尚未通过交易锁定的村民；已锁定职业/交易不能随意刷新。
- 动态光源：只改变客户端照明效果，不改变服务器刷怪光照规则；低性能电脑可以取消勾选。
- 按键冲突可在游戏“选项 → 控制”里调整；更新器不覆盖玩家按键、地图数据、账号和配置。
- 自己增加的模组可以保留，但需使用 26.3 NeoForge 版本。与整合包重复的同 ID 模组会提示移出，不自动删除个人文件。

只有 Java 游戏版本与加载器版本改变时才创建新实例；个人辅助模组不会未经验证迁移到新的游戏大版本。
