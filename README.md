# 液态战争重制版 · Haoke Liquid Wars OS

Android **2.13 / versionCode 86**，GPL-3.0-or-later。沿用正式包名和发布证书，Android 8.0+，支持 ARM64、ARM32、x86、x86_64。

[下载详情](https://www.haoke.org/software/f2a585b84fbf5b85?platform=android) · [构建](BUILDING.md) · [许可](THIRD_PARTY.md) · [本版验证](docs/game/formal-2.13/RELEASE.md)

本次改进左右摇杆：两侧共用原跟随触点，先按者控制，松开后另一侧可接管；摇杆模式下地图普通点按不再指挥粒子。设置入口统一放在下方“帮助”右边，经典、大地图和3D一致，小地图位置不变。可调整两侧位置、大小100%～250%和灵敏度50%～200%，支持保存、取消与恢复摇杆默认配置。设置跨对局、重启和同签覆盖安装保存；开局选择按模式分别记忆。

保留原版粒子核心、现有镜头与粒子尺寸、经典/大地图/Godot 3D、指挥和道具玩法。3D仍在优化中。冻结所有敌方10秒，净化被冻时仍可用且可解除黑洞吸引；黑洞中心释放10秒，自己也受影响且仍被障碍阻挡，主动道具各最多10个。现有主菜单、本地首次更新说明、自愿“支持游戏”及声音设置保留。

**公网账号与公网匹配已移除**；经典模式保留Wi-Fi局域网和蓝牙，3D使用本地AI。

正式APK：674027379字节；SHA-256 `0120d6ab83b90d97c23ebc2d9a0367f9f3dd6814c68df9d0938d48af709e906f`。包名 `com.xenris.liquidwarsos.haoke`，证书SHA-256 `4ca5a614020e7b450a504fbf2fec33242ad071d25443604a3b8dae7c0801aede`。

**直接覆盖安装原正式版，不要先卸载或清除数据。** 独立QA包与正式版数据隔离，不会自动迁移。清除数据、卸载或换设备不属于覆盖保留范围。作者已确认将2.13安装包上传原网盘入口，**2.13客户端更新通知于2026-11-18正式发布**（stable revision3，code86）。原下载入口保留；自动化流程未下载核验远端网盘文件。

本版68项release JVM测试、R8、lintVital和成品检查通过。MuMu1完成2.12→2.13正式覆盖升级；已有偏好文件哈希一致。再保存非默认摇杆大小/灵敏度/位置，同版本覆盖和冷启动后保持一致，并在3D界面看到相同值。真实2D/3D启动和设置入口通过。上一阶段3项QA设备交互测试另有记录；本轮未做实体手机或双设备局域网/蓝牙验收。

当前源码与v2.13的resources.zip共同提供构建材料和可编辑素材；在仓库根目录解压资源包恢复art/。历史已停用的用户音轨不是构建依赖，未随公开资源包分发；本地维护原件保留且不重定许可。

## English

Version 2.13 (86) keeps the original simulation, visual assets, camera and particle size. Either enlarged joystick controls the same follow point; ordinary map taps are isolated in joystick mode. Settings sits immediately after Help in classic, large-map and Godot 3D modes without moving the minimap. Pad size, sensitivity and normalized positions persist across matches and same-signature upgrades. Match setup choices are remembered separately per mode. Defaults apply only to missing settings.

The official identity and certificate remain unchanged. Install over the existing official app without uninstalling or clearing data. Separate QA installations do not share settings. Release build, R8, lintVital, 68 JVM tests and MuMu1 upgrade checks passed; a customized pad layout survived reinstallation and cold start. No physical-device or Wi-Fi/Bluetooth peer testing was performed in this release pass. See the release evidence for limits.

Public matchmaking and accounts remain removed. Classic local Wi-Fi/Bluetooth and 3D local AI remain. After the author confirmed the APK upload and authorized publication, the 2.13 client notification was published on 2026-11-18 (stable revision 3, code 86). Existing cloud-drive destinations are unchanged; the remote APK was not downloaded or verified by automation.

Copyright (C) 2013 Henry Shepperd and contributors; [upstream](https://github.com/xenris/liquid-wars-android). Original GPL notices remain. Noto Sans SC: SIL OFL 1.1. Godot: MIT. User-supplied materials retain their separate provenance.
