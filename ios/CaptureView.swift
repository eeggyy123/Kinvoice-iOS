import SwiftUI
import SwiftData
import Foundation

struct CaptureView: View {
    @Query private var people: [FamilyPerson]
    @StateObject private var recorder = AudioRecorder()
    @StateObject private var recordingPreview = RecordingPlayer()
    @StateObject private var transcriber = SpeechTranscriber()
    @State private var narrator = ""
    @State private var prompt = "小时候家里过年，最不能少的一道菜是什么？"
    @State private var transcript = ""
    @State private var draft: MemoryDraft?
    @State private var isLoading = false
    @State private var message = ""

    var body: some View {
        Form {
            Section("这次和谁聊") { TextField("讲述人，例如：外婆", text: $narrator) }
            Section("采访问题") {
                TextField("问题", text: $prompt, axis: .vertical)
                Menu("换一个问题", systemImage: "shuffle") {
                    ForEach(Self.prompts, id: \.self) { value in Button(value) { prompt = value } }
                }
            }
            Section("口述记录") {
                Button { recorder.toggle() } label: { Label(recorder.isRecording ? "结束录音" : "开始录音", systemImage: recorder.isRecording ? "stop.circle.fill" : "mic.circle.fill") }
                    .foregroundStyle(recorder.isRecording ? .red : KinTheme.accent)
                if recorder.isRecording {
                    Label("正在录音 \(formattedDuration)", systemImage: "waveform")
                        .font(.footnote)
                        .foregroundStyle(.red)
                }
                if recorder.fileURL != nil {
                    Label("录音已保存在本机 · \(formattedDuration)", systemImage: "checkmark.circle").font(.footnote).foregroundStyle(.secondary)
                    Button { if let path = recorder.fileURL?.path { recordingPreview.toggle(path: path) } } label: {
                        Label(recordingPreview.isPlaying ? "停止试听" : "试听录音", systemImage: recordingPreview.isPlaying ? "stop.circle" : "play.circle")
                    }
                    Button { Task { await transcribeRecording() } } label: {
                        if transcriber.isTranscribing { Label("正在使用 Apple 语音转写", systemImage: "waveform") }
                        else { Label("转写这段录音", systemImage: "text.bubble") }
                    }
                    .disabled(recorder.isRecording || transcriber.isTranscribing)
                    Button("丢弃并重录", systemImage: "trash", role: .destructive) {
                        recordingPreview.stop()
                        recorder.discard()
                    }
                    .disabled(recorder.isRecording || transcriber.isTranscribing)
                }
                TextEditor(text: $transcript).frame(minHeight: 170).overlay(alignment: .topLeading) { if transcript.isEmpty { Text("输入、粘贴或使用 Apple 语音转写生成口述文字。") .foregroundStyle(.tertiary).padding(.top, 8).allowsHitTesting(false) } }
                if !recorder.errorMessage.isEmpty {
                    Label(recorder.errorMessage, systemImage: "exclamationmark.triangle")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            Section { Button { Task { await organize() } } label: { HStack { Spacer(); if isLoading { ProgressView() } else { Label("整理成记忆草稿", systemImage: "sparkles") }; Spacer() } }.disabled(transcript.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading) }
            if !message.isEmpty { Section { Text(message).font(.footnote).foregroundStyle(.secondary) } }
        }
        .navigationTitle("采集口述")
        .alert("需要麦克风权限", isPresented: $recorder.permissionDenied) { Button("好", role: .cancel) {} } message: { Text("请在系统设置中允许家声访问麦克风。") }
        .sheet(item: $draft) { value in
            NavigationStack {
                DraftReviewView(
                    draft: value,
                    narrator: narrator.isEmpty ? "家人" : narrator,
                    audioPath: recorder.fileURL?.path ?? "",
                    onSaved: resetAfterSave
                )
            }
        }
        .onDisappear {
            if recorder.isRecording { recorder.stop() }
            recordingPreview.stop()
        }
    }

    private func organize() async {
        isLoading = true; message = ""
        do { draft = try await APIClient().createDraft(transcript: transcript, narrator: narrator, prompt: prompt, knownPeople: people.map(\.name)) }
        catch {
            draft = MemoryDraft(title: String(transcript.prefix(18)), summary: String(transcript.prefix(80)), content: transcript, people: narrator.isEmpty ? [] : [narrator], timeHint: nil, location: nil, topics: ["待整理"], quote: nil, confidence: 0.3, needsReview: true)
            message = "当前离线，已生成可手动校订的本地草稿。"
        }
        isLoading = false
    }

    private func transcribeRecording() async {
        guard let fileURL = recorder.fileURL else { return }
        message = ""
        do {
            transcript = try await transcriber.transcribe(fileURL: fileURL)
            message = "转写完成，请校对可能识别错误的人名、地名和年代。"
        } catch {
            message = error.localizedDescription
        }
    }

    private func resetAfterSave() {
        transcript = ""
        recordingPreview.stop()
        recorder.detachFile()
        draft = nil
        message = "记忆已保存，可继续采集下一段口述。"
    }

    private var formattedDuration: String {
        String(format: "%02d:%02d", recorder.elapsedSeconds / 60, recorder.elapsedSeconds % 60)
    }

    static let prompts = ["你年轻时学会的第一门手艺是什么？", "家里有哪条规矩，背后藏着什么故事？", "哪一次搬家或远行让你印象最深？", "这道家常菜是谁教你的？"]
}
