package ru.tomilo.lib.mobile.ui.screens.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.data.api.NotificationDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.push.toOpenRequest
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.ConfirmActionDialog
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.components.PageIntro
import ru.tomilo.lib.mobile.ui.components.StatusPill
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenTitle: (String) -> Unit = {},
    onOpenChapter: (titleId: String?, chapterId: String) -> Unit = { _, _ -> },
    onOpenLink: (String) -> Unit = {},
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<NotificationDto>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }
    var pendingDelete by remember { mutableStateOf<NotificationDto?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(user?.stableId(), reload) {
        if (user == null) return@LaunchedEffect
        loading = true
        error = null
        socialRepository.notifications()
            .onSuccess { items = it }
            .onFailure { error = it.message }
        loading = false
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = { Text("Уведомления") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (user != null) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    socialRepository.markAllNotificationsRead()
                                    reload += 1
                                }
                            },
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Прочитать все")
                        }
                    }
                },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        if (user == null) {
            Column(Modifier.padding(padding)) {
                ErrorBox("Войдите, чтобы видеть уведомления", onRetry = onLogin)
            }
            return@Scaffold
        }
        when {
            loading -> LoadingBox(Modifier.padding(padding))
            error != null && items.isEmpty() -> Column(Modifier.padding(padding)) {
                ErrorBox(error ?: "Ошибка") { reload += 1 }
            }
            items.isEmpty() -> EmptyState(
                title = "Пока тихо",
                message = "Новые главы, ответы и системные сообщения появятся здесь.",
                icon = Icons.Default.NotificationsNone,
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = ScreenPadding,
            ) {
                item {
                    val unread = items.count { !it.read() }
                    PageIntro(
                        title = if (unread > 0) "$unread непрочитанных" else "Вы всё прочитали",
                        subtitle = "Новые главы, ответы и важные события аккаунта",
                        icon = Icons.Default.NotificationsActive,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        trailing = { StatusPill(if (unread > 0) "$unread новых" else "Готово") },
                    )
                }
                items(items, key = { it.stableId() }) { n ->
                    Row(
                        Modifier
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (n.read()) TomiloSurface.copy(alpha = 0.62f) else TomiloPrimary.copy(alpha = 0.10f))
                            .border(1.dp, if (n.read()) TomiloBorder.copy(alpha = 0.55f) else TomiloPrimary.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
                            .clickable {
                                scope.launch {
                                    socialRepository.markNotificationRead(n.stableId())
                                    items = items.map { item ->
                                        if (item.stableId() == n.stableId()) item.copy(isRead = true) else item
                                    }
                                    val open = n.toOpenRequest()
                                    when {
                                        !open.chapterId.isNullOrBlank() ->
                                            onOpenChapter(open.titleId, open.chapterId)
                                        !open.titleId.isNullOrBlank() -> onOpenTitle(open.titleId)
                                        !open.linkUrl.isNullOrBlank() -> onOpenLink(open.linkUrl)
                                    }
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                                .background(if (n.read()) TomiloMuted.copy(alpha = 0.10f) else TomiloPrimary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.NotificationsNone, null, tint = if (n.read()) TomiloMuted else TomiloPrimary)
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                n.title ?: n.type ?: "Уведомление",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (n.read()) FontWeight.Normal else FontWeight.SemiBold,
                                color = if (n.read()) TomiloMuted else MaterialTheme.colorScheme.onBackground,
                            )
                            if (!n.message.isNullOrBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(n.message!!, style = MaterialTheme.typography.bodyMedium)
                            }
                            n.createdAt?.take(16)?.let {
                                Spacer(Modifier.height(2.dp))
                                Text(it.replace('T', ' '), style = MaterialTheme.typography.bodySmall, color = TomiloMuted)
                            }
                        }
                        IconButton(
                            onClick = { pendingDelete = n },
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Удалить")
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { notification ->
        ConfirmActionDialog(
            title = "Удалить уведомление?",
            message = notification.title ?: notification.message ?: "Уведомление будет удалено без возможности восстановления.",
            confirmLabel = "Удалить",
            onConfirm = {
                pendingDelete = null
                scope.launch {
                    socialRepository.deleteNotification(notification.stableId())
                        .onSuccess {
                            items = items.filterNot { it.stableId() == notification.stableId() }
                        }
                        .onFailure { error = it.message }
                }
            },
            onDismiss = { pendingDelete = null },
        )
    }
}
