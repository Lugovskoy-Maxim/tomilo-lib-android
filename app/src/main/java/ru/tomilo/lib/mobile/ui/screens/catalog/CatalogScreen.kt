package ru.tomilo.lib.mobile.ui.screens.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.ReaderMode
import ru.tomilo.lib.mobile.data.api.CatalogFilterOptionsDto
import ru.tomilo.lib.mobile.data.api.CatalogQuery
import ru.tomilo.lib.mobile.data.api.CatalogTitleDto
import ru.tomilo.lib.mobile.data.local.ContentPrefs
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.ui.components.CatalogGridSkeleton
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.StatusPill
import ru.tomilo.lib.mobile.ui.components.TitlePosterCard
import ru.tomilo.lib.mobile.ui.components.TitleSearchCard
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import ru.tomilo.lib.mobile.ui.theme.TomiloText

private data class SortOption(
    val sortBy: String,
    val sortOrder: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
)

private val SORTS = listOf(
    SortOption("updatedAt", "desc", "Обновления", Icons.Default.Schedule),
    SortOption("createdAt", "desc", "Новые", Icons.Default.AutoAwesome),
    SortOption("views", "desc", "Популярные", Icons.AutoMirrored.Filled.TrendingUp),
    SortOption("averageRating", "desc", "Рейтинг", Icons.Default.Star),
    SortOption("name", "asc", "А–Я", Icons.Default.Sort),
)

private val STATUS_LABELS = mapOf(
    "ongoing" to "Онгоинг",
    "completed" to "Завершено",
    "pause" to "Пауза",
    "hiatus" to "Пауза",
    "cancelled" to "Отменён",
    "announced" to "Анонс",
)

private val FAST_TYPES = listOf(
    null to "Все",
    "manga" to "Манга",
    "manhwa" to "Манхва",
    "manhua" to "Маньхуа",
    "comic" to "Комиксы",
)

private val DEFAULT_TYPES = listOf("manga", "manhwa", "manhua", "comic")
private val DEFAULT_AGES = listOf(0, 12, 16, 18)

