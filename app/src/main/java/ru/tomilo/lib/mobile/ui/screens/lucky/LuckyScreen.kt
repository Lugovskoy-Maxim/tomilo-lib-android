package ru.tomilo.lib.mobile.ui.screens.lucky

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.tomilo.lib.mobile.core.toUserFacingError
import ru.tomilo.lib.mobile.data.api.CatalogTitleDto
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.TitlePosterCard
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted

/**
 * Подборка «Мне повезёт»: бесконечная лента случайных тайтлов.
 * Каждая прокрутка к концу подгружает новую порцию; кнопка-кубик перемешивает ленту заново.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuckyScreen(
    catalogRepository: CatalogRepository,
    includeAdult: Boolean,
    onBack: () -> Unit,
    onOpenTitle: (id: String, slug: String?) -> Unit,
) {
    var items by remember { mutableStateOf<List<CatalogTitleDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var endReached by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var seed by remember { mutableIntStateOf(0) }
    val seen = remember { mutableSetOf<String>() }
    val gridState = rememberLazyGridState()

    suspend fun loadPage() {
        if (loadingMore || endReached) return
        loadingMore = true
        error = null
        catalogRepository.randomTitles(limit = 18, includeAdult = includeAdult)
            .onSuccess { batch ->
                val fresh = batch.filter { seen.add(it.stableId()) }
                items = items + fresh
                // Сервер может вернуть только уже виденные тайтлы — лента исчерпана.
                if (fresh.isEmpty()) endReached = true
            }
            .onFailure { error = it.toUserFacingError("Не удалось загрузить подборку.") }
        loadingMore = false
        loading = false
    }

    LaunchedEffect(seed, includeAdult) {
        items = emptyList()
        seen.clear()
        endReached = false
        loading = true
        loadPage()
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= items.size - 8
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && items.isNotEmpty()) loadPage()
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = {
                    Text("Мне повезёт", fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { seed += 1 }, enabled = !loading) {
                        Icon(Icons.Default.Casino, contentDescription = "Перемешать ленту")
                    }
                },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        when {
            loading && items.isEmpty() -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            error != null && items.isEmpty() -> ErrorBox(
                message = error.orEmpty(),
                onRetry = { seed += 1 },
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            items.isEmpty() -> EmptyState(
                title = "Пока пусто",
                message = "Не удалось собрать подборку. Попробуйте перемешать ещё раз.",
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            else -> LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(TomiloBg),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items, key = { it.stableId() }) { item ->
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
                        plain = true,
                        showCoverType = false,
                        showCoverChapter = false,
                        showFooterRating = false,
                        footerTrailing = item.totalChapters?.let { "$it глав" },
                    )
                }
                if (loadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "Загружаем ещё…",
                            style = MaterialTheme.typography.bodySmall,
                            color = TomiloMuted,
                            modifier = Modifier.padding(vertical = 14.dp),
                        )
                    }
                } else if (endReached) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "Лента исчерпана — нажмите кубик, чтобы перемешать заново",
                            style = MaterialTheme.typography.bodySmall,
                            color = TomiloMuted,
                            modifier = Modifier.padding(vertical = 14.dp),
                        )
                    }
                }
            }
        }
    }
}
