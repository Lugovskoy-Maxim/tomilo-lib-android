package ru.tomilo.lib.mobile.ui.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.data.api.PublicUserDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.DecoratedAvatar
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloText
import kotlin.math.floor
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenChat: (conversationId: String, title: String) -> Unit,
) {
    val me by authRepository.userFlow.collectAsState(initial = null)
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var user by remember { mutableStateOf<PublicUserDto?>(null) }
    var friendStatus by remember { mutableStateOf("none") }
    var friendActionLoading by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        loading = true
        error = null
        socialRepository.publicUser(userId)
            .onSuccess { user = it }
            .onFailure { error = it.message }
        loading = false
    }

    LaunchedEffect(userId, me?.stableId()) {
        if (me != null && me?.stableId() != userId) {
            friendStatus = socialRepository.friendStatus(userId).getOrDefault("none")
        }
    }

    fun sendFriendRequest() {
        scope.launch {
            friendActionLoading = true
            socialRepository.sendFriendRequest(userId)
                .onSuccess {
                    friendStatus = "pending_outgoing"
                    snackbar.showSnackbar("Заявка в друзья отправлена")
                }
                .onFailure { snackbar.showSnackbar(it.message ?: "Не удалось отправить заявку") }
            friendActionLoading = false
        }
    }

    fun removeFriend() {
        scope.launch {
            friendActionLoading = true
            socialRepository.removeFriend(userId)
                .onSuccess {
                    friendStatus = "none"
                    confirmRemove = false
                    snackbar.showSnackbar("Пользователь удалён из друзей")
                }
                .onFailure { snackbar.showSnackbar(it.message ?: "Не удалось удалить друга") }
            friendActionLoading = false
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Удалить из друзей?") },
            text = { Text("Личный чат сохранится, но для дружбы потребуется новая заявка.") },
            confirmButton = { TextButton(onClick = ::removeFriend) { Text("Удалить") } },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Отмена") } },
        )
    }

    Scaffold(
        containerColor = TomiloBg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(user?.username ?: "Профиль") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (me?.stableId()?.let { it != userId } == true) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    socialRepository.openConversationWith(userId)
                                        .onSuccess {
                                            onOpenChat(
                                                it.stableId(),
                                                user?.username ?: "Чат",
                                            )
                                        }
                                        .onFailure { error = it.message }
                                }
                            },
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Написать")
                        }
                    }
                },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        when {
            loading -> LoadingBox(Modifier.padding(padding))
            error != null && user == null -> Column(Modifier.padding(padding)) {
                ErrorBox(error ?: "Ошибка")
            }
            user != null -> user?.let { u ->
                PublicProfileContent(
                    user = u,
                    modifier = Modifier.padding(padding),
                    isAuthenticated = me != null,
                    isOwnProfile = me?.stableId() == userId,
                    friendStatus = friendStatus,
                    friendActionLoading = friendActionLoading,
                    onAddFriend = ::sendFriendRequest,
                    onRemoveFriend = { confirmRemove = true },
                    onOpenFriends = onOpenFriends,
                    onMessage = {
                        scope.launch {
                            socialRepository.openConversationWith(userId)
                                .onSuccess { onOpenChat(it.stableId(), u.username ?: "Чат") }
                                .onFailure { snackbar.showSnackbar(it.message ?: "Не удалось открыть чат") }
                        }
                    },
                    onLogin = onLogin,
                )
            }
        }
    }
}

