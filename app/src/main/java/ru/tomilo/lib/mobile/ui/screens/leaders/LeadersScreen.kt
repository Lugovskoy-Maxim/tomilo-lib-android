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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.data.api.LeaderboardUserDto
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.DecoratedAvatar
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LeaderboardSkeleton
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
                loading -> LeaderboardSkeleton()
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
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(TomiloSurface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        periods.forEachIndexed { index, period ->
            val selected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) TomiloPrimary.copy(alpha = 0.20f) else Color.Transparent)
                    .clickable { onSelected(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    period.label,
                    color = if (selected) TomiloPrimary else TomiloMuted,
                    style = MaterialTheme.typography.bodyMedium,
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(category.color.copy(alpha = 0.09f))
            .border(1.dp, category.color.copy(alpha = 0.24f), RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(category.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(category.icon, contentDescription = null, tint = category.color, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(category.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
    if (top.isEmpty()) return

    val first = top.getOrNull(0)
    val second = top.getOrNull(1)
    val third = top.getOrNull(2)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        color = TomiloSurface.copy(alpha = 0.65f),
        shape = RoundedCornerShape(26.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Gold.copy(alpha = 0.12f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                        ),
                    ),
                )
                .padding(top = 16.dp, bottom = 14.dp, start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Gold, modifier = Modifier.size(17.dp))
                Text(
                    "ПЬЕДЕСТАЛ ПОЧЁТА",
                    color = Gold,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Olympic Podium: [2nd Silver] [1st Gold Center] [3rd Bronze]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                if (second != null) {
                    PodiumPillar(
                        rank = 2,
                        user = second,
                        category = category,
                        accentColor = Silver,
                        pedestalHeight = 68.dp,
                        avatarSize = 58,
                        onClick = { second.stableId().takeIf(String::isNotBlank)?.let(onOpenUser) },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }

                if (first != null) {
                    PodiumPillar(
                        rank = 1,
                        user = first,
                        category = category,
                        accentColor = Gold,
                        pedestalHeight = 96.dp,
                        avatarSize = 74,
                        isWinner = true,
                        onClick = { first.stableId().takeIf(String::isNotBlank)?.let(onOpenUser) },
                        modifier = Modifier.weight(1.18f),
                    )
                }

                if (third != null) {
                    PodiumPillar(
                        rank = 3,
                        user = third,
                        category = category,
                        accentColor = Bronze,
                        pedestalHeight = 52.dp,
                        avatarSize = 54,
                        onClick = { third.stableId().takeIf(String::isNotBlank)?.let(onOpenUser) },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PodiumPillar(
    rank: Int,
    user: LeaderboardUserDto,
    category: LeaderCategory,
    accentColor: Color,
    pedestalHeight: Dp,
    avatarSize: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isWinner: Boolean = false,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isWinner) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFFFDF00), Color(0xFFD4AF37))))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFF261800), modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("ТОП 1", color = Color(0xFF261800), fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(4.dp))
        } else {
            Box(
                Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(accentColor.copy(alpha = 0.22f))
                    .border(0.8.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(99.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text("#$rank", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(4.dp))
        }

        LeaderAvatar(user = user, size = avatarSize, ringColor = accentColor)
        Spacer(Modifier.height(6.dp))

        PremiumName(
            user = user,
            style = if (isWinner) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        Text(
            text = metricLine(category.id, user),
            color = if (isWinner) Gold else category.color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        val pedestalGradient = when (rank) {
            1 -> Brush.verticalGradient(listOf(Color(0xFF8A6508), Color(0xFF382700)))
            2 -> Brush.verticalGradient(listOf(Color(0xFF4B5563), Color(0xFF1F2937)))
            else -> Brush.verticalGradient(listOf(Color(0xFF6E3917), Color(0xFF291508)))
        }
        val pedestalBorder = when (rank) {
            1 -> Color(0xFFFFD700).copy(alpha = 0.65f)
            2 -> Color(0xFFD1D5DB).copy(alpha = 0.45f)
            else -> Color(0xFFCD7F32).copy(alpha = 0.45f)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(pedestalHeight)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(pedestalGradient)
                .border(1.dp, pedestalBorder, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$rank",
                    fontSize = if (isWinner) 28.sp else 22.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                )
                Text(
                    text = "Lv ${user.level ?: 1}",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                )
            }
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
    val isTopTen = rank in 4..10
    val isPremium = Premium.isActive(user.subscriptionExpiresAt)

    Surface(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp),
        color = if (isTopTen) TomiloSurface else TomiloSurface.copy(alpha = 0.75f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isTopTen) TomiloPrimary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.055f),
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
                            "Ур. ${user.level ?: 1}",
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
