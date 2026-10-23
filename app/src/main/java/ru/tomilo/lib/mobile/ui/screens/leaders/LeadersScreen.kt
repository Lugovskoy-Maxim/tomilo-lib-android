package ru.tomilo.lib.mobile.ui.screens.leaders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.data.api.LeaderboardUserDto
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.ui.components.DecoratedAvatar
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LeaderboardSkeleton
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import java.text.NumberFormat
import java.util.Locale

private data class LeaderCategory(
    val id: String,
    val label: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
)

private data class LeaderPeriod(val id: String, val label: String)

private data class LeaderSelectorItem(val label: String, val icon: ImageVector? = null)

@Composable
private fun leaderCategories() = listOf(
    LeaderCategory("level", "Уровень", "Уровень и опыт", "Кто набрал больше всего опыта", Icons.AutoMirrored.Filled.TrendingUp, TomiloPrimary),
    LeaderCategory("chaptersRead", "Главы", "Прочитанные главы", "Самые активные читатели", Icons.Default.Bolt, Color(0xFF57C7B8)),
    LeaderCategory("ratings", "Оценки", "Оценки тайтлов", "Кто оценил больше всего историй", Icons.Default.Star, TomiloPremium),
    LeaderCategory("comments", "Комментарии", "Комментарии", "Самые активные участники обсуждений", Icons.Default.Forum, Color(0xFF9B8CFF)),
    LeaderCategory("streak", "Серия", "Серия активности", "Самая длинная текущая серия дней", Icons.Default.LocalFireDepartment, Color(0xFFFF7A59)),
    LeaderCategory("likesReceived", "Лайки", "Лайки на комментариях", "Авторы самых полезных комментариев", Icons.Default.Favorite, Color(0xFFF06E9C)),
    LeaderCategory("developmentHelp", "Помощь", "Помощь проекту", "Принятые предложения персонажей", Icons.Default.AutoAwesome, Color(0xFF62B8FF)),
    LeaderCategory("balance", "Монеты", "Накопленные монеты", "Рейтинг по балансу монет", Icons.Default.MonetizationOn, Color(0xFFFFB84D)),
)

private val periods = listOf(
    LeaderPeriod("week", "Неделя"),
    LeaderPeriod("month", "Месяц"),
    LeaderPeriod("all", "Всё время"),
)

