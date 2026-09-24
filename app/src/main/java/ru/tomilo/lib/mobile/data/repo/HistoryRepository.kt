package ru.tomilo.lib.mobile.data.repo

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
import ru.tomilo.lib.mobile.data.api.RateChapterRequest
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

    suspend fun history(page: Int = 1): Result<List<HistoryEntryDto>> = runCatchingCancellable {
        val res = api.readingHistory(page = page, limit = 50, light = true)
        if (!res.success) error(res.message ?: "Ошибка истории")
        parseHistory(res.data)
    }

    suspend fun progress(titleId: String): Result<ReadingProgressDto> = runCatchingCancellable {
        if (titleId.isBlank()) error("empty titleId")
        val res = api.readingProgress(titleId)
        if (!res.success) error(res.message ?: "Ошибка прогресса")
        res.data ?: ReadingProgressDto(titleId = titleId)
    }

    /** Fetch title progress concurrently with a small limit to keep list screens responsive. */
    suspend fun progressMap(titleIds: Collection<String>): Map<String, ReadingProgressDto> = coroutineScope {
        val ids = titleIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return@coroutineScope emptyMap()
        val semaphore = Semaphore(permits = 4)
        ids.map { id ->
            async {
                semaphore.withPermit {
                    id to progress(id).getOrNull()
                }
            }
        }.mapNotNull { request ->
            val (id, progress) = request.await()
            progress?.let { id to it }
        }
            .toMap(LinkedHashMap())
    }

    /**
     * Id прочитанных глав тайтла — для отметок на списке глав.
     */
    suspend fun readIds(titleId: String): Result<Set<String>> = runCatchingCancellable {
        if (titleId.isBlank()) return@runCatchingCancellable emptySet()
        val res = api.historyReadIds(titleId)
        if (!res.success) {
            // fallback: full history entry for title
            return@runCatchingCancellable readIdsFromTitleHistory(titleId)
        }
        val data = res.data ?: ReadIdsDto()
        data.chapterIds.filter { it.isNotBlank() }.toSet()
    }

    suspend fun markRead(titleId: String, chapterId: String): Result<HistoryReward> = runCatchingCancellable {
        if (titleId.isBlank() || chapterId.isBlank()) return@runCatchingCancellable HistoryReward()
        val res = api.addHistory(titleId, chapterId)
        if (!res.success) error(res.message ?: "Не удалось сохранить прогресс")
        parseHistoryReward(res.data)
    }

    suspend fun removeChapterFromHistory(titleId: String, chapterId: String): Result<Unit> = runCatchingCancellable {
        if (titleId.isBlank() || chapterId.isBlank()) error("Не удалось определить главу")
        val res = api.removeChapterFromHistory(titleId, chapterId)
        if (!res.success) error(res.message ?: "Не удалось снять отметку о прочтении")
    }

    suspend fun rateTitle(titleId: String, rating: Int): Result<Unit> = runCatchingCancellable {
        val res = api.rateTitle(titleId, RateTitleRequest(rating.coerceIn(1, 10)))
        if (!res.success) error(res.message ?: "Не удалось оценить")
    }

    suspend fun myTitleRating(titleId: String): Result<Int?> = runCatchingCancellable {
        if (titleId.isBlank()) return@runCatchingCancellable null
        val res = api.myTitleRating(titleId)
        if (!res.success) error(res.message ?: "Не удалось загрузить оценку")
        val data = res.data ?: return@runCatchingCancellable null
        when (data) {
            is kotlinx.serialization.json.JsonPrimitive -> data.content.toIntOrNull()
            is JsonObject -> data["rating"]?.jsonPrimitive?.content?.toIntOrNull()
            else -> null
        }
    }

    suspend fun rateChapter(chapterId: String, rating: Int): Result<Unit> = runCatchingCancellable {
        val res = api.rateChapter(chapterId, RateChapterRequest(rating.coerceIn(1, 10)))
        if (!res.success) error(res.message ?: "Не удалось оценить главу")
    }

    suspend fun myChapterRating(chapterId: String): Result<Int?> = runCatchingCancellable {
        if (chapterId.isBlank()) return@runCatchingCancellable null
        val res = api.chapterRating(chapterId)
        if (!res.success) error(res.message ?: "Не удалось загрузить оценку главы")
        val data = res.data ?: return@runCatchingCancellable null
        when (data) {
            is kotlinx.serialization.json.JsonPrimitive -> data.content.toIntOrNull()
            is JsonObject -> data["userRating"]?.jsonPrimitive?.content?.toIntOrNull()
                ?: data["rating"]?.jsonPrimitive?.content?.toIntOrNull()
            else -> null
        }
    }

    suspend fun deleteTitleHistory(titleId: String): Result<Unit> = runCatchingCancellable {
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
