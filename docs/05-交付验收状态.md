# KinVoice 家声：交付验收状态

更新时间：2026-07-25

## 当前结论

代码已经达到“交给 Mac 同学执行 Xcode 16+ 首次编译与 TestFlight 配置”的交付候选状态，但还不能表述为“已经通过 Xcode 编译”或“已经符合启航赛道提交硬门槛”。原因是当前 Windows 环境没有 Apple SDK、签名工具和 App Store Connect 权限。

## 已完成并在 Windows 复核

- Xcode 工程直接包含 14 个 Swift 源文件、Assets、Info.plist 和隐私清单。
- App 目标为 iPhone / iOS 17，使用 SwiftUI、SwiftData、AVFoundation、Speech 与 AVSpeechSynthesizer。
- 记忆搜索、新增、校订、删除，录音、试听、录音后转写、系统朗读和本地离线草稿可用。
- 家庭成员可新增、显式保存修改并在确认后删除。
- 问家通过自有 HTTPS 后端调用 OpenAI 兼容中转站，回答必须带有效来源或降级为相关原文。
- 精简后端 10 项单元/API 测试通过；网页预览 JavaScript 语法检查通过。
- App Icon 为 1024×1024、RGB、不含透明通道。
- 生产入口不加载 vivo、音色克隆、旧聊天、数据库或快应用模块。
- 交付包不包含 `.env`、模型密钥、私钥、日志、数据库、虚拟环境和旧 vivo 代码。

## Mac 上必须完成的最终验收

1. 运行 `bash ios/verify_mac_build.sh`，确认模拟器 Debug 构建成功。
2. 配置 Apple Developer Team、唯一 Bundle ID、正式 HTTPS `APIBaseURL` 和可选 `APIAccessToken`。
3. 在真实 iPhone 验证麦克风、录音、试听、Apple Speech、原声回放、系统朗读、权限拒绝和断网场景。
4. 完成 Archive、上传 TestFlight、外部 Beta App Review，并用非开发者设备安装外部链接。

## 不能放入参赛陈述的未完成能力

- 未完成 CloudKit 多设备/多成员同步。
- 未完成边录边出的实时转写。
- 未实现声音克隆，也不依赖 vivo。
- 尚未完成 App Store Connect/TestFlight 外部发布。

只有 Mac 构建、真机验收和外部 TestFlight 均完成后，作品才满足启航赛道的技术交付硬门槛。
