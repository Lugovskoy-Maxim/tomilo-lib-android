package ru.tomilo.lib.mobile.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import ru.tomilo.lib.mobile.ui.components.TomiloCoverImage
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import ru.tomilo.lib.mobile.ui.theme.TomiloText
import androidx.compose.ui.graphics.vector.ImageVector

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
    onOpenGenre: (String) -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenUpdates: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenQuests: () -> Unit = {},
    onOpenFriends: () -> Unit = {},
    onOpenOffline: () -> Unit = {},
    onOpenGames: () -> Unit = {},
    onContinueReading: (titleId: String, chapterId: String) -> Unit = { _, _ -> },
) {
    val contentSettings by contentPrefs.settingsFlow.collectAsState(initial = ContentSettings())
    val user by authRepository.userFlow.collectAsState(initial = null)
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var updates by remember { mutableStateOf<List<CatalogTitleDto>>(emptyList()) }
    var popular by remember { mutableStateOf<List<CatalogTitleDto>>(emptyList()) }
    var continueItems by remember { mutableStateOf<List<HistoryEntryDto>>(emptyList()) }
    var genres by remember { mutableStateOf<List<String>>(emptyList()) }
    var reloadToken by remember { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }

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
        if (genres.isEmpty()) {
            genres = catalogRepository.filterOptions().getOrNull()?.genres.orEmpty().take(16)
        }
        loading = false
        refreshing = false
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
                    Text("TOMILO LIB", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
            else -> PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    refreshing = true
                    reloadToken += 1
                },
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(TomiloPrimary.copy(alpha = 0.035f), TomiloBg, TomiloBg),
                        ),
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 110.dp),
            ) {
                HomeHero(
                    greeting = greeting,
                    continueCount = continueItems.size,
                    onOpenHistory = onOpenHistory,
                )

                HomeSearchBar(onClick = onOpenSearch)
                ShortcutRow(
                    onUpdates = onOpenUpdates,
                    onQuests = onOpenQuests,
                    onOffline = onOpenOffline,
                    onFriends = onOpenFriends,
                    onGames = onOpenGames,
                )

                if (continueItems.isNotEmpty()) {
                    SectionHead("Продолжить", action = "История", onAction = onOpenHistory)
                    Row(
                        Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        continueItems.take(2).forEach { item ->
                            ContinueCard(
                                item = item,
                                modifier = Modifier.weight(1f),
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
                Column(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    updates.take(6).chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { item ->
                                TitlePosterCard(
                                    title = item.displayTitle(),
                                    cover = item.coverPath(),
                                    onClick = { onOpenTitle(item.stableId(), item.slug) },
                                    modifier = Modifier.weight(1f),
                                    width = null,
                                    type = ReaderMode.typeLabel(item.type),
                                    rating = item.displayRating(),
                                    chapterBadge = item.chapterBadge() ?: item.totalChapters?.let { "$it гл." },
                                    isAdult = item.isAdult == true,
                                    compact = true,
                                )
                            }
                            repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))

                if (genres.isNotEmpty()) {
                    SectionHead("Жанры", action = "Все жанры", onAction = onOpenCatalog)
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        genres.forEach { genre ->
                            FilterChip(
                                selected = false,
                                onClick = { onOpenGenre(genre) },
                                label = { Text(ru.tomilo.lib.mobile.core.GenreLabels.ru(genre)) },
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                SectionHead("Сейчас читают", onAction = onOpenCatalog)
                PosterRow(
                    items = popular.take(12),
                    onOpen = { onOpenTitle(it.stableId(), it.slug) },
                )
            }
            }
        }
    }
}

private fun (() -> Unit).ifBlankAction(fallback: () -> Unit): () -> Unit = this

@Composable
private fun HomeHero(
    greeting: String,
    continueCount: Int,
    onOpenHistory: () -> Unit,
) {
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        TomiloPrimary.copy(alpha = 0.22f),
                        TomiloSurface2,
                        TomiloSurface,
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(26.dp))
            .padding(horizontal = 18.dp, vertical = 17.dp),
    ) {
        Text(greeting, color = TomiloMuted, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "Что будем читать?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        val subtitle = if (continueCount > 0) {
            "В истории $continueCount ${continueCount.readingItemsLabel()} — продолжите с того же места"
        } else {
            "Свежие главы и любимые тайтлы уже ждут вас"
        }
        Text(subtitle, color = TomiloMuted, style = MaterialTheme.typography.bodyMedium)
        if (continueCount > 0) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onOpenHistory, contentPadding = PaddingValues()) {
                Text("Открыть историю  ›", color = TomiloPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun Int.readingItemsLabel(): String = when {
    this % 100 in 11..14 -> "тайтлов"
    this % 10 == 1 -> "тайтл"
    this % 10 in 2..4 -> "тайтла"
    else -> "тайтлов"
}

@Composable
private fun HomeSearchBar(onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TomiloSurface2.copy(alpha = 0.92f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = TomiloMuted)
        Spacer(Modifier.width(10.dp))
        Text("Быстрый поиск", color = TomiloMuted, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ShortcutRow(
    onUpdates: () -> Unit,
    onQuests: () -> Unit,
    onOffline: () -> Unit,
    onFriends: () -> Unit,
    onGames: () -> Unit,
) {
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ShortcutChip("Обновления", Icons.Outlined.Update, onUpdates)
        ShortcutChip("Задания", Icons.Outlined.CardGiftcard, onQuests)
        ShortcutChip("Игры", Icons.Outlined.SportsEsports, onGames)
        ShortcutChip("Офлайн", Icons.Outlined.CloudOff, onOffline)
        ShortcutChip("Друзья", Icons.Outlined.People, onFriends)
    }
}

@Composable
private fun ShortcutChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Box(
                Modifier.size(28.dp).clip(RoundedCornerShape(10.dp)).background(TomiloPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = TomiloPrimary) }
        },
    )
}

@Composable
private fun SectionHead(title: String, action: String = "Все", onAction: () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        TextButton(onClick = onAction) {
            Text("$action  ›", color = TomiloPrimary, fontWeight = FontWeight.SemiBold)
        }
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
private fun ContinueCard(item: HistoryEntryDto, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val totalChapters = (item.titleId as? kotlinx.serialization.json.JsonObject)
        ?.get("totalChapters")?.toString()?.trim('"')?.toFloatOrNull()
    val currentChapter = item.lastChapter?.numberLabel()?.toFloatOrNull()
    val progress = if (totalChapters != null && totalChapters > 0f && currentChapter != null) {
        (currentChapter / totalChapters).coerceIn(0f, 1f)
    } else null
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(TomiloSurface)
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(22.dp))
            .clickable(onClick = onOpen),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(TomiloSurface),
        ) {
            TomiloCoverImage(
                source = item.coverPath(),
                contentDescription = item.displayTitle(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.94f))))
                    .padding(10.dp),
            ) {
                Column {
                    Row {
                        Text("Читать ", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        Text(item.chapterLabel(), color = TomiloPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    progress?.let {
                        Spacer(Modifier.height(7.dp))
                        LinearProgressIndicator(
                            progress = { it },
                            modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(99.dp)),
                            color = TomiloPrimary,
                            trackColor = Color.White.copy(alpha = 0.16f),
                        )
                    }
                }
            }
        }
        Text(
            item.displayTitle(),
            color = TomiloText,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp),
        )
        item.chaptersCount?.takeIf { it > 0 }?.let { count ->
            Text(
                "Прочитано $count гл.",
                color = TomiloMuted,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 12.dp),
            )
        } ?: Spacer(Modifier.height(12.dp))
    }
}
