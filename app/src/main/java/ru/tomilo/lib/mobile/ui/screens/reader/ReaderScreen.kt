package ru.tomilo.lib.mobile.ui.screens.reader

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.api.ChapterDto
import ru.tomilo.lib.mobile.data.local.ReadingPrefs
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.data.repo.OfflineRepository
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    chapterId: String,
    titleId: String?,
    preferOffline: Boolean,
    catalogRepository: CatalogRepository,
    offlineRepository: OfflineRepository,
    readingPrefs: ReadingPrefs,
    onBack: () -> Unit,
    onOpenChapter: (chapterId: String) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? android.app.Activity
    val settings by readingPrefs.settingsFlow.collectAsState(
        initial = ru.tomilo.lib.mobile.data.local.ReadingSettings(),
    )
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var pages by remember { mutableStateOf<List<String>>(emptyList()) }
    var title by remember { mutableStateOf("Глава") }
    var offline by remember { mutableStateOf(false) }
    var currentChapterId by remember(chapterId) { mutableStateOf(chapterId) }

    var chromeVisible by remember { mutableStateOf(!settings.startFullscreen) }
    var fullscreen by remember { mutableStateOf(settings.startFullscreen) }
    var autoScroll by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(settings.autoScrollSpeed) }
    var showChapters by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var chapters by remember { mutableStateOf<List<ChapterDto>>(emptyList()) }
    var hasPrev by remember { mutableStateOf(false) }
    var hasNext by remember { mutableStateOf(false) }

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

    fun loadChapter(id: String) {
        scope.launch {
            loading = true
            error = null
            currentChapterId = id
            autoScroll = false
            if (preferOffline || offlineRepository.isDownloaded(id)) {
                val local = offlineRepository.getLocalPages(id)
                if (!local.isNullOrEmpty()) {
                    val entity = offlineRepository.getEntity(id)
                    title = entity?.let { "Глава ${it.chapterNumber}" } ?: "Глава (офлайн)"
                    pages = local.map { File(it).toURI().toString() }
                    offline = true
                    loading = false
                    return@launch
                }
            }
            catalogRepository.chapter(id)
                .onSuccess { ch ->
                    title = ch.name?.ifBlank { "Глава ${ch.numberLabel()}" }
                        ?: "Глава ${ch.numberLabel()}"
                    pages = ch.pages.orEmpty().map { MediaUrl.resolve(it) }
                    offline = false
                    if (pages.isEmpty()) error = "Страницы недоступны"
                }
                .onFailure { error = it.message ?: "Не удалось открыть главу" }
            loading = false
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(chapterId) { loadChapter(chapterId) }

    LaunchedEffect(titleId, currentChapterId) {
        val tid = titleId
        if (!tid.isNullOrBlank()) {
            catalogRepository.chapters(tid, limit = 200)
                .onSuccess { chapters = it }
        }
        catalogRepository.chapterPrev(currentChapterId)
            .onSuccess { hasPrev = true }
            .onFailure { hasPrev = false }
        catalogRepository.chapterNext(currentChapterId)
            .onSuccess { hasNext = true }
            .onFailure { hasNext = false }
    }

    // Auto-scroll loop
    LaunchedEffect(autoScroll, speed, pages) {
        if (!autoScroll || pages.isEmpty()) return@LaunchedEffect
        while (isActive && autoScroll) {
            val delta = (speed * 2.2f).toInt().coerceAtLeast(1)
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()
            if (last != null && last.index >= pages.lastIndex &&
                last.offset + last.size <= info.viewportEndOffset + 4
            ) {
                autoScroll = false
                break
            }
            listState.dispatchRawDelta(delta.toFloat())
            delay(16L)
        }
    }

    BackHandler {
        if (showChapters) showChapters = false
        else if (!chromeVisible) chromeVisible = true
        else onBack()
    }

    val progress = remember(listState, pages.size) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = pages.size.coerceAtLeast(1)
            val first = info.visibleItemsInfo.firstOrNull()?.index ?: 0
            (first + 1).toFloat() / total
        }
    }
    var readProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(progress) {
        progress.collect { readProgress = it }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        chromeVisible = !chromeVisible
                        if (chromeVisible) autoScroll = false
                    },
                )
            },
    ) {
        when {
            loading -> LoadingBox()
            error != null -> ErrorBox(error ?: "Ошибка") { loadChapter(currentChapterId) }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(pages, key = { i, _ -> "$currentChapterId-$i" }) { index, page ->
                    AsyncImage(
                        model = page,
                        contentDescription = "Стр. ${index + 1}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    catalogRepository.chapterPrev(currentChapterId)
                                        .onSuccess { onOpenChapter(it.stableId()) }
                                }
                            },
                            enabled = hasPrev,
                        ) { Text("← Пред.", color = Color.White) }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    catalogRepository.chapterNext(currentChapterId)
                                        .onSuccess { onOpenChapter(it.stableId()) }
                                }
                            },
                            enabled = hasNext,
                        ) { Text("След. →", color = Color.White) }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.88f))) {
                TopAppBar(
                    title = {
                        Text(
                            if (offline) "$title · offline" else title,
                            color = Color.White,
                            maxLines = 1,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = Color.White,
                            )
                        }
                    },
                    actions = {
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
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                catalogRepository.chapterPrev(currentChapterId)
                                    .onSuccess { onOpenChapter(it.stableId()) }
                            }
                        },
                        enabled = hasPrev,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.NavigateBefore,
                            contentDescription = "Предыдущая",
                            tint = if (hasPrev) Color.White else Color.Gray,
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
                    IconButton(onClick = { showSpeed = true }) {
                        Icon(Icons.Default.Speed, contentDescription = "Скорость", tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                catalogRepository.chapterNext(currentChapterId)
                                    .onSuccess { onOpenChapter(it.stableId()) }
                            }
                        },
                        enabled = hasNext,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = "Следующая",
                            tint = if (hasNext) Color.White else Color.Gray,
                        )
                    }
                }
                Text(
                    text = if (autoScroll) "Автопрокрутка · скорость ${"%.1f".format(speed)}"
                    else "Тап — скрыть панели · ${pages.size} стр.",
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
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Скорость автопрокрутки", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Slider(
                    value = speed,
                    onValueChange = { speed = it },
                    valueRange = 0.4f..5f,
                    steps = 22,
                )
                Text("${"%.1f".format(speed)}×", color = TomiloMuted)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    scope.launch { readingPrefs.setAutoScrollSpeed(speed) }
                    showSpeed = false
                }) { Text("Сохранить") }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showChapters) {
        ModalBottomSheet(
            onDismissRequest = { showChapters = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF1E222A),
        ) {
            Text(
                "Главы",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            LazyColumn(Modifier.height(420.dp)) {
                items(chapters, key = { it.stableId() }) { ch ->
                    val selected = ch.stableId() == currentChapterId
                    Text(
                        text = "Глава ${ch.numberLabel()}" +
                            (ch.name?.takeIf { it.isNotBlank() && !it.startsWith("Глава") }?.let { " — $it" } ?: ""),
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showChapters = false
                                onOpenChapter(ch.stableId())
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
