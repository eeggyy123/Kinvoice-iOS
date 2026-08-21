package com.kinvoice.family

import org.junit.Assert.*
import org.junit.Test

class LocalKnowledgeTest {
    private val memories = listOf(
        MemoryEntity(id="food",title="外婆的红烧肉",content="冰糖要小火慢慢炒成糖色，最后再放盐。",author="林秀兰",topics="家常菜|年夜饭",quote="糖色不能急。",confirmed=true),
        MemoryEntity(id="train",title="爷爷第一次坐火车",content="十八岁去上海学修机器。",author="陈国安",topics="远行|成长",confirmed=true)
    )

    @Test fun `red braised pork question only returns food source`() {
        val (answer,sources)=LocalKnowledge.ask("外婆做红烧肉最关键的一步是什么？",memories)
        assertEquals("糖色不能急。",answer)
        assertEquals(listOf("food"),sources.map{it.id})
    }

    @Test fun `unknown question refuses to invent`() {
        val (answer,sources)=LocalKnowledge.ask("家里谁会修理宇宙飞船？",memories)
        assertTrue(sources.isEmpty())
        assertTrue(answer.contains("暂未找到"))
    }
}