private val Gold = Color(0xFFFFC857)
private val Silver = Color(0xFFC0C7D4)
private val Bronze = Color(0xFFCD7F32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadersScreen(
    socialRepository: SocialRepository,
    authRepository: AuthRepository,
    onBack: () -> Unit,
    onOpenUser: (userId: String) -> Unit,
) {
    var categoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var periodIndex by rememberSaveable { mutableIntStateOf(2) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showTeam by rememberSaveable { mutableStateOf(true) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var users by remember { mutableStateOf<List<LeaderboardUserDto>>(emptyList()) }
    var loadedKey by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    val leaderboardSnapshots = remember { mutableStateMapOf<String, List<LeaderboardUserDto>>() }
    val currentUser by authRepository.userFlow.collectAsState(initial = null)

    val category = leaderCategories()[categoryIndex]
    val supportsPeriod = category.id in setOf("chaptersRead", "ratings", "comments")
    val period = if (supportsPeriod) periods[periodIndex] else periods.last()
    val requestKey = "${category.id}:${period.id}"
    val hasCurrentResults = loadedKey == requestKey

    LaunchedEffect(requestKey, reload) {
        loading = true
        error = null
        var freshLoaded = false
        val snapshot = leaderboardSnapshots[requestKey]
        if (snapshot != null) {
            users = snapshot
            loadedKey = requestKey
        } else if (loadedKey != requestKey) {
            users = emptyList()
        }
        val cacheSnapshotJob: Job? = if (snapshot == null) {
            launch {
                socialRepository.leaderboardCacheSnapshot(category = category.id, period = period.id)
                    .onSuccess { cachedUsers ->
                        if (!freshLoaded && cachedUsers.isNotEmpty()) {
                            val cachedSnapshot = cachedUsers.distinctBy(LeaderboardUserDto::stableId)
                            users = cachedSnapshot
                            leaderboardSnapshots[requestKey] = cachedSnapshot
                            loadedKey = requestKey
                        }
                    }
            }
        } else {
            null
        }
        socialRepository.leaderboard(category = category.id, period = period.id)
            .onSuccess {
                freshLoaded = true
                users = it.distinctBy(LeaderboardUserDto::stableId)
                leaderboardSnapshots[requestKey] = users
                loadedKey = requestKey
            }
            .onFailure { error = it.message }
        cacheSnapshotJob?.join()
        loading = false
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Лидеры", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Рейтинг сообщества",
                            color = TomiloMuted,
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
                    IconButton(enabled = !loading, onClick = { reload += 1 }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить рейтинг")
                    }
                },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Gold.copy(alpha = 0.055f), TomiloBg, TomiloBg)),
                ),
        ) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)) {
                Text(category.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(category.description, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
            }
            CategorySelector(
                selectedIndex = categoryIndex,
                onSelected = {
                    categoryIndex = it
                    searchQuery = ""
                },
            )
            if (supportsPeriod) {
                PeriodSelector(
                    selectedIndex = periodIndex,
                    onSelected = { periodIndex = it },
                )
            }
            LeaderSearchControls(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                showTeam = showTeam,
                onToggleTeam = { showTeam = !showTeam },
            )

            when {
                !hasCurrentResults && error != null -> ErrorBox(error ?: "Не удалось загрузить рейтинг") { reload += 1 }
                !hasCurrentResults -> LeaderboardSkeleton()
                error != null && users.isEmpty() -> ErrorBox(error ?: "Не удалось загрузить рейтинг") { reload += 1 }
                users.isEmpty() -> EmptyState(
                    title = "Рейтинг пока пуст",
                    message = "За выбранный период ещё нет результатов. Попробуйте другой период или категорию.",
                    icon = Icons.Default.EmojiEvents,
                    illustration = ru.tomilo.lib.mobile.R.drawable.illust_success_mascot,
                )
                else -> {
                    val rankedUsers = users.mapIndexed { index, user -> (index + 1) to user }
                    val visibleUsers = rankedUsers.filter { (_, user) ->
                        val role = user.role.orEmpty().lowercase()
                        (showTeam || (role != "admin" && role != "moderator")) &&
                            user.username.orEmpty().contains(searchQuery.trim(), ignoreCase = true)
                    }
                    if (visibleUsers.isEmpty()) {
                        EmptyState(
                            title = if (searchQuery.isBlank()) "В этой категории пока никого нет" else "Никого не нашли",
                            message = if (searchQuery.isBlank()) "Попробуйте включить отображение команды или выбрать другую категорию." else "Попробуйте другой запрос.",
                            icon = if (searchQuery.isBlank()) Icons.Default.EmojiEvents else Icons.Default.Search,
                        )
                    } else {
                        LeaderboardContent(
                            users = visibleUsers,
                            allUsers = users,
                            category = category,
                            searchQuery = searchQuery,
                            currentUserId = currentUser?.stableId().orEmpty(),
                            onOpenUser = onOpenUser,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(selectedIndex: Int, onSelected: (Int) -> Unit) {
    LeaderSegmentedSelector(
        items = periods.map { LeaderSelectorItem(it.label) },
        selectedIndex = selectedIndex,
        onSelected = onSelected,
        equalWidth = true,
    )
}

@Composable
private fun CategorySelector(selectedIndex: Int, onSelected: (Int) -> Unit) {
    LeaderSegmentedSelector(
        items = leaderCategories().map { LeaderSelectorItem(it.label) },
        selectedIndex = selectedIndex,
        onSelected = onSelected,
        equalWidth = false,
    )
}

@Composable
private fun LeaderSegmentedSelector(
    items: List<LeaderSelectorItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    equalWidth: Boolean,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(shape)
            .background(TomiloSurface)
            .border(1.dp, TomiloBorder, shape),
    ) {
        val rowModifier = if (equalWidth) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.horizontalScroll(rememberScrollState())
        }
        Row(
            modifier = rowModifier.padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val selected = selectedIndex == index
                val itemModifier = if (equalWidth) Modifier.weight(1f) else Modifier.widthIn(min = 104.dp)
                Row(
                    modifier = itemModifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) TomiloPrimary.copy(alpha = 0.20f) else Color.Transparent)
                        .clickable { onSelected(index) }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item.icon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) TomiloPrimary else TomiloMuted,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = item.label,
                        color = if (selected) TomiloPrimary else TomiloMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderSearchControls(
    query: String,
    onQueryChange: (String) -> Unit,
    showTeam: Boolean,
    onToggleTeam: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = TomiloSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, TomiloBorder),
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = TomiloMuted, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 13.dp)) {
                    if (query.isBlank()) {
                        Text("Поиск по нику", color = TomiloMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Очистить поиск", tint = TomiloMuted)
                    }
                }
            }
        }
        FilterChip(
            selected = showTeam,
            onClick = onToggleTeam,
            label = { Text("Команда") },
            leadingIcon = {
                Icon(
                    if (showTeam) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = TomiloPrimary.copy(alpha = 0.12f),
                selectedLabelColor = TomiloPrimary,
                selectedLeadingIconColor = TomiloPrimary,
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = showTeam,
                borderColor = TomiloBorder,
                selectedBorderColor = TomiloPrimary.copy(alpha = 0.5f),
            ),
        )
    }
}

@Composable
private fun LeaderboardContent(
    users: List<Pair<Int, LeaderboardUserDto>>,
    allUsers: List<LeaderboardUserDto>,
    category: LeaderCategory,
    searchQuery: String,
    currentUserId: String,
    onOpenUser: (String) -> Unit,
) {
    val showPodium = searchQuery.isBlank()
    val top = if (showPodium) users.take(3) else emptyList()
    val rest = if (showPodium) users.drop(3) else users
    val currentRankedUser = allUsers.indexOfFirst { it.stableId() == currentUserId }
        .takeIf { it >= 0 }
        ?.let { (it + 1) to allUsers[it] }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (top.isNotEmpty()) {
            item(key = "top") {
                Podium(
                    top = top,
                    category = category,
                    onOpenUser = onOpenUser,
                )
            }
        }
        if (currentRankedUser != null && searchQuery.isBlank()) {
            item(key = "my_position") {
                PersonalPosition(
                    rank = currentRankedUser.first,
                    user = currentRankedUser.second,
                    category = category,
                )
            }
        }
        item(key = "list_note") {
            Text(
                "В списке ${users.size} участников${if (searchQuery.isBlank()) "" else " по запросу"}. Места сохраняют порядок общего рейтинга.",
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                color = TomiloMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (rest.isNotEmpty()) {
            item(key = "list_title") {
                Text(
                    if (searchQuery.isBlank()) "Остальные участники" else "Результаты поиска",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
                )
            }
        }
        itemsIndexed(
            items = rest,
            key = { index, ranked -> ranked.second.stableId().ifBlank { "leader_${ranked.first}_$index" } },
        ) { _, (rank, user) ->
            LeaderRow(
                rank = rank,
                user = user,
                category = category,
                isCurrentUser = user.stableId() == currentUserId,
                onClick = { user.stableId().takeIf(String::isNotBlank)?.let(onOpenUser) },
            )
        }
    }
}

@Composable
private fun Podium(
    top: List<Pair<Int, LeaderboardUserDto>>,
    category: LeaderCategory,
    onOpenUser: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        top.forEach { (rank, user) ->
            TopLeaderCard(
                rank = rank,
                user = user,
                category = category,
                onClick = { user.stableId().takeIf(String::isNotBlank)?.let(onOpenUser) },
            )
        }
    }
}

@Composable
private fun TopLeaderCard(
    rank: Int,
    user: LeaderboardUserDto,
    category: LeaderCategory,
    onClick: () -> Unit,
) {
    val decorationTextShadow = remember {
        Shadow(
            color = Color.Black.copy(alpha = 0.82f),
            offset = Offset(0f, 2f),
            blurRadius = 8f,
        )
    }
    val rankColor = when (rank) {
        1 -> Gold
        2 -> Silver
        else -> Bronze
    }
    val backgroundUrl = user.decorations()?.backgroundUrl()
    val backgroundModel = remember(backgroundUrl) {
        backgroundUrl
            ?.let { MediaUrl.decorationPreviewUrl(it, 640) }
            ?.let(MediaUrl::resolve)
    }
    val rankChange = user.rankChange?.day ?: user.rankChange?.week

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (rank == 1) TomiloPrimary.copy(alpha = 0.55f) else TomiloBorder,
        ),
    ) {
        Box {
            if (backgroundModel != null) {
                AsyncImage(
                    model = backgroundModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier
                        .width(48.dp)
                        .padding(horizontal = 5.dp, vertical = 4.dp),
                ) {
                    Text(
                        "#$rank",
                        color = rankColor,
                        style = MaterialTheme.typography.titleSmall.copy(shadow = decorationTextShadow),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        rankLabel(rank),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(shadow = decorationTextShadow),
                        maxLines = 1,
                    )
                }
                LeaderAvatar(user = user, size = 44, ringColor = rankColor)
                Spacer(Modifier.width(10.dp))
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    PremiumName(
                        user = user,
                        style = MaterialTheme.typography.titleSmall,
                        textColor = Color.White,
                        textShadow = decorationTextShadow,
                    )
                    Text(
                        "Уровень ${user.level ?: 1}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(shadow = decorationTextShadow),
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        metricValue(category.id, user),
                        color = if (rank == 1) Gold else Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(shadow = decorationTextShadow),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        metricUnit(category.id),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(shadow = decorationTextShadow),
                        maxLines = 1,
                    )
                    if (rankChange != null) {
                        Text(
                            when {
                                rankChange > 0 -> "↑ $rankChange"
                                rankChange < 0 -> "↓ ${-rankChange}"
                                else -> "Без изменений"
                            },
                            color = when {
                                rankChange > 0 -> Color(0xFF79E0A5)
                                rankChange < 0 -> Color(0xFFFF9999)
                                else -> Color.White.copy(alpha = 0.78f)
                            },
                            style = MaterialTheme.typography.labelSmall.copy(shadow = decorationTextShadow),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalPosition(rank: Int, user: LeaderboardUserDto, category: LeaderCategory) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = TomiloSurface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TomiloPrimary.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(TomiloPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Text("#" + rank, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text("Ваша позиция", color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
                PremiumName(user = user, style = MaterialTheme.typography.titleSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(metricValue(category.id, user), color = category.color, fontWeight = FontWeight.Bold)
                Text(metricUnit(category.id), color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun rankLabel(rank: Int): String = when (rank) {
    1 -> "1 место"
    2 -> "2 место"
    else -> "3 место"
}

@Composable
private fun LeaderRow(
    rank: Int,
    user: LeaderboardUserDto,
    category: LeaderCategory,
    isCurrentUser: Boolean,
    onClick: () -> Unit,
) {
    val isTopTen = rank in 4..10
    val isPremium = Premium.isActive(user.subscriptionExpiresAt)

    Surface(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp),
        color = when {
            isCurrentUser -> TomiloPrimary.copy(alpha = 0.08f)
            isTopTen -> TomiloSurface
            else -> TomiloSurface.copy(alpha = 0.75f)
        },
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                isCurrentUser -> TomiloPrimary.copy(alpha = 0.55f)
                isTopTen -> TomiloPrimary.copy(alpha = 0.18f)
                else -> TomiloBorder
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isTopTen) TomiloPrimary.copy(alpha = 0.15f) else TomiloSurface2)
                    .border(
                        1.dp,
                        if (isTopTen) TomiloPrimary.copy(alpha = 0.35f) else Color.Transparent,
                        RoundedCornerShape(11.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "#$rank",
                    color = if (isTopTen) TomiloPrimary else TomiloMuted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(12.dp))
            LeaderAvatar(
                user = user,
                size = 48,
                ringColor = if (isPremium) TomiloPremium else if (isTopTen) TomiloPrimary.copy(alpha = 0.6f) else TomiloBorder,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PremiumName(user = user, style = MaterialTheme.typography.titleSmall)
                    if (isPremium) {
                        Spacer(Modifier.width(5.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(TomiloPremium)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        ) {
                            Text("PRO", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TomiloSurface2)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "Ур. ${user.level ?: 1}${if (isCurrentUser) " · Вы" else ""}",
                            color = TomiloMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    metricValue(category.id, user),
                    color = category.color,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(metricUnit(category.id), color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LeaderAvatar(user: LeaderboardUserDto, size: Int, ringColor: Color) {
    DecoratedAvatar(
        avatarUrl = user.avatar,
        username = user.username,
        decorations = user.decorations(),
        size = size.dp,
        ringColor = ringColor,
    )
}

@Composable
private fun PremiumName(
    user: LeaderboardUserDto,
    style: androidx.compose.ui.text.TextStyle,
    textAlign: TextAlign? = null,
    textColor: Color? = null,
    textShadow: Shadow? = null,
) {
    Text(
        text = user.username?.takeIf(String::isNotBlank) ?: "Пользователь",
        color = textColor ?: if (Premium.isActive(user.subscriptionExpiresAt)) TomiloPremium else MaterialTheme.colorScheme.onSurface,
        style = style.copy(shadow = textShadow),
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
    )
}

private fun metricLine(category: String, user: LeaderboardUserDto): String =
    "${metricValue(category, user)} ${metricUnit(category)}".trim()

private fun metricValue(category: String, user: LeaderboardUserDto): String {
    val value = when (category) {
        "chaptersRead" -> user.chaptersRead
        "ratings" -> user.ratingsCount
        "comments" -> user.commentsCount
        "streak" -> user.currentStreak
        "likesReceived" -> user.likesReceivedCount
        "developmentHelp" -> user.charactersAcceptedCount
        "balance" -> user.balance
        else -> user.experience
    }
    return NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru-RU")).format(value ?: 0)
}

private fun metricUnit(category: String): String = when (category) {
    "chaptersRead" -> "глав"
    "ratings" -> "оценок"
    "comments" -> "комментариев"
    "streak" -> "дней подряд"
    "likesReceived" -> "лайков"
    "developmentHelp" -> "персонажей"
    "balance" -> "монет"
    else -> "опыта"
}
