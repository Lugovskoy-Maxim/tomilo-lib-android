package ru.tomilo.lib.mobile.ui.screens.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.TokenBridge
import ru.tomilo.lib.mobile.data.api.ConversationPreviewDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.DecoratedAvatar
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.components.PageIntro
import ru.tomilo.lib.mobile.ui.components.StatusPill
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloDanger
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

private enum class ChatsTab { Chats, Support }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    onLogin: () -> Unit,
    onOpenChat: (conversationId: String, title: String) -> Unit,
    onOpenFriends: () -> Unit,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    val token by authRepository.tokenFlow.collectAsState(initial = null)
    val isAdmin = user?.isAdmin() == true
    var chatsLoading by remember { mutableStateOf(true) }
    var supportLoading by remember { mutableStateOf(false) }
    var chatsError by remember { mutableStateOf<String?>(null) }
    var supportError by remember { mutableStateOf<String?>(null) }
    var chatItems by remember { mutableStateOf<List<ConversationPreviewDto>>(emptyList()) }
    var supportItems by remember { mutableStateOf<List<ConversationPreviewDto>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }
    var supportBusy by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(ChatsTab.Chats) }
    val scope = rememberCoroutineScope()

    var authReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        runCatching {
            val t = authRepository.tokenFlow.first()
            TokenBridge.setCached(t)
        }
        delay(50)
        authReady = true
    }

    // Роль в локальной сессии могла измениться после входа. Перед открытием
    // админского inbox обновляем профиль, чтобы вкладка поддержки не пропадала.
    LaunchedEffect(token) {
        if (!token.isNullOrBlank()) authRepository.refreshProfile()
    }

    // Regular users: never stay on Support tab
    LaunchedEffect(isAdmin) {
        if (!isAdmin && tab == ChatsTab.Support) tab = ChatsTab.Chats
    }

    LaunchedEffect(user?.stableId(), token, reload, authReady, isAdmin) {
        if (!authReady) return@LaunchedEffect
        if (user == null || token.isNullOrBlank()) {
            chatItems = emptyList()
            supportItems = emptyList()
            chatsLoading = false
            supportLoading = false
            chatsError = null
            supportError = null
            return@LaunchedEffect
        }
        TokenBridge.setCached(token)

        suspend fun loadChats(silent: Boolean) {
            if (!silent) chatsLoading = true
            chatsError = null
            socialRepository.conversations()
                .onSuccess { list ->
                    chatItems = if (isAdmin) list.filter { it.type != "support" } else list
                }
                .onFailure { chatsError = it.message ?: "Не удалось загрузить чаты" }
            chatsLoading = false
        }

        suspend fun loadSupport(silent: Boolean) {
            if (!isAdmin) {
                supportItems = emptyList()
                supportLoading = false
                supportError = null
                return
            }
            if (!silent) supportLoading = true
            supportError = null
            socialRepository.supportInbox()
                .onSuccess { supportItems = it }
                .onFailure { supportError = it.message ?: "Не удалось загрузить обращения" }
            supportLoading = false
        }

        loadChats(silent = false)
        loadSupport(silent = false)

        // Inbox поддержки должен обновляться без ручного выхода со страницы.
        while (isActive) {
            delay(12_000)
            if (tab == ChatsTab.Support && isAdmin) loadSupport(silent = true)
            else loadChats(silent = true)
        }
    }

    val items = if (isAdmin && tab == ChatsTab.Support) supportItems else chatItems
    val loading = if (isAdmin && tab == ChatsTab.Support) supportLoading else chatsLoading
    val error = if (isAdmin && tab == ChatsTab.Support) supportError else chatsError
    val chatsUnread = chatItems.sumOf { it.unreadCount }
    val supportUnread = supportItems.sumOf { it.unreadCount }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            isAdmin && tab == ChatsTab.Support -> "Поддержка"
                            else -> "Чаты"
                        },
                    )
                },
                actions = {
                    if (user != null && !token.isNullOrBlank()) {
                        IconButton(onClick = onOpenFriends) {
                            Icon(Icons.Default.Group, contentDescription = "Друзья и заявки")
                        }
                        IconButton(onClick = { reload += 1 }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                        }
                        // Regular users: open personal support thread
                        if (!isAdmin) {
                            IconButton(
                                enabled = !supportBusy,
                                onClick = {
                                    scope.launch {
                                        supportBusy = true
                                        chatsError = null
                                        TokenBridge.setCached(token)
                                        socialRepository.supportConversation()
                                            .onSuccess {
                                                val id = it.stableId()
                                                if (id.isBlank()) {
                                                    chatsError = "Пустой id диалога поддержки"
                                                } else {
                                                    onOpenChat(id, "Поддержка")
                                                }
                                            }
                                            .onFailure {
                                                chatsError = it.message ?: "Не удалось открыть поддержку"
                                            }
                                        supportBusy = false
                                    }
                                },
                            ) {
                                Icon(Icons.Default.SupportAgent, contentDescription = "Поддержка")
                            }
                        }
                    }
                },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        if (!authReady) {
            LoadingBox(Modifier.padding(padding), message = "Загружаем чаты…")
            return@Scaffold
        }
        if (user == null || token.isNullOrBlank()) {
            Column(Modifier.padding(padding).fillMaxSize()) {
                ErrorBox("Войдите, чтобы писать в чаты", onRetry = onLogin)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp),
        ) {
            item(key = "chats_intro") {
                PageIntro(
                    title = if (chatsUnread + supportUnread > 0) "Есть новые сообщения" else "Оставайтесь на связи",
                    subtitle = if (isAdmin) "Личные диалоги и обращения поддержки" else "Друзья и команда поддержки TOMILO LIB",
                    icon = Icons.Outlined.Forum,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    trailing = {
                        val unread = chatsUnread + supportUnread
                        StatusPill(if (unread > 0) unreadLabel(unread) else "Онлайн")
                    },
                )
            }
            if (isAdmin) {
                item(key = "chats_filters") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        FilterChip(
                            selected = tab == ChatsTab.Chats,
                            onClick = { tab = ChatsTab.Chats },
                            label = {
                                Text(if (chatsUnread > 0) "Чаты · ${unreadLabel(chatsUnread)}" else "Чаты")
                            },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        FilterChip(
                            selected = tab == ChatsTab.Support,
                            onClick = { tab = ChatsTab.Support },
                            label = {
                                Text(
                                    if (supportUnread > 0) "Поддержка · ${unreadLabel(supportUnread)}"
                                    else "Поддержка",
                                )
                            },
                        )
                    }
                }
            }

            when {
                loading && items.isEmpty() -> item(key = "chats_loading") {
                    LoadingBox(
                        Modifier.fillMaxWidth().height(360.dp),
                        message = if (tab == ChatsTab.Support) {
                            "Загружаем обращения…"
                        } else {
                            "Загружаем чаты…"
                        },
                    )
                }
                error != null && items.isEmpty() -> item(key = "chats_error") {
                    ErrorBox(
                        error ?: "Ошибка",
                        modifier = Modifier.fillMaxWidth().height(360.dp),
                    ) { reload += 1 }
                }
                items.isEmpty() -> item(key = "chats_empty") {
                    EmptyState(
                        title = if (tab == ChatsTab.Support) "Нет обращений" else "Диалогов пока нет",
                        message = when {
                            tab == ChatsTab.Support ->
                                "Новые обращения пользователей появятся здесь."
                            isAdmin ->
                                "Личные диалоги доступны с друзьями, а обращения — во вкладке поддержки."
                            else ->
                                "Начните диалог с другом или напишите в поддержку через кнопку вверху."
                        },
                        icon = Icons.Outlined.Forum,
                        modifier = Modifier.fillMaxWidth().height(360.dp).padding(ScreenPadding),
                    )
                }
                else -> {
                    if (error != null) {
                        item {
                            Text(
                                error!!,
                                color = TomiloDanger,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                    items(
                        items,
                        key = { c -> c.stableId().ifBlank { c.hashCode().toString() } },
                    ) { c ->
                        val name = when {
                            c.type == "support" || c.participant?.isSupport == true -> {
                                if (isAdmin && tab == ChatsTab.Support) {
                                    c.participant?.username?.takeIf { it.isNotBlank() }
                                        ?: "Пользователь"
                                } else {
                                    "Поддержка"
                                }
                            }
                            else -> c.participant?.username ?: "Диалог"
                        }
                        val id = c.stableId()
                        Row(
                            Modifier
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(19.dp))
                                .background(if (c.unreadCount > 0) TomiloPrimary.copy(alpha = 0.10f) else TomiloSurface)
                                .border(1.dp, if (c.unreadCount > 0) TomiloPrimary.copy(alpha = 0.22f) else TomiloBorder.copy(alpha = 0.62f), RoundedCornerShape(19.dp))
                                .clickable(enabled = id.isNotBlank()) {
                                    onOpenChat(id, name)
                                }
                                .padding(horizontal = 13.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DecoratedAvatar(
                                avatarUrl = c.participant?.avatar,
                                username = name,
                                decorations = c.participant?.decorations(),
                                size = 48.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Text(
                                    c.lastMessagePreview?.takeIf { it.isNotBlank() } ?: "Нет сообщений",
                                    color = TomiloMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                ru.tomilo.lib.mobile.core.ChatTime.label(c.lastMessageAt)?.let { time ->
                                    Text(
                                        time,
                                        color = TomiloMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                if (c.unreadCount > 0) {
                                    Spacer(Modifier.height(6.dp))
                                    StatusPill(unreadLabel(c.unreadCount), TomiloPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun unreadLabel(count: Int): String = if (count > 99) "99+" else count.toString()
