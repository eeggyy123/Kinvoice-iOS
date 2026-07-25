import SwiftUI
import SwiftData

struct MemoryDetailView: View {
    @Bindable var memory: MemoryEntry
    @StateObject private var speech = SpeechPlayer()
    @StateObject private var recording = RecordingPlayer()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(memory.title).font(.largeTitle.bold()).foregroundStyle(KinTheme.ink)
                    Text([memory.author, memory.timeHint, memory.location].filter { !$0.isEmpty }.joined(separator: " · ")).foregroundStyle(KinTheme.secondary)
                }
                if !memory.quote.isEmpty {
                    Text("“\(memory.quote)”").font(.title3).italic().padding(.leading, 14).overlay(alignment: .leading) { Rectangle().fill(KinTheme.accent).frame(width: 3) }
                }
                Text(memory.content).font(.body).lineSpacing(6).textSelection(.enabled)
                if !memory.audioPath.isEmpty {
                    Button { recording.toggle(path: memory.audioPath) } label: {
                        Label(recording.isPlaying ? "停止原声" : "回听讲述人原声", systemImage: recording.isPlaying ? "stop.circle.fill" : "waveform.circle.fill")
                    }
                    .buttonStyle(.bordered)
                    .tint(KinTheme.accent)
                }
                if !memory.topics.isEmpty { Text(memory.topics.map { "#\($0)" }.joined(separator: "  ")).font(.footnote).foregroundStyle(KinTheme.secondary) }
            }.frame(maxWidth: .infinity, alignment: .leading).padding()
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItemGroup(placement: .topBarTrailing) {
                Button { speech.speak(memory.content) } label: { Image(systemName: "speaker.wave.2") }.accessibilityLabel("朗读记忆")
                NavigationLink { MemoryEditView(memory: memory) } label: { Image(systemName: "pencil") }.accessibilityLabel("编辑记忆")
            }
        }.onDisappear { speech.stop(); recording.stop() }.kinPage()
    }
}

struct MemoryEditView: View {
    let memory: MemoryEntry
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var context
    @State private var title: String
    @State private var summary: String
    @State private var content: String
    @State private var author: String
    @State private var timeHint: String
    @State private var location: String
    @State private var quote: String
    @State private var topicsText: String

    init(memory: MemoryEntry) {
        self.memory = memory
        _title = State(initialValue: memory.title)
        _summary = State(initialValue: memory.summary)
        _content = State(initialValue: memory.content)
        _author = State(initialValue: memory.author)
        _timeHint = State(initialValue: memory.timeHint)
        _location = State(initialValue: memory.location)
        _quote = State(initialValue: memory.quote)
        _topicsText = State(initialValue: memory.topics.joined(separator: "、"))
    }

    var body: some View {
        Form {
            Section("标题与摘要") {
                TextField("标题", text: $title)
                TextField("摘要", text: $summary, axis: .vertical)
            }
            Section("讲述信息") {
                TextField("讲述人", text: $author)
                TextField("时间线索", text: $timeHint)
                TextField("地点", text: $location)
            }
            Section("正文") { TextEditor(text: $content).frame(minHeight: 220) }
            Section("线索") {
                TextField("原话摘录", text: $quote, axis: .vertical)
                TextField("主题，用顿号分隔", text: $topicsText)
            }
        }
        .navigationTitle("校订记忆")
        .toolbar { Button("保存") { save() }.disabled(title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) }
    }

    private func save() {
        memory.title = title.trimmingCharacters(in: .whitespacesAndNewlines)
        memory.summary = summary
        memory.content = content.trimmingCharacters(in: .whitespacesAndNewlines)
        memory.author = author.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "家人" : author
        memory.timeHint = timeHint
        memory.location = location
        memory.quote = quote
        memory.topics = topicsText.split(separator: "、").map(String.init).filter { !$0.isEmpty }
        try? context.save()
        dismiss()
    }
}
