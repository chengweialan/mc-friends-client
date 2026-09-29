# Friends MC 服务器与模组管理指南

以现有服务器、GitHub 仓库与 0.4.1 客户端为准 · 2026-09-29

> 日常加减模组：修改主清单 → 准备服务端 → 发布清单 → 等国内发布任务完成 → 玩家退出游戏并运行 Start。不要让朋友手工下载每个模组，也不要只往服务器的 mods 目录扔文件。

## 01 · 当前状态与管理目标

| 项目 | 当前内容 |
|---|---|
| 游戏 / 加载器 | Minecraft 26.3 / NeoForge 26.3.0.26-beta |
| Java | 服务端与现有便携客户端使用 Java 25 |
| 客户端工具版本 | 0.4.1；Windows 用 PCL，Mac 用 Prism |
| 游戏整合包发布版本 | 0.4.1；已加入 FerriteCore，服务端已补装 JEI 与 MezzConfig |
| 服务器 | Ubuntu 24.04，4 核 / 8 GB，服务账户 minecraft |
| 正式目录 | `/srv/mc-data/minecraft` |
| 服务名 | `minecraft.service` |
| 游戏连接 | `wze.rainplay.cn:21250` → 内部 TCP 25565 |
| 语音 | `wze.rainplay.cn:51612` → 内部 UDP 24454 |
| SSH | `root@wze.rainplay.cn`，端口 46000 |
| 代码与发布清单 | [chengweialan/mc-friends-client](https://github.com/chengweialan/mc-friends-client) |
| 国内清单 | 北京 COS：`friends-mc-downloads-1318356926`，目录 `friends-mc/` |

完整客户端只在 GitHub 分发，不再搬运完整 ZIP 到 COS。COS 用于更新清单、允许再分发的模组和旧用户补丁。部分模组继续使用作者 Modrinth CDN；在内地服务器测试可访问，不等于所有运营商、所有时间都绝对可用。

**“内地能下载”和“文件全部存放中国内地”是两件事。**目前做到的是国内清单优先、部分模组国内镜像、其余作者源回退。不是所有 JAR 都已在 COS。新增模组的作者源如果不通，需要另行解决分发条件与镜像，不能靠修改版本号解决。

本指南是操作手册，不代表其中的迁移、插件、配置文件同步等功能已执行或实现。当前世界、游戏版本和加载器没有因编写指南而改变。

## 02 · Start 到底能同步什么

实际流程：`Start → 国内 channel.json → 固定版本 mods.lock.json → 玩家选择 → 下载需要的 JAR → 替换托管模组 → 打开启动器`。GitHub 保留管理历史；朋友不需要 Git，也不执行 git pull。

| 内容 | 当前能力 | 服主应怎样处理 |
|---|---|---|
| 清单里的必装模组 | 自动安装 / 更新，不能取消勾选 | 所有玩家必须有的内容设 required=true |
| 清单里的可选模组 | 玩家选择，记住各自选择 | JEI、地图等可以可选；“对齐”不等于人人安装全部可选项 |
| 清单里的依赖 | 选中上游时自动带上 | dependencies 必须写内部 ID；依赖本身也要有完整条目 |
| 被移除的托管模组 | 下次同步移出 mods，保留更新备份 | 只影响更新器已经托管的文件，不清空整个 mods |
| 玩家额外放入的 JAR | 一般保留；检测到部分重复 modId 会阻止更新 | 额外模组不受服主版本约束，也不能保证相互兼容 |
| config、defaultconfigs、KubeJS、任务脚本、资源包 | **当前不会自动发布同步** | 导入涉及这些文件的整合包前，必须扩展更新器 |
| 玩家按键、账号、小地图、存档 | 普通同版本 JAR 更新不主动覆盖 | 跨版本创建新实例时，不承诺自动搬移这些数据 |
| 服务端 mods 和配置 | **客户端 Start 不会更新服务器** | 服主需要单独部署服务端 |
| Java / 更新器本身 / 启动脚本 | 不靠普通模组清单自动升级 | 需要一次客户端补丁或新版完整包 |
| 切换 Forge / Fabric / Paper | 当前不支持由清单自动切换 | 当前启动逻辑固定 NeoForge，需先改造客户端 |

**保证范围：使用兼容客户端、通过 Start 完成同步后，选中的托管模组使用发布清单规定的版本。**无法强迫绕过 Start 直接开 PCL 的玩家更新，也不能保证任意个人模组兼容。若要严格禁止旧包进服，需要额外的服务端版本准入机制；现阶段没有实现。

客户端内部还会计算文件指纹，这是现有同步协议用于识别版本与缓存的字段，通常无需手工处理。按服主的速度优先要求，不再上传后逐个重新下载核验，也不要求每次重跑全量客户端测试；保留一次启动观察与必要备份。

## 03 · 三种版本号不要混用

| 版本 | 在哪里 | 什么时候改变 |
|---|---|---|
| Minecraft / NeoForge | `pack/mods.lock.json` 与 `channel.json` | 迁移游戏或加载器版本 |
| 整合包 release | `pack/mods.lock.json`，再生成 pack.toml；channel 随发布更新 | 添加、删除、升级模组，例如下一次 0.4.2 |
| 客户端工具 CLIENT-VERSION | 仓库根目录 `CLIENT-VERSION` | 修改 Start、Java、更新器能力等，需要发客户端包 |

例如：客户端工具仍是 0.4.1，整合包可以升到 0.4.2、0.4.3，朋友只运行 Start。不要仅因加了一个模组就重新构建三套客户端。

packwiz 文件是由 `build-pack.py` 从 `mods.lock.json` 生成的兼容元数据。**当前主清单是 JSON，不是 .pw.toml。**只运行 `packwiz update --all` 或只编辑 .pw.toml，不会自动改变 Start 实际使用的主清单；后续重新生成还可能覆盖你的改动。packwiz 原生安装器是另一种可选架构，当前并未用它执行日常同步。[packwiz 官方说明](https://packwiz.infra.link/tutorials/installing/packwiz-installer/)

## 04 · 服主电脑首次准备

只有服主需要 Git 与 Python 3.11+；玩家不需要。下面的 Windows 命令在 **PowerShell** 中执行，服务器命令在 **SSH 登录后的 Ubuntu shell** 中执行，两者不要混用。

在自己的维护目录建立真正的 Git 工作副本。当前 `outputs/mc-friends-client/repository` 是本次工作的源文件副本，不要假定它就是可直接 push 的 Git 仓库。

```powershell
# Windows PowerShell：只在首次建立工作副本时执行
git clone https://github.com/chengweialan/mc-friends-client.git
Set-Location mc-friends-client
python --version
git status
```

以后从该目录开始：

```powershell
git status
git pull --ff-only
python tools/manage-pack.py list
```

如果 status 显示自己未提交的改动，先保留或提交，不要直接覆盖。push 时使用自己的 GitHub 登录；朋友不会接触这些凭据。

配套工具 `tools/manage-pack.py` 只准备仓库文件与下载缓存，**不会自动提交、推送、重启服务器或发布**。

## 05 · 添加一个客户端或双端模组

### 第一步：先选对文件

在作者的 Modrinth 版本页选择准确的 **Minecraft 26.3 + NeoForge** 文件，并查看是否有必需依赖。不要把 Fabric、Forge 或其他 MC 版本的 JAR 当作同一文件。模组自己支持的范围以作者说明与 JAR 元数据为准。

先分类：

| 类型 | 主清单设置 | 服务端 |
|---|---|---|
| 新物品、方块、维度、影响双方玩法 | required=true，client=true，server=true | 装同一适配版本 |
| 小地图、UI、纯客户端辅助 | required=false，client=true，server=false | 通常不装 |
| 双端可选辅助 / 优化 | required=false，client=true，server=true，default=true | 按该模组规则安装 |
| 前置库 | hidden=true、required=false，由 dependencies 引用 | 上游需要时也装对应库 |
| 纯服务端优化 / 管理模组 | **单独管理服务端清单** | 不放进客户端主清单 |

特别注意：当前更新器的选择逻辑主要看 required、hidden、default、dependencies，不能只靠 `client=false` 阻止下载。纯服务端模组不要混入客户端主清单；`server=true` 也不代表系统会自动远程部署。

### 第二步：用辅助工具生成条目

用版本页最后一段的 **版本 ID**，而不是项目 ID。FerriteCore 这次使用的真实示例：

```powershell
python tools/manage-pack.py upsert LtVvw4uS --id ferritecore --kind optional --server --name "FerriteCore 内存优化" --description "降低内存占用，默认推荐安装"
```

这会下载一次该文件、读取 modId、生成更新器需要的文件指纹与大小、写入主清单和 packwiz 元数据，不需要你手工算哈希。已安装相同项目时更新原条目；不要给同一个项目换内部 ID。

普通必装双端模组用以下模板；先把示例 ID 换成真实版本 ID：

```powershell
python tools/manage-pack.py upsert 实际版本ID --kind required --server
```

纯客户端可选模组：

```powershell
python tools/manage-pack.py upsert 实际版本ID --kind optional
```

前置库先添加，然后添加引用它的模组：

```powershell
python tools/manage-pack.py upsert 前置版本ID --kind dependency --server
python tools/manage-pack.py upsert 上游版本ID --kind required --server
```

工具只识别 Modrinth API 声明的 required 依赖。作者额外说明、内嵌库、冲突关系、配置要求仍需看版本说明；工具不会替你判断所有兼容问题。当前更新器限制原始 URL 为 Modrinth CDN，不能直接填任意网盘或 CurseForge URL。

### 第三步：检查这几个字段的含义

```json
{
  "id": "example-mod",
  "name": "显示给玩家的名字",
  "description": "用途及额外操作",
  "required": true,
  "server": true,
  "client": true,
  "hidden": false,
  "default": true,
  "dependencies": ["example-library"]
}
```

上面仅是字段说明片段，不能作为完整条目直接发布。完整条目还包含 version、projectId、versionId、filename、url、sha256、size、modIds、source。可以用 JEI、FerriteCore 等现有条目作为参考。

新增可选模组 default=true 时，尚无该模组选项记录的玩家会默认勾选。玩家以前取消过某项，改 default=true 不会强行改变他的选择。需要全员安装的玩法内容必须用 required=true，不能把默认勾选当作强制安装。

### 第四步：服务端单独部署

如果是纯客户端模组，跳过这一步。双端模组先用第 10 节备份并停服，再通过 SCP 或 SFTP 上传本次需要的 JAR 和前置库，放入服务器 `mods`。不要把客户端全部 JAR 不加区分地复制过去。

```bash
# Ubuntu SSH：先建立上传暂存目录
mkdir -p /srv/mc-data/incoming
```

```powershell
# Windows PowerShell：将“具体文件.jar”替换为本次准备的真实文件名
scp -i "$env:USERPROFILE\.ssh\rainyun-rgs-mc-server.pem" -P 46000 ".mod-cache\具体文件.jar" root@wze.rainplay.cn:/srv/mc-data/incoming/
```

完成备份并停服后：

```bash
# Ubuntu SSH：逐个安装实际需要的文件
install -o minecraft -g minecraft -m 0640 \
  /srv/mc-data/incoming/具体文件.jar \
  /srv/mc-data/minecraft/mods/具体文件.jar
systemctl start minecraft
tail -n 80 /srv/mc-data/minecraft/logs/latest.log
```

看到这次启动的 `Done (...)!` 后再发布客户端清单。`systemctl is-active` 只说明进程还活着，不等于 MC 已加载完成。同步维护服务器的 `friends-server-mods.json` 记录，它只是记录文件，不会自动控制启停。

更新已有模组时，先把旧 JAR 移到 `mods` **以外**的备份目录，避免两个版本同时加载。不要把 old.jar 留在 mods 内仅换一个仍以 .jar 结尾的名字。

## 06 · 发布给所有 Start 用户：必须走完的流程

下面以“下一次整合包 0.4.2”为例。它不升级客户端工具，不改 Minecraft，也不新建 GitHub Release。

### A. 生成并提交整合包

```powershell
python tools/manage-pack.py bump 0.4.2
git diff --stat
git add pack
git commit -m "Pack 0.4.2: describe mod changes"
$packCommit = (git rev-parse HEAD).Trim()
git push origin main
```

如果改了作者说明、许可文件、镜像许可条目，也将对应文件纳入提交。不要提交 `.mod-cache`、账号、密钥、世界或游戏本体。

### B. 更新发布入口，指向刚才的固定提交

```powershell
python tools/manage-pack.py channel $packCommit
git add channel.json
git commit -m "Publish pack 0.4.2"
git push origin main
```

辅助工具直接从 **Git 已提交的文件内容**生成 channel 指纹，避免 Windows CRLF 与 GitHub LF 不同导致发布失败。这里只计算小清单的版本标识，不会重新下载所有模组。

不允许让 packUrl 指向 main，也不要先发布 channel 再提交 pack。两次提交的目的就是使 channel 始终引用一个已经存在且不会随主分支变化的快照。

### C. 等待国内清单发布成功

打开仓库 Actions → `Publish COS update mirror`，看这次提交的 **publish 作业**是否成功。它会上传清单与获准镜像的模组，最后更新 channel。完整客户端不再作为这一步的前置工作。

可以仅打开 [国内当前版本清单](https://friends-mc-downloads-1318356926.cos.ap-beijing.myqcloud.com/friends-mc/channel.json) 看 release 是否已改变，无需下载全部模组再验一遍。如果浏览器可能缓存，给 URL 加 `?t=当前时间数字`。

**GitHub 提交成功 ≠ COS 发布完成。**COS 仍有旧清单时，Start 正常优先读取旧清单，不会因为 GitHub 已更新就强行跳到新版。

### D. 告诉玩家这样做

> 退出 Minecraft 和启动器，双击原文件夹里的 Start.cmd；Mac 双击 Start.command。保持需要的可选项勾选，等更新完成，再打开 FriendsMC 实例进服。不用下载新的完整包。

如果此次内容影响进服兼容性，在服务端维护窗口发布，并让大家更新后再回服。当前没有维护公告自动发送或强制版本准入，不要假定已在线玩家会自动更新。

## 07 · 更新、删除与个人模组

### 更新单个模组

用新版本 ID 再运行 upsert，同一项目保留原内部 ID。确认前置库是否升级；服务端先替换适用文件，随后执行第 06 节发布。不要只换 filename 而沿用旧 URL、大小或指纹。

### 删除托管模组

```powershell
python tools/manage-pack.py remove 要删除的内部ID
python tools/manage-pack.py bump 0.4.2
```

若其他模组依赖它，工具会拒绝删除，先处理上游。之后按第 06 节提交、切换 channel、发布。朋友下次 Start 会把已托管旧文件移出加载位置；个人自装且未被托管的 JAR 不会被统一清空。

删除带方块、物品、实体、维度的模组，可能改变存档内容；先保留停服备份。客户端能自动删 JAR，不代表存档内容可无损删除或恢复。

### 玩家个人模组

Windows 当前实例位置：`FriendsMC/launcher/.minecraft/versions/FriendsMC-26.3-26.3.0.26-beta/mods/`。

Mac 当前实例位置：`FriendsMC/launcher-data/instances/FriendsMC-26.3-26.3.0.26-beta/.minecraft/mods/`。

放入额外模组前退出游戏。适合额外放纯客户端、同版本 NeoForge 模组；不能额外装一个服务端没有的内容模组就要求服务器识别其方块。

不要手工替换托管模组为自己喜欢的版本，下次 Start 可能恢复服主发布的版本。重复 modId 提示时，把个人旧 JAR 移出 mods，再运行 Start。当前重复检测并不能覆盖所有内嵌模组和所有冲突。

## 08 · 国内模组下载怎样维护

`download-sources.json` 定义 COS 地址。客户端先尝试 `mods/<sha256>.jar`，没有镜像文件时再尝试清单中的作者 URL；因此看到一次镜像 404 后成功换源，不代表安装失败。

当前 FerriteCore 从作者 Modrinth CDN 下载，已获准镜像的其他模组走 COS。需要把新增模组放到 COS 时：

1. 确认该具体文件的再分发条件；这不是兼容性检测，不能用“跳过哈希”代替作者许可。
2. 将其 SHA-256 → 授权说明链接加入 `mirror-approvals.json`，把要求附带的许可证放入 `mirror-licenses/`，更新 `MOD-CREDITS.md`。
3. 提交这些文件，触发国内发布。发布程序会按主清单下载并上传许可范围内的文件。
4. 不新增完整客户端 ZIP 镜像任务，不需要分块上传权限来完成普通小模组清单更新。

新增模组只有 CurseForge 或其他下载地址时：当前 helper 与更新器不能直接接入任意 URL。先找到作者官方 Modrinth 发布；没有的话，需要扩展更新器 URL 支持，并给旧用户发一次更新器补丁，或在遵守许可的前提下改造为受支持的托管下载机制。不要填一个伪造 Modrinth 地址。

## 09 · 日常服务器管理

### 连接与服务操作

```powershell
# Windows PowerShell
ssh -i "$env:USERPROFILE\.ssh\rainyun-rgs-mc-server.pem" -p 46000 root@wze.rainplay.cn
```

```bash
# 以下在 Ubuntu SSH 中执行
systemctl start minecraft       # 启动
systemctl stop minecraft        # 正常停止并保存
systemctl restart minecraft     # 重启，会断开在线玩家
systemctl status minecraft --no-pager
tail -n 80 /srv/mc-data/minecraft/logs/latest.log
tail -f /srv/mc-data/minecraft/logs/latest.log
```

Ctrl+C 退出 `tail -f` 不会停止服务器。不要重复手工运行 run.sh，否则可能抢端口或占用同一个世界。systemd 的 on-failure 负责异常退出后的重启；手动 stop 会保持停止。崩溃反复重启时先查日志，处理原因后才 reset-failed/start。

`journalctl -u minecraft -n 80 --no-pager` 可查看服务启动失败原因；游戏加载情况优先看 `logs/latest.log`；崩溃报告在 `crash-reports/`（存在时）。

### 白名单、OP 与游戏内命令

由已有 OP 在游戏聊天框执行：

```text
/whitelist add 玩家名
/whitelist remove 玩家名
/whitelist list
/op 玩家名
/deop 玩家名
/difficulty normal
/list
```

控制台命令通常不带开头的 `/`，但当前 SSH shell **不是 MC 控制台**。不要在 Linux 提示符里直接输入 whitelist。当前 RCON 关闭；要网页控制台或远程命令功能，先单独部署并配置管理方式，不能假定现在已经有面板。

### 当前参数与常用调整

| 参数 | 当前值 / 起点 | 影响 |
|---|---|---|
| difficulty | normal | 普通难度 |
| allow-flight | true | 已开启，关闭原版飞行检测踢出；不赋予生存飞行能力 |
| max-players | 10 | 最大在线人数 |
| view-distance | 10 | 发给玩家的区块视距，影响内存与带宽 |
| simulation-distance | 6 | 模拟范围，影响 CPU 负载 |
| spawn-protection | 0 | 出生点不设原版方块保护 |
| white-list / enforce-whitelist | true / true | 白名单访问 |
| online-mode | true | 正版账号验证 |
| server-port | 25565 | 内部端口；朋友仍填外部 21250 |
| server-ip | 留空 | 不填公网 NAT 域名 |
| user_jvm_args.txt | -Xms2G / -Xmx6G | 为 8 GB 系统留出非堆和系统空间 |

修改 `server.properties` 前停服、备份原文件、编辑、启动。运行中修改可能被覆盖或不生效。遇到负载先将视距试降到 6–8、模拟距离 4–6，再看真实游玩表现；人数和模组数量不是可靠容量保证。

PVP 的设置位置随版本可能变化，本次只读检查没有在 server.properties 中看到 pvp 键；不要仅凭添加 `pvp=true` 就认定生效。要调整时应检查当前版本实际支持的游戏规则、命令补全或相关模组配置，再在游戏中确认。

## 10 · 备份与回滚

### 停服备份：一次保存整套可恢复状态

这是修改模组、迁移世界前的备份模板。备份在数据盘同级目录，不会递归打包自身。执行后服务器保持停止，方便接下来修改；若备份报错，先解决空间或路径问题，不继续改服。

```bash
set -eu
mountpoint -q /srv/mc-data
systemctl stop minecraft
mkdir -p /srv/mc-data/backups
stamp=$(date +%Y%m%d-%H%M%S)
backup="/srv/mc-data/backups/minecraft-${stamp}.tar.gz"
tar -czf "$backup" -C /srv/mc-data minecraft
echo "备份：$backup"
```

如果只是做备份，完成后 `systemctl start minecraft`。更新稳定后再按保留策略清理旧备份，不在这里自动删除。把重要备份另存到自己的电脑；同一块数据盘里的备份不能抵御整盘损坏。

```powershell
# Windows：替换为真实备份文件名
scp -i "$env:USERPROFILE\.ssh\rainyun-rgs-mc-server.pem" -P 46000 root@wze.rainplay.cn:/srv/mc-data/backups/minecraft-实际时间.tar.gz .
```

### 服务端恢复

先停服。保留故障目录，确认归档确实含顶层 minecraft 目录，再恢复此前可信备份；不要在运行中的世界上直接覆盖文件。

```bash
systemctl stop minecraft
stamp=$(date +%Y%m%d-%H%M%S)
mv /srv/mc-data/minecraft "/srv/mc-data/minecraft-failed-${stamp}"
tar -xzf /srv/mc-data/backups/minecraft-实际时间.tar.gz -C /srv/mc-data
chown -R minecraft:minecraft /srv/mc-data/minecraft
systemctl start minecraft
```

路径和备份名必须替换正确。版本迁移如果改了 systemd unit/drop-in、Java 路径或端口映射，也需要一起还原；仅恢复 minecraft 目录不会恢复 `/etc/systemd/system`。

### 客户端版本回滚

服务端回滚后，恢复对应的旧 channel.json 并提交，让 COS 发布旧入口。运行 Start 的玩家会回到旧托管 JAR。**这不自动回滚世界、玩家个人配置、旧加载器实例或更新器本身。**删除内容模组导致的存档变化，依靠对应世界备份恢复。

```powershell
# 在维护仓库中；替换为包含正确旧 channel 的提交
git restore --source=旧发布提交 -- channel.json
git add channel.json
git commit -m "Rollback pack channel"
git push origin main
```

## 11 · 导入别人制作的整合包

### 先判断是否能直接纳入当前流程

| 整合包情况 | 处理 |
|---|---|
| 同 MC + NeoForge，仅增加兼容 JAR | 可转换到现有主清单 |
| 同加载器，但依赖 config、KubeJS、任务文件等 | 先扩展文件同步能力，不能只导入 JAR |
| 其他 MC / NeoForge 版本 | 按第 13 节新建测试实例，再迁移 |
| Forge / Fabric / Quilt | 当前 Start 不支持；先升级客户端加载器支持或采用另一套客户端 |
| .mrpack / CurseForge manifest.zip | 是清单/覆盖文件容器，不是全部 JAR；导入启动器不等于接入本仓库同步 |

具体步骤：

1. 下载作者提供的服务端包和对应客户端包，锁定同一个整合包版本，不要混用不同日期的客户端与服务端。
2. 在 `/srv/mc-data/minecraft-staging/` 创建独立测试服；使用新世界和独立端口，不碰正式世界。
3. 按作者指定的 Java、MC、加载器启动；复制服务端包自带的配置、脚本、默认配置、数据包等。不要只拿 mods 文件夹。
4. 本地创建对应客户端实例，确认作者包能够正常进测试服。对重型包至少实际走动、生成新区块、体验主要玩法，单纯出现 Done 不代表负载可用。
5. 把客户端所需 JAR 转成 mods.lock 条目，并明确必装、可选、依赖；服务器独有文件单独管理。来源不受当前更新器支持的文件，先解决下载支持。
6. 盘点 JAR 以外文件：哪些是共享强制配置，哪些只首次安装，哪些必须保留玩家改动。完成第 12 节的更新器扩展后，才宣称该整合包能靠 Start 全量同步。
7. 为所有玩家分发一次支持该包的客户端升级补丁，之后由 Start 接管；如果已跨加载器/Java，则可能需要新完整包。
8. 安排正式服维护，备份旧服，切换新服与发布清单，让玩家经 Start 更新后进入。大型整合包优先新世界，旧世界迁移不是默认无损操作。

所谓“脆骨症级别”需要按具体包版本实测。当前 8 GB 总内存并不保证能支撑 5–10 人的重型包；整合包作者的服务器建议、实际内存、持续 MSPT/TPS、探索压力比“200 个 mod”更有参考价值。

## 12 · 要让整合包配置也一键同步，必须补哪些能力

**以下是待开发要求，不是现有功能。**单纯把 config 目录提交 GitHub，当前 Start 不会自动下载它。

在保留现有入口的前提下，可扩展为统一文件清单，分别标记：

- `managed`：服主强制管理的玩法脚本/指定配置，更新前备份后替换。
- `seed`：只首次创建，例如玩家可自由调整的 UI 默认配置；已有文件不覆盖。
- `preserve`：账号、按键、地图、截图、存档等玩家数据，不接管。
- `server`：仅部署服务器，玩家不下载。

待实现清单至少包含相对路径、下载地址、版本标识、客户端/服务端作用域、更新策略；需要处理移除文件、回滚、相对路径边界和只覆盖托管文件。Windows 和 Mac 必须使用同一套规则，先发一次更新器补丁，再发布依赖这些能力的整合包。

若要求玩家永远只运行原 Start 文件就能升级更新器本身，还需要实现独立的 bootstrap 自更新机制。现有 0.4.1 没有自动替换自身脚本与 Java 的功能，因此不能承诺跨所有版本都永远不用补丁。

不要全量覆盖玩家 `.minecraft`，也不要同步 `.friends-sync`、`state` 或其他玩家的选择记录。服务端自行同步某些 config 的能力由具体模组提供，不能推广成所有文件都自动同步。

## 13 · 迁移 Minecraft、NeoForge 或 Java

### 仅升级 NeoForge 或 MC，仍使用当前支持的 Java 与 NeoForge 架构

1. 列出每个模组在目标 MC / NeoForge 上的可用版本。任一必需内容模组缺失，就不能直接迁移现有玩法。
2. 停服备份之外，先在 staging 目录安装目标加载器，用新世界试运行；需要测试旧世界时复制停服备份，别直接让新版本打开正式世界。
3. 将对应客户端清单中的 minecraft、neoforge 及全部模组条目一起更新，运行 build-pack.py，再按固定提交发布入口。
4. 当前 Start 以 MC + NeoForge 版本生成实例目录名，会为变化后的版本准备新实例。首次仍由 PCL / Prism 下载游戏资源，可能出现安装确认；不是原目录无缝覆盖。
5. 玩家确认新实例可进测试服后再切正式服。地图标记、截图、按键等需要单独选择性迁移，不自动复制不兼容配置。
6. 留下旧实例和旧世界备份；回退到旧游戏版本时使用旧备份，不让旧版直接打开已被新版写入的世界。

安装新的 NeoForge 服务端，应在新目录使用目标官方 installer 的 `--installServer`，调整 `user_jvm_args.txt` 并使用生成的 run.sh。具体 Java 版本按目标版本要求选择。[NeoForge 服务端安装文档](https://docs.neoforged.net/user/docs/server/)

### Java 大版本变化

当前 Windows 检查 channel.java 与 client.json 的 javaMajor，Java 更新器也按现有协议要求 Java 25。修改 channel.java 并不能升级玩家的 Java。

应先修改运行时下载源、客户端配置及更新器的版本检查，构建并分发新客户端或补丁，再发布要求新 Java 的整合包。服务器也要独立安装目标 Java 并修改 systemd 的 JAVA_HOME / PATH。不要只替换某个 java.exe。

### 跨加载器 / 转 Paper

当前 Start 的实例创建逻辑固定 NeoForge；在 JSON 里把 neoforge 改成 forge 或 fabric 不会得到可运行实例。需要相应加载器支持和新的客户端迁移方案。

服务端测试目录可通过新的 systemd drop-in 切换 WorkingDirectory / ExecStart，但这属于实际迁移步骤，不在编写本指南时自动执行。切换前记录旧 unit、Java 和端口，便于回滚。

## 14 · 插件怎么装，与 mod 有什么区别

**当前是 NeoForge 服，不能直接安装 Bukkit / Spigot / Paper 插件。**创建 plugins 文件夹并放 JAR 不会让它自动具备插件 API。Paper 的官方文档也明确区分 Paper 插件与 Forge/Fabric 模组。[Paper 迁移说明](https://docs.papermc.io/paper/migration/)

优先选择适配当前 NeoForge 版本的管理类模组；具体传送、领地、权限、备份功能先查作者支持版本。纯服务端管理模组不要求所有玩家安装，除非作者规定需要配套客户端。

如果明确要用 Paper 插件：

1. 另外建立 Paper 测试服，选择明确支持目标 Paper / MC 的插件。
2. 插件放入该 Paper 服 plugins 目录，按作者要求安装前置，启动生成配置，停服修改后重启。[Paper 安装插件文档](https://docs.papermc.io/paper/adding-plugins/)
3. 不把插件 JAR 发布给客户端 Start；它们是服务端组件。
4. 现有 Corpse、Carry On 等 NeoForge 玩法不能假定在 Paper 上保留；旧世界里的模组内容也不能假定能转换。
5. 若要保留当前模组服，使用独立世界和不同端口并行提供 Paper 服，不覆盖正式目录。

混合端不是任意插件和模组的兼容保证。先验证目标项目是否支持当前版本，再独立测试；不要为了一个插件直接更换正式服核心。

## 15 · 更新器、启动器和安装包本身怎样发布

普通模组更新不做这一节。只有改了更新器、Start、Java、加载器支持等才需要发布客户端工具。

1. 修改 `client-source/`、`mac-source/`、`updater/` 或 `build-release.py` 中对应实现；两端同步维护。
2. 提升 `CLIENT-VERSION`，例如 0.4.2；客户端版本与整合包 release 可以不同。
3. 更新 RELEASE-NOTES，说明旧用户是否必须覆盖补丁、玩家数据是否需要迁移。
4. 提交推送后，在 Actions 手动运行 `Build Windows and macOS clients`。它构建并发布 GitHub 完整包与两端补丁，不上传完整包到 COS。
5. 旧用户退出游戏后合并覆盖补丁；不要删除整个 launcher、launcher-data、state 或玩家世界。新用户直接解压新完整包。

更换 PCL 或 Prism 还要同步 THIRD-PARTY / sources 的准确版本、文件来源和构建要求；不要只改下载 URL 就忽略启动方式差异。PCL 是 Windows 端，Mac 继续使用 Prism。

## 16 · 常见问题与快速处理

| 现象 | 优先处理 |
|---|---|
| Start 没出现新模组 | 看国内 channel.release，确认 publish 作业成功；不要重下完整包 |
| 可选模组没安装 | 看该玩家是否取消了勾选；default 不覆盖旧选择 |
| 朋友进服提示缺模组/版本不符 | 对比正式服与已发布清单；让玩家退出并通过 Start 更新，不只打开 PCL |
| JEI 提示服务器没装 | 服务端安装相同适配 JEI 及 MezzConfig；本服已经处理 |
| ModernFix 提示 no_ferritecore | Start 勾选 FerriteCore；服务器安装并不能代替客户端安装 |
| 重复 modId | 移出个人旧 JAR；不要一边托管更新一边手工替换相同项目 |
| 类找不到 / 缺依赖 | 检查加载器、MC 版本、作者依赖、客户端模组是否误放服务器 |
| config 改了朋友没变化 | 当前只同步托管 JAR；按第 12 节扩展配置同步 |
| 更新失败后无法启动 | 保留更新日志，先关闭全部游戏进程，再运行 Start；不要清空整个实例 |
| 服务器 active 但进不去 | 看这次启动的 Done、latest.log、内部 25565 是否监听，再查外部映射 |
| 语音不通而游戏正常 | 检查 UDP 24454 → 51612 映射、voice_host、客户端语音模组和麦克风权限 |
| 想用降级解决问题 | 回滚对应世界备份与整合包清单，不让旧版直接读取新版存档 |

## 17 · 服主每次更新只做这几件事

1. 选对 MC / NeoForge 文件，确定必装、可选、依赖、安装端。
2. 用 helper 更新主清单；普通模组不用构建客户端 ZIP。
3. 涉及服务端时备份、部署、启动一次，看 Done。
4. 提交 pack，再提交指向固定 commit 的 channel。
5. 等 publish 成功，通知玩家退出并运行 Start。
6. 有问题恢复服务端备份与旧 channel；没有新问题不重复做全量测试。

这些步骤保证发布流程可控，不保证任意模组组合或所有网络永不出问题。对于当前支持范围内的托管 JAR，玩家更新入口始终是原 Start；超出范围的整合包配置、加载器与 Java 迁移，先扩展客户端能力，再发布内容。

## 18 · 权威参考与实现依据

- [本项目仓库](https://github.com/chengweialan/mc-friends-client)：`updater/FriendsUpdater.java`、`client-source/scripts/Common.ps1`、`Start.ps1`、`build-pack.py`、发布工作流。
- [NeoForge 服务端安装](https://docs.neoforged.net/user/docs/server/)：安装器、run.sh、内存参数和整合包服务端准备。
- [NeoForge 客户端安装](https://docs.neoforged.net/user/docs/client/)：客户端加载器安装说明。
- [packwiz 原生安装器](https://packwiz.infra.link/tutorials/installing/packwiz-installer/)：与本项目自定义同步器的区别。
- [Paper 迁移](https://docs.papermc.io/paper/migration/)及[安装插件](https://docs.papermc.io/paper/adding-plugins/)：插件与模组服务端的边界。

以上为本次查阅与当前代码状态；以后更新器升级后，以新的实现和说明为准。
