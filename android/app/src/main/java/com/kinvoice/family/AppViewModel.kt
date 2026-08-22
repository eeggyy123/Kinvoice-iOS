package com.kinvoice.family

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class DraftUi(val title:String, val summary:String, val content:String, val author:String, val timeHint:String="", val location:String="", val quote:String="", val topics:List<String> = emptyList())
data class AnswerUi(val text:String, val sources:List<MemoryEntity>, val online:Boolean)
data class InterviewTurnUi(val role:String, val content:String)
data class InterviewNextUi(val question:String, val shouldFinish:Boolean, val online:Boolean)
data class InterviewSummaryUi(val profile:InterviewProfileDto, val memories:List<InterviewMemoryDto>, val online:Boolean)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as KinVoiceApplication).repository
    private val api = ApiFactory.create()
    val memories = repository.memories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val people = repository.people.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val prompts = repository.prompts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    var recorder: MediaRecorder? = null; private set
    var player: MediaPlayer? = null; private set
    var recordingName: String = ""; private set

    init { viewModelScope.launch { repository.initializeIfNeeded() } }
    fun saveMemory(memory:MemoryEntity) = viewModelScope.launch { repository.saveMemory(memory) }
    fun deleteMemory(memory:MemoryEntity) = viewModelScope.launch { repository.deleteMemory(memory) }
    fun savePerson(person:PersonEntity) = viewModelScope.launch { repository.savePerson(person) }
    fun deletePerson(person:PersonEntity) = viewModelScope.launch { repository.deletePerson(person) }
    fun savePrompts(values:List<String>) = viewModelScope.launch { repository.savePrompts(values) }
    fun deleteAll() = viewModelScope.launch { repository.deleteEverything(memories.value) }

    fun startRecording(context: Context): Result<Unit> = runCatching {
        stopPlayback()
        recordingName = "recording-${System.currentTimeMillis()}.m4a"
        @Suppress("DEPRECATION")
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(context.getFileStreamPath(recordingName).absolutePath)
            prepare(); start()
        }
    }
    fun stopRecording(): String {
        runCatching { recorder?.stop() }
        recorder?.release(); recorder = null
        return recordingName
    }
    fun discardRecording(context:Context) { if (recordingName.isNotBlank()) context.getFileStreamPath(recordingName).delete(); recordingName = "" }
    fun play(context: Context, path:String, onDone:()->Unit) {
        stopPlayback(); if (path.isBlank()) return
        runCatching {
            player = MediaPlayer().apply { setDataSource(context.getFileStreamPath(path).absolutePath); setOnCompletionListener { stopPlayback(); onDone() }; prepare(); start() }
        }.onFailure { stopPlayback(); onDone() }
    }
    fun stopPlayback() { player?.stop(); player?.release(); player = null }

    suspend fun createDraft(transcript:String, narrator:String, prompt:String): Result<DraftUi> = runCatching {
        val remote = api?.draft(DraftRequest(transcript, narrator, prompt, people.value.map { it.name }))
        if (remote != null) DraftUi(remote.title, remote.summary, remote.content, narrator, remote.timeHint.orEmpty(), remote.location.orEmpty(), remote.quote.orEmpty(), remote.topics)
        else DraftUi("${narrator.ifBlank { "家人" }}的一段口述", "一段等待家人共同确认的记忆。", transcript, narrator.ifBlank { "家人" }, topics=listOf("新记忆"))
    }
    suspend fun ask(question:String): Result<AnswerUi> = runCatching {
        val current = memories.value.take(40)
        if (api != null && current.isNotEmpty()) {
            val response = api.ask(AskRequest(question, current.map { KnowledgeMemory(it.id,it.title,it.content,listOf(it.author),it.timeHint) }))
            val sources = response.citations.mapNotNull { citation -> current.find { it.id == citation.memoryId } }
            if (response.grounded && sources.isNotEmpty()) AnswerUi(response.answer, sources, true) else {
                val local = LocalKnowledge.ask(question,current); AnswerUi(local.first,local.second,false)
            }
        } else { val local=LocalKnowledge.ask(question,current); AnswerUi(local.first,local.second,false) }
    }
    suspend fun nextInterviewQuestion(narrator:String, relation:String, theme:String, turns:List<InterviewTurnUi>): Result<InterviewNextUi> = runCatching {
        val request = InterviewRequest(narrator, relation, theme, turns.map { InterviewTurnDto(it.role,it.content) })
        val remote = api?.interviewNext(request)
        if (remote != null) InterviewNextUi(remote.question,remote.shouldFinish,!remote.degraded)
        else {
            val answerCount=turns.count{it.role=="user"&&it.content!="我想跳过这个问题。"}; val questions=KinVoiceRepository.defaultPrompts
            InterviewNextUi(questions[answerCount.coerceAtMost(questions.lastIndex)],answerCount>=6,false)
        }
    }
    suspend fun summarizeInterview(narrator:String, relation:String, theme:String, turns:List<InterviewTurnUi>): Result<InterviewSummaryUi> = runCatching {
        val request=InterviewRequest(narrator,relation,theme,turns.map{InterviewTurnDto(it.role,it.content)})
        val remote=api?.interviewSummarize(request)
        if(remote!=null) InterviewSummaryUi(remote.profile,remote.memories,!remote.degraded) else {
            val answers=turns.mapIndexedNotNull{i,t->if(t.role=="user"&&t.content!="我想跳过这个问题。") i to t.content else null}
            InterviewSummaryUi(
                InterviewProfileDto(narrator,relation,answers.joinToString(" "){it.second}.take(300)),
                answers.take(5).map{(index,text)->InterviewMemoryDto(text.substringBefore('。').take(28).ifBlank{"一段家庭讲述"},text.take(120),text,null,null,listOf(theme),text.substringBefore('。').take(100),listOf(index))},
                false
            )
        }
    }
    fun saveInterviewResult(person:PersonEntity, memories:List<MemoryEntity>) = viewModelScope.launch {
        val existing=people.value.firstOrNull{it.name==person.name}
        repository.savePerson(if(existing==null) person else existing.copy(relation=person.relation,note=person.note))
        memories.forEach { repository.saveMemory(it) }
    }
    fun export(context:Context): Intent {
        val root = JSONObject().put("exportedAt", System.currentTimeMillis()).put("memories", JSONArray().apply { memories.value.forEach { put(JSONObject().put("id",it.id).put("title",it.title).put("summary",it.summary).put("content",it.content).put("author",it.author).put("topics",JSONArray(it.topicList())).put("timeHint",it.timeHint).put("location",it.location).put("quote",it.quote).put("confirmed",it.confirmed)) } }).put("people", JSONArray().apply { people.value.forEach { put(JSONObject().put("name",it.name).put("relation",it.relation).put("role",it.role).put("note",it.note)) } })
        val file = File(context.cacheDir,"kinvoice-family-export.json").apply { writeText(root.toString(2)) }
        val uri: Uri = FileProvider.getUriForFile(context,"${context.packageName}.files",file)
        return Intent(Intent.ACTION_SEND).apply { type="application/json"; putExtra(Intent.EXTRA_STREAM,uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    override fun onCleared() { runCatching { recorder?.release() }; stopPlayback(); super.onCleared() }
}
