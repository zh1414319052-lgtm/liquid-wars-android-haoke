# 构建 / Build

Android 8.0+；Java / C++ / GLES，未迁移引擎。四个 ABI：armeabi-v7a、arm64-v8a、x86、x86_64。

本版使用 Gradle 8.9、Android Gradle Plugin 8.7.3、JDK 21、Android SDK 34、NDK 28.2.13676358。使用本机已有工具或自行在用户目录配置；无需修改系统全局环境变量。

1. 将 `ANDROID_HOME` 指向 SDK，`JAVA_HOME` 指向 JDK；也可在不提交的 `local.properties` 设置 `sdk.dir`。
2. Windows：`gradlew.bat assembleDebug assembleRelease lintVitalRelease`。Linux/macOS：`sh gradlew assembleDebug assembleRelease lintVitalRelease`。
3. Debug 输出 `app/build/outputs/apk/debug/app-debug.apk`，包名带 `.debug`。Release 输出 `app/build/outputs/apk/release/app-release-unsigned.apk`。
4. 发布者使用已有私钥执行 zipalign / apksigner。仓库不包含签名私钥、口令或 API 配置。自行签名的包不能覆盖安装官方签名包；最终正式 APK 指纹见 README。

首次构建需从 Gradle/Google/Maven 下载固定依赖；这属于开发环境，游戏本地 AI 不发送网络请求。`--offline` 只适用于依赖已经缓存的机器。`app/src/main/assets` 包含完整运行素材，正常构建不需要重新生图或解压美术原件。

编辑素材时，从本版本 GitHub Release 下载 `liquidwars-1.85-art-source.zip` 并解压到仓库根目录，恢复 `art/source/`、SVG、PNG 原件、碰撞 JSON、提示词和素材登记。校验同 Release 的 SHA-256 清单。源稿包含历史停用素材，运行地图以 `art/ASSETS.csv` 和 app assets 为准。生成器在 `tools/`；部分历史生成器保留当时本机路径，重生成前按实际素材路径调整。当前运行素材全部已导出。

## 1.85正式版

版本1.85/code55，协议30。`gradlew.bat testDebugUnitTest assembleRelease lintVitalRelease -PuiQa185`执行当前39项更新/生命周期测试并构建正式变体；正式包签名由源码外原发布身份完成。源码不含签名凭据，也不能从源码恢复发布者身份。常规开发使用Debug独立包；`-PuiQa185`排除旧x86专用测试探针，允许ARM手机执行原生UI测试。`-PformalDevice185 assembleReleaseAndroidTest`生成针对release的测试组件，需使用同签名；仅在授权设备执行，Formal185DeviceTest写入合成测试标记，覆盖安装后upgradeKeepsSettings检查并删除它。

客户端更新固定软件ID、包名、stable通道和公开Ed25519信任锚。测试资源中的签名none响应是冻结验证fixture，不是实时更新数据；单元测试使用固定验证时间。实际APK使用当前时钟与公开接口。公开验签身份不是私钥。

联机客户端使用HTTPS/WSS公网服务；离线游戏无需账号或模型密钥。新运行时依赖为OkHttp4.12.0及EdDSA0.3.0；锁定版本、源码/许可证见third_party及app/build.gradle。网站后端为独立维护项目，不是Android APK构建输入，客户端协议和调用代码随源码提供。

Noto静态400/600字体：`python tools/build185_font.py`，需fontTools；原创8种音效：`python tools/build_audio181.py`，需NumPy。两个生成器都使用归档中的真实源文件，无临时目录依赖。1.85完整美术附件解压到本仓库根，不需叠加历史版本。设计JSON/参考图在art/source/online-1.85，运行界面以Java代码为准；用户表情仅有实际PNG原稿，未声称有分层绘图工程。

## 真实核心对照

`tools/local_ai179.cpp` 直接编译生产 State/Move/Map/Items；host GLES 仅提供编译桩，不绘图、不替换规则。Windows 可用 `tools/build_local179.cmd core`（按实际 VS 路径调整脚本首行）；需先建立 `build/local-ai-1.79/`。随后：

