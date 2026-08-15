package ru.tomilo.lib.mobile.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.api.ConversationUserDto
import ru.tomilo.lib.mobile.data.api.FriendEntryDto
import ru.tomilo.lib.mobile.data.api.FriendRequestEntryDto
import ru.tomilo.lib.mobile.data.api.FriendRequestsDto
import ru.tomilo.lib.mobile.data.api.FriendSearchResultDto
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.ConfirmActionDialog
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.components.PageIntro
import ru.tomilo.lib.mobile.ui.components.StatusPill
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

private enum class FriendsTab(val label: String) { Friends("Друзья"), Requests("Заявки"), Search("Поиск") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    socialRepository: SocialRepository,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenChat: (String, String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf(FriendsTab.Friends) }
    var friends by remember { mutableStateOf<List<FriendEntryDto>>(emptyList()) }
    var requests by remember { mutableStateOf(FriendRequestsDto()) }
    var searchResults by remember { mutableStateOf<List<FriendSearchResultDto>>(emptyList()) }
    var query by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var actionBusy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var removeRequest by remember { mutableStateOf<FriendEntryDto?>(null) }

    fun notify(text: String) { scope.launch { snackbar.showSnackbar(text) } }

    LaunchedEffect(reload) {
        loading = true
        error = null
        val f = socialRepository.friends()
        val r = socialRepository.friendRequests()
        f.onSuccess { friends = it }.onFailure { error = it.message }
        r.onSuccess { requests = it }.onFailure { if (error == null) error = it.message }
        loading = false
    }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) { searchResults = emptyList(); return@LaunchedEffect }
        delay(300)
        socialRepository.searchFriends(q)
            .onSuccess { searchResults = it }
            .onFailure { error = it.message }
    }

    suspend fun openChat(user: ConversationUserDto) {
        actionBusy = true
        socialRepository.openConversationWith(user.stableId())
            .onSuccess { onOpenChat(it.stableId(), user.username ?: "Диалог") }
            .onFailure { notify(it.message ?: "Не удалось открыть диалог") }
        actionBusy = false
    }

    Scaffold(
        containerColor = TomiloBg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Друзья")
                        Text("${friends.size} в списке · ${requests.incoming.size} входящих", color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
                actions = { IconButton(enabled = !loading, onClick = { reload += 1 }) { Icon(Icons.Default.Refresh, "Обновить") } },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PageIntro(
                title = "Ваш круг общения",
                subtitle = "Профили, заявки и быстрый переход в личный чат",
                icon = Icons.Outlined.Group,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                trailing = { StatusPill("${friends.size} друзей") },
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FriendsTab.entries.forEach { item ->
                    val count = when (item) { FriendsTab.Friends -> friends.size; FriendsTab.Requests -> requests.incoming.size; FriendsTab.Search -> 0 }
                    FilterChip(
                        selected = tab == item,
                        onClick = { tab = item },
                        label = { Text(item.label + if (count > 0) " · $count" else "") },
                    )
                }
            }
            if (tab == FriendsTab.Search) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Очистить") } },
                    placeholder = { Text("Никнейм пользователя") },
                    shape = RoundedCornerShape(15.dp),
                )
            }

            when {
                loading -> LoadingBox(message = "Загружаем друзей…")
                error != null && friends.isEmpty() && requests.incoming.isEmpty() -> ErrorBox(error ?: "Ошибка") { reload += 1 }
                tab == FriendsTab.Friends -> if (friends.isEmpty()) EmptyState(
                    title = "Добавьте друзей",
                    message = "Найдите пользователя по никнейму. С друзьями можно начинать личные диалоги.",
                    icon = Icons.Outlined.Group,
                    actionLabel = "Найти людей",
                    onAction = { tab = FriendsTab.Search },
                ) else LazyColumn(contentPadding = ScreenPadding) {
                    items(friends, key = { it.friendshipId.ifBlank { it.user.stableId() } }) { entry ->
                        UserRow(
                            user = entry.user,
                            onUser = { onOpenUser(entry.user.stableId()) },
                            actions = {
                                IconButton(enabled = !actionBusy, onClick = { scope.launch { openChat(entry.user) } }) { Icon(Icons.AutoMirrored.Filled.Chat, "Написать") }
                                IconButton(onClick = { removeRequest = entry }) { Icon(Icons.Default.Close, "Удалить из друзей", tint = MaterialTheme.colorScheme.error) }
                            },
                        )
                    }
                }
                tab == FriendsTab.Requests -> RequestsContent(
                    data = requests,
                    busy = actionBusy,
                    onOpenUser = onOpenUser,
                    onAccept = { request ->
                        scope.launch {
                            actionBusy = true
                            socialRepository.acceptFriendRequest(request.stableId())
                                .onSuccess { notify("Заявка принята"); reload += 1 }
                                .onFailure { notify(it.message ?: "Не удалось принять заявку") }
                            actionBusy = false
                        }
                    },
                    onReject = { request ->
                        scope.launch {
                            actionBusy = true
                            socialRepository.rejectFriendRequest(request.stableId())
                                .onSuccess { notify("Заявка отклонена"); reload += 1 }
                                .onFailure { notify(it.message ?: "Не удалось отклонить заявку") }
                            actionBusy = false
                        }
                    },
                )
                else -> SearchContent(
                    query = query,
                    results = searchResults,
                    busy = actionBusy,
                    onOpenUser = onOpenUser,
                    onAdd = { result ->
                        scope.launch {
                            actionBusy = true
                            socialRepository.sendFriendRequest(result.user.stableId())
                                .onSuccess { notify("Заявка отправлена"); searchResults = searchResults.map { if (it.user.stableId() == result.user.stableId()) it.copy(status = "pending_outgoing") else it } }
                                .onFailure { notify(it.message ?: "Не удалось отправить заявку") }
                            actionBusy = false
                        }
                    },
                )
            }
        }
    }

    removeRequest?.let { entry ->
        ConfirmActionDialog(
            title = "Удалить из друзей?",
            message = "${entry.user.username ?: "Пользователь"} будет удалён из списка. Личный диалог сохранится.",
            confirmLabel = "Удалить",
            onConfirm = {
                removeRequest = null
                scope.launch {
                    socialRepository.removeFriend(entry.user.stableId())
                        .onSuccess { friends = friends.filterNot { it.user.stableId() == entry.user.stableId() }; notify("Удалено из друзей") }
                        .onFailure { notify(it.message ?: "Не удалось удалить друга") }
                }
            },
            onDismiss = { removeRequest = null },
        )
    }
}

