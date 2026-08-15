package ru.tomilo.lib.mobile.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.R
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.api.CatalogTitleDto
import ru.tomilo.lib.mobile.data.local.ContentPrefs
import ru.tomilo.lib.mobile.data.local.ContentSettings
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.HistoryRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.data.api.HistoryEntryDto
import ru.tomilo.lib.mobile.ui.components.TitleSearchCard
import ru.tomilo.lib.mobile.ui.components.SwipeActionContainer
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.components.SectionTitle
import ru.tomilo.lib.mobile.ui.components.TitlePosterCard
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface

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
    var reloadToken by remember { mutableStateOf(0) }
    var continueItems by remember { mutableStateOf<List<HistoryEntryDto>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(user?.stableId(), reloadToken) {
        continueItems = if (user == null) emptyList() else historyRepository.history().getOrDefault(emptyList()).take(4)
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
        fun List<CatalogTitleDto>.filterAdult() =
            if (showAdult) this else filter { it.isAdult != true }
        updates = u.getOrDefault(emptyList()).filterAdult()
        popular = p.getOrDefault(emptyList()).filterAdult()
        loading = false
    }

    Scaffold(
        containerColor = TomiloBg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.logo_tomilo_ring),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Image(
                                painter = painterResource(R.drawable.logo_tomilo_wordmark),
                                contentDescription = "Tomilo",
                                modifier = Modifier
                                    .height(20.dp)
                                    .width(104.dp),
                                contentScale = ContentScale.Fit,
                            )
                            Text(
                                "Манга и манхва",
                                style = MaterialTheme.typography.labelSmall,
                                color = TomiloMuted,
                                fontWeight = FontWeight.Medium,
                            )
                        }
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
            loading -> LoadingBox(Modifier.padding(padding), message = "Загружаем ленту…")
            error != null && updates.isEmpty() && popular.isEmpty() -> {
                ErrorBox(error ?: "Ошибка") {
                    reloadToken += 1
                }
            }
            else -> {
                Column(
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(ScreenPadding),
                ) {
                    Text(
                        text = user?.username?.takeIf { it.isNotBlank() }?.let { "Рады видеть, $it" }
                            ?: "Откройте свою историю",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Читайте, общайтесь и получайте награды в tomilo-lib",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TomiloMuted,
                    )
                    Spacer(Modifier.height(16.dp))

                    popular.firstOrNull()?.let { leader ->
                        WeeklyHero(
                            item = leader,
                            onClick = { onOpenTitle(leader.stableId(), leader.slug) },
                        )
                        Spacer(Modifier.height(14.dp))
                    }

                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        QuickHomeAction("Колесо", "Испытать судьбу", Icons.Default.Casino, Color(0xFFFFC857), onOpenWheel)
                        QuickHomeAction("Задания", "Награды дня", Icons.Default.TaskAlt, Color(0xFF57C7B8), onOpenQuests)
                        QuickHomeAction("Друзья", "Общение", Icons.Default.Groups, Color(0xFF62B8FF), onOpenFriends)
                        QuickHomeAction("Офлайн", "Загрузки", Icons.Default.DownloadForOffline, Color(0xFF9B8CFF), onOpenOffline)
                        QuickHomeAction("Обновления", "Новые главы", Icons.Default.LocalFireDepartment, Color(0xFFF06E72), onOpenUpdates)
                        QuickHomeAction("Мне повезёт", "Случайный тайтл", Icons.Default.AutoAwesome, Color(0xFFFFB85C)) {
                            scope.launch {
                                catalogRepository.randomTitle(contentSettings.showAdultContent)
                                    .onSuccess { onOpenTitle(it.stableId(), it.slug) }
                                    .onFailure { error = it.message }
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))

                    if (continueItems.isNotEmpty()) {
                        SectionTitle(
                            text = "Продолжить чтение",
                            actionLabel = "История",
                            onAction = onOpenHistory,
                        )
                        continueItems.forEach { history ->
                            val continueTitleId = history.titleKey()
                            SwipeActionContainer(
                                actionLabel = "В закладки",
                                actionIcon = Icons.Outlined.BookmarkAdd,
                                actionColor = TomiloPrimary,
                                enabled = user != null && continueTitleId.isNotBlank(),
                                onAction = {
                                    scope.launch {
                                        socialRepository.addBookmark(continueTitleId, "reading")
                                            .onSuccess { snackbar.showSnackbar("Добавлено в «Читаю»") }
                                            .onFailure {
                                                snackbar.showSnackbar(
                                                    it.message ?: "Не удалось добавить в закладки",
                                                )
                                            }
                                    }
                                },
                            ) {
                                TitleSearchCard(
                                    title = history.displayTitle(),
                                    cover = history.coverPath(),
                                    type = history.type(),
                                    subtitle = history.chapterLabel(),
                                    progressLine = "Продолжить с последней страницы",
                                    onClick = {
                                    val titleId = history.titleKey()
                                    val chapterId = history.chapterKey()
                                    if (titleId.isNotBlank() && chapterId.isNotBlank()) {
                                        onContinueReading(titleId, chapterId)
                                    }
                                },
                                    secondaryActionIcon = Icons.Outlined.Info,
                                    secondaryActionDescription = "Открыть страницу тайтла",
                                    onSecondaryAction = {
                                        val titleId = history.titleKey()
                                        if (titleId.isNotBlank()) {
                                            onOpenTitle(titleId, history.slug())
                                        }
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    // Catalog CTA — glass pill like site cards
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(TomiloSurface)
                            .border(1.dp, TomiloPrimary.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                            .clickable(onClick = onOpenCatalog)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(TomiloPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = null,
                                tint = TomiloPrimary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Открыть каталог",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Поиск, фильтры и жанры",
                                style = MaterialTheme.typography.bodySmall,
                                color = TomiloMuted,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TomiloPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    if (updates.isNotEmpty()) {
                        SectionTitle(
                            text = "Обновления",
                            actionLabel = "Все",
                            onAction = onOpenUpdates,
                        )
                        Row(
                            Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            updates.forEach { item ->
                                TitlePosterCard(
                                    title = item.displayTitle(),
                                    cover = item.coverPath(),
                                    type = item.type,
                                    rating = item.displayRating(),
                                    totalChapters = item.totalChapters,
                                    chapterBadge = item.chapter,
                                    status = item.status,
                                    isAdult = item.isAdult == true,
                                    year = item.releaseYear,
                                    onClick = {
                                        onOpenTitle(item.stableId(), item.slug)
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (popular.isNotEmpty()) {
                        SectionTitle("Популярное")
                        Row(
                            Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            popular.forEach { item ->
                                TitlePosterCard(
                                    title = item.displayTitle(),
                                    cover = item.coverPath(),
                                    type = item.type,
                                    rating = item.displayRating(),
                                    totalChapters = item.totalChapters,
                                    status = item.status,
                                    isAdult = item.isAdult == true,
                                    year = item.releaseYear,
                                    onClick = {
                                        onOpenTitle(item.stableId(), item.slug)
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyHero(item: CatalogTitleDto, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(174.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(TomiloSurface)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = MediaUrl.resolve(item.coverPath()),
            contentDescription = item.displayTitle(),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.90f)),
                ),
            ),
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFF786B), modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("ТОП НЕДЕЛИ · #1", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                item.displayTitle(),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            Text(
                listOfNotNull(item.type?.takeIf { it.isNotBlank() }, item.totalChapters?.let { "$it глав" }).joinToString(" · "),
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun QuickHomeAction(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(132.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(TomiloSurface)
            .border(1.dp, color.copy(alpha = 0.20f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(13.dp),
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = color, modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.height(10.dp))
        Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        Text(subtitle, color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
    }
}
