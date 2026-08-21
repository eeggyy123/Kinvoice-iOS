# KinVoice 家声 Android

原生 Android 版本，使用 Kotlin、Jetpack Compose、Room、MediaRecorder、Android Speech/TTS 和 Retrofit。它不是 WebView 包装；家庭记忆和录音默认保存在应用私有目录。

## 环境

- Android Studio Ladybug（2024.2.1）或更新版本
- JDK 17
- Android SDK 35
- 最低系统 Android 8.0（API 26）

## 构建

```powershell
cd android
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

调试 APK：`app/build/outputs/apk/debug/app-debug.apk`。

没有 Android Studio 的协作者也可在 GitHub Actions 的 `Android quality gate` 中下载 debug APK artifact。

## AI 服务配置

默认不配置服务地址时，App 使用本地带来源检索和本地草稿，不伪装成大模型结果。生产构建在用户目录的 `~/.gradle/gradle.properties` 设置：

```properties
KINVOICE_API_BASE_URL=https://api.your-domain.example
KINVOICE_API_ACCESS_TOKEN=replace-with-non-secret-app-gateway-token
```

`KINVOICE_API_BASE_URL` 必须是自有 HTTPS KinVoice 后端，不能直接填写模型中转站。真正的模型 API Key 只放在后端 `.env`。客户端中的共享 token 可以被提取，只能作为轻量限流标识，不能替代用户认证或云端风控。

## 正式签名与 AAB

首次发布前在离线安全位置生成签名；同一包名后续更新必须使用同一密钥：

```powershell
keytool -genkeypair -v -keystore kinvoice-release.jks -alias kinvoice -keyalg RSA -keysize 4096 -validity 10000
```

只在本机 `~/.gradle/gradle.properties` 设置，不提交仓库：

```properties
KINVOICE_STORE_FILE=D:/secure/kinvoice-release.jks
KINVOICE_STORE_PASSWORD=your-store-password
KINVOICE_KEY_ALIAS=kinvoice
KINVOICE_KEY_PASSWORD=your-key-password
```

```powershell
.\gradlew.bat clean testReleaseUnitTest lintRelease bundleRelease assembleRelease
```

- Google Play 优先上传 `app/build/outputs/bundle/release/app-release.aab`。
- 国内部分市场上传 `app/build/outputs/apk/release/app-release.apk`。
- 永远不要提交 `.jks`、密码、模型密钥或生产 `.env`。

## 权限与数据

- `INTERNET`：仅在用户主动使用 AI 整理/问答时访问自有 HTTPS 后端。
- `RECORD_AUDIO`：用户主动录音或调用系统语音识别时运行时申请。
- 不申请相册、通讯录、定位、电话、存储、广告标识或后台录音权限。
- Android 云备份和设备迁移备份已关闭，避免家庭正文与录音被系统自动上传。
- “家庭”页面可导出 JSON，也可删除数据库、成员、问题和所有录音。

## 发布资料

- [隐私政策](../docs/07-Android隐私政策.md)
- [上架与合规清单](docs/ANDROID-RELEASE-CHECKLIST.md)
- [商店文案](store-assets/STORE-LISTING-zh-CN.md)
