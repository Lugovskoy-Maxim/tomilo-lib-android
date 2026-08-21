package ru.tomilo.lib.mobile.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Surface
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import ru.tomilo.lib.mobile.ui.components.DecoratedAvatar
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
    onOpenGames: () -> Unit,
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
    var moreExpanded by rememberSaveable { mutableStateOf(false) }

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
                title = { Text("Я") },
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
                        DecoratedAvatar(
                            avatarUrl = user!!.avatar,
                            username = user!!.username,
                            decorations = user!!.decorations(),
                            size = 88.dp,
                            ringColor = if (premium) TomiloPremium else TomiloPrimary.copy(alpha = 0.45f),
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
                Spacer(Modifier.height(22.dp))
                ProfileSectionHeader("Быстрый доступ", "Самое нужное — без поиска по меню")
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileQuickAction(
                        Icons.Default.DownloadForOffline,
                        "Офлайн",
                        formatBytes(offlineBytes),
                        onOpenOffline,
                        Modifier.weight(1f),
                    )
                    ProfileQuickAction(
                        Icons.Default.NotificationsNone,
                        "Уведомления",
                        if (notifUnread > 0) "$notifUnread новых" else "Всё прочитано",
                        onOpenNotifications,
                        Modifier.weight(1f),
                        badge = notifUnread.takeIf { it > 0 }?.let { if (it > 99) "99+" else "$it" },
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileQuickAction(
                        Icons.Default.Group,
                        "Друзья",
                        "Люди и заявки",
                        onOpenFriends,
                        Modifier.weight(1f),
                    )
                    ProfileQuickAction(
                        Icons.Default.SportsEsports,
                        "Игры",
                        "Арена и секта",
                        onOpenGames,
                        Modifier.weight(1f),
                        iconTint = Color(0xFF9B8CFF),
                    )
                }
                Spacer(Modifier.height(18.dp))
                ActionRow(
                    icon = Icons.Default.Star,
                    title = if (premium) "Premium активен" else "Tomilo Premium",
                    subtitle = if (premium) "Управление подпиской" else "Премиум-главы, офлайн и без рекламы",
                    badge = if (premium) "АКТИВЕН" else null,
                    iconTint = TomiloPremium,
                    onClick = onOpenPremium,
                )
                Spacer(Modifier.height(22.dp))
                ProfileSectionHeader("Награды и коллекция", "Опыт, монеты и оформление профиля")
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileQuickAction(Icons.Default.TaskAlt, "Задания", "Получить XP", onOpenQuests, Modifier.weight(1f))
                    ProfileQuickAction(Icons.Default.Casino, "Колесо", "Испытать удачу", onOpenWheel, Modifier.weight(1f), iconTint = TomiloPremium)
                    ProfileQuickAction(Icons.Default.ShoppingBag, "Магазин", "${user!!.balance ?: 0} монет", onOpenShop, Modifier.weight(1f), iconTint = TomiloPremium)
                }
                Spacer(Modifier.height(22.dp))
                Surface(
                    onClick = { moreExpanded = !moreExpanded },
                    modifier = Modifier.fillMaxWidth(),
                    color = TomiloSurface,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Row(
                        Modifier.border(1.dp, TomiloBorder.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Все возможности", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (moreExpanded) "Нажмите, чтобы свернуть" else "История, лидеры, настройки и сервисы",
                                color = TomiloMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        androidx.compose.material3.Icon(
                            if (moreExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (moreExpanded) "Свернуть" else "Показать всё",
                            tint = TomiloPrimary,
                        )
                    }
                }
                AnimatedVisibility(
                    visible = moreExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column {
                        Spacer(Modifier.height(18.dp))
                        ProfileSectionHeader("Чтение", "История и новые главы")
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionRow(Icons.Default.History, "История чтения", onOpenHistory, subtitle = "Продолжить с последней главы")
                            ActionRow(Icons.Default.Update, "Обновления каталога", onOpenUpdates, subtitle = "Свежие главы и релизы")
                        }
                        Spacer(Modifier.height(18.dp))
                        ProfileSectionHeader("Сообщество", "Рейтинг и пространство Tomilo")
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionRow(Icons.Default.Leaderboard, "Лидеры", onOpenLeaders, subtitle = "Рейтинг активных читателей")
                            ActionRow(Icons.Default.Explore, "Мир Tomilo", onOpenHub, subtitle = "Подборки, новости и гайды")
                            if (user!!.isStaff()) {
                                ActionRow(Icons.Default.AdminPanelSettings, "Панель управления", onOpenAdmin, subtitle = "Инструменты команды")
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        ProfileSectionHeader("Настройки", "Контент, память и аккаунт")
                        AdultToggleRow(
                            contentSettings = contentSettings,
                            onToggle = { show -> scope.launch { contentPrefs.setShowAdult(show) } },
                        )
                        Spacer(Modifier.height(8.dp))
                        ActionRow(
                            Icons.Default.CleaningServices,
                            "Очистить кеш",
                            onClick = {
                                scope.launch {
                                    context.imageLoader.memoryCache?.clear()
                                    context.imageLoader.diskCache?.clear()
                                    offlineBytes = offlineRepository.offlineBytesTotal()
                                    cacheMsg = "Кеш очищен, офлайн-главы сохранены"
                                }
                            },
                            subtitle = "Офлайн-главы останутся на устройстве",
                        )
                        cacheMsg?.let {
                            Spacer(Modifier.height(6.dp))
                            Text(it, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        ActionRow(
                            Icons.AutoMirrored.Filled.Logout,
                            "Выйти из аккаунта",
                            onClick = { confirmLogout = true },
                            subtitle = "Данные аккаунта сохранятся в облаке",
                            iconTint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
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
private fun ProfileSectionHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProfileQuickAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    iconTint: Color = TomiloPrimary,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(108.dp),
        shape = RoundedCornerShape(20.dp),
        color = TomiloSurface,
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier
                .border(1.dp, TomiloBorder.copy(alpha = 0.68f), RoundedCornerShape(20.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                        .background(iconTint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.weight(1f))
                badge?.let {
                    Text(
                        it,
                        color = iconTint,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(subtitle, color = TomiloMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
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
