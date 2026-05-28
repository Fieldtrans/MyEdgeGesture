# EdgeGesture Community Posts

This file contains ready-to-use copy for sharing EdgeGesture in Android, LSPosed, Xposed, Magisk, and rooted-device communities.

Project: https://github.com/Fieldtrans/EdgeGesture  
Download: https://github.com/Fieldtrans/EdgeGesture/releases/latest  
Release: https://github.com/Fieldtrans/EdgeGesture/releases/tag/v1.0

## XDA / English Long Post

Title:

```text
[LSPosed][Android 16] EdgeGesture - One-handed edge gestures with line pointer and tracker cursor
```

Body:

```text
Hi everyone,

I am sharing EdgeGesture, an LSPosed/Xposed module for one-handed edge gestures on Android.

The goal is simple: make it easier to tap hard-to-reach areas of a large screen with one hand.

Main features:

- Right-edge swipe-up gesture.
- Line pointer mode: move your thumb in a small area and release to tap at the arrow tip.
- Tracker + Cursor mode: joystick-style cursor control for one-handed tapping.
- Adjustable trigger area, pointer speed, smoothing, color, cancel timeout, and cancel distance.
- Top-edge notification shade trigger.
- Configuration import/export.
- Visual-only overlay; no transparent full-screen touch layer.
- Gesture handling runs through the LSPosed/Xposed hook path inside system_server.

Requirements:

- Rooted Android device.
- LSPosed installed and enabled.
- Android system/framework scope enabled for the module.
- Reboot after enabling the module.

Current version:

- v1.0

Download:

https://github.com/Fieldtrans/EdgeGesture/releases/latest

Source code:

https://github.com/Fieldtrans/EdgeGesture

Notes:

This module hooks input handling in system_server, so please test carefully and keep a recovery path available. If gestures do not respond, check LSPosed logs and search for EdgeGesture.

Feedback is welcome, especially from Android 16 / high-refresh-rate device users.
```

## XDA / English Short Reply

```text
I made a small LSPosed module called EdgeGesture for one-handed edge gestures on Android.

It supports a line pointer mode and a Tracker + Cursor mode, with adjustable trigger area, speed, smoothing, color, cancel timeout, and cancel distance.

GitHub:
https://github.com/Fieldtrans/EdgeGesture

Latest APK:
https://github.com/Fieldtrans/EdgeGesture/releases/latest
```

## 酷安 / 中文长帖

标题：

```text
EdgeGesture：一个 LSPosed 单手边缘手势模块
```

正文：

```text
做了一个 LSPosed/Xposed 模块：EdgeGesture。

它主要解决大屏手机单手点不到上方、远处按钮的问题。

核心逻辑是从右侧边缘上划，调出指针，然后用小范围拇指移动去控制屏幕上更远的位置，松手后执行点击。

主要功能：

- 右侧边缘上划触发。
- 直线箭头模式：松手点击箭头尖位置。
- Tracker + Cursor 摇杆光标模式：类似摇杆控制光标。
- 可调触发区域、控制圆、速度、平滑度、颜色、取消时间、取消距离。
- 顶部触发通知栏下拉。
- 支持配置导入/导出。
- Overlay 只负责显示，不使用全屏透明触摸层。
- 手势监听运行在 LSPosed 注入的 system_server 中，正常情况下杀掉 App 后仍可用。

使用要求：

- 手机已 Root。
- 已安装并启用 LSPosed。
- 模块作用域需要包含 Android 系统/框架。
- 启用后需要重启手机。

下载：

https://github.com/Fieldtrans/EdgeGesture/releases/latest

项目地址：

https://github.com/Fieldtrans/EdgeGesture

目前版本是 v1.0，主要面向 Android 16 / API 36。

这是会 hook system_server 输入处理逻辑的模块，建议谨慎测试，保留可恢复手段。遇到问题可以在 LSPosed 日志里搜索 EdgeGesture。
```

## 中文短帖 / 群聊版

```text
我做了一个 LSPosed 单手边缘手势模块：EdgeGesture。

右侧边缘上划后可以用直线箭头或摇杆光标控制屏幕远处位置，松手执行点击，适合大屏手机单手操作。

支持调触发区域、速度、平滑度、颜色、取消范围、通知栏下拉、配置导入导出。

GitHub：
https://github.com/Fieldtrans/EdgeGesture

APK：
https://github.com/Fieldtrans/EdgeGesture/releases/latest
```

## Telegram / English Short Post

```text
EdgeGesture v1.0 is an LSPosed/Xposed module for one-handed edge gestures on Android.

Features:
- Right-edge swipe-up gesture
- Line pointer mode
- Tracker + Cursor mode
- Adjustable trigger area, speed, smoothing, color, and cancel behavior
- Notification shade trigger
- Config import/export

GitHub:
https://github.com/Fieldtrans/EdgeGesture

Download:
https://github.com/Fieldtrans/EdgeGesture/releases/latest
```

## Reddit / English Post

Title:

```text
I made an LSPosed module for one-handed edge gestures on Android
```

Body:

```text
I built EdgeGesture, an LSPosed/Xposed module for one-handed edge gestures.

The idea is to make large-screen phones easier to use with one hand. Swipe up from the right edge, move a pointer or cursor with your thumb, then release to tap.

Features:

- Line pointer mode
- Tracker + Cursor mode
- Adjustable trigger area, speed, smoothing, color, cancel timeout, and cancel distance
- Notification shade trigger
- Config import/export
- Visual-only overlay, no transparent full-screen touch layer

It currently targets Android 16 / API 36 and requires root + LSPosed.

GitHub:
https://github.com/Fieldtrans/EdgeGesture

Latest APK:
https://github.com/Fieldtrans/EdgeGesture/releases/latest

Feedback from rooted Android / LSPosed users would be helpful.
```

## Suggested Tags

```text
Android
Root
LSPosed
Xposed
Magisk
Edge Gestures
One-handed mode
Android 16
Quick Cursor
```
