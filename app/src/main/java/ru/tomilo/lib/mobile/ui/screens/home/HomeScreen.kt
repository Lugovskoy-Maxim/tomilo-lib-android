package ru.tomilo.lib.mobile.ui.screens.home

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.core.ReaderMode
import ru.tomilo.lib.mobile.data.api.CatalogTitleDto
import ru.tomilo.lib.mobile.data.api.HistoryEntryDto
import ru.tomilo.lib.mobile.data.local.ContentPrefs
import ru.tomilo.lib.mobile.data.local.ContentSettings
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.data.repo.HistoryRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.HomeFeedSkeleton
import ru.tomilo.lib.mobile.ui.components.TitlePosterCard
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    catalogRepository: CatalogRepository,
    contentPrefs: ContentPrefs,
    authRepository: AuthRepository,
    historyRepository: HistoryRepository,
    socialRepository: SocialRepository,
    onOpenTitle: (id: String, slug: String?) -> Unit,
    onOpenCatalog: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenUpdates: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenQuests: () -> Unit = {},
    onOpenFriends: () -> Unit = {},
    onOpenOffline: () -> Unit = {},
    onOpenWheel: () -> Unit = {},
    onContinueReading: (titleId: String, chapterId: String) -> Unit = { _, _ -> },
) {
    val contentSettings by contentPrefs.settingsFlow.collectAsState(initial = ContentSettings())
    val user by authRepository.userFlow.collectAsState(initial = null)
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var updates by remember { mutableStateOf<List<CatalogTitleDto>>(emptyList()) }
    var popular by remember { mutableStateOf<List<CatalogTitleDto>>(emptyList()) }
    var continueItems by remember { mutableStateOf<List<HistoryEntryDto>>(emptyList()) }
    var reloadToken by remember { mutableStateOf(0) }

    LaunchedEffect(user?.stableId(), reloadToken) {
        continueItems = if (user == null) {
            emptyList()
        } else {
            historyRepository.history().getOrDefault(emptyList()).take(8)
        }
    }

    LaunchedEffect(reloadToken, contentSettings.showAdultContent) {
        loading = true
        error = null
        val u = catalogRepository.latestUpdates()
        val p = catalogRepository.popular()
        if (u.isFailure && p.isFailure) {
            error = u.exceptionOrNull()?.message ?: "Не удалось загрузить"
            loading = false
            return@LaunchedEffect
        }
        val showAdult = contentSettings.showAdultContent
        fun List<CatalogTitleDto>.filterAdult() = if (showAdult) this else filter { it.isAdult != true }
        updates = u.getOrDefault(emptyList()).filterAdult()
        popular = p.getOrDefault(emptyList()).filterAdult()
        loading = false
    }

    val greeting = remember(user) {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val part = when (hour) {
            in 5..11 -> "Доброе утро"
            in 12..17 -> "Добрый день"
            else -> "Добрый вечер"
        }
        val name = user?.username?.takeIf { it.isNotBlank() }
        if (name != null) "$part, $name" else part
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("TOMILO LIB", style = MaterialTheme.typography.titleLarge)
                        Text(greeting, style = MaterialTheme.typography.bodySmall, color = TomiloMuted)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Поиск")
                    }
                },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        when {
            loading && updates.isEmpty() && popular.isEmpty() -> HomeFeedSkeleton(Modifier.padding(padding))
            error != null && updates.isEmpty() -> Column(Modifier.padding(padding)) {
                ErrorBox(error ?: "Ошибка") { reloadToken += 1 }
            }
            else -> Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 110.dp),
            ) {
                if (continueItems.isNotEmpty()) {
                    SectionHead("Продолжить", action = "История", onAction = onOpenHistory)
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        continueItems.forEach { item ->
                            ContinueCard(
                                item = item,
                                onOpen = {
                                    val chapter = item.chapterKey()
                                    if (chapter.isNotBlank()) onContinueReading(item.titleKey(), chapter)
                                    else onOpenTitle(item.titleKey(), item.slug())
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                }

                SectionHead("Новые главы", action = "Каталог", onAction = onOpenUpdates.ifBlankAction(onOpenCatalog))
                PosterRow(
                    items = updates.take(12),
                    onOpen = { onOpenTitle(it.stableId(), it.slug) },
                )
                Spacer(Modifier.height(22.dp))

                SectionHead("Сейчас читают", onAction = onOpenCatalog)
                PosterRow(
                    items = popular.take(12),
                    onOpen = { onOpenTitle(it.stableId(), it.slug) },
                )
            }
        }
    }
}

private fun (() -> Unit).ifBlankAction(fallback: () -> Unit): () -> Unit = this

@Composable
private fun SectionHead(title: String, action: String = "Все", onAction: () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onAction) { Text(action, color = TomiloPrimary) }
    }
}

@Composable
private fun PosterRow(items: List<CatalogTitleDto>, onOpen: (CatalogTitleDto) -> Unit) {
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.forEach { item ->
            TitlePosterCard(
                title = item.displayTitle(),
                cover = item.coverPath(),
                onClick = { onOpen(item) },
                type = ReaderMode.typeLabel(item.type),
                rating = item.displayRating(),
                totalChapters = item.totalChapters,
                year = item.releaseYear,
                isAdult = item.isAdult == true,
            )
        }
    }
}

@Composable
private fun ContinueCard(item: HistoryEntryDto, onOpen: () -> Unit) {
    Column(
        Modifier
            .width(168.dp)
            .background(TomiloSurface, RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(TomiloSurface),
        ) {
            AsyncImage(
                model = MediaUrl.resolve(item.coverPath()),
                contentDescription = item.displayTitle(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                    .padding(10.dp),
            ) {
                Text(
                    "Читать ${item.chapterLabel()}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(
            item.displayTitle(),
            color = TomiloText,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp),
        )
    }
}
