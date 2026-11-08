# 来源、许可与对应源码 / Provenance

- Liquid Wars OS：Henry Shepperd 等原作者，源自 https://github.com/xenris/liquid-wars-android 。保留原作者版权和历史；游戏核心、界面及本项目改动按 GPL-3.0-or-later 发布。当前根 LICENSE 为 GPLv3 全文。源文件中更具体的第三方许可保持有效。
- 浩科改动：中文界面、地图/粒子/道具、联机扩展、控制与镜头、离线战术 AI；2026-11-08 的 1.79 修改仅限本地 AI 和相应说明/验证，不改战斗规则或 1.78 镜头。
- ODE：Russell L. Smith 等，源码保留在 `app/src/main/cpp/ode`。各源文件声明 LGPL-2.1-or-later 或 BSD 双许可；此分发选择 LGPL-2.1-or-later，全文见 `third_party/ode/LGPL-2.1.txt`。没有替换既有库二进制，完整随附源码仍可构建。
- LodePNG / aclib：保留各文件原有作者与许可声明，随对应源码提供。
- Grafika：Apache-2.0；固定提交及修改说明在 `third_party/grafika/NOTICE.md`，原始参考文件与许可全文一并提供。
- Noto Sans SC：SIL OFL 1.1，允许商用，全文在 `app/src/main/assets/licenses/NotoSansSC-OFL.txt`。本项目 UI/证据排版使用该字体。
- AndroidX / Material：现有 Gradle 固定版本、Apache-2.0；依赖见 `app/build.gradle`。Android SDK/NDK 是开发工具，NDK libc++ 保留其 LLVM 许可与例外，官方源码与 notices 随对应 NDK 发布。未新增运行依赖。
- 本项目生成的地图/粒子/图标原件与 SVG、碰撞资料见 `art/ASSETS.csv` 和同版本 art-source Release 归档；项目作者提供的素材及派生几何随 GPL-3.0-or-later 项目发布，生成内容不保证独占性。原件无需调用生成服务即可取得。

本轮核实了 behaviac、gdx-ai、MicroRTS，但没有复制或接入它们的运行时代码。实际方案为自行实现的局部态势评分、连续计划和网格寻路；固定核实提交及拒绝接入原因见 `docs/game/local-ai-1.79/RESEARCH.md`。

No private signing identity, credentials, player data or paid API service is required to build the source. Existing optional model-provider code is retained but is not used by the offline AI or this test campaign.
