# 2.07 本地正式版（2026-11-18）

- 可选摇杆移动指挥点，经典/3D共用，松手保留目标，默认仍为跟随。
- 黑洞前1.5秒预警、1秒渐进、吸入放慢，增加细引力线；仍被障碍阻挡且可净化。
- 吞噬声音使用用户提供素材，黑洞保留暗涡。
- 正式包77，原包名/签名；公网账号与匹配保持移除。更新通知、网站及GitHub发布待用户后续指令。

# 2.04

移除经典和 3D 公网联机、好友房、匹配、游客注册、邮箱注册登录和密码重置。Wi-Fi 多人对战仅允许直连局域网地址。保留蓝牙、单机和本地 3D 远征。正式包沿用已有签名，versionCode 74。

# 2.03 · 2026-11-17

沿用正式签名和包名，Android versionCode 73。3D 使用经典粒子、560 格默认远景及自然林地外观，主菜单提示仍在优化。公网大厅重排；加入时输入房间码，创建后显示自动生成的码。经典公网同步减少等待阻塞；新增支持游戏和浏览器打开 QQ 群邀请。客户端更新通知待用户上传网盘后另行发布。

# 1.94 · 2026-11-16

Formalizes the accepted 1.93 gameplay and Godot/HUD/devouring effects, retains publisher identity, adds English fallback for new HUD, protects Godot JNI/reflected methods from R8. Version64, protocol193. Client-update notification explicitly deferred.

# 1.85 正式版 · 2026-11-14

整合1.80–1.85的据点/分队路线、长按跟随、10秒道具、音效、弱化HUD、公网匹配/AI补位/表情、密码与验证码重置、联机UI、全面屏及系统分享。新增仅安全前台检查的签名更新。版本55，协议30，原发布证书。详细范围、证据与边界见 README 和 docs/game/formal-1.85/RELEASE.md。

# 更新记录 / Changelog

##1.79 · 2026-11-08

离线本地战术AI增强；保留1.78触屏与镜头。修正卡边缘与传送隐藏占位读取；错峰规划、地形和道具边缓存。现有战斗规则、协议28、权限、依赖和地图素材不变。

标准/困难战术更新，休闲保留。192局真实核心对照和MuMu2检查，失败与限制见docs/game/local-ai-1.79/RESULT.md。来源、构建及原件同时发布，未进行客户端通知。

Offline tactical AI improvement; existing camera, authoritative rules and assets retained. See the bilingual README and evidence report for measured scope and limitations.
