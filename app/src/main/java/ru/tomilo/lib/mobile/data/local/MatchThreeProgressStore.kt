package ru.tomilo.lib.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.tomilo.lib.mobile.core.MatchThreeEngine

private val Context.matchThreeStore by preferencesDataStore("match_three_progress")

@Serializable
data class MatchThreeRecord(val level: Int, val at: Long)
@Serializable
data class MatchThreeSave(val lives: Int = 5, val lifeStamp: Long = 0, val records: List<MatchThreeRecord> = emptyList(), val customLevels: List<MatchThreeCustomLevel> = emptyList())
@Serializable
data class MatchThreeCustomLevel(val number: Int, val target: Int, val moves: Int, val color: Int, val obstacleKinds: List<Int>, val seed: Int)

class MatchThreeProgressStore(private val context: Context) {
    private val key = stringPreferencesKey("save_v1")
    private val json = Json { ignoreUnknownKeys = true }
    suspend fun read(): MatchThreeSave {
        val raw = context.matchThreeStore.data.first()[key] ?: return MatchThreeSave(lifeStamp = System.currentTimeMillis())
        return runCatching { json.decodeFromString<MatchThreeSave>(raw) }.getOrDefault(MatchThreeSave(lifeStamp = System.currentTimeMillis()))
    }
    suspend fun write(save: MatchThreeSave) { context.matchThreeStore.edit { it[key] = json.encodeToString(save) } }
}
