package ru.tomilo.lib.mobile.ui.screens.bookmarks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.data.api.BookmarkEntryDto
import ru.tomilo.lib.mobile.data.api.ReadingProgressDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.HistoryRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.components.TitleSearchCard
import ru.tomilo.lib.mobile.ui.components.SwipeActionContainer
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloDanger

private val CATEGORIES = listOf(
    null to "Все",
    "reading" to "Читаю",
    "planned" to "В планах",
    "completed" to "Прочитано",
    "favorites" to "Избранное",
    "dropped" to "Брошено",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    historyRepository: HistoryRepository,
    onLogin: () -> Unit,
    onOpenTitle: (id: String, slug: String?) -> Unit,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    var catIndex by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<BookmarkEntryDto>>(emptyList()) }
    var progressByTitle by remember { mutableStateOf<Map<String, ReadingProgressDto>>(emptyMap()) }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var authReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        authReady = true
    }

    LaunchedEffect(user?.stableId(), catIndex, reload, authReady) {
        if (!authReady) return@LaunchedEffect
        if (user == null) {
            items = emptyList()
            progressByTitle = emptyMap()
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        val cat = CATEGORIES[catIndex].first
        socialRepository.bookmarks(cat)
            .onSuccess { list ->
                items = list.filter {
                    it.resolvedTitleId().isNotBlank() ||
                        it.displayName() != "Тайтл" ||
                        it.coverPath() != null
                }.ifEmpty { list }
            }
            .onFailure { error = it.message }
        loading = false
    }

    // Прогресс чтения (прочитано X / Y) для карточек
    LaunchedEffect(items, user?.stableId()) {
        if (user == null || items.isEmpty()) {
            progressByTitle = emptyMap()
            return@LaunchedEffect
        }
        val ids = items.map { it.resolvedTitleId() }.filter { it.isNotBlank() }.distinct()
        progressByTitle = historyRepository.progressMap(ids)
    }

    Scaffold(
        containerColor = TomiloBg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Закладки") },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        if (!authReady || (loading && user == null)) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        if (user == null) {
            Column(Modifier.padding(padding).fillMaxSize()) {
                ErrorBox("Войдите, чтобы видеть закладки", onRetry = onLogin)
            }
            return@Scaffold
        }
        when {
            loading -> Column(Modifier.padding(padding).fillMaxSize()) {
                BookmarkCategorySelector(catIndex) { catIndex = it }
                LoadingBox()
            }
            error != null && items.isEmpty() -> Column(Modifier.padding(padding).fillMaxSize()) {
                BookmarkCategorySelector(catIndex) { catIndex = it }
                ErrorBox(error ?: "Ошибка") { reload += 1 }
            }
            items.isEmpty() -> Column(Modifier.padding(padding).fillMaxSize()) {
                BookmarkCategorySelector(catIndex) { catIndex = it }
                EmptyState(
                    title = "Здесь пока пусто",
                    message = if (catIndex == 0) {
                        "Добавляйте тайтлы в закладки, чтобы быстро к ним возвращаться."
                    } else {
                        "В категории «${CATEGORIES[catIndex].second}» пока нет тайтлов."
                    },
                    icon = Icons.Outlined.BookmarkBorder,
                )
            }
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = ScreenPadding,
            ) {
                    item(key = "bookmark_filters") {
                        BookmarkCategorySelector(catIndex) { catIndex = it }
                    }
                    items(
                        items,
                        key = { bm ->
                            val id = bm.resolvedTitleId().ifBlank { bm.hashCode().toString() }
                            id + "|" + (bm.category ?: "") + "|" + (bm.addedAt ?: "")
                        },
                    ) { bm ->
                        val titleId = bm.resolvedTitleId()
                        val t = bm.resolvedTitle()
                        val progress = progressByTitle[titleId]
                        val totalFromTitle = t?.totalChapters ?: t?.chaptersCount
                        val progressLine = when {
                            progress != null -> {
                                // если API total=0, подставим total из карточки тайтла
                                val read = progress.chaptersRead
                                val total = progress.totalChapters.takeIf { it > 0 }
                                    ?: totalFromTitle
                                    ?: 0
                                val pct = when {
                                    progress.progressPercent > 0 -> progress.progressPercent
                                    total > 0 -> (100 * read / total).coerceIn(0, 100)
                                    else -> 0
                                }
                                when {
                                    total > 0 -> "Прочитано $read / $total гл." +
                                        if (pct > 0) " · $pct%" else ""
                                    read > 0 -> "Прочитано $read гл."
                                    else -> "Не начато"
                                }
                            }
                            totalFromTitle != null -> "0 / $totalFromTitle гл."
                            else -> null
                        }
                        SwipeActionContainer(
                            actionLabel = "Убрать",
                            actionIcon = Icons.Outlined.DeleteOutline,
                            actionColor = TomiloDanger,
                            enabled = titleId.isNotBlank(),
                            onAction = {
                                val snapshot = items
                                items = items.filterNot { it === bm }
                                progressByTitle = progressByTitle - titleId
                                scope.launch {
                                    socialRepository.removeBookmark(titleId)
                                        .onSuccess { snackbar.showSnackbar("Удалено из закладок") }
                                        .onFailure {
                                            items = snapshot
                                            snackbar.showSnackbar(it.message ?: "Не удалось удалить")
                                        }
                                }
                            },
                        ) {
                            TitleSearchCard(
                                title = bm.displayName(),
                                cover = bm.coverPath(),
                                type = t?.type,
                                rating = t?.averageRating,
                                totalChapters = totalFromTitle,
                                status = t?.status,
                                subtitle = categoryLabel(bm.category),
                                progressLine = progressLine,
                                onClick = {
                                    if (titleId.isNotBlank()) {
                                        onOpenTitle(titleId, t?.slug)
                                    }
                                },
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun BookmarkCategorySelector(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        CATEGORIES.forEachIndexed { index, pair ->
            FilterChip(
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                label = { Text(pair.second) },
                modifier = Modifier.padding(horizontal = 3.dp),
            )
        }
    }
}

private fun categoryLabel(c: String?): String = when (c) {
    "reading" -> "Читаю"
    "planned" -> "В планах"
    "completed" -> "Прочитано"
    "favorites" -> "Избранное"
    "dropped" -> "Брошено"
    else -> c.orEmpty()
}
