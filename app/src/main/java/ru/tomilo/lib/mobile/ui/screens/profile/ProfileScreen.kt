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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import ru.tomilo.lib.mobile.core.MediaUrl
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ShoppingBag
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
import ru.tomilo.lib.mobile.ui.components.ConfirmActionDialog
import ru.tomilo.lib.mobile.ui.components.ActionRow
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import androidx.compose.ui.graphics.Brush

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
    onOpenFriends: () -> Unit,
    onOpenQuests: () -> Unit,
    onOpenUpdates: () -> Unit,
    onOpenHub: () -> Unit,
    onOpenWheel: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenMyPublicProfile: (userId: String) -> Unit,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    val contentSettings by contentPrefs.settingsFlow.collectAsState(initial = ContentSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var notifUnread by remember { mutableIntStateOf(0) }
    var offlineBytes by remember { mutableLongStateOf(0L) }
    var cacheMsg by remember { mutableStateOf<String?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }

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
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
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
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(TomiloPrimary.copy(alpha = 0.18f), TomiloSurface)))
                        .border(1.dp, TomiloPrimary.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = MediaUrl.resolve(user!!.avatar),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(TomiloPrimary.copy(alpha = 0.14f)),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            user!!.username ?: "Пользователь",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(user!!.email.orEmpty(), color = TomiloMuted)
                        user!!.level?.let {
                            Spacer(Modifier.height(4.dp))
                            Text("Уровень $it", color = TomiloMuted)
                        }
                        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                            user!!.balance?.let { Text("$it мон.", color = TomiloPremium, style = MaterialTheme.typography.labelMedium) }
                            user!!.currentStreak?.let { Text("$it дн. серия", color = TomiloPrimary, style = MaterialTheme.typography.labelMedium) }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val premium = Premium.isActive(user!!.subscriptionExpiresAt)
                ActionRow(
                    icon = Icons.Default.Star,
                    title = if (premium) "Premium активен" else "Tomilo Premium",
                    subtitle = if (premium) "Управление подпиской" else "Премиум-главы, офлайн и без рекламы",
                    badge = if (premium) "АКТИВЕН" else null,
                    iconTint = TomiloPremium,
                    onClick = onOpenPremium,
                )
                Spacer(Modifier.height(12.dp))
                AdultToggleRow(
                    contentSettings = contentSettings,
                    onToggle = { show -> scope.launch { contentPrefs.setShowAdult(show) } },
                )
                Spacer(Modifier.height(22.dp))
                Text("Библиотека", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    ActionRow(
                        icon = Icons.Default.Person,
                        title = "Публичный профиль",
                        subtitle = "Статистика и достижения",
                        onClick = { onOpenMyPublicProfile(user!!.stableId()) },
                    )
                    ActionRow(
                        icon = Icons.Default.Casino,
                        title = "Колесо судьбы",
                        subtitle = "Призы, монеты, опыт и редкие предметы",
                        iconTint = TomiloPremium,
                        onClick = onOpenWheel,
                    )
                    ActionRow(
                        icon = Icons.Default.ShoppingBag,
                        title = "Магазин декораций",
                        subtitle = "Аватары, рамки и фоны · ${user!!.balance ?: 0} монет",
                        iconTint = TomiloPremium,
                        onClick = onOpenShop,
                    )
                    ActionRow(
                        icon = Icons.Default.TaskAlt,
                        title = "Задания и награды",
                        subtitle = "Ежедневный бонус, опыт и монеты",
                        onClick = onOpenQuests,
                    )
                    ActionRow(
                        icon = Icons.Default.Group,
                        title = "Друзья",
                        subtitle = "Заявки, поиск людей и личные диалоги",
                        onClick = onOpenFriends,
                    )
                    ActionRow(
                        icon = Icons.Default.NotificationsNone,
                        title = "Уведомления",
                        subtitle = "Ответы, обновления и системные сообщения",
                        badge = notifUnread.takeIf { it > 0 }?.toString(),
                        onClick = onOpenNotifications,
                    )
                    ActionRow(
                        icon = Icons.Default.Update,
                        title = "Все обновления",
                        subtitle = "Архив новых глав и тайтлов",
                        onClick = onOpenUpdates,
                    )
                    ActionRow(
                        icon = Icons.Default.History,
                        title = "История чтения",
                        subtitle = "Продолжить с последней главы",
                        onClick = onOpenHistory,
                    )
                    ActionRow(
                        icon = Icons.Default.DownloadForOffline,
                        title = "Офлайн-библиотека",
                        subtitle = "${formatBytes(offlineBytes)} сохранено на устройстве",
                        onClick = onOpenOffline,
                    )
                    ActionRow(
                        icon = Icons.Default.Leaderboard,
                        title = "Лидеры",
                        subtitle = "Рейтинг читателей Tomilo",
                        onClick = onOpenLeaders,
                    )
                }
                if (user!!.isStaff()) {
                    Spacer(Modifier.height(8.dp))
                    ActionRow(
                        icon = Icons.Default.AdminPanelSettings,
                        title = "Панель управления",
                        subtitle = "Инструменты команды",
                        onClick = onOpenAdmin,
                    )
                }
                Spacer(Modifier.height(22.dp))
                Text("Приложение", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                ActionRow(
                    icon = Icons.Default.Explore,
                    title = "Мир Tomilo",
                    subtitle = "Подборки, новости, гайды, магазин и игры",
                    onClick = onOpenHub,
                )
                Spacer(Modifier.height(8.dp))
                ActionRow(
                    icon = Icons.Default.CleaningServices,
                    title = "Очистить кеш",
                    subtitle = "Офлайн-главы останутся на устройстве",
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
                )
                if (cacheMsg != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(cacheMsg!!, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                ActionRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Выйти из аккаунта",
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = { confirmLogout = true },
                )
            }
        }
    }

    if (confirmLogout) {
        ConfirmActionDialog(
            title = "Выйти из аккаунта?",
            message = "Закладки, история и чаты останутся в аккаунте. Скачанные главы сохранятся на устройстве.",
            confirmLabel = "Выйти",
            onConfirm = {
                confirmLogout = false
                scope.launch { authRepository.logout() }
            },
            onDismiss = { confirmLogout = false },
        )
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
