# Changelog

## 1.0.4 - 2026-05-29

- Removed the bottom-edge action option to avoid conflict with Android system Home/Recents gestures.
- Action settings now focus on left and right edges only.

## 1.0.3 - 2026-05-29

- Implemented actions currently available in this release:
  - Edge swipe up: One-hand tap, Back, Home, or Recents.
  - Edge double tap: Recents.
- Removed unimplemented action rows from the action settings page.
- Removed the top-edge action option; action settings now focus on left, right, and bottom edges.
- Kept the gesture detector on the stable InputFilter-based path.

## 1.0.2 - 2026-05-29

- Emergency hotfix release.
- Restored the stable v1.0.1 input handling path.
- Removed the experimental all-action gesture detector build from the release path because it could cause high `system_server` CPU usage on real devices.

## 1.0.1 - 2026-05-28

- Added GPL-3.0 license.
- Added GitHub Actions release APK build.
- Added install and troubleshooting documentation.
- Added real-device screenshots to the project page.
- Removed non-download demo GIF from GitHub Release assets.
- Added local release signing support through ignored `signing.properties`.

## 1.0 - 2026-05-28

- Renamed the project to EdgeGesture.
- Added line arrow pointer mode.
- Added Tracker + Cursor mode.
- Added adjustable trigger area, control circle, pointer speed, smoothing, color, cancel timeout, and cancel distance.
- Added top-edge notification shade trigger and lightweight pre-animation.
- Added configuration import and export.
- Optimized overlay drawing to reduce unnecessary refreshes.
- Added a simple app icon and bilingual UI/README.
