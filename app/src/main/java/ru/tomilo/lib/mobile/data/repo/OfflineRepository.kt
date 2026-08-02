package ru.tomilo.lib.mobile.data.repo

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.api.TomiloApi
import ru.tomilo.lib.mobile.data.local.OfflineChapterEntity
import ru.tomilo.lib.mobile.data.local.OfflineDao
import java.io.File
import java.util.concurrent.TimeUnit

class OfflineRepository(
    private val context: Context,
    private val api: TomiloApi,
    private val dao: OfflineDao,
    private val authRepository: AuthRepository,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun observeAll(): Flow<List<OfflineChapterEntity>> = dao.observeAll()
    fun observeByTitle(titleId: String): Flow<List<OfflineChapterEntity>> = dao.observeByTitle(titleId)

    suspend fun isDownloaded(chapterId: String): Boolean = dao.isDownloaded(chapterId)

    suspend fun getLocalPages(chapterId: String): List<String>? {
        val entity = dao.get(chapterId) ?: return null
        val dir = File(entity.localDir)
        if (!dir.isDirectory) return null
        return dir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp", "avif") }
            ?.sortedBy { it.name }
            ?.map { it.absolutePath }
    }

    /**
     * Скачивание главы только для активных premium-пользователей.
     * Страницы сохраняются в app-private storage.
     */
    suspend fun downloadChapter(
        titleId: String,
        titleName: String,
        titleSlug: String,
        titleCover: String?,
        chapterId: String,
        onProgress: (downloaded: Int, total: Int) -> Unit = { _, _ -> },
    ): Result<OfflineChapterEntity> = withContext(Dispatchers.IO) {
        runCatching {
            if (!authRepository.isLoggedIn()) error("Войдите в аккаунт")
            if (!authRepository.isPremium()) {
                error("Офлайн-чтение доступно только Premium")
            }
            if (dao.isDownloaded(chapterId)) {
                return@runCatching dao.get(chapterId)!!
            }

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
                val url = MediaUrl.resolve(path)
                val ext = path.substringAfterLast('.', "jpg").take(5)
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
            }

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
    }
}
