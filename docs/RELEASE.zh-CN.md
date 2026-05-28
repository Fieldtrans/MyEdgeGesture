# EdgeGesture 发布流程

## 1. 修改版本号

在 `app/build.gradle.kts` 中更新：

```kotlin
versionCode = 8
versionName = "1.0.1"
```

同时更新：

- `CHANGELOG.md`
- `README.md`

## 2. 配置正式签名

正式签名文件不提交到 GitHub。

本地需要有：

```text
signing.properties
release-signing/edgegesture-release.jks
```

`signing.properties` 格式：

```properties
EDGEGESTURE_STORE_FILE=release-signing/edgegesture-release.jks
EDGEGESTURE_STORE_PASSWORD=你的密码
EDGEGESTURE_KEY_ALIAS=edgegesture
EDGEGESTURE_KEY_PASSWORD=你的密码
```

如果没有 `signing.properties`，Gradle 会回退到 debug 签名，方便 GitHub Actions 自动构建。

## 3. 构建 APK

```bash
./gradlew :app:assembleRelease
```

输出位置：

```text
app/build/outputs/apk/release/app-release.apk
```

## 4. 验证签名

```bash
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

如果显示的是 `CN=EdgeGesture`，说明使用了正式签名。

## 5. 提交并打 tag

```bash
git add .
git commit -m "Release EdgeGesture 1.0.1"
git tag -a v1.0.1 -m "EdgeGesture 1.0.1"
git push origin main --tags
```

## 6. 创建 GitHub Release

Release 附件只上传 APK。演示图片和 GIF 保留在 README 中，不作为 Release 下载资产。
