package ru.tomilo.lib.mobile.data.repo

import ru.tomilo.lib.mobile.data.api.CatalogTitleDto
import ru.tomilo.lib.mobile.data.api.ChapterDto
import ru.tomilo.lib.mobile.data.api.SearchHitDto
import ru.tomilo.lib.mobile.data.api.TitleDetailDto
import ru.tomilo.lib.mobile.data.api.TomiloApi

class CatalogRepository(private val api: TomiloApi) {
    suspend fun latestUpdates(limit: Int = 24): Result<List<CatalogTitleDto>> = runCatching {
        val res = api.latestUpdates(limit = limit)
        if (!res.success) error(res.message ?: "Ошибка загрузки")
        res.data.orEmpty()
    }

    suspend fun popular(limit: Int = 24): Result<List<CatalogTitleDto>> = runCatching {
        val res = api.popular(limit = limit)
        if (!res.success) error(res.message ?: "Ошибка загрузки")
        res.data.orEmpty()
    }

    suspend fun title(idOrSlug: String): Result<TitleDetailDto> = runCatching {
        val byId = runCatching { api.titleById(idOrSlug) }.getOrNull()
        val res = if (byId?.success == true && byId.data != null) {
            byId
        } else {
            api.titleBySlug(idOrSlug)
        }
        if (!res.success) error(res.message ?: "Тайтл не найден")
        res.data ?: error("Тайтл не найден")
    }

    suspend fun chapters(titleId: String, page: Int = 1, limit: Int = 100): Result<List<ChapterDto>> =
        runCatching {
            val res = api.chaptersByTitle(titleId, page = page, limit = limit, sortOrder = "asc")
            if (!res.success) error(res.message ?: "Ошибка глав")
            res.data?.chapters.orEmpty()
        }

    suspend fun chapter(chapterId: String): Result<ChapterDto> = runCatching {
        val res = api.chapterById(chapterId)
        if (!res.success) error(res.message ?: "Глава недоступна")
        res.data ?: error("Глава недоступна")
    }

    suspend fun chapterNext(chapterId: String): Result<ChapterDto> = runCatching {
        val res = api.chapterNext(chapterId)
        if (!res.success) error(res.message ?: "Нет следующей главы")
        res.data ?: error("Нет следующей главы")
    }

    suspend fun chapterPrev(chapterId: String): Result<ChapterDto> = runCatching {
        val res = api.chapterPrev(chapterId)
        if (!res.success) error(res.message ?: "Нет предыдущей главы")
        res.data ?: error("Нет предыдущей главы")
    }

    suspend fun search(query: String): Result<List<SearchHitDto>> = runCatching {
        val q = query.trim()
        if (q.length < 2) return@runCatching emptyList()
        val auto = api.searchAutocomplete(q = q, limit = 20, type = "titles")
        if (auto.success) {
            auto.data.orEmpty().filter { it.kind == null || it.kind == "title" }
        } else {
            val full = api.search(q = q, limit = 20, type = "titles")
            if (!full.success) error(full.message ?: "Ошибка поиска")
            full.data.orEmpty().filter { it.kind == null || it.kind == "title" }
        }
    }
}
