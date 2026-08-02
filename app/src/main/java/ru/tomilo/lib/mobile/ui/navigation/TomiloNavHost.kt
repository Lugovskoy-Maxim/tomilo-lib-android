package ru.tomilo.lib.mobile.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.AppContainer
import ru.tomilo.lib.mobile.data.local.ContentSettings
import ru.tomilo.lib.mobile.ui.components.AgeGateDialog
import ru.tomilo.lib.mobile.ui.screens.admin.AdminScreen
import ru.tomilo.lib.mobile.ui.screens.auth.LoginScreen
import ru.tomilo.lib.mobile.ui.screens.bookmarks.BookmarksScreen
import ru.tomilo.lib.mobile.ui.screens.catalog.CatalogScreen
import ru.tomilo.lib.mobile.ui.screens.chats.ChatThreadScreen
import ru.tomilo.lib.mobile.ui.screens.chats.ChatsScreen
import ru.tomilo.lib.mobile.ui.screens.history.HistoryScreen
import ru.tomilo.lib.mobile.ui.screens.home.HomeScreen
import ru.tomilo.lib.mobile.ui.screens.leaders.LeadersScreen
import ru.tomilo.lib.mobile.ui.screens.notifications.NotificationsScreen
import ru.tomilo.lib.mobile.ui.screens.offline.OfflineLibraryScreen
import ru.tomilo.lib.mobile.ui.screens.premium.PremiumScreen
import ru.tomilo.lib.mobile.ui.screens.profile.ProfileScreen
import ru.tomilo.lib.mobile.ui.screens.reader.ReaderScreen
import ru.tomilo.lib.mobile.ui.screens.title.TitleScreen
import ru.tomilo.lib.mobile.ui.screens.user.UserProfileScreen
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val Home = "home"
    const val Catalog = "catalog"
    const val Search = "search"
    const val Bookmarks = "bookmarks"
    const val Chats = "chats"
    const val Profile = "profile"
    const val Offline = "offline"
    const val Leaders = "leaders"
    const val Notifications = "notifications"
    const val History = "history"
    const val Admin = "admin"
    const val Login = "login"
    const val Premium = "premium"
    const val Title = "title/{key}"
    const val Reader = "reader/{chapterId}?offline={offline}&titleId={titleId}"
    /** title в path — надёжнее, чем query, для кириллицы */
    const val ChatThread = "chat/{id}/{title}"
    const val User = "user/{id}"

    fun title(key: String) = "title/${enc(key)}"
    fun reader(chapterId: String, offline: Boolean = false, titleId: String? = null) =
        "reader/${enc(chapterId)}?offline=$offline&titleId=${enc(titleId.orEmpty())}"
    fun chat(id: String, title: String) =
        "chat/${enc(id)}/${enc(title.ifBlank { "Чат" })}"
    fun user(id: String) = "user/${enc(id)}"

    fun enc(v: String) = URLEncoder.encode(v, StandardCharsets.UTF_8.toString()).replace("+", "%20")
    fun dec(v: String) = URLDecoder.decode(v, StandardCharsets.UTF_8.toString())
}

