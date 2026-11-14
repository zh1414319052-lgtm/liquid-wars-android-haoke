# 液态战争重制版 · Haoke Liquid Wars OS

Android 正式版 **1.85 / versionCode 55**（2026-11-14）。Java/C++/GLES，Android 8.0+，GPL-3.0-or-later。支持 arm64-v8a、armeabi-v7a、x86、x86_64。

[安装包与软件详情](https://www.haoke.org/software/f2a585b84fbf5b85?platform=android) · [构建](BUILDING.md) · [操作和验证范围](docs/game/formal-1.85/RELEASE.md) · [来源与许可](THIRD_PARTY.md)

液体粒子流动、吞噬与包围仍是核心。点击目的地后松手继续行进；默认点选下长按或拖动会实时跟随，松手保留最后目标。画线模式规划路线，A/B/C 分队可先后下令，停止、分兵、合流不要求多指持续按屏。经典玩法保留，据点模式加入持续产液与争夺。49张普通地图、10张大地图、离线本地AI、可选模型队伍、五指操作和Wi-Fi/蓝牙入口继续可用。

本次整合1.80–1.85的改进：据点经营与分队路线；8种原创液体音效；道具图标去名称牌和白色外圈，16类限时效果统一10秒；移除移动箭头尖并减弱HUD遮挡；游戏内公网匹配与好友房（最多6席，不足由电脑补齐），16种预置表情交流；邮箱验证码登录、密码登录/设置/验证码重置；重做联机界面并修正主菜单左侧黑边；系统分享可交给微信、QQ等已安装应用，游戏分享统一使用上方下载详情链接，房间邀请保留房间信息。

1.85正式收尾新增可信签名更新：仅在主菜单前台自动检查，玩法说明底部可手动检查；对局与联机等待不弹更新。验证Ed25519签名、包名、通道、发布证书、版本、有效期及设备兼容性，支持稍后提醒和忽略版本。不自动安装，无新增安装权限或后台服务。

公网账号与中继使用 cleanbizai.com。客户端以房主运行模拟、服务器转发消息；房主离开会结束对局，目前没有房主迁移、战斗断线续局或中途加入。仅发送预置表情，不开放自由文字。交流群：1136036532。

正式APK：**239,243,436 bytes**；包名 `com.xenris.liquidwarsos.haoke`；协议30。

SHA-256：`7a47e7ba65ea33826e0d6b5095baa3a9681b711516e16b9aae36c6ba6300dc6e`

发布证书SHA-256：`4CA5A614020E7B450A504FBF2FEC33242AD071D25443604A3B8DAE7C0801AEDE`。

本次39项更新单元/生命周期测试、Release构建与lint通过；OPPO真机对最终非调试签名包验证了实际签名接口、自动/手动检查、战斗启动、暂停恢复、同包覆盖安装和合成设置保留。此前1.85 QA验证了联机UI、分享和密码流程；1.84进行了两Android客户端/四AI和六连接中继检查。**没有完成六台实体手机同时对战，也没有长时间热稳定性、全部59图、人类平衡试玩或iOS/macOS/Windows原生版本验收。** 自动测试不代表已经证明好玩。

运行素材完整保存在Git。全部素材原件、已选设计基准和历史备选在同版本Release附件 `liquidwars-1.85-art-source.zip`，解压到仓库根即可恢复 `art/`，无需旧版附件。下载入口由站长维护，源码发布不表示已代传或核验网盘中的APK。私钥、网站凭据和玩家数据不在仓库。

## English

**Release 1.85 (55)** is a native Android 8+ liquid-army strategy game. Tap to keep moving, hold/drag to follow your finger, or draw persistent routes. Sequential squad commands and capture/production objectives complement the classic encirclement battles. This release consolidates the 1.80–1.85 gameplay, subdued HUD, ten-second timed items, procedural audio, six-slot Internet matchmaking/friend rooms with AI fill, preset emotes, email/password/reset flows, redesigned lobby, cutout fix and system sharing. Optional model teams remain separate from offline AI.

Signed update checks run only in the foreground main menu or on an explicit About action. Authentication, expiry, identity and compatibility are verified before presenting a release; no automatic installation or background update service is added. The final four-ABI APK and publisher fingerprint are listed above. Build with the pinned toolchain in BUILDING.md. Runtime assets are included; the matching art archive restores the actual editable/raster originals without contacting image-generation services.

Validation includes 39 focused update/lifecycle tests and final signed-package checks on an OPPO phone, including real-feed verification, gameplay entry, pause/resume and data retention across an in-place reinstall. Earlier two-client/four-AI relay checks are not a six-physical-phone test. Host migration, battle reconnect and mid-game joining are not implemented. Long-duration thermal behavior, all-map balance and other native platforms remain unverified.

Original Liquid Wars OS copyright (C) 2013 Henry Shepperd and contributors; [upstream source](https://github.com/xenris/liquid-wars-android). Haoke modifications retain GPL-3.0-or-later and original attribution. Separate dependency/font licenses are preserved. See LICENSE and THIRD_PARTY.md.
