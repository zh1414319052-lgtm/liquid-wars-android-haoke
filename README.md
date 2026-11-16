# 液态战争重制版 · Haoke Liquid Wars OS

Android **1.94 / versionCode 64**，GPL-3.0-or-later。原发布包名与证书不变；Java/C++ 模拟、GLES经典场景、Godot 4.7.2三维远征。Android8+，四ABI。

[软件下载详情](https://www.haoke.org/software/f2a585b84fbf5b85?platform=android) · [构建](BUILDING.md) · [来源与许可证](THIRD_PARTY.md)

本版整合已验收试玩：Godot十倍新大陆、默认远景400格、经典2D粒子、仅动态河流；游客直接匹配及可选邮箱登录；经典最多6席、远征2–10席，不足60秒电脑补齐；两种主动道具可积累至各10个，全体敌人冻结10秒，受冻仍可净化。保留自动道具、金币与装备、占点和分队。

紧凑半透明HUD，大地图/3D相同按钮布局及可展开小地图；3D整轮道具吃完才刷新下一轮，金币单独刷新。道具与金币使用真实接触吞噬进度，最快约2.1秒，状态驱动吞噬碎粒、冻结、净化与产兵反馈。默认跟随及用户选择会保存。

对应源码公开不等于新安装包已经上传网盘。**站长自行上传APK；客户端更新通知本次暂不发布**，原稳定更新feed保持不变。源码和素材由此仓库及v1.94的配套资源附件组成，不含管理员凭据、签名私钥或玩家数据。

公网服务 cleanbizai.com负责账号和匹配中继；软件详情/下载与签名更新在haoke.org。房主模拟、服务端中继；没有房主迁移、战斗断线续局或中途加入。游戏交流群1136036532。

## Source and build

Checkout tag v1.94 and extract the matching `liquidwars-1.94-resources.zip` at the repository root. It contains actual editable artwork; runtime data is in Git, not links or placeholders. Follow BUILDING.md. Signing keys remain outside source; self-builds cannot replace the publisher's installed app without the same signing identity.

The Android release preserves the original gameplay and publisher identity. Guest matchmaking is available without email; classic supports up to six seats and expedition up to ten, with AI fill after 60 seconds. Signed client-update publication is deliberately deferred until the owner uploads the installer and requests notification. This repository distributes corresponding source, not the APK.

Validation scope and limitations are recorded in `docs/game/formal-1.94/RELEASE.md`. Simulator checks are not physical-device performance or ten-human-player validation.

Original copyright (C) 2013 Henry Shepperd and contributors; [upstream](https://github.com/xenris/liquid-wars-android). Haoke modifications retain GPL-3.0-or-later and attribution. Noto Sans SC uses SIL OFL 1.1. Other dependencies and original art retain their included licenses.

APK: 630769374 bytes; SHA-256 `8267e898e889034d02ae328a9aa3e6d01b0dc2812e3c483f65545a91ad564e4d`. Publisher certificate `4CA5A614020E7B450A504FBF2FEC33242AD071D25443604A3B8DAE7C0801AEDE`.

Final validation: 43 JVM tests, four-ABI release/R8 + lint; MuMu2 signed-package update checks, classic battle, pause/resume, in-place upgrade and Godot six-seat local-AI scene passed. ARM device, ten human connections and prolonged load remain unverified.
