# 构建 / Build

Android 8.0+；Java / C++ / GLES，未迁移引擎。四个 ABI：armeabi-v7a、arm64-v8a、x86、x86_64。

本版使用 Gradle 8.9、Android Gradle Plugin 8.7.3、JDK 21、Android SDK 34、NDK 28.2.13676358。使用本机已有工具或自行在用户目录配置；无需修改系统全局环境变量。

1. 将 `ANDROID_HOME` 指向 SDK，`JAVA_HOME` 指向 JDK；也可在不提交的 `local.properties` 设置 `sdk.dir`。
2. Windows：`gradlew.bat assembleDebug assembleRelease lintVitalRelease`。Linux/macOS：`sh gradlew assembleDebug assembleRelease lintVitalRelease`。
3. Debug 输出 `app/build/outputs/apk/debug/app-debug.apk`，包名带 `.debug`。Release 输出 `app/build/outputs/apk/release/app-release-unsigned.apk`。
4. 发布者使用已有私钥执行 zipalign / apksigner。仓库不包含签名私钥、口令或 API 配置。自行签名的包不能覆盖安装官方签名包；最终正式 APK 指纹见 README。

首次构建需从 Gradle/Google/Maven 下载固定依赖；这属于开发环境，游戏本地 AI 不发送网络请求。`--offline` 只适用于依赖已经缓存的机器。`app/src/main/assets` 包含完整运行素材，正常构建不需要重新生图或解压美术原件。

编辑素材时，从本版本 GitHub Release 下载 `liquidwars-1.79-art-source.zip` 并解压到仓库根目录，恢复 `art/source/`、SVG、PNG 原件、碰撞 JSON、提示词和素材登记。校验同 Release 的 SHA-256 清单。源稿包含历史停用素材，运行地图以 `art/ASSETS.csv` 和 app assets 为准。生成器在 `tools/`；部分历史生成器保留当时本机路径，重生成前按实际素材路径调整。当前运行素材全部已导出。

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
