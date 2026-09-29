package ru.tomilo.lib.mobile.data.repo

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.Request
import ru.tomilo.lib.mobile.core.ImageIntegrity
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.api.NetworkModule
import ru.tomilo.lib.mobile.data.api.TomiloApi
import ru.tomilo.lib.mobile.data.download.DownloadStage
import ru.tomilo.lib.mobile.data.local.OfflineChapterEntity
import ru.tomilo.lib.mobile.data.local.OfflineChapterMeta
import ru.tomilo.lib.mobile.data.local.OfflineDao
import ru.tomilo.lib.mobile.data.local.OfflineTitleEntity
import java.io.File
import java.util.Locale

data class LocalChapterPages(
    val pageCount: Int,
    val slots: List<String?>,
    val complete: Boolean,
)

data class OfflineIntegrityReport(
    val validChapters: Int,
    val removedEntries: Int,
    val orphanDirectories: Int,
    val freedBytes: Long,
)

class OfflineRepository(
    private val context: Context,
    private val api: TomiloApi,
    private val dao: OfflineDao,
    private val authRepository: AuthRepository,
    private val adRewardStore: ru.tomilo.lib.mobile.data.local.AdRewardStore? = null,
) {
    private val http by lazy { NetworkModule.createMediaClient(context) }
    private val json = NetworkModule.json

    fun observeAll(): Flow<List<OfflineChapterEntity>> = dao.observeAll()
    fun observeByTitle(titleId: String): Flow<List<OfflineChapterEntity>> = dao.observeByTitle(titleId)
    fun observeTitles(): Flow<List<OfflineTitleEntity>> = dao.observeTitles()

    suspend fun isDownloaded(chapterId: String): Boolean = getLocalPages(chapterId)?.isNotEmpty() == true

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
        runCatchingCancellable {
            val detail = runCatchingCancellable { api.titleById(titleId) }.getOrNull()
                ?.takeIf { it.success }?.data
            // серверный MAX_LIST_LIMIT = 200 — грузим все страницы.
            // Сервер не всегда отдаёт корректную пагинацию (hasMore/pages), поэтому
            // идём дальше, пока страница приходит полной; стоп по пустой или повтору id.
            var catalogComplete = true
            val chapters = buildList {
                var page = 1
                val seen = mutableSetOf<String>()
                while (page <= 100) {
                    val res = runCatchingCancellable {
                        api.chaptersByTitle(titleId, page = page, limit = 200, sortOrder = "asc")
                    }.getOrNull()?.takeIf { it.success }?.data
                    if (res == null) {
                        catalogComplete = false
                        break
                    }
                    val batch = res.chapters
                    val fresh = batch.filter { seen.add(it.stableId()) }
                    addAll(fresh)
                    if (batch.isEmpty()) break
                    val pag = res.pagination
                    val serverSaysMore = pag?.hasMore == true || (pag != null && pag.pages > page)
                    val fullPage = batch.size >= 200
                    if (fresh.isEmpty()) break // сервер игнорирует page — дальше не продвинуться
                    if (!fullPage && !serverSaysMore) break
                    page++
                }
            }.distinctBy { it.stableId() }

            val metaList = chapters.map { ch ->
                OfflineChapterMeta(
                    chapterId = ch.stableId(),
                    chapterNumber = ch.numberLabel(),
                    name = ch.name,
                    pagesCount = ch.pagesCount ?: ch.pagePaths().size,
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
                chaptersJson = if (catalogComplete && metaList.isNotEmpty()) {
                    json.encodeToString(metaList)
                } else {
                    existing?.chaptersJson.orEmpty()
                },
                lastSyncedAt = if (catalogComplete) System.currentTimeMillis() else existing?.lastSyncedAt ?: 0L,
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

    suspend fun getLocalPages(chapterId: String): List<String>? =
        readLocalChapter(chapterId)?.slots?.filterNotNull()?.takeIf { it.isNotEmpty() }

    /**
     * Слоты совпадают с номерами страниц главы. Пустой слот — дыра или битый файл,
     * его нельзя выкидывать из списка: читалка показывает ошибку на этом номере.
     */
    suspend fun readLocalChapter(chapterId: String): LocalChapterPages? = withContext(Dispatchers.IO) {
        val entity = dao.get(chapterId) ?: return@withContext null
        val dir = File(entity.localDir)
        if (!dir.isDirectory) return@withContext null
        val byIndex = HashMap<Int, File>()
        dir.listFiles()?.forEach { file ->
            if (!file.isFile || file.name.endsWith(".part")) return@forEach
            val index = pageIndexFromName(file.name) ?: return@forEach
            if (!ImageIntegrity.isValidFile(file)) return@forEach
            val previous = byIndex[index]
            if (previous == null || file.lastModified() >= previous.lastModified()) {
                byIndex[index] = file
            }
        }
        val count = maxOf(entity.pageCount, (byIndex.keys.maxOrNull() ?: -1) + 1)
        if (count <= 0) return@withContext null
        val slots = List(count) { index -> byIndex[index]?.absolutePath }
        if (slots.all { it == null }) return@withContext null
        LocalChapterPages(
            pageCount = count,
            slots = slots,
            complete = slots.none { it == null },
        )
    }

    suspend fun repairChapter(chapterId: String): Result<OfflineChapterEntity> {
        val entity = getEntity(chapterId)
            ?: return Result.failure(IllegalStateException("Глава не скачана"))
        val meta = getTitleMeta(entity.titleId)
        return downloadChapter(
            titleId = entity.titleId,
            titleName = meta?.name ?: entity.titleName,
            titleSlug = meta?.slug ?: entity.titleSlug,
            titleCover = meta?.coverImage ?: entity.titleCover,
            chapterId = chapterId,
        )
    }

    suspend fun downloadedChapters(titleId: String): List<OfflineChapterEntity> =
        withContext(Dispatchers.IO) {
            dao.chaptersForTitle(titleId).filter { getLocalPages(it.chapterId)?.isNotEmpty() == true }
        }

    suspend fun verifyAndRepair(): OfflineIntegrityReport = withContext(Dispatchers.IO) {
        val entities = dao.allChapters()
        var valid = 0
        var removed = 0
        var orphans = 0
        var freed = 0L

        val knownDirs = entities.map { File(it.localDir).canonicalPath }.toSet()
        entities.forEach { entity ->
            val dir = File(entity.localDir)
            if (getLocalPages(entity.chapterId) == null) {
                freed += dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                dir.deleteRecursively()
                dao.delete(entity.chapterId)
                removed++
            } else {
                valid++
            }
        }

        File(context.filesDir, "offline").walkTopDown()
            .filter { it.isDirectory && it.parentFile?.parentFile?.name == "offline" }
            .toList()
            .forEach { dir ->
                if (dir.canonicalPath !in knownDirs) {
                    freed += dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                    if (dir.deleteRecursively()) orphans++
                }
            }

        OfflineIntegrityReport(valid, removed, orphans, freed)
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
        runCatchingCancellable {
            onStage(DownloadStage.CheckingAccess, 0, 0, null)
            if (!authRepository.isLoggedIn()) error("Войдите в аккаунт")
            val premium = authRepository.isPremium()
            var usedAdCredit = false
            if (!premium) {
                val store = adRewardStore
                    ?: error("Офлайн доступен с Premium или после просмотра рекламы")
                usedAdCredit = store.tryConsumeOfflineCredit()
                if (!usedAdCredit) {
                    error("Офлайн: Premium или «Смотреть рекламу» (+1 глава)")
                }
            }
            try {
                // Один снимок каталога на тайтл вместо двух запросов на каждую главу.
                val cachedTitle = dao.getTitle(titleId)
                if (cachedTitle == null || System.currentTimeMillis() - cachedTitle.lastSyncedAt > 6 * 60 * 60 * 1000L) {
                    syncTitleCatalog(titleId, titleName, titleSlug, titleCover)
                }
                currentCoroutineContext().ensureActive()

                val existingChapter = dao.get(chapterId)
                val localChapter = existingChapter?.let { readLocalChapter(chapterId) }
                if (existingChapter != null && localChapter?.complete == true) {
                    if (usedAdCredit) adRewardStore?.refundOfflineCredit()
                    onStage(DownloadStage.Completed, 0, 0, "Уже скачано")
                    return@runCatchingCancellable existingChapter
                }
                if (existingChapter != null && usedAdCredit) {
                    // Докачка дыр не списывает новый кредит.
                    adRewardStore?.refundOfflineCredit()
                    usedAdCredit = false
                }

                onStage(DownloadStage.FetchingChapter, 0, 0, null)
                val res = api.chapterById(chapterId)
                if (!res.success) error(res.message ?: "Не удалось получить главу")
                val chapter = res.data ?: error("Глава пуста")
                val pages = chapter.pagePaths()
                if (pages.isEmpty()) error("У главы нет страниц")

                val root = File(context.filesDir, "offline/$titleId/$chapterId")
                root.mkdirs()

                var bytes = root.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
                pages.forEachIndexed { index, path ->
                    currentCoroutineContext().ensureActive()
                    val url = MediaUrl.resolve(path)
                    val cleanPath = runCatching { java.net.URI(url).path }.getOrDefault(path)
                    val ext = cleanPath.substringAfterLast('.', "jpg").filter { it.isLetterOrDigit() }.take(5)
                        .ifBlank { "jpg" }
                    val out = File(root, String.format(Locale.ROOT, "%04d.%s", index + 1, ext))
                    val keep = findValidPage(root, index)
                    if (keep != null) {
                        deletePageSiblings(root, index, keep)
                        onProgress(index + 1, pages.size)
                        onStage(
                            DownloadStage.DownloadingPages,
                            index + 1,
                            pages.size,
                            "Продолжаем загрузку",
                        )
                        return@forEachIndexed
                    }
                    deletePageSiblings(root, index, null)
                    onStage(DownloadStage.DownloadingPages, index, pages.size, null)
                    downloadPageWithRetry(url = url, dest = out, pageIndex = index)
                    bytes += out.length()
                    onProgress(index + 1, pages.size)
                    onStage(DownloadStage.DownloadingPages, index + 1, pages.size, null)
                }

                onStage(DownloadStage.Saving, pages.size, pages.size, null)
                bytes = root.listFiles()
                    ?.filter { it.isFile && !it.name.endsWith(".part") }
                    ?.sumOf { it.length() }
                    ?: bytes
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
                File(root, ".complete").writeText(pages.size.toString())
                onStage(DownloadStage.Completed, pages.size, pages.size, null)
                entity
            } catch (e: Throwable) {
                if (usedAdCredit) withContext(NonCancellable) { adRewardStore?.refundOfflineCredit() }
                throw e
            }
        }.onFailure { if (it is CancellationException) throw it }
    }

    private fun pageIndexFromName(name: String): Int? {
        if (name.endsWith(".part")) return null
        val number = name.substringBefore('.').toIntOrNull() ?: return null
        if (number <= 0) return null
        return number - 1
    }

    private fun findValidPage(root: File, index: Int): File? {
        val prefix = String.format(Locale.ROOT, "%04d.", index + 1)
        return root.listFiles()
            ?.filter { file ->
                file.isFile &&
                    file.name.startsWith(prefix) &&
                    !file.name.endsWith(".part") &&
                    ImageIntegrity.isValidFile(file)
            }
            ?.maxByOrNull { it.lastModified() }
    }

    private fun deletePageSiblings(root: File, index: Int, keep: File?) {
        val prefix = String.format(Locale.ROOT, "%04d.", index + 1)
        root.listFiles()?.forEach { file ->
            if (!file.isFile || !file.name.startsWith(prefix)) return@forEach
            if (keep != null && file.absolutePath == keep.absolutePath) return@forEach
            file.delete()
        }
    }

    private suspend fun downloadPageWithRetry(url: String, dest: File, pageIndex: Int) {
        val part = File(dest.parentFile, dest.name + ".part")
        var lastError: Throwable? = null
        val candidates = MediaUrl.candidates(url).ifEmpty { listOf(url) }
        repeat(4) { attempt ->
            currentCoroutineContext().ensureActive()
            part.delete()
            dest.delete()
            try {
                val source = candidates[attempt % candidates.size]
                val separator = if ('?' in source) '&' else '?'
                val retryUrl = if (attempt == 0) source else "$source${separator}tomilo_retry=$attempt"
                val req = Request.Builder()
                    .url(retryUrl)
                    .header("Cache-Control", if (attempt == 0) "max-age=3600" else "no-cache")
                    .header("Accept", "image/avif,image/webp,image/*,*/*;q=0.8")
                    .get()
                    .build()
                http.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("Не удалось скачать страницу ${pageIndex + 1} (${response.code})")
                    }
                    val body = response.body ?: error("Пустой ответ страницы ${pageIndex + 1}")
                    body.byteStream().use { input ->
                        part.outputStream().use { output ->
                            val buffer = ByteArray(32 * 1024)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                            }
                        }
                    }
                }
                if (!ImageIntegrity.isValidFile(part) || !part.renameTo(dest) || !ImageIntegrity.isValidFile(dest)) {
                    part.delete()
                    dest.delete()
                    error("Страница ${pageIndex + 1} повреждена, повторяем")
                }
                return
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                lastError = e
                part.delete()
                dest.delete()
                if (attempt < 3) delay(500L * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("Не удалось скачать страницу ${pageIndex + 1}")
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
