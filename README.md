# Friends MC

MC 26.3 / NeoForge 26.3.0.26-beta / Java 25。首版零额外模组。

## 发布顺序

1. 在维护机安装 packwiz，进入 `pack/` 管理模组；区分 client/server/both。
2. 更新清单并 `packwiz refresh`，测试客户端和备份后的测试服务端。
3. 提交 `pack/`，取得完整 40 位提交 SHA。
4. 将 channel.template.json 复制为 channel.json，packUrl 填写：
   `https://raw.githubusercontent.com/chengweialan/mc-friends-client/<SHA>/pack/pack.toml`。
5. 正式服务端停服备份并部署相同清单，确认成功后再提交 channel.json。
6. 玩家启动时跟随 channel.json 指向的不可变提交。不要指向 main 下的 pack.toml。

玩家请从 [Releases](https://github.com/chengweialan/mc-friends-client/releases) 下载便携包，参考 [玩家说明](PLAYER-GUIDE.md)。
`channel.json` 指向已验证的不可变 pack 提交；不要将模板中的占位地址作为正式地址。
服务器不自动追随 main，也不由客户端更新脚本启动或升级。
回滚世界需恢复备份；仅回退模组清单不等于安全回滚世界。
不提交账号凭据、世界存档、Java/Minecraft 二进制文件及服务器私钥。
受管理配置可能覆盖玩家修改，因此不要收录 options.txt、地图标记等个人文件。

## 客户端

client-source 内是客户端启动/更新脚本；二进制下载来源与校验值见 client-source/THIRD-PARTY.json。
更新器固定为 packwiz-installer 0.5.14，bootstrap 0.0.3，不动态下载新的更新器代码。
版本变更建立新实例；Java 大版本变化要求新客户端包。

## 构建

安装 Python 3.11+，执行 `python build-release.py`，输出到新的 `dist/` 目录。
脚本下载固定版本组件并校验 SHA-256，保留许可文件，不包含 Minecraft 本体或账号数据。
`channel.json` 在 main 更新时，GitHub Actions 自动构建并创建 Releases 下载；release 字段应使用新的版本号，避免覆盖已发布版本。
构建前自动验证发布清单的版本与索引校验值。实际游戏启动仍需用户登录验收。
