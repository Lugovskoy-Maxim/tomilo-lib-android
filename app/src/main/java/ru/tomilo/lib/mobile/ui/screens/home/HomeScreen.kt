package ru.tomilo.lib.mobile.ui.screens.home

import android.text.Html
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.ChatTime
import ru.tomilo.lib.mobile.core.networkAvailabilityFlow
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.core.ReaderMode
import ru.tomilo.lib.mobile.data.api.CatalogTitleDto
import ru.tomilo.lib.mobile.data.api.HistoryEntryDto
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
import ru.tomilo.lib.mobile.ui.components.formatRating
import ru.tomilo.lib.mobile.ui.components.statusColor
import ru.tomilo.lib.mobile.ui.components.statusLabel
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
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
    onOpenProfile: () -> Unit = {},
    onContinueReading: (titleId: String, chapterId: String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val contentSettings by contentPrefs.settingsFlow.collectAsState(initial = ContentSettings())
    val online by remember(context) { context.networkAvailabilityFlow() }.collectAsState(initial = true)
    val user by authRepository.userFlow.collectAsState(initial = null)
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var updates by remember { mutableStateOf<List<CatalogTitleDto>>(emptyList()) }
    var popular by remember { mutableStateOf<List<CatalogTitleDto>>(emptyList()) }
    var continueItems by remember { mutableStateOf<List<HistoryEntryDto>>(emptyList()) }
    var reloadToken by remember { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(FeedFilter.ALL) }
    var showCarousel by remember { mutableStateOf(false) }
    var observedInitialResume by remember { mutableStateOf(false) }
    var previousOnline by remember { mutableStateOf(online) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (observedInitialResume) reloadToken += 1 else observedInitialResume = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(online) {
        if (online && !previousOnline) reloadToken += 1
        previousOnline = online
    }

    LaunchedEffect(user?.stableId(), reloadToken) {
        continueItems = if (user == null) {
            emptyList()
        } else {
            historyRepository.history().getOrDefault(emptyList()).take(10)
        }
    }

    LaunchedEffect(reloadToken, contentSettings.showAdultContent) {
        loading = true
        error = null
        val (u, p) = coroutineScope {
            val updatesRequest = async { catalogRepository.latestUpdates() }
            val popularRequest = async { catalogRepository.popular() }
            updatesRequest.await() to popularRequest.await()
        }
        if (u.isFailure && p.isFailure) {
            error = u.exceptionOrNull()?.message ?: "Не удалось загрузить данные"
            loading = false
            refreshing = false
            return@LaunchedEffect
        }
        val showAdult = contentSettings.showAdultContent
        fun List<CatalogTitleDto>.filterAdult() = if (showAdult) this else filter { it.isAdult != true }
        updates = u.getOrDefault(emptyList()).filterAdult()
        popular = p.getOrDefault(emptyList()).filterAdult()
        loading = false
        refreshing = false
    }

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

    val featured = remember(popular, updates) {
        (popular + updates).distinctBy { it.stableId() }.filter { it.coverPath() != null }.take(10)
    }

    val isPremiumUser = Premium.isActive(user?.subscriptionExpiresAt)

    Scaffold(
        containerColor = TomiloBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                        .background(TomiloBg)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 110.dp),
                ) {
                    if (featured.isNotEmpty()) {
                        HomeHeroCarousel(
                            items = featured,
                            username = user?.username,
                            avatar = user?.avatar,
                            coins = user?.balance ?: 0,
                            decorations = user?.decorations(),
                            onOpen = { item -> onOpenTitle(item.stableId(), item.slug) },
                            onLuckyRandom = {
                                val pool = (popular + updates).distinctBy { it.stableId() }
                                if (pool.isNotEmpty()) {
                                    val pick = pool.random()
                                    onOpenTitle(pick.stableId(), pick.slug)
                                }
                            },
                            onOpenProfile = onOpenProfile,
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    HomeFilterRow(
                        selected = selectedFilter,
                        onSelect = { selectedFilter = it },
                    )

                    ShortcutRow(
                        onUpdates = onOpenUpdates,
                        onSearch = onOpenSearch,
                        onCarousel = { if (featured.isNotEmpty()) showCarousel = true },
                        onQuests = onOpenQuests,
                        onWheel = onOpenWheel,
                        onOffline = onOpenOffline,
                        onFriends = onOpenFriends,
                        onGames = onOpenGames,
                    )

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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            continueItems.forEach { item ->
                                TitlePosterCard(
                                    title = item.displayTitle(),
                                    cover = item.coverPath(),
                                    onClick = {
                                        val chapter = item.chapterKey()
                                        if (chapter.isNotBlank()) onContinueReading(item.titleKey(), chapter)
                                        else onOpenTitle(item.titleKey(), item.slug())
                                    },
                                    width = 148.dp,
                                    type = ReaderMode.typeLabel(item.type()),
                                    rating = item.rating(),
                                    status = item.status(),
                                    isAdult = false,
                                    plain = true,
                                    showCoverType = false,
                                    showCoverChapter = false,
                                    showFooterRating = false,
                                    footerTrailing = item.totalChaptersValue()?.let { "$it глав" },
                                )
                            }
                        }
                        Spacer(Modifier.height(22.dp))
                    }

                    SectionHead(
                        title = "Сейчас читают",
                        action = "Каталог",
                        onAction = onOpenCatalog,
                    )
                    PosterGrid(
                        items = filteredPopular.take(6),
                        columns = 2,
                        onOpen = { onOpenTitle(it.stableId(), it.slug) },
                        trailing = { item -> item.totalChapters?.let { "$it глав" } },
                    )
                    Spacer(Modifier.height(22.dp))

                    if (!isPremiumUser) {
                        HomePremiumPromotionBanner(onOpenPremium = onOpenPremium)
                        Spacer(Modifier.height(22.dp))
                    }

                    SectionHead(
                        title = "Новые главы",
                        action = "Все",
                        onAction = onOpenUpdates,
                    )
                    PosterGrid(
                        items = filteredUpdates.take(9),
                        columns = 3,
                        compact = true,
                        onOpen = { onOpenTitle(it.stableId(), it.slug) },
                        subtitle = { item -> item.updatedAtRaw()?.let { ChatTime.relativeAgo(it) } },
                        trailing = { item -> item.latestChapterFooter() },
                        trailingAccent = true,
                    )
                }
            }
        }
    }

    if (showCarousel && featured.isNotEmpty()) {
        Dialog(
            onDismissRequest = { showCarousel = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            HomeTitleCarousel(
                items = featured,
                catalogRepository = catalogRepository,
                continueItems = continueItems,
                onDismiss = { showCarousel = false },
                onOpenTitle = { item ->
                    showCarousel = false
                    onOpenTitle(item.stableId(), item.slug)
                },
                onReadChapter = { titleId, chapterId ->
                    showCarousel = false
                    onContinueReading(titleId, chapterId)
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeHeroCarousel(
    items: List<CatalogTitleDto>,
    username: String?,
    avatar: String?,
    coins: Int,
    decorations: ru.tomilo.lib.mobile.data.api.EquippedDecorationsDto?,
    onOpen: (CatalogTitleDto) -> Unit,
    onLuckyRandom: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    val screenH = LocalConfiguration.current.screenHeightDp.dp
    val heroHeight = (screenH * 0.62f).coerceIn(440.dp, 580.dp)

    Box(
        Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .background(TomiloBg),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val item = items[page]
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable { onOpen(item) },
            ) {
                TomiloCoverImage(
                    source = item.coverPath(),
                    contentDescription = item.displayTitle(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.28f),
                                    Color.Black.copy(alpha = 0.12f),
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Black.copy(alpha = 0.88f),
                                ),
                            ),
                        ),
                )
            }
        }

        val current = items.getOrNull(pagerState.currentPage)

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.Black.copy(alpha = 0.62f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Row {
                        Text(
                            "TOMILO",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 1.1.sp,
                        )
                        Text(
                            " LIB",
                            color = TomiloPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 1.1.sp,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.62f))
                        .clickable(onClick = onOpenProfile)
                        .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DecoratedAvatar(
                        avatarUrl = avatar,
                        username = username,
                        decorations = decorations,
                        size = 28.dp,
                        ringColor = Color.White.copy(alpha = 0.35f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Paid,
                        contentDescription = null,
                        tint = TomiloPremium,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "$coins",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Column {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(TomiloPrimary)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "В центре внимания",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
                current?.displayRating()?.let { rating ->
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = TomiloPremium,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            formatRating(rating),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { current?.let(onOpen) },
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TomiloPrimary),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                ) {
                    Text("Читать сейчас", fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                }
                Row(
                    Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xCC2A2A2E))
                        .clickable(onClick = onLuckyRandom)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Casino,
                        contentDescription = null,
                        tint = TomiloPremium,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Мне повезет",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        current?.displayTitle().orEmpty(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        current?.type?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                ReaderMode.typeLabel(it),
                                color = TomiloPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        current?.releaseYear?.let {
                            Text("$it", color = TomiloMuted, fontSize = 12.sp)
                        }
                    }
                }
                current?.status?.takeIf { it.isNotBlank() }?.let { status ->
                    HomeStatusChip(status)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(items.size) { index ->
                    val active = index == pagerState.currentPage
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .height(5.dp)
                            .width(if (active) 18.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (active) TomiloPrimary else Color.White.copy(alpha = 0.35f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeStatusChip(status: String) {
    val color = statusColor(status)
    Row(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            statusLabel(status),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
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
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FeedFilter.values().forEach { filter ->
            val isSelected = selected == filter
            Text(
                filter.label,
                color = Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isSelected) TomiloPrimary else Color(0xFF1A1C20))
                    .border(
                        1.dp,
                        if (isSelected) TomiloPrimary else Color.White.copy(alpha = 0.10f),
                        RoundedCornerShape(999.dp),
                    )
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ShortcutRow(
    onUpdates: () -> Unit,
    onSearch: () -> Unit,
    onCarousel: () -> Unit,
    onQuests: () -> Unit,
    onWheel: () -> Unit,
    onOffline: () -> Unit,
    onFriends: () -> Unit,
    onGames: () -> Unit,
) {
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShortcutActionItem("Новинки", Icons.Outlined.Update, onUpdates)
        ShortcutActionItem("Поиск", Icons.Default.Search, onSearch)
        ShortcutActionItem("Больше", Icons.Outlined.ViewCarousel, onCarousel)
    }
}

@Composable
private fun ShortcutActionItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        Modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF17191D))
                .border(1.dp, Color.White.copy(alpha = 0.09f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = TomiloPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(label, color = TomiloText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun SectionHead(
    title: String,
    action: String = "Все",
    onAction: () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TomiloText,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text("$action  >", color = TomiloPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PosterGrid(
    items: List<CatalogTitleDto>,
    columns: Int,
    onOpen: (CatalogTitleDto) -> Unit,
    compact: Boolean = false,
    subtitle: (CatalogTitleDto) -> String? = { null },
    trailing: (CatalogTitleDto) -> String? = { null },
    trailingAccent: Boolean = false,
) {
    Column(
        Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 14.dp),
    ) {
        items.chunked(columns).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { item ->
                    TitlePosterCard(
                        title = item.displayTitle(),
                        cover = item.coverPath(),
                        onClick = { onOpen(item) },
                        modifier = Modifier.weight(1f),
                        width = null,
                        type = ReaderMode.typeLabel(item.type),
                        rating = item.displayRating(),
                        status = item.status,
                        isAdult = item.isAdult == true,
                        compact = compact,
                        plain = true,
                        showCoverType = false,
                        showCoverChapter = false,
                        showFooterRating = false,
                        subtitle = subtitle(item),
                        footerTrailing = trailing(item),
                        footerTrailingAccent = trailingAccent,
                    )
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HomePremiumPromotionBanner(
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gold = TomiloPremium
    Surface(
        onClick = onOpenPremium,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = TomiloSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, gold.copy(alpha = 0.42f)),
    ) {
        Column(
            Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            gold.copy(alpha = 0.16f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(18.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(gold.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = gold,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Tomilo Premium",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = gold,
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(gold)
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        ) {
                            Text(
                                "VIP",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Без рекламы, безлимитный офлайн и скидка 20% в магазине",
                        color = TomiloMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "от 150 ₽ / мес",
                        color = gold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                    )
                    Text(
                        "Отменить можно в любой момент",
                        color = TomiloMuted,
                        fontSize = 11.sp,
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(gold)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        "Попробовать",
                        color = Color(0xFF1E1300),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeTitleCarousel(
    items: List<CatalogTitleDto>,
    catalogRepository: CatalogRepository,
    continueItems: List<HistoryEntryDto>,
    onDismiss: () -> Unit,
    onOpenTitle: (CatalogTitleDto) -> Unit,
    onReadChapter: (titleId: String, chapterId: String) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    var descriptions by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var opening by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage, items) {
        val item = items.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        val key = item.stableId().ifBlank { item.slug.orEmpty() }
        if (key.isBlank() || key in descriptions) return@LaunchedEffect
        val local = htmlToPlain(item.description)
        if (local.isNotBlank()) {
            descriptions = descriptions + (key to local)
            return@LaunchedEffect
        }
        val lookup = item.stableId().ifBlank { item.slug.orEmpty() }
        catalogRepository.title(lookup).onSuccess { detail ->
            descriptions = descriptions + (key to htmlToPlain(detail.description))
        }
    }

    fun readNow(item: CatalogTitleDto) {
        if (opening) return
        opening = true
        scope.launch {
            val titleId = item.stableId()
            val fromHistory = continueItems.firstOrNull { it.titleKey() == titleId }
                ?.chapterKey()
                ?.takeIf { it.isNotBlank() }
            val chapterId = fromHistory
                ?: catalogRepository.chapters(titleId).getOrNull()?.firstOrNull()?.stableId()
            opening = false
            if (!chapterId.isNullOrBlank()) onReadChapter(titleId, chapterId)
            else onOpenTitle(item)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(TomiloBg),
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val item = items[page]
            val key = item.stableId().ifBlank { item.slug.orEmpty() }
            HomeTitleCarouselPage(
                item = item,
                description = descriptions[key].orEmpty(),
                opening = opening,
                onBack = onDismiss,
                onRead = { readNow(item) },
                onOpenTitle = { onOpenTitle(item) },
            )
        }
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .fillMaxHeight(0.28f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            repeat(items.size) { index ->
                val active = index == pagerState.currentPage
                Box(
                    Modifier
                        .padding(vertical = 3.dp)
                        .size(if (active) 7.dp else 5.dp)
                        .clip(CircleShape)
                        .background(if (active) Color.White else Color.White.copy(alpha = 0.38f)),
                )
            }
        }
    }
}

@Composable
private fun HomeTitleCarouselPage(
    item: CatalogTitleDto,
    description: String,
    opening: Boolean,
    onBack: () -> Unit,
    onRead: () -> Unit,
    onOpenTitle: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        TomiloCoverImage(
            source = item.coverPath(),
            contentDescription = item.displayTitle(),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.38f),
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.42f),
                            Color.Black.copy(alpha = 0.92f),
                        ),
                    ),
                ),
        )
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                item.displayRating()?.takeIf { it > 0 }?.let { rating ->
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = TomiloPremium,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            formatRating(rating),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onRead,
                    enabled = !opening,
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TomiloPrimary),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                ) {
                    Text(
                        if (opening) "Открытие…" else "Читать сейчас",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                }
                Row(
                    Modifier
                        .height(44.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xCC2A2A2E))
                        .clickable(enabled = !opening, onClick = onOpenTitle)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Перейти на страницу тайтла",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.displayTitle(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item.type?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                ReaderMode.typeLabel(it),
                                color = TomiloPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        item.releaseYear?.let {
                            Text("$it", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
                        }
                    }
                }
                item.status?.takeIf { it.isNotBlank() }?.let { status ->
                    HomeStatusChip(status)
                }
            }

            if (description.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    description,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 7,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun htmlToPlain(raw: String?): String {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return ""
    return Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace('\u00A0', ' ')
        .trim()
}
