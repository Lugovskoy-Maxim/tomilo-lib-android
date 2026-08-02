package ru.tomilo.lib.mobile.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.data.local.ContentPrefs
import ru.tomilo.lib.mobile.data.local.ContentSettings
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.OfflineRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.TomiloRingLogo
import ru.tomilo.lib.mobile.ui.components.TomiloWordmark
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class)
@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    offlineRepository: OfflineRepository,
    contentPrefs: ContentPrefs,
    onLogin: () -> Unit,
    onOpenOffline: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenLeaders: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenPremium: () -> Unit,
    onOpenMyPublicProfile: (userId: String) -> Unit,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    val contentSettings by contentPrefs.settingsFlow.collectAsState(initial = ContentSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var notifUnread by remember { mutableIntStateOf(0) }
    var offlineBytes by remember { mutableLongStateOf(0L) }
    var cacheMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user?.stableId()) {
        if (user != null) {
            authRepository.refreshProfile()
            notifUnread = socialRepository.notificationsUnread()
        } else {
            notifUnread = 0
        }
        offlineBytes = offlineRepository.offlineBytesTotal()
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TomiloBg),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            if (user == null) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TomiloRingLogo(size = 72.dp)
                    Spacer(Modifier.height(10.dp))
                    TomiloWordmark(maxWidth = 180.dp)
                }
                Spacer(Modifier.height(16.dp))
                Text("Вы не вошли", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Войдите через email, Яндекс или VK — закладки, чаты, офлайн (Premium).",
                    color = TomiloMuted,
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
                    Text("Войти")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onOpenLeaders, modifier = Modifier.fillMaxWidth()) {
                    Text("Лидеры")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onOpenPremium, modifier = Modifier.fillMaxWidth()) {
                    Text("Премиум-подписка")
                }
                AdultToggleRow(
                    contentSettings = contentSettings,
                    onToggle = { show -> scope.launch { contentPrefs.setShowAdult(show) } },
                )
            } else {
                Text(user!!.username ?: "Пользователь", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(user!!.email.orEmpty(), color = TomiloMuted)
                user!!.level?.let {
                    Spacer(Modifier.height(4.dp))
                    Text("Уровень $it", color = TomiloMuted)
                }
                Spacer(Modifier.height(12.dp))
                val premium = Premium.isActive(user!!.subscriptionExpiresAt)
                Text(
                    if (premium) "Premium активен" else "Без Premium",
                    color = if (premium) TomiloPremium else TomiloMuted,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onOpenPremium, modifier = Modifier.fillMaxWidth()) {
                    Text(if (premium) "Управление Premium" else "Оформить Premium")
                }
                Spacer(Modifier.height(16.dp))
                AdultToggleRow(
                    contentSettings = contentSettings,
                    onToggle = { show -> scope.launch { contentPrefs.setShowAdult(show) } },
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onOpenMyPublicProfile(user!!.stableId()) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Мой публичный профиль") }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onOpenNotifications, modifier = Modifier.fillMaxWidth()) {
                    Text(if (notifUnread > 0) "Уведомления ($notifUnread)" else "Уведомления")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
                    Text("История чтения")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onOpenLeaders, modifier = Modifier.fillMaxWidth()) {
                    Text("Лидеры")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onOpenOffline, modifier = Modifier.fillMaxWidth()) {
                    Text("Офлайн-библиотека")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Без Premium: 1 офлайн-глава = 1 просмотр рекламы",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (user!!.isStaff()) {
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onOpenAdmin, modifier = Modifier.fillMaxWidth()) {
                        Text("Админка")
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Офлайн: ${formatBytes(offlineBytes)}",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            context.imageLoader.memoryCache?.clear()
                            context.imageLoader.diskCache?.clear()
                            // http cache
                            runCatching {
                                val dir = context.cacheDir
                                dir.listFiles()?.forEach { f ->
                                    if (f.name.contains("cache") || f.name.contains("http") ||
                                        f.name.contains("coil") || f.name.contains("media")
                                    ) {
                                        f.deleteRecursively()
                                    }
                                }
                            }
                            offlineBytes = offlineRepository.offlineBytesTotal()
                            cacheMsg = "Кеш изображений и API очищен (офлайн-главы сохранены)"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Очистить кеш") }
                if (cacheMsg != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(cacheMsg!!, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { scope.launch { authRepository.logout() } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Выйти") }
            }
        }
    }
}

@Composable
private fun AdultToggleRow(
    contentSettings: ContentSettings,
    onToggle: (Boolean) -> Unit,
) {
    val canEnable = contentSettings.isAdultUser == true
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Показывать 18+", style = MaterialTheme.typography.titleSmall)
            Text(
                when {
                    contentSettings.isAdultUser == false -> "Недоступно (возраст < 18)"
                    contentSettings.showAdultContent -> "В каталоге и на главной"
                    else -> "Скрыто (можно включить)"
                },
                color = TomiloMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = contentSettings.showAdultContent && canEnable,
            onCheckedChange = { if (canEnable) onToggle(it) },
            enabled = canEnable,
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.2f GB".format(mb / 1024.0)
}
