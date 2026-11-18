# 液态战争重制版 · Haoke Liquid Wars OS

Android **2.12 / versionCode 85**，GPL-3.0-or-later。沿用正式包名和发布证书，Android 8.0+，支持 ARM64、ARM32、x86、x86_64。

[下载详情](https://www.haoke.org/software/f2a585b84fbf5b85?platform=android) · [构建](BUILDING.md) · [许可](THIRD_PARTY.md) · [本版验证](docs/game/formal-2.12/RELEASE.md)

本版保留原版粒子核心和后续玩法，重新设计并对齐主菜单。每个新版本首次进入主菜单显示一次本地更新内容及道具技巧，可立即关闭、从底栏主动重看。新增主菜单「支持游戏」，自愿赞赏用于继续开发维护和未来服务器预算；移除粒子互相吞噬音效，黑洞声音及其他音效保留。

经典、大地图与 Godot 3D 模式保留；3D 仍在优化中。点击、路线、长按跟随、松手续行、分队、摇杆指挥点、AI、据点、金币装备保持。冻结所有敌方 10 秒；净化在被冻时可用，并可解除黑洞吸引。黑洞在地图中心持续 10 秒，影响自己，障碍阻挡不变。主动道具各最多 10 个。

**公网账号与公网匹配已移除**；经典模式保留 Wi-Fi 局域网和蓝牙，3D 使用本地 AI。以后是否开放公网功能及时间以实际公告为准。

安装包由作者上传至原网盘入口。**2.12 客户端更新通知暂未发布，等待作者上传后另行授权**；源码公开不表示网盘文件已经更新。当前源码与 v2.12 的 resources.zip 共同提供构建材料和可编辑素材，在仓库根目录解压资源包恢复 art/。历史已停用的用户音轨不是构建依赖，未随公开资源包分发；本地维护原件保留且不重定许可。

## English

Version 2.12 (85) retains the original particle core and the existing classic, large-map and Godot 3D modes. The main menu now uses aligned cards. Local release notes and item tips appear once per installed version and can be dismissed or reopened. Optional support funds ongoing development and a possible future server budget. Particle-versus-particle swallowing audio is removed; black-hole and other audio remain.

Public matchmaking and accounts remain removed. Classic modes retain local Wi-Fi/Bluetooth; 3D uses local AI and remains under refinement. The official Android package and signing certificate are unchanged. Client update notification is deliberately withheld until the author uploads the APK and authorizes notification. See the release report for actual tests and untested devices.

Copyright (C) 2013 Henry Shepperd and contributors; [upstream](https://github.com/xenris/liquid-wars-android). Original GPL notices remain. Noto Sans SC: SIL OFL 1.1. Godot: MIT. User-supplied materials retain their separate provenance.