@Composable
private fun PublicProfileContent(
    user: PublicUserDto,
    modifier: Modifier,
    isAuthenticated: Boolean,
    isOwnProfile: Boolean,
    friendStatus: String,
    friendActionLoading: Boolean,
    onAddFriend: () -> Unit,
    onRemoveFriend: () -> Unit,
    onOpenFriends: () -> Unit,
    onMessage: () -> Unit,
    onLogin: () -> Unit,
) {
    val premium = Premium.isActive(user.subscriptionExpiresAt)
    val accent = if (premium) TomiloPremium else TomiloPrimary
    val level = (user.level ?: 0).coerceAtLeast(0)
    val experience = (user.experience ?: 0).coerceAtLeast(0)
    val levelBase = publicLevelExperience(level)
    val nextLevel = publicLevelExperience(level + 1).coerceAtLeast(levelBase + 1)
    val levelProgress = ((experience - levelBase).coerceAtLeast(0).toFloat() / (nextLevel - levelBase))
        .coerceIn(0f, 1f)
    val decoration = user.decorations()
    val backgroundUrl = decoration?.backgroundUrl() ?: decoration?.cardUrl()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = TomiloSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.34f)),
        ) {
            Box {
                if (!backgroundUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = backgroundUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accent.copy(alpha = if (backgroundUrl == null) 0.20f else 0.42f),
                                    TomiloSurface.copy(alpha = if (backgroundUrl == null) 0.94f else 0.84f),
                                    TomiloSurface,
                                ),
                            ),
                        ),
                )
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DecoratedAvatar(
                            avatarUrl = user.avatar,
                            username = user.username,
                            decorations = decoration,
                            size = 92.dp,
                            ringColor = accent,
                        )
                        Spacer(Modifier.size(14.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    user.username ?: "Пользователь",
                                    color = TomiloText,
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (premium) {
                                    Spacer(Modifier.size(6.dp))
                                    Icon(
                                        Icons.Default.WorkspacePremium,
                                        contentDescription = "Premium",
                                        tint = TomiloPremium,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ProfileBadge(publicRankTitle(level), accent)
                                if (user.role.equals("admin", true) || user.role.equals("moderator", true)) {
                                    ProfileBadge(if (user.role.equals("admin", true)) "Администратор" else "Модератор", TomiloPrimary)
                                }
                            }
                            if (premium) {
                                Spacer(Modifier.height(6.dp))
                                Text("Tomilo Premium", color = TomiloPremium, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Уровень $level", color = TomiloText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("$experience XP", color = TomiloMuted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(7.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.12f)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(levelProgress)
                                .height(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.horizontalGradient(listOf(accent, TomiloPrimary))),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "До ${level + 1} уровня: ${(nextLevel - experience).coerceAtLeast(0)} XP",
                        color = TomiloMuted,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        if (!user.bio.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Surface(
                color = TomiloSurface2.copy(alpha = 0.72f),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TomiloBorder.copy(alpha = 0.62f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(15.dp)) {
                    Text("О себе", color = TomiloMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(user.bio.orEmpty(), color = TomiloText, fontSize = 15.sp, lineHeight = 21.sp)
                }
            }
        }

        if (!isOwnProfile) {
            Spacer(Modifier.height(12.dp))
            if (isAuthenticated) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FriendActionButton(
                        status = friendStatus,
                        loading = friendActionLoading,
                        onAdd = onAddFriend,
                        onRemove = onRemoveFriend,
                        onOpenFriends = onOpenFriends,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = onMessage,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = TomiloSurface2),
                    ) {
                        Icon(Icons.Default.ChatBubble, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(7.dp))
                        Text("Написать")
                    }
                }
            } else {
                Button(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(18.dp)) {
                    Text("Войти, чтобы связаться")
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        PublicSectionTitle("Активность чтения", "Статистика пользователя")
        Spacer(Modifier.height(10.dp))
        if (user.showStats == false && !isOwnProfile) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = TomiloSurface2.copy(alpha = 0.68f),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TomiloBorder.copy(alpha = 0.58f)),
            ) {
                Text(
                    "Пользователь скрыл подробную статистику",
                    color = TomiloMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PublicStatCard("Главы", "${user.chaptersRead ?: 0}", Modifier.weight(1f), Icons.AutoMirrored.Filled.MenuBook, accent)
                PublicStatCard("Тайтлы", "${user.titlesReadCount ?: 0}", Modifier.weight(1f), Icons.Default.AutoAwesome, accent)
                PublicStatCard("Завершено", "${user.completedTitlesCount ?: 0}", Modifier.weight(1f), Icons.Default.Star, accent)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PublicStatCard("Серия", "${user.currentStreak ?: 0} дн.", Modifier.weight(1f), Icons.Default.LocalFireDepartment, Color(0xFFFF7A45))
                PublicStatCard("Комментарии", "${user.commentsCount ?: 0}", Modifier.weight(1f), Icons.Default.ChatBubble, accent)
                PublicStatCard("Лайки", "${user.likesReceivedCount ?: 0}", Modifier.weight(1f), Icons.Default.Favorite, Color(0xFFEF5B67))
            }
            Spacer(Modifier.height(12.dp))
            RowStat("Время чтения", publicReadingTime(user.readingTimeMinutes ?: 0), Icons.Default.Bolt)
            RowStat("Лучшая серия", "${user.longestStreak ?: user.currentStreak ?: 0} дней", Icons.Default.LocalFireDepartment)
            RowStat("Оценок поставлено", "${user.ratingsCount ?: 0}", Icons.Default.Star)
        }

        user.createdAt?.takeIf { it.isNotBlank() }?.let { createdAt ->
            Spacer(Modifier.height(18.dp))
            RowStat("В Tomilo с", publicMemberSince(createdAt), Icons.Default.CalendarMonth)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ProfileBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.13f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.32f)),
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
    }
}

@Composable
private fun FriendActionButton(
    status: String,
    loading: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onOpenFriends: () -> Unit,
    modifier: Modifier,
) {
    val label = when (status) {
        "friends" -> "Вы друзья"
        "pending_outgoing" -> "Заявка отправлена"
        "pending_incoming" -> "Ответить"
        else -> "В друзья"
    }
    OutlinedButton(
        onClick = when (status) {
            "friends" -> onRemove
            "pending_outgoing", "pending_incoming" -> onOpenFriends
            else -> onAdd
        },
        enabled = !loading && status != "blocked",
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
        else Icon(if (status == "friends") Icons.Default.People else Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(7.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PublicSectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, color = TomiloText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = TomiloMuted, fontSize = 12.sp)
    }
}

@Composable
private fun PublicStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = TomiloPrimary,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TomiloSurface2)
            .border(1.dp, TomiloBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(5.dp))
        }
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(label, color = TomiloMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RowStat(label: String, value: String, icon: ImageVector? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(TomiloSurface2.copy(alpha = 0.70f))
            .border(1.dp, TomiloBorder.copy(alpha = 0.60f), RoundedCornerShape(15.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = TomiloPrimary, modifier = Modifier.size(19.dp))
            Spacer(Modifier.size(10.dp))
        }
        Text(label, color = TomiloMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

private fun publicLevelExperience(level: Int): Int =
    floor(100.0 * level.coerceAtLeast(0).toDouble().pow(1.5)).toInt()

private fun publicRankTitle(level: Int): String = when (level.coerceAtLeast(0)) {
    0 -> "Начинающий читатель"
    1 -> "Ученик боевых искусств"
    2 -> "Царство единого начала"
    3 -> "Царство двойственности"
    4 -> "Царство трёх начал"
    5 -> "Царство четырёх стихий"
    6 -> "Царство пяти стихий"
    7 -> "Царство шести направлений"
    8 -> "Царство семи созвездий"
    9 -> "Царство восьми пустынь"
    else -> "Царство девяти небес"
}

private fun publicReadingTime(minutes: Int): String = when {
    minutes <= 0 -> "0 мин"
    minutes < 60 -> "$minutes мин"
    else -> "${minutes / 60} ч ${minutes % 60} мин"
}

private fun publicMemberSince(value: String): String {
    val date = value.take(10)
    val parts = date.split('-')
    if (parts.size != 3) return date
    val month = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря",
    ).getOrNull(parts[1].toIntOrNull()?.minus(1) ?: -1) ?: return date
    return "${parts[2].trimStart('0')} $month ${parts[0]}"
}
