# MyEdgeGesture

MyEdgeGesture is an LSPosed/Xposed module for one-hand edge gestures.

The app is only a settings panel. The gesture listener runs through the module in `system_server`, so killing the app process should not stop the gesture after the module is loaded.

## Features

- Right-edge swipe-up gesture for one-hand screen tapping.
- Line arrow pointer mode.
- Tracker + cursor pointer mode.
- Adjustable trigger region, pointer speed, smoothing, color, timeout, and cancel distance.
- Compose + Material 3 settings UI.
- LSPosed log output with the `MyEdgeGesture` tag.

## Requirements

- Rooted Android device.
- LSPosed installed and enabled.
- Android API 36 target project setup.

## Build

```bash
./gradlew :app:assembleRelease
```

Release APK:

```text
app/build/outputs/apk/release/app-release.apk
```

The release build currently uses the debug signing config for easier local testing.

## Usage

1. Install the APK.
2. Enable the module in LSPosed.
3. Make sure the scope includes Android system/framework.
4. Reboot the device.
5. Open the app and enable gestures.

If you need logs, search `MyEdgeGesture` in LSPosed logs.

## Status

Current version: `1.0-beta1`.

This is still a test module. Use it carefully because it hooks input handling in `system_server`.
