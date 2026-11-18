# 来源、许可与对应源码 / Provenance

- Liquid Wars OS：Henry Shepperd 等原作者，源自 https://github.com/xenris/liquid-wars-android 。保留原作者版权和历史；游戏核心、界面及本项目改动按 GPL-3.0-or-later 发布。当前根 LICENSE 为 GPLv3 全文。源文件中更具体的第三方许可保持有效。
- 浩科改动：中文界面、地图/粒子/道具、联机扩展、控制与镜头、离线战术 AI；1.80–1.85新增据点、路线/分队、原创音效、公网接入、账号界面和签名更新；保留原作者归属。
- ODE：Russell L. Smith 等，源码保留在 `app/src/main/cpp/ode`。各源文件声明 LGPL-2.1-or-later 或 BSD 双许可；此分发选择 LGPL-2.1-or-later，全文见 `third_party/ode/LGPL-2.1.txt`。没有替换既有库二进制，完整随附源码仍可构建。
- LodePNG / aclib：保留各文件原有作者与许可声明，随对应源码提供。
- Grafika：Apache-2.0；固定提交及修改说明在 `third_party/grafika/NOTICE.md`，原始参考文件与许可全文一并提供。
- Noto Sans SC：SIL OFL 1.1，允许商用，全文在 `app/src/main/assets/licenses/NotoSansSC-OFL.txt`。本项目 UI/证据排版使用该字体。
- AndroidX / Material：现有 Gradle 固定版本、Apache-2.0；依赖见 `app/build.gradle`。Android SDK/NDK 是开发工具，NDK libc++ 保留其 LLVM 许可与例外，官方源码与 notices 随对应 NDK 发布。
- 1.85签名更新：从浩科WPGen维护版复用GPL-3.0-or-later原生更新模块及测试，使用本游戏独立的软件/包/通道绑定。EdDSA-Java 0.3.0（str4d），CC0-1.0；固定对应源码位于 `third_party/eddsa/eddsa-0.3.0-sources.jar`，许可全文与来源见同目录。
- 本项目生成的地图/粒子/图标原件与 SVG、碰撞资料见 `art/ASSETS.csv` 和1.85完整 art-source Release 归档；项目作者提供的素材及派生几何随 GPL-3.0-or-later 项目发布，生成内容不保证独占性。原件无需调用生成服务即可取得。

本轮核实了 behaviac、gdx-ai、MicroRTS，但没有复制或接入它们的运行时代码。实际方案为自行实现的局部态势评分、连续计划和网格寻路；固定核实提交及拒绝接入原因见 `docs/game/local-ai-1.79/RESEARCH.md`。

No private signing identity, credentials, player data or paid API service is required to build the source. Existing optional model-provider code is retained but is not used by the offline AI or this test campaign.

1.90 QA使用Godot Engine 4.7.2，MIT许可，Android依赖固定为`org.godotengine:godot:4.7.2.stable`。引擎许可证、第三方版权列表和对应Android接口源码jar位于`third_party/godot-4.7.2/`；APK内含`assets/licenses/Godot-MIT.txt`。来源：https://github.com/godotengine/godot/tree/4.7.2-stable 。Godot的MIT许可不改变本项目原有GPL-3.0-or-later许可。此次未引入外部付费模型、贴图或字体；HUD继续使用已归档SIL OFL1.1的Noto Sans SC。


## 1.95 QA ground material
Aerial Grass Rock by Rob Tuytel / Poly Haven, CC0 1.0; commercial use and redistribution allowed. https://polyhaven.com/a/aerial_grass_rock and https://polyhaven.com/license . Source maps and provenance: art/source/surface-1.95; runtime albedo and OpenGL normal map: app/godot/assets/meadow-*.jpg.

## 2.05–2.07 黑洞及音效
黑洞像素源稿、标识、选稿及生成脚本在 `art/source/black-hole-2.05`。原创「暗涡」与其他音效设计在 `art/source/audio-2.06`。2.07吞噬音轨由用户提供MP4提取，原件、转换方式和来源说明在 `art/source/audio-2.07`；第三方权利来源未独立核实，不宣称该音频本身为GPL授权。本轮仅向提供者交付本地安装包与对应源码，未公开上传。

2.12：用户音轨已从运行资源移除，不进入APK。历史audio-2.07原件仅本地维护，不随本版公开资源包分发，也不是构建依赖。主菜单设计及本地首次说明沿用Noto Sans SC（SIL OFL 1.1）；赞赏码是作者提供的公开游戏支持入口，保留其原始归属，不将码内头像重新声明为GPL素材。