```text
build/local-ai-1.79/core.exe app/src/main/assets unit
python tools/bench_local179.py replay --full --seeds 4 --exe core
```

Python 3 标准库即可运行批量对照。工具拒绝覆盖同名实验结果。Java 定向测试 `tools/check179_java.py` 需要按本机调整 JDK 路径。Android instrumentation 使用 `assembleDebugAndroidTest`，相关探针在 `app/src/androidTest`；只能在自己授权的设备上运行。

## English

Build with the pinned Gradle wrapper, JDK21, SDK34 and NDK28.2.13676358. Set SDK/JDK paths per process or in an untracked local.properties. Run the Gradle tasks above. All runtime assets and native library source are included. The optional release art archive restores editable originals; it is not required for APK compilation. Signing keys and player API settings are deliberately excluded. A locally rebuilt APK has your own signature, not the publisher's identity.

The benchmark uses the production simulation, not an approximate RTS model. See the report for seeds, limits, information asymmetry and uncertainty. Android tests and desktop-core tests are different evidence; neither establishes physical-phone comfort or thermal performance.


## 1.80-QA 复现

沿用 Java/C++/GLES 与锁定依赖。Windows 本机可用 `python tools/build180.py 唯一日志名` 构建 Debug、测试辅助包、Release 和 lintVitalRelease；脚本固定本机 JDK21/SDK/NDK 路径，换机器需按开头说明配置。Debug 使用独立 `.debug` 包名。签名仍使用源码外原有证书，源码不包含私钥或口令。

规则测试：`tools\build_operations180.cmd 唯一名称`，再运行 `build\operations-1.80\名称.exe app\src\main\assets`；`placement` 参数检查59图据点。`python tools/check180_java.py` 检查命令/历史/协议。设备脚本只绑定 MuMu2 16448，不能不经核对换成其他用户手机。

新增HUD字体生成：`python tools/build_operations180_font.py`，依赖 fontTools；原件与 SIL OFL 在 `art/source/free-maps-1.60/fonts`。生成物已交付，正常编译无需重新生成字体。

## 1.81-QA 视听复现

构建命令为 `python tools/build181.py 唯一日志名`，同上固定工具链。`python tools/check181_java.py` 检查音效事件边界；真实App测试为 `Audio181ProbeTest`，设备脚本 `device181.py` 仅绑定已授权的MuMu2。声音已随运行assets交付，不需生成即可编译。

原创音效重生成：`python tools/build_audio181.py`，依赖 Python3 与 NumPy。8个48kHz/16bit/mono WAV位于 `app/src/main/assets/audio`；原速顺序试听和音色/哈希清单位于 `art/source/audio-1.81`，生成脚本及音频许可GPL-3.0-or-later。没有外部录音、运行时模型或音频下载依赖。试听文件中各段之间保留0.65秒间隔；设备操作录像不含内部音轨。


## 1.82-QA 跟随与10秒道具

`python tools/build182.py 唯一日志名` 构建四ABI Debug/测试包/Release与lint；`python tools/check182_java.py` 验证跟随命令合并及既有队列/历史帧/wire。生产核心用 `tools\build_follow182.cmd 唯一名称`，然后 `build\follow-1.82\名称.exe app\src\main\assets`，工具沿用现有Windows本机工具链路径。

实际App验证类为 `Follow182ProbeTest`，另覆盖 `Operations180ProbeTest#commandsAndCapture`、`Operations180RegressionTest`、`Audio181ProbeTest#realCaptureAndProductionCues`；`device182.py` 只绑定既有MuMu2 serial16448，不能改连用户其他手机。输出含原速录像、原始截图/JSON和APK绑定。

当前道具规则源仍在 `art/source/items-1.77/catalog.json`（历史目录名，内容已随1.82更新），16个正时长条目均10000ms。`build_items177_assets.py` 由此导出中英文说明；正常编译无需重生成素材。原生唯一时长常量 `Items::EFFECT_MS` 为10000。网络协议30，旧版29的20秒规则不兼容。
