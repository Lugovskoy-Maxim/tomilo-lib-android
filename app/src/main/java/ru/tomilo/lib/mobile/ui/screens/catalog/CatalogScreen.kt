package ru.tomilo.lib.mobile.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.api.CatalogFilterOptionsDto
import ru.tomilo.lib.mobile.data.api.CatalogQuery
import ru.tomilo.lib.mobile.data.api.CatalogTitleDto
import ru.tomilo.lib.mobile.data.local.ContentPrefs
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

private data class SortOption(val sortBy: String, val sortOrder: String, val label: String)

private val SORTS = listOf(
    SortOption("updatedAt", "desc", "Обновления"),
    SortOption("createdAt", "desc", "Новые"),
    SortOption("views", "desc", "Просмотры"),
    SortOption("weekViews", "desc", "За неделю"),
    SortOption("name", "asc", "А–Я"),
)

private val STATUS_LABELS = mapOf(
    "ongoing" to "Онгоинг",
    "completed" to "Завершён",
    "pause" to "Пауза",
    "cancelled" to "Отменён",
)

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun CatalogScreen(
    catalogRepository: CatalogRepository,
    contentPrefs: ContentPrefs,
    onOpenTitle: (id: String, slug: String?) -> Unit,
) {
    val contentSettings by contentPrefs.settingsFlow.collectAsState(
        initial = ru.tomilo.lib.mobile.data.local.ContentSettings(),
    )
    val scope = rememberCoroutineScope()
    var searchInput by remember { mutableStateOf("") }
    var debouncedSearch by remember { mutableStateOf("") }
    var sortIndex by remember { mutableIntStateOf(0) }
    var selectedTypes by remember { mutableStateOf(setOf<String>()) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var selectedGenres by remember { mutableStateOf(setOf<String>()) }
    // Глобальная настройка 18+; локальный чип синхронизирован
    var includeAdult by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }

    var options by remember { mutableStateOf(CatalogFilterOptionsDto()) }
    var items by remember { mutableStateOf<List<CatalogTitleDto>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var total by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    val gridState = rememberLazyGridState()
    val canShowAdult = contentSettings.isAdultUser == true

    LaunchedEffect(contentSettings.showAdultContent) {
        includeAdult = contentSettings.showAdultContent && canShowAdult
    }

    LaunchedEffect(Unit) {
        catalogRepository.filterOptions()
            .onSuccess { options = it }
    }

    LaunchedEffect(searchInput) {
        snapshotFlow { searchInput }
            .debounce(350)
            .distinctUntilChanged()
            .collect { debouncedSearch = it.trim() }
    }

    fun buildQuery(pageNum: Int): CatalogQuery {
        val sort = SORTS[sortIndex.coerceIn(0, SORTS.lastIndex)]
        return CatalogQuery(
            page = pageNum,
            limit = 24,
            search = debouncedSearch.ifBlank { null },
            genres = selectedGenres.takeIf { it.isNotEmpty() }?.joinToString(","),
            types = selectedTypes.takeIf { it.isNotEmpty() }?.joinToString(","),
            status = selectedStatus,
            sortBy = sort.sortBy,
            sortOrder = sort.sortOrder,
            includeAdult = includeAdult,
        )
    }

    LaunchedEffect(
        debouncedSearch,
        sortIndex,
        selectedTypes,
        selectedStatus,
        selectedGenres,
        includeAdult,
        reload,
    ) {
        loading = true
        loadingMore = false
        error = null
        page = 1
        catalogRepository.catalog(buildQuery(1))
            .onSuccess { data ->
                // unique keys — иначе crash LazyGrid
                items = data.titles.distinctBy { it.stableId().ifBlank { it.slug.orEmpty() } }
                totalPages = data.pagination?.pages?.coerceAtLeast(1) ?: 1
                total = data.pagination?.total ?: data.titles.size
            }
            .onFailure {
                error = it.message
                items = emptyList()
            }
        loading = false
        runCatching { gridState.scrollToItem(0) }
    }

    // infinite scroll — один collector, без перезапуска на каждый page
    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = info.totalItemsCount
            Triple(last, totalItems, loading || loadingMore)
        }
            .filter { (last, totalItems, busy) ->
                !busy && totalItems > 0 && last >= totalItems - 6
            }
            .collect {
                // повторно проверяем актуальные page/totalPages (не из closure keys)
                if (loading || loadingMore) return@collect
                if (page >= totalPages) return@collect
                loadingMore = true
                val next = page + 1
                try {
                    catalogRepository.catalog(buildQuery(next))
                        .onSuccess { data ->
                            val existing = items.map {
                                it.stableId().ifBlank { it.slug.orEmpty() }
                            }.filter { it.isNotBlank() }.toHashSet()
                            val merged = items + data.titles.filter { t ->
                                val key = t.stableId().ifBlank { t.slug.orEmpty() }
                                key.isNotBlank() && key !in existing
                            }
                            items = merged
                            page = next
                            totalPages = data.pagination?.pages?.coerceAtLeast(1) ?: totalPages
                            total = data.pagination?.total ?: total
                        }
                        .onFailure {
                            // не роняем UI — просто не листаем дальше в этот раз
                        }
                } catch (_: Throwable) {
                    // защита от OOM/сетевых сбоев при догрузке
                } finally {
                    loadingMore = false
                }
            }
    }

    val activeFilters = selectedTypes.size + selectedGenres.size +
        (if (selectedStatus != null) 1 else 0) + (if (includeAdult) 1 else 0)

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = { Text("Каталог") },
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Фильтры",
                            tint = if (activeFilters > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TomiloBg),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            OutlinedTextField(
                value = searchInput,
                onValueChange = { searchInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text("Название, автор…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )

            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
            ) {
                SORTS.forEachIndexed { i, s ->
                    FilterChip(
                        selected = sortIndex == i,
                        onClick = { sortIndex = i },
                        label = { Text(s.label) },
                        modifier = Modifier.padding(horizontal = 3.dp),
                    )
                }
            }

            Text(
                if (total > 0) "Найдено: $total" else " ",
                color = TomiloMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )

            when {
                loading && items.isEmpty() -> LoadingBox(
                    message = "Загружаем каталог…",
                )
                error != null && items.isEmpty() -> ErrorBox(error ?: "Ошибка") { reload += 1 }
                items.isEmpty() -> Text(
                    "Ничего не найдено",
                    color = TomiloMuted,
                    modifier = Modifier.padding(16.dp),
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    state = gridState,
                    contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = items,
                        key = { item ->
                            item.stableId().ifBlank {
                                "${item.slug.orEmpty()}_${item.displayTitle()}_${item.hashCode()}"
                            }
                        },
                    ) { item ->
                        CatalogCard(item) {
                            onOpenTitle(item.stableId(), item.slug)
                        }
                    }
                    if (loadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "loading_more") {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CircularProgressIndicator(strokeWidth = 2.dp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Подгружаем ещё… стр. ${page + 1}" +
                                        if (totalPages > 0) " / $totalPages" else "",
                                    color = TomiloMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    } else if (page >= totalPages && items.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "end") {
                            Text(
                                "Все $total тайтлов загружены",
                                color = TomiloMuted,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = TomiloBg,
        ) {
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
            ) {
                Text("Фильтры", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))

                Text("Тип", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                FlowChips(
                    options = options.types.ifEmpty { listOf("manga", "manhwa", "manhua", "comic") },
                    selected = selectedTypes,
                    label = { it },
                    onToggle = { t ->
                        selectedTypes = if (t in selectedTypes) selectedTypes - t else selectedTypes + t
                    },
                )

                Spacer(Modifier.height(12.dp))
                Text("Статус", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { selectedStatus = null },
                        label = { Text("Любой") },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    (options.status.ifEmpty { STATUS_LABELS.keys.toList() }).forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = {
                                selectedStatus = if (selectedStatus == st) null else st
                            },
                            label = { Text(STATUS_LABELS[st] ?: st) },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }

                if (options.genres.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Жанры", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    FlowChips(
                        options = options.genres.take(40),
                        selected = selectedGenres,
                        label = { it },
                        onToggle = { g ->
                            selectedGenres =
                                if (g in selectedGenres) selectedGenres - g else selectedGenres + g
                        },
                    )
                }

                Spacer(Modifier.height(12.dp))
                if (canShowAdult) {
                    FilterChip(
                        selected = includeAdult,
                        onClick = {
                            val next = !includeAdult
                            includeAdult = next
                            scope.launch { contentPrefs.setShowAdult(next) }
                        },
                        label = { Text(if (includeAdult) "18+ вкл" else "18+ выкл") },
                    )
                } else {
                    Text(
                        "Контент 18+ недоступен (возраст не подтверждён)",
                        color = TomiloMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            selectedTypes = emptySet()
                            selectedGenres = emptySet()
                            selectedStatus = null
                            includeAdult = false
                            scope.launch { contentPrefs.setShowAdult(false) }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Сбросить") }
                    Button(
                        onClick = { showFilters = false },
                        modifier = Modifier.weight(1f),
                    ) { Text("Готово") }
                }
            }
        }
    }
}

@Composable
private fun FlowChips(
    options: List<String>,
    selected: Set<String>,
    label: (String) -> String,
    onToggle: (String) -> Unit,
) {
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        options.forEach { opt ->
            FilterChip(
                selected = opt in selected,
                onClick = { onToggle(opt) },
                label = { Text(label(opt)) },
                modifier = Modifier.padding(end = 4.dp),
            )
        }
    }
}

@Composable
private fun CatalogCard(
    item: CatalogTitleDto,
    onClick: () -> Unit,
) {
    ru.tomilo.lib.mobile.ui.components.TitlePosterCard(
        title = item.displayTitle(),
        cover = item.coverPath(),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        width = null,
        type = item.type,
        rating = item.displayRating(),
        totalChapters = item.totalChapters,
        status = item.status,
        isAdult = item.isAdult == true,
        year = item.releaseYear,
    )
}
