package ru.tomilo.lib.mobile.ui.screens.leaders

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Forum
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.data.api.LeaderboardUserDto
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.components.PageIntro
import ru.tomilo.lib.mobile.ui.components.StatusPill
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

private data class LeaderCategory(
    val id: String,
    val label: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
)

private data class LeaderPeriod(val id: String, val label: String)

private val categories = listOf(
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
    onBack: () -> Unit,
    onOpenUser: (userId: String) -> Unit,
) {
    var categoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var periodIndex by rememberSaveable { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var users by remember { mutableStateOf<List<LeaderboardUserDto>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }

    val category = categories[categoryIndex]
    val period = periods[periodIndex]

    LaunchedEffect(category.id, period.id, reload) {
        loading = true
        error = null
        socialRepository.leaderboard(category = category.id, period = period.id)
            .onSuccess { users = it.distinctBy(LeaderboardUserDto::stableId) }
            .onFailure {
                users = emptyList()
                error = it.message
            }
        loading = false
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Лидеры")
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
                .fillMaxSize(),
        ) {
            PageIntro(
                title = "Лучшие читатели сообщества",
                subtitle = "${category.title} · ${period.label}",
                icon = Icons.Default.EmojiEvents,
                accent = Gold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                trailing = { if (!loading) StatusPill("${users.size} мест", Gold) },
            )
            PeriodSelector(
                selectedIndex = periodIndex,
                onSelected = { periodIndex = it },
            )
            CategorySelector(
                selectedIndex = categoryIndex,
                onSelected = { categoryIndex = it },
            )
            CategorySummary(category = category, period = period)

            when {
                loading -> LoadingBox(message = "Обновляем рейтинг…")
                error != null -> ErrorBox(error ?: "Не удалось загрузить рейтинг") { reload += 1 }
                users.isEmpty() -> EmptyState(
                    title = "Рейтинг пока пуст",
                    message = "За выбранный период ещё нет результатов. Попробуйте другой период или категорию.",
                    icon = Icons.Default.EmojiEvents,
                )
                else -> LeaderboardContent(
                    users = users,
                    category = category,
                    onOpenUser = onOpenUser,
                )
            }
        }
    }
}

