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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
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
import ru.tomilo.lib.mobile.core.toUserFacingError
import ru.tomilo.lib.mobile.data.api.BookmarkEntryDto
import ru.tomilo.lib.mobile.data.api.BookmarkGroupDto
import ru.tomilo.lib.mobile.data.api.ReadingProgressDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.HistoryRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.ListCardsSkeleton
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.components.TitleSearchCard
import ru.tomilo.lib.mobile.ui.components.SwipeActionContainer
import ru.tomilo.lib.mobile.ui.components.rememberSwipeRevealCoordinator
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
    var groups by remember { mutableStateOf<List<BookmarkGroupDto>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<BookmarkGroupDto?>(null) }
    var movingBookmark by remember { mutableStateOf<BookmarkEntryDto?>(null) }
    var editingGroup by remember { mutableStateOf<BookmarkGroupDto?>(null) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val reveal = rememberSwipeRevealCoordinator()

    var authReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        authReady = true
    }

    LaunchedEffect(user?.stableId(), catIndex, selectedGroup, reload, authReady) {
        if (!authReady) return@LaunchedEffect
        if (user == null) {
            items = emptyList()
            progressByTitle = emptyMap()
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        groups = socialRepository.bookmarkGroups().getOrDefault(emptyList())
        val cat = selectedGroup?.let { "group:${it.id.removePrefix("group:")}" } ?: CATEGORIES[catIndex].first
        socialRepository.bookmarks(cat)
            .onSuccess { list ->
                items = list.filter {
                    it.resolvedTitleId().isNotBlank() ||
                        it.displayName() != "Тайтл" ||
                        it.coverPath() != null
                }.ifEmpty { list }
            }
            .onFailure { error = it.toUserFacingError("Не удалось загрузить закладки.") }
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
            ListCardsSkeleton(Modifier.padding(padding))
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
                BookmarkCategorySelector(catIndex, selectedGroup != null) { catIndex = it; selectedGroup = null }
                ListCardsSkeleton()
            }
            error != null && items.isEmpty() -> Column(Modifier.padding(padding).fillMaxSize()) {
                BookmarkCategorySelector(catIndex, selectedGroup != null) { catIndex = it; selectedGroup = null }
                ErrorBox(error ?: "Ошибка") { reload += 1 }
            }
            items.isEmpty() -> Column(Modifier.padding(padding).fillMaxSize()) {
                BookmarkCategorySelector(catIndex, selectedGroup != null) { catIndex = it; selectedGroup = null }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                    groups.forEach { group ->
                        FilterChip(selected = selectedGroup?.id == group.id, onClick = { selectedGroup = group }, label = { Text(group.name) }, modifier = Modifier.padding(horizontal = 3.dp))
                    }
                    FilterChip(selected = false, onClick = { groupName = ""; showCreateGroupDialog = true }, label = { Text("Создать группу") }, leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) })
                    if (groups.isNotEmpty()) TextButton(onClick = { showGroupDialog = true }) { Text("Порядок") }
                }
                EmptyState(
                    title = "Здесь пока пусто",
                    message = if (catIndex == 0) {
                        "Добавляйте тайтлы в закладки, чтобы быстро к ним возвращаться."
                    } else {
                        "В категории «${CATEGORIES[catIndex].second}» пока нет тайтлов."
                    },
                    icon = Icons.Outlined.BookmarkBorder,
                    illustration = ru.tomilo.lib.mobile.R.drawable.illust_mascot_guardian,
                )
            }
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = ScreenPadding,
            ) {
                    item(key = "bookmark_filters") {
                        BookmarkCategorySelector(catIndex, selectedGroup != null) { catIndex = it; selectedGroup = null }
                    }
                    item(key = "bookmark_groups") {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                            groups.forEach { group ->
                                FilterChip(
                                    selected = selectedGroup?.id == group.id,
                                    onClick = { selectedGroup = group; catIndex = 0 },
                                    label = { Text(group.name) },
                                    modifier = Modifier.padding(horizontal = 3.dp),
                                )
                            }
                            FilterChip(selected = false, onClick = { editingGroup = null; groupName = ""; showCreateGroupDialog = true }, label = { Text("Создать") }, leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) })
                            TextButton(onClick = { showGroupDialog = true }) { Text("Порядок") }
                        }
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
                            revealKey = titleId,
                            coordinator = reveal,
                            onAction = {
                                val snapshot = items
                                items = items.filterNot { it === bm }
                                progressByTitle = progressByTitle - titleId
                                scope.launch {
                                    socialRepository.removeBookmark(titleId)
                                        .onSuccess { snackbar.showSnackbar("Удалено из закладок") }
                                        .onFailure {
                                            items = snapshot
                                            snackbar.showSnackbar(it.toUserFacingError("Не удалось удалить закладку."))
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
                                subtitle = selectedGroup?.name ?: categoryLabel(bm.category, groups),
                                secondaryActionIcon = Icons.Default.Edit,
                                secondaryActionDescription = "Переместить в группу",
                                onSecondaryAction = { movingBookmark = bm },
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

    if (movingBookmark != null) {
        AlertDialog(
            onDismissRequest = { movingBookmark = null },
            title = { Text("Переместить закладку") },
            text = {
                Column {
                    CATEGORIES.filter { it.first != null }.forEach { (category, label) ->
                        TextButton(onClick = {
                            val bookmark = movingBookmark ?: return@TextButton
                            scope.launch {
                                socialRepository.updateBookmarkCategory(bookmark.resolvedTitleId(), category!!)
                                    .onSuccess { reload += 1; snackbar.showSnackbar("Перемещено в «$label»") }
                                    .onFailure { snackbar.showSnackbar(it.toUserFacingError("Не удалось переместить закладку.")) }
                                movingBookmark = null
                            }
                        }) { Text(label) }
                    }
                    groups.forEach { group ->
                        TextButton(onClick = {
                            val bookmark = movingBookmark ?: return@TextButton
                            scope.launch {
                                socialRepository.updateBookmarkCategory(bookmark.resolvedTitleId(), "group:${group.id.removePrefix("group:")}")
                                    .onSuccess { reload += 1; snackbar.showSnackbar("Перемещено в «${group.name}»") }
                                    .onFailure { snackbar.showSnackbar(it.toUserFacingError("Не удалось переместить закладку.")) }
                                movingBookmark = null
                            }
                        }) { Text(group.name) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { movingBookmark = null }) { Text("Отмена") } },
        )
    }

    if (showGroupDialog) {
        AlertDialog(
            onDismissRequest = { showGroupDialog = false },
            title = { Text(if (editingGroup == null) "Порядок групп" else "Изменить группу") },
            text = {
                Column {
                    if (editingGroup != null) OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("Название") }, singleLine = true)
                    groups.forEachIndexed { index, group ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(group.name, Modifier.weight(1f))
                            IconButton(enabled = index > 0, onClick = {
                                val ordered = groups.toMutableList().also { it.add(index - 1, it.removeAt(index)) }
                                scope.launch {
                                    socialRepository.reorderBookmarkGroups(ordered.map { it.id })
                                        .onSuccess { groups = it; reload += 1 }
                                        .onFailure { snackbar.showSnackbar(it.toUserFacingError("Не удалось изменить порядок групп.")) }
                                }
                            }) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Выше") }
                            IconButton(enabled = index < groups.lastIndex, onClick = {
                                val ordered = groups.toMutableList().also { it.add(index + 1, it.removeAt(index)) }
                                scope.launch {
                                    socialRepository.reorderBookmarkGroups(ordered.map { it.id })
                                        .onSuccess { groups = it; reload += 1 }
                                        .onFailure { snackbar.showSnackbar(it.toUserFacingError("Не удалось изменить порядок групп.")) }
                                }
                            }) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Ниже") }
                            IconButton(onClick = { editingGroup = group; groupName = group.name }) {
                                Icon(Icons.Default.Edit, contentDescription = "Переименовать")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (editingGroup != null) Button(onClick = {
                    val current = editingGroup ?: return@Button
                    scope.launch {
                        socialRepository.renameBookmarkGroup(current.id, groupName).onSuccess { updated ->
                            groups = groups.map { if (it.id == updated.id) updated else it }
                            if (selectedGroup?.id == updated.id) selectedGroup = updated
                            editingGroup = null
                            groupName = ""
                            reload += 1
                        }.onFailure { snackbar.showSnackbar(it.toUserFacingError("Не удалось переименовать группу.")) }
                    }
                }, enabled = groupName.isNotBlank()) { Text("Сохранить") }
                else TextButton(onClick = { showGroupDialog = false }) { Text("Готово") }
            },
            dismissButton = {},
        )
    }

    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Новая группа") },
            text = { OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("Название") }) },
            confirmButton = { Button(onClick = {
                scope.launch {
                    socialRepository.createBookmarkGroup(groupName).onSuccess { groups = groups + it; groupName = ""; showCreateGroupDialog = false }
                        .onFailure { snackbar.showSnackbar(it.toUserFacingError("Не удалось создать группу.")) }
                }
            }, enabled = groupName.isNotBlank()) { Text("Создать") } },
            dismissButton = { TextButton(onClick = { showCreateGroupDialog = false }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun BookmarkCategorySelector(
    selectedIndex: Int,
    customGroupSelected: Boolean = false,
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
                selected = !customGroupSelected && selectedIndex == index,
                onClick = { onSelect(index) },
                label = { Text(pair.second) },
                modifier = Modifier.padding(horizontal = 3.dp),
            )
        }
    }
}

private fun categoryLabel(c: String?, groups: List<BookmarkGroupDto> = emptyList()): String = when (c) {
    "reading" -> "Читаю"
    "planned" -> "В планах"
    "completed" -> "Прочитано"
    "favorites" -> "Избранное"
    "dropped" -> "Брошено"
    else -> c?.let { id -> groups.firstOrNull { it.id == id || it.id == "group:$id" }?.name } ?: c.orEmpty()
}
