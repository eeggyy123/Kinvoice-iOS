import AVFoundation
import Combine
import Foundation
import Speech

@MainActor final class AudioRecorder: NSObject, ObservableObject, AVAudioRecorderDelegate {
    @Published var isRecording = false
    @Published var fileURL: URL?
    @Published var permissionDenied = false
    @Published private(set) var elapsedSeconds = 0
    @Published private(set) var errorMessage = ""
    private var recorder: AVAudioRecorder?
    private var timer: Timer?

    func toggle() {
        isRecording ? stop() : requestAndStart()
    }

    private func requestAndStart() {
        AVAudioSession.sharedInstance().requestRecordPermission { [weak self] granted in
            Task { @MainActor in
                if granted { self?.start() } else { self?.permissionDenied = true }
            }
        }
    }

    private func start() {
        do {
            if let fileURL { try? FileManager.default.removeItem(at: fileURL) }
            errorMessage = ""
            elapsedSeconds = 0
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playAndRecord, mode: .spokenAudio, options: [.defaultToSpeaker])
            try session.setActive(true)
            let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appending(path: "memory-\(UUID().uuidString).m4a")
            recorder = try AVAudioRecorder(url: url, settings: [AVFormatIDKey: Int(kAudioFormatMPEG4AAC), AVSampleRateKey: 44_100, AVNumberOfChannelsKey: 1, AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue])
            recorder?.delegate = self
            guard recorder?.record() == true else { throw CocoaError(.fileWriteUnknown) }
            fileURL = url
            isRecording = true
            timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
                Task { @MainActor in self?.elapsedSeconds += 1 }
            }
        } catch {
            isRecording = false
            permissionDenied = false
            errorMessage = "无法开始录音，请检查麦克风权限和设备存储空间。"
            try? AVAudioSession.sharedInstance().setActive(false)
        }
    }

    func stop() {
        timer?.invalidate()
        timer = nil
        recorder?.stop()
        recorder = nil
        isRecording = false
        try? AVAudioSession.sharedInstance().setActive(false)
    }

    func discard() {
        stop()
        if let fileURL { try? FileManager.default.removeItem(at: fileURL) }
        fileURL = nil
        elapsedSeconds = 0
        errorMessage = ""
    }

    func detachFile() {
        fileURL = nil
        elapsedSeconds = 0
        errorMessage = ""
    }
}

enum SpeechTranscriptionError: LocalizedError {
    case permissionDenied
    case unavailable
    case emptyResult

    var errorDescription: String? {
        switch self {
        case .permissionDenied: return "未获得语音识别权限。你仍可以手动输入口述文字。"
        case .unavailable: return "Apple 语音识别当前不可用，请稍后重试或手动输入。"
        case .emptyResult: return "没有识别到清晰语音，请检查录音后重试。"
        }
    }
}

@MainActor final class SpeechTranscriber: ObservableObject {
    @Published private(set) var isTranscribing = false
    private var recognitionTask: SFSpeechRecognitionTask?

    func transcribe(fileURL: URL) async throws -> String {
        let authorization = await withCheckedContinuation { continuation in
            SFSpeechRecognizer.requestAuthorization { continuation.resume(returning: $0) }
        }
        guard authorization == .authorized else { throw SpeechTranscriptionError.permissionDenied }
        guard let recognizer = SFSpeechRecognizer(locale: Locale(identifier: "zh-CN")), recognizer.isAvailable else {
            throw SpeechTranscriptionError.unavailable
        }

        recognitionTask?.cancel()
        isTranscribing = true
        let request = SFSpeechURLRecognitionRequest(url: fileURL)
        request.shouldReportPartialResults = false

        return try await withCheckedThrowingContinuation { continuation in
            recognitionTask = recognizer.recognitionTask(with: request) { [weak self] result, error in
                Task { @MainActor in
                    guard let self, self.isTranscribing else { return }
                    if let error {
                        self.isTranscribing = false
                        self.recognitionTask = nil
                        continuation.resume(throwing: error)
                    } else if let result, result.isFinal {
                        self.isTranscribing = false
                        self.recognitionTask = nil
                        let text = result.bestTranscription.formattedString.trimmingCharacters(in: .whitespacesAndNewlines)
                        if text.isEmpty { continuation.resume(throwing: SpeechTranscriptionError.emptyResult) }
                        else { continuation.resume(returning: text) }
                    }
                }
            }
        }
    }
}

@MainActor final class SpeechPlayer: ObservableObject {
    private let synthesizer = AVSpeechSynthesizer()
    func speak(_ text: String) {
        synthesizer.stopSpeaking(at: .immediate)
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: "zh-CN")
        utterance.rate = 0.48
        synthesizer.speak(utterance)
    }
    func stop() { synthesizer.stopSpeaking(at: .immediate) }
}

@MainActor final class RecordingPlayer: NSObject, ObservableObject, AVAudioPlayerDelegate {
    @Published private(set) var isPlaying = false
    private var player: AVAudioPlayer?

    func toggle(path: String) {
        if isPlaying { stop(); return }
        guard !path.isEmpty else { return }
        do {
            player = try AVAudioPlayer(contentsOf: URL(fileURLWithPath: path))
            player?.delegate = self
            player?.prepareToPlay()
            isPlaying = player?.play() == true
        } catch {
            isPlaying = false
        }
    }

    func stop() {
        player?.stop()
        player = nil
        isPlaying = false
    }

    nonisolated func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        Task { @MainActor in self.stop() }
    }
}