private data class Tab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
fun TomiloNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route.orEmpty()
    val contentSettings by container.contentPrefs.settingsFlow.collectAsState(initial = ContentSettings())
    val scope = rememberCoroutineScope()

    val tabs = listOf(
        Tab(Routes.Home, "Главная", Icons.Default.Home),
        Tab(Routes.Catalog, "Каталог", Icons.Default.GridView),
        Tab(Routes.Bookmarks, "Закладки", Icons.Default.Bookmark),
        Tab(Routes.Chats, "Чаты", Icons.AutoMirrored.Filled.Chat),
        Tab(Routes.Profile, "Профиль", Icons.Default.Person),
    )
    val showBottomBar = tabs.any { current == it.route }

    fun goLogin() = navController.navigate(Routes.Login)

    if (!contentSettings.ageGateAnswered) {
        AgeGateDialog(
            onAdult = { scope.launch { container.contentPrefs.answerAgeGate(isAdult = true) } },
            onMinor = { scope.launch { container.contentPrefs.answerAgeGate(isAdult = false) } },
        )
    }

    // Только нижний inset (tab bar). Верх (status bar) обрабатывают экраны/TopAppBar —
    // иначе двойной отступ и «пустое место» сверху.
    Scaffold(
        containerColor = TomiloBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = TomiloBg,
                    windowInsets = NavigationBarDefaults.windowInsets,
                ) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home,
            modifier = Modifier
                .padding(bottom = padding.calculateBottomPadding())
                .then(
                    if (!showBottomBar) {
                        Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    } else {
                        Modifier
                    },
                ),
        ) {
            composable(Routes.Home) {
                HomeScreen(
                    catalogRepository = container.catalogRepository,
                    contentPrefs = container.contentPrefs,
                    onOpenTitle = { id, slug ->
                        navController.navigate(Routes.title(slug?.takeIf { it.isNotBlank() } ?: id))
                    },
                    onOpenCatalog = {
                        navController.navigate(Routes.Catalog) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Routes.Catalog) {
                CatalogScreen(
                    catalogRepository = container.catalogRepository,
                    contentPrefs = container.contentPrefs,
                    onOpenTitle = { id, slug ->
                        navController.navigate(Routes.title(slug?.takeIf { it.isNotBlank() } ?: id))
                    },
                )
            }
            composable(Routes.Bookmarks) {
                BookmarksScreen(
                    authRepository = container.authRepository,
                    socialRepository = container.socialRepository,
                    historyRepository = container.historyRepository,
                    onLogin = { goLogin() },
                    onOpenTitle = { id, slug ->
                        navController.navigate(Routes.title(slug?.takeIf { it.isNotBlank() } ?: id))
                    },
                )
            }
            composable(Routes.Chats) {
                ChatsScreen(
                    authRepository = container.authRepository,
                    socialRepository = container.socialRepository,
                    onLogin = { goLogin() },
                    onOpenChat = { id, title ->
                        navController.navigate(Routes.chat(id, title))
                    },
                )
            }
            composable(Routes.Profile) {
                ProfileScreen(
                    authRepository = container.authRepository,
                    socialRepository = container.socialRepository,
                    offlineRepository = container.offlineRepository,
                    contentPrefs = container.contentPrefs,
                    onLogin = { goLogin() },
                    onOpenOffline = { navController.navigate(Routes.Offline) },
                    onOpenNotifications = { navController.navigate(Routes.Notifications) },
                    onOpenLeaders = { navController.navigate(Routes.Leaders) },
                    onOpenHistory = { navController.navigate(Routes.History) },
                    onOpenAdmin = { navController.navigate(Routes.Admin) },
                    onOpenPremium = { navController.navigate(Routes.Premium) },
                    onOpenMyPublicProfile = { id -> navController.navigate(Routes.user(id)) },
                )
            }
            composable(Routes.Premium) {
                PremiumScreen(
                    authRepository = container.authRepository,
                    onBack = { navController.popBackStack() },
                    onLogin = { goLogin() },
                )
            }
            composable(Routes.History) {
                HistoryScreen(
                    authRepository = container.authRepository,
                    historyRepository = container.historyRepository,
                    onBack = { navController.popBackStack() },
                    onLogin = { goLogin() },
                    onOpenTitle = { id, slug ->
                        navController.navigate(Routes.title(slug?.takeIf { it.isNotBlank() } ?: id))
                    },
                    onOpenChapter = { titleId, chapterId ->
                        navController.navigate(Routes.reader(chapterId, offline = false, titleId = titleId))
                    },
                )
            }
            composable(Routes.Admin) {
                AdminScreen(
                    adminRepository = container.adminRepository,
                    onBack = { navController.popBackStack() },
                    onOpenTitle = { id, slug ->
                        navController.navigate(Routes.title(slug?.takeIf { it.isNotBlank() } ?: id))
                    },
                )
            }
            composable(Routes.Offline) {
                OfflineLibraryScreen(
                    offlineRepository = container.offlineRepository,
                    historyRepository = container.historyRepository,
                    authRepository = container.authRepository,
                    onOpenChapter = { chapterId, titleId ->
                        navController.navigate(
                            Routes.reader(
                                chapterId,
                                offline = true,
                                titleId = titleId.ifBlank { null },
                            ),
                        )
                    },
                    onOpenTitle = { id, slug ->
                        navController.navigate(Routes.title(slug?.takeIf { it.isNotBlank() } ?: id))
                    },
                )
            }
            composable(Routes.Leaders) {
                LeadersScreen(
                    socialRepository = container.socialRepository,
                    onOpenUser = { id -> navController.navigate(Routes.user(id)) },
                )
            }
            composable(Routes.Notifications) {
                NotificationsScreen(
                    authRepository = container.authRepository,
                    socialRepository = container.socialRepository,
                    onBack = { navController.popBackStack() },
                    onLogin = { goLogin() },
                )
            }
            composable(Routes.Login) {
                LoginScreen(
                    authRepository = container.authRepository,
                    onSuccess = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.Title,
                arguments = listOf(navArgument("key") { type = NavType.StringType }),
            ) { entry ->
                val key = Routes.dec(entry.arguments?.getString("key").orEmpty())
                TitleScreen(
                    titleKey = key,
                    catalogRepository = container.catalogRepository,
                    offlineRepository = container.offlineRepository,
                    authRepository = container.authRepository,
                    socialRepository = container.socialRepository,
                    historyRepository = container.historyRepository,
                    downloadManager = container.downloadManager,
                    rewardedAdManager = container.rewardedAdManager,
                    adRewardStore = container.adRewardStore,
                    onBack = { navController.popBackStack() },
                    onLogin = { goLogin() },
                    onOpenChapter = { titleId, chapterId, offline ->
                        navController.navigate(
                            Routes.reader(chapterId, offline, titleId),
                        )
                    },
                    onOpenUser = { id -> navController.navigate(Routes.user(id)) },
                    onOpenPremium = { navController.navigate(Routes.Premium) },
                )
            }
            composable(
                route = Routes.Reader,
                arguments = listOf(
                    navArgument("chapterId") { type = NavType.StringType },
                    navArgument("offline") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("titleId") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val chapterId = Routes.dec(entry.arguments?.getString("chapterId").orEmpty())
                val offline = entry.arguments?.getBoolean("offline") == true
                val titleId = Routes.dec(entry.arguments?.getString("titleId").orEmpty())
                    .ifBlank { null }
                ReaderScreen(
                    chapterId = chapterId,
                    titleId = titleId,
                    preferOffline = offline,
                    catalogRepository = container.catalogRepository,
                    offlineRepository = container.offlineRepository,
                    historyRepository = container.historyRepository,
                    readingPrefs = container.readingPrefs,
                    authRepository = container.authRepository,
                    chapterTransitionAds = container.chapterTransitionAds,
                    onBack = { navController.popBackStack() },
                    onOpenChapter = { nextId ->
                        // Заменяем текущий reader в стеке, чтобы «Назад» = выход к тайтлу
                        navController.navigate(
                            Routes.reader(nextId, offline = false, titleId = titleId),
                        ) {
                            popUpTo(Routes.Reader) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenPremium = { navController.navigate(Routes.Premium) },
                    onLogin = { goLogin() },
                )
            }
            composable(
                route = Routes.ChatThread,
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                ),
            ) { entry ->
                val id = Routes.dec(entry.arguments?.getString("id").orEmpty())
                val title = Routes.dec(entry.arguments?.getString("title").orEmpty().ifBlank { "Чат" })
                ChatThreadScreen(
                    conversationId = id,
                    title = title,
                    authRepository = container.authRepository,
                    socialRepository = container.socialRepository,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.User,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = Routes.dec(entry.arguments?.getString("id").orEmpty())
                UserProfileScreen(
                    userId = id,
                    authRepository = container.authRepository,
                    socialRepository = container.socialRepository,
                    onBack = { navController.popBackStack() },
                    onLogin = { goLogin() },
                    onOpenChat = { convId, title ->
                        navController.navigate(Routes.chat(convId, title))
                    },
                )
            }
        }
    }
}
