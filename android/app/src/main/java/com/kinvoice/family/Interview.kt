package com.kinvoice.family

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.util.UUID

enum class InterviewerVoice(val label:String,val detail:String,val rate:Float,val pitch:Float){
    WARM("温和陪伴","慢一点，适合长辈",.88f,1.03f),
    STEADY("沉稳记录","低缓、克制",.82f,.92f),
    CLEAR("清晰引导","节奏清楚",.96f,1f)
}

@Composable
fun InterviewScreen(vm:AppViewModel,people:List<PersonEntity>,onSpeak:(String,InterviewerVoice)->Unit,onSaved:()->Unit){
    val context=androidx.compose.ui.platform.LocalContext.current;val scope=rememberCoroutineScope()
    var narrator by rememberSaveable{mutableStateOf(people.firstOrNull()?.name.orEmpty())};var relation by rememberSaveable{mutableStateOf(people.firstOrNull()?.relation.orEmpty())}
    var theme by rememberSaveable{mutableStateOf("童年与家庭")};var voice by rememberSaveable{mutableStateOf(InterviewerVoice.WARM)}
    var started by rememberSaveable{mutableStateOf(false)};var question by rememberSaveable{mutableStateOf("")};var answer by rememberSaveable{mutableStateOf("")}
    var turns by remember{mutableStateOf(listOf<InterviewTurnUi>())};var busy by remember{mutableStateOf(false)};var finishSuggested by remember{mutableStateOf(false)}
    var summary by remember{mutableStateOf<InterviewSummaryUi?>(null)};var message by remember{mutableStateOf<String?>(null)};var showTranscript by remember{mutableStateOf(false)};var abandon by remember{mutableStateOf(false)}
    val speech=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let{answer=it}}
    fun launchSpeech(){runCatching{speech.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_LANGUAGE,"zh-CN")})}.onFailure{message="设备未提供可用的系统语音识别服务"}}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it)launchSpeech()else message="未获得麦克风权限，可继续手动输入"}
    fun requestSpeech(){if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)launchSpeech()else permission.launch(Manifest.permission.RECORD_AUDIO)}
    fun loadNext(updated:List<InterviewTurnUi>){scope.launch{busy=true;vm.nextInterviewQuestion(narrator,relation,theme,updated).onSuccess{question=it.question;finishSuggested=it.shouldFinish;turns=updated+InterviewTurnUi("assistant",it.question);onSpeak(it.question,voice)}.onFailure{message="采访服务暂时不可用，请稍后重试"};busy=false}}
    fun finish(){scope.launch{busy=true;vm.summarizeInterview(narrator,relation,theme,turns).onSuccess{summary=it}.onFailure{message="暂时无法整理，请保留对话后重试"};busy=false}}
    if(people.isEmpty()){LazyColumn{item{PageHeader("AI 引导式采访","先添加一位家人","采访需要明确讲述者，资料才不会混在一起。")};item{Text("请前往“家庭”添加成员后再开始。",Modifier.padding(20.dp),color=Color(0xFF68706B))}};return}
    LazyColumn(Modifier.fillMaxSize()){
        item{PageHeader("家庭口述采访","和 AI 采访者聊一聊",if(started)"每次只问一个问题，结束后由你确认画像和记忆。" else "选择讲述者与主题，AI 会温和地逐步追问。")}
        if(!started)item{Column(Modifier.padding(horizontal=20.dp)){Text("讲述者",color=Color(0xFF68706B),fontSize=12.sp);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){people.take(4).forEach{p->FilterChip(narrator==p.name,{narrator=p.name;relation=p.relation},{Text(p.name)})}};Field("采访主题",theme){theme=it};Text("选择采访者",color=Color(0xFF68706B),fontSize=12.sp);InterviewerVoice.entries.forEach{item->Surface(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable{voice=item},shape=RoundedCornerShape(8.dp),color=if(voice==item)Color(0xFFDCEAE5)else Color.White){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){RadioButton(voice==item,{voice=item});Column{Text(item.label,fontWeight=FontWeight.Bold);Text("AI 合成音色 · ${item.detail}",color=Color(0xFF68706B),fontSize=11.sp)}}}};Button(onClick={started=true;loadNext(emptyList())},Modifier.fillMaxWidth().padding(top=14.dp),enabled=narrator.isNotBlank()&&!busy){Text(if(busy)"正在准备…" else "开始采访")}}}
        if(started)item{Column(Modifier.padding(horizontal=20.dp)){Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(8.dp),color=Color.White){Column(Modifier.padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(38.dp).background(Color(0xFFDCEAE5),CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Default.RecordVoiceOver,null,tint=Color(0xFF2F6F62))};Column(Modifier.padding(start=10.dp).weight(1f)){Text(voice.label,fontWeight=FontWeight.Bold);Text("AI 合成音色",color=Color(0xFF68706B),fontSize=10.sp)};IconButton(onClick={onSpeak(question,voice)}){Icon(Icons.Default.VolumeUp,"再次播报")}};Field("当前问题（可编辑）",question,3){question=it;turns=turns.dropLast(1)+InterviewTurnUi("assistant",it)} }};Field("你的回答",answer,5){answer=it};OutlinedButton(onClick={requestSpeech()},Modifier.fillMaxWidth()){Icon(Icons.Default.GraphicEq,null);Text(" 语音回答")};Button(onClick={if(answer.isBlank()){message="请先回答，或选择跳过"}else{val updated=turns+InterviewTurnUi("user",answer.trim());answer="";loadNext(updated)}},Modifier.fillMaxWidth().padding(top=8.dp),enabled=!busy){Text(if(busy)"正在思考下一问…" else "回答并继续")};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick={loadNext(turns+InterviewTurnUi("user","我想跳过这个问题。"))},enabled=!busy){Text("跳过")};TextButton(onClick={showTranscript=true}){Text("查看对话")};TextButton(onClick={finish()},enabled=turns.any{it.role=="user"}&&!busy){Text(if(finishSuggested)"建议结束" else "结束并整理")}};TextButton(onClick={abandon=true},Modifier.fillMaxWidth()){Text("放弃本次采访",color=Color(0xFFA94331))};message?.let{Text(it,color=Color(0xFFA94331),fontSize=12.sp)};Spacer(Modifier.height(20.dp))}}
    }
    if(showTranscript)AlertDialog(onDismissRequest={showTranscript=false},title={Text("完整对话")},text={LazyColumn{itemsIndexed(turns){index,turn->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top){Text(if(turn.role=="assistant")"AI" else narrator,Modifier.width(54.dp),fontWeight=FontWeight.Bold,color=if(turn.role=="assistant")Color(0xFF2F6F62)else Color(0xFFA94331));Text(turn.content,Modifier.weight(1f));if(turn.role=="user")IconButton(onClick={turns=turns.toMutableList().also{it.removeAt(index)}}){Icon(Icons.Default.Delete,"删除回答")}};HorizontalDivider()}}},confirmButton={Button(onClick={showTranscript=false}){Text("完成")}})
    if(abandon)AlertDialog(onDismissRequest={abandon=false},title={Text("放弃本次采访？")},text={Text("本次对话尚未保存，放弃后无法恢复。")},confirmButton={Button(onClick={started=false;turns=emptyList();question="";answer="";abandon=false}){Text("确认放弃")}},dismissButton={TextButton(onClick={abandon=false}){Text("继续采访")}})
    summary?.let{InterviewSummaryDialog(it,narrator,relation,onDismiss={summary=null}){person,memories->vm.saveInterviewResult(person,memories);summary=null;onSaved()}}
}

