package com.kinvoice.family

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val summary: String = "",
    val content: String,
    val author: String = "家人",
    val topics: String = "",
    val timeHint: String = "",
    val location: String = "",
    val quote: String = "",
    val audioPath: String = "",
    val confirmed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun topicList() = topics.split("|").map(String::trim).filter(String::isNotEmpty)
}

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val relation: String,
    val role: String = "讲述者",
    val note: String = ""
)

@Entity(tableName = "prompts")
data class PromptEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val text: String,
    val position: Int
)

@Dao
interface KinVoiceDao {
    @Query("SELECT * FROM memories ORDER BY createdAt DESC") fun observeMemories(): Flow<List<MemoryEntity>>
    @Query("SELECT * FROM people ORDER BY name") fun observePeople(): Flow<List<PersonEntity>>
    @Query("SELECT * FROM prompts ORDER BY position") fun observePrompts(): Flow<List<PromptEntity>>
    @Query("SELECT COUNT(*) FROM memories") suspend fun memoryCount(): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveMemory(memory: MemoryEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePerson(person: PersonEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePrompts(prompts: List<PromptEntity>)
    @Delete suspend fun deleteMemory(memory: MemoryEntity)
    @Delete suspend fun deletePerson(person: PersonEntity)
    @Query("DELETE FROM memories") suspend fun deleteMemories()
    @Query("DELETE FROM people") suspend fun deletePeople()
    @Query("DELETE FROM prompts") suspend fun deletePrompts()
}

@Database(entities = [MemoryEntity::class, PersonEntity::class, PromptEntity::class], version = 1, exportSchema = true)
abstract class KinVoiceDatabase : RoomDatabase() {
    abstract fun dao(): KinVoiceDao
    companion object {
        @Volatile private var instance: KinVoiceDatabase? = null
        fun get(context: Context): KinVoiceDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context, KinVoiceDatabase::class.java, "kinvoice.db")
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
        }
    }
}

class KinVoiceRepository(private val context: Context, private val dao: KinVoiceDao) {
    val memories = dao.observeMemories()
    val people = dao.observePeople()
    val prompts = dao.observePrompts()

    suspend fun seedIfEmpty() {
        val preferences = context.getSharedPreferences("kinvoice_state", Context.MODE_PRIVATE)
        if (preferences.getBoolean("initial_seed_completed", false) || dao.memoryCount() > 0) {
            preferences.edit().putBoolean("initial_seed_completed", true).apply()
            return
        }
        listOf(
            PersonEntity(name = "林秀兰", relation = "外婆", note = "擅长苏州家常菜和传统节气手艺"),
            PersonEntity(name = "周明芳", relation = "妈妈", note = "喜欢缝纫，也负责整理旧照片"),
            PersonEntity(name = "陈国安", relation = "爷爷", note = "年轻时学习机器维修"),
            PersonEntity(name = "陈建平", relation = "爸爸", role = "编辑者", note = "家里的摄影记录者")
        ).forEach { dao.savePerson(it) }
        val demo = listOf(
            MemoryEntity(title="外婆的红烧肉：先炒糖色，再等香气", summary="一道从曾外婆传下来的年夜饭做法。", content="外婆说，糖色不能急。冰糖在小火里慢慢化开，颜色像老茶时才下肉。肉要先擦干水，不然油会溅。加热水没过一半，最后十分钟再放盐。她年轻时第一次掌勺是在 1978 年的除夕，这道菜后来成了家里的年夜饭。", author="林秀兰", topics="家常菜|年夜饭", timeHint="1978 年除夕", location="苏州", quote="糖色不能急，闻到焦糖香才算到时候。", confirmed=true),
            MemoryEntity(title="旧缝纫机旁学会的耐心", summary="母亲教女儿做衣服，也教她遇事先量清楚。", content="妈妈踩缝纫机前，总会把布铺平量三遍。她说剪刀落下去就不能反悔，生活里许多事也是这样。那台缝纫机是她结婚时买的，后来给家里四个孩子做过棉衣。", author="周明芳", topics="手艺|家训", timeHint="1980 年代", location="无锡", quote="剪刀落下去就不能反悔，先量清楚。", confirmed=true),
            MemoryEntity(title="爷爷第一次坐火车去上海", summary="一张硬座票和一只搪瓷杯，装着第一次远行的忐忑。", content="爷爷十八岁第一次坐火车去上海学修机器。奶奶给他装了三个饭团，他一路舍不得吃完。到了上海以后，他把那张硬座票夹进字典里，提醒自己学会一门真正能帮到别人的手艺。", author="陈国安", topics="远行|成长", timeHint="1966 年夏", location="上海", quote="车窗外的电线杆一根接一根，像在带我往前走。", confirmed=true),
            MemoryEntity(title="端午香囊里的草木配方", summary="外婆每年端午都会教孩子辨认艾叶、薄荷和藿香。", content="外婆会把晒干的艾叶、薄荷和藿香分成小碟，让孩子蒙着眼睛闻。认对以后，再一起装进布袋缝好。她说配方不是最重要的，记住季节和一家人一起做香囊的过程才重要。", author="林秀兰", topics="节气|手艺", timeHint="每年端午", location="苏州", quote="先闻，再认叶子的边，草木才记得牢。", confirmed=true),
            MemoryEntity(title="爸爸教我的第一张全家福", summary="不是看镜头，而是先让每个人都放松下来。", content="爸爸把胶片相机交给我时，只讲了光圈和快门最基本的用法。他更在意我有没有看到每个人当时的样子。那张全家福里，小姨正在笑，外公还没来得及看镜头，后来反而成了大家最喜欢的一张。", author="陈建平", topics="影像|家庭", timeHint="2003 年春节", location="杭州", quote="好照片不是把人排整齐，是把那一刻留下来。", confirmed=false)
        )
        demo.forEach { dao.saveMemory(it) }
        savePrompts(defaultPrompts)
        preferences.edit().putBoolean("initial_seed_completed", true).apply()
    }

    suspend fun saveMemory(memory: MemoryEntity) = dao.saveMemory(memory)
    suspend fun deleteMemory(memory: MemoryEntity) {
        if (memory.audioPath.isNotBlank()) context.getFileStreamPath(memory.audioPath).delete()
        dao.deleteMemory(memory)
    }
    suspend fun savePerson(person: PersonEntity) = dao.savePerson(person)
    suspend fun deletePerson(person: PersonEntity) = dao.deletePerson(person)
    suspend fun savePrompts(values: List<String>) {
        dao.deletePrompts()
        dao.savePrompts(values.filter(String::isNotBlank).take(20).mapIndexed { i, value -> PromptEntity(text=value.trim(), position=i) })
    }
    suspend fun deleteEverything(memories: List<MemoryEntity>) {
        memories.forEach { if (it.audioPath.isNotBlank()) context.getFileStreamPath(it.audioPath).delete() }
        dao.deleteMemories(); dao.deletePeople(); dao.deletePrompts()
    }
    companion object {
        val defaultPrompts = listOf("小时候家里过年，最不能少的一道菜是什么？", "你年轻时学会的第一门手艺是什么？", "这道家常菜是谁教你的？", "家里有哪条规矩，背后藏着什么故事？", "你第一次离开家去远方时，记得什么？")
    }
}
