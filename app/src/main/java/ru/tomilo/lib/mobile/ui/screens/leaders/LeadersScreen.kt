package ru.tomilo.lib.mobile.ui.screens.leaders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
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
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

/** Как на сайте: level, chaptersRead, ratings, comments, streak, likesReceived, developmentHelp, balance */
private val CATEGORIES = listOf(
    "level" to "Уровень",
    "chaptersRead" to "Главы",
    "ratings" to "Оценки",
    "comments" to "Комменты",
    "streak" to "Серия",
    "likesReceived" to "Лайки",
    "developmentHelp" to "Помощь",
    "balance" to "Монеты",
)

/** На сайте: week → month → all */
private val PERIODS = listOf(
    "week" to "Неделя",
    "month" to "Месяц",
    "all" to "Всё время",
)

private val Gold = Color(0xFFFFC857)
private val Silver = Color(0xFFC0C7D4)
private val Bronze = Color(0xFFCD7F32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadersScreen(
    socialRepository: SocialRepository,
    onOpenUser: (userId: String) -> Unit,
) {
    var catIndex by remember { mutableIntStateOf(0) }
    var periodIndex by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var users by remember { mutableStateOf<List<LeaderboardUserDto>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }

    val category = CATEGORIES[catIndex].first
    val period = PERIODS[periodIndex].first

    LaunchedEffect(catIndex, periodIndex, reload) {
        loading = true
        error = null
        socialRepository.leaderboard(category = category, period = period)
            .onSuccess { users = it }
            .onFailure { error = it.message }
        loading = false
    }

    val top3 = users.take(3)
    val rest = users.drop(3)

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = { Text("Лидеры") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TomiloBg),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = catIndex,
                edgePadding = 8.dp,
                containerColor = TomiloBg,
                divider = {},
            ) {
                CATEGORIES.forEachIndexed { i, p ->
                    FilterChip(
                        selected = catIndex == i,
                        onClick = { catIndex = i },
                        label = { Text(p.second) },
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            ScrollableTabRow(
                selectedTabIndex = periodIndex,
                edgePadding = 8.dp,
                containerColor = TomiloBg,
                divider = {},
            ) {
                PERIODS.forEachIndexed { i, p ->
                    FilterChip(
                        selected = periodIndex == i,
                        onClick = { periodIndex = i },
                        label = { Text(p.second) },
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            when {
                loading -> LoadingBox()
                error != null && users.isEmpty() -> ErrorBox(error ?: "Ошибка") { reload += 1 }
                users.isEmpty() -> Text(
                    "Пока нет данных за выбранный период",
                    color = TomiloMuted,
                    modifier = Modifier.padding(24.dp),
                )
                else -> LazyColumn(contentPadding = ScreenPadding) {
                    if (top3.isNotEmpty()) {
                        item(key = "podium") {
                            PodiumRow(
                                top = top3,
                                category = category,
                                onOpenUser = onOpenUser,
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    itemsIndexed(rest, key = { _, u -> u.stableId() }) { index, u ->
                        val rank = index + 4
                        LeaderRow(
                            rank = rank,
                            user = u,
                            category = category,
                            onClick = { onOpenUser(u.stableId()) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumRow(
    top: List<LeaderboardUserDto>,
    category: String,
    onOpenUser: (String) -> Unit,
) {
    // Порядок как на сайте: 2 | 1 | 3 визуально, но компактно в ряд 1-2-3 с акцентом на 1
    val ordered = listOfNotNull(top.getOrNull(1), top.getOrNull(0), top.getOrNull(2))
    val ranks = listOf(
        top.getOrNull(1)?.let { 2 },
        top.getOrNull(0)?.let { 1 },
        top.getOrNull(2)?.let { 3 },
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        ordered.forEachIndexed { i, u ->
            val rank = ranks[i] ?: (i + 1)
            val accent = when (rank) {
                1 -> Gold
                2 -> Silver
                else -> Bronze
            }
            val avatarSize = if (rank == 1) 72.dp else 56.dp
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TomiloSurface2.copy(alpha = 0.55f))
                    .clickable { onOpenUser(u.stableId()) }
                    .padding(vertical = 12.dp, horizontal = 6.dp),
            ) {
                Text(
                    when (rank) {
                        1 -> "👑 1"
                        2 -> "🥈 2"
                        else -> "🥉 3"
                    },
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .size(avatarSize)
                        .border(2.dp, accent, CircleShape)
                        .clip(CircleShape)
                        .background(TomiloSurface2),
                ) {
                    AsyncImage(
                        model = MediaUrl.resolve(u.avatar),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    u.username ?: "user",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = if (Premium.isActive(u.subscriptionExpiresAt)) TomiloPremium
                    else MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    metricLine(category, u),
                    color = TomiloMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun LeaderRow(
    rank: Int,
    user: LeaderboardUserDto,
    category: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$rank",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(32.dp),
            color = TomiloMuted,
        )
        AsyncImage(
            model = MediaUrl.resolve(user.avatar),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(TomiloSurface2),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(user.username ?: "user", style = MaterialTheme.typography.titleMedium)
                if (Premium.isActive(user.subscriptionExpiresAt)) {
                    Spacer(Modifier.width(6.dp))
                    Text("PRO", color = TomiloPremium, style = MaterialTheme.typography.labelLarge)
                }
            }
            Text(
                metricLine(category, user),
                color = TomiloMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text("Lv ${user.level ?: 0}", color = TomiloMuted)
    }
}

private fun metricLine(category: String, u: LeaderboardUserDto): String = when (category) {
    "chaptersRead" -> "${u.chaptersRead ?: 0} глав"
    "ratings" -> "${u.ratingsCount ?: 0} оценок"
    "comments" -> "${u.commentsCount ?: 0} комм."
    "streak" -> {
        val s = u.currentStreak ?: 0
        val days = when {
            s == 1 -> "день"
            s in 2..4 -> "дня"
            else -> "дней"
        }
        "$s $days 🔥"
    }
    "likesReceived" -> "${u.likesReceivedCount ?: 0} лайков"
    "developmentHelp" -> "${u.charactersAcceptedCount ?: 0} перс."
    "balance" -> "${u.balance ?: 0} монет"
    else -> "ур. ${u.level ?: 0} · опыт ${u.experience ?: 0}"
}
