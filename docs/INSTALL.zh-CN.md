# EdgeGesture 安装指南

## 准备条件

- 手机已经 Root。
- 已安装并启用 LSPosed。
- 已安装 EdgeGesture APK。
- 当前版本主要面向 Android 16 / API 36。

## 安装步骤

1. 在 GitHub Releases 下载最新版 APK。
2. 安装 APK。
3. 打开 LSPosed。
4. 在模块列表中启用 EdgeGesture。
5. 进入 EdgeGesture 的作用域设置。
6. 勾选 Android 系统/框架相关作用域。
7. 重启手机。
8. 打开 EdgeGesture，确认模块状态显示为已加载。
9. 打开启用模块手势开关。
10. 选择直线箭头或摇杆光标模式。

## 推荐首次设置

- 触发边缘宽度：先使用默认值。
- 直线箭头灵敏度：先使用默认值。
- 控制圆：先保持可见，确认手感后再调低透明度。
- 取消范围：先保持默认值，避免误触。

## 检查是否成功

成功时通常会看到：

- EdgeGesture App 中显示 LSPosed 已加载模块。
- 右侧边缘上划后出现指针或摇杆光标。
- LSPosed 日志中可以搜索到 `EdgeGesture`。

## 更新版本

如果你之前安装过测试版，通常可以直接覆盖安装。

注意：`v1.0.1` 开始使用正式 release 签名。如果你手机上安装的是 `v1.0` 或更早的 debug 签名 APK，可能无法直接覆盖安装。遇到安装失败时，请先卸载旧版本，再安装新版。

更新后建议：

1. 重新打开 EdgeGesture 保存一次配置。
2. 在 LSPosed 中确认模块仍然启用。
3. 重启手机。

## 卸载

1. 在 LSPosed 中禁用 EdgeGesture。
2. 重启手机。
3. 卸载 EdgeGesture App。
