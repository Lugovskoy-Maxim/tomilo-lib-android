package ru.tomilo.lib.mobile.ui.screens.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import ru.tomilo.lib.mobile.data.api.CatalogTitleDto
import ru.tomilo.lib.mobile.data.local.ContentPrefs
import ru.tomilo.lib.mobile.data.local.ContentSettings
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.ListCardsSkeleton
import ru.tomilo.lib.mobile.ui.components.LoadingMoreBar
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.components.TitleSearchCard
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.components.PageIntro
import ru.tomilo.lib.mobile.ui.components.StatusPill
import ru.tomilo.lib.mobile.ui.theme.TomiloBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(
    catalogRepository: CatalogRepository,
    contentPrefs: ContentPrefs,
    onBack: () -> Unit,
    onOpenTitle: (String, String?) -> Unit,
) {
    val settings by contentPrefs.settingsFlow.collectAsState(initial = ContentSettings())
    val listState = rememberLazyListState()
    var items by remember { mutableStateOf<List<CatalogTitleDto>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    fun filterAdult(list: List<CatalogTitleDto>) =
        if (settings.showAdultContent) list else list.filter { it.isAdult != true }

    LaunchedEffect(reload, settings.showAdultContent) {
        loading = true
        error = null
        page = 1
        hasMore = true
        catalogRepository.latestUpdatesPage(1, 24)
            .onSuccess {
                items = filterAdult(it).distinctBy { title -> title.stableId() }
                hasMore = it.size >= 24
            }
            .onFailure { error = it.message }
        loading = false
    }

    LaunchedEffect(listState, page, hasMore, loading) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }.distinctUntilChanged()
            .filter { (last, total) -> total > 0 && last >= total - 5 }
            .collect {
                if (loading || loadingMore || !hasMore) return@collect
                loadingMore = true
                val next = page + 1
                catalogRepository.latestUpdatesPage(next, 24)
                    .onSuccess { batch ->
                        val filtered = filterAdult(batch)
                        items = (items + filtered).distinctBy { it.stableId() }
                        page = next
                        hasMore = batch.size >= 24
                    }
                    .onFailure { error = it.message }
                loadingMore = false
            }
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Обновления")
                        Text("Новые главы и тайтлы", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(enabled = !loading, onClick = { reload += 1 }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        when {
            loading && items.isEmpty() -> ListCardsSkeleton(Modifier.padding(padding))
            error != null && items.isEmpty() -> ErrorBox(error ?: "Ошибка", Modifier.padding(padding)) { reload += 1 }
            items.isEmpty() -> EmptyState(
                title = "Обновлений пока нет",
                message = "Новые главы появятся здесь сразу после публикации.",
                icon = Icons.Outlined.Update,
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item {
                    PageIntro(
                        title = "Свежие главы без пропусков",
                        subtitle = "Лента автоматически догружает предыдущие обновления",
                        icon = Icons.Outlined.Update,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        trailing = { StatusPill("${items.size}") },
                    )
                }
                items(items, key = { it.stableId().ifBlank { it.slug.orEmpty() } }) { title ->
                    TitleSearchCard(
                        title = title.displayTitle(),
                        cover = title.coverPath(),
                        type = title.type,
                        rating = title.displayRating(),
                        totalChapters = title.totalChapters,
                        year = title.releaseYear,
                        status = title.status,
                        subtitle = title.chapter?.let { "Новая глава: $it" } ?: "Недавно обновлено",
                        isAdult = title.isAdult == true,
                        onClick = { onOpenTitle(title.stableId(), title.slug) },
                    )
                }
                item { LoadingMoreBar(loadingMore, "Загружаем предыдущие обновления…") }
            }
        }
    }
}
