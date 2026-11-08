# 液态战争重制版 · Haoke Liquid Wars OS

当前正式版 **1.79 / 48**（2026-11-08），Android8+，Java/C++/GLES，GPL-3.0-or-later。

[安装包与软件详情](https://www.haoke.org/software/f2a585b84fbf5b85) · [构建说明](BUILDING.md) · [1.79实测报告](docs/game/local-ai-1.79/RESULT.md) · [许可与来源](THIRD_PARTY.md)

按住/拖动触点指挥液态队伍。保留49张普通地图、10张大地图、随机出生、六队/组队、20种道具、中英文、五指输入、Wi-Fi/蓝牙入口和可选模型队伍。本版增强完全离线的标准/困难本地AI，加入局部兵力与健康判断、进退/集结/支援比较、绕行重评估和道具反制；休闲仍容易上手。保留1.78触屏与镜头，不加电脑速度、伤害或数量。

最终真实核心192局：两队52胜7负，混战47胜16负，组队54胜7负，其余平局或临时混合联盟。不是人类胜率，8图种子集曾用于调优；有些窄地形仍会输。MuMu实例2验证与正式包覆盖安装通过；**真机未验证，没有人类试玩**，多机联机、长期性能及全59图未测。详细不确定性、失败案例、帧时口径见报告。

包名 `com.xenris.liquidwarsos.haoke`；APK **236,423,468 bytes /225.47MiB**；四ABI。SHA-256：
`1c26ef3bc728fa90c29f2b8eec1f436074fc87383a138e8b448ee828e042d7da`

正式证书SHA-256：`4CA5A614020E7B450A504FBF2FEC33242AD071D25443604A3B8DAE7C0801AEDE`。

完整运行素材在Git，编辑原件在同版本Release的 `liquidwars-1.79-art-source.zip`；恢复方法见构建说明。验证附件含原速App录像与原始采样。私钥、口令、玩家配置和本机凭据不在仓库。下载入口由站长维护，本轮源码发布不表示已代传网盘APK。

## English

Release **1.79 (48)** improves the fully offline Standard/Hard AI using local combat assessment, persistent plans, retreat/regroup/support choices, weighted grid routes and item/counter decisions. It preserves1.78 touch/camera behavior and authoritative rules. No new runtime dependencies or AI stat bonuses. Optional model-controlled teams never fall back to local AI.

The192 production-core comparisons cover8 maps, fixed seeds and swapped assignments. Results favor the new controller, with retained losses and uncertainty in the report. These are not human win rates or an unseen-map evaluation. MuMu2 checks passed; physical phones, multi-device networking and long-run performance were not tested. Build from included runtime sources/assets using the pinned Gradle wrapper; restore optional editable artwork from the release archive.

Original Liquid Wars OS copyright (C)2013 Henry Shepperd and contributors; [upstream](https://github.com/xenris/liquid-wars-android). Haoke modifications and tests are provided under GPL-3.0-or-later; third-party notices remain intact. This repository preserves upstream history and publishes the actual maintained Android source. See LICENSE for warranty and redistribution terms.
