package com.kinvoice.family

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private val Ink = Color(0xFF202522); private val Muted = Color(0xFF68706B); private val Paper = Color(0xFFF7F5F0)
private val Accent = Color(0xFFA94331); private val AccentSoft = Color(0xFFF4DFD9); private val Sage = Color(0xFF2F6F62); private val SageSoft = Color(0xFFDCEAE5)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); enableEdgeToEdge(); tts=TextToSpeech(this,this); setContent { KinVoiceTheme { KinVoiceApp(onSpeak={ speak(it) }) } } }
    override fun onInit(status:Int) { if(status==TextToSpeech.SUCCESS) tts?.language=Locale.SIMPLIFIED_CHINESE }
    private fun speak(text:String) { tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,"kinvoice") }
    override fun onDestroy() { tts?.stop(); tts?.shutdown(); super.onDestroy() }
}

@Composable fun KinVoiceTheme(content:@Composable ()->Unit) {
    MaterialTheme(colorScheme=lightColorScheme(primary=Accent,secondary=Sage,background=Paper,surface=Color.White,onSurface=Ink), typography=Typography(bodyLarge=LocalTextStyle.current.copy(letterSpacing=0.sp)), content=content)
}

enum class AppTab(val label:String) { Library("记忆库"), Capture("采集"), Ask("问家"), Family("家庭") }

@Composable fun KinVoiceApp(vm:AppViewModel=viewModel(), onSpeak:(String)->Unit) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(AppTab.Library) }
    val memories by vm.memories.collectAsState(); val people by vm.people.collectAsState(); val prompts by vm.prompts.collectAsState()
    Scaffold(containerColor=Paper, bottomBar={ NavigationBar(containerColor=Color.White) { AppTab.entries.forEach { item -> NavigationBarItem(selected=tab==item,onClick={tab=item},icon={Icon(when(item){AppTab.Library->Icons.Default.AutoStories;AppTab.Capture->Icons.Default.Mic;AppTab.Ask->Icons.Default.QuestionAnswer;AppTab.Family->Icons.Default.Group},null)},label={Text(item.label)}) } } }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) { when(tab) {
            AppTab.Library -> LibraryScreen(memories,{tab=AppTab.Capture},vm::saveMemory,vm::deleteMemory,onSpeak) { path, done -> vm.play(context, path, done) }
            AppTab.Capture -> CaptureScreen(vm,people,prompts.map{it.text}) { tab=AppTab.Library }
            AppTab.Ask -> AskScreen(vm)
            AppTab.Family -> FamilyScreen(vm,people,memories)
        } }
    }
}

@Composable fun PageHeader(eyebrow:String,title:String,description:String, action:(@Composable ()->Unit)?=null) {
    Column(Modifier.fillMaxWidth().padding(20.dp,18.dp,20.dp,16.dp)) { Text(eyebrow,color=Accent,fontSize=11.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.height(5.dp)); Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)){Text(title,fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink);Text(description,color=Muted,lineHeight=22.sp)}; action?.invoke() } }
}

