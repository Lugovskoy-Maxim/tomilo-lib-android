package ru.tomilo.lib.mobile.ui.screens.leaders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
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

private val CATEGORIES = listOf(
    "level" to "Уровень",
    "chaptersRead" to "Главы",
    "readingTime" to "Время",
    "ratings" to "Оценки",
    "comments" to "Комменты",
    "streak" to "Стрик",
    "likesReceived" to "Лайки",
)

private val PERIODS = listOf(
    "all" to "Всё время",
    "month" to "Месяц",
    "week" to "Неделя",
)

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

    LaunchedEffect(catIndex, periodIndex) {
        loading = true
        error = null
        socialRepository.leaderboard(
            category = CATEGORIES[catIndex].first,
            period = PERIODS[periodIndex].first,
        ).onSuccess { users = it }
            .onFailure { error = it.message }
        loading = false
    }

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
            ScrollableTabRow(selectedTabIndex = catIndex, edgePadding = 8.dp, containerColor = TomiloBg, divider = {}) {
                CATEGORIES.forEachIndexed { i, p ->
                    FilterChip(
                        selected = catIndex == i,
                        onClick = { catIndex = i },
                        label = { Text(p.second) },
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            ScrollableTabRow(selectedTabIndex = periodIndex, edgePadding = 8.dp, containerColor = TomiloBg, divider = {}) {
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
                error != null && users.isEmpty() -> ErrorBox(error ?: "Ошибка") {
                    // retrigger
                    catIndex = catIndex
                }
                else -> LazyColumn(contentPadding = ScreenPadding) {
                    itemsIndexed(users, key = { _, u -> u.stableId() }) { index, u ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenUser(u.stableId()) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.width(32.dp),
                                color = TomiloMuted,
                            )
                            AsyncImage(
                                model = MediaUrl.resolve(u.avatar),
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
                                    Text(u.username ?: "user", style = MaterialTheme.typography.titleMedium)
                                    if (Premium.isActive(u.subscriptionExpiresAt)) {
                                        Spacer(Modifier.width(6.dp))
                                        Text("PRO", color = TomiloPremium, style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                                Text(
                                    metricLine(CATEGORIES[catIndex].first, u),
                                    color = TomiloMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text("Lv ${u.level ?: 0}", color = TomiloMuted)
                        }
                    }
                }
            }
        }
    }
}

private fun metricLine(category: String, u: LeaderboardUserDto): String = when (category) {
    "chaptersRead" -> "${u.chaptersRead ?: 0} глав"
    "readingTime" -> "${u.readingTimeMinutes ?: 0} мин"
    "ratings" -> "${u.ratingsCount ?: 0} оценок"
    "comments" -> "${u.commentsCount ?: 0} комм."
    "streak" -> "стрик ${u.currentStreak ?: 0}"
    "likesReceived" -> "${u.likesReceivedCount ?: 0} лайков"
    "balance" -> "${u.balance ?: 0} монет"
    else -> "опыт ${u.experience ?: 0}"
}
