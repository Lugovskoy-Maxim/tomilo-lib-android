package ru.tomilo.lib.mobile.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import ru.tomilo.lib.mobile.core.ReaderMode
import ru.tomilo.lib.mobile.data.api.CatalogFilterOptionsDto
import ru.tomilo.lib.mobile.data.api.CatalogQuery
import ru.tomilo.lib.mobile.data.api.CatalogTitleDto
import ru.tomilo.lib.mobile.data.local.ContentPrefs
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.CatalogGridSkeleton
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.components.StatusPill
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

private data class SortOption(val sortBy: String, val sortOrder: String, val label: String)

private val SORTS = listOf(
    SortOption("updatedAt", "desc", "Обновления"),
    SortOption("createdAt", "desc", "Новые"),
    SortOption("views", "desc", "Просмотры"),
    SortOption("weekViews", "desc", "За неделю"),
    SortOption("averageRating", "desc", "Рейтинг"),
    SortOption("name", "asc", "А–Я"),
)

private val STATUS_LABELS = mapOf(
    "ongoing" to "Онгоинг",
    "completed" to "Завершён",
    "pause" to "Пауза",
    "hiatus" to "Пауза",
    "cancelled" to "Отменён",
    "announced" to "Анонс",
)

private val DEFAULT_TYPES = listOf("manga", "manhwa", "manhua", "comic")
private val DEFAULT_AGES = listOf(0, 12, 16, 18)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, FlowPreview::class)
@Composable
fun CatalogScreen(
    catalogRepository: CatalogRepository,
    contentPrefs: ContentPrefs,
    initialGenre: String? = null,
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
    var selectedGenres by remember { mutableStateOf(initialGenre?.takeIf { it.isNotBlank() }?.let { setOf(it) } ?: emptySet()) }
    var selectedYears by remember { mutableStateOf(setOf<Int>()) }
    var selectedAges by remember { mutableStateOf(setOf<Int>()) }
    var genreQuery by remember { mutableStateOf("") }
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

    LaunchedEffect(initialGenre) {
        val genre = initialGenre?.trim().orEmpty()
        if (genre.isNotBlank() && genre !in selectedGenres) {
            selectedGenres = setOf(genre)
        }
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
            releaseYears = selectedYears.takeIf { it.isNotEmpty() }?.sortedDescending()?.joinToString(","),
            ageLimits = selectedAges.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(","),
            includeAdult = includeAdult,
        )
    }

    LaunchedEffect(
        debouncedSearch,
        sortIndex,
        selectedTypes,
        selectedStatus,
        selectedGenres,
        selectedYears,
        selectedAges,
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

    val activeFilters = selectedTypes.size + selectedGenres.size + selectedYears.size +
        selectedAges.size + (if (selectedStatus != null) 1 else 0) + (if (includeAdult) 1 else 0)

    fun clearFilters() {
        selectedTypes = emptySet()
        selectedGenres = emptySet()
        selectedYears = emptySet()
        selectedAges = emptySet()
        selectedStatus = null
        genreQuery = ""
        includeAdult = false
        scope.launch { contentPrefs.setShowAdult(false) }
    }

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
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            state = gridState,
            contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "catalog_controls") {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        placeholder = { Text("Название, автор…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchInput.isNotEmpty()) {
                                IconButton(onClick = { searchInput = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Очистить")
                                }
                            }
                        },
                    )

                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp, bottom = 2.dp)) {
                        FilterChip(
                            selected = activeFilters > 0,
                            onClick = { showFilters = true },
                            label = { Text(if (activeFilters > 0) "Фильтры · $activeFilters" else "Все фильтры") },
                            leadingIcon = {
                                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.height(17.dp))
                            },
                            modifier = Modifier.padding(horizontal = 3.dp),
                        )
                        SORTS.forEachIndexed { i, s ->
                            FilterChip(
                                selected = sortIndex == i,
                                onClick = { sortIndex = i },
                                label = { Text(s.label) },
                                modifier = Modifier.padding(horizontal = 3.dp),
                            )
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (total > 0) "$total тайтлов" else "Подбираем тайтлы", color = TomiloMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        if (activeFilters > 0) StatusPill("$activeFilters фильтр.")
                    }

                    if (activeFilters > 0) {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            selectedTypes.forEach { type ->
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedTypes = selectedTypes - type },
                                    label = { Text(ReaderMode.typeLabel(type)) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.height(14.dp)) },
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                            }
                            selectedStatus?.let { st ->
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedStatus = null },
                                    label = { Text(STATUS_LABELS[st] ?: ru.tomilo.lib.mobile.core.GenreLabels.status(st)) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.height(14.dp)) },
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                            }
                            selectedGenres.forEach { genre ->
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedGenres = selectedGenres - genre },
                                    label = { Text(ru.tomilo.lib.mobile.core.GenreLabels.ru(genre)) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.height(14.dp)) },
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                            }
                            selectedYears.forEach { year ->
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedYears = selectedYears - year },
                                    label = { Text("$year") },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.height(14.dp)) },
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                            }
                            selectedAges.forEach { age ->
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedAges = selectedAges - age },
                                    label = { Text(if (age == 0) "0+" else "$age+") },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.height(14.dp)) },
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                            }
                            if (includeAdult) {
                                FilterChip(
                                    selected = true,
                                    onClick = {
                                        includeAdult = false
                                        scope.launch { contentPrefs.setShowAdult(false) }
                                    },
                                    label = { Text("18+") },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.height(14.dp)) },
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                            }
                            TextButton(onClick = { clearFilters() }) { Text("Сбросить") }
                        }
                    }
                }
            }

            when {
                loading && items.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }, key = "catalog_loading") {
                    CatalogGridSkeleton(Modifier.fillMaxWidth())
                }
                error != null && items.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }, key = "catalog_error") {
                    ErrorBox(error ?: "Ошибка", modifier = Modifier.fillMaxWidth().height(390.dp)) { reload += 1 }
                }
                items.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }, key = "catalog_empty") {
                    EmptyState(
                        title = "Ничего не найдено",
                        message = "Попробуйте изменить запрос или сбросить выбранные фильтры.",
                        icon = Icons.Outlined.SearchOff,
                        modifier = Modifier.fillMaxWidth().height(390.dp),
                        actionLabel = if (activeFilters > 0 || searchInput.isNotBlank()) "Сбросить фильтры" else null,
                        onAction = if (activeFilters > 0 || searchInput.isNotBlank()) {
                            {
                                searchInput = ""
                                sortIndex = 0
                                clearFilters()
                            }
                        } else null,
                    )
                }
                else -> {
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
            val typeOptions = options.types.ifEmpty { DEFAULT_TYPES }
            val statusOptions = options.status.ifEmpty { STATUS_LABELS.keys.toList() }
            val yearOptions = options.releaseYears.ifEmpty {
                (2026 downTo 2005).toList()
            }.sortedDescending()
            val ageOptions = options.ageLimits.ifEmpty { DEFAULT_AGES }.sorted()
            val visibleGenres = options.genres.filter {
                val ru = ru.tomilo.lib.mobile.core.GenreLabels.ru(it)
                genreQuery.isBlank() ||
                    it.contains(genreQuery, ignoreCase = true) ||
                    ru.contains(genreQuery, ignoreCase = true)
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Фильтры", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    if (activeFilters > 0) {
                        TextButton(onClick = { clearFilters() }) { Text("Сбросить всё") }
                    }
                }
                Text(
                    if (activeFilters > 0) "Выбрано: $activeFilters" else "Тип, статус, жанр, год и возраст",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text("Тип", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    WrapChips(
                        options = typeOptions,
                        selected = selectedTypes,
                        label = { ReaderMode.typeLabel(it) },
                        onToggle = { t ->
                            selectedTypes = if (t in selectedTypes) selectedTypes - t else selectedTypes + t
                        },
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Статус", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    WrapChips(
                        options = listOf("__any") + statusOptions,
                        selected = if (selectedStatus == null) setOf("__any") else setOf(selectedStatus!!),
                        label = {
                            if (it == "__any") "Любой"
                            else STATUS_LABELS[it] ?: ru.tomilo.lib.mobile.core.GenreLabels.status(it)
                        },
                        onToggle = { st ->
                            selectedStatus = if (st == "__any" || selectedStatus == st) null else st
                        },
                    )

                    if (options.genres.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Жанры" + if (selectedGenres.isNotEmpty()) " · ${selectedGenres.size}" else "",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = genreQuery,
                            onValueChange = { genreQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            placeholder = { Text("Найти жанр") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        )
                        Spacer(Modifier.height(8.dp))
                        if (visibleGenres.isEmpty()) {
                            Text("Нет такого жанра", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                        } else {
                            WrapChips(
                                options = visibleGenres,
                                selected = selectedGenres,
                                label = { ru.tomilo.lib.mobile.core.GenreLabels.ru(it) },
                                onToggle = { g ->
                                    selectedGenres = if (g in selectedGenres) selectedGenres - g else selectedGenres + g
                                },
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Год выпуска", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    WrapChips(
                        options = yearOptions.map { it.toString() },
                        selected = selectedYears.map { it.toString() }.toSet(),
                        label = { it },
                        onToggle = { raw ->
                            val year = raw.toIntOrNull() ?: return@WrapChips
                            selectedYears = if (year in selectedYears) selectedYears - year else selectedYears + year
                        },
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Возраст", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    WrapChips(
                        options = ageOptions.map { it.toString() },
                        selected = selectedAges.map { it.toString() }.toSet(),
                        label = { age -> if (age == "0") "0+" else "$age+" },
                        onToggle = { raw ->
                            val age = raw.toIntOrNull() ?: return@WrapChips
                            selectedAges = if (age in selectedAges) selectedAges - age else selectedAges + age
                        },
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Контент 18+", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (canShowAdult) {
                        FilterChip(
                            selected = includeAdult,
                            onClick = {
                                val next = !includeAdult
                                includeAdult = next
                                scope.launch { contentPrefs.setShowAdult(next) }
                            },
                            label = { Text(if (includeAdult) "Показывать 18+" else "Скрывать 18+") },
                        )
                    } else {
                        Text(
                            "Недоступно: возраст не подтверждён",
                            color = TomiloMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }
                Row(
                    Modifier.padding(top = 10.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { clearFilters() },
                        modifier = Modifier.weight(1f),
                    ) { Text("Сбросить") }
                    Button(
                        onClick = { showFilters = false },
                        modifier = Modifier.weight(1f),
                    ) { Text(if (activeFilters > 0) "Показать · $activeFilters" else "Готово") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WrapChips(
    options: List<String>,
    selected: Set<String>,
    label: (String) -> String,
    onToggle: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { opt ->
            FilterChip(
                selected = opt in selected,
                onClick = { onToggle(opt) },
                label = { Text(label(opt)) },
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
