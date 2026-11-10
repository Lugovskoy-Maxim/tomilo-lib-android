package ru.tomilo.lib.mobile.ui.screens.top

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import ru.tomilo.lib.mobile.core.toUserFacingError
import ru.tomilo.lib.mobile.data.api.CatalogTitleDto
import ru.tomilo.lib.mobile.data.local.ContentPrefs
import ru.tomilo.lib.mobile.data.local.ContentSettings
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.ListCardsSkeleton
import ru.tomilo.lib.mobile.ui.components.PageIntro
import ru.tomilo.lib.mobile.ui.components.TitlePosterCard
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg

private data class TopPeriod(val key: String, val label: String)

private val PERIODS = listOf(
    TopPeriod("day", "День"),
    TopPeriod("week", "Неделя"),
    TopPeriod("month", "Месяц"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopTitlesScreen(
    catalogRepository: CatalogRepository,
    contentPrefs: ContentPrefs,
    onBack: () -> Unit,
    onOpenTitle: (String, String?) -> Unit,
) {
    val settings by contentPrefs.settingsFlow.collectAsState(initial = ContentSettings())
    val includeAdult = settings.showAdultContent && settings.isAdultUser == true
    var periodIndex by remember { mutableIntStateOf(1) }
    var items by remember { mutableStateOf<List<CatalogTitleDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    val period = PERIODS[periodIndex.coerceIn(0, PERIODS.lastIndex)]

    LaunchedEffect(period.key, includeAdult, reload) {
        loading = true
        error = null
        try {
            catalogRepository.topTitles(period.key, includeAdult = includeAdult)
                .onSuccess { list ->
                    items = if (includeAdult) list else list.filter { it.isAdult != true }
                }
                .onFailure { error = it.toUserFacingError("Не удалось загрузить топ.") }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            error = failure.toUserFacingError("Не удалось загрузить топ.")
        } finally {
            loading = false
        }
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Топ тайтлов")
                        Text(
                            "По просмотрам · ${period.label.lowercase()}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { reload++ }, enabled = !loading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = periodIndex,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                PERIODS.forEachIndexed { index, item ->
                    Tab(
                        selected = periodIndex == index,
                        onClick = { periodIndex = index },
                        text = { Text(item.label) },
                    )
                }
            }
            when {
                loading && items.isEmpty() -> {
                    ListCardsSkeleton(Modifier.fillMaxSize())
                }
                error != null && items.isEmpty() -> {
                    ErrorBox(error.orEmpty(), Modifier.fillMaxSize()) { reload++ }
                }
                items.isEmpty() -> {
                    EmptyState(
                        title = "Пока пусто",
                        message = "В этом периоде ещё нет данных топа",
                        icon = Icons.Outlined.EmojiEvents,
                        actionLabel = "Обновить",
                        onAction = { reload++ },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            PageIntro(
                                title = "Топ ${period.label.lowercase()}",
                                subtitle = "Как на сайте tomilo-lib.ru/top",
                                icon = Icons.Outlined.EmojiEvents,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        itemsIndexed(
                            items,
                            key = { _, item -> item.stableId().ifBlank { item.displayTitle() } },
                        ) { index, item ->
                            TitlePosterCard(
                                title = item.displayTitle(),
                                cover = item.coverPath(),
                                onClick = { onOpenTitle(item.stableId(), item.slug) },
                                width = null,
                                type = item.type,
                                rating = item.displayRating(),
                                year = item.releaseYear,
                                status = item.status,
                                isAdult = item.isAdult == true,
                                rank = index + 1,
                                subtitle = item.views?.takeIf { it > 0 }?.let { "$it просм." },
                            )
                        }
                    }
                }
            }
        }
    }
}
