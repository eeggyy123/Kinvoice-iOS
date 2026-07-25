import SwiftUI
import SwiftData

struct FamilyView: View {
    @Environment(\.modelContext) private var context
    @Query private var people: [FamilyPerson]
    @Query private var memories: [MemoryEntry]
    @State private var name = ""
    @State private var relation = ""
    @State private var pendingDeletion: FamilyPerson?

    var body: some View {
        List {
            Section("家庭成员") {
                ForEach(people) { person in
                    NavigationLink { FamilyPersonEditView(person: person) } label: {
                        HStack { Image(systemName: "person.crop.circle").foregroundStyle(KinTheme.accent); Text(person.name); Spacer(); Text(person.relation).foregroundStyle(.secondary) }
                    }
                    .swipeActions(edge: .trailing) {
                        Button("删除", systemImage: "trash", role: .destructive) { pendingDeletion = person }
                    }
                }
                TextField("姓名", text: $name); TextField("关系，例如：外婆", text: $relation)
                Button("添加成员", systemImage: "person.badge.plus") { addPerson() }
                    .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            Section("数据") {
                LabeledContent("家庭记忆", value: "\(memories.count) 条")
                Button("载入演示家庭", systemImage: "wand.and.stars") { DemoData.seed(in: context) }.disabled(!memories.isEmpty)
                NavigationLink("隐私与数据管理", destination: PrivacyView())
            }
            Section("关于") { LabeledContent("版本", value: "1.0.0"); Text("KinVoice 家声 · 家庭知识传承库").foregroundStyle(.secondary) }
        }
        .navigationTitle("家庭")
        .alert("删除家庭成员？", isPresented: Binding(
            get: { pendingDeletion != nil },
            set: { if !$0 { pendingDeletion = nil } }
        )) {
            Button("删除", role: .destructive) { deletePendingPerson() }
            Button("取消", role: .cancel) { pendingDeletion = nil }
        } message: {
            Text("成员资料会被删除，已经保存的记忆不会受影响。")
        }
    }

    private func addPerson() {
        context.insert(FamilyPerson(name: name.trimmingCharacters(in: .whitespacesAndNewlines), relation: relation.trimmingCharacters(in: .whitespacesAndNewlines)))
        try? context.save()
        name = ""
        relation = ""
    }

    private func deletePendingPerson() {
        guard let person = pendingDeletion else { return }
        context.delete(person)
        try? context.save()
        pendingDeletion = nil
    }
}

struct FamilyPersonEditView: View {
    let person: FamilyPerson
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var context
    @State private var name: String
    @State private var relation: String
    @State private var note: String

    init(person: FamilyPerson) {
        self.person = person
        _name = State(initialValue: person.name)
        _relation = State(initialValue: person.relation)
        _note = State(initialValue: person.note)
    }

    var body: some View {
        Form {
            Section("基本信息") {
                TextField("姓名", text: $name)
                TextField("家庭关系", text: $relation)
                TextField("备注", text: $note, axis: .vertical)
            }
            Section {
                Text("成员资料只用于归档讲述人和检索家庭记忆。")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .navigationTitle("编辑成员")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { ToolbarItem(placement: .confirmationAction) { Button("保存") { save() }.disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) } }
    }

    private func save() {
        person.name = name.trimmingCharacters(in: .whitespacesAndNewlines)
        person.relation = relation.trimmingCharacters(in: .whitespacesAndNewlines)
        person.note = note.trimmingCharacters(in: .whitespacesAndNewlines)
        try? context.save()
        dismiss()
    }
}