@Composable fun LibraryScreen(memories:List<MemoryEntity>,onCapture:()->Unit,onSave:(MemoryEntity)->Unit,onDelete:(MemoryEntity)->Unit,onSpeak:(String)->Unit,onPlay:(String,()->Unit)->Unit) {
    var query by rememberSaveable { mutableStateOf("") }; var filter by rememberSaveable { mutableStateOf("全部") }; var selected by remember { mutableStateOf<MemoryEntity?>(null) }
    val filtered=memories.filter{m->(query.isBlank()||"${m.title}${m.author}${m.summary}${m.content}${m.quote}${m.topics}".contains(query,true))&&(filter=="全部"||(filter=="待校订"&&!m.confirmed)||m.topicList().contains(filter))}
    LazyColumn(Modifier.fillMaxSize()) {
        item { PageHeader("家庭知识传承库","记忆库","把家人的声音、经验和手艺，留给下一代。") { FilledTonalIconButton(onClick=onCapture){Icon(Icons.Default.Add,"开始采访")} } }
        item { Row(Modifier.padding(horizontal=20.dp).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Metric("记忆",memories.size.toString(),Modifier.weight(1f));Metric("已确认",memories.count{it.confirmed}.toString(),Modifier.weight(1f));Metric("原声",memories.count{it.audioPath.isNotBlank()}.toString(),Modifier.weight(1f))} }
        item { OutlinedTextField(query,{query=it},Modifier.padding(20.dp,18.dp,20.dp,8.dp).fillMaxWidth(),placeholder={Text("搜索人物、地点、手艺或原话")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true); Row(Modifier.padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){listOf("全部","待校订","手艺","节气").forEach{f->FilterChip(selected=filter==f,onClick={filter=f},label={Text(f)})}}; Text("${filtered.size} 条家庭记忆",Modifier.padding(20.dp,15.dp,20.dp,8.dp),color=Muted,fontSize=12.sp) }
        if(filtered.isEmpty()) item { EmptyState("没有找到相关记忆","换个人物、地点或原话试试") }
        items(filtered,key={it.id}) { memory -> MemoryCard(memory){selected=memory} }
        item { Spacer(Modifier.height(20.dp)) }
    }
    selected?.let { memory -> MemoryDialog(memory,onDismiss={selected=null},onSave={onSave(it);selected=null},onDelete={onDelete(memory);selected=null},onSpeak={onSpeak(memory.content)},onPlay={done->onPlay(memory.audioPath,done)}) }
}

@Composable fun Metric(label:String,value:String,modifier:Modifier=Modifier){Surface(modifier,shape=RoundedCornerShape(7.dp),color=Color.White,tonalElevation=1.dp){Column(Modifier.padding(13.dp)){Text(label,color=Muted,fontSize=11.sp);Text(value,fontSize=24.sp,fontWeight=FontWeight.Bold);Text("家庭资料",color=Sage,fontSize=10.sp)}}}
@Composable fun MemoryCard(m:MemoryEntity,onClick:()->Unit){Surface(Modifier.padding(horizontal=20.dp,vertical=6.dp).fillMaxWidth().clickable(onClick=onClick),shape=RoundedCornerShape(8.dp),color=Color.White,tonalElevation=1.dp){Column(Modifier.padding(16.dp)){Row{Text("${m.author} · ${m.timeHint}",Modifier.weight(1f),color=Sage,fontSize=11.sp,fontWeight=FontWeight.Bold);StatusPill(m.confirmed)};Text(m.title,Modifier.padding(top=10.dp),fontSize=17.sp,fontWeight=FontWeight.Bold);Text(m.summary,Modifier.padding(top=5.dp),color=Muted,maxLines=2,overflow=TextOverflow.Ellipsis,lineHeight=20.sp);HorizontalDivider(Modifier.padding(vertical=12.dp));Row{Text(if(m.audioPath.isBlank())"文字记录" else "◉ 有原声",Modifier.weight(1f),color=Muted,fontSize=11.sp);Text(m.topicList().take(2).joinToString("  "){"#$it"},color=Color(0xFF385B79),fontSize=11.sp)}}}}
@Composable fun StatusPill(confirmed:Boolean){Text(if(confirmed)"家人已确认" else "待校订",color=if(confirmed)Sage else Color(0xFF8B641D),fontSize=10.sp,modifier=Modifier.background(if(confirmed)SageSoft else Color(0xFFF6EDDB),RoundedCornerShape(4.dp)).padding(6.dp,3.dp))}
@Composable fun EmptyState(title:String,detail:String){Column(Modifier.fillMaxWidth().padding(50.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.Inventory2,null,tint=Muted);Text(title,Modifier.padding(top=10.dp),fontWeight=FontWeight.Bold);Text(detail,color=Muted,fontSize=12.sp)}}

@Composable fun MemoryDialog(memory:MemoryEntity,onDismiss:()->Unit,onSave:(MemoryEntity)->Unit,onDelete:()->Unit,onSpeak:()->Unit,onPlay:(()->Unit)->Unit){var edit by remember{mutableStateOf(false)};var title by remember(memory){mutableStateOf(memory.title)};var content by remember(memory){mutableStateOf(memory.content)};var summary by remember(memory){mutableStateOf(memory.summary)};var confirmed by remember(memory){mutableStateOf(memory.confirmed)};var playing by remember{mutableStateOf(false)}
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(edit)"编辑记忆" else memory.title,fontWeight=FontWeight.Bold)},text={LazyColumn{item{if(edit){Field("标题",title){title=it};Field("摘要",summary){summary=it};Field("正文",content,5){content=it};Row(verticalAlignment=Alignment.CenterVertically){Checkbox(confirmed,{confirmed=it});Text("家人已确认事实与措辞")}}else{Text("${memory.author} · ${memory.timeHint} · ${memory.location}",color=Muted,fontSize=12.sp);if(memory.quote.isNotBlank())Text("“${memory.quote}”",Modifier.padding(vertical=16.dp),color=Accent,fontSize=17.sp);Text(memory.content,lineHeight=25.sp);if(memory.audioPath.isNotBlank())Text("◉ 保存有本地原声",Modifier.padding(top=12.dp),color=Sage)}}}},confirmButton={Button(onClick={if(edit)onSave(memory.copy(title=title,summary=summary,content=content,confirmed=confirmed))else onDismiss()}){Text(if(edit)"保存" else "完成")}},dismissButton={Row{if(!edit&&memory.audioPath.isNotBlank())TextButton(onClick={playing=true;onPlay{playing=false}}){Text(if(playing)"播放中" else "播放原声")};TextButton(onClick={if(edit)onDelete()else onSpeak()}){Text(if(edit)"删除" else "朗读")};TextButton(onClick={edit=!edit}){Text(if(edit)"取消" else "编辑")}}})}

