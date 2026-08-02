package ru.tomilo.lib.mobile.ui.screens.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.data.api.NotificationDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    onBack: () -> Unit,
    onLogin: () -> Unit,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<NotificationDto>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TomiloBg),
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
            items.isEmpty() -> Text(
                "Нет уведомлений",
                color = TomiloMuted,
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            else -> LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = ScreenPadding,
            ) {
                items(items, key = { it.stableId() }) { n ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    socialRepository.markNotificationRead(n.stableId())
                                    reload += 1
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
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
                }
            }
        }
    }
}
