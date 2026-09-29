# Friends MC 国内更新源接入

当前阶段：更新器支持重试和镜像，但 download-sources.json 的 mirrors 仍为空。存储桶未配置前，仍然使用 GitHub / Modrinth，不能宣称已解决国内连接问题。

## 1. 创建专用存储桶

打开 https://console.cloud.tencent.com/cos/bucket ，创建仅用于客户端公开下载的存储桶：

- 名称：friends-mc-downloads（控制台会追加 APPID）。
- 地域：北京 ap-beijing，适合现有北京玩家；香港玩家的实际访问速度在接入后测试。
- 存储：标准存储；暂不开启多 AZ、全球加速或 CDN。
- 访问：公有读私有写。只放允许公开下载的文件，不放账号、密钥、世界备份或服务端配置。
- 保留 HTTPS 默认访问域名。费用告警按自己的预算设置；告警不等于硬性费用上限。

把桶名称与地域或默认域名发给维护者即可，不要在聊天里发送 SecretKey。

官方说明：https://cloud.tencent.com/document/product/436/14106

## 2. 准备发布文件

执行 `python prepare-mirror.py --output mirror-stage`。这个命令验证当前 channel.json 与模组清单的 SHA-256 一致，并将固定版本清单放入 releases/<commit>/pack。

默认不复制第三方模组。逐个确认作者的再分发许可及附带许可证要求后，使用 `--approvals 文件.json --cache 模组缓存目录`，审批文件格式为 `{ "模组SHA256": "https://许可证或授权说明URL" }`。将必要许可证说明一起放入 mirror-stage。mirror-report.json 列出已镜像和仍从作者地址下载的模组。仅镜像清单就能绕开本次 GitHub 清单超时，但未镜像的模组仍取决于作者下载源的连通性。

## 3. 上传并验证

发布机器安装 `cos-python-sdk-v5`，通过环境变量提供 COS_SECRET_ID / COS_SECRET_KEY（临时凭证另加 COS_SESSION_TOKEN）。使用仅限此桶 friends-mc 前缀的上传账号。不要放到客户端、仓库文件或日志。

运行：

```sh
python publish-cos.py --directory mirror-stage --bucket friends-mc-downloads-实际APPID --region ap-beijing
```

脚本先上传固定路径文件，逐个验证匿名下载内容，再最后发布 channel.json。上传失败应修复并重跑，不提前手动切换 channel.json。脚本不改变桶权限，也不删除旧版本。

## 4. 接入客户端

修改仓库根目录 download-sources.json：

```json
{
  "schema": 1,
  "mirrors": ["https://实际桶名称.cos.ap-beijing.myqcloud.com/friends-mc"]
}
```

重新打包 Windows 和 Mac，旧用户需覆盖一次新的 scripts/Common.ps1（Windows）、tools/friends-updater.jar、download-sources.json。Mac 只需要后两项。普通模组更新以后无需更换这些文件。

更新器三轮尝试，每轮先镜像、后原始地址；校验不符也会换源。模组全部验证通过后才替换，个人模组和用户配置继续保留。镜像清单保留 GitHub 固定提交地址作为版本标识，客户端不会因为该字段而强制从 GitHub 下载。

## 5. 验收后发布

在关闭代理的内地网络测试 Windows / Mac 的首次获取清单、模组下载、第二次无变化启动，以及镜像不可用时的备用源。首次游戏资源下载由 PCL / Prism 负责，需要单独验证，不能将模组同步成功视为整个首次安装已验证。

发布补丁及完整包到国内源后，再更新好友下载指南；保留 GitHub Release 为备用。当前尚未设置云账号，也未发布国内下载补丁。
