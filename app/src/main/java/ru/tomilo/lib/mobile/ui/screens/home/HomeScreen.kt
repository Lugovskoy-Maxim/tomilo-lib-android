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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.tomilo.lib.mobile.core.GenreLabels
import ru.tomilo.lib.mobile.core.ReaderMode
import ru.tomilo.lib.mobile.data.api.CatalogTitleDto
import ru.tomilo.lib.mobile.data.api.HistoryEntryDto
import ru.tomilo.lib.mobile.data.api.LeaderboardUserDto
import ru.tomilo.lib.mobile.data.local.ContentPrefs
import ru.tomilo.lib.mobile.data.local.ContentSettings
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.data.repo.HistoryRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.DecoratedAvatar
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.HomeFeedSkeleton
import ru.tomilo.lib.mobile.ui.components.TitlePosterCard
import ru.tomilo.lib.mobile.ui.components.TomiloCoverImage
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import ru.tomilo.lib.mobile.ui.theme.TomiloText

enum class FeedFilter(val label: String) {
    ALL("Все"),
    MANHWA("Манхва"),
    MANGA("Манга"),
    MANHUA("Маньхуа"),
    TOP_RATED("Топ недели"),
}

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
    onOpenWheel: () -> Unit = {},
    onOpenLeaders: () -> Unit = {},
    onOpenPremium: () -> Unit = {},
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
    var leaders by remember { mutableStateOf<List<LeaderboardUserDto>>(emptyList()) }
    var reloadToken by remember { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(FeedFilter.ALL) }

    LaunchedEffect(user?.stableId(), reloadToken) {
        continueItems = if (user == null) {
            emptyList()
        } else {
            historyRepository.history().getOrDefault(emptyList()).take(10)
        }
    }

    LaunchedEffect(reloadToken) {
        socialRepository.leaderboard("level", "week").onSuccess {
            leaders = it.take(5)
        }.onFailure {
            socialRepository.leaderboard("exp", "all").onSuccess {
                leaders = it.take(5)
            }
        }
    }

    LaunchedEffect(reloadToken, contentSettings.showAdultContent) {
        loading = true
        error = null
        val u = catalogRepository.latestUpdates()
        val p = catalogRepository.popular()
        if (u.isFailure && p.isFailure) {
            error = u.exceptionOrNull()?.message ?: "Не удалось загрузить данные"
            loading = false
            return@LaunchedEffect
        }
        val showAdult = contentSettings.showAdultContent
        fun List<CatalogTitleDto>.filterAdult() = if (showAdult) this else filter { it.isAdult != true }
        updates = u.getOrDefault(emptyList()).filterAdult()
        popular = p.getOrDefault(emptyList()).filterAdult()
        if (genres.isEmpty()) {
            val fetchedGenres = catalogRepository.filterOptions().getOrNull()?.genres.orEmpty()
            genres = if (fetchedGenres.isNotEmpty()) {
                fetchedGenres.take(16)
            } else {
                listOf("shounen", "romance", "fantasy", "action", "isekai", "adventure", "drama", "comedy")
            }
        }
        loading = false
        refreshing = false
    }

    val greeting = remember(user) {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Доброе утро"
            in 12..17 -> "Добрый день"
            in 18..22 -> "Добрый вечер"
            else -> "Доброй ночи"
        }
    }

    // Filtered lists based on active category
    val filteredUpdates = remember(updates, selectedFilter) {
        when (selectedFilter) {
            FeedFilter.ALL -> updates
            FeedFilter.MANHWA -> updates.filter { it.type.equals("manhwa", ignoreCase = true) }
            FeedFilter.MANGA -> updates.filter { it.type.equals("manga", ignoreCase = true) }
            FeedFilter.MANHUA -> updates.filter { it.type.equals("manhua", ignoreCase = true) }
            FeedFilter.TOP_RATED -> updates.sortedByDescending { it.displayRating() ?: 0.0 }
        }
    }

    val filteredPopular = remember(popular, selectedFilter) {
        when (selectedFilter) {
            FeedFilter.ALL -> popular
            FeedFilter.MANHWA -> popular.filter { it.type.equals("manhwa", ignoreCase = true) }
            FeedFilter.MANGA -> popular.filter { it.type.equals("manga", ignoreCase = true) }
            FeedFilter.MANHUA -> popular.filter { it.type.equals("manhua", ignoreCase = true) }
            FeedFilter.TOP_RATED -> popular.sortedByDescending { it.displayRating() ?: 0.0 }
        }
    }

    // Spotlight title to feature in the hero section
    val featuredTitle = remember(popular, updates) {
        popular.firstOrNull() ?: updates.firstOrNull()
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "TOMILO LIB",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = TomiloText,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Поиск", tint = TomiloText)
                    }
                },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        when {
            loading && updates.isEmpty() && popular.isEmpty() -> HomeFeedSkeleton(Modifier.padding(padding))
            error != null && updates.isEmpty() && popular.isEmpty() -> Column(Modifier.padding(padding)) {
                ErrorBox(error ?: "Ошибка сети") { reloadToken += 1 }
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
                                listOf(
                                    TomiloPrimary.copy(alpha = 0.06f),
                                    TomiloBg,
                                    TomiloBg,
                                ),
                            ),
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 110.dp),
                ) {
                    // Header Greeting & User Stats Pill
                    HomeHeaderGreeting(
                        greeting = greeting,
                        username = user?.username,
                        streak = user?.currentStreak,
                        coins = user?.balance,
                    )

                    // Hero Featured Title Spotlight (Featured Banner)
                    featuredTitle?.let { hero ->
                        HomeSpotlightBanner(
                            item = hero,
                            onOpen = { onOpenTitle(hero.stableId(), hero.slug) },
                            onLuckyRandom = {
                                val pool = (popular + updates).distinctBy { it.stableId() }
                                if (pool.isNotEmpty()) {
                                    val randomPick = pool.random()
                                    onOpenTitle(randomPick.stableId(), randomPick.slug)
                                }
                            },
                        )
                    }

                    // Modern Quick Search Field
                    HomeSearchBar(onClick = onOpenSearch)

                    // Quick Format Filter Chips
                    HomeFilterRow(
                        selected = selectedFilter,
                        onSelect = { selectedFilter = it },
                    )

                    // Fast Actions & Shortcuts Row
                    ShortcutRow(
                        onUpdates = onOpenUpdates,
                        onQuests = onOpenQuests,
                        onWheel = onOpenWheel,
                        onOffline = onOpenOffline,
                        onFriends = onOpenFriends,
                        onGames = onOpenGames,
                    )

                    // "Продолжить чтение" Horizontal Gallery
                    if (continueItems.isNotEmpty()) {
                        SectionHead(
                            title = "Продолжить чтение",
                            action = "История",
                            onAction = onOpenHistory,
                        )
                        Row(
                            Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            continueItems.forEach { item ->
                                ContinueReadingCard(
                                    item = item,
                                    onOpen = {
                                        val chapter = item.chapterKey()
                                        if (chapter.isNotBlank()) onContinueReading(item.titleKey(), chapter)
                                        else onOpenTitle(item.titleKey(), item.slug())
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    // "Новые главы и релизы"
                    SectionHead(
                        title = "Свежие обновления",
                        action = "Каталог",
                        badge = if (filteredUpdates.isNotEmpty()) "${filteredUpdates.size}" else null,
                        onAction = onOpenUpdates,
                    )
                    Column(
                        Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        filteredUpdates.take(8).chunked(2).forEach { row ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                row.forEach { item ->
                                    TitlePosterCard(
                                        title = item.displayTitle(),
                                        cover = item.coverPath(),
                                        onClick = { onOpenTitle(item.stableId(), item.slug) },
                                        modifier = Modifier.weight(1f),
                                        width = null,
                                        type = ReaderMode.typeLabel(item.type),
                                        rating = item.displayRating(),
                                        status = item.status,
                                        chapterBadge = item.chapterBadge() ?: item.totalChapters?.let { "$it гл." },
                                        isAdult = item.isAdult == true,
                                        compact = false,
                                    )
                                }
                                repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    // Компактный блок "Топ читателей недели" для разбивки однотипности тайтлов
                    HomeLeaderboardPreview(
                        leaders = leaders,
                        onOpenLeaders = onOpenLeaders,
                    )
                    Spacer(Modifier.height(24.dp))

                    // "Реклама" Премиум
                    HomePremiumPromotionBanner(
                        onOpenPremium = onOpenPremium,
                    )
                    Spacer(Modifier.height(24.dp))

                    // "Популярное сейчас" with Rank Medals
                    SectionHead(
                        title = "Сейчас читают",
                        action = "Все",
                        onAction = onOpenCatalog,
                    )
                    RankedPosterRow(
                        items = filteredPopular.take(15),
                        onOpen = { onOpenTitle(it.stableId(), it.slug) },
                    )
                    Spacer(Modifier.height(24.dp))

                    // Curated Genre Highlights & Categories
                    if (genres.isNotEmpty()) {
                        SectionHead(
                            title = "Популярные жанры",
                            action = "Каталог",
                            onAction = onOpenCatalog,
                        )
                        Row(
                            Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            genres.forEach { genre ->
                                GenreBadgeChip(
                                    genre = genre,
                                    onClick = { onOpenGenre(genre) },
                                )
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeaderGreeting(
    greeting: String,
    username: String?,
    streak: Int?,
    coins: Int?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (username.isNullOrBlank()) greeting else "$greeting, $username!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TomiloText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Что почитаем сегодня?",
                style = MaterialTheme.typography.bodySmall,
                color = TomiloMuted,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (streak != null && streak > 0) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(TomiloPrimary.copy(alpha = 0.15f))
                        .border(1.dp, TomiloPrimary.copy(alpha = 0.3f), RoundedCornerShape(99.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.LocalFireDepartment,
                        contentDescription = null,
                        tint = TomiloPrimary,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "$streak дн.",
                        color = TomiloPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (coins != null && coins > 0) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(TomiloPremium.copy(alpha = 0.15f))
                        .border(1.dp, TomiloPremium.copy(alpha = 0.3f), RoundedCornerShape(99.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "🪙 $coins",
                        color = TomiloPremium,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSpotlightBanner(
    item: CatalogTitleDto,
    onOpen: () -> Unit,
    onLuckyRandom: () -> Unit,
) {
    Box(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(TomiloSurface)
            .border(1.dp, TomiloPrimary.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
            .clickable(onClick = onOpen),
    ) {
        // High-res cover backdrop with dark gradient fade (+19% height for more presence)
        TomiloCoverImage(
            source = item.coverPath(),
            contentDescription = item.displayTitle(),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        )

        // Gradient overlay
        Box(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.70f),
                            TomiloSurface,
                        ),
                    ),
                ),
        )

        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TomiloPrimary)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        "🔥 В ЦЕНТРЕ ВНИМАНИЯ",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Spacer(Modifier.weight(1f))
                item.displayRating()?.let { rating ->
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = TomiloPremium,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "%.1f".format(rating),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                item.displayTitle(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item.type?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        ReaderMode.typeLabel(it),
                        color = TomiloPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                item.totalChapters?.let {
                    Text(
                        "·  $it глав",
                        color = TomiloMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                item.releaseYear?.let {
                    Text(
                        "·  $it",
                        color = TomiloMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onOpen,
                    modifier = Modifier.weight(1.3f).height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TomiloPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Читать сейчас", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onLuckyRandom,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TomiloText),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(TomiloBorder, TomiloBorder))),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                ) {
                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp), tint = TomiloPremium)
                    Spacer(Modifier.width(6.dp))
                    Text("Мне повезёт", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun HomeSearchBar(onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TomiloSurface2.copy(alpha = 0.95f))
            .border(1.dp, TomiloBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = TomiloPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Поиск по названию, автору или жанру...",
            color = TomiloMuted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HomeFilterRow(
    selected: FeedFilter,
    onSelect: (FeedFilter) -> Unit,
) {
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FeedFilter.values().forEach { filter ->
            val isSelected = selected == filter
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(filter) },
                label = {
                    Text(
                        filter.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TomiloPrimary,
                    selectedLabelColor = Color.White,
                    containerColor = TomiloSurface,
                    labelColor = TomiloMuted,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = TomiloBorder,
                    selectedBorderColor = TomiloPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
            )
        }
    }
}

@Composable
private fun ShortcutRow(
    onUpdates: () -> Unit,
    onQuests: () -> Unit,
    onWheel: () -> Unit,
    onOffline: () -> Unit,
    onFriends: () -> Unit,
    onGames: () -> Unit,
) {
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ShortcutActionItem("Новинки", Icons.Outlined.Update, TomiloPrimary, onUpdates)
        ShortcutActionItem("Квесты", Icons.Outlined.CardGiftcard, Color(0xFF4CAF50), onQuests)
        ShortcutActionItem("Колесо", Icons.Default.Casino, TomiloPremium, onWheel)
        ShortcutActionItem("Офлайн", Icons.Outlined.CloudOff, Color(0xFF29B6F6), onOffline)
        ShortcutActionItem("Игры", Icons.Outlined.SportsEsports, Color(0xFFAB47BC), onGames)
        ShortcutActionItem("Друзья", Icons.Outlined.People, Color(0xFFFF7043), onFriends)
    }
}

@Composable
private fun ShortcutActionItem(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = TomiloSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, TomiloBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = TomiloText)
        }
    }
}

@Composable
private fun SectionHead(
    title: String,
    action: String = "Все",
    badge: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TomiloText,
        )
        badge?.let {
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(TomiloSurface2)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(it, color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onAction) {
            Text("$action  ›", color = TomiloPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ContinueReadingCard(
    item: HistoryEntryDto,
    onOpen: () -> Unit,
) {
    val totalChapters = (item.titleId as? kotlinx.serialization.json.JsonObject)
        ?.get("totalChapters")?.toString()?.trim('"')?.toFloatOrNull()
    val currentChapter = item.lastChapter?.numberLabel()?.toFloatOrNull()
    val completedChapters = (item.chaptersCount?.toFloat() ?: currentChapter ?: 0f).coerceAtLeast(0f)
    val progress = if (totalChapters != null && totalChapters > 0f) {
        (completedChapters / totalChapters).coerceIn(0f, 1f)
    } else null

    Column(
        Modifier
            .width(160.dp)
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(TomiloSurface)
            .border(1.dp, TomiloBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(TomiloSurface2),
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
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.90f)),
                        ),
                    )
                    .padding(8.dp),
            ) {
                Text(
                    "Глава ${item.chapterLabel()}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(Modifier.padding(10.dp)) {
            Text(
                item.displayTitle(),
                color = TomiloText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val label = if (completedChapters > 0f) "Гл. ${completedChapters.toInt()}" else "Начать"
                Text(label, color = TomiloMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                progress?.let {
                    Text(
                        "${(it * 100).toInt()}%",
                        color = TomiloPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (progress != null) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(99.dp)),
                    color = TomiloPrimary,
                    trackColor = TomiloSurface2,
                )
            }
        }
    }
}

@Composable
private fun RankedPosterRow(
    items: List<CatalogTitleDto>,
    onOpen: (CatalogTitleDto) -> Unit,
) {
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.forEachIndexed { index, item ->
            TitlePosterCard(
                title = item.displayTitle(),
                cover = item.coverPath(),
                onClick = { onOpen(item) },
                type = ReaderMode.typeLabel(item.type),
                rating = item.displayRating(),
                status = item.status,
                totalChapters = item.totalChapters,
                year = item.releaseYear,
                isAdult = item.isAdult == true,
                rank = index + 1,
            )
        }
    }
}

@Composable
private fun HomePremiumPromotionBanner(
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onOpenPremium,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF161418),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            Brush.linearGradient(
                listOf(
                    Color(0xFFFFDF00),
                    Color(0xFFFF7A59),
                    Color(0xFFE5A60D).copy(alpha = 0.35f),
                ),
            ),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF281C10),
                            Color(0xFF1E1716),
                            Color(0xFF121418),
                        ),
                    ),
                )
                .padding(18.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFFFFDF00), Color(0xFFFF9800))))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Text("VIP", color = Color(0xFF261800), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                        Text(
                            "TOMILO PREMIUM",
                            color = TomiloPremium,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                        )
                    }

                    Box(
                        Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            "Реклама",
                            color = TomiloMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Читай без рекламы и ограничений",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TomiloText,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Ранний доступ к новым главам, загрузка в офлайн и эксклюзивные значки читателя",
                    style = MaterialTheme.typography.bodySmall,
                    color = TomiloMuted,
                    lineHeight = 16.sp,
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        "⚡ Без рекламы",
                        "⬇️ Безлимит офлайн",
                        "🎨 Кастомизация",
                        "🛍️ Скидка 20%",
                    ).forEach { feature ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 4.dp),
                        ) {
                            Text(feature, color = Color.White.copy(alpha = 0.95f), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("от 150 ₽ / мес", color = TomiloPremium, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp)
                        Text("без рекламы · офлайн · скидка 20%", color = TomiloMuted, fontSize = 10.sp)
                    }

                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFFDF00), Color(0xFFFF7A59)),
                                ),
                            )
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                    ) {
                        Text(
                            "Попробовать",
                            color = Color(0xFF1E1300),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

private val FallbackLeaders = listOf(
    LeaderboardUserDto(id = "fb_1", username = "ShadowReader", level = 42, chaptersRead = 1240),
    LeaderboardUserDto(id = "fb_2", username = "MangaKing", level = 38, chaptersRead = 980),
    LeaderboardUserDto(id = "fb_3", username = "SakuraBloom", level = 35, chaptersRead = 890),
)

@Composable
private fun HomeLeaderboardPreview(
    leaders: List<LeaderboardUserDto>,
    onOpenLeaders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayLeaders = if (leaders.isNotEmpty()) leaders else FallbackLeaders

    Column(modifier = modifier) {
        SectionHead(
            title = "Топ читателей недели",
            action = "Весь рейтинг",
            badge = "Топ",
            onAction = onOpenLeaders,
        )

        Surface(
            onClick = onOpenLeaders,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(22.dp),
            color = TomiloSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
        ) {
            Column(
                Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFFFC857).copy(alpha = 0.08f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .padding(14.dp),
            ) {
                val podiumUsers = if (displayLeaders.size >= 3) {
                    listOf(displayLeaders[1] to 1, displayLeaders[0] to 0, displayLeaders[2] to 2)
                } else {
                    displayLeaders.take(3).mapIndexed { idx, u -> u to idx }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    podiumUsers.forEach { (user, rankIndex) ->
                        val medalColor = when (rankIndex) {
                            0 -> Color(0xFFFFD700)
                            1 -> Color(0xFFC0C7D4)
                            else -> Color(0xFFCD7F32)
                        }
                        val medalBg = when (rankIndex) {
                            0 -> Brush.horizontalGradient(listOf(Color(0xFFFFDF00), Color(0xFFE5A60D)))
                            1 -> Brush.horizontalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFA6ADBB)))
                            else -> Brush.horizontalGradient(listOf(Color(0xFFFFB076), Color(0xFFD97706)))
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = if (rankIndex == 0) 0.dp else 6.dp),
                        ) {
                            if (rankIndex == 0) {
                                Text(
                                    "👑",
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(bottom = 2.dp),
                                )
                            }
                            Box(contentAlignment = Alignment.BottomEnd) {
                                DecoratedAvatar(
                                    avatarUrl = user.avatar,
                                    username = user.username,
                                    decorations = user.decorations(),
                                    size = if (rankIndex == 0) 56.dp else 46.dp,
                                    ringColor = medalColor,
                                )
                                Box(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(medalBg)
                                        .padding(horizontal = 6.dp, vertical = 1.dp),
                                ) {
                                    Text(
                                        "#${rankIndex + 1}",
                                        color = if (rankIndex == 1) Color.Black else Color(0xFF1E1300),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = user.username?.takeIf(String::isNotBlank) ?: "Читатель",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (rankIndex == 0) FontWeight.Bold else FontWeight.SemiBold,
                                color = TomiloText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Lv ${user.level ?: 1}",
                                color = if (rankIndex == 0) TomiloPrimary else TomiloMuted,
                                fontSize = 10.sp,
                                fontWeight = if (rankIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreBadgeChip(
    genre: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = TomiloSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.border(1.dp, TomiloBorder, RoundedCornerShape(12.dp)),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(TomiloPrimary),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                GenreLabels.ru(genre),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = TomiloText,
            )
        }
    }
}
