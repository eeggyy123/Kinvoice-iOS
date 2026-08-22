package com.kinvoice.family

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

data class DraftRequest(val transcript:String, val narrator:String?, @Json(name="interview_prompt") val prompt:String?, @Json(name="known_people") val knownPeople:List<String>)
data class DraftResponse(val title:String, val summary:String, val content:String, val people:List<String> = emptyList(), @Json(name="time_hint") val timeHint:String?, val location:String?, val topics:List<String> = emptyList(), val quote:String?, val confidence:Double = .5, @Json(name="needs_review") val needsReview:Boolean = true)
data class KnowledgeMemory(val id:String, val title:String, val content:String, val people:List<String>, @Json(name="time_hint") val timeHint:String?)
data class AskRequest(val question:String, val memories:List<KnowledgeMemory>)
data class Citation(@Json(name="memory_id") val memoryId:String, val title:String, val excerpt:String)
data class AskResponse(val answer:String, val citations:List<Citation> = emptyList(), val grounded:Boolean = false)
data class InterviewTurnDto(val role:String, val content:String)
data class InterviewRequest(@Json(name="narrator_name") val narratorName:String, val relation:String, val theme:String, val turns:List<InterviewTurnDto>)
data class InterviewNextResponse(val question:String, val reason:String = "", @Json(name="should_finish") val shouldFinish:Boolean = false, val degraded:Boolean = false)
data class InterviewProfileDto(@Json(name="display_name") val displayName:String, val relation:String, val bio:String = "", val traits:List<String> = emptyList())
data class InterviewMemoryDto(val title:String, val summary:String = "", val content:String, @Json(name="time_hint") val timeHint:String?, val location:String?, val topics:List<String> = emptyList(), val quote:String?, @Json(name="source_turns") val sourceTurns:List<Int> = emptyList(), @Json(name="needs_review") val needsReview:Boolean = true)
data class InterviewSummaryResponse(val profile:InterviewProfileDto, val memories:List<InterviewMemoryDto> = emptyList(), val degraded:Boolean = false)

interface KinVoiceApi {
    @POST("v1/memories/draft") suspend fun draft(@Body request: DraftRequest): DraftResponse
    @POST("v1/knowledge/ask") suspend fun ask(@Body request: AskRequest): AskResponse
    @POST("v1/interviews/next") suspend fun interviewNext(@Body request: InterviewRequest): InterviewNextResponse
    @POST("v1/interviews/summarize") suspend fun interviewSummarize(@Body request: InterviewRequest): InterviewSummaryResponse
}

object ApiFactory {
    fun create(): KinVoiceApi? {
        val base = BuildConfig.API_BASE_URL.trim()
        if (!base.startsWith("https://")) return null
        val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(45, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder().apply {
                    if (BuildConfig.API_ACCESS_TOKEN.isNotBlank()) header("X-App-Token", BuildConfig.API_ACCESS_TOKEN)
                }.build()
                chain.proceed(request)
            }.build()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        return Retrofit.Builder().baseUrl(base.trimEnd('/') + "/").client(client).addConverterFactory(MoshiConverterFactory.create(moshi)).build().create(KinVoiceApi::class.java)
    }
}

object LocalKnowledge {
    fun ask(question: String, memories: List<MemoryEntity>): Pair<String, List<MemoryEntity>> {
        val normalized = question.lowercase().filterNot { it.isWhitespace() || it in "，。？！、" }
        val stop = setOf("什么", "哪些", "怎么", "如何", "家里", "一个")
        val terms = normalized.windowed(2).filterNot(stop::contains).distinct()
        val scored = memories.map { memory ->
            val text = "${memory.title}${memory.author}${memory.content}${memory.quote}${memory.topics}".lowercase()
            memory to terms.count(text::contains)
        }.filter { it.second >= 2 }.sortedByDescending { it.second }
        val top = scored.firstOrNull()?.second ?: return "家庭记忆中暂未找到相关记录。家声不会根据常识猜测答案。" to emptyList()
        val sources = scored.filter { it.second >= maxOf(2, (top * .7).toInt()) }.take(3).map { it.first }
        val primary = sources.first()
        return (primary.quote.ifBlank { primary.summary.ifBlank { primary.content.take(160) } }) to sources
    }
}
