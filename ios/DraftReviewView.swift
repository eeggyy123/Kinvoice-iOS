import SwiftUI
import SwiftData

struct DraftReviewView: View {
    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    let narrator: String
    let audioPath: String
    let onSaved: () -> Void
    @State private var title: String
    @State private var summary: String
    @State private var content: String
    @State private var timeHint: String
    @State private var location: String
    @State private var quote: String
    @State private var topics: String

    init(draft: MemoryDraft, narrator: String, audioPath: String, onSaved: @escaping () -> Void = {}) {
        self.narrator = narrator; self.audioPath = audioPath; self.onSaved = onSaved
        _title = State(initialValue: draft.title); _summary = State(initialValue: draft.summary); _content = State(initialValue: draft.content)
        _timeHint = State(initialValue: draft.timeHint ?? ""); _location = State(initialValue: draft.location ?? ""); _quote = State(initialValue: draft.quote ?? "")
        _topics = State(initialValue: draft.topics.joined(separator: "、"))
    }

    var body: some View {
        Form {
            Section { Label("AI 只生成草稿，请由家人确认事实和措辞后保存。", systemImage: "checkmark.seal") }
            Section("标题与摘要") { TextField("标题", text: $title); TextField("摘要", text: $summary, axis: .vertical) }
            Section("记忆正文") { TextEditor(text: $content).frame(minHeight: 220) }
            Section("线索") { TextField("时间", text: $timeHint); TextField("地点", text: $location); TextField("原话摘录", text: $quote, axis: .vertical); TextField("主题，用顿号分隔", text: $topics) }
        }.navigationTitle("校订草稿").navigationBarTitleDisplayMode(.inline)
        .toolbar { ToolbarItem(placement: .confirmationAction) { Button("保存") { save() }.disabled(title.isEmpty || content.isEmpty) }; ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } } }
    }

    private func save() {
        let tags = topics.split(separator: "、").map(String.init).filter { !$0.isEmpty }
        context.insert(MemoryEntry(title: title, summary: summary, content: content, author: narrator, topics: tags, timeHint: timeHint, location: location, quote: quote, audioPath: audioPath))
        try? context.save(); onSaved(); dismiss()
    }
}
