package ru.tomilo.lib.mobile.ui.screens.user

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.core.toUserFacingError
import ru.tomilo.lib.mobile.data.api.PublicUserDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.DecoratedAvatar
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.components.ProfileDecorationLayer
import ru.tomilo.lib.mobile.ui.components.profileHeaderGlass
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloOnPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface3
import ru.tomilo.lib.mobile.ui.theme.TomiloText
import kotlin.math.floor
import kotlin.math.pow

private val PublicProfileTabs = listOf("Профиль", "Тайтлы", "Лента", "Комментарии", "Посты")

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
    val context = LocalContext.current
    val me by authRepository.userFlow.collectAsState(initial = null)
    var reload by remember(userId) { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var user by remember { mutableStateOf<PublicUserDto?>(null) }
    var friendStatus by remember { mutableStateOf("none") }
    var friendActionLoading by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId, reload) {
        user = null
        friendStatus = "none"
        loading = true
        error = null
        try {
            socialRepository.publicUser(userId)
                .onSuccess { user = it }
                .onFailure { error = it.toUserFacingError("Не удалось загрузить профиль пользователя.") }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            error = failure.toUserFacingError("Не удалось загрузить профиль пользователя.")
        } finally {
            loading = false
        }
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
                .onFailure {
                    snackbar.showSnackbar(it.toUserFacingError("Не удалось отправить заявку в друзья."))
                }
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
                .onFailure {
                    snackbar.showSnackbar(it.toUserFacingError("Не удалось удалить пользователя из друзей."))
                }
            friendActionLoading = false
        }
    }

    fun openChat(username: String?) {
        scope.launch {
            socialRepository.openConversationWith(userId)
                .onSuccess { onOpenChat(it.stableId(), username ?: "Чат") }
                .onFailure { snackbar.showSnackbar(it.toUserFacingError("Не удалось открыть чат.")) }
        }
    }

    fun share() {
        val url = "${MediaUrl.siteOrigin()}/user/$userId"
        val name = user?.username?.takeIf { it.isNotBlank() }
        val text = if (name == null) url else "$name — $url"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, "Поделиться профилем"))
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
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            when {
                loading -> {
                    LoadingBox()
                    ProfileCoverActions(onBack = onBack, onShare = null)
                }
                error != null && user == null -> {
                    Column(Modifier.fillMaxSize().statusBarsPadding()) {
                        ProfileCoverActions(onBack = onBack, onShare = null)
                        ErrorBox(error ?: "Не удалось загрузить профиль пользователя.", onRetry = { reload += 1 })
                    }
                }
                user != null -> user?.let { profile ->
                    PublicProfileContent(
                        user = profile,
                        isAuthenticated = me != null,
                        isOwnProfile = me?.stableId() == userId,
                        friendStatus = friendStatus,
                        friendActionLoading = friendActionLoading,
                        onBack = onBack,
                        onShare = ::share,
                        onAddFriend = ::sendFriendRequest,
                        onRemoveFriend = { confirmRemove = true },
                        onOpenFriends = onOpenFriends,
                        onMessage = { openChat(profile.username) },
                        onLogin = onLogin,
                    )
                }
            }
        }
    }
}

