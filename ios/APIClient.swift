import Foundation

struct MemoryDraft: Codable, Identifiable {
    var id = UUID()
    var title: String
    var summary: String
    var content: String
    var people: [String]
    var timeHint: String?
    var location: String?
    var topics: [String]
    var quote: String?
    var confidence: Double
    var needsReview: Bool

    enum CodingKeys: String, CodingKey { case title, summary, content, people, timeHint, location, topics, quote, confidence, needsReview }
}

struct KnowledgeCitation: Codable, Identifiable {
    var id: String { memoryId }
    let memoryId: String
    let title: String
    let excerpt: String
}

struct KnowledgeAnswer: Codable {
    let answer: String
    let citations: [KnowledgeCitation]
    let grounded: Bool
}

enum APIError: LocalizedError {
    case invalidConfiguration
    case invalidResponse
    case server(Int)

    var errorDescription: String? {
        switch self {
        case .invalidConfiguration: return "尚未配置可用的服务地址"
        case .invalidResponse: return "服务返回了无法识别的结果"
        case .server(let status): return "知识服务暂时不可用（HTTP \(status)）"
        }
    }
}

struct APIClient {
    private let session: URLSession
    private let baseURL: URL?
    private let accessToken: String

    init(session: URLSession = .shared) {
        self.session = session
        let value = Bundle.main.object(forInfoDictionaryKey: "APIBaseURL") as? String
        self.baseURL = value.flatMap(URL.init(string:)).flatMap { url in
            guard url.scheme == "https", url.host != "api.example.com" else { return nil }
            return url
        }
        self.accessToken = (Bundle.main.object(forInfoDictionaryKey: "APIAccessToken") as? String) ?? ""
    }

    func createDraft(transcript: String, narrator: String, prompt: String, knownPeople: [String]) async throws -> MemoryDraft {
        let body: [String: Any] = ["transcript": transcript, "narrator": narrator, "interview_prompt": prompt, "known_people": knownPeople]
        return try await post(path: "/v1/memories/draft", body: body)
    }

    func ask(question: String, memories: [MemoryEntry]) async throws -> KnowledgeAnswer {
        let sources = memories.prefix(40).map { ["id": $0.uuid.uuidString, "title": $0.title, "content": $0.content, "people": [$0.author], "time_hint": $0.timeHint] as [String: Any] }
        return try await post(path: "/v1/knowledge/ask", body: ["question": question, "memories": sources])
    }

    private func post<T: Decodable>(path: String, body: [String: Any]) async throws -> T {
        guard let baseURL else { throw APIError.invalidConfiguration }
        let component = path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        var request = URLRequest(url: baseURL.appendingPathComponent(component))
        request.httpMethod = "POST"
        request.timeoutInterval = 40
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if !accessToken.isEmpty { request.setValue(accessToken, forHTTPHeaderField: "X-App-Token") }
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw APIError.invalidResponse }
        guard 200..<300 ~= http.statusCode else { throw APIError.server(http.statusCode) }
        let decoder = JSONDecoder(); decoder.keyDecodingStrategy = .convertFromSnakeCase
        return try decoder.decode(T.self, from: data)
    }
}
