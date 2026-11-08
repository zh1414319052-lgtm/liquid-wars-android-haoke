# Grafika 帧调度适配
上游：https://github.com/google/grafika
固定提交：b1df331e89cffeab621f02b102d4c2c25eb6088a (2020-01-17)
许可：Apache License 2.0，兼容本项目 GPL-3.0-or-later。完整 LICENSE 与原始 ScheduledSwapActivity.java.txt 随对应源码归档。

VsyncPacer 改编上游 ScheduledSwapActivity.doFrame 的 Choreographer 主线程回调转发思路；保留版权头。修改：转发到现有 GLSurfaceView.requestRender、合并未完成帧、生命周期停止、整数刷新周期选择与过期回调丢弃。不复制演示 Activity，不引入其媒体/录制代码，不接管 EGL 或重复 swapBuffers。FrameCadence / DisplayFrameRate / 三路径生命周期适配是本项目新增代码。

Grafika 已归档，上游自述为演示项目，不能称仍在维护的生产SDK。已在本项目验证适配范围；未借用其名称保证性能。Google Swappy 对当前 GLSurfaceView 需要替换 EGL 交换层，不能把 SwappyGL_swap 直接塞入 renderer，故本轮不引入 Swappy 二进制。无新网络端点、权限或 Gradle 依赖。
