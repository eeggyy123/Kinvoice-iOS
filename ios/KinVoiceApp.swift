import SwiftUI
import SwiftData

@main
struct KinVoiceApp: App {
    var body: some Scene {
        WindowGroup { RootTabView() }
            .modelContainer(for: [MemoryEntry.self, FamilyPerson.self])
    }
}
