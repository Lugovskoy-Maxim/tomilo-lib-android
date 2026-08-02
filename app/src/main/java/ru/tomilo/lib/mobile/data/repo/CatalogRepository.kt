package ru.tomilo.lib.mobile.data.repo

import ru.tomilo.lib.mobile.data.api.CatalogFilterOptionsDto
import ru.tomilo.lib.mobile.data.api.CatalogPageDto
import ru.tomilo.lib.mobile.data.api.CatalogQuery
import ru.tomilo.lib.mobile.data.api.CatalogTitleDto
import ru.tomilo.lib.mobile.data.api.ChapterDto
import ru.tomilo.lib.mobile.data.api.SearchHitDto
import ru.tomilo.lib.mobile.data.api.TitleDetailDto
import ru.tomilo.lib.mobile.data.api.TomiloApi

class CatalogRepository(private val api: TomiloApi) {
    suspend fun catalog(query: CatalogQuery): Result<CatalogPageDto> = runCatching {
        val res = api.catalogTitles(
            page = query.page,
            limit = query.limit,
            search = query.search?.ifBlank { null },
            genres = query.genres?.ifBlank { null },
            types = query.types?.ifBlank { null },
            status = query.status?.ifBlank { null },
            sortBy = query.sortBy,
            sortOrder = query.sortOrder,
            releaseYears = query.releaseYears?.ifBlank { null },
            ageLimits = query.ageLimits?.ifBlank { null },
            includeAdult = if (query.includeAdult) true else null,
        )
        if (!res.success) error(res.message ?: "Ошибка каталога")
        res.data ?: CatalogPageDto()
    }

    suspend fun filterOptions(): Result<CatalogFilterOptionsDto> = runCatching {
        val res = api.catalogFilterOptions()
        if (!res.success) error(res.message ?: "Ошибка фильтров")
        res.data ?: CatalogFilterOptionsDto()
    }

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

    /**
     * Все главы тайтла (сервер режет list limit до 200 — ходим по страницам).
     */
    suspend fun chaptersAll(titleId: String, pageSize: Int = 200): Result<List<ChapterDto>> =
        runCatching {
            val all = mutableListOf<ChapterDto>()
            var page = 1
            val limit = pageSize.coerceIn(1, 200)
            while (page <= 100) {
                val res = api.chaptersByTitle(
                    titleId = titleId,
                    page = page,
                    limit = limit,
                    sortOrder = "asc",
                )
                if (!res.success) error(res.message ?: "Ошибка глав")
                val batch = res.data?.chapters.orEmpty()
                all.addAll(batch)
                val pag = res.data?.pagination
                val hasMore = pag?.hasMore == true ||
                    (pag != null && pag.pages > 0 && page < pag.pages) ||
                    (pag == null && batch.size >= limit)
                if (!hasMore || batch.isEmpty()) break
                page++
            }
            all.distinctBy { it.stableId() }.filter { it.stableId().isNotBlank() }
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
