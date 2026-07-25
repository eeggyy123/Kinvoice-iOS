import SwiftUI
import SwiftData

struct RootTabView: View {
    @Environment(\.modelContext) private var context
    @Query private var memories: [MemoryEntry]
    @AppStorage("didSeedDemoV1") private var didSeedDemo = false

    var body: some View {
        TabView {
            NavigationStack { MemoryLibraryView() }.tabItem { Label("记忆库", systemImage: "books.vertical") }
            NavigationStack { CaptureView() }.tabItem { Label("采集", systemImage: "mic") }
            NavigationStack { AskFamilyView() }.tabItem { Label("问家", systemImage: "text.bubble") }
            NavigationStack { FamilyView() }.tabItem { Label("家庭", systemImage: "person.2") }
        }
        .kinPage()
        .task {
            guard !didSeedDemo else { return }
            if memories.isEmpty { DemoData.seed(in: context) }
            didSeedDemo = true
        }
    }
}

@MainActor
private func makePreviewModelContainer() -> ModelContainer {
    let schema = Schema([MemoryEntry.self, FamilyPerson.self])
    let configuration = ModelConfiguration(schema: schema, isStoredInMemoryOnly: true)
    let container = try! ModelContainer(for: schema, configurations: [configuration])
    DemoData.seed(in: container.mainContext)
    return container
}

#Preview("完整 App") {
    RootTabView()
        .modelContainer(makePreviewModelContainer())
}
