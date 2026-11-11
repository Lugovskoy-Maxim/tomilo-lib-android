package ru.tomilo.lib.mobile.ui.screens.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import ru.tomilo.lib.mobile.core.toUserFacingError
import ru.tomilo.lib.mobile.data.api.CollectionDetailDto
import ru.tomilo.lib.mobile.data.api.EditorialCollectionDto
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.ListCardsSkeleton
import ru.tomilo.lib.mobile.ui.components.PageIntro
import ru.tomilo.lib.mobile.ui.components.TitlePosterCard
import ru.tomilo.lib.mobile.ui.components.TomiloCoverImage
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    catalogRepository: CatalogRepository,
    onBack: () -> Unit,
    onOpenTitle: (String, String?) -> Unit,
) {
    var collections by remember { mutableStateOf<List<EditorialCollectionDto>>(emptyList()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<CollectionDetailDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var detailLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var detailError by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var detailReload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) {
        loading = true
        error = null
        try {
            catalogRepository.editorialCollections()
                .onSuccess { collections = it.filter { item -> item.stableId().isNotBlank() } }
                .onFailure { error = it.toUserFacingError("Не удалось загрузить подборки.") }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            error = failure.toUserFacingError("Не удалось загрузить подборки.")
        } finally {
            loading = false
        }
    }

    LaunchedEffect(selectedId, detailReload) {
        val id = selectedId ?: run {
            detail = null
            detailError = null
            return@LaunchedEffect
        }
        detailLoading = true
        detailError = null
        try {
            catalogRepository.collection(id)
                .onSuccess { detail = it }
                .onFailure { detailError = it.toUserFacingError("Не удалось открыть подборку.") }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            detailError = failure.toUserFacingError("Не удалось открыть подборку.")
        } finally {
            detailLoading = false
        }
    }

    val showingDetail = selectedId != null
    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (showingDetail) detail?.name ?: "Подборка" else "Подборки")
                        Text(
                            if (showingDetail) "Тайтлы коллекции" else "Тематические подборки сайта",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (showingDetail) {
                                selectedId = null
                                detail = null
                            } else {
                                onBack()
                            }
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        enabled = if (showingDetail) !detailLoading else !loading,
                        onClick = {
                            if (showingDetail) detailReload++ else reload++
                        },
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        when {
            !showingDetail && loading && collections.isEmpty() ->
                ListCardsSkeleton(Modifier.padding(padding))
            !showingDetail && error != null && collections.isEmpty() ->
                ErrorBox(error.orEmpty(), Modifier.padding(padding)) { reload++ }
            !showingDetail && collections.isEmpty() ->
                EmptyState(
                    title = "Подборок пока нет",
                    message = "Редакционные коллекции появятся здесь после публикации на сайте.",
                    icon = Icons.Default.CollectionsBookmark,
                    modifier = Modifier.padding(padding),
                )
            !showingDetail -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    PageIntro(
                        title = "Читайте подборками",
                        subtitle = "Те же коллекции, что на tomilo-lib.ru/collections",
                        icon = Icons.Default.CollectionsBookmark,
                        accent = TomiloPrimary,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(collections, key = { it.stableId() }) { collection ->
                    CollectionRow(
                        collection = collection,
                        onClick = { selectedId = collection.stableId() },
                    )
                }
            }
            detailLoading && detail == null ->
                ListCardsSkeleton(Modifier.padding(padding))
            detailError != null && detail == null ->
                ErrorBox(detailError.orEmpty(), Modifier.padding(padding)) { detailReload++ }
            else -> {
                val titles = detail?.titles.orEmpty()
                if (titles.isEmpty() && !detailLoading) {
                    EmptyState(
                        title = "В подборке пусто",
                        message = "Тайтлы ещё не добавлены или недоступны.",
                        icon = Icons.Default.CollectionsBookmark,
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        modifier = Modifier.padding(padding).fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                                detail?.description?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, color = TomiloMuted, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(8.dp))
                                }
                                Text(
                                    "${titles.size} тайтлов",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        items(
                            titles,
                            key = { it.stableId().ifBlank { it.displayTitle() } },
                        ) { title ->
                            TitlePosterCard(
                                title = title.displayTitle(),
                                cover = title.coverPath(),
                                type = title.type,
                                rating = title.displayRating(),
                                year = title.releaseYear,
                                status = title.status,
                                totalChapters = title.totalChapters,
                                width = null,
                                onClick = { onOpenTitle(title.stableId(), title.slug) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionRow(
    collection: EditorialCollectionDto,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = TomiloSurface2,
        border = androidx.compose.foundation.BorderStroke(1.dp, TomiloBorder),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(width = 72.dp, height = 96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TomiloBg),
            ) {
                TomiloCoverImage(
                    source = collection.coverPath(),
                    contentDescription = collection.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    collection.name.orEmpty().ifBlank { "Без названия" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    collection.description.orEmpty(),
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    buildString {
                        append(collection.titleCount())
                        append(" тайтлов")
                        collection.views?.takeIf { it > 0 }?.let {
                            append(" · ")
                            append(it)
                            append(" просм.")
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = TomiloPrimary,
                )
            }
        }
    }
}
