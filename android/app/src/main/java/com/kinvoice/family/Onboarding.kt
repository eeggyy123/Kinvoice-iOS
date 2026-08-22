package com.kinvoice.family

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class OnboardingPage(val icon:ImageVector,val title:String,val body:String)

@Composable
fun OnboardingScreen(onComplete:(PersonEntity)->Unit){
    val pages=listOf(
        OnboardingPage(Icons.Default.AutoStories,"每个家庭，都有值得留下的声音","从一道菜、一门手艺或一次远行开始，把口述变成可寻找的家庭知识。"),
        OnboardingPage(Icons.Default.FactCheck,"AI 负责提问，事实由你确认","采访者会温和追问并整理草稿；保存前，你可以逐字修改或放弃。"),
        OnboardingPage(Icons.Default.Lock,"默认保存在本机","只有主动使用 AI 时才发送本次所需文字；资料可以随时导出和删除。")
    )
    var page by rememberSaveable{mutableIntStateOf(0)}
    var creating by rememberSaveable{mutableStateOf(false)}
    var name by rememberSaveable{mutableStateOf("")};var relation by rememberSaveable{mutableStateOf("")}
    Surface(Modifier.fillMaxSize(),color=Color(0xFFF7F8F6)){
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){pages.indices.forEach{index->Box(Modifier.height(4.dp).weight(1f).background(if(index<=page)Color(0xFFA94331) else Color(0xFFDDE1DD),CircleShape))}}
            Spacer(Modifier.weight(1f))
            AnimatedContent(page,label="onboarding-page"){index->Column(horizontalAlignment=Alignment.CenterHorizontally){Surface(shape=CircleShape,color=Color(0xFFDCEAE5),modifier=Modifier.size(82.dp)){Box(contentAlignment=Alignment.Center){Icon(pages[index].icon,null,tint=Color(0xFF2F6F62),modifier=Modifier.size(36.dp))}};Text(pages[index].title,Modifier.padding(top=24.dp),fontSize=27.sp,fontWeight=FontWeight.Bold);Text(pages[index].body,Modifier.padding(top=12.dp),color=Color(0xFF68706B),lineHeight=24.sp)}}
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(creating,enter=fadeIn(),exit=fadeOut()){Column{Text("先建立第一位家人",fontWeight=FontWeight.Bold);OutlinedTextField(name,{name=it},Modifier.fillMaxWidth().padding(top=8.dp),label={Text("姓名或称呼")},singleLine=true);OutlinedTextField(relation,{relation=it},Modifier.fillMaxWidth().padding(top=8.dp),label={Text("与我的关系")},singleLine=true)}}
            Button(onClick={when{page<pages.lastIndex->page++;!creating->creating=true;name.isNotBlank()->onComplete(PersonEntity(name=name.trim(),relation=relation.trim().ifBlank{"家人"}))}},Modifier.fillMaxWidth().padding(top=16.dp),enabled=!creating||name.isNotBlank()){Text(when{page<pages.lastIndex->"继续";!creating->"建立我的家庭";else->"创建并开始采访"})}
            if(page>0&&!creating)TextButton(onClick={page--}){Text("返回")}
        }
    }
}
