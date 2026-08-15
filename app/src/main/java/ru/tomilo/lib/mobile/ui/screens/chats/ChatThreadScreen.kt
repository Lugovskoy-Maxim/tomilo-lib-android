package ru.tomilo.lib.mobile.ui.screens.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import ru.tomilo.lib.mobile.TokenBridge
import ru.tomilo.lib.mobile.data.api.DirectMessageDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloDanger
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import ru.tomilo.lib.mobile.ui.theme.TomiloText
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    conversationId: String,
    title: String,
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    onBack: () -> Unit,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    val token by authRepository.tokenFlow.collectAsState(initial = null)
    val myId = user?.stableId().orEmpty()
    val convId = conversationId.trim()
    var loading by remember { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf<List<DirectMessageDto>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    suspend fun reload(silent: Boolean = false) {
        if (convId.isBlank()) {
            error = "Некорректный диалог"
            loading = false
            return
        }
        // Always re-sync token before chat API calls
        val liveToken = token ?: runCatching { authRepository.tokenFlow.first() }.getOrNull()
        if (liveToken.isNullOrBlank()) {
            error = "Войдите в аккаунт"
            loading = false
            return
        }
        TokenBridge.setCached(liveToken)
        if (!silent) loading = true
        socialRepository.messages(convId)
            .onSuccess {
                messages = it
                error = null
                socialRepository.markConversationRead(convId)
            }
            .onFailure {
                val msg = it.message ?: "Не удалось загрузить сообщения"
                if (messages.isEmpty()) error = msg
                else error = msg
            }
        loading = false
    }

    LaunchedEffect(convId, token) {
        reload()
    }

    // Лёгкий poll, пока экран открыт
    LaunchedEffect(convId, token) {
        if (token.isNullOrBlank() || convId.isBlank()) return@LaunchedEffect
        while (isActive) {
            delay(8_000)
            reload(silent = true)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            runCatching { listState.animateScrollToItem(messages.lastIndex) }
        }
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title.ifBlank { "Чат" }, maxLines = 1)
                        Text("Обновляется автоматически", color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding(),
        ) {
            when {
                token.isNullOrBlank() -> {
                    ErrorBox("Войдите, чтобы писать в чат") { /* parent handles login via back */ }
                }
                loading && messages.isEmpty() -> LoadingBox(
                    Modifier.weight(1f),
                    message = "Загружаем сообщения…",
                )
                error != null && messages.isEmpty() -> {
                    Column(Modifier.weight(1f)) {
                        ErrorBox(error ?: "Ошибка") {
                            scope.launch { reload() }
                        }
                    }
                }
                messages.isEmpty() -> {
                    EmptyState(
                        title = "Начните разговор",
                        message = "Напишите первое сообщение — оно появится здесь сразу после отправки.",
                        icon = Icons.Outlined.Forum,
                        modifier = Modifier.weight(1f),
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item { Spacer(Modifier.height(8.dp)) }
                        items(
                            messages,
                            key = { m -> m.stableId().ifBlank { m.hashCode().toString() } },
                        ) { msg ->
                            val mine = myId.isNotBlank() && msg.senderKey() == myId
                            val bodyText = when {
                                msg.deletedAt != null -> "Сообщение удалено"
                                else -> msg.textBody().ifBlank { " " }
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                            ) {
                                Box(
                                    Modifier
                                        .widthIn(max = 300.dp)
                                        .clip(
                                            if (mine) RoundedCornerShape(19.dp, 19.dp, 5.dp, 19.dp)
                                            else RoundedCornerShape(19.dp, 19.dp, 19.dp, 5.dp),
                                        )
                                        .background(
                                            if (mine) TomiloPrimary.copy(alpha = 0.78f)
                                            else TomiloSurface2,
                                        )
                                        .border(
                                            1.dp,
                                            if (mine) TomiloPrimary.copy(alpha = 0.9f) else TomiloBorder,
                                            if (mine) RoundedCornerShape(19.dp, 19.dp, 5.dp, 19.dp)
                                            else RoundedCornerShape(19.dp, 19.dp, 19.dp, 5.dp),
                                        )
                                        .padding(horizontal = 13.dp, vertical = 9.dp),
                                ) {
                                    Column {
                                        Text(
                                            bodyText,
                                            color = if (mine) Color.White else TomiloText,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        msg.createdAtLabel()?.let { label ->
                                            Text(
                                                label,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (mine) Color.White.copy(alpha = 0.68f) else TomiloMuted,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }

            if (error != null && messages.isNotEmpty()) {
                Text(
                    error!!,
                    color = TomiloDanger,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(TomiloBg)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Сообщение") },
                    maxLines = 4,
                    enabled = !sending && !token.isNullOrBlank(),
                    shape = RoundedCornerShape(24.dp),
                )
                IconButton(
                    enabled = !sending && draft.isNotBlank() && !token.isNullOrBlank(),
                    onClick = {
                        val text = draft.trim()
                        if (text.isEmpty() || sending) return@IconButton
                        scope.launch {
                            sending = true
                            error = null
                            // Оптимистично покажем своё сообщение
                            TokenBridge.setCached(token)
                            val optimistic = DirectMessageDto(
                                underscoreId = "local-${System.currentTimeMillis()}",
                                id = "local-${System.currentTimeMillis()}",
                                conversationId = convId,
                                senderId = if (myId.isNotBlank()) JsonPrimitive(myId) else null,
                                body = text,
                                createdAt = JsonPrimitive(
                                    java.time.Instant.now().toString(),
                                ),
                            )
                            messages = messages + optimistic
                            draft = ""
                            socialRepository.sendMessage(convId, text)
                                .onSuccess {
                                    // перезагрузка, чтобы заменить optimistic на серверные
                                    reload(silent = true)
                                }
                                .onFailure { e ->
                                    error = e.message ?: "Не удалось отправить"
                                    // убрать optimistic
                                    messages = messages.filter { m ->
                                        !m.stableId().startsWith("local-")
                                    }
                                    draft = text
                                }
                            sending = false
                        }
                    },
                ) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(
                            if (draft.isNotBlank()) TomiloPrimary else TomiloSurface2,
                        ),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить", tint = if (draft.isNotBlank()) Color.White else TomiloMuted) }
                }
            }
            if (sending) {
                Text(
                    "Отправка…",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                )
            }
        }
    }
}