@Composable
private fun PeriodSelector(selectedIndex: Int, onSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(TomiloSurface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        periods.forEachIndexed { index, period ->
            val selected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) TomiloPrimary.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelected(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    period.label,
                    color = if (selected) TomiloPrimary else TomiloMuted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(selectedIndex: Int, onSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEachIndexed { index, category ->
            val selected = selectedIndex == index
            FilterChip(
                selected = selected,
                onClick = { onSelected(index) },
                label = { Text(category.label) },
                leadingIcon = {
                    Icon(
                        category.icon,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = category.color.copy(alpha = 0.16f),
                    selectedLabelColor = category.color,
                    selectedLeadingIconColor = category.color,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = TomiloBorder,
                    selectedBorderColor = category.color.copy(alpha = 0.5f),
                ),
            )
        }
    }
}

@Composable
private fun CategorySummary(category: LeaderCategory, period: LeaderPeriod) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(category.color.copy(alpha = 0.09f))
            .border(1.dp, category.color.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(category.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(category.icon, contentDescription = null, tint = category.color, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(category.title, style = MaterialTheme.typography.titleMedium)
            Text(category.description, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            period.label,
            color = category.color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun LeaderboardContent(
    users: List<LeaderboardUserDto>,
    category: LeaderCategory,
    onOpenUser: (String) -> Unit,
) {
    val top = users.take(3)
    val rest = users.drop(3)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "top") {
            Podium(
                top = top,
                category = category,
                onOpenUser = onOpenUser,
            )
        }
        if (rest.isNotEmpty()) {
            item(key = "list_title") {
                Text(
                    "Остальные участники",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp),
                )
            }
        }
        itemsIndexed(
            items = rest,
            key = { index, user -> user.stableId().ifBlank { "leader_${index + 4}" } },
        ) { index, user ->
            LeaderRow(
                rank = index + 4,
                user = user,
                category = category,
                onClick = { user.stableId().takeIf(String::isNotBlank)?.let(onOpenUser) },
            )
        }
    }
}

@Composable
private fun Podium(
    top: List<LeaderboardUserDto>,
    category: LeaderCategory,
    onOpenUser: (String) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        top.firstOrNull()?.let { winner ->
            WinnerCard(
                user = winner,
                category = category,
                onClick = { winner.stableId().takeIf(String::isNotBlank)?.let(onOpenUser) },
            )
        }
        if (top.size > 1) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                top.drop(1).forEachIndexed { index, user ->
                    RunnerCard(
                        rank = index + 2,
                        user = user,
                        category = category,
                        onClick = { user.stableId().takeIf(String::isNotBlank)?.let(onOpenUser) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (top.size == 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WinnerCard(user: LeaderboardUserDto, category: LeaderCategory, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Gold.copy(alpha = 0.10f),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.38f)),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LeaderAvatar(user = user, size = 74, ringColor = Gold)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("1 место", color = Gold, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(5.dp))
                PremiumName(user = user, style = MaterialTheme.typography.titleLarge)
                Text(metricLine(category.id, user), color = category.color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Text("Lv ${user.level ?: 0}", color = TomiloMuted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun RunnerCard(
    rank: Int,
    user: LeaderboardUserDto,
    category: LeaderCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (rank == 2) Silver else Bronze
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = TomiloSurface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(3.dp))
                Text("$rank место", color = accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(9.dp))
            LeaderAvatar(user = user, size = 58, ringColor = accent)
            Spacer(Modifier.height(8.dp))
            PremiumName(user = user, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                metricLine(category.id, user),
                color = category.color,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LeaderRow(
    rank: Int,
    user: LeaderboardUserDto,
    category: LeaderCategory,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp),
        color = TomiloSurface.copy(alpha = 0.78f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TomiloSurface2),
                contentAlignment = Alignment.Center,
            ) {
                Text(rank.toString(), color = TomiloMuted, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            LeaderAvatar(user = user, size = 46, ringColor = TomiloBorder)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                PremiumName(user = user, style = MaterialTheme.typography.titleMedium)
                Text("Уровень ${user.level ?: 0}", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    metricValue(category.id, user),
                    color = category.color,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(metricUnit(category.id), color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LeaderAvatar(user: LeaderboardUserDto, size: Int, ringColor: Color) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .border(2.dp, ringColor, CircleShape)
            .padding(3.dp)
            .clip(CircleShape)
            .background(TomiloSurface2),
    ) {
        AsyncImage(
            model = MediaUrl.resolve(user.avatar),
            contentDescription = user.username,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PremiumName(
    user: LeaderboardUserDto,
    style: androidx.compose.ui.text.TextStyle,
    textAlign: TextAlign? = null,
) {
    Text(
        text = user.username?.takeIf(String::isNotBlank) ?: "Пользователь",
        color = if (Premium.isActive(user.subscriptionExpiresAt)) TomiloPremium else MaterialTheme.colorScheme.onSurface,
        style = style,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
    )
}

private fun metricLine(category: String, user: LeaderboardUserDto): String =
    "${metricValue(category, user)} ${metricUnit(category)}".trim()

private fun metricValue(category: String, user: LeaderboardUserDto): String = when (category) {
    "chaptersRead" -> (user.chaptersRead ?: 0).toString()
    "ratings" -> (user.ratingsCount ?: 0).toString()
    "comments" -> (user.commentsCount ?: 0).toString()
    "streak" -> (user.currentStreak ?: 0).toString()
    "likesReceived" -> (user.likesReceivedCount ?: 0).toString()
    "developmentHelp" -> (user.charactersAcceptedCount ?: 0).toString()
    "balance" -> (user.balance ?: 0).toString()
    else -> (user.experience ?: 0).toString()
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
