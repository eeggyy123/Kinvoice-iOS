import SwiftUI
import SwiftData

struct AskFamilyView: View {
    @Query(sort: \MemoryEntry.createdAt, order: .reverse) private var memories: [MemoryEntry]
    @State private var question = ""
    @State private var answer: KnowledgeAnswer?
    @State private var isLoading = false
    @State private var errorMessage = ""

    var body: some View {
        List {
            Section {
                TextField("例如：外婆做红烧肉最关键的一步是什么？", text: $question, axis: .vertical)
                Button { Task { await ask() } } label: { HStack { Spacer(); if isLoading { ProgressView() } else { Label("从家庭记忆中查找", systemImage: "magnifyingglass") }; Spacer() } }
                    .disabled(question.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || memories.isEmpty || isLoading)
            }
            if memories.isEmpty { ContentUnavailableView("还没有可查询的记忆", systemImage: "text.bubble", description: Text("先在采集页保存一条经家人确认的记忆")) }
            if let answer {
                Section("回答") {
                    Text(answer.answer).textSelection(.enabled)
                    if !answer.grounded { Label("现有家庭资料不足，未生成推测性答案", systemImage: "exclamationmark.shield").font(.footnote).foregroundStyle(.secondary) }
                }
                if !answer.citations.isEmpty {
                    Section("来源") {
                        ForEach(answer.citations) { citation in
                            if let memory = memories.first(where: { $0.uuid.uuidString == citation.memoryId }) {
                                NavigationLink { MemoryDetailView(memory: memory) } label: {
                                    VStack(alignment: .leading, spacing: 4) { Text(citation.title).font(.headline); Text(citation.excerpt).font(.caption).foregroundStyle(.secondary).lineLimit(2) }
                                }
                            }
                        }
                    }
                }
            }
            if !errorMessage.isEmpty { Section { Text(errorMessage).foregroundStyle(.secondary) } }
        }.navigationTitle("问家")
    }

    private func ask() async {
        isLoading = true; errorMessage = ""; answer = nil
        do { answer = try await APIClient().ask(question: question, memories: memories) }
        catch { errorMessage = "当前无法连接知识服务。你的家庭记忆仍安全保存在本机，请稍后再试。" }
        isLoading = false
    }
}
