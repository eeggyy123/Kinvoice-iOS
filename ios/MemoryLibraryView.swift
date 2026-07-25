import SwiftUI
import SwiftData

struct MemoryLibraryView: View {
    @Environment(\.modelContext) private var context
    @Query(sort: \MemoryEntry.createdAt, order: .reverse) private var memories: [MemoryEntry]
    @State private var search = ""
    @State private var pendingDeletion: MemoryEntry?
    private var filtered: [MemoryEntry] {
        guard !search.isEmpty else { return memories }
        return memories.filter { memory in
            [memory.title, memory.summary, memory.content, memory.author, memory.timeHint, memory.location, memory.quote]
                .joined(separator: " ")
                .appending(" " + memory.topics.joined(separator: " "))
                .localizedCaseInsensitiveContains(search)
        }
    }

    var body: some View {
        List {
            Section {
                Text("把家人的声音、经验和手艺，留给下一代。\n每一条记忆都可以校订、回听和追问。")
                    .font(.title3.weight(.medium)).foregroundStyle(KinTheme.ink).listRowBackground(Color.clear)
            }
            Section(search.isEmpty ? "最近保存" : "找到 \(filtered.count) 条") {
                if filtered.isEmpty {
                    if memories.isEmpty {
                        ContentUnavailableView("还没有家庭记忆", systemImage: "books.vertical", description: Text("从采集一次口述开始"))
                    } else {
                        ContentUnavailableView.search(text: search)
                    }
                }
                ForEach(filtered) { memory in
                    NavigationLink { MemoryDetailView(memory: memory) } label: {
                        VStack(alignment: .leading, spacing: 5) {
                            Text(memory.title).font(.headline)
                            Text("\(memory.author) · \(memory.summary.isEmpty ? memory.content : memory.summary)").font(.subheadline).foregroundStyle(KinTheme.secondary).lineLimit(2)
                        }.padding(.vertical, 5)
                    }
                    .swipeActions(edge: .trailing) {
                        Button("删除", systemImage: "trash", role: .destructive) { pendingDeletion = memory }
                    }
                }
            }
        }
        .navigationTitle("家声")
        .searchable(text: $search, prompt: "搜索人物、地点、主题或原话")
        .alert("删除这条记忆？", isPresented: Binding(
            get: { pendingDeletion != nil },
            set: { if !$0 { pendingDeletion = nil } }
        )) {
            Button("删除", role: .destructive) { deletePendingMemory() }
            Button("取消", role: .cancel) { pendingDeletion = nil }
        } message: {
            Text("记忆正文及关联的本机录音将被永久删除。")
        }
    }

    private func deletePendingMemory() {
        guard let memory = pendingDeletion else { return }
        if !memory.audioPath.isEmpty { try? FileManager.default.removeItem(atPath: memory.audioPath) }
        context.delete(memory)
        try? context.save()
        pendingDeletion = nil
    }
}