enum class CatalogLayoutMode {
    GRID_2,
    GRID_3,
    LIST,
}

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
    val haptics = LocalHapticFeedback.current

    var searchInput by remember { mutableStateOf("") }
    var debouncedSearch by remember { mutableStateOf("") }
    var sortIndex by remember { mutableIntStateOf(0) }
    var selectedTypes by remember { mutableStateOf(setOf<String>()) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var selectedGenres by remember { mutableStateOf(initialGenre?.takeIf { it.isNotBlank() }?.let { setOf(it) } ?: emptySet()) }
    var selectedYears by remember { mutableStateOf(setOf<Int>()) }
    var selectedAges by remember { mutableStateOf(setOf<Int>()) }
    var genreQuery by remember { mutableStateOf("") }
    var includeAdult by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var layoutMode by rememberSaveable { mutableStateOf(CatalogLayoutMode.GRID_2) }

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

    // Infinite scroll
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
                        .onFailure {}
                } catch (_: Throwable) {
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
                title = {
                    Text(
                        "Каталог",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    // Sleek layout switcher segmented control
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(TomiloSurface2)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf(
                            CatalogLayoutMode.GRID_2 to Icons.Default.GridView,
                            CatalogLayoutMode.GRID_3 to Icons.Default.ViewModule,
                            CatalogLayoutMode.LIST to Icons.Default.ViewAgenda,
                        ).forEach { (mode, icon) ->
                            val isSelected = layoutMode == mode
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) TomiloPrimary.copy(alpha = 0.28f) else Color.Transparent)
                                    .clickable {
                                        if (layoutMode != mode) {
                                            layoutMode = mode
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) TomiloPrimary else TomiloMuted,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(6.dp))

                    // Filter button with active count badge
                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(onClick = { showFilters = true }) {
                            Icon(
                                Icons.Outlined.Tune,
                                contentDescription = "Фильтры",
                                tint = if (activeFilters > 0) TomiloPrimary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        if (activeFilters > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp, end = 6.dp)
                                    .size(17.dp)
                                    .clip(CircleShape)
                                    .background(TomiloPrimary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "$activeFilters",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        val gridColumns = when (layoutMode) {
            CatalogLayoutMode.GRID_2 -> GridCells.Fixed(2)
            CatalogLayoutMode.GRID_3 -> GridCells.Fixed(3)
            CatalogLayoutMode.LIST -> GridCells.Fixed(1)
        }

        LazyVerticalGrid(
            columns = gridColumns,
            state = gridState,
            contentPadding = PaddingValues(
                start = 14.dp,
                top = 6.dp,
                end = 14.dp,
                bottom = 100.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(
                if (layoutMode == CatalogLayoutMode.GRID_3) 6.dp else 10.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(
                if (layoutMode == CatalogLayoutMode.LIST) 10.dp else 12.dp,
            ),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "catalog_controls") {
                Column(Modifier.fillMaxWidth()) {
                    // Google-style modern search pill
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        placeholder = {
                            Text(
                                "Поиск по названию или автору…",
                                color = TomiloMuted,
                                fontSize = 14.sp,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = TomiloMuted,
                            )
                        },
                        trailingIcon = {
                            if (searchInput.isNotEmpty()) {
                                IconButton(onClick = { searchInput = "" }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Очистить",
                                        tint = TomiloMuted,
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TomiloSurface,
                            unfocusedContainerColor = TomiloSurface,
                            focusedBorderColor = TomiloPrimary.copy(alpha = 0.65f),
                            unfocusedBorderColor = TomiloBorder,
                        ),
                    )

                    // Fast 1-tap category pills (Все / Манга / Манхва / Маньхуа / Комиксы)
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FAST_TYPES.forEach { (typeKey, label) ->
                            val isSelected = if (typeKey == null) selectedTypes.isEmpty() else typeKey in selectedTypes
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTypes = if (typeKey == null) {
                                        emptySet()
                                    } else {
                                        if (typeKey in selectedTypes) selectedTypes - typeKey else setOf(typeKey)
                                    }
                                },
                                label = {
                                    Text(
                                        label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TomiloPrimary.copy(alpha = 0.22f),
                                    selectedLabelColor = TomiloPrimary,
                                ),
                            )
                        }
                    }

                    // Sort pills row
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 2.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SORTS.forEachIndexed { i, s ->
                            val isSortSelected = sortIndex == i
                            FilterChip(
                                selected = isSortSelected,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    sortIndex = i
                                },
                                leadingIcon = s.icon?.let { icon ->
                                    {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (isSortSelected) Color.White else TomiloMuted,
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        s.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSortSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TomiloSurface2,
                                    selectedLabelColor = Color.White,
                                ),
                            )
                        }
                    }

                    // Status counter & active filter chip summary
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (total > 0) "• $total тайтлов" else "Подбираем тайтлы…",
                            color = TomiloMuted,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                        )
                        if (activeFilters > 0) {
                            StatusPill(
                                text = "$activeFilters фильтра",
                                color = TomiloPrimary,
                            )
                        }
                    }

                    // Active removable chips row
                    if (activeFilters > 0) {
                        Row(
                            Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            selectedTypes.forEach { type ->
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedTypes = selectedTypes - type },
                                    label = { Text(ReaderMode.typeLabel(type), fontSize = 11.sp) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(12.dp)) },
                                    shape = RoundedCornerShape(10.dp),
                                )
                            }
                            selectedStatus?.let { st ->
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedStatus = null },
                                    label = {
                                        Text(
                                            STATUS_LABELS[st] ?: ru.tomilo.lib.mobile.core.GenreLabels.status(st),
                                            fontSize = 11.sp,
                                        )
                                    },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(12.dp)) },
                                    shape = RoundedCornerShape(10.dp),
                                )
                            }
                            selectedGenres.forEach { genre ->
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedGenres = selectedGenres - genre },
                                    label = { Text(ru.tomilo.lib.mobile.core.GenreLabels.ru(genre), fontSize = 11.sp) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(12.dp)) },
                                    shape = RoundedCornerShape(10.dp),
                                )
                            }
                            selectedYears.forEach { year ->
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedYears = selectedYears - year },
                                    label = { Text("$year", fontSize = 11.sp) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(12.dp)) },
                                    shape = RoundedCornerShape(10.dp),
                                )
                            }
                            selectedAges.forEach { age ->
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedAges = selectedAges - age },
                                    label = { Text(if (age == 0) "0+" else "$age+", fontSize = 11.sp) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(12.dp)) },
                                    shape = RoundedCornerShape(10.dp),
                                )
                            }
                            if (includeAdult) {
                                FilterChip(
                                    selected = true,
                                    onClick = {
                                        includeAdult = false
                                        scope.launch { contentPrefs.setShowAdult(false) }
                                    },
                                    label = { Text("18+", fontSize = 11.sp) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(12.dp)) },
                                    shape = RoundedCornerShape(10.dp),
                                )
                            }
                            TextButton(onClick = { clearFilters() }) {
                                Text("Сбросить всё", fontSize = 12.sp, color = TomiloPrimary)
                            }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(390.dp),
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
                        when (layoutMode) {
                            CatalogLayoutMode.GRID_2 -> {
                                TitlePosterCard(
                                    title = item.displayTitle(),
                                    cover = item.coverPath(),
                                    onClick = { onOpenTitle(item.stableId(), item.slug) },
                                    modifier = Modifier.fillMaxWidth(),
                                    width = null,
                                    type = item.type,
                                    rating = item.displayRating(),
                                    totalChapters = item.totalChapters,
                                    chapterBadge = item.chapterBadge(),
                                    status = item.status,
                                    isAdult = item.isAdult == true,
                                    year = item.releaseYear,
                                    compact = false,
                                )
                            }
                            CatalogLayoutMode.GRID_3 -> {
                                TitlePosterCard(
                                    title = item.displayTitle(),
                                    cover = item.coverPath(),
                                    onClick = { onOpenTitle(item.stableId(), item.slug) },
                                    modifier = Modifier.fillMaxWidth(),
                                    width = null,
                                    type = item.type,
                                    rating = item.displayRating(),
                                    totalChapters = item.totalChapters,
                                    chapterBadge = item.chapterBadge(),
                                    status = item.status,
                                    isAdult = item.isAdult == true,
                                    year = item.releaseYear,
                                    compact = true,
                                )
                            }
                            CatalogLayoutMode.LIST -> {
                                TitleSearchCard(
                                    title = item.displayTitle(),
                                    cover = item.coverPath(),
                                    onClick = { onOpenTitle(item.stableId(), item.slug) },
                                    modifier = Modifier.fillMaxWidth(),
                                    type = item.type,
                                    status = item.status,
                                    rating = item.displayRating(),
                                    year = item.releaseYear,
                                    totalChapters = item.totalChapters,
                                )
                            }
                        }
                    }

                    if (loadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "more_loading") {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Загружаем страницу ${page + 1}" +
                                        if (totalPages > 0) " из $totalPages" else "",
                                    color = TomiloMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    } else if (page >= totalPages && items.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "end") {
                            Text(
                                "Все $total тайтлов показаны",
                                color = TomiloMuted,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Filter Sheet
    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = TomiloBg,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
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
                    .padding(horizontal = 18.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        "Фильтры каталога",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    if (activeFilters > 0) {
                        TextButton(onClick = { clearFilters() }) {
                            Text("Сбросить всё", color = TomiloPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Text(
                    if (activeFilters > 0) "Активно фильтров: $activeFilters" else "Выберите параметры для точного поиска",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(14.dp))

                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    FilterSectionTitle("Тип тайтла")
                    WrapChips(
                        options = typeOptions,
                        selected = selectedTypes,
                        label = { ReaderMode.typeLabel(it) },
                        onToggle = { t ->
                            selectedTypes = if (t in selectedTypes) selectedTypes - t else selectedTypes + t
                        },
                    )

                    Spacer(Modifier.height(18.dp))
                    FilterSectionTitle("Статус перевода")
                    WrapChips(
                        options = listOf("__any") + statusOptions,
                        selected = if (selectedStatus == null) setOf("__any") else setOf(selectedStatus!!),
                        label = {
                            if (it == "__any") "Любой статус"
                            else STATUS_LABELS[it] ?: ru.tomilo.lib.mobile.core.GenreLabels.status(it)
                        },
                        onToggle = { st ->
                            selectedStatus = if (st == "__any" || selectedStatus == st) null else st
                        },
                    )

                    if (options.genres.isNotEmpty()) {
                        Spacer(Modifier.height(18.dp))
                        FilterSectionTitle(
                            "Жанры" + if (selectedGenres.isNotEmpty()) " · ${selectedGenres.size}" else "",
                        )
                        OutlinedTextField(
                            value = genreQuery,
                            onValueChange = { genreQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            placeholder = { Text("Быстрый поиск жанра…", color = TomiloMuted) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TomiloMuted) },
                            trailingIcon = {
                                if (genreQuery.isNotEmpty()) {
                                    IconButton(onClick = { genreQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Очистить", tint = TomiloMuted)
                                    }
                                }
                            },
                        )
                        if (visibleGenres.isEmpty()) {
                            Text(
                                "Жанры не найдены",
                                color = TomiloMuted,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
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

                    Spacer(Modifier.height(18.dp))
                    FilterSectionTitle("Год выпуска")
                    WrapChips(
                        options = yearOptions.map { it.toString() },
                        selected = selectedYears.map { it.toString() }.toSet(),
                        label = { it },
                        onToggle = { raw ->
                            val year = raw.toIntOrNull() ?: return@WrapChips
                            selectedYears = if (year in selectedYears) selectedYears - year else selectedYears + year
                        },
                    )

                    Spacer(Modifier.height(18.dp))
                    FilterSectionTitle("Возрастной рейтинг")
                    WrapChips(
                        options = ageOptions.map { it.toString() },
                        selected = selectedAges.map { it.toString() }.toSet(),
                        label = { age -> if (age == "0") "0+" else "$age+" },
                        onToggle = { raw ->
                            val age = raw.toIntOrNull() ?: return@WrapChips
                            selectedAges = if (age in selectedAges) selectedAges - age else selectedAges + age
                        },
                    )

                    Spacer(Modifier.height(18.dp))
                    FilterSectionTitle("Контент для взрослых (18+)")
                    if (canShowAdult) {
                        FilterChip(
                            selected = includeAdult,
                            onClick = {
                                val next = !includeAdult
                                includeAdult = next
                                scope.launch { contentPrefs.setShowAdult(next) }
                            },
                            label = { Text(if (includeAdult) "✓ 18+ включен" else "18+ скрыт") },
                            shape = RoundedCornerShape(12.dp),
                        )
                    } else {
                        Text(
                            "Для просмотра 18+ подтвердите возраст в настройках",
                            color = TomiloMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // Sticky bottom action buttons
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { clearFilters() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("Сбросить")
                    }
                    Button(
                        onClick = { showFilters = false },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            if (activeFilters > 0) "Применить ($activeFilters)" else "Показать тайтлы",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
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
            val isSelected = opt in selected
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(opt) },
                label = {
                    Text(
                        label(opt),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                shape = RoundedCornerShape(12.dp),
            )
        }
    }
}
