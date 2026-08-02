package ru.tomilo.lib.mobile.ui.screens.title

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.api.ChapterDto
import ru.tomilo.lib.mobile.data.api.TitleDetailDto
import ru.tomilo.lib.mobile.data.download.DownloadManager
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.data.repo.HistoryRepository
import ru.tomilo.lib.mobile.data.repo.OfflineRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.CommentsSection
import ru.tomilo.lib.mobile.ui.components.DownloadProgressSheet
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

private enum class ChapterSort(val label: String) {
    NumberAsc("№ ↑"),
    NumberDesc("№ ↓"),
    DateNew("Новые"),
    DateOld("Старые"),
    Views("Просмотры"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleScreen(
    titleKey: String,
    catalogRepository: CatalogRepository,
    offlineRepository: OfflineRepository,
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    historyRepository: HistoryRepository,
    downloadManager: DownloadManager,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenChapter: (titleId: String, chapterId: String, offline: Boolean) -> Unit,
    onOpenUser: (userId: String) -> Unit,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf<TitleDetailDto?>(null) }
    var chapters by remember { mutableStateOf<List<ChapterDto>>(emptyList()) }
    var bookmarked by remember { mutableStateOf(false) }
    var bookmarkCategory by remember { mutableStateOf<String?>(null) }
    var selectMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var showDownloadSheet by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(ChapterSort.NumberAsc) }
    var myRating by remember { mutableIntStateOf(0) }

    val sortedChapters = remember(chapters, sort) {
        when (sort) {
            ChapterSort.NumberAsc -> chapters.sortedBy { it.chapterNumberAsDouble() ?: Double.MAX_VALUE }
            ChapterSort.NumberDesc -> chapters.sortedByDescending { it.chapterNumberAsDouble() ?: -1.0 }
            ChapterSort.DateNew -> chapters.sortedByDescending { it.releaseDate.orEmpty() }
            ChapterSort.DateOld -> chapters.sortedBy { it.releaseDate.orEmpty() }
            ChapterSort.Views -> chapters.sortedByDescending {
                it.views?.toString()?.trim('"')?.toDoubleOrNull() ?: 0.0
            }
        }
    }

