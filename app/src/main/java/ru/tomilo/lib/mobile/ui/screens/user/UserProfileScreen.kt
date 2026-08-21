package ru.tomilo.lib.mobile.ui.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.People
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
import androidx.compose.ui.unit.dp
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
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder

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
                    if (me != null && me!!.stableId() != userId) {
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
            user != null -> {
                val u = user!!
                Column(
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DecoratedAvatar(
                        avatarUrl = u.avatar,
                        username = u.username,
                        decorations = u.decorations(),
                        size = 96.dp,
                        ringColor = if (Premium.isActive(u.subscriptionExpiresAt)) TomiloPremium else MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(u.username ?: "user", style = MaterialTheme.typography.headlineMedium)
                    if (Premium.isActive(u.subscriptionExpiresAt)) {
                        Text("Premium", color = TomiloPremium)
                    }
                    Text("Уровень ${u.level ?: 0}", color = TomiloMuted)
                    if (!u.bio.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(u.bio!!, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (me != null && me?.stableId() != userId) {
                        Spacer(Modifier.height(18.dp))
                        when (friendStatus) {
                            "friends" -> OutlinedButton(
                                onClick = { confirmRemove = true },
                                enabled = !friendActionLoading,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.People, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Вы друзья · удалить")
                            }
                            "pending_outgoing" -> OutlinedButton(
                                onClick = onOpenFriends,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Заявка отправлена") }
                            "pending_incoming" -> Button(
                                onClick = onOpenFriends,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Ответить на заявку") }
                            "blocked", "self" -> Unit
                            else -> Button(
                                onClick = ::sendFriendRequest,
                                enabled = !friendActionLoading,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (friendActionLoading) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                                }
                                Spacer(Modifier.size(8.dp))
                                Text("Добавить в друзья")
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        PublicStatCard("Главы", "${u.chaptersRead ?: 0}", Modifier.weight(1f))
                        PublicStatCard("Тайтлы", "${u.titlesReadCount ?: 0}", Modifier.weight(1f))
                        PublicStatCard("Серия", "${u.currentStreak ?: 0}", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        PublicStatCard("Комменты", "${u.commentsCount ?: 0}", Modifier.weight(1f))
                        PublicStatCard("Лайки", "${u.likesReceivedCount ?: 0}", Modifier.weight(1f))
                        PublicStatCard("Завершено", "${u.completedTitlesCount ?: 0}", Modifier.weight(1f))
                    }
                    u.readingTimeMinutes?.takeIf { it > 0 }?.let {
                        Spacer(Modifier.height(8.dp))
                        RowStat("Время чтения", "$it мин")
                    }

                    if (me == null) {
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
                            Text("Войти, чтобы написать")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TomiloSurface2)
            .border(1.dp, TomiloBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(2.dp))
        Text(label, color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun RowStat(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(TomiloSurface2.copy(alpha = 0.70f))
            .border(1.dp, TomiloBorder.copy(alpha = 0.60f), RoundedCornerShape(15.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Text(label, color = TomiloMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
