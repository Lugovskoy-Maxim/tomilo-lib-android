package ru.tomilo.lib.mobile.ui.screens.reader

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.debounce
import ru.tomilo.lib.mobile.ads.ChapterTransitionAds
import ru.tomilo.lib.mobile.core.ChapterAccess
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.core.PageImages
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.data.api.ChapterDto
import ru.tomilo.lib.mobile.data.local.ReadingPosition
import ru.tomilo.lib.mobile.data.local.ReadingPrefs
import ru.tomilo.lib.mobile.data.local.ReadingSettings
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.data.repo.HistoryRepository
import ru.tomilo.lib.mobile.data.repo.OfflineRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.CommentsSection
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, FlowPreview::class)
@Composable
fun ReaderScreen(
    chapterId: String,
    titleId: String?,
    preferOffline: Boolean,
    catalogRepository: CatalogRepository,
    offlineRepository: OfflineRepository,
    historyRepository: HistoryRepository,
    socialRepository: SocialRepository,
    readingPrefs: ReadingPrefs,
    authRepository: AuthRepository,
    chapterTransitionAds: ChapterTransitionAds,
    onBack: () -> Unit,
    onOpenChapter: (chapterId: String) -> Unit,
    onOpenTitle: (titleId: String) -> Unit = {},
    onOpenUser: (userId: String) -> Unit = {},
    onOpenPremium: () -> Unit = {},
    onLogin: () -> Unit = {},
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? android.app.Activity
    val user by authRepository.userFlow.collectAsState(initial = null)
    val isPremium = Premium.isActive(user?.subscriptionExpiresAt)
    val storedSettings: ReadingSettings? by readingPrefs.settingsFlow.collectAsState(initial = null)
    val settings = storedSettings ?: ReadingSettings()
    val scope = rememberCoroutineScope()
    // Не привязываем remember к nav chapterId — иначе при in-place смене главы state сбрасывается
    var currentChapterId by rememberSaveable { mutableStateOf(chapterId) }
    // Новый LazyListState на каждую главу — иначе индекс последней страницы
    // предыдущей главы залипает (scrollToItem до attach списка не срабатывает).
    val listState = rememberSaveable(currentChapterId, saver = LazyListState.Saver) {
        LazyListState(0, 0)
    }
    val chaptersListState = rememberLazyListState()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var needsPremium by remember { mutableStateOf(false) }
    var needsLogin by remember { mutableStateOf(false) }
    var pages by remember { mutableStateOf<List<String>>(emptyList()) }
    var title by remember { mutableStateOf("Глава") }
    var offline by remember { mutableStateOf(false) }

    var chromeVisible by remember { mutableStateOf(!settings.startFullscreen) }
    var fullscreen by remember { mutableStateOf(settings.startFullscreen) }
    var autoScroll by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(settings.autoScrollSpeed) }
    var showChapters by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }
    var chapters by remember { mutableStateOf<List<ChapterDto>>(emptyList()) }
    var failedPages by remember { mutableStateOf(setOf<Int>()) }
    var loadedPages by remember { mutableStateOf(setOf<Int>()) }
    var restoredChapterId by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<ReadingPosition?>(null) }
    var pageRetryNonce by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var chapterNavMessage by remember { mutableStateOf<String?>(null) }
    var settingsApplied by remember { mutableStateOf(false) }
    var pageSliderValue by remember { mutableFloatStateOf(0f) }
    var pageSliderActive by remember { mutableStateOf(false) }
    var effectiveTitleId by remember { mutableStateOf(titleId) }
    var hasScrolledThisChapter by remember { mutableStateOf(false) }
    var autoAdvanceFromChapter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(storedSettings) {
        val saved = storedSettings ?: return@LaunchedEffect
        if (!settingsApplied) {
            speed = saved.autoScrollSpeed
            fullscreen = saved.startFullscreen
            chromeVisible = !saved.startFullscreen
            settingsApplied = true
        } else if (!autoScroll) {
            speed = saved.autoScrollSpeed
        }
    }

    // Соседние главы из локального списка (API next/prev требует номер главы и часто падает)
    val currentIndex = remember(chapters, currentChapterId) {
        chapters.indexOfFirst { it.stableId() == currentChapterId }
    }
    val prevChapterId = remember(currentIndex, chapters) {
        if (currentIndex > 0) chapters[currentIndex - 1].stableId() else null
    }
    val nextChapterId = remember(currentIndex, chapters) {
        if (currentIndex >= 0 && currentIndex < chapters.lastIndex) {
            chapters[currentIndex + 1].stableId()
        } else null
    }
    val hasPrev = prevChapterId != null
    val hasNext = nextChapterId != null

    // Keep screen on
    DisposableEffect(settings.keepScreenOn) {
        val window = activity?.window
        if (settings.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // System bars for fullscreen
    DisposableEffect(fullscreen, chromeVisible) {
        val window = activity?.window
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, !fullscreen)
            val controller = WindowInsetsControllerCompat(window, view)
            if (fullscreen && !chromeVisible) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    fun loadChapter(id: String, restorePosition: Boolean = true) {
        if (id.isBlank()) return
        scope.launch {
            loading = true
            error = null
            needsPremium = false
            needsLogin = false
            currentChapterId = id
            autoScroll = false
            pages = emptyList()
            failedPages = emptySet()
            loadedPages = emptySet()
            pageRetryNonce = emptyMap()
            restoredChapterId = null
            pendingRestore = if (restorePosition) {
                readingPrefs.readingPosition(id)
            } else {
                ReadingPosition(0, 0)
            }
            chapterNavMessage = null
            hasScrolledThisChapter = false
            autoAdvanceFromChapter = null

            // Офлайн-копия
            if (preferOffline || offlineRepository.isDownloaded(id)) {
                val local = offlineRepository.getLocalPages(id)
                if (!local.isNullOrEmpty()) {
                    val entity = offlineRepository.getEntity(id)
                    effectiveTitleId = titleId ?: entity?.titleId
                    title = entity?.let { "Глава ${it.chapterNumber}" } ?: "Глава (офлайн)"
                    pages = local.map { File(it).toURI().toString() }
                    offline = true
                    loading = false
                    val tid = effectiveTitleId
                    if (!tid.isNullOrBlank()) {
                        val loggedIn = authRepository.isLoggedIn()
                        readingPrefs.markLocalRead(tid, id, queueSync = loggedIn)
                        if (loggedIn) {
                            historyRepository.markRead(tid, id)
                                .onSuccess { readingPrefs.markHistorySynced(tid, id) }
                        }
                    }
                    return@launch
                }
            }

            // JWT + свежий Premium (сервер отдаёт pages по subscriptionExpiresAt)
            var subExpires = user?.subscriptionExpiresAt
            if (authRepository.isLoggedIn()) {
                authRepository.refreshProfile().onSuccess { subExpires = it.subscriptionExpiresAt }
            }

            suspend fun applyChapter(chapter: ChapterDto, allowRetry: Boolean) {
                title = chapter.name?.ifBlank { "Глава ${chapter.numberLabel()}" }
                    ?: "Глава ${chapter.numberLabel()}"
                offline = false

                if (chapter.isWithdrawn()) {
                    error = "Глава скрыта или удалена"
                    return
                }

                val canRead = ChapterAccess.userCanRead(
                    isPaid = chapter.isPaid,
                    freeAt = chapter.freeAt,
                    unlockedByActivityCoins = chapter.isUnlockedByActivityCoins,
                    subscriptionExpiresAt = subExpires,
                )
                val resolved = chapter.pages.orEmpty().map { MediaUrl.resolve(it) }

                when {
                    resolved.isNotEmpty() -> {
                        pages = resolved
                        if (!titleId.isNullOrBlank()) {
                            val loggedIn = authRepository.isLoggedIn()
                            readingPrefs.markLocalRead(titleId, id, queueSync = loggedIn)
                            if (loggedIn) {
                                historyRepository.markRead(titleId, id)
                                    .onSuccess { readingPrefs.markHistorySynced(titleId, id) }
                            }
                        }
                    }
                    chapter.isPaid == true && !canRead -> {
                        needsPremium = true
                        needsLogin = !authRepository.isLoggedIn()
                        error = ChapterAccess.lockHint(
                            isPaid = true,
                            freeAt = chapter.freeAt,
                            unlockPrice = chapter.unlockPrice,
                            isPremiumUser = false,
                        )
                    }
                    // Premium (или freeAt), но pages пустые — повтор запроса
                    canRead && allowRetry && authRepository.isLoggedIn() -> {
                        authRepository.refreshProfile().onSuccess {
                            subExpires = it.subscriptionExpiresAt
                        }
                        catalogRepository.chapter(id)
                            .onSuccess { applyChapter(it, allowRetry = false) }
                            .onFailure { error = it.message ?: "Не удалось открыть главу" }
                    }
                    canRead -> {
                        error = "Страницы пока недоступны. Потяните назад и откройте главу снова."
                    }
                    else -> error = "Страницы недоступны"
                }
            }

            catalogRepository.chapter(id)
                .onSuccess { applyChapter(it, allowRetry = true) }
                .onFailure { error = it.message ?: "Не удалось открыть главу" }

            loading = false
        }
    }

    // После композиции LazyColumn: иначе scrollToItem бьёт в ещё не прикреплённый список
    // и остаётся индекс последней страницы предыдущей главы.
    LaunchedEffect(currentChapterId, loading, pages.size, pendingRestore) {
        if (loading || pages.isEmpty()) return@LaunchedEffect
        val pos = pendingRestore ?: ReadingPosition()
        val index = pos.pageIndex.coerceIn(0, pages.lastIndex)
        val offset = if (pos.pageIndex > pages.lastIndex) 0 else pos.scrollOffset.coerceAtLeast(0)
        runCatching { listState.scrollToItem(index, offset) }
        restoredChapterId = currentChapterId
    }

    /**
     * Переход на другую главу **внутри** экрана (без пересоздания через NavHost —
     * иначе next/prev ломались). Реклама ~1 раз / 10 мин для non-Premium.
     */
    fun goChapter(nextId: String, restorePosition: Boolean = false) {
        if (nextId.isBlank() || nextId == currentChapterId || loading) return
        chapterTransitionAds.maybeShowThen(
            activity = activity,
            user = user,
            proceed = { loadChapter(nextId, restorePosition = restorePosition) },
        )
    }

    fun goPrev() {
        val id = prevChapterId
        if (id != null) {
            goChapter(id)
            return
        }
        // fallback API (список ещё не подгрузился)
        scope.launch {
            catalogRepository.chapterPrev(currentChapterId)
                .onSuccess { ch -> goChapter(ch.stableId()) }
                .onFailure { chapterNavMessage = "Предыдущей главы нет" }
        }
    }

    fun goNext() {
        val id = nextChapterId
        if (id != null) {
            goChapter(id)
            return
        }
        scope.launch {
            catalogRepository.chapterNext(currentChapterId)
                .onSuccess { ch -> goChapter(ch.stableId()) }
                .onFailure { chapterNavMessage = "Следующей главы нет" }
        }
    }

    fun leaveReader() {
        val id = currentChapterId
        val index = listState.firstVisibleItemIndex
        val offset = listState.firstVisibleItemScrollOffset
        scope.launch { readingPrefs.saveReadingPosition(id, index, offset) }
        onBack()
    }

    // Старт / внешняя навигация (тайтл → глава)
    LaunchedEffect(chapterId) {
        if (chapterId.isNotBlank()) {
            loadChapter(currentChapterId.ifBlank { chapterId })
        }
    }

    // После входа / активации Premium — перезагрузить главу
    LaunchedEffect(user?.stableId(), user?.subscriptionExpiresAt) {
        if (pages.isEmpty() && !loading && (needsPremium || error != null)) {
            loadChapter(currentChapterId)
        }
    }

    // Список глав тайтла для prev/next и sheet
    LaunchedEffect(titleId) {
        val tid = titleId
        if (!tid.isNullOrBlank()) {
            catalogRepository.chaptersAll(tid)
                .onSuccess { list ->
                    chapters = list.sortedWith(
                        compareBy(
                            { it.chapterNumberAsDouble() ?: Double.MAX_VALUE },
                            { it.chapterNumber?.toString().orEmpty() },
                        ),
                    )
                }
        }
    }

    // Для офлайн-входа соседние главы берём из Room: переходы не зависят от API.
    LaunchedEffect(effectiveTitleId, offline) {
        val tid = effectiveTitleId
        if (offline && !tid.isNullOrBlank()) {
            val local = offlineRepository.downloadedChapters(tid)
            if (local.isNotEmpty()) {
                chapters = local.map { entity ->
                    val numberJson = entity.chapterNumber.toDoubleOrNull()?.let {
                        kotlinx.serialization.json.JsonPrimitive(it)
                    } ?: kotlinx.serialization.json.JsonPrimitive(entity.chapterNumber)
                    ChapterDto(
                        id = entity.chapterId,
                        name = entity.chapterName ?: "Глава ${entity.chapterNumber}",
                        chapterNumber = numberJson,
                        pagesCount = entity.pageCount,
                    )
                }.sortedBy { it.chapterNumberAsDouble() ?: Double.MAX_VALUE }
            }
        }
    }

    // Открыли список глав → прокрутить к текущей
    LaunchedEffect(showChapters, chapters, currentChapterId) {
        if (!showChapters || chapters.isEmpty()) return@LaunchedEffect
        val idx = chapters.indexOfFirst { it.stableId() == currentChapterId }
        if (idx >= 0) {
            // чуть выше центра, чтобы текущая была видна
            val target = (idx - 2).coerceAtLeast(0)
            chaptersListState.scrollToItem(target)
        }
    }

    // Auto-scroll loop
    LaunchedEffect(autoScroll, speed, pages) {
        if (!autoScroll || pages.isEmpty()) return@LaunchedEffect
        while (isActive && autoScroll) {
            val delta = (speed * 2.2f).toInt().coerceAtLeast(1)
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()
            if (!listState.canScrollForward) {
                autoScroll = false
                hasScrolledThisChapter = true
                if (autoAdvanceFromChapter != currentChapterId) {
                    autoAdvanceFromChapter = currentChapterId
                    goNext()
                }
                break
            }
            listState.dispatchRawDelta(delta.toFloat())
            delay(16L)
        }
    }

    // Сохраняем точную страницу и смещение, но не пишем DataStore на каждый кадр.
    LaunchedEffect(listState, currentChapterId, pages.size, restoredChapterId) {
        if (pages.isEmpty() || restoredChapterId != currentChapterId) return@LaunchedEffect
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
            .debounce(450)
            .collect { (index, offset) ->
                readingPrefs.saveReadingPosition(currentChapterId, index, offset)
            }
    }

    LaunchedEffect(chapterNavMessage) {
        if (chapterNavMessage != null) {
            delay(2_500)
            chapterNavMessage = null
        }
    }

    // Непрерывное чтение: после осознанного скролла до нижнего блока открываем
    // следующую главу. На восстановленной последней странице автопереход не сработает.
    LaunchedEffect(currentChapterId, pages.size, loading) {
        if (pages.isEmpty() || loading) return@LaunchedEffect
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            Triple(listState.isScrollInProgress, lastVisible, listState.canScrollForward)
        }.collect { (scrolling, lastVisible, canScrollForward) ->
            if (scrolling) hasScrolledThisChapter = true
            val reachedChapterFooter = lastVisible >= pages.size && !canScrollForward
            if (!scrolling && hasScrolledThisChapter && reachedChapterFooter &&
                autoAdvanceFromChapter != currentChapterId
            ) {
                autoAdvanceFromChapter = currentChapterId
                delay(350)
                goNext()
            }
        }
    }

    BackHandler {
        if (showChapters) showChapters = false
        else if (!chromeVisible) chromeVisible = true
        else leaveReader()
    }

    var readProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(listState, pages.size) {
        snapshotFlow {
            if (pages.isEmpty()) return@snapshotFlow 0f
            val info = listState.layoutInfo
            val visible = info.visibleItemsInfo.firstOrNull() ?: return@snapshotFlow 0f
            val pageIndex = visible.index.coerceIn(0, pages.lastIndex)
            val withinPage = if (visible.size > 0) {
                (-visible.offset).toFloat() / visible.size.toFloat()
            } else 0f
            ((pageIndex + withinPage.coerceIn(0f, 1f)) / pages.size.toFloat())
                .coerceIn(0f, 1f)
        }.collect { readProgress = it }
    }

    // Заранее прогреваем несколько следующих страниц; битый кеш выкидываем и качаем снова.
    LaunchedEffect(listState, pages, currentChapterId) {
        if (pages.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { first ->
                val start = (first + 1).coerceAtMost(pages.lastIndex)
                val end = (first + 3).coerceAtMost(pages.lastIndex)
                if (start <= end) {
                    for (index in start..end) {
                        if (index in failedPages) continue
                        PageImages.prefetch(context, pages[index])
                    }
                }
            }
    }

    LaunchedEffect(listState.firstVisibleItemIndex, pages.size, pageSliderActive) {
        if (!pageSliderActive) {
            pageSliderValue = listState.firstVisibleItemIndex
                .coerceIn(0, pages.lastIndex.coerceAtLeast(0))
                .toFloat()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            loading -> LoadingBox()
            error != null && needsPremium -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        error ?: "Платная глава",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(16.dp))
                    if (needsLogin) {
                        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
                            Text("Войти")
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(onClick = onOpenPremium, modifier = Modifier.fillMaxWidth()) {
                        Text("Оформить Premium")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { loadChapter(currentChapterId) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Повторить") }
                }
            }
            error != null -> ErrorBox(error ?: "Ошибка") { loadChapter(currentChapterId) }
            pages.isEmpty() -> ErrorBox("Нет страниц") { loadChapter(currentChapterId) }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(pages, key = { i, _ -> "$currentChapterId-$i" }) { index, page ->
                    var zoomScale by remember(page) { mutableFloatStateOf(1f) }
                    var zoomOffset by remember(page) { mutableStateOf(Offset.Zero) }
                    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                        val nextScale = (zoomScale * zoomChange).coerceIn(1f, 4f)
                        zoomScale = nextScale
                        zoomOffset = if (nextScale <= 1.01f) Offset.Zero else zoomOffset + panChange
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black)
                            .clipToBounds()
                            .transformable(
                                state = transformState,
                                canPan = { zoomScale > 1f },
                            )
                            .pointerInput(page) {
                                detectTapGestures(
                                    onTap = {
                                        chromeVisible = !chromeVisible
                                        if (chromeVisible) autoScroll = false
                                    },
                                    onDoubleTap = {
                                        zoomScale = if (zoomScale > 1f) 1f else 2f
                                        if (zoomScale == 1f) zoomOffset = Offset.Zero
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        key(pageRetryNonce[index] ?: 0) {
                            val attempt = pageRetryNonce[index] ?: 0
                            AsyncImage(
                                model = PageImages.request(context, page, attempt),
                                contentDescription = "Страница ${index + 1} из ${pages.size}",
                                contentScale = ContentScale.FillWidth,
                                onState = { state ->
                                    when (state) {
                                        is AsyncImagePainter.State.Error -> {
                                            if (state.result.throwable !is CancellationException) {
                                                loadedPages = loadedPages - index
                                                if (attempt + 1 < PageImages.MAX_ATTEMPTS) {
                                                    PageImages.evict(context, page)
                                                    failedPages = failedPages - index
                                                    pageRetryNonce = pageRetryNonce + (index to attempt + 1)
                                                } else {
                                                    failedPages = failedPages + index
                                                }
                                            }
                                        }
                                        is AsyncImagePainter.State.Success -> {
                                            val drawable = state.result.drawable
                                            val ok = drawable.intrinsicWidth >= 8 &&
                                                drawable.intrinsicHeight >= 8
                                            if (!ok && attempt + 1 < PageImages.MAX_ATTEMPTS) {
                                                PageImages.evict(context, page)
                                                pageRetryNonce = pageRetryNonce + (index to attempt + 1)
                                            } else if (!ok) {
                                                failedPages = failedPages + index
                                                loadedPages = loadedPages - index
                                            } else {
                                                failedPages = failedPages - index
                                                loadedPages = loadedPages + index
                                            }
                                        }
                                        else -> Unit
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 280.dp)
                                    .graphicsLayer {
                                        scaleX = zoomScale
                                        scaleY = zoomScale
                                        translationX = zoomOffset.x
                                        translationY = zoomOffset.y
                                    },
                            )
                        }
                        if (index !in loadedPages && index !in failedPages) {
                            Column(
                                modifier = Modifier.padding(vertical = 52.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Загрузка страницы ${index + 1}",
                                    color = TomiloMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        if (index in failedPages) {
                            Column(
                                modifier = Modifier.padding(vertical = 48.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    Icons.Default.BrokenImage,
                                    contentDescription = null,
                                    tint = TomiloMuted,
                                    modifier = Modifier.size(36.dp),
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Не удалось загрузить страницу ${index + 1}",
                                    color = Color.White,
                                )
                                TextButton(
                                    onClick = {
                                        failedPages = failedPages - index
                                        loadedPages = loadedPages - index
                                        PageImages.evict(context, page)
                                        val attempt = pageRetryNonce[index] ?: 0
                                        pageRetryNonce = pageRetryNonce + (index to attempt + 1)
                                    },
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Text("Повторить")
                                }
                            }
                        }
                    }
                }
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(Color.Black)
                            .padding(top = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            if (hasNext || chapters.isEmpty()) {
                                "Потяните ниже — следующая глава откроется автоматически"
                            } else {
                                "Вы дочитали доступные главы"
                            },
                            color = TomiloMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        TextButton(
                            onClick = { goPrev() },
                            enabled = hasPrev || chapters.isEmpty(),
                        ) { Text("← Пред.", color = Color.White) }
                        TextButton(
                            onClick = { goNext() },
                            enabled = hasNext || chapters.isEmpty(),
                        ) { Text("След. →", color = Color.White) }
                    }
                }
            }
        }


        AnimatedVisibility(
            visible = chapterNavMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                chapterNavMessage.orEmpty(),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.82f), MaterialTheme.shapes.medium)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(22.dp)).background(Color.Black.copy(alpha = 0.90f)),
            ) {
                TopAppBar(
                    title = {
                        Text(
                            if (offline) "$title · offline" else title,
                            color = Color.White,
                            maxLines = 1,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { leaveReader() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = Color.White,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showComments = true }) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = "Комментарии главы",
                                tint = Color.White,
                            )
                        }
                        IconButton(
                            onClick = {
                                effectiveTitleId?.takeIf { it.isNotBlank() }?.let(onOpenTitle)
                            },
                            enabled = !effectiveTitleId.isNullOrBlank(),
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = "Открыть страницу тайтла",
                                tint = if (effectiveTitleId.isNullOrBlank()) TomiloMuted else Color.White,
                            )
                        }
                        IconButton(onClick = {
                            fullscreen = !fullscreen
                            if (fullscreen) chromeVisible = false
                        }) {
                            Icon(
                                if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Полный экран",
                                tint = Color.White,
                            )
                        }
                        IconButton(onClick = { showChapters = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = "Главы",
                                tint = Color.White,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
                LinearProgressIndicator(
                    progress = { readProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.DarkGray,
                )
            }
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.92f))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { goPrev() },
                        enabled = hasPrev || chapters.isEmpty(),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.NavigateBefore,
                            contentDescription = "Предыдущая",
                            tint = if (hasPrev || chapters.isEmpty()) Color.White else Color.Gray,
                        )
                    }
                    IconButton(onClick = {
                        autoScroll = !autoScroll
                        if (autoScroll) chromeVisible = false
                    }) {
                        Icon(
                            if (autoScroll) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Автопрокрутка",
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = { showComments = true }) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = "Комментарии главы",
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = { showSpeed = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки чтения", tint = Color.White)
                    }
                    IconButton(
                        onClick = { goNext() },
                        enabled = hasNext || chapters.isEmpty(),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = "Следующая",
                            tint = if (hasNext || chapters.isEmpty()) Color.White else Color.Gray,
                        )
                    }
                }
                if (pages.size > 1) {
                    Slider(
                        value = pageSliderValue.coerceIn(0f, pages.lastIndex.toFloat()),
                        onValueChange = {
                            pageSliderActive = true
                            pageSliderValue = it
                        },
                        onValueChangeFinished = {
                            val target = pageSliderValue.toInt().coerceIn(0, pages.lastIndex)
                            scope.launch { listState.animateScrollToItem(target) }
                            pageSliderActive = false
                        },
                        valueRange = 0f..pages.lastIndex.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                    )
                }
                Text(
                    text = if (autoScroll) "Автопрокрутка · скорость ${String.format(Locale.ROOT, "%.1f", speed)}"
                    else "Стр. ${(listState.firstVisibleItemIndex + 1).coerceAtMost(pages.size)} / ${pages.size} · " +
                        "${(readProgress * 100).toInt()}%" +
                        if (failedPages.isNotEmpty()) " · ошибок: ${failedPages.size}"
                        else " · двойной тап увеличивает",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }

    if (showSpeed) {
        ModalBottomSheet(
            onDismissRequest = { showSpeed = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF1E222A),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Настройки чтения", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(18.dp))
                Text("Скорость автопрокрутки", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Slider(
                    value = speed,
                    onValueChange = { speed = it },
                    valueRange = 0.4f..5f,
                    steps = 22,
                )
                Text("${String.format(Locale.ROOT, "%.1f", speed)}×", color = TomiloMuted)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Не выключать экран", color = Color.White)
                        Text("Пока открыта читалка", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = settings.keepScreenOn,
                        onCheckedChange = { value ->
                            scope.launch { readingPrefs.setKeepScreenOn(value) }
                        },
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Открывать без панелей", color = Color.White)
                        Text("Полноэкранный режим по умолчанию", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = settings.startFullscreen,
                        onCheckedChange = { value ->
                            scope.launch { readingPrefs.setStartFullscreen(value) }
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch { readingPrefs.setAutoScrollSpeed(speed) }
                        showSpeed = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Готово") }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showComments) {
        ModalBottomSheet(
            onDismissRequest = { showComments = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 680.dp)) {
                item(key = "chapter-comments-$currentChapterId") {
                    CommentsSection(
                        entityType = "chapter",
                        entityId = currentChapterId,
                        socialRepository = socialRepository,
                        isLoggedIn = user != null,
                        onLoginRequired = onLogin,
                        onOpenUser = onOpenUser,
                    )
                    Spacer(Modifier.height(30.dp))
                }
            }
        }
    }

    if (showChapters) {
        ModalBottomSheet(
            onDismissRequest = { showChapters = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF1E222A),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Text(
                "Главы" + if (chapters.isNotEmpty()) " (${chapters.size})" else "",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            if (chapters.isEmpty()) {
                Text(
                    "Список загружается…",
                    color = TomiloMuted,
                    modifier = Modifier.padding(20.dp),
                )
            } else {
                LazyColumn(
                    state = chaptersListState,
                    modifier = Modifier.height(420.dp),
                ) {
                    items(chapters, key = { it.stableId() }) { ch ->
                        val selected = ch.stableId() == currentChapterId
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    showChapters = false
                                    goChapter(ch.stableId())
                                }
                                .background(
                                    if (selected) Color.White.copy(alpha = 0.08f)
                                    else Color.Transparent,
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Глава ${ch.numberLabel()}" +
                                    (ch.name?.takeIf { n ->
                                        n.isNotBlank() && !n.startsWith("Глава")
                                    }?.let { " — $it" } ?: "") +
                                    if (selected) "  · сейчас" else "",
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                                style = if (selected) {
                                    MaterialTheme.typography.titleMedium
                                } else {
                                    MaterialTheme.typography.bodyLarge
                                },
                                modifier = Modifier.weight(1f),
                            )
                            if (ChapterAccess.isPremiumOnly(
                                    ch.isPaid,
                                    ch.freeAt,
                                    ch.isUnlockedByActivityCoins,
                                ) && !isPremium
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Premium",
                                    tint = TomiloMuted,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