@Composable
private fun PublicProfileContent(
    user: PublicUserDto,
    isAuthenticated: Boolean,
    isOwnProfile: Boolean,
    friendStatus: String,
    friendActionLoading: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onAddFriend: () -> Unit,
    onRemoveFriend: () -> Unit,
    onOpenFriends: () -> Unit,
    onMessage: () -> Unit,
    onLogin: () -> Unit,
) {
    val premium = Premium.isActive(user.subscriptionExpiresAt)
    val level = (user.level ?: 0).coerceAtLeast(0)
    val experience = (user.experience ?: 0).coerceAtLeast(0)
    val levelBase = publicLevelExperience(level)
    val nextLevel = publicLevelExperience(level + 1).coerceAtLeast(levelBase + 1)
    val needed = (nextLevel - levelBase).coerceAtLeast(1)
    val gained = (experience - levelBase).coerceAtLeast(0).coerceAtMost(needed)
    val levelProgress = (gained.toFloat() / needed.toFloat()).coerceIn(0f, 1f)
    val decoration = user.decorations()
    val backgroundUrl = decoration?.backgroundUrl() ?: decoration?.cardUrl()
    val roleLabel = publicRoleLabel(user.role)
    val rankTitle = publicRankTitle(level)
    val statsHidden = user.showStats == false && !isOwnProfile
    var tab by remember(user.stableId()) { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val bannerHeight = maxWidth * (339f / 393f)
            val cardTop = maxWidth * (188f / 393f)
            ProfileDecorationLayer(
                imageUrl = backgroundUrl,
                modifier = Modifier.fillMaxWidth().height(bannerHeight),
            )
            Column(
                Modifier
                    .padding(start = 12.dp, end = 12.dp, top = cardTop)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(profileHeaderGlass())
                    .border(1.dp, TomiloPrimary.copy(alpha = 0.30f), RoundedCornerShape(24.dp))
                    .padding(14.dp),
            ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DecoratedAvatar(
                            avatarUrl = user.avatar,
                            username = user.username,
                            decorations = decoration,
                            size = 64.dp,
                            ringColor = TomiloPrimary,
                        )
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    user.username ?: "Пользователь",
                                    color = TomiloText,
                                    fontSize = 18.sp,
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
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            ProfileBadge(roleLabel ?: rankTitle, TomiloPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Ур. $level · $rankTitle",
                                color = TomiloMuted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = TomiloPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Surface(color = TomiloPrimary, shape = RoundedCornerShape(9.dp)) {
                            Text(
                                "Уровень $level",
                                color = TomiloOnPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "$gained/$needed XP  ${(levelProgress * 100).toInt()}%",
                            color = TomiloMuted,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(TomiloSurface3),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(levelProgress)
                                .height(7.dp)
                                .clip(CircleShape)
                                .background(TomiloPrimary),
                        )
                    }

                    if (!isOwnProfile) {
                        Spacer(Modifier.height(14.dp))
                        if (isAuthenticated) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FriendActionButton(
                                    status = friendStatus,
                                    loading = friendActionLoading,
                                    onAdd = onAddFriend,
                                    onRemove = onRemoveFriend,
                                    onOpenFriends = onOpenFriends,
                                    modifier = Modifier.weight(1f),
                                )
                                Surface(
                                    onClick = onMessage,
                                    shape = CircleShape,
                                    color = TomiloSurface3,
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.ChatBubble, contentDescription = "Написать", tint = TomiloText)
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = onLogin,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TomiloPrimary,
                                    contentColor = TomiloOnPrimary,
                                ),
                            ) {
                                Text("Войти, чтобы связаться")
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    if (statsHidden) {
                        Text(
                            "Пользователь скрыл подробную статистику",
                            color = TomiloMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        )
                    } else {
                        ProfileStatGrid(
                            listOf(
                                compactCount(user.chaptersRead ?: 0) to "Глав прочитано",
                                compactCount(user.titlesReadCount ?: 0) to "Тайтлы",
                                compactCount(user.likesReceivedCount ?: 0) to "Лайки",
                                compactCount(user.commentsCount ?: 0) to "Комментарии",
                                compactCount(user.completedTitlesCount ?: 0) to "Завершено",
                                compactCount(user.currentStreak ?: 0) to "Серия",
                            ),
                        )
                    }
            }
            ProfileCoverActions(onBack = onBack, onShare = onShare)
        }

        Column(Modifier.padding(horizontal = 12.dp)) {
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PublicProfileTabs.forEachIndexed { index, label ->
                    Text(
                        label,
                        color = if (tab == index) TomiloText else TomiloMuted,
                        fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clickable { tab = index }
                            .padding(vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            when (tab) {
                0 -> ProfileAboutTab(
                    user = user,
                    roleLabel = roleLabel,
                    statsHidden = statsHidden,
                )
                1 -> ProfileEmptyTab("Список тайтлов этого профиля пока не открыт")
                2 -> ProfileEmptyTab("Лента появится позже")
                3 -> ProfileEmptyTab("Комментарии пользователя пока не открыты")
                else -> ProfileEmptyTab("Посты появятся позже")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileCoverActions(onBack: () -> Unit, onShare: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Назад", onBack)
        if (onShare != null) {
            CoverIconButton(Icons.Default.Share, "Поделиться", onShare)
        } else {
            Spacer(Modifier.size(40.dp))
        }
    }
}

@Composable
private fun CoverIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.38f),
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ProfileStatGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (value, label) ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(TomiloSurface3)
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(value, color = TomiloText, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            label,
                            color = TomiloMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAboutTab(user: PublicUserDto, roleLabel: String?, statsHidden: Boolean) {
    val bio = user.bio?.takeIf { it.isNotBlank() }
    if (bio != null) {
        Text(
            if (roleLabel == "Переводчики") "О команде" else "О себе",
            color = TomiloText,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(bio, color = TomiloText, fontSize = 15.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(14.dp))
    }
    if (!statsHidden) {
        RowStat("Время чтения", publicReadingTime(user.readingTimeMinutes ?: 0), Icons.Default.Bolt)
        RowStat("Лучшая серия", "${user.longestStreak ?: user.currentStreak ?: 0} дней", Icons.Default.LocalFireDepartment)
        RowStat("Оценок поставлено", "${user.ratingsCount ?: 0}", Icons.Default.Star)
    }
    user.createdAt?.takeIf { it.isNotBlank() }?.let { createdAt ->
        RowStat("В Tomilo с", publicMemberSince(createdAt), Icons.Default.CalendarMonth)
    }
    if (bio == null && statsHidden && user.createdAt.isNullOrBlank()) {
        ProfileEmptyTab("Пользователь пока ничего не рассказал о себе")
    }
}

@Composable
private fun ProfileEmptyTab(text: String) {
    Text(
        text,
        color = TomiloMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
    )
}

@Composable
private fun ProfileBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
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
    Button(
        onClick = when (status) {
            "friends" -> onRemove
            "pending_outgoing", "pending_incoming" -> onOpenFriends
            else -> onAdd
        },
        enabled = !loading && status != "blocked",
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TomiloPrimary,
            contentColor = TomiloOnPrimary,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = TomiloOnPrimary)
        } else {
            Icon(
                if (status == "friends") Icons.Default.People else Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.size(7.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RowStat(label: String, value: String, icon: ImageVector) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(TomiloSurface2.copy(alpha = 0.70f))
            .border(1.dp, TomiloBorder.copy(alpha = 0.60f), RoundedCornerShape(15.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = TomiloPrimary, modifier = Modifier.size(19.dp))
        Spacer(Modifier.size(10.dp))
        Text(label, color = TomiloMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = TomiloText)
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

private fun publicRoleLabel(role: String?): String? = when (role?.trim()?.lowercase()) {
    "admin" -> "Администратор"
    "moderator" -> "Модератор"
    "translator" -> "Переводчики"
    else -> null
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

private fun compactCount(value: Int): String {
    val n = value.coerceAtLeast(0)
    return when {
        n >= 1_000_000 -> formatCompact(n / 1_000_000.0, "M")
        n >= 10_000 -> formatCompact(n / 1_000.0, "K")
        else -> n.toString()
    }
}

private fun formatCompact(value: Double, suffix: String): String {
    val rounded = if (value >= 10.0) value.toInt().toString() else "%.1f".format(value).replace(".0", "")
    return rounded + suffix
}
