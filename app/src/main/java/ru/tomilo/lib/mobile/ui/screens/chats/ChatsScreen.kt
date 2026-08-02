package ru.tomilo.lib.mobile.ui.screens.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.api.ConversationPreviewDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    onLogin: () -> Unit,
    onOpenChat: (conversationId: String, title: String) -> Unit,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<ConversationPreviewDto>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(user?.stableId(), reload) {
        if (user == null) {
            items = emptyList()
            return@LaunchedEffect
        }
        loading = true
        error = null
        socialRepository.conversations()
            .onSuccess { items = it }
            .onFailure { error = it.message }
        loading = false
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = { Text("Чаты") },
                actions = {
                    if (user != null) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    socialRepository.supportConversation()
                                        .onSuccess {
                                            onOpenChat(it.stableId(), "Поддержка")
                                        }
                                        .onFailure { error = it.message }
                                }
                            },
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = "Поддержка")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TomiloBg),
            )
        },
    ) { padding ->
        if (user == null) {
            Column(Modifier.padding(padding).fillMaxSize()) {
                ErrorBox("Войдите, чтобы писать в чаты", onRetry = onLogin)
            }
            return@Scaffold
        }
        when {
            loading -> LoadingBox(Modifier.padding(padding))
            error != null && items.isEmpty() -> Column(Modifier.padding(padding)) {
                ErrorBox(error ?: "Ошибка") { reload += 1 }
            }
            items.isEmpty() -> Text(
                "Нет диалогов. Откройте профиль пользователя и напишите ему.",
                color = TomiloMuted,
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            else -> LazyColumn(
                Modifier.padding(padding),
                contentPadding = ScreenPadding,
            ) {
                items(items, key = { it.stableId() }) { c ->
                    val name = c.participant?.username
                        ?: if (c.type == "support") "Поддержка" else "Диалог"
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenChat(c.stableId(), name) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = MediaUrl.resolve(c.participant?.avatar),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(TomiloSurface2),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                c.lastMessagePreview ?: "Нет сообщений",
                                color = TomiloMuted,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (c.unreadCount > 0) {
                            Text(
                                c.unreadCount.toString(),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}
