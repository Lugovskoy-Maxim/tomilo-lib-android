package ru.tomilo.lib.mobile.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.data.api.BookmarkEntryDto
import ru.tomilo.lib.mobile.data.api.HistoryEntryDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.HistoryRepository
import ru.tomilo.lib.mobile.data.repo.OfflineRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.ListCardsSkeleton
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.components.SwipeActionContainer
import ru.tomilo.lib.mobile.ui.components.TitleSearchCard
import ru.tomilo.lib.mobile.ui.components.rememberSwipeRevealCoordinator
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloDanger
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface

private enum class ShelfTab(val label: String) {
    Reading("Читаю"),
    History("История"),
    Offline("Офлайн"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    historyRepository: HistoryRepository,
    offlineRepository: OfflineRepository,
    onLogin: () -> Unit,
    onOpenTitle: (id: String, slug: String?) -> Unit,
    onContinue: (titleId: String, chapterId: String, offline: Boolean) -> Unit,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    var tab by remember { mutableStateOf(ShelfTab.Reading) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var bookmarks by remember { mutableStateOf<List<BookmarkEntryDto>>(emptyList()) }
    var history by remember { mutableStateOf<List<HistoryEntryDto>>(emptyList()) }
    val offline by offlineRepository.observeAll().collectAsState(initial = emptyList())
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val reveal = rememberSwipeRevealCoordinator()

    LaunchedEffect(user?.stableId(), reload, tab) {
        if (user == null) return@LaunchedEffect
        loading = true
        error = null
        when (tab) {
            ShelfTab.Reading -> socialRepository.bookmarks("reading")
                .onSuccess { bookmarks = it }
                .onFailure { error = it.message }
            ShelfTab.History -> historyRepository.history()
                .onSuccess { history = it }
                .onFailure { error = it.message }
            ShelfTab.Offline -> Unit
        }
        loading = false
    }

    val offlineGroups = remember(offline) {
        offline.groupBy { it.titleId.ifBlank { it.titleName } }
            .toList()
            .sortedByDescending { it.second.maxOfOrNull { ch -> ch.downloadedAt } ?: 0L }
    }

    Scaffold(
        containerColor = TomiloBg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(title = { Text("Полка") }, colors = tomiloTopBarColors())
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Row(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(TomiloSurface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ShelfTab.entries.forEach { item ->
                    FilterChip(
                        selected = tab == item,
                        onClick = { tab = item },
                        label = { Text(item.label) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (user == null && tab != ShelfTab.Offline) {
                ErrorBox("Войдите, чтобы видеть полку", onRetry = onLogin)
                return@Column
            }

            when {
                loading && tab != ShelfTab.Offline -> ListCardsSkeleton()
                error != null && tab != ShelfTab.Offline -> ErrorBox(error ?: "Ошибка") { reload += 1 }
                tab == ShelfTab.Reading && bookmarks.isEmpty() -> EmptyState(
                    title = "Пока пусто",
                    message = "Добавьте тайтл в «Читаю» — он появится здесь одной кнопкой.",
                    icon = Icons.Outlined.BookmarkBorder,
                    modifier = Modifier.padding(ScreenPadding),
                )
                tab == ShelfTab.History && history.isEmpty() -> EmptyState(
                    title = "История пуста",
                    message = "Откройте главу — продолжение чтения появится на полке и на ленте.",
                    icon = Icons.Outlined.History,
                    modifier = Modifier.padding(ScreenPadding),
                )
                tab == ShelfTab.Offline && offlineGroups.isEmpty() -> EmptyState(
                    title = "Нет офлайн-глав",
                    message = "Скачайте главы с страницы тайтла — они откроются без сети.",
                    icon = Icons.Outlined.CloudOff,
                    modifier = Modifier.padding(ScreenPadding),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 110.dp),
                ) {
                    when (tab) {
                        ShelfTab.Reading -> items(bookmarks, key = { it.resolvedTitleId() }) { item ->
                            val titleId = item.resolvedTitleId()
                            SwipeActionContainer(
                                actionLabel = "Убрать",
                                actionIcon = Icons.Outlined.DeleteOutline,
                                actionColor = TomiloDanger,
                                enabled = titleId.isNotBlank(),
                                revealKey = "bm-$titleId",
                                coordinator = reveal,
                                onAction = {
                                    val snapshot = bookmarks
                                    bookmarks = bookmarks.filterNot { it === item }
                                    scope.launch {
                                        socialRepository.removeBookmark(titleId)
                                            .onSuccess { snackbar.showSnackbar("Убрано с полки") }
                                            .onFailure {
                                                bookmarks = snapshot
                                                snackbar.showSnackbar(it.message ?: "Не удалось удалить")
                                            }
                                    }
                                },
                            ) {
                                TitleSearchCard(
                                    title = item.displayName(),
                                    cover = item.coverPath(),
                                    subtitle = "Читаю",
                                    onClick = {
                                        onOpenTitle(titleId, item.resolvedTitle()?.slug)
                                    },
                                )
                            }
                        }
                        ShelfTab.History -> items(
                            history,
                            key = { it.titleKey() + (it.chapterKey()) },
                        ) { item ->
                            val chapterId = item.chapterKey()
                            val titleId = item.titleKey()
                            SwipeActionContainer(
                                actionLabel = "Удалить",
                                actionIcon = Icons.Outlined.DeleteOutline,
                                actionColor = TomiloDanger,
                                enabled = titleId.isNotBlank(),
                                revealKey = "hi-$titleId-$chapterId",
                                coordinator = reveal,
                                onAction = {
                                    val snapshot = history
                                    history = history.filterNot { it === item }
                                    scope.launch {
                                        historyRepository.deleteTitleHistory(titleId)
                                            .onSuccess { snackbar.showSnackbar("Удалено из истории") }
                                            .onFailure {
                                                history = snapshot
                                                snackbar.showSnackbar(it.message ?: "Не удалось удалить")
                                            }
                                    }
                                },
                            ) {
                                TitleSearchCard(
                                    title = item.displayTitle(),
                                    cover = item.coverPath(),
                                    subtitle = item.chapterLabel(),
                                    onClick = {
                                        if (chapterId.isNotBlank()) {
                                            onContinue(titleId, chapterId, false)
                                        } else {
                                            onOpenTitle(titleId, item.slug())
                                        }
                                    },
                                )
                            }
                        }
                        ShelfTab.Offline -> items(offlineGroups, key = { it.first }) { (titleId, chapters) ->
                            val first = chapters.first()
                            SwipeActionContainer(
                                actionLabel = "Удалить",
                                actionIcon = Icons.Outlined.DeleteOutline,
                                actionColor = TomiloDanger,
                                enabled = titleId.isNotBlank(),
                                revealKey = "off-$titleId",
                                coordinator = reveal,
                                onAction = {
                                    scope.launch {
                                        offlineRepository.deleteTitle(titleId)
                                        snackbar.showSnackbar("Офлайн-копия удалена")
                                    }
                                },
                            ) {
                                TitleSearchCard(
                                    title = first.titleName.ifBlank { "Тайтл" },
                                    cover = first.titleCover,
                                    subtitle = "${chapters.size} гл. офлайн",
                                    onClick = {
                                        val latest = chapters.maxByOrNull { it.downloadedAt }
                                        if (latest != null) {
                                            onContinue(titleId, latest.chapterId, true)
                                        } else {
                                            onOpenTitle(titleId, first.titleSlug)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
