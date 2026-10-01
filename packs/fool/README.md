# 愚者发布记录

这里保存愚者的版本通道、完整文件清单和原生 packwiz 索引快照，供 GitHub 追踪发布历史。

**实际安装地址以 `channel.json` 的 `packUrl` 为准，使用北京 COS。**本目录不包含约 2GB 的完整资源，也不是可直接使用 GitHub raw 地址安装的完整 packwiz 源。

- `channel.json`：MC / Forge / Java 版本、不可变整合包地址和运行库信息。
- `inventory.json`：所有托管文件的相对路径、版本指纹、大小及个人配置保留策略。
- `pack.toml` / `index.toml`：本次发布的索引快照；完整对应文件位于 COS 的发布目录。

本地完整源和发布目录由 `tools/import-fool.py` 从作者包准备。维护操作见仓库根目录 `FOOL-SETUP.md`。服务端与客户端分开部署，发布更新入口之前先准备好服务端和全部下载文件。
