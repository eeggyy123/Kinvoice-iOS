# Android 构建验收记录

验收日期：2026-08-21  
源码版本：Android 首次产品化版本（最终提交哈希以 Git 日志为准）

## 验证环境

- Windows 11 x64
- Eclipse Temurin JDK 17.0.20+8
- Android SDK Platform 35
- Android Build Tools 35.0.0
- Gradle 8.9
- Android Gradle Plugin 8.7.3

## 已通过门禁

```text
testDebugUnitTest lintDebug assembleDebug
BUILD SUCCESSFUL
Lint: No issues found.

testReleaseUnitTest lintRelease assembleRelease bundleRelease
BUILD SUCCESSFUL
```

验证内容包括 Kotlin/Compose 编译、Room KSP、API 35 资源链接、Manifest 合并、单元测试、Android Lint、debug APK 打包、Release R8 混淆、资源收缩和 AAB 打包。

## 产物

### 可安装体验包

`dist/android/KinVoice-1.0.0-debug.apk`

```text
SHA-256 B91B2F562EA86C5A4AEDE1548D8E67B60498AE6C3EAC07B2D58461EC4D74FE3F
```

- 包名：`com.kinvoice.family.debug`
- 版本：`1.0.0-debug`（versionCode 1）
- 最低系统：Android 8 / API 26
- 目标系统：API 35
- 权限：`INTERNET`、`RECORD_AUDIO`
- 签名：Android debug certificate，APK Signature Scheme v2 验证通过
- 用途：本机和小范围测试，不能上传应用市场

### Release 构建证明

`dist/android/KinVoice-1.0.0-unsigned.aab`

```text
SHA-256 B3BD60B4BD448DE58437BEDA8D74953FEBB02AA12D18AA1AEE403E3C09BAA846
```

该 AAB 完成 Release 编译、R8 和资源收缩，但没有运营者正式签名。必须按 `android/README.md` 配置 release keystore 后重新执行 `bundleRelease`，才能上传应用市场。

## 已知外部阻断

GitHub Actions 工作流已配置，但首次运行没有进入构建步骤，GitHub 返回：账号因 billing 问题被锁定。该问题不影响本机验收结果；账号恢复后工作流会继续作为远程质量门禁。

## 仍需真机验收

本机没有连接 Android 真机或模拟器，因此未验证具体厂商 ROM 的麦克风、系统语音识别、TTS 和安装界面。上架前必须按 `ANDROID-RELEASE-CHECKLIST.md` 在目标设备执行真机矩阵。