@Composable private fun InterviewSummaryDialog(initial:InterviewSummaryUi,narrator:String,relation:String,onDismiss:()->Unit,onSave:(PersonEntity,List<MemoryEntity>)->Unit){
    var bio by remember{mutableStateOf(initial.profile.bio)};var drafts by remember{mutableStateOf(initial.memories.map{EditableMemory(it,true)})}
    AlertDialog(onDismissRequest=onDismiss,title={Text("校订采访结果")},text={LazyColumn{item{Text("AI 只整理本次回答。请修改不准确内容，再选择要保存的记忆。",color=Color(0xFF2F6F62),fontSize=12.sp);Field("人物简介",bio,4){bio=it}};itemsIndexed(drafts){index,item->HorizontalDivider(Modifier.padding(vertical=10.dp));Row(verticalAlignment=Alignment.CenterVertically){Checkbox(item.selected,{checked->drafts=drafts.toMutableList().also{it[index]=item.copy(selected=checked)}});Text("保存为记忆",fontWeight=FontWeight.Bold)};Field("标题",item.data.title){value->drafts=drafts.toMutableList().also{it[index]=item.copy(data=item.data.copy(title=value))}};Field("正文",item.data.content,4){value->drafts=drafts.toMutableList().also{it[index]=item.copy(data=item.data.copy(content=value))}}}}},confirmButton={Button(onClick={onSave(PersonEntity(name=narrator,relation=relation.ifBlank{"家人"},note=bio),drafts.filter{it.selected}.map{d->MemoryEntity(title=d.data.title,summary=d.data.summary,content=d.data.content,author=narrator,topics=d.data.topics.joinToString("|"),timeHint=d.data.timeHint.orEmpty(),location=d.data.location.orEmpty(),quote=d.data.quote.orEmpty(),confirmed=false)})},enabled=drafts.any{it.selected}){Text("确认并保存")}},dismissButton={TextButton(onClick=onDismiss){Text("返回采访")}})
}
private data class EditableMemory(val data:InterviewMemoryDto,val selected:Boolean)
