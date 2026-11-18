# 构建 / Build 2.12 (85)

JDK 21、Android SDK 35 / build-tools 35.0.1、NDK 28.2.13676358、项目自带 Gradle wrapper。Godot Android 依赖为 4.7.2.stable；minSdk 26、targetSdk 34。仅设置本进程 JAVA_HOME / ANDROID_HOME，或使用不提交的 local.properties。

```text
gradlew.bat :app:assembleRelease :app:testReleaseUnitTest :app:lintVitalRelease
```

输出四 ABI 未签名包 app/build/outputs/apk/release/app-release-unsigned.apk。正式发布者使用既有受保护签名：

```text
python tools/release_signing.py sign --input app/build/outputs/apk/release/app-release-unsigned.apk --output <new-signed.apk>
```

输出文件须不存在；不会创建新签名或导出凭据。公开证书引用在 docs/game/formal-1.53/signing-public.json。自行签名的包不能覆盖原发布者安装。QA 使用 `:app:assembleDebug :app:testDebugUnitTest -PgardenQa186`，独立包名，不覆盖正式应用。

## 场景、素材、字体

app/godot 是可编辑 Godot 场景；运行数据 app/src/main/assets/battle.pck 已提供。GitHub 用户另下载同版本 liquidwars-2.12-resources.zip，在仓库根目录解压，恢复 art/。本地交付「源码」已含真实原件，无需再下载。

修改场景后用准确 Godot 4.7.2 导入并重新导出：

```text
godot --headless --path app/godot --editor --import
godot --headless --path app/godot --export-pack "Android data" ../src/main/assets/battle.pck
```

实际战斗由 Android 宿主和原生核心驱动；桌面场景预览不代表完整游戏。新增中文后可运行 tools/build185_font.py，使用已归档 Noto Sans SC / OFL 原件生成字体子集。黑洞源稿 art/source/black-hole-2.05，声音 art/source/audio-2.06，主菜单设计 art/source/menu-2.12。用户指定的旧吞噬音轨已停用，常规构建不需要 audio-2.07 原视频，也不运行历史音轨提取脚本。

本版检查：60 项 release JVM 测试、R8/JNI 与最终资源核验、同签升级、首次说明/重启、菜单与对战入口。具体结果以 docs/game/formal-2.12/RELEASE.md 为准。只在指定设备上测试，不修改设备全局设置或清用户数据。历史测试/发布脚本包含各自的版本与设备绑定，不自动用于当前版本。

No build command uploads an APK or publishes an update. Preserve upstream attribution and third-party license files. Private signing keys, administrator credentials and player data are not distributed.
