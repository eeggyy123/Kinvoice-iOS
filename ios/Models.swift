import Foundation
import SwiftData

@Model final class MemoryEntry {
    var uuid: UUID
    var title: String
    var summary: String
    var content: String
    var author: String
    var topics: [String]
    var timeHint: String
    var location: String
    var quote: String
    var audioPath: String
    var createdAt: Date

    init(title: String, summary: String = "", content: String, author: String = "家人", topics: [String] = [], timeHint: String = "", location: String = "", quote: String = "", audioPath: String = "") {
        self.uuid = UUID()
        self.title = title; self.summary = summary; self.content = content; self.author = author
        self.topics = topics; self.timeHint = timeHint; self.location = location; self.quote = quote
        self.audioPath = audioPath; self.createdAt = .now
    }
}

@Model final class FamilyPerson {
    var name: String
    var relation: String
    var note: String

    init(name: String, relation: String, note: String = "") {
        self.name = name
        self.relation = relation
        self.note = note
    }
}
