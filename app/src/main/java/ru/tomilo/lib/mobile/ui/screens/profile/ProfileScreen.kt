package ru.tomilo.lib.mobile.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.BuildConfig
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.data.local.ContentPrefs
import ru.tomilo.lib.mobile.data.local.ContentSettings
import ru.tomilo.lib.mobile.data.local.ReadingPrefs
import ru.tomilo.lib.mobile.data.local.ReadingSettings
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.OfflineRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.ActionRow
import ru.tomilo.lib.mobile.ui.components.ConfirmActionDialog
import ru.tomilo.lib.mobile.ui.components.DecoratedAvatar
import ru.tomilo.lib.mobile.ui.components.TomiloRingLogo
import ru.tomilo.lib.mobile.ui.components.TomiloWordmark
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import ru.tomilo.lib.mobile.ui.theme.TomiloText

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class)
@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    offlineRepository: OfflineRepository,
    contentPrefs: ContentPrefs,
    readingPrefs: ReadingPrefs,
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
    val readingSettings by readingPrefs.settingsFlow.collectAsState(initial = ReadingSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var notifUnread by remember { mutableIntStateOf(0) }
    var offlineBytes by remember { mutableLongStateOf(0L) }
    var cacheMsg by remember { mutableStateOf<String?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }
    var selectedProfileTab by rememberSaveable { mutableIntStateOf(0) }
    var settingsExpanded by rememberSaveable { mutableStateOf(false) }

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
                title = {
                    Text(
                        "Профиль",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TomiloText,
                    )
                },
                colors = tomiloTopBarColors(),
                actions = {
                    if (user != null) {
                        IconButton(onClick = onOpenNotifications) {
                            Box {
                                Icon(
                                    Icons.Default.NotificationsNone,
                                    contentDescription = "Уведомления",
                                    tint = TomiloText,
                                )
                                if (notifUnread > 0) {
                                    Box(
                                        Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(TomiloPrimary)
                                            .align(Alignment.TopEnd),
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        ) {
            if (user == null) {
                // Logged-out state
                LoggedOutProfileCard(
                    onLogin = onLogin,
                    onOpenLeaders = onOpenLeaders,
                    onOpenPremium = onOpenPremium,
                    contentSettings = contentSettings,
                    onToggleAdult = { show -> scope.launch { contentPrefs.setShowAdult(show) } },
                )
            } else {
                val premium = Premium.isActive(user!!.subscriptionExpiresAt)

                // 1. User Header & Level Progress Card
                UserProfileHeaderCard(
                    user = user!!,
                    isPremium = premium,
                    onOpenPublic = { onOpenMyPublicProfile(user!!.stableId()) },
                    onCopyId = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Tomilo User ID", user!!.stableId()))
                        Toast.makeText(context, "ID скопирован в буфер обмена", Toast.LENGTH_SHORT).show()
                    },
                )

                Spacer(Modifier.height(16.dp))

                // Modern Segmented Tab Switcher (Website-grade profile UX)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = TomiloSurface2,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier
                            .padding(4.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        listOf(
                            Triple(0, "Обзор", Icons.Default.Dashboard),
                            Triple(1, "Кастомизация", Icons.Default.Palette),
                            Triple(2, "Настройки", Icons.Default.Settings),
                        ).forEach { (tabIdx, label, icon) ->
                            val isSelected = selectedProfileTab == tabIdx
                            Surface(
                                onClick = { selectedProfileTab = tabIdx },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) TomiloPrimary else Color.Transparent,
                                modifier = Modifier.weight(1f),
                            ) {
                                Row(
                                    Modifier.padding(vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else TomiloMuted,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        label,
                                        color = if (isSelected) Color.White else TomiloMuted,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                when (selectedProfileTab) {
                    0 -> {
                        // 1. Reading Statistics
                        UserReadingStatisticsGrid(user = user!!)

                        Spacer(Modifier.height(20.dp))

                        // 2. Services Hub
                        ProfileSectionHeader("Сервисы и награды", "Бонусы, валюта и коллекция")
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProfileQuickCard(
                                icon = Icons.Default.TaskAlt,
                                title = "Задания",
                                subtitle = "XP и награды",
                                onClick = onOpenQuests,
                                iconTint = Color(0xFF4CAF50),
                                modifier = Modifier.weight(1f),
                            )
                            ProfileQuickCard(
                                icon = Icons.Default.Casino,
                                title = "Колесо",
                                subtitle = "Испытать удачу",
                                onClick = onOpenWheel,
                                iconTint = TomiloPremium,
                                modifier = Modifier.weight(1f),
                            )
                            ProfileQuickCard(
                                icon = Icons.Default.ShoppingBag,
                                title = "Магазин",
                                subtitle = "${user!!.balance ?: 0} монет",
                                onClick = onOpenShop,
                                iconTint = TomiloPrimary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProfileQuickCard(
                                icon = Icons.Default.DownloadForOffline,
                                title = "Офлайн",
                                subtitle = formatBytes(offlineBytes),
                                onClick = onOpenOffline,
                                iconTint = Color(0xFF29B6F6),
                                modifier = Modifier.weight(1f),
                            )
                            ProfileQuickCard(
                                icon = Icons.Default.SportsEsports,
                                title = "Арена & Игры",
                                subtitle = "Дуэли и секты",
                                onClick = onOpenGames,
                                iconTint = Color(0xFFAB47BC),
                                modifier = Modifier.weight(1f),
                            )
                            ProfileQuickCard(
                                icon = Icons.Default.Group,
                                title = "Друзья",
                                subtitle = "Заявки и чаты",
                                onClick = onOpenFriends,
                                iconTint = Color(0xFFFF7043),
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        // 3. Tomilo Premium Banner
                        ProfilePremiumBanner(
                            isPremium = premium,
                            onClick = onOpenPremium,
                        )

                        Spacer(Modifier.height(20.dp))

                        // 4. Reading & Catalog
                        ProfileSectionHeader("Чтение и каталог", "История и свежие главы")
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionRow(Icons.Default.History, "История чтения", onOpenHistory, subtitle = "Все прочитанные тайтлы и главы")
                            ActionRow(Icons.Default.Update, "Обновления каталога", onOpenUpdates, subtitle = "Свежие релизы авторов")
                            ActionRow(Icons.Default.Leaderboard, "Рейтинг читателей", onOpenLeaders, subtitle = "Топ читателей по опыту")
                            ActionRow(Icons.Default.Explore, "Мир Tomilo", onOpenHub, subtitle = "Подборки, новости и сообщество")
                        }
                    }
                    1 -> {
                        ProfileCustomizationTab(
                            user = user!!,
                            isPremium = premium,
                            onOpenShop = onOpenShop,
                            onOpenPremium = onOpenPremium,
                        )
                    }
                    2 -> {
                        ProfileSettingsTab(
                            readingSettings = readingSettings,
                            contentSettings = contentSettings,
                            offlineBytes = offlineBytes,
                            cacheMsg = cacheMsg,
                            isStaff = user!!.isStaff(),
                            onKeepScreenOn = { scope.launch { readingPrefs.setKeepScreenOn(it) } },
                            onStartFullscreen = { scope.launch { readingPrefs.setStartFullscreen(it) } },
                            onAutoScrollSpeed = { scope.launch { readingPrefs.setAutoScrollSpeed(it) } },
                            onToggleAdult = { show -> scope.launch { contentPrefs.setShowAdult(show) } },
                            onClearCache = {
                                scope.launch {
                                    context.imageLoader.memoryCache?.clear()
                                    context.imageLoader.diskCache?.clear()
                                    offlineBytes = offlineRepository.offlineBytesTotal()
                                    cacheMsg = "Кеш изображений успешно очищен"
                                }
                            },
                            onOpenAdmin = onOpenAdmin,
                            onLogout = { confirmLogout = true },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            AppVersionFooter()
        }
    }

    if (confirmLogout) {
        ConfirmActionDialog(
            title = "Выйти из аккаунта?",
            message = "Закладки, история и уровень сохранятся в вашем аккаунте Tomilo.",
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
private fun UserProfileHeaderCard(
    user: ru.tomilo.lib.mobile.data.api.UserDto,
    isPremium: Boolean,
    onOpenPublic: () -> Unit,
    onCopyId: () -> Unit,
) {
    val level = user.level ?: 1
    val exp = user.experience ?: 0
    val nextLevelExp = level * 250
    val currentLevelBaseExp = (level - 1) * 250
    val levelExpDelta = (exp - currentLevelBaseExp).coerceAtLeast(0)
    val neededExp = (nextLevelExp - currentLevelBaseExp).coerceAtLeast(1)
    val progress = (levelExpDelta.toFloat() / neededExp.toFloat()).coerceIn(0f, 1f)

    val rankTitle = when {
        level <= 2 -> "Новичок историй"
        level <= 5 -> "Читатель-адепт"
        level <= 9 -> "Книжный червь"
        level <= 15 -> "Знаток манги"
        level <= 25 -> "Мастер сюжетов"
        else -> "Легендарный читатель"
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = TomiloSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPremium) TomiloPremium.copy(alpha = 0.38f) else TomiloPrimary.copy(alpha = 0.22f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            // Website-Style Profile Hero Cover Banner
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        Brush.horizontalGradient(
                            if (isPremium) listOf(
                                Color(0xFF2C1900),
                                Color(0xFF4C3308),
                                Color(0xFF7A540C),
                                Color(0xFF2C1900),
                            )
                            else listOf(
                                TomiloPrimary.copy(alpha = 0.35f),
                                Color(0xFF1E2640),
                                Color(0xFF281E38),
                                TomiloPrimary.copy(alpha = 0.20f),
                            ),
                        ),
                    ),
            )

            Column(
                Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (isPremium) TomiloPremium.copy(alpha = 0.12f) else TomiloPrimary.copy(alpha = 0.10f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                DecoratedAvatar(
                    avatarUrl = user.avatar,
                    username = user.username,
                    decorations = user.decorations(),
                    size = 80.dp,
                    ringColor = if (isPremium) TomiloPremium else TomiloPrimary,
                )

                Spacer(Modifier.width(16.dp))

                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            user.username ?: "Пользователь",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = TomiloText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (isPremium) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(TomiloPremium, Color(0xFFFFB300)),
                                        ),
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text("PRO", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        if (user.isAdmin()) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE53935))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text("ADMIN", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPremium) TomiloPremium.copy(alpha = 0.15f) else TomiloPrimary.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isPremium) TomiloPremium else TomiloPrimary,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    rankTitle,
                                    color = if (isPremium) TomiloPremium else TomiloPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        user.email?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it,
                                color = TomiloMuted,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Surface(
                        onClick = onCopyId,
                        color = TomiloSurface2,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(
                                "ID: ${user.stableId().take(8)}…",
                                color = TomiloMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Копировать ID",
                                tint = TomiloMuted,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Level & XP Progression Bar
            Surface(
                color = TomiloSurface2.copy(alpha = 0.85f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPremium) TomiloPremium.copy(alpha = 0.2f) else TomiloPrimary.copy(alpha = 0.18f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                "УРОВЕНЬ $level",
                                fontWeight = FontWeight.Black,
                                color = if (isPremium) TomiloPremium else TomiloPrimary,
                                fontSize = 12.sp,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "$exp / $nextLevelExp XP · ${(progress * 100).toInt()}%",
                            color = TomiloText,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Custom gradient progress bar
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(TomiloBg),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress)
                                .height(8.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        if (isPremium) listOf(TomiloPremium, Color(0xFFFFB300))
                                        else listOf(TomiloPrimary, Color(0xFFFF7043)),
                                    ),
                                ),
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "До уровня ${level + 1}: ещё ${(nextLevelExp - exp).coerceAtLeast(0)} XP",
                            color = TomiloMuted,
                            fontSize = 11.sp,
                        )
                        Text(
                            "+250 XP/ур.",
                            color = TomiloMuted.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                onClick = onOpenPublic,
                shape = RoundedCornerShape(14.dp),
                color = TomiloSurface2.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, TomiloBorder),
                modifier = Modifier.fillMaxWidth().height(42.dp),
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Открыть публичный профиль",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TomiloText,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = TomiloPrimary,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}
}

@Composable
private fun UserReadingStatisticsGrid(user: ru.tomilo.lib.mobile.data.api.UserDto) {
    val chapters = user.readChaptersTotal()
    val titles = user.titlesReadCount ?: 0
    val streak = user.currentStreak ?: 0
    val coins = user.balance ?: 0
    val minutes = user.readingTimeMinutes ?: 0
    val timeLabel = when {
        minutes >= 60 -> "${minutes / 60} ч. ${minutes % 60} м."
        minutes > 0 -> "$minutes мин."
        else -> "0 мин."
    }
    val comments = user.commentsCount ?: 0
    val likes = user.likesReceivedCount ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Hero Streak Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = TomiloSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5722).copy(alpha = 0.28f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFF5722).copy(alpha = 0.18f),
                                Color(0xFFFF9800).copy(alpha = 0.08f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFF5722).copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Серия чтения: ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TomiloText,
                        )
                        Text(
                            "$streak дн. подряд",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF7043),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (streak > 0) "Отличный темп! Читай каждый день, чтобы получать ежедневные бонусы"
                        else "Прочитай хотя бы одну главу сегодня, чтобы запустить серию дней!",
                        color = TomiloMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                    )
                }
            }
        }

        // Bento 6-metric Grid
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                icon = Icons.Outlined.AutoStories,
                value = "$chapters",
                label = "Глав прочитано",
                tint = TomiloPrimary,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Default.Star,
                value = "$titles",
                label = "В закладках",
                tint = Color(0xFF29B6F6),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Outlined.Schedule,
                value = timeLabel,
                label = "Время чтения",
                tint = Color(0xFFAB47BC),
                modifier = Modifier.weight(1f),
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                icon = Icons.Default.Casino,
                value = "$coins",
                label = "Баланс монет",
                tint = TomiloPremium,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Default.Group,
                value = "$comments",
                label = "Комментариев",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Default.Favorite,
                value = "$likes",
                label = "Лайков",
                tint = Color(0xFFE91E63),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = TomiloSurface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = modifier,
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TomiloText,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                color = TomiloMuted,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ProfileQuickCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = TomiloPrimary,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(102.dp),
        shape = RoundedCornerShape(20.dp),
        color = TomiloSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.065f)),
    ) {
        Column(
            Modifier.padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TomiloText,
                    maxLines = 1,
                )
                Text(
                    subtitle,
                    color = TomiloMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ProfileCustomizationTab(
    user: ru.tomilo.lib.mobile.data.api.UserDto,
    isPremium: Boolean,
    onOpenShop: () -> Unit,
    onOpenPremium: () -> Unit,
) {
    val decor = user.decorations()
    val gold = Color(0xFFFFD700)

    val rankTitle = when {
        (user.level ?: 1) <= 2 -> "Новичок историй"
        (user.level ?: 1) <= 5 -> "Читатель-адепт"
        (user.level ?: 1) <= 9 -> "Книжный червь"
        (user.level ?: 1) <= 15 -> "Знаток манги"
        (user.level ?: 1) <= 25 -> "Мастер сюжетов"
        else -> "Легендарный читатель"
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Live Profile Card Preview
        ProfileSectionHeader("Предпросмотр профиля", "Так вашу карточку видят другие читатели")
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = TomiloSurface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isPremium) gold.copy(alpha = 0.35f) else TomiloPrimary.copy(alpha = 0.20f),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (isPremium) gold.copy(alpha = 0.12f) else TomiloPrimary.copy(alpha = 0.10f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DecoratedAvatar(
                        avatarUrl = user.avatar,
                        username = user.username,
                        decorations = decor,
                        size = 68.dp,
                        ringColor = if (isPremium) gold else TomiloPrimary,
                    )

                    Spacer(Modifier.width(14.dp))

                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                user.username ?: "Читатель",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TomiloText,
                            )
                            if (isPremium) {
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            Brush.horizontalGradient(listOf(gold, Color(0xFFFFB300))),
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text("PRO", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Text(
                            rankTitle,
                            color = if (isPremium) gold else TomiloPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            "Уровень ${user.level ?: 1} · ${user.readChaptersTotal()} глав",
                            color = TomiloMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }

        // Customization Status / Premium Perk Banner
        Surface(
            onClick = if (!isPremium) onOpenPremium else onOpenShop,
            shape = RoundedCornerShape(20.dp),
            color = if (isPremium) gold.copy(alpha = 0.10f) else TomiloSurface2,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isPremium) gold.copy(alpha = 0.35f) else TomiloBorder,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isPremium) gold.copy(alpha = 0.20f) else TomiloPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isPremium) Icons.Default.AutoAwesome else Icons.Default.Palette,
                        contentDescription = null,
                        tint = if (isPremium) gold else TomiloPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        if (isPremium) "Кастомизация разблокирована" else "Кастомизация профиля",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isPremium) gold else TomiloText,
                    )
                    Text(
                        if (isPremium) "Привилегия Tomilo Premium активна · Скидка 20% в магазине"
                        else "Доступна в Premium (150 ₽ / мес) — рамки, ауры, градиенты ника",
                        color = TomiloMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (!isPremium) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(gold)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text("150 ₽", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Equipped Customization Slots
        ProfileSectionHeader("Активные элементы", "Экипированные украшения из вашей коллекции")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EquippedSlotCard(
                slotName = "Рамка аватара",
                itemName = if (decor?.frameUrl() != null) "Эксклюзивная рамка" else "Стандартная рамка",
                icon = Icons.Default.Palette,
                onOpenShop = onOpenShop,
            )
            EquippedSlotCard(
                slotName = "Бейдж читателя",
                itemName = if (decor?.badgeUrl() != null) "Активный бейдж" else (if (isPremium) "Бейдж Tomilo PRO" else "Без бейджа"),
                icon = Icons.Default.AutoAwesome,
                onOpenShop = onOpenShop,
            )
            EquippedSlotCard(
                slotName = "Цвет и градиент ника",
                itemName = if (isPremium) "Золотой градиент PRO" else "По умолчанию (белый)",
                icon = Icons.Default.Star,
                onOpenShop = onOpenShop,
            )
            EquippedSlotCard(
                slotName = "Фон карточки и тема",
                itemName = if (decor?.cardUrl() != null || decor?.backgroundUrl() != null) "Кастомный фон" else "Стандартная тема",
                icon = Icons.Default.WorkspacePremium,
                onOpenShop = onOpenShop,
            )
        }

        // Shop Link Button
        Button(
            onClick = onOpenShop,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TomiloPrimary,
                contentColor = Color.White,
            ),
        ) {
            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Перейти в Магазин предметов", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (isPremium) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.20f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text("-20%", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun EquippedSlotCard(
    slotName: String,
    itemName: String,
    icon: ImageVector,
    onOpenShop: () -> Unit,
) {
    Surface(
        onClick = onOpenShop,
        color = TomiloSurface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TomiloBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TomiloSurface2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = TomiloPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(slotName, color = TomiloMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(itemName, color = TomiloText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Text("Изменить", color = TomiloPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileSettingsTab(
    readingSettings: ReadingSettings,
    contentSettings: ContentSettings,
    offlineBytes: Long,
    cacheMsg: String?,
    isStaff: Boolean,
    onKeepScreenOn: (Boolean) -> Unit,
    onStartFullscreen: (Boolean) -> Unit,
    onAutoScrollSpeed: (Float) -> Unit,
    onToggleAdult: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onOpenAdmin: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Reader Settings
        ProfileSectionHeader("Настройки читалки", "Параметры отображения и комфорта")
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = TomiloSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, TomiloBorder, RoundedCornerShape(20.dp)),
        ) {
            Column(Modifier.padding(16.dp)) {
                // Keep Screen On
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Не гасить экран при чтении", style = MaterialTheme.typography.titleSmall, color = TomiloText)
                        Text("Экран остаётся включённым в читалке", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = readingSettings.keepScreenOn,
                        onCheckedChange = onKeepScreenOn,
                        colors = SwitchDefaults.colors(checkedThumbColor = TomiloPrimary),
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Fullscreen Mode
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Полноэкранный режим", style = MaterialTheme.typography.titleSmall, color = TomiloText)
                        Text("Скрывать системные панели при старте", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = readingSettings.startFullscreen,
                        onCheckedChange = onStartFullscreen,
                        colors = SwitchDefaults.colors(checkedThumbColor = TomiloPrimary),
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Auto-scroll Speed
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Скорость автопрокрутки", style = MaterialTheme.typography.titleSmall, color = TomiloText, modifier = Modifier.weight(1f))
                        Text("%.1fx".format(readingSettings.autoScrollSpeed), color = TomiloPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = readingSettings.autoScrollSpeed,
                        onValueChange = onAutoScrollSpeed,
                        valueRange = 0.5f..4.0f,
                        steps = 6,
                        colors = SliderDefaults.colors(
                            thumbColor = TomiloPrimary,
                            activeTrackColor = TomiloPrimary,
                            inactiveTrackColor = TomiloSurface2,
                        ),
                    )
                }
            }
        }

        // 2. Content & Restrictions
        ProfileSectionHeader("Контент и цензура", "Ограничение возрастных материалов")
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = TomiloSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, TomiloBorder, RoundedCornerShape(20.dp)),
        ) {
            Column(Modifier.padding(16.dp)) {
                AdultToggleRow(
                    contentSettings = contentSettings,
                    onToggle = onToggleAdult,
                )
            }
        }

        // 3. Memory & Cache
        ProfileSectionHeader("Память и данные", "Очистка кеша и офлайн-главы")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionRow(
                icon = Icons.Default.CleaningServices,
                title = "Очистить кеш обложек",
                subtitle = "Освобождает оперативную и flash-память",
                onClick = onClearCache,
            )
            cacheMsg?.let {
                Text(it, color = TomiloPrimary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
            }
            Surface(
                color = TomiloSurface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TomiloBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF29B6F6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.DownloadForOffline, contentDescription = null, tint = Color(0xFF29B6F6), modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Офлайн-хранилище", color = TomiloText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Занято сохраненными главами", color = TomiloMuted, fontSize = 11.sp)
                    }
                    Text(formatBytes(offlineBytes), color = Color(0xFF29B6F6), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // 4. Moderation / Staff if applicable
        if (isStaff) {
            ProfileSectionHeader("Администрирование", "Инструменты команды проекта")
            ActionRow(
                icon = Icons.Default.AdminPanelSettings,
                title = "Панель модератора",
                subtitle = "Управление контентом и жалобами",
                onClick = onOpenAdmin,
            )
        }

        // 5. Account & Session
        ProfileSectionHeader("Учетная запись", "Управление сессией")
        ActionRow(
            icon = Icons.AutoMirrored.Filled.Logout,
            title = "Выйти из аккаунта",
            subtitle = "Данные и закладки сохранятся на сервере",
            onClick = onLogout,
            iconTint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ProfilePremiumBanner(
    isPremium: Boolean,
    onClick: () -> Unit,
) {
    val gold = Color(0xFFFFD700)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = TomiloSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPremium) gold.copy(alpha = 0.45f) else gold.copy(alpha = 0.28f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            gold.copy(alpha = if (isPremium) 0.18f else 0.12f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(18.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(gold.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = gold,
                        modifier = Modifier.size(26.dp),
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Tomilo Premium",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = gold,
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(gold)
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        ) {
                            Text(
                                if (isPremium) "АКТИВЕН" else "VIP",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (isPremium) "Все привилегии активны: без рекламы, безлимитный офлайн, кастомизация и скидка 20%"
                        else "150 ₽ / мес · Без рекламы, безлимит офлайн, кастомизация профиля и -20% в магазине",
                        color = TomiloMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                    )
                }
            }

            if (!isPremium) {
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("⚡ Без рекламы", "⬇️ Офлайн", "🎨 Профиль", "🛍️ -20%").forEach { perk ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(gold.copy(alpha = 0.10f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(perk, color = gold, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoggedOutProfileCard(
    onLogin: () -> Unit,
    onOpenLeaders: () -> Unit,
    onOpenPremium: () -> Unit,
    contentSettings: ContentSettings,
    onToggleAdult: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = TomiloSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TomiloPrimary.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            TomiloPrimary.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TomiloRingLogo(size = 68.dp)
            Spacer(Modifier.height(12.dp))
            TomiloWordmark(maxWidth = 160.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                "Твой читательский профиль",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TomiloText,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Войди в аккаунт, чтобы сохранять закладки, участвовать в рейтинге, получать награды за задания и общаться с читателями.",
                color = TomiloMuted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(Modifier.height(18.dp))

            // Value props
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(TomiloSurface2.copy(alpha = 0.6f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LoggedOutBenefitRow("📚 Облачная синхронизация закладок и истории")
                LoggedOutBenefitRow("🏆 Прокачка уровней, ежедневные квесты и колесо")
                LoggedOutBenefitRow("💬 Комментарии, рецензии и таблица лидеров")
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TomiloPrimary),
            ) {
                Text("Войти в аккаунт", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onOpenLeaders,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Лидеры", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onOpenPremium,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Premium", color = TomiloPremium, maxLines = 1)
                }
            }

            Spacer(Modifier.height(16.dp))
            AdultToggleRow(contentSettings = contentSettings, onToggle = onToggleAdult)
        }
    }
}

@Composable
private fun LoggedOutBenefitRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = TomiloText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AdultToggleRow(
    contentSettings: ContentSettings,
    onToggle: (Boolean) -> Unit,
) {
    val canEnable = contentSettings.isAdultUser == true
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Показывать 18+", style = MaterialTheme.typography.titleSmall, color = TomiloText)
            Text(
                when {
                    contentSettings.isAdultUser == false -> "Недоступно (возраст < 18)"
                    contentSettings.showAdultContent -> "Включено в каталоге и поиске"
                    else -> "Скрыто по умолчанию"
                },
                color = TomiloMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = contentSettings.showAdultContent && canEnable,
            onCheckedChange = { if (canEnable) onToggle(it) },
            enabled = canEnable,
            colors = SwitchDefaults.colors(checkedThumbColor = TomiloPrimary),
        )
    }
}

@Composable
private fun ProfileSectionHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TomiloText)
        Text(subtitle, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AppVersionFooter() {
    val channel = when (BuildConfig.STORE_CHANNEL) {
        "play" -> "Google Play"
        else -> "RuStore"
    }
    Text(
        "TOMILO LIB v${BuildConfig.VERSION_NAME} ($channel · b${BuildConfig.VERSION_CODE})",
        color = TomiloMuted.copy(alpha = 0.6f),
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.2f GB".format(mb / 1024.0)
}
