import SwiftUI
import SwiftData

struct PrivacyView: View {
    @Environment(\.modelContext) private var context
    @Query private var memories: [MemoryEntry]
    @Query private var people: [FamilyPerson]
    @State private var confirmDelete = false

    var body: some View {
        List {
            Section("数据如何使用") {
                Label("记忆正文默认保存在本机", systemImage: "iphone")
                Label("语音转写使用 Apple Speech，是否可用取决于设备与系统服务", systemImage: "waveform")
                Label("使用 AI 整理或问答时，仅发送完成该请求所需的文字", systemImage: "network")
                Label("不会在未经明确同意时克隆家人音色", systemImage: "waveform.badge.mic")
            }
            Section("你的控制") {
                Text("保存前可以校订语音转写与 AI 草稿；来源不足时问答不会编造答案。录音和记忆可以随时删除。")
                Button("删除本机全部家庭数据", systemImage: "trash", role: .destructive) { confirmDelete = true }
            }
        }.navigationTitle("隐私与数据")
        .alert("删除全部家庭数据？", isPresented: $confirmDelete) {
            Button("删除", role: .destructive) { deleteAll() }; Button("取消", role: .cancel) {}
        } message: { Text("本机上的记忆、录音索引和家庭成员将被永久删除。") }
    }

    private func deleteAll() {
        memories.forEach { memory in
            if !memory.audioPath.isEmpty { try? FileManager.default.removeItem(atPath: memory.audioPath) }
            context.delete(memory)
        }
        people.forEach(context.delete)
        try? context.save()
    }
}
