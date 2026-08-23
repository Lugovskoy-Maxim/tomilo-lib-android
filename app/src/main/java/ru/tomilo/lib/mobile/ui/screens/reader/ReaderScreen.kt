package ru.tomilo.lib.mobile.ui.screens.reader

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.ads.ChapterTransitionAds
import ru.tomilo.lib.mobile.core.ChapterAccess
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.core.PageImages
import ru.tomilo.lib.mobile.core.PageDimensions
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.core.ReaderDirection
import ru.tomilo.lib.mobile.core.ReaderLayout
import ru.tomilo.lib.mobile.core.ReaderMode
import ru.tomilo.lib.mobile.core.WebtoonTile
import ru.tomilo.lib.mobile.core.WebtoonTiles
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
import ru.tomilo.lib.mobile.ui.components.RewardNotifications
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
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
    var currentChapterId by rememberSaveable { mutableStateOf(chapterId) }
    val listState = rememberSaveable(currentChapterId, saver = LazyListState.Saver) {
        LazyListState(0, 0)
    }
    val chaptersListState = rememberLazyListState()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var needsPremium by remember { mutableStateOf(false) }
    var needsLogin by remember { mutableStateOf(false) }
    var pages by remember { mutableStateOf<List<String>>(emptyList()) }
    var pageDimensions by remember { mutableStateOf<List<PageDimensions>>(emptyList()) }
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
    var title by remember { mutableStateOf("Глава") }
    var offline by remember { mutableStateOf(false) }
    var titleType by remember { mutableStateOf<String?>(null) }
    var layout by remember { mutableStateOf(ReaderLayout.WEBTOON) }
    var direction by remember { mutableStateOf(ReaderDirection.LTR) }

    var chromeVisible by remember { mutableStateOf(!settings.startFullscreen) }
    var fullscreen by remember { mutableStateOf(settings.startFullscreen) }
    var autoScroll by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(settings.autoScrollSpeed) }
    var brightness by remember { mutableFloatStateOf(-1f) }
    var showChapters by remember { mutableStateOf(false) }
    var chapterQuery by rememberSaveable { mutableStateOf("") }
    var showComments by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
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

    LaunchedEffect(effectiveTitleId, titleType) {
        val tid = effectiveTitleId.orEmpty()
        layout = readingPrefs.layoutFor(tid, titleType)
        direction = readingPrefs.directionFor(tid, titleType)
    }

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
    val atTitleEnd = !hasNext && chapters.isNotEmpty()
    val visibleChapters = remember(chapters, chapterQuery) {
        val needle = chapterQuery.trim()
        if (needle.isBlank()) chapters else chapters.filter { chapter ->
            chapter.numberLabel().contains(needle, ignoreCase = true) ||
                chapter.name.orEmpty().contains(needle, ignoreCase = true)
        }
    }
    val canOpenTitle = !effectiveTitleId.isNullOrBlank()
    val currentPage by remember(layout, pagerState, listState) {
        derivedStateOf {
            if (layout == ReaderLayout.PAGER) pagerState.currentPage
            else listState.firstVisibleItemIndex
        }
    }

    DisposableEffect(settings.keepScreenOn) {
        val window = activity?.window
        if (settings.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    DisposableEffect(brightness) {
        val window = activity?.window
        val attrs = window?.attributes
        if (window != null && attrs != null) {
            attrs.screenBrightness = if (brightness < 0f) WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE else brightness
            window.attributes = attrs
        }
        onDispose {
            if (window != null) {
                val reset = window.attributes
                reset.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = reset
            }
        }
    }

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
            pageDimensions = emptyList()
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

            if (preferOffline || offlineRepository.isDownloaded(id)) {
                val local = offlineRepository.getLocalPages(id)
                if (!local.isNullOrEmpty()) {
                    val entity = offlineRepository.getEntity(id)
                    effectiveTitleId = titleId ?: entity?.titleId
                    title = entity?.let { "Глава ${it.chapterNumber}" } ?: "Глава (офлайн)"
                    val localSources = local.map { File(it).toURI().toString() }
                    pageDimensions = WebtoonTiles.measureLocalSources(localSources)
                    pages = localSources
                    offline = true
                    loading = false
                    val tid = effectiveTitleId
                    if (!tid.isNullOrBlank()) {
                        val loggedIn = authRepository.isLoggedIn()
                        readingPrefs.markLocalRead(tid, id, queueSync = loggedIn)
                        if (loggedIn) {
                            historyRepository.markRead(tid, id)
                                .onSuccess { reward ->
                                    readingPrefs.markHistorySynced(tid, id)
                                    RewardNotifications.show(
                                        experience = reward.experienceGained,
                                        coins = reward.coinsGained,
                                        source = reward.reason ?: "Чтение главы",
                                    )
                                    if (reward.experienceGained != 0 || reward.coinsGained != 0) {
                                        authRepository.refreshProfile()
                                    }
                                }
                        }
                    }
                    return@launch
                }
            }

            var subExpires = user?.subscriptionExpiresAt
            if (authRepository.isLoggedIn()) {
                authRepository.refreshProfile().onSuccess { subExpires = it.subscriptionExpiresAt }
            }

            suspend fun applyChapter(chapter: ChapterDto, allowRetry: Boolean) {
                title = chapter.name?.ifBlank { "Глава ${chapter.numberLabel()}" }
                    ?: "Глава ${chapter.numberLabel()}"
                offline = false
                chapter.titleKey().takeIf { it.isNotBlank() }?.let { effectiveTitleId = it }
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
                val resolved = chapter.pagePaths().map { MediaUrl.resolve(it) }.filter { it.isNotBlank() }
                when {
                    resolved.isNotEmpty() -> {
                        pageDimensions = chapter.pageDimensions.orEmpty()
                        pages = resolved
                        val resolvedTitleId = chapter.titleKey().ifBlank { effectiveTitleId.orEmpty() }
                        if (resolvedTitleId.isNotBlank()) {
                            val loggedIn = authRepository.isLoggedIn()
                            readingPrefs.markLocalRead(resolvedTitleId, id, queueSync = loggedIn)
                            if (loggedIn) {
                                historyRepository.markRead(resolvedTitleId, id)
                                    .onSuccess { reward ->
                                        readingPrefs.markHistorySynced(resolvedTitleId, id)
                                        RewardNotifications.show(
                                            experience = reward.experienceGained,
                                            coins = reward.coinsGained,
                                            source = reward.reason ?: "Чтение главы",
                                        )
                                        if (reward.experienceGained != 0 || reward.coinsGained != 0) {
                                            authRepository.refreshProfile()
                                        }
                                    }
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
                    canRead && allowRetry && authRepository.isLoggedIn() -> {
                        authRepository.refreshProfile().onSuccess {
                            subExpires = it.subscriptionExpiresAt
                        }
                        catalogRepository.chapter(id)
                            .onSuccess { applyChapter(it, allowRetry = false) }
                            .onFailure { error = it.message ?: "Не удалось открыть главу" }
                    }
                    canRead -> error = "Страницы пока недоступны. Откройте главу снова."
                    else -> error = "Страницы недоступны"
                }
            }

            catalogRepository.chapter(id)
                .onSuccess { applyChapter(it, allowRetry = true) }
                .onFailure { error = it.message ?: "Не удалось открыть главу" }
            loading = false
        }
    }

    LaunchedEffect(currentChapterId, loading, pages.size, pendingRestore, layout) {
        if (loading || pages.isEmpty()) return@LaunchedEffect
        val pos = pendingRestore ?: ReadingPosition()
        val index = pos.pageIndex.coerceIn(0, pages.lastIndex)
        val offset = if (pos.pageIndex > pages.lastIndex) 0 else pos.scrollOffset.coerceAtLeast(0)
        if (layout == ReaderLayout.PAGER) {
            runCatching { pagerState.scrollToPage(index) }
        } else {
            runCatching { listState.scrollToItem(index, offset) }
        }
        restoredChapterId = currentChapterId
    }

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

    fun currentPosition(): Pair<Int, Int> {
        return if (layout == ReaderLayout.PAGER) {
            pagerState.currentPage to 0
        } else {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
    }

    /** Явный переход к родительскому тайтлу не зависит от того, откуда открыли читалку. */
    fun openParentTitle() {
        val id = currentChapterId
        val (index, offset) = currentPosition()
        scope.launch { readingPrefs.saveReadingPosition(id, index, offset) }
        effectiveTitleId?.takeIf { it.isNotBlank() }?.let(onOpenTitle) ?: onBack()
    }

    suspend fun stepPage(forward: Boolean) {
        if (pages.isEmpty()) return
        if (layout == ReaderLayout.PAGER) {
            val next = if (forward) pagerState.currentPage + 1 else pagerState.currentPage - 1
            when {
                next in pages.indices -> pagerState.animateScrollToPage(next)
                forward -> goNext()
                else -> goPrev()
            }
        } else {
            val next = if (forward) listState.firstVisibleItemIndex + 1 else listState.firstVisibleItemIndex - 1
            when {
                next in pages.indices -> listState.animateScrollToItem(next)
                forward -> goNext()
                else -> goPrev()
            }
        }
    }

    LaunchedEffect(chapterId) {
        if (chapterId.isNotBlank()) {
            loadChapter(currentChapterId.ifBlank { chapterId })
        }
    }

    LaunchedEffect(user?.stableId(), user?.subscriptionExpiresAt) {
        if (pages.isEmpty() && !loading && (needsPremium || error != null)) {
            loadChapter(currentChapterId)
        }
    }

    LaunchedEffect(titleId, effectiveTitleId) {
        val tid = titleId ?: effectiveTitleId
        if (!tid.isNullOrBlank()) {
            catalogRepository.title(tid).onSuccess { detail ->
                titleType = detail.type
                effectiveTitleId = detail.stableId().ifBlank { tid }
            }
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

    LaunchedEffect(showChapters, chapters, currentChapterId) {
        if (!showChapters || chapters.isEmpty()) return@LaunchedEffect
        val idx = chapters.indexOfFirst { it.stableId() == currentChapterId }
        if (idx >= 0) chaptersListState.scrollToItem((idx - 2).coerceAtLeast(0))
    }

    LaunchedEffect(autoScroll, speed, pages, layout) {
        if (!autoScroll || pages.isEmpty() || layout != ReaderLayout.WEBTOON) return@LaunchedEffect
        while (isActive && autoScroll) {
            val delta = (speed * 2.2f).toInt().coerceAtLeast(1)
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

    LaunchedEffect(listState, pagerState, currentChapterId, pages.size, restoredChapterId, layout) {
        if (pages.isEmpty() || restoredChapterId != currentChapterId) return@LaunchedEffect
        if (layout == ReaderLayout.PAGER) {
            snapshotFlow { pagerState.currentPage }
                .debounce(450)
                .collect { index -> readingPrefs.saveReadingPosition(currentChapterId, index, 0) }
        } else {
            snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                .debounce(450)
                .collect { (index, offset) ->
                    readingPrefs.saveReadingPosition(currentChapterId, index, offset)
                }
        }
    }

    LaunchedEffect(chapterNavMessage) {
        if (chapterNavMessage != null) {
            delay(2_500)
            chapterNavMessage = null
        }
    }

    LaunchedEffect(currentChapterId, pages.size, loading, layout) {
        if (pages.isEmpty() || loading || layout != ReaderLayout.WEBTOON) return@LaunchedEffect
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
        else openParentTitle()
    }

    var readProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(listState, pagerState, pages.size, layout) {
        snapshotFlow {
            if (pages.isEmpty()) return@snapshotFlow 0f
            if (layout == ReaderLayout.PAGER) {
                ((pagerState.currentPage + 1).toFloat() / pages.size.toFloat()).coerceIn(0f, 1f)
            } else {
                val info = listState.layoutInfo
                val visible = info.visibleItemsInfo.firstOrNull() ?: return@snapshotFlow 0f
                val pageIndex = visible.index.coerceIn(0, pages.lastIndex)
                val withinPage = if (visible.size > 0) (-visible.offset).toFloat() / visible.size else 0f
                ((pageIndex + withinPage.coerceIn(0f, 1f)) / pages.size.toFloat()).coerceIn(0f, 1f)
            }
        }.collect { readProgress = it }
    }

    LaunchedEffect(currentPage, pages.size, pageSliderActive) {
        if (!pageSliderActive) {
            pageSliderValue = currentPage.coerceIn(0, pages.lastIndex.coerceAtLeast(0)).toFloat()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            loading -> ReaderLoading()
            error != null && needsPremium -> PremiumGate(
                message = error,
                needsLogin = needsLogin,
                onLogin = onLogin,
                onPremium = onOpenPremium,
                onRetry = { loadChapter(currentChapterId) },
            )
            error != null -> ReaderError(error ?: "Ошибка") { loadChapter(currentChapterId) }
            pages.isEmpty() -> ReaderError("Нет страниц") { loadChapter(currentChapterId) }
            layout == ReaderLayout.PAGER -> PagerReader(
                pages = pages,
                pagerState = pagerState,
                direction = direction,
                chapterId = currentChapterId,
                failedPages = failedPages,
                loadedPages = loadedPages,
                pageRetryNonce = pageRetryNonce,
                onRetry = { index, page ->
                    failedPages = failedPages - index
                    loadedPages = loadedPages - index
                    PageImages.evict(context, page)
                    pageRetryNonce = pageRetryNonce + (index to (pageRetryNonce[index] ?: 0) + 1)
                },
                onState = { index, success, attempt, page ->
                    handlePageState(
                        index = index,
                        success = success,
                        attempt = attempt,
                        page = page,
                        context = context,
                        failedPages = failedPages,
                        loadedPages = loadedPages,
                        pageRetryNonce = pageRetryNonce,
                        onFailed = { failedPages = it },
                        onLoaded = { loadedPages = it },
                        onRetryMap = { pageRetryNonce = it },
                    )
                },
                onToggleChrome = { chromeVisible = !chromeVisible; if (chromeVisible) autoScroll = false },
                onPrevPage = { scope.launch { stepPage(forward = false) } },
                onNextPage = { scope.launch { stepPage(forward = true) } },
            )
            else -> WebtoonReader(
                pages = pages,
                pageDimensions = pageDimensions,
                listState = listState,
                chapterId = currentChapterId,
                failedPages = failedPages,
                loadedPages = loadedPages,
                pageRetryNonce = pageRetryNonce,
                hasNext = hasNext || chapters.isEmpty(),
                showTitleButton = atTitleEnd && canOpenTitle,
                onOpenTitle = { openParentTitle() },
                onRetry = { index, page ->
                    failedPages = failedPages - index
                    loadedPages = loadedPages - index
                    PageImages.evict(context, page)
                    pageRetryNonce = pageRetryNonce + (index to (pageRetryNonce[index] ?: 0) + 1)
                },
                onState = { index, success, attempt, page ->
                    handlePageState(
                        index = index,
                        success = success,
                        attempt = attempt,
                        page = page,
                        context = context,
                        failedPages = failedPages,
                        loadedPages = loadedPages,
                        pageRetryNonce = pageRetryNonce,
                        onFailed = { failedPages = it },
                        onLoaded = { loadedPages = it },
                        onRetryMap = { pageRetryNonce = it },
                    )
                },
                onToggleChrome = { chromeVisible = !chromeVisible; if (chromeVisible) autoScroll = false },
                onPrev = { goPrev() },
                onNext = { goNext() },
            )
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
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
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
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent),
                        ),
                    )
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 10.dp, end = 10.dp, bottom = 22.dp),
            ) {
                Surface(
                    color = Color(0xEA15151A),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    shadowElevation = 10.dp,
                ) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { openParentTitle() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "К странице тайтла", tint = Color.White)
                            }
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = canOpenTitle) { openParentTitle() }
                                    .padding(horizontal = 4.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    title,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                )
                                Text(
                                    buildString {
                                        append("Стр. ${(currentPage + 1).coerceAtMost(pages.size)} из ${pages.size}")
                                        append(" · ")
                                        append(ReaderMode.layoutLabel(layout))
                                        if (offline) append(" · офлайн")
                                    },
                                    color = TomiloMuted,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                )
                            }
                            Text(
                                "${(readProgress * 100).toInt().coerceIn(0, 100)}%",
                                color = TomiloPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(TomiloPrimary.copy(alpha = 0.13f))
                                    .padding(horizontal = 9.dp, vertical = 6.dp),
                            )
                            IconButton(onClick = { chapterQuery = ""; showChapters = true }) {
                                Icon(Icons.AutoMirrored.Filled.List, "Главы", tint = Color.White)
                            }
                        }
                        LinearProgressIndicator(
                            progress = { readProgress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(3.dp),
                            color = TomiloPrimary,
                            trackColor = Color.White.copy(alpha = 0.08f),
                        )
                    }
                }
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
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)),
                        ),
                    )
                    .navigationBarsPadding()
                    .padding(start = 10.dp, end = 10.dp, top = 26.dp, bottom = 8.dp),
            ) {
                Surface(
                    color = Color(0xF216161B),
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    shadowElevation = 14.dp,
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Страница ${(currentPage + 1).coerceAtMost(pages.size)}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                if (currentIndex >= 0) "Глава ${currentIndex + 1} из ${chapters.size}" else "${pages.size} стр.",
                                color = TomiloMuted,
                                style = MaterialTheme.typography.labelSmall,
                            )
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
                                    scope.launch {
                                        if (layout == ReaderLayout.PAGER) pagerState.animateScrollToPage(target)
                                        else listState.animateScrollToItem(target)
                                    }
                                    pageSliderActive = false
                                },
                                valueRange = 0f..pages.lastIndex.toFloat(),
                                modifier = Modifier.height(34.dp),
                            )
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            ReaderDockAction(
                                icon = Icons.AutoMirrored.Filled.NavigateBefore,
                                label = "Пред.",
                                enabled = hasPrev || chapters.isEmpty(),
                                modifier = Modifier.weight(1f),
                                onClick = { goPrev() },
                            )
                            if (layout == ReaderLayout.WEBTOON) {
                                ReaderDockAction(
                                    icon = if (autoScroll) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    label = if (autoScroll) "Стоп" else "Авто",
                                    active = autoScroll,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        autoScroll = !autoScroll
                                        if (autoScroll) chromeVisible = false
                                    },
                                )
                            } else {
                                ReaderDockAction(
                                    icon = Icons.AutoMirrored.Filled.MenuBook,
                                    label = "Тайтл",
                                    enabled = canOpenTitle,
                                    modifier = Modifier.weight(1f),
                                    onClick = { openParentTitle() },
                                )
                            }
                            ReaderDockAction(
                                icon = Icons.Default.ChatBubbleOutline,
                                label = "Обсудить",
                                modifier = Modifier.weight(1f),
                                onClick = { showComments = true },
                            )
                            ReaderDockAction(
                                icon = Icons.Default.Settings,
                                label = "Режим",
                                modifier = Modifier.weight(1f),
                                onClick = { showSettings = true },
                            )
                            ReaderDockAction(
                                icon = Icons.AutoMirrored.Filled.NavigateNext,
                                label = "След.",
                                enabled = hasNext || chapters.isEmpty(),
                                modifier = Modifier.weight(1f),
                                onClick = { goNext() },
                            )
                        }
                        if (atTitleEnd && canOpenTitle) {
                            Button(
                                onClick = { openParentTitle() },
                                modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                            ) { Text("Вернуться к тайтлу") }
                        }
                        if (failedPages.isNotEmpty()) {
                            Text(
                                "Не загрузилось страниц: ${failedPages.size}",
                                color = Color(0xFFE98273),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            )
                        }
                    }
                }
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
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp),
            ) {
                item(key = "chapter-comments-$currentChapterId") {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(TomiloPrimary.copy(alpha = 0.13f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.ChatBubbleOutline, null, tint = TomiloPrimary)
                        }
                        Spacer(Modifier.size(11.dp))
                        Column {
                            Text("Обсуждение главы", style = MaterialTheme.typography.titleLarge)
                            Text("Мнения читателей без ухода со страницы", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
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

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = TomiloSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text("Режим чтения", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text("Настройте читалку под этот тайтл", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(14.dp))
                Text("Раскладка", color = TomiloMuted, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = layout == ReaderLayout.WEBTOON,
                        onClick = {
                            layout = ReaderLayout.WEBTOON
                            scope.launch { readingPrefs.setLayoutFor(effectiveTitleId.orEmpty(), layout) }
                        },
                        label = { Text("Лента") },
                        leadingIcon = { Icon(Icons.Default.ViewDay, null, Modifier.size(16.dp)) },
                    )
                    FilterChip(
                        selected = layout == ReaderLayout.PAGER,
                        onClick = {
                            layout = ReaderLayout.PAGER
                            autoScroll = false
                            scope.launch { readingPrefs.setLayoutFor(effectiveTitleId.orEmpty(), layout) }
                        },
                        label = { Text("Страницы") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, null, Modifier.size(16.dp)) },
                    )
                }
                if (layout == ReaderLayout.PAGER) {
                    Spacer(Modifier.height(14.dp))
                    Text("Направление", color = TomiloMuted, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = direction == ReaderDirection.LTR,
                            onClick = {
                                direction = ReaderDirection.LTR
                                scope.launch { readingPrefs.setDirectionFor(effectiveTitleId.orEmpty(), direction) }
                            },
                            label = { Text("Слева направо") },
                        )
                        FilterChip(
                            selected = direction == ReaderDirection.RTL,
                            onClick = {
                                direction = ReaderDirection.RTL
                                scope.launch { readingPrefs.setDirectionFor(effectiveTitleId.orEmpty(), direction) }
                            },
                            label = { Text("Справа налево") },
                            leadingIcon = { Icon(Icons.Default.SwapHoriz, null, Modifier.size(16.dp)) },
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Яркость", color = Color.White)
                Slider(
                    value = if (brightness < 0f) 0.55f else brightness,
                    onValueChange = { brightness = it },
                    valueRange = 0.08f..1f,
                )
                if (layout == ReaderLayout.WEBTOON) {
                    Text("Скорость ленты", color = Color.White)
                    Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.4f..5f, steps = 22)
                    Text("${String.format(Locale.ROOT, "%.1f", speed)}×", color = TomiloMuted)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Не выключать экран", color = Color.White)
                        Text("Пока открыта читалка", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = settings.keepScreenOn,
                        onCheckedChange = { value -> scope.launch { readingPrefs.setKeepScreenOn(value) } },
                    )
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Сразу без панелей", color = Color.White)
                        Text("Полный экран при открытии", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = settings.startFullscreen,
                        onCheckedChange = { value ->
                            scope.launch { readingPrefs.setStartFullscreen(value) }
                            fullscreen = value
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch { readingPrefs.setAutoScrollSpeed(speed) }
                        showSettings = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Готово") }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showChapters) {
        ModalBottomSheet(
            onDismissRequest = { showChapters = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = TomiloSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Главы", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text(
                        if (currentIndex >= 0) "Сейчас ${currentIndex + 1} из ${chapters.size}" else "Всего ${chapters.size}",
                        color = TomiloMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = { showChapters = false }) {
                    Icon(Icons.Default.Close, "Закрыть", tint = Color.White)
                }
            }
            if (chapters.isEmpty()) {
                Text("Список загружается…", color = TomiloMuted, modifier = Modifier.padding(20.dp))
            } else {
                OutlinedTextField(
                    value = chapterQuery,
                    onValueChange = { chapterQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    placeholder = { Text("Номер или название главы") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (chapterQuery.isNotBlank()) {
                            IconButton(onClick = { chapterQuery = "" }) {
                                Icon(Icons.Default.Close, "Очистить")
                            }
                        }
                    },
                )
                if (visibleChapters.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().height(260.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.Search, null, tint = TomiloMuted, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Главы не найдены", color = Color.White)
                        Text("Измените запрос", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                    }
                } else LazyColumn(state = chaptersListState, modifier = Modifier.fillMaxHeight(0.68f)) {
                    items(visibleChapters, key = { it.stableId() }) { ch ->
                        val selected = ch.stableId() == currentChapterId
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(17.dp))
                                .clickable {
                                    showChapters = false
                                    goChapter(ch.stableId())
                                }
                                .background(if (selected) TomiloPrimary.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.025f))
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Глава ${ch.numberLabel()}" +
                                    (ch.name?.takeIf { n -> n.isNotBlank() && !n.startsWith("Глава") }?.let { " — $it" } ?: "") +
                                    if (selected) "  · сейчас" else "",
                                color = if (selected) TomiloPrimary else Color.White,
                                style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            if (ChapterAccess.isPremiumOnly(ch.isPaid, ch.freeAt, ch.isUnlockedByActivityCoins) && !isPremium) {
                                Icon(Icons.Default.Lock, "Premium", tint = TomiloMuted, modifier = Modifier.size(18.dp))
                            } else if (selected) {
                                Icon(Icons.Default.CheckCircle, "Текущая глава", tint = TomiloPrimary, modifier = Modifier.size(19.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReaderDockAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = when {
        !enabled -> Color.White.copy(alpha = 0.28f)
        active -> TomiloPrimary
        else -> Color.White.copy(alpha = 0.88f)
    }
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) TomiloPrimary.copy(alpha = 0.13f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = tint, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

private fun handlePageState(
    index: Int,
    success: Boolean?,
    attempt: Int,
    page: String,
    context: android.content.Context,
    failedPages: Set<Int>,
    loadedPages: Set<Int>,
    pageRetryNonce: Map<Int, Int>,
    onFailed: (Set<Int>) -> Unit,
    onLoaded: (Set<Int>) -> Unit,
    onRetryMap: (Map<Int, Int>) -> Unit,
) {
    when (success) {
        false -> {
            onLoaded(loadedPages - index)
            if (attempt + 1 < PageImages.MAX_ATTEMPTS) {
                PageImages.evict(context, page)
                onFailed(failedPages - index)
                onRetryMap(pageRetryNonce + (index to attempt + 1))
            } else {
                onFailed(failedPages + index)
            }
        }
        true -> {
            onFailed(failedPages - index)
            onLoaded(loadedPages + index)
        }
        null -> Unit
    }
}

@Composable
private fun ReaderLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(TomiloPrimary.copy(alpha = 0.24f), Color(0xFF17171D))))
                    .border(1.dp, TomiloPrimary.copy(alpha = 0.30f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = TomiloPrimary,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(Modifier.height(15.dp))
            Text("Открываем главу", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text("Подготавливаем страницы и позицию чтения", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ReaderError(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.Center) {
        Surface(
            color = Color(0xFF17171D),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(62.dp).clip(RoundedCornerShape(21.dp)).background(Color(0xFFE98273).copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.BrokenImage, null, tint = Color(0xFFE98273), modifier = Modifier.size(31.dp)) }
                Spacer(Modifier.height(15.dp))
                Text("Страница не открылась", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(5.dp))
                Text(message, color = TomiloMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(18.dp))
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(7.dp))
                    Text("Попробовать снова")
                }
            }
        }
    }
}

@Composable
private fun PremiumGate(
    message: String?,
    needsLogin: Boolean,
    onLogin: () -> Unit,
    onPremium: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.Center) {
        Surface(
            color = Color(0xFF17171D),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color(0xFFE4B85D).copy(alpha = 0.30f)),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(68.dp).clip(RoundedCornerShape(23.dp)).background(Color(0xFFE4B85D).copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Lock, null, tint = Color(0xFFE4B85D), modifier = Modifier.size(32.dp)) }
                Spacer(Modifier.height(15.dp))
                Text("Глава доступна в Premium", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(5.dp))
                Text(message ?: "Платная глава", color = TomiloMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(18.dp))
                if (needsLogin) {
                    OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Войти в аккаунт") }
                    Spacer(Modifier.height(8.dp))
                }
                Button(onClick = onPremium, modifier = Modifier.fillMaxWidth()) { Text("Оформить Premium") }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Проверить доступ снова") }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WebtoonReader(
    pages: List<String>,
    pageDimensions: List<PageDimensions>,
    listState: LazyListState,
    chapterId: String,
    failedPages: Set<Int>,
    loadedPages: Set<Int>,
    pageRetryNonce: Map<Int, Int>,
    hasNext: Boolean,
    showTitleButton: Boolean = false,
    onOpenTitle: () -> Unit = {},
    onRetry: (Int, String) -> Unit,
    onState: (index: Int, success: Boolean?, attempt: Int, page: String) -> Unit,
    onToggleChrome: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(pages, key = { i, _ -> "$chapterId-$i" }) { index, page ->
            val dimensions = pageDimensions.getOrNull(index)
            if (dimensions?.isValid() == true) {
                TiledWebtoonPage(
                    page = page,
                    index = index,
                    total = pages.size,
                    dimensions = dimensions,
                    attempt = pageRetryNonce[index] ?: 0,
                    onTap = onToggleChrome,
                )
            } else {
                ReaderPage(
                    page = page,
                    index = index,
                    total = pages.size,
                    failed = index in failedPages,
                    loaded = index in loadedPages,
                    attempt = pageRetryNonce[index] ?: 0,
                    fillHeight = false,
                    onRetry = { onRetry(index, page) },
                    onState = { success, attempt -> onState(index, success, attempt, page) },
                    onTap = onToggleChrome,
                )
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 18.dp),
                color = Color(0xFF15151A),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(54.dp).clip(CircleShape).background(TomiloPrimary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = TomiloPrimary, modifier = Modifier.size(29.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (hasNext) "Глава прочитана" else "Вы дочитали доступные главы",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (hasNext) "Следующая глава откроется автоматически" else "Можно вернуться к тайтлу или обсудить главу",
                        color = TomiloMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (showTitleButton) {
                        Button(
                            onClick = onOpenTitle,
                            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                        ) { Text("Вернуться к тайтлу") }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = onPrev) { Text("← Предыдущая", color = Color.White) }
                        if (hasNext) {
                            TextButton(onClick = onNext) { Text("Следующая →", color = Color.White) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TiledWebtoonPage(
    page: String,
    index: Int,
    total: Int,
    dimensions: PageDimensions,
    attempt: Int,
    onTap: () -> Unit,
) {
    val tiles = remember(dimensions) { WebtoonTiles.split(dimensions) }
    Column(Modifier.fillMaxWidth().background(Color.Black)) {
        tiles.forEach { tile ->
            WebtoonTileImage(
                page = page,
                pageIndex = index,
                totalPages = total,
                tile = tile,
                attempt = attempt,
                onTap = onTap,
            )
        }
    }
}

@Composable
private fun WebtoonTileImage(
    page: String,
    pageIndex: Int,
    totalPages: Int,
    tile: WebtoonTile,
    attempt: Int,
    onTap: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var active by remember(page, tile.index) { mutableStateOf(false) }
    var bitmap by remember(page, tile.index, attempt) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var loading by remember(page, tile.index, attempt) { mutableStateOf(false) }
    var error by remember(page, tile.index, attempt) { mutableStateOf<String?>(null) }
    var localRetry by remember(page, tile.index, attempt) { mutableIntStateOf(0) }

    LaunchedEffect(active, page, tile, attempt, localRetry) {
        if (!active) {
            delay(900)
            if (!active) bitmap = null
            return@LaunchedEffect
        }
        if (bitmap != null || loading) return@LaunchedEffect
        loading = true
        error = null
        var lastFailure: Throwable? = null
        repeat(PageImages.MAX_ATTEMPTS) { retry ->
            val result = runCatching {
                WebtoonTiles.decode(context, page, tile, retry = attempt + localRetry + retry)
            }
            result.onSuccess {
                bitmap = it
                loading = false
                return@LaunchedEffect
            }.onFailure { lastFailure = it }
            delay(300L * (retry + 1))
        }
        error = lastFailure?.message ?: "Не удалось загрузить фрагмент"
        loading = false
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(tile.width.toFloat() / tile.height.toFloat())
            .background(Color.Black)
            .onGloballyPositioned { coordinates ->
                val top = coordinates.positionInWindow().y
                val bottom = top + coordinates.size.height
                val screenHeight = view.height.takeIf { it > 0 }
                    ?: context.resources.displayMetrics.heightPixels
                active = bottom >= -screenHeight * 0.5f &&
                    top <= screenHeight * 1.5f
            }
            .pointerInput(page, tile.index) {
                detectTapGestures(onTap = { onTap() })
            },
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let { ready ->
            Image(
                bitmap = ready.asImageBitmap(),
                contentDescription = "Страница ${pageIndex + 1} из $totalPages, фрагмент ${tile.index + 1}",
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.High,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (loading && bitmap == null) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(25.dp),
                color = TomiloPrimary,
                strokeWidth = 2.dp,
            )
        }
        if (error != null && bitmap == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                Icon(Icons.Default.BrokenImage, null, tint = TomiloMuted)
                Text("Не загрузился фрагмент", color = Color.White)
                TextButton(onClick = { localRetry += 1 }) {
                    Icon(Icons.Default.Refresh, null)
                    Text("Повторить")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagerReader(
    pages: List<String>,
    pagerState: PagerState,
    direction: ReaderDirection,
    chapterId: String,
    failedPages: Set<Int>,
    loadedPages: Set<Int>,
    pageRetryNonce: Map<Int, Int>,
    onRetry: (Int, String) -> Unit,
    onState: (index: Int, success: Boolean?, attempt: Int, page: String) -> Unit,
    onToggleChrome: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            reverseLayout = direction == ReaderDirection.RTL,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            val page = pages.getOrNull(index) ?: return@HorizontalPager
            key("$chapterId-$index") {
                ReaderPage(
                    page = page,
                    index = index,
                    total = pages.size,
                    failed = index in failedPages,
                    loaded = index in loadedPages,
                    attempt = pageRetryNonce[index] ?: 0,
                    fillHeight = true,
                    onRetry = { onRetry(index, page) },
                    onState = { success, attempt -> onState(index, success, attempt, page) },
                    onTap = onToggleChrome,
                )
            }
        }
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    ) {
                        if (direction == ReaderDirection.RTL) onNextPage() else onPrevPage()
                    },
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    ) { onToggleChrome() },
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    ) {
                        if (direction == ReaderDirection.RTL) onPrevPage() else onNextPage()
                    },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReaderPage(
    page: String,
    index: Int,
    total: Int,
    failed: Boolean,
    loaded: Boolean,
    attempt: Int,
    fillHeight: Boolean,
    onRetry: () -> Unit,
    onState: (success: Boolean?, attempt: Int) -> Unit,
    onTap: () -> Unit,
) {
    val context = LocalContext.current
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
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
            .background(Color.Black)
            .clipToBounds()
            .transformable(state = transformState, canPan = { zoomScale > 1f })
            .pointerInput(page) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        zoomScale = if (zoomScale > 1f) 1f else 2f
                        if (zoomScale == 1f) zoomOffset = Offset.Zero
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        key(attempt) {
            AsyncImage(
                model = PageImages.request(context, page, attempt),
                contentDescription = "Страница ${index + 1} из $total",
                contentScale = if (fillHeight) ContentScale.Fit else ContentScale.FillWidth,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Error -> {
                            if (state.result.throwable !is CancellationException) {
                                onState(false, attempt)
                            }
                        }
                        is AsyncImagePainter.State.Success -> {
                            val ok = state.result.drawable.intrinsicWidth >= 8 &&
                                state.result.drawable.intrinsicHeight >= 8
                            onState(if (ok) true else false, attempt)
                        }
                        else -> Unit
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier.heightIn(min = 280.dp))
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = zoomOffset.x
                        translationY = zoomOffset.y
                    },
            )
        }
        if (!loaded && !failed) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(48.dp)) {
                androidx.compose.material3.CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.dp, color = TomiloPrimary)
                Spacer(Modifier.height(8.dp))
                Text("Страница ${index + 1}", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (failed) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.BrokenImage, null, tint = TomiloMuted, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(8.dp))
                Text("Не загрузилась страница ${index + 1}", color = Color.White)
                TextButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, null)
                    Text("Повторить")
                }
            }
        }
    }
}
