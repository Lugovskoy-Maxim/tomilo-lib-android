package ru.tomilo.lib.mobile.data.download

enum class DownloadStage {
    Queued,
    CheckingAccess,
    FetchingChapter,
    DownloadingPages,
    Saving,
    Completed,
    Failed,
    Cancelled,
}

data class ChapterDownloadProgress(
    val chapterId: String,
    val chapterLabel: String,
    val stage: DownloadStage = DownloadStage.Queued,
    val pagesDone: Int = 0,
    val pagesTotal: Int = 0,
    val message: String? = null,
) {
    val fraction: Float
        get() = when (stage) {
            DownloadStage.Queued -> 0f
            DownloadStage.CheckingAccess -> 0.05f
            DownloadStage.FetchingChapter -> 0.12f
            DownloadStage.DownloadingPages -> {
                if (pagesTotal <= 0) 0.15f
                else 0.15f + 0.8f * (pagesDone.toFloat() / pagesTotal)
            }
            DownloadStage.Saving -> 0.96f
            DownloadStage.Completed -> 1f
            DownloadStage.Failed, DownloadStage.Cancelled -> pagesDone.toFloat()
                .div(pagesTotal.coerceAtLeast(1))
                .coerceIn(0f, 1f)
        }

    val stageLabel: String
        get() = when (stage) {
            DownloadStage.Queued -> "В очереди"
            DownloadStage.CheckingAccess -> "Проверка доступа…"
            DownloadStage.FetchingChapter -> "Получение главы…"
            DownloadStage.DownloadingPages ->
                if (pagesTotal > 0) "Скачивание $pagesDone из $pagesTotal стр."
                else "Скачивание страниц…"
            DownloadStage.Saving -> "Сохранение на устройство…"
            DownloadStage.Completed -> "✓ Готово"
            DownloadStage.Failed -> message ?: "Ошибка"
            DownloadStage.Cancelled -> "Отменено"
        }
}

data class BatchDownloadState(
    val titleName: String = "",
    val items: List<ChapterDownloadProgress> = emptyList(),
    val activeIndex: Int = -1,
    val finished: Boolean = false,
    val runningInBackground: Boolean = false,
) {
    val overallFraction: Float
        get() {
            if (items.isEmpty()) return 0f
            return items.map { it.fraction }.average().toFloat()
        }

    val completedCount: Int get() = items.count { it.stage == DownloadStage.Completed }
    val failedCount: Int get() = items.count { it.stage == DownloadStage.Failed }
    val activeItem: ChapterDownloadProgress?
        get() = items.getOrNull(activeIndex)

    val statusSummary: String
        get() {
            if (items.isEmpty()) return ""
            if (finished) {
                return buildString {
                    append("Готово: $completedCount из ${items.size}")
                    if (failedCount > 0) append(" · ошибок $failedCount")
                }
            }
            val active = activeItem
            return buildString {
                append("${completedCount + if (active?.stage == DownloadStage.Completed) 0 else 0}")
                // current index is 1-based for user
                val n = (activeIndex + 1).coerceAtLeast(1)
                append("Глава $n из ${items.size}")
                active?.let {
                    append(" · ")
                    append(it.stageLabel)
                }
            }
        }
}

/** Пакет для фонового сервиса */
data class DownloadBatchRequest(
    val titleId: String,
    val titleName: String,
    val titleSlug: String,
    val titleCover: String?,
    val chapters: List<DownloadChapterRef>,
)

data class DownloadChapterRef(
    val chapterId: String,
    val chapterLabel: String,
)
