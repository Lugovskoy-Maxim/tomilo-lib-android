package ru.tomilo.lib.mobile.data.download

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.data.api.ChapterDto
import ru.tomilo.lib.mobile.data.repo.OfflineRepository

/**
 * Очередь скачивания глав с поэтапным прогрессом для UI.
 */
class DownloadManager(
    private val offlineRepository: OfflineRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(BatchDownloadState())
    val state: StateFlow<BatchDownloadState> = _state.asStateFlow()

    private var job: Job? = null

    fun isBusy(): Boolean = job?.isActive == true

    fun enqueue(
        titleId: String,
        titleName: String,
        titleSlug: String,
        titleCover: String?,
        chapters: List<ChapterDto>,
    ) {
        if (chapters.isEmpty()) return
        job?.cancel()
        val initial = chapters.map {
            ChapterDownloadProgress(
                chapterId = it.stableId(),
                chapterLabel = "Глава ${it.numberLabel()}",
                stage = DownloadStage.Queued,
            )
        }
        _state.value = BatchDownloadState(items = initial, activeIndex = 0, finished = false)

        job = scope.launch(Dispatchers.IO) {
            chapters.forEachIndexed { index, chapter ->
                if (!isActive) return@launch
                _state.update { s ->
                    s.copy(
                        activeIndex = index,
                        items = s.items.mapIndexed { i, item ->
                            when {
                                i == index -> item.copy(stage = DownloadStage.CheckingAccess)
                                i < index && item.stage != DownloadStage.Failed -> item
                                else -> item
                            }
                        },
                    )
                }

                updateItem(chapter.stableId()) {
                    it.copy(stage = DownloadStage.FetchingChapter)
                }

                val result = offlineRepository.downloadChapter(
                    titleId = titleId,
                    titleName = titleName,
                    titleSlug = titleSlug,
                    titleCover = titleCover,
                    chapterId = chapter.stableId(),
                    onStage = { stage, done, total, msg ->
                        updateItem(chapter.stableId()) {
                            it.copy(
                                stage = stage,
                                pagesDone = done,
                                pagesTotal = total,
                                message = msg,
                            )
                        }
                    },
                )

                result
                    .onSuccess {
                        updateItem(chapter.stableId()) {
                            it.copy(stage = DownloadStage.Completed, pagesDone = it.pagesTotal)
                        }
                    }
                    .onFailure { e ->
                        updateItem(chapter.stableId()) {
                            it.copy(stage = DownloadStage.Failed, message = e.message)
                        }
                    }
            }
            _state.update { it.copy(finished = true, activeIndex = -1) }
        }
    }

    fun cancel() {
        job?.cancel()
        _state.update { s ->
            s.copy(
                finished = true,
                activeIndex = -1,
                items = s.items.map {
                    if (it.stage == DownloadStage.Completed || it.stage == DownloadStage.Failed) it
                    else it.copy(stage = DownloadStage.Cancelled)
                },
            )
        }
    }

    fun clear() {
        if (isBusy()) return
        _state.value = BatchDownloadState()
    }

    private fun updateItem(chapterId: String, transform: (ChapterDownloadProgress) -> ChapterDownloadProgress) {
        _state.update { s ->
            s.copy(
                items = s.items.map { if (it.chapterId == chapterId) transform(it) else it },
            )
        }
    }
}
