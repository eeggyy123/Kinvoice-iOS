# Kinvoice-iOS

iOS端家庭知识库app



\# KinVoice 家声 iOS



这是面向启航赛道的 SwiftUI/SwiftData iPhone 客户端。声音能力全部使用 Apple 框架：AVFoundation 录音/回放、Apple Speech 转写、AVSpeechSynthesizer 系统朗读。



\## Mac 交接



1\. 安装 Xcode 16+，直接打开 `KinVoice.xcodeproj`；`project.yml` 仅用于需要时用 XcodeGen 重建。

2\. 先运行 `bash verify\_mac\_build.sh`，完成无签名模拟器编译。

3\. 设置唯一 Bundle Identifier 和开发团队，把 `Info.plist` 的 `APIBaseURL` 改成真实 HTTPS 后端。

4\. 在 iPhone 真机检查录音、试听、Apple Speech 转写、回放、问家和隐私删除。

5\. 执行 Product > Archive，在 Organizer 上传 App Store Connect，再创建 TestFlight 外部测试组。



完整步骤见工作区 `docs/04-App总结与Mac交付手册.md`。



\## SwiftUI Canvas 预览



1\. 在 Xcode 左侧打开 `RootTabView.swift`。

2\. 选择菜单 `Editor > Canvas`，或按 `Option + Command + Return`。

3\. 点击 Canvas 中的 `Resume`，等待“完整 App”预览出现。

4\. 开启 Canvas 的交互模式后，可切换记忆库、采集、问家和家庭四个 Tab。



预览使用独立的内存 SwiftData 数据库，并自动载入演示家庭，不会改动模拟器或真机数据。录音、Apple Speech、网络和音频路由仍应使用 iPhone Simulator 或真机通过 `Command + R` 验证。



\## 产品边界



家声的第一版只有一个主功能：家庭知识传承库。音频采集、AI 整理、来源引用问答和系统语音回听都是记忆条目的子模块。



