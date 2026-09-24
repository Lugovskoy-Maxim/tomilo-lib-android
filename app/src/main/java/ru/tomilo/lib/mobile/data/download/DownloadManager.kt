package ru.tomilo.lib.mobile.data.download

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val queueStore = DownloadQueueStore(appContext)
    private val executionMutex = Mutex()

    private val _state = MutableStateFlow(BatchDownloadState())
    val state: StateFlow<BatchDownloadState> = _state.asStateFlow()

    private var job: Job? = null
    @Volatile
    private var generation = 0L

    @Volatile
    private var pendingRequest: DownloadBatchRequest? = null
    private var lastRequest: DownloadBatchRequest? = null

    fun isBusy(): Boolean {
        if (job?.isActive == true || pendingRequest != null) return true
        val s = _state.value
        return s.items.isNotEmpty() && !s.finished
    }

    /** Resume a saved batch from the foreground service when the user opens the app. */
    fun resumePersisted() {
        if (isBusy()) return
        val request = queueStore.load() ?: return
        DownloadResumeWorker.cancel(appContext)
        pendingRequest = request
        lastRequest = request
        _state.value = BatchDownloadState(
            titleName = request.titleName,
            items = request.chapters.map { ref ->
                ChapterDownloadProgress(ref.chapterId, ref.chapterLabel, DownloadStage.Queued)
            },
            activeIndex = 0,
            finished = false,
            runningInBackground = true,
        )
        DownloadForegroundService.start(appContext)
    }

    /** Keep the durable checkpoint; Android may allow resuming after its data-sync budget resets. */
    fun pauseForSystemTimeout() {
        generation++
        job?.cancel()
        pendingRequest = queueStore.load()
        _state.update { state ->
            state.copy(
                finished = true,
                activeIndex = -1,
                runningInBackground = false,
                items = state.items.map {
                    if (it.stage == DownloadStage.Completed || it.stage == DownloadStage.Failed) it
                    else it.copy(stage = DownloadStage.Cancelled, message = "Приостановлено системой. Откройте приложение, чтобы продолжить.")
                },
            )
        }
        if (pendingRequest != null) DownloadResumeWorker.enqueue(appContext)
    }

    /** Continue a checkpoint in ordinary deferrable work after Android's FGS data-sync budget expires. */
    suspend fun runPersistedFromWorker(onProgress: (BatchDownloadState) -> Unit): Boolean {
        if (job?.isActive == true) return false
        return executionMutex.withLock {
            if (job?.isActive == true) return@withLock false
            val request = queueStore.load() ?: return@withLock true
            pendingRequest = null
            lastRequest = request
            val batchGeneration = ++generation
            _state.value = queuedState(request)
            try {
                executeBatch(request, onProgress)
                if (batchGeneration != generation) return@withLock false
                persistIncompleteChapters(request, _state.value)
                pendingRequest = null
                onProgress(_state.value)
                true
            } catch (cancelled: CancellationException) {
                if (batchGeneration == generation) {
                    pendingRequest = queueStore.load()
                    _state.update { state ->
                        state.copy(
                            finished = true,
                            activeIndex = -1,
                            runningInBackground = false,
                            items = state.items.map {
                                if (it.stage == DownloadStage.Completed || it.stage == DownloadStage.Failed) it
                                else it.copy(stage = DownloadStage.Cancelled, message = "Приостановлено системой. Загрузка продолжится автоматически.")
                            },
                        )
                    }
                }
                throw cancelled
            }
        }
    }

    fun enqueue(
        titleId: String,
        titleName: String,
        titleSlug: String,
        titleCover: String?,
        chapters: List<ChapterDto>,
    ) {
        if (chapters.isEmpty() || isBusy()) return
        job?.cancel()

        val refs = chapters.filter { it.stableId().isNotBlank() }.distinctBy { it.stableId() }.map {
            DownloadChapterRef(
                chapterId = it.stableId(),
                chapterLabel = "Глава ${it.numberLabel()}",
            )
        }
        if (refs.isEmpty()) return
        val request = DownloadBatchRequest(
            titleId = titleId,
            titleName = titleName,
            titleSlug = titleSlug,
            titleCover = titleCover,
            chapters = refs,
        )
        pendingRequest = request
        lastRequest = request
        queueStore.save(request)

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
        if (pendingRequest == null && job?.isActive == true) return
        val request = pendingRequest ?: queueStore.load() ?: run {
            onProgressNotify(_state.value.copy(finished = true, runningInBackground = false))
            return
        }
        pendingRequest = null
        lastRequest = request
        if (_state.value.items.isEmpty() || _state.value.finished) {
            _state.value = BatchDownloadState(
                titleName = request.titleName,
                items = request.chapters.map { ref ->
                    ChapterDownloadProgress(
                        chapterId = ref.chapterId,
                        chapterLabel = ref.chapterLabel,
                        stage = DownloadStage.Queued,
                    )
                },
                activeIndex = 0,
                finished = false,
                runningInBackground = true,
            )
        }
        val previousJob = job
        previousJob?.cancel()
        val batchGeneration = ++generation
        job = scope.launch {
            previousJob?.join()
            try {
                executionMutex.withLock {
                    if (generation != batchGeneration || queueStore.load() == null) return@withLock
                    executeBatch(request) { state ->
                        if (generation == batchGeneration) onProgressNotify(state)
                    }
                }
            } finally {
                if (generation == batchGeneration) {
                    _state.update { it.copy(finished = true, activeIndex = -1, runningInBackground = false) }
                    onProgressNotify(_state.value)
                    persistIncompleteChapters(request, _state.value)
                    DownloadForegroundService.stop(appContext)
                }
            }
        }
    }

    private suspend fun executeBatch(
        request: DownloadBatchRequest,
        onNotify: (BatchDownloadState) -> Unit,
    ) {
        request.chapters.forEachIndexed { index, ref ->
            currentCoroutineContext().ensureActive()

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
        generation++
        job?.cancel()
        DownloadResumeWorker.cancel(appContext)
        pendingRequest = null
        queueStore.clear()
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
        pendingRequest = null
        queueStore.clear()
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

        val retryRequest = previous.copy(chapters = retryRefs)
        pendingRequest = retryRequest
        queueStore.save(retryRequest)
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

    /** Persist only unfinished chapters so completed work is never repeated on recovery. */
    private fun persistIncompleteChapters(
        request: DownloadBatchRequest,
        state: BatchDownloadState,
    ) {
        val incompleteIds = state.items
            .filter { it.stage != DownloadStage.Completed }
            .mapTo(hashSetOf()) { it.chapterId }
        val incomplete = request.chapters.filter { it.chapterId in incompleteIds }
        if (incomplete.isEmpty()) queueStore.clear()
        else queueStore.save(request.copy(chapters = incomplete))
    }

    private fun queuedState(request: DownloadBatchRequest) = BatchDownloadState(
        titleName = request.titleName,
        items = request.chapters.map { ref ->
            ChapterDownloadProgress(ref.chapterId, ref.chapterLabel, DownloadStage.Queued)
        },
        activeIndex = 0,
        finished = false,
        runningInBackground = true,
    )
}
