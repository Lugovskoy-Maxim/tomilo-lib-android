package ru.tomilo.lib.mobile.data.repo

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.Request
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.api.NetworkModule
import ru.tomilo.lib.mobile.data.api.TomiloApi
import ru.tomilo.lib.mobile.data.download.DownloadStage
import ru.tomilo.lib.mobile.data.local.OfflineChapterEntity
import ru.tomilo.lib.mobile.data.local.OfflineChapterMeta
import ru.tomilo.lib.mobile.data.local.OfflineDao
import ru.tomilo.lib.mobile.data.local.OfflineTitleEntity
import java.io.File

class OfflineRepository(
    private val context: Context,
    private val api: TomiloApi,
    private val dao: OfflineDao,
    private val authRepository: AuthRepository,
) {
    private val http by lazy { NetworkModule.createMediaClient(context) }
    private val json = NetworkModule.json

    fun observeAll(): Flow<List<OfflineChapterEntity>> = dao.observeAll()
    fun observeByTitle(titleId: String): Flow<List<OfflineChapterEntity>> = dao.observeByTitle(titleId)
    fun observeTitles(): Flow<List<OfflineTitleEntity>> = dao.observeTitles()

    suspend fun isDownloaded(chapterId: String): Boolean = dao.isDownloaded(chapterId)

    suspend fun getEntity(chapterId: String) = dao.get(chapterId)

    suspend fun getTitleMeta(titleId: String) = dao.getTitle(titleId)

    fun parseChapterMeta(jsonStr: String): List<OfflineChapterMeta> =
        runCatching { json.decodeFromString<List<OfflineChapterMeta>>(jsonStr) }.getOrDefault(emptyList())

    /**
     * Тянет карточку тайтла + список глав и сохраняет локально.
     * Вызывается при скачивании и при фоновом обновлении.
     */
    suspend fun syncTitleCatalog(
        titleId: String,
        fallbackName: String = "",
        fallbackSlug: String = "",
        fallbackCover: String? = null,
    ): Result<OfflineTitleEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val detail = runCatching { api.titleById(titleId) }.getOrNull()
                ?.takeIf { it.success }?.data
            val chapters = runCatching {
                api.chaptersByTitle(titleId, page = 1, limit = 500, sortOrder = "asc")
            }.getOrNull()?.takeIf { it.success }?.data?.chapters.orEmpty()

            val metaList = chapters.map { ch ->
                OfflineChapterMeta(
                    chapterId = ch.stableId(),
                    chapterNumber = ch.numberLabel(),
                    name = ch.name,
                    pagesCount = ch.pagesCount ?: ch.pages?.size,
                    releaseDate = ch.releaseDate,
                )
            }

            val existing = dao.getTitle(titleId)
            val entity = OfflineTitleEntity(
                titleId = titleId,
                name = detail?.name?.takeIf { it.isNotBlank() }
                    ?: fallbackName.ifBlank { existing?.name ?: "Тайтл" },
                slug = detail?.slug?.takeIf { it.isNotBlank() }
                    ?: fallbackSlug.ifBlank { existing?.slug.orEmpty() },
                coverImage = detail?.coverImage ?: fallbackCover ?: existing?.coverImage,
                type = detail?.type ?: existing?.type,
                status = detail?.status ?: existing?.status,
                description = detail?.description ?: existing?.description,
                totalChapters = detail?.totalChapters
                    ?: metaList.size.takeIf { it > 0 }
                    ?: existing?.totalChapters,
                averageRating = detail?.averageRating ?: existing?.averageRating,
                releaseYear = detail?.releaseYear ?: existing?.releaseYear,
                chaptersJson = if (metaList.isNotEmpty()) {
                    json.encodeToString(metaList)
                } else {
                    existing?.chaptersJson.orEmpty()
                },
                lastSyncedAt = System.currentTimeMillis(),
            )
            dao.upsertTitle(entity)
            entity
        }
    }

    /** Обновить локальные тайтлы, у которых есть скачанные главы (или снапшот). */
    suspend fun refreshStaleTitles(maxAgeMs: Long = 6 * 60 * 60 * 1000L): Int =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val candidates = buildSet {
                addAll(dao.distinctTitleIds())
                addAll(dao.allTitles().map { it.titleId })
                File(context.filesDir, "offline").listFiles()
                    ?.mapNotNull { it.name.takeIf { n -> n.isNotBlank() } }
                    ?.let { addAll(it) }
            }.filter { it.isNotBlank() }

            var updated = 0
            for (id in candidates) {
                val existing = dao.getTitle(id)
                val stale = existing == null || now - existing.lastSyncedAt > maxAgeMs
                if (!stale) continue
                if (syncTitleCatalog(id).isSuccess) updated++
            }
            updated
        }

    suspend fun getLocalPages(chapterId: String): List<String>? {
        val entity = dao.get(chapterId) ?: return null
        val dir = File(entity.localDir)
        if (!dir.isDirectory) return null
        return dir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp", "avif") }
            ?.sortedBy { it.name }
            ?.map { it.absolutePath }
    }

    suspend fun offlineBytesTotal(): Long = withContext(Dispatchers.IO) {
        File(context.filesDir, "offline")
            .walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    suspend fun clearAllOffline() = withContext(Dispatchers.IO) {
        File(context.filesDir, "offline").deleteRecursively()
        dao.clearAll()
        dao.clearTitles()
    }

    /**
     * Скачивание главы (Premium). [onStage] — этапы для UI.
     */
    suspend fun downloadChapter(
        titleId: String,
        titleName: String,
        titleSlug: String,
        titleCover: String?,
        chapterId: String,
        onStage: (
            stage: DownloadStage,
            pagesDone: Int,
            pagesTotal: Int,
            message: String?,
        ) -> Unit = { _, _, _, _ -> },
        onProgress: (downloaded: Int, total: Int) -> Unit = { _, _ -> },
    ): Result<OfflineChapterEntity> = withContext(Dispatchers.IO) {
        runCatching {
            onStage(DownloadStage.CheckingAccess, 0, 0, null)
            if (!authRepository.isLoggedIn()) error("Войдите в аккаунт")
            if (!authRepository.isPremium()) {
                error("Офлайн-чтение доступно только Premium")
            }
            // Всегда обновляем метаданные тайтла (список глав для офлайн-UI)
            runCatching {
                syncTitleCatalog(titleId, titleName, titleSlug, titleCover)
            }

            if (dao.isDownloaded(chapterId)) {
                onStage(DownloadStage.Completed, 0, 0, "Уже скачано")
                return@runCatching dao.get(chapterId)!!
            }

            onStage(DownloadStage.FetchingChapter, 0, 0, null)
            val res = api.chapterById(chapterId)
            if (!res.success) error(res.message ?: "Не удалось получить главу")
            val chapter = res.data ?: error("Глава пуста")
            val pages = chapter.pages.orEmpty()
            if (pages.isEmpty()) error("У главы нет страниц")

            val root = File(context.filesDir, "offline/$titleId/$chapterId")
            if (root.exists()) root.deleteRecursively()
            root.mkdirs()

            var bytes = 0L
            pages.forEachIndexed { index, path ->
                onStage(DownloadStage.DownloadingPages, index, pages.size, null)
                val url = MediaUrl.resolve(path)
                val ext = path.substringAfterLast('.', "jpg").filter { it.isLetterOrDigit() }.take(5)
                    .ifBlank { "jpg" }
                val out = File(root, String.format("%04d.%s", index + 1, ext))
                val req = Request.Builder().url(url).get().build()
                http.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("Не удалось скачать страницу ${index + 1}")
                    }
                    val body = response.body ?: error("Пустой ответ страницы")
                    body.byteStream().use { input ->
                        out.outputStream().use { output -> input.copyTo(output) }
                    }
                    bytes += out.length()
                }
                onProgress(index + 1, pages.size)
                onStage(DownloadStage.DownloadingPages, index + 1, pages.size, null)
            }

            onStage(DownloadStage.Saving, pages.size, pages.size, null)
            val entity = OfflineChapterEntity(
                chapterId = chapterId,
                titleId = titleId,
                titleName = titleName,
                titleSlug = titleSlug,
                titleCover = titleCover,
                chapterNumber = chapter.numberLabel(),
                chapterName = chapter.name,
                pageCount = pages.size,
                localDir = root.absolutePath,
                downloadedAt = System.currentTimeMillis(),
                bytesTotal = bytes,
            )
            dao.upsert(entity)
            // ещё раз синк — на случай новых глав
            runCatching { syncTitleCatalog(titleId, titleName, titleSlug, titleCover) }
            onStage(DownloadStage.Completed, pages.size, pages.size, null)
            entity
        }
    }

    suspend fun deleteChapter(chapterId: String) = withContext(Dispatchers.IO) {
        val entity = dao.get(chapterId) ?: return@withContext
        File(entity.localDir).deleteRecursively()
        dao.delete(chapterId)
    }

    suspend fun deleteTitle(titleId: String) = withContext(Dispatchers.IO) {
        val items = dao.chapterIdsForTitle(titleId)
        items.forEach { id ->
            dao.get(id)?.let { File(it.localDir).deleteRecursively() }
            dao.delete(id)
        }
        dao.deleteTitleMeta(titleId)
        File(context.filesDir, "offline/$titleId").deleteRecursively()
    }
}
