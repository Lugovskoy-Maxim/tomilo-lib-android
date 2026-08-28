package ru.tomilo.lib.mobile.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import ru.tomilo.lib.mobile.ui.theme.TomiloText

private enum class ShelfTab(val label: String, val bookmarkCategory: String? = null) {
    Reading("Читаю", "reading"),
    Planned("Планы", "planned"),
    Completed("Прочитано", "completed"),
    Favorites("Избранное", "favorites"),
    Dropped("Брошено", "dropped"),
    History("История"),
    Offline("Офлайн"),
}

@Composable
private fun LibrarySummary(tab: ShelfTab, count: Int, isSearching: Boolean) {
    val icon: ImageVector
    val title: String
    val subtitle: String
    when (tab) {
        ShelfTab.History -> {
            icon = Icons.Outlined.History
            title = "История чтения"
            subtitle = "Вернитесь к тайтлу в один тап"
        }
        ShelfTab.Offline -> {
            icon = Icons.Outlined.CloudOff
            title = "Офлайн-библиотека"
            subtitle = "Главы доступны без подключения"
        }
        else -> {
            icon = Icons.Outlined.BookmarkBorder
            title = tab.label
            subtitle = "Свайпните карточку влево для действий"
        }
    }
    val countLabel = if (isSearching) "Найдено: $count" else "$count ${count.libraryItemsLabel()}"

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(TomiloPrimary.copy(alpha = 0.14f), TomiloSurface2, TomiloSurface),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(TomiloPrimary.copy(alpha = 0.16f)),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = TomiloPrimary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
        }
        Text(countLabel, color = TomiloText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

private fun Int.libraryItemsLabel(): String = when {
    this % 100 in 11..14 -> "тайтлов"
    this % 10 == 1 -> "тайтл"
    this % 10 in 2..4 -> "тайтла"
    else -> "тайтлов"
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
    var lastBookmarkTab by remember { mutableStateOf(ShelfTab.Reading) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var bookmarks by remember { mutableStateOf<List<BookmarkEntryDto>>(emptyList()) }
    var history by remember { mutableStateOf<List<HistoryEntryDto>>(emptyList()) }
    val offline by offlineRepository.observeAll().collectAsState(initial = emptyList())
    var reload by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val reveal = rememberSwipeRevealCoordinator()

    LaunchedEffect(user?.stableId(), reload, tab) {
        if (user == null) {
            loading = false
            error = null
            bookmarks = emptyList()
            history = emptyList()
            return@LaunchedEffect
        }
        loading = true
        error = null
        when (tab) {
            ShelfTab.History -> historyRepository.history()
                .onSuccess { history = it }
                .onFailure { error = it.message }
            ShelfTab.Offline -> Unit
            else -> socialRepository.bookmarks(tab.bookmarkCategory)
                .onSuccess { bookmarks = it }
                .onFailure { error = it.message }
        }
        loading = false
    }

    val needle = query.trim()
    val filteredBookmarks = remember(bookmarks, needle) {
        if (needle.isBlank()) bookmarks
        else bookmarks.filter { it.displayName().contains(needle, ignoreCase = true) }
    }
    val filteredHistory = remember(history, needle) {
        if (needle.isBlank()) history
        else history.filter {
            it.displayTitle().contains(needle, ignoreCase = true) ||
                it.chapterLabel().contains(needle, ignoreCase = true)
        }
    }
    val offlineGroups = remember(offline, needle) {
        offline.groupBy { it.titleId.ifBlank { it.titleName } }
            .toList()
            .filter { (_, chapters) ->
                needle.isBlank() || chapters.any { ch ->
                    ch.titleName.contains(needle, ignoreCase = true)
                }
            }
            .sortedByDescending { it.second.maxOfOrNull { ch -> ch.downloadedAt } ?: 0L }
    }

    Scaffold(
        containerColor = TomiloBg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Полка", fontWeight = FontWeight.Bold) },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp),
        ) {
            item(key = "library_search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(TomiloSurface2.copy(alpha = 0.8f)),
                    singleLine = true,
                    placeholder = { Text("Поиск на полке") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TomiloPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                )
            }
            item(key = "library_tabs") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = tab.bookmarkCategory != null,
                        onClick = { tab = lastBookmarkTab },
                        label = { Text("Закладки") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = tab == ShelfTab.History,
                        onClick = { tab = ShelfTab.History },
                        label = { Text("История") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = tab == ShelfTab.Offline,
                        onClick = { tab = ShelfTab.Offline },
                        label = { Text("Офлайн") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (tab.bookmarkCategory != null) {
                item(key = "library_bookmark_categories") {
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ShelfTab.entries.filter { it.bookmarkCategory != null }.forEach { item ->
                            FilterChip(
                                selected = tab == item,
                                onClick = {
                                    lastBookmarkTab = item
                                    tab = item
                                },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
            }
            item(key = "library_summary_${tab.name}") {
                val count = when (tab) {
                    ShelfTab.History -> filteredHistory.size
                    ShelfTab.Offline -> offlineGroups.size
                    else -> filteredBookmarks.size
                }
                LibrarySummary(tab = tab, count = count, isSearching = needle.isNotBlank())
            }

            when {
                user == null && tab != ShelfTab.Offline -> item(key = "library_guest") {
                    ErrorBox(
                        "Войдите, чтобы видеть полку",
                        modifier = Modifier.fillMaxWidth().height(380.dp),
                        onRetry = onLogin,
                    )
                }
                loading && tab != ShelfTab.Offline -> item(key = "library_loading") {
                    ListCardsSkeleton(count = 5)
                }
                error != null && tab != ShelfTab.Offline -> item(key = "library_error") {
                    ErrorBox(
                        error ?: "Ошибка",
                        modifier = Modifier.fillMaxWidth().height(380.dp),
                    ) { reload += 1 }
                }
                tab.bookmarkCategory != null && filteredBookmarks.isEmpty() -> item(key = "library_empty_bookmarks") {
                    EmptyState(
                        title = if (needle.isBlank()) "Пока пусто" else "Нет совпадений",
                        message = if (needle.isBlank()) {
                            "Добавьте тайтл в «${tab.label}» со страницы тайтла."
                        } else {
                            "На полке нет «$needle» в категории «${tab.label}»."
                        },
                        icon = Icons.Outlined.BookmarkBorder,
                        modifier = Modifier.fillMaxWidth().height(380.dp).padding(ScreenPadding),
                    )
                }
                tab == ShelfTab.History && filteredHistory.isEmpty() -> item(key = "library_empty_history") {
                    EmptyState(
                        title = "История пуста",
                        message = "Откройте главу — продолжение чтения появится на полке и на ленте.",
                        icon = Icons.Outlined.History,
                        modifier = Modifier.fillMaxWidth().height(380.dp).padding(ScreenPadding),
                    )
                }
                tab == ShelfTab.Offline && offlineGroups.isEmpty() -> item(key = "library_empty_offline") {
                    EmptyState(
                        title = "Нет офлайн-глав",
                        message = "Скачайте главы со страницы тайтла — они откроются без сети.",
                        icon = Icons.Outlined.CloudOff,
                        modifier = Modifier.fillMaxWidth().height(380.dp).padding(ScreenPadding),
                    )
                }
                else -> {
                    when (tab) {
                        ShelfTab.History, ShelfTab.Offline -> Unit
                        else -> items(filteredBookmarks, key = { it.resolvedTitleId() + tab.name }) { item ->
                            val titleId = item.resolvedTitleId()
                            SwipeActionContainer(
                                actionLabel = "Убрать",
                                actionIcon = Icons.Outlined.DeleteOutline,
                                actionColor = TomiloDanger,
                                enabled = titleId.isNotBlank(),
                                revealKey = "bm-$titleId-${tab.name}",
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
                                    subtitle = tab.label,
                                    onClick = {
                                        onOpenTitle(titleId, item.resolvedTitle()?.slug)
                                    },
                                )
                            }
                        }
                    }
                    when (tab) {
                        ShelfTab.History -> items(
                            filteredHistory,
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
                        else -> Unit
                    }
                }
            }
        }
    }
}