    val offlineAll by offlineRepository.observeAll().collectAsState(initial = emptyList())
    val downloadState by downloadManager.state.collectAsState()
    val downloadedIds = remember(offlineAll, title?.stableId()) {
        val tid = title?.stableId().orEmpty()
        offlineAll.filter { tid.isNotBlank() && it.titleId == tid }.map { it.chapterId }.toSet()
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(titleKey) {
        loading = true
        error = null
        selectMode = false
        selected = emptySet()
        val t = catalogRepository.title(titleKey)
        t.onFailure {
            error = it.message
            loading = false
            return@LaunchedEffect
        }
        val detail = t.getOrThrow()
        title = detail
        catalogRepository.chapters(detail.stableId(), limit = 200)
            .onSuccess { chapters = it }
            .onFailure { error = it.message }
        loading = false
    }

    LaunchedEffect(title?.stableId(), user?.stableId()) {
        val tid = title?.stableId().orEmpty()
        if (tid.isBlank() || user == null) {
            bookmarked = false
            bookmarkCategory = null
            return@LaunchedEffect
        }
        socialRepository.bookmarkStatus(tid)
            .onSuccess {
                bookmarked = it.active()
                bookmarkCategory = it.category
            }
    }

    LaunchedEffect(downloadState.finished, downloadState.items) {
        if (downloadState.items.isNotEmpty()) showDownloadSheet = true
    }

    Scaffold(
        containerColor = TomiloBg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectMode) "Выбрано: ${selected.size}"
                        else title?.name ?: "Тайтл",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectMode) {
                            selectMode = false
                            selected = emptySet()
                        } else onBack()
                    }) {
                        Icon(
                            if (selectMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                        )
                    }
                },
                actions = {
                    if (selectMode) {
                        IconButton(onClick = {
                            val allIds = sortedChapters.map { it.stableId() }.filter { it !in downloadedIds }
                            selected = if (selected.size >= allIds.size) emptySet() else allIds.toSet()
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Выбрать все")
                        }
                    } else {
                        IconButton(onClick = { selectMode = true }) {
                            Icon(Icons.Default.CheckBoxOutlineBlank, contentDescription = "Выбор загрузки")
                        }
                        IconButton(
                            onClick = {
                                val tid = title?.stableId().orEmpty()
                                if (tid.isBlank()) return@IconButton
                                if (user == null) {
                                    onLogin()
                                    return@IconButton
                                }
                                scope.launch {
                                    if (bookmarked) {
                                        socialRepository.removeBookmark(tid)
                                            .onSuccess {
                                                bookmarked = false
                                                bookmarkCategory = null
                                                snackbar.showSnackbar("Убрано из закладок")
                                            }
                                    } else {
                                        socialRepository.addBookmark(tid, "reading")
                                            .onSuccess {
                                                bookmarked = true
                                                bookmarkCategory = "reading"
                                                snackbar.showSnackbar("В закладках")
                                            }
                                    }
                                }
                            },
                        ) {
                            Icon(
                                if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Закладка",
                                tint = if (bookmarked) MaterialTheme.colorScheme.primary else TomiloMuted,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TomiloBg),
            )
        },
        bottomBar = {
            if (selectMode) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(TomiloSurface2)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = {
                        selectMode = false
                        selected = emptySet()
                    }, modifier = Modifier.weight(1f)) { Text("Отмена") }
                    Button(
                        onClick = {
                            val t = title ?: return@Button
                            if (user == null) {
                                onLogin()
                                return@Button
                            }
                            val toDownload = sortedChapters.filter {
                                it.stableId() in selected && it.stableId() !in downloadedIds
                            }
                            if (toDownload.isEmpty()) {
                                scope.launch { snackbar.showSnackbar("Нечего скачивать") }
                                return@Button
                            }
                            downloadManager.enqueue(
                                titleId = t.stableId(),
                                titleName = t.name.orEmpty(),
                                titleSlug = t.slug.orEmpty(),
                                titleCover = t.coverImage,
                                chapters = toDownload,
                            )
                            showDownloadSheet = true
                            selectMode = false
                            selected = emptySet()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selected.isNotEmpty() && !downloadManager.isBusy(),
                    ) {
                        Text("Скачать (${selected.size})")
                    }
                }
            }
        },
    ) { padding ->
        when {
            loading -> LoadingBox(Modifier.padding(padding))
            error != null && title == null -> ErrorBox(error ?: "Ошибка")
            title != null -> {
                val t = title!!
                LazyColumn(
                    Modifier.padding(padding).fillMaxSize(),
                ) {
                    item {
                        Row(Modifier.padding(16.dp)) {
                            AsyncImage(
                                model = MediaUrl.resolve(t.coverImage),
                                contentDescription = t.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(110.dp)
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(TomiloSurface2),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(t.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(6.dp))
                                val meta = listOfNotNull(
                                    t.type,
                                    t.status,
                                    t.releaseYear?.toString(),
                                    t.averageRating?.let { "★ %.1f".format(it) },
                                    t.totalChapters?.let { "$it гл." },
                                ).joinToString(" · ")
                                Text(meta, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Долгое нажатие / □ — выбор глав для офлайн",
                                    color = TomiloMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        if (!t.description.isNullOrBlank()) {
                            Text(
                                t.description!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TomiloMuted,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                        // Rating
                        Text(
                            "Оценка",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        Row(
                            Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp),
                        ) {
                            (1..10).forEach { star ->
                                FilterChip(
                                    selected = myRating == star,
                                    onClick = {
                                        if (user == null) {
                                            onLogin()
                                            return@FilterChip
                                        }
                                        scope.launch {
                                            historyRepository.rateTitle(t.stableId(), star)
                                                .onSuccess {
                                                    myRating = star
                                                    snackbar.showSnackbar("Оценка: $star/10")
                                                }
                                                .onFailure {
                                                    snackbar.showSnackbar(it.message ?: "Ошибка")
                                                }
                                        }
                                    },
                                    label = { Text("$star") },
                                    modifier = Modifier.padding(horizontal = 2.dp),
                                )
                            }
                        }
                        Text(
                            "Главы · сортировка",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                        Row(
                            Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            ChapterSort.entries.forEach { option ->
                                FilterChip(
                                    selected = sort == option,
                                    onClick = { sort = option },
                                    label = { Text(option.label) },
                                    modifier = Modifier.padding(horizontal = 3.dp),
                                )
                            }
                        }
                    }
                    items(sortedChapters, key = { it.stableId() }) { chapter ->
                        val id = chapter.stableId()
                        val isOffline = id in downloadedIds
                        val isSelected = id in selected
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectMode) {
                                        if (isOffline) return@clickable
                                        selected = if (isSelected) selected - id else selected + id
                                    } else {
                                        onOpenChapter(t.stableId(), id, isOffline)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (selectMode) {
                                Checkbox(
                                    checked = isSelected || isOffline,
                                    onCheckedChange = {
                                        if (isOffline) return@Checkbox
                                        selected = if (isSelected) selected - id else selected + id
                                    },
                                    enabled = !isOffline,
                                )
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                                Text("Глава ${chapter.numberLabel()}")
                                if (!chapter.name.isNullOrBlank() &&
                                    chapter.name != "Глава ${chapter.numberLabel()}"
                                ) {
                                    Text(
                                        chapter.name!!,
                                        color = TomiloMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                            if (!selectMode) {
                                IconButton(
                                    onClick = {
                                        if (isOffline) {
                                            scope.launch {
                                                offlineRepository.deleteChapter(id)
                                                snackbar.showSnackbar("Удалено из офлайн")
                                            }
                                            return@IconButton
                                        }
                                        if (user == null) {
                                            onLogin()
                                            return@IconButton
                                        }
                                        downloadManager.enqueue(
                                            titleId = t.stableId(),
                                            titleName = t.name.orEmpty(),
                                            titleSlug = t.slug.orEmpty(),
                                            titleCover = t.coverImage,
                                            chapters = listOf(chapter),
                                        )
                                        showDownloadSheet = true
                                    },
                                ) {
                                    Icon(
                                        if (isOffline) Icons.Default.DownloadDone else Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = if (isOffline) MaterialTheme.colorScheme.primary else TomiloMuted,
                                    )
                                }
                            } else if (isOffline) {
                                Icon(
                                    Icons.Default.DownloadDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp).padding(end = 8.dp),
                                )
                            }
                        }
                    }
                    item {
                        CommentsSection(
                            entityType = "title",
                            entityId = t.stableId(),
                            socialRepository = socialRepository,
                            isLoggedIn = user != null,
                            onLoginRequired = onLogin,
                            onOpenUser = onOpenUser,
                        )
                        Spacer(Modifier.height(100.dp))
                    }
                }
            }
        }
    }

    if (showDownloadSheet && downloadState.items.isNotEmpty()) {
        DownloadProgressSheet(
            state = downloadState,
            onCancel = { downloadManager.cancel() },
            onDismiss = {
                showDownloadSheet = false
                downloadManager.clear()
            },
        )
    }
}
