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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import ru.tomilo.lib.mobile.data.api.DirectMessageDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

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
    val myId = user?.stableId().orEmpty()
    var loading by remember { mutableStateOf(true) }
    var messages by remember { mutableStateOf<List<DirectMessageDto>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        loading = true
        socialRepository.messages(conversationId)
            .onSuccess {
                messages = it.sortedBy { m -> m.createdAt.orEmpty() }
                socialRepository.markConversationRead(conversationId)
            }
            .onFailure { error = it.message }
        loading = false
    }

    LaunchedEffect(conversationId) { reload() }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TomiloBg),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (loading && messages.isEmpty()) {
                LoadingBox(Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(messages, key = { it.stableId() }) { msg ->
                        val mine = msg.senderId == myId
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                        ) {
                            Box(
                                Modifier
                                    .widthIn(max = 300.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (mine) TomiloPrimary.copy(alpha = 0.35f) else TomiloSurface2)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Column {
                                    Text(
                                        if (msg.deletedAt != null) "Сообщение удалено"
                                        else msg.body.orEmpty(),
                                    )
                                    msg.createdAt?.take(16)?.let {
                                        Text(
                                            it.replace('T', ' '),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TomiloMuted,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
            if (error != null) {
                Text(error!!, color = TomiloMuted, modifier = Modifier.padding(horizontal = 12.dp))
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Сообщение") },
                    maxLines = 4,
                )
                IconButton(
                    onClick = {
                        val text = draft.trim()
                        if (text.isEmpty()) return@IconButton
                        scope.launch {
                            socialRepository.sendMessage(conversationId, text)
                                .onSuccess {
                                    draft = ""
                                    reload()
                                }
                                .onFailure { error = it.message }
                        }
                    },
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
                }
            }
        }
    }
}
