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

    suspend fun initializeIfNeeded() {
        val preferences = context.getSharedPreferences("kinvoice_state", Context.MODE_PRIVATE)
        if (preferences.getBoolean("initial_seed_completed", false)) {
            preferences.edit().putBoolean("onboarding_completed", true).apply()
        }
        if (!preferences.getBoolean("content_initialized", false)) {
            savePrompts(defaultPrompts)
            preferences.edit().putBoolean("content_initialized", true).apply()
        }
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
        savePrompts(defaultPrompts)
    }
    companion object {
        val defaultPrompts = listOf("小时候家里过年，最不能少的一道菜是什么？", "你年轻时学会的第一门手艺是什么？", "这道家常菜是谁教你的？", "家里有哪条规矩，背后藏着什么故事？", "你第一次离开家去远方时，记得什么？")
    }
}
