package ru.tomilo.lib.mobile.data.repo

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import ru.tomilo.lib.mobile.data.api.HistoryEntryDto
import ru.tomilo.lib.mobile.data.api.NetworkModule
import ru.tomilo.lib.mobile.data.api.RateTitleRequest
import ru.tomilo.lib.mobile.data.api.ReadIdsDto
import ru.tomilo.lib.mobile.data.api.ReadingProgressDto
import ru.tomilo.lib.mobile.data.api.TomiloApi

data class HistoryReward(
    val experienceGained: Int = 0,
    val coinsGained: Int = 0,
    val reason: String? = null,
)

class HistoryRepository(private val api: TomiloApi) {
    private val json = NetworkModule.json

    suspend fun history(page: Int = 1): Result<List<HistoryEntryDto>> = runCatching {
        val res = api.readingHistory(page = page, limit = 50, light = true)
        if (!res.success) error(res.message ?: "Ошибка истории")
        parseHistory(res.data)
    }

    suspend fun progress(titleId: String): Result<ReadingProgressDto> = runCatching {
        if (titleId.isBlank()) error("empty titleId")
        val res = api.readingProgress(titleId)
        if (!res.success) error(res.message ?: "Ошибка прогресса")
        res.data ?: ReadingProgressDto(titleId = titleId)
    }

    /** Параллельно для списка тайтлов (закладки). */
    suspend fun progressMap(titleIds: Collection<String>): Map<String, ReadingProgressDto> {
        val ids = titleIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return emptyMap()
        // Последовательно безопаснее для API; список закладок обычно небольшой
        val out = LinkedHashMap<String, ReadingProgressDto>()
        for (id in ids) {
            progress(id).getOrNull()?.let { out[id] = it }
        }
        return out
    }

    /**
     * Id прочитанных глав тайтла — для отметок на списке глав.
     */
    suspend fun readIds(titleId: String): Result<Set<String>> = runCatching {
        if (titleId.isBlank()) return@runCatching emptySet()
        val res = api.historyReadIds(titleId)
        if (!res.success) {
            // fallback: full history entry for title
            return@runCatching readIdsFromTitleHistory(titleId)
        }
        val data = res.data ?: ReadIdsDto()
        data.chapterIds.filter { it.isNotBlank() }.toSet()
    }

    suspend fun markRead(titleId: String, chapterId: String): Result<HistoryReward> = runCatching {
        if (titleId.isBlank() || chapterId.isBlank()) return@runCatching HistoryReward()
        val res = api.addHistory(titleId, chapterId)
        if (!res.success) error(res.message ?: "Не удалось сохранить прогресс")
        parseHistoryReward(res.data)
    }

    suspend fun rateTitle(titleId: String, rating: Int): Result<Unit> = runCatching {
        val res = api.rateTitle(titleId, RateTitleRequest(rating.coerceIn(1, 10)))
        if (!res.success) error(res.message ?: "Не удалось оценить")
    }

    suspend fun myTitleRating(titleId: String): Result<Int?> = runCatching {
        if (titleId.isBlank()) return@runCatching null
        val res = api.myTitleRating(titleId)
        if (!res.success) error(res.message ?: "Не удалось загрузить оценку")
        val data = res.data ?: return@runCatching null
        when (data) {
            is kotlinx.serialization.json.JsonPrimitive -> data.content.toIntOrNull()
            is JsonObject -> data["rating"]?.jsonPrimitive?.content?.toIntOrNull()
            else -> null
        }
    }

    suspend fun deleteTitleHistory(titleId: String): Result<Unit> = runCatching {
        val res = api.deleteTitleHistory(titleId)
        if (!res.success) error(res.message ?: "Не удалось удалить")
    }

    private suspend fun readIdsFromTitleHistory(titleId: String): Set<String> {
        val res = api.historyByTitle(titleId)
        if (!res.success) return emptySet()
        val data = res.data ?: return emptySet()
        val chaptersEl: JsonArray? = when (data) {
            is JsonArray -> data
            is JsonObject -> (data["chapters"] as? JsonArray) ?: (data["items"] as? JsonArray)
            else -> null
        }
        if (chaptersEl == null) return emptySet()
        return chaptersEl.mapNotNull { ch ->
            if (ch !is JsonObject) return@mapNotNull null
            val idEl = ch["chapterId"]
            when (idEl) {
                is kotlinx.serialization.json.JsonPrimitive -> idEl.content
                is JsonObject -> idEl["_id"]?.toString()?.trim('"')
                else -> null
            }
        }.filter { it.isNotBlank() }.toSet()
    }

    private fun parseHistory(data: JsonElement?): List<HistoryEntryDto> {
        if (data == null) return emptyList()
        val arr = when (data) {
            is JsonArray -> data
            is JsonObject -> {
                val nested = data["items"]
                    ?: data["history"]
                    ?: data["data"]
                    ?: data["results"]
                when (nested) {
                    is JsonArray -> nested
                    is JsonObject -> nested["items"] as? JsonArray
                    else -> null
                }
            }
            else -> null
        } ?: return emptyList()
        return arr.mapNotNull {
            runCatching { json.decodeFromJsonElement<HistoryEntryDto>(it) }.getOrNull()
        }.filter { it.titleKey().isNotBlank() }
    }

    private fun parseHistoryReward(data: JsonElement?): HistoryReward {
        val root = data as? JsonObject ?: return HistoryReward()
        val progress = root["progress"] as? JsonObject ?: root
        fun int(vararg keys: String): Int = keys.firstNotNullOfOrNull { key ->
            progress[key]?.jsonPrimitive?.intOrNull ?: root[key]?.jsonPrimitive?.intOrNull
        } ?: 0
        val reason = progress["reason"]?.jsonPrimitive?.content
            ?: root["reason"]?.jsonPrimitive?.content
        return HistoryReward(
            experienceGained = int("expGained", "experienceGained"),
            coinsGained = int("bonusCoins", "coinsGained"),
            reason = reason,
        )
    }
}