@Composable
fun CaptureScreen(
    vm: AppViewModel,
    people: List<PersonEntity>,
    prompts: List<String>,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var narrator by rememberSaveable { mutableStateOf(people.firstOrNull()?.name.orEmpty()) }
    var prompt by rememberSaveable { mutableStateOf(prompts.firstOrNull() ?: KinVoiceRepository.defaultPrompts.first()) }
    var transcript by rememberSaveable { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }
    var audioPath by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf<DraftUi?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showDisclosure by remember { mutableStateOf(false) }
    var pendingSpeech by remember { mutableStateOf(false) }
    var committed by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let {
            transcript = if (transcript.isBlank()) it else "$transcript\n$it"
        }
    }
    fun launchSpeech() {
        runCatching {
            speechLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            })
        }.onFailure { message = "设备未提供可用的系统语音识别服务" }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            pendingSpeech = false
            message = "未获得麦克风权限，可继续手动输入文字"
        } else if (pendingSpeech) {
            pendingSpeech = false
            launchSpeech()
        } else {
            vm.startRecording(context)
                .onSuccess { recording = true; elapsed = 0 }
                .onFailure { message = "无法开始录音，请检查麦克风是否被占用" }
        }
    }
    fun requestMicrophone(forSpeech: Boolean) {
        pendingSpeech = forSpeech
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (forSpeech) launchSpeech() else vm.startRecording(context)
                .onSuccess { recording = true; elapsed = 0 }
                .onFailure { message = "无法开始录音，请检查麦克风是否被占用" }
        } else {
            showDisclosure = true
        }
    }

    LaunchedEffect(recording) { while (recording) { delay(1_000); elapsed++ } }
    DisposableEffect(Unit) {
        onDispose {
            if (vm.recorder != null) vm.stopRecording()
            if (!committed) vm.discardRecording(context)
        }
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item { PageHeader("引导式采访", "采集口述", "用一个好问题，换来一段可以留给下一代的真实声音。") }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Field("这次和谁聊", narrator) { narrator = it }
                Field("采访问题（可直接编辑）", prompt, 3) { prompt = it }
                prompts.drop(1).take(3).forEach { option ->
                    OutlinedButton(onClick = { prompt = option }, modifier = Modifier.fillMaxWidth()) { Text(option) }
                }
                Spacer(Modifier.height(14.dp))
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = Color.White) {
                    Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = {
                                if (recording) { audioPath = vm.stopRecording(); recording = false }
                                else requestMicrophone(forSpeech = false)
                            },
                            modifier = Modifier.size(68.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (recording) Color(0xFFB72E27) else Accent)
                        ) { Icon(if (recording) Icons.Default.Stop else Icons.Default.Mic, "录音", Modifier.size(30.dp)) }
                        Text("%02d:%02d".format(elapsed / 60, elapsed % 60), Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
                        Text(if (recording) "正在录音，点击结束" else if (audioPath.isNotBlank()) "录音已保存在本机" else "点击开始录音", color = Muted, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Field("文字草稿", transcript, 6) { transcript = it }
                OutlinedButton(onClick = { requestMicrophone(forSpeech = true) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.GraphicEq, null); Spacer(Modifier.width(7.dp)); Text("使用系统语音识别")
                }
                Button(
                    onClick = {
                        if (transcript.isBlank()) message = "请先输入或识别一段口述文字"
                        else scope.launch {
                            busy = true
                            vm.createDraft(transcript, narrator, prompt)
                                .onSuccess { draft = it }
                                .onFailure { message = "整理服务暂时不可用，文字和录音仍保留在本机" }
                            busy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = !busy
                ) { Text(if (busy) "正在整理…" else "整理成记忆草稿") }
                message?.let { Text(it, color = Accent, modifier = Modifier.padding(top = 8.dp)) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (showDisclosure) AlertDialog(
        onDismissRequest = { showDisclosure = false; pendingSpeech = false },
        title = { Text("使用麦克风") },
        text = { Text("家声只在你主动点击后录音。录音默认保存在本机；选择系统语音识别时，音频可能由设备提供的识别服务处理。拒绝后仍可手动输入文字。") },
        confirmButton = { Button(onClick = { showDisclosure = false; permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) { Text("继续") } },
        dismissButton = { TextButton(onClick = { showDisclosure = false; pendingSpeech = false }) { Text("暂不使用") } }
    )
    draft?.let { value ->
        DraftDialog(value, audioPath, onDismiss = { draft = null }) {
            committed = true
            vm.saveMemory(it)
            draft = null
            onSaved()
        }
    }
}

@Composable fun DraftDialog(initial:DraftUi,audioPath:String,onDismiss:()->Unit,onSave:(MemoryEntity)->Unit){var title by remember{mutableStateOf(initial.title)};var summary by remember{mutableStateOf(initial.summary)};var content by remember{mutableStateOf(initial.content)};AlertDialog(onDismissRequest=onDismiss,title={Text("校订记忆草稿")},text={Column{Text("AI 只负责整理，请由家人确认事实后保存。",color=Sage,fontSize=12.sp);Field("标题",title){title=it};Field("摘要",summary){summary=it};Field("正文",content,5){content=it}}},confirmButton={Button(onClick={onSave(MemoryEntity(title=title,summary=summary,content=content,author=initial.author,topics=initial.topics.joinToString("|"),timeHint=initial.timeHint,location=initial.location,quote=initial.quote,audioPath=audioPath,confirmed=false))}){Text("确认并保存")}},dismissButton={TextButton(onClick=onDismiss){Text("取消")}})}

@Composable fun AskScreen(vm:AppViewModel){val scope=rememberCoroutineScope();var question by rememberSaveable{mutableStateOf("")};var answer by remember{mutableStateOf<AnswerUi?>(null)};var loading by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
    LazyColumn(Modifier.fillMaxSize()){item{PageHeader("仅依据家庭资料","问家","没有证据时，家声会明确告诉你资料不足。")};item{Column(Modifier.padding(horizontal=20.dp)){Surface(color=Color(0xFFE6EDF2),shape=RoundedCornerShape(6.dp)){Text(if(BuildConfig.API_BASE_URL.isBlank())"当前为本地检索模式。配置 HTTPS 后端后自动启用大模型问答。" else "AI 只会依据你主动提交的候选记忆回答，并返回来源。",Modifier.padding(12.dp),color=Color(0xFF304F69),fontSize=12.sp)};Field("你想问什么",question,4){question=it};Button(onClick={if(question.isBlank())error="请先输入问题" else scope.launch{loading=true;vm.ask(question).onSuccess{answer=it}.onFailure{error="知识服务暂时不可用"};loading=false}},Modifier.fillMaxWidth(),enabled=!loading){Text(if(loading)"正在查找…" else "从家庭记忆中查找")};error?.let{Text(it,color=Accent,modifier=Modifier.padding(top=8.dp))};answer?.let{a->HorizontalDivider(Modifier.padding(vertical=20.dp));Text(if(a.sources.isEmpty())"资料不足" else "✓ 来自 ${a.sources.size} 条家庭记忆",color=Sage,fontWeight=FontWeight.Bold,fontSize=12.sp);Text(a.text,Modifier.padding(vertical=14.dp),fontSize=18.sp,lineHeight=29.sp);a.sources.forEach{m->Surface(Modifier.padding(vertical=5.dp).fillMaxWidth(),shape=RoundedCornerShape(7.dp),color=Color.White){Column(Modifier.padding(13.dp)){Text(m.title,fontWeight=FontWeight.Bold);Text("${m.author} · ${m.timeHint} · ${if(m.confirmed)"已确认" else "待校订"}",color=Muted,fontSize=11.sp)}}}};Spacer(Modifier.height(24.dp))}}}
}

@Composable fun FamilyScreen(vm:AppViewModel,people:List<PersonEntity>,memories:List<MemoryEntity>){val context=LocalContext.current;var editing by remember{mutableStateOf<PersonEntity?>(null)};var adding by remember{mutableStateOf(false)};var confirmDelete by remember{mutableStateOf(false)};var showPrivacy by remember{mutableStateOf(false)}
    LazyColumn(Modifier.fillMaxSize()){item{PageHeader("成员与数据","家庭协作","成员角色、声音授权和家庭数据都由你掌控。"){FilledTonalIconButton(onClick={adding=true}){Icon(Icons.Default.PersonAdd,"添加成员")}}};items(people,key={it.id}){p->Surface(Modifier.padding(horizontal=20.dp,vertical=5.dp).fillMaxWidth().clickable{editing=p},shape=RoundedCornerShape(8.dp),color=Color.White){Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(42.dp).background(SageSoft,CircleShape),contentAlignment=Alignment.Center){Text(p.name.takeLast(1),color=Sage,fontWeight=FontWeight.Bold)};Column(Modifier.padding(start=11.dp).weight(1f)){Text(p.name,fontWeight=FontWeight.Bold);Text("${p.relation} · ${p.role}",color=Muted,fontSize=12.sp);if(p.note.isNotBlank())Text(p.note,color=Muted,fontSize=11.sp,maxLines=1)}}}};item{Column(Modifier.padding(20.dp)){Text("家庭资料",fontWeight=FontWeight.Bold);DataRow("家庭记忆","${memories.size} 条");DataRow("待校订","${memories.count{!it.confirmed}} 条");DataRow("保存位置","仅本机");HorizontalDivider(Modifier.padding(vertical=16.dp));Text("隐私与数据",fontWeight=FontWeight.Bold);Text("录音和家庭资料默认只保存在本机。只有主动使用 AI 时，才发送当前请求所需文字。当前版本不提供声音克隆。",Modifier.padding(vertical=10.dp),color=Muted,fontSize=12.sp,lineHeight=19.sp);TextButton(onClick={showPrivacy=true}){Text("查看隐私政策")};OutlinedButton(onClick={context.startActivity(Intent.createChooser(vm.export(context),"导出家庭资料"))},Modifier.fillMaxWidth()){Icon(Icons.Default.UploadFile,null);Text(" 导出家庭资料")};OutlinedButton(onClick={confirmDelete=true},Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=Accent)){Text("删除本机全部数据")};Spacer(Modifier.height(18.dp))}}}
    if(adding)PersonDialog(null,{adding=false},{vm.savePerson(it);adding=false},{});editing?.let{p->PersonDialog(p,{editing=null},{vm.savePerson(it);editing=null},{vm.deletePerson(p);editing=null})};if(confirmDelete)AlertDialog(onDismissRequest={confirmDelete=false},title={Text("删除全部家庭资料？")},text={Text("这会删除记忆、成员、采访问题和录音，且无法撤销。")},confirmButton={Button(onClick={vm.deleteAll();confirmDelete=false},colors=ButtonDefaults.buttonColors(containerColor=Accent)){Text("确认删除")}},dismissButton={TextButton(onClick={confirmDelete=false}){Text("取消")}});if(showPrivacy)AlertDialog(onDismissRequest={showPrivacy=false},title={Text("隐私摘要")},text={Text("家庭记忆和录音默认只保存在本机，系统云备份已关闭。只有主动使用 AI 时才发送当前请求所需文字。应用不含广告，不读取通讯录、相册或定位，不提供未经授权的声音克隆。你可以随时导出或删除全部资料。")},confirmButton={Button(onClick={showPrivacy=false;context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/eeggyy123/Kinvoice-iOS/blob/main/docs/07-Android%E9%9A%90%E7%A7%81%E6%94%BF%E7%AD%96.md")))}){Text("查看完整政策")}},dismissButton={TextButton(onClick={showPrivacy=false}){Text("关闭")}})
}

@Composable fun PersonDialog(person:PersonEntity?,onDismiss:()->Unit,onSave:(PersonEntity)->Unit,onDelete:()->Unit){var name by remember{mutableStateOf(person?.name.orEmpty())};var relation by remember{mutableStateOf(person?.relation.orEmpty())};var role by remember{mutableStateOf(person?.role?:"讲述者")};var note by remember{mutableStateOf(person?.note.orEmpty())};AlertDialog(onDismissRequest=onDismiss,title={Text(if(person==null)"添加家庭成员" else "编辑家庭成员")},text={Column{Field("姓名",name){name=it};Field("家庭关系",relation){relation=it};Text("角色",color=Muted,fontSize=12.sp);Row{listOf("讲述者","编辑者","只读成员").forEach{FilterChip(role==it,{role=it},{Text(it)},modifier=Modifier.padding(end=5.dp))}};Field("人物介绍",note,3){note=it}}},confirmButton={Button(onClick={if(name.isNotBlank())onSave(PersonEntity(person?.id?:java.util.UUID.randomUUID().toString(),name,relation.ifBlank{"家人"},role,note))}){Text("保存")}},dismissButton={Row{if(person!=null)TextButton(onClick=onDelete){Text("删除",color=Accent)};TextButton(onClick=onDismiss){Text("取消")}}})}
@Composable fun DataRow(label:String,value:String){Row(Modifier.fillMaxWidth().padding(vertical=12.dp)){Text(label,Modifier.weight(1f));Text(value,color=Muted)}}
@Composable fun Field(label:String,value:String,lines:Int=1,onChange:(String)->Unit){Column(Modifier.padding(vertical=7.dp)){Text(label,color=Muted,fontSize=12.sp,fontWeight=FontWeight.Medium);OutlinedTextField(value,onChange,Modifier.fillMaxWidth(),minLines=lines,maxLines=if(lines==1)1 else 8,singleLine=lines==1)}}
