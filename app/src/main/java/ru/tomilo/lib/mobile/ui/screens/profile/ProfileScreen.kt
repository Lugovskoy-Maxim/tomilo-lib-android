package ru.tomilo.lib.mobile.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import ru.tomilo.lib.mobile.core.MediaUrl
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.material.icons.filled.SystemUpdateAlt
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
import ru.tomilo.lib.mobile.BuildConfig
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.data.local.ContentPrefs
import ru.tomilo.lib.mobile.data.local.ContentSettings
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.OfflineRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.data.update.AppRelease
import ru.tomilo.lib.mobile.data.update.AppUpdateManager
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
    appUpdateManager: AppUpdateManager,
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
                val premium = Premium.isActive(user!!.subscriptionExpiresAt)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(TomiloPrimary.copy(alpha = 0.20f), TomiloSurface)))
                        .border(1.dp, TomiloPrimary.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                        .clickable { onOpenMyPublicProfile(user!!.stableId()) }
                        .padding(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = MediaUrl.resolve(user!!.avatar),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .border(
                                    2.dp,
                                    if (premium) TomiloPremium else TomiloPrimary.copy(alpha = 0.45f),
                                    CircleShape,
                                )
                                .background(TomiloPrimary.copy(alpha = 0.14f)),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                user!!.username ?: "Пользователь",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            if (premium) {
                                Text("Premium", color = TomiloPremium, style = MaterialTheme.typography.labelLarge)
                            }
                            user!!.email?.takeIf { it.isNotBlank() }?.let {
                                Text(it, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "Уровень ${user!!.level ?: 0}" +
                                    (user!!.experience?.let { " · $it XP" } ?: ""),
                                color = TomiloMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ProfileStatChip("Главы", "${user!!.chaptersRead ?: 0}", Modifier.weight(1f))
                        ProfileStatChip("Тайтлы", "${user!!.titlesReadCount ?: 0}", Modifier.weight(1f))
                        ProfileStatChip("Серия", "${user!!.currentStreak ?: 0}", Modifier.weight(1f))
                        ProfileStatChip("Монеты", "${user!!.balance ?: 0}", Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                ActionRow(
                    icon = Icons.Default.Star,
                    title = if (premium) "Premium активен" else "Tomilo Premium",
                    subtitle = if (premium) "Управление подпиской" else "Премиум-главы, офлайн и без рекламы",
                    badge = if (premium) "АКТИВЕН" else null,
                    iconTint = TomiloPremium,
                    onClick = onOpenPremium,
                )
                Spacer(Modifier.height(8.dp))
                AdultToggleRow(
                    contentSettings = contentSettings,
                    onToggle = { show -> scope.launch { contentPrefs.setShowAdult(show) } },
                )
                Spacer(Modifier.height(22.dp))
                Text("Чтение", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionRow(
                        icon = Icons.Default.History,
                        title = "История чтения",
                        subtitle = "Продолжить с последней главы",
                        onClick = onOpenHistory,
                    )
                    ActionRow(
                        icon = Icons.Default.DownloadForOffline,
                        title = "Офлайн",
                        subtitle = "${formatBytes(offlineBytes)} на устройстве",
                        onClick = onOpenOffline,
                    )
                    ActionRow(
                        icon = Icons.Default.NotificationsNone,
                        title = "Уведомления",
                        subtitle = "Новые главы и ответы",
                        badge = notifUnread.takeIf { it > 0 }?.toString(),
                        onClick = onOpenNotifications,
                    )
                    ActionRow(
                        icon = Icons.Default.Update,
                        title = "Обновления",
                        subtitle = "Свежие главы по каталогу",
                        onClick = onOpenUpdates,
                    )
                }
                Spacer(Modifier.height(22.dp))
                Text("Сообщество", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionRow(
                        icon = Icons.Default.Group,
                        title = "Друзья",
                        subtitle = "Заявки, поиск людей и профили",
                        onClick = onOpenFriends,
                    )
                    ActionRow(
                        icon = Icons.Default.TaskAlt,
                        title = "Задания",
                        subtitle = "Ежедневный бонус, опыт и монеты",
                        onClick = onOpenQuests,
                    )
                    ActionRow(
                        icon = Icons.Default.Casino,
                        title = "Колесо судьбы",
                        subtitle = "Призы, монеты и редкие предметы",
                        iconTint = TomiloPremium,
                        onClick = onOpenWheel,
                    )
                    ActionRow(
                        icon = Icons.Default.ShoppingBag,
                        title = "Магазин",
                        subtitle = "Аватары и рамки · ${user!!.balance ?: 0} монет",
                        iconTint = TomiloPremium,
                        onClick = onOpenShop,
                    )
                    ActionRow(
                        icon = Icons.Default.Leaderboard,
                        title = "Лидеры",
                        subtitle = "Рейтинг читателей",
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
            Spacer(Modifier.height(20.dp))
            GithubUpdateBlock(appUpdateManager)
            Spacer(Modifier.height(12.dp))
            AppVersionLabel()
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

private sealed interface UpdateUi {
    data object Idle : UpdateUi
    data object Checking : UpdateUi
    data object Current : UpdateUi
    data class Available(val release: AppRelease) : UpdateUi
    data class Downloading(val release: AppRelease, val progress: Float) : UpdateUi
    data class Ready(val release: AppRelease, val file: java.io.File) : UpdateUi
    data class Error(val message: String) : UpdateUi
}

@Composable
private fun GithubUpdateBlock(updater: AppUpdateManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var ui by remember { mutableStateOf<UpdateUi>(UpdateUi.Idle) }
    var pendingInstall by remember { mutableStateOf<java.io.File?>(null) }

    val installPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        val apk = pendingInstall
        pendingInstall = null
        if (apk != null && updater.hasInstallPermission()) {
            runCatching { context.startActivity(updater.installIntent(apk)) }
        }
    }

    fun install(file: java.io.File) {
        if (!updater.hasInstallPermission()) {
            pendingInstall = file
            installPermission.launch(updater.installSettingsIntent())
            return
        }
        runCatching { context.startActivity(updater.installIntent(file)) }
            .onFailure { ui = UpdateUi.Error(it.message ?: "Не удалось открыть установщик") }
    }

    fun check() {
        ui = UpdateUi.Checking
        scope.launch {
            updater.fetchLatest()
                .onSuccess { release ->
                    ui = if (updater.isNewer(release)) UpdateUi.Available(release)
                    else UpdateUi.Current
                }
                .onFailure { ui = UpdateUi.Error(it.message ?: "Не удалось проверить") }
        }
    }

    fun download(release: AppRelease) {
        if (!updater.canInstallInPlace()) {
            runCatching { context.startActivity(updater.openReleaseIntent(release.htmlUrl)) }
            return
        }
        ui = UpdateUi.Downloading(release, 0f)
        scope.launch {
            updater.download(release) { progress ->
                ui = UpdateUi.Downloading(release, progress)
            }.onSuccess { file ->
                ui = UpdateUi.Ready(release, file)
                install(file)
            }.onFailure {
                ui = UpdateUi.Error(it.message ?: "Не удалось скачать")
            }
        }
    }

    val title = when (val state = ui) {
        UpdateUi.Idle -> "Обновление с GitHub"
        UpdateUi.Checking -> "Проверяем GitHub…"
        UpdateUi.Current -> "Версия актуальна"
        is UpdateUi.Available -> "Доступна ${state.release.versionName}"
        is UpdateUi.Downloading -> "Скачиваем ${state.release.versionName}"
        is UpdateUi.Ready -> "Готово к установке"
        is UpdateUi.Error -> "Не удалось проверить"
    }
    val subtitle = when (val state = ui) {
        UpdateUi.Idle -> "APK из Releases, тот же ключ подписи"
        UpdateUi.Checking -> "Смотрим latest release"
        UpdateUi.Current -> "Установлена ${BuildConfig.VERSION_NAME}"
        is UpdateUi.Available -> formatApkSize(state.release.apkSize)
        is UpdateUi.Downloading -> "${(state.progress * 100).toInt()}%"
        is UpdateUi.Ready -> "Нажмите, чтобы установить ${state.release.versionName}"
        is UpdateUi.Error -> state.message
    }

    Column(Modifier.fillMaxWidth()) {
        ActionRow(
            icon = Icons.Default.SystemUpdateAlt,
            title = title,
            subtitle = subtitle,
            badge = if (ui is UpdateUi.Available) "Новое" else null,
            onClick = {
                when (val state = ui) {
                    UpdateUi.Idle, UpdateUi.Current, is UpdateUi.Error -> check()
                    UpdateUi.Checking, is UpdateUi.Downloading -> Unit
                    is UpdateUi.Available -> download(state.release)
                    is UpdateUi.Ready -> install(state.file)
                }
            },
        )
        if (ui is UpdateUi.Downloading) {
            LinearProgressIndicator(
                progress = { (ui as UpdateUi.Downloading).progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

private fun formatApkSize(bytes: Long): String {
    if (bytes <= 0) return "Новый APK с GitHub"
    val mb = bytes / (1024.0 * 1024.0)
    return "APK %.1f МБ · GitHub Releases".format(mb)
}

@Composable
private fun AppVersionLabel() {
    val channel = when (BuildConfig.STORE_CHANNEL) {
        "play" -> "Google Play"
        else -> "RuStore"
    }
    val debug = if (BuildConfig.DEBUG) " debug" else ""
    Text(
        "TOMILO LIB ${BuildConfig.VERSION_NAME} ($channel · ${BuildConfig.VERSION_CODE})$debug",
        color = TomiloMuted,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ProfileStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TomiloBg.copy(alpha = 0.45f))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
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
