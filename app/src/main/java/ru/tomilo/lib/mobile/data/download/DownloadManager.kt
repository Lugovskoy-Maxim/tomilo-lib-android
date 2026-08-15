package ru.tomilo.lib.mobile.data.download

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.data.api.ChapterDto
import ru.tomilo.lib.mobile.data.repo.OfflineRepository

/**
 * Очередь скачивания глав.
 * Работает через [DownloadForegroundService], чтобы продолжать в фоне.
 */
class DownloadManager(
    private val context: Context,
    private val offlineRepository: OfflineRepository,
) {
    private val appContext = context.applicationContext
    /** Собственный scope — не привязан к Activity. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(BatchDownloadState())
    val state: StateFlow<BatchDownloadState> = _state.asStateFlow()

    private var job: Job? = null

    @Volatile
    private var pendingRequest: DownloadBatchRequest? = null
    private var lastRequest: DownloadBatchRequest? = null

    fun isBusy(): Boolean {
        if (job?.isActive == true || pendingRequest != null) return true
        val s = _state.value
        return s.items.isNotEmpty() && !s.finished
    }

    fun enqueue(
        titleId: String,
        titleName: String,
        titleSlug: String,
        titleCover: String?,
        chapters: List<ChapterDto>,
    ) {
        if (chapters.isEmpty()) return
        job?.cancel()

        val refs = chapters.map {
            DownloadChapterRef(
                chapterId = it.stableId(),
                chapterLabel = "Глава ${it.numberLabel()}",
            )
        }
        val request = DownloadBatchRequest(
            titleId = titleId,
            titleName = titleName,
            titleSlug = titleSlug,
            titleCover = titleCover,
            chapters = refs,
        )
        pendingRequest = request
        lastRequest = request

        val initial = refs.map {
            ChapterDownloadProgress(
                chapterId = it.chapterId,
                chapterLabel = it.chapterLabel,
                stage = DownloadStage.Queued,
            )
        }
        _state.value = BatchDownloadState(
            titleName = titleName,
            items = initial,
            activeIndex = 0,
            finished = false,
            runningInBackground = true,
        )

        DownloadForegroundService.start(appContext)
    }

    /** Вызывается сервисом после startForeground. */
    fun runPendingFromService(onProgressNotify: (BatchDownloadState) -> Unit) {
        val request = pendingRequest ?: run {
            onProgressNotify(_state.value.copy(finished = true, runningInBackground = false))
            return
        }
        pendingRequest = null
        job?.cancel()
        job = scope.launch {
            try {
                executeBatch(request) { state ->
                    onProgressNotify(state)
                }
            } finally {
                _state.update { it.copy(finished = true, activeIndex = -1, runningInBackground = false) }
                onProgressNotify(_state.value)
                DownloadForegroundService.stop(appContext)
            }
        }
    }

    private suspend fun executeBatch(
        request: DownloadBatchRequest,
        onNotify: (BatchDownloadState) -> Unit,
    ) {
        request.chapters.forEachIndexed { index, ref ->
            if (!currentCoroutineContext().isActive) return

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
            onNotify(_state.value)

            updateItem(ref.chapterId) { it.copy(stage = DownloadStage.FetchingChapter) }
            onNotify(_state.value)

            val result = offlineRepository.downloadChapter(
                titleId = request.titleId,
                titleName = request.titleName,
                titleSlug = request.titleSlug,
                titleCover = request.titleCover,
                chapterId = ref.chapterId,
                onStage = { stage, done, total, msg ->
                    updateItem(ref.chapterId) {
                        it.copy(
                            stage = stage,
                            pagesDone = done,
                            pagesTotal = total,
                            message = msg,
                        )
                    }
                    onNotify(_state.value)
                },
            )

            result
                .onSuccess {
                    updateItem(ref.chapterId) {
                        it.copy(stage = DownloadStage.Completed, pagesDone = it.pagesTotal)
                    }
                }
                .onFailure { e ->
                    updateItem(ref.chapterId) {
                        it.copy(stage = DownloadStage.Failed, message = e.message)
                    }
                }
            onNotify(_state.value)
        }
        _state.update { it.copy(finished = true, activeIndex = -1) }
        onNotify(_state.value)
    }

    fun cancel() {
        job?.cancel()
        pendingRequest = null
        _state.update { s ->
            s.copy(
                finished = true,
                activeIndex = -1,
                runningInBackground = false,
                items = s.items.map {
                    if (it.stage == DownloadStage.Completed || it.stage == DownloadStage.Failed) it
                    else it.copy(stage = DownloadStage.Cancelled)
                },
            )
        }
        DownloadForegroundService.stop(appContext)
    }

    fun clear() {
        if (isBusy()) return
        _state.value = BatchDownloadState()
    }

    fun retryFailed() {
        if (isBusy()) return
        val previous = lastRequest ?: return
        val retryIds = _state.value.items
            .filter { it.stage == DownloadStage.Failed || it.stage == DownloadStage.Cancelled }
            .map { it.chapterId }
            .toSet()
        val retryRefs = previous.chapters.filter { it.chapterId in retryIds }
        if (retryRefs.isEmpty()) return

        pendingRequest = previous.copy(chapters = retryRefs)
        val initial = retryRefs.map {
            ChapterDownloadProgress(
                chapterId = it.chapterId,
                chapterLabel = it.chapterLabel,
                stage = DownloadStage.Queued,
            )
        }
        _state.value = BatchDownloadState(
            titleName = previous.titleName,
            items = initial,
            activeIndex = 0,
            finished = false,
            runningInBackground = true,
        )
        DownloadForegroundService.start(appContext)
    }

    private fun updateItem(
        chapterId: String,
        transform: (ChapterDownloadProgress) -> ChapterDownloadProgress,
    ) {
        _state.update { s ->
            s.copy(
                items = s.items.map { if (it.chapterId == chapterId) transform(it) else it },
            )
        }
    }
}