@Composable
private fun RequestsContent(
    data: FriendRequestsDto,
    busy: Boolean,
    onOpenUser: (String) -> Unit,
    onAccept: (FriendRequestEntryDto) -> Unit,
    onReject: (FriendRequestEntryDto) -> Unit,
) {
    if (data.incoming.isEmpty() && data.outgoing.isEmpty()) {
        EmptyState("Нет новых заявок", "Входящие и отправленные заявки появятся здесь.", icon = Icons.Outlined.Group)
        return
    }
    LazyColumn(contentPadding = ScreenPadding) {
        if (data.incoming.isNotEmpty()) item { Text("Входящие", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp, 10.dp)) }
        items(data.incoming, key = { "in_${it.stableId()}" }) { request ->
            UserRow(request.user, { onOpenUser(request.user.stableId()) }) {
                IconButton(enabled = !busy, onClick = { onAccept(request) }) { Icon(Icons.Default.Check, "Принять", tint = TomiloPrimary) }
                IconButton(enabled = !busy, onClick = { onReject(request) }) { Icon(Icons.Default.Close, "Отклонить") }
            }
        }
        if (data.outgoing.isNotEmpty()) item { Text("Отправленные", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 8.dp)) }
        items(data.outgoing, key = { "out_${it.stableId()}" }) { request ->
            UserRow(request.user, { onOpenUser(request.user.stableId()) }) { Text("Ожидает ответа", color = TomiloMuted, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun SearchContent(query: String, results: List<FriendSearchResultDto>, busy: Boolean, onOpenUser: (String) -> Unit, onAdd: (FriendSearchResultDto) -> Unit) {
    when {
        query.trim().length < 2 -> EmptyState("Найдите читателей", "Введите минимум два символа никнейма.", icon = Icons.Outlined.PersonSearch)
        results.isEmpty() -> EmptyState("Никого не нашли", "Проверьте написание никнейма или попробуйте другой запрос.", icon = Icons.Outlined.PersonSearch)
        else -> LazyColumn(contentPadding = ScreenPadding) {
            items(results, key = { it.user.stableId() }) { result ->
                UserRow(result.user, { onOpenUser(result.user.stableId()) }) {
                    when (result.status) {
                        "friends" -> Text("В друзьях", color = TomiloPrimary, style = MaterialTheme.typography.labelMedium)
                        "pending_outgoing" -> Text("Заявка отправлена", color = TomiloMuted, style = MaterialTheme.typography.labelMedium)
                        "pending_incoming" -> Text("Есть входящая", color = TomiloMuted, style = MaterialTheme.typography.labelMedium)
                        "self" -> Text("Это вы", color = TomiloMuted, style = MaterialTheme.typography.labelMedium)
                        else -> OutlinedButton(enabled = !busy, onClick = { onAdd(result) }) { Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.size(6.dp)); Text("Добавить") }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRow(user: ConversationUserDto, onUser: () -> Unit, actions: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp).clip(RoundedCornerShape(19.dp)).background(TomiloSurface2.copy(alpha = 0.78f)).clickable(onClick = onUser).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(model = MediaUrl.resolve(user.avatar), contentDescription = user.username, contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp).clip(CircleShape).background(TomiloSurface2))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
            Text(user.username ?: "Пользователь", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Уровень ${user.level ?: 0}", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
            Text("Открыть профиль", color = TomiloPrimary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) { actions() }
    }
}
