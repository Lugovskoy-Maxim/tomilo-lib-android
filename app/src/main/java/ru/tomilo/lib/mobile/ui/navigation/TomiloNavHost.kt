package ru.tomilo.lib.mobile.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import ru.tomilo.lib.mobile.ui.components.RewardNotificationHost
import ru.tomilo.lib.mobile.ui.components.TomiloBottomBar
import ru.tomilo.lib.mobile.ui.components.TomiloTabItem
import ru.tomilo.lib.mobile.ui.screens.admin.AdminScreen
import ru.tomilo.lib.mobile.ui.screens.auth.LoginScreen
import ru.tomilo.lib.mobile.ui.screens.bookmarks.BookmarksScreen
import ru.tomilo.lib.mobile.ui.screens.catalog.CatalogScreen
import ru.tomilo.lib.mobile.ui.screens.chats.ChatThreadScreen
import ru.tomilo.lib.mobile.ui.screens.chats.ChatsScreen
import ru.tomilo.lib.mobile.ui.screens.history.HistoryScreen
import ru.tomilo.lib.mobile.ui.screens.hub.TomiloHubScreen
import ru.tomilo.lib.mobile.ui.screens.friends.FriendsScreen
import ru.tomilo.lib.mobile.ui.screens.home.HomeScreen
import ru.tomilo.lib.mobile.ui.screens.games.GamesScreen
import ru.tomilo.lib.mobile.ui.screens.leaders.LeadersScreen
import ru.tomilo.lib.mobile.ui.screens.notifications.NotificationsScreen
import ru.tomilo.lib.mobile.ui.screens.offline.OfflineLibraryScreen
import ru.tomilo.lib.mobile.ui.screens.premium.PremiumScreen
import ru.tomilo.lib.mobile.ui.screens.profile.ProfileScreen
import ru.tomilo.lib.mobile.ui.screens.library.LibraryScreen
import ru.tomilo.lib.mobile.ui.screens.reader.ReaderScreen
import ru.tomilo.lib.mobile.ui.screens.quests.QuestsScreen
import ru.tomilo.lib.mobile.ui.screens.search.SearchScreen
import ru.tomilo.lib.mobile.ui.screens.shop.ShopScreen
import ru.tomilo.lib.mobile.ui.screens.title.TitleScreen
import ru.tomilo.lib.mobile.ui.screens.user.UserProfileScreen
import ru.tomilo.lib.mobile.ui.screens.updates.UpdatesScreen
import ru.tomilo.lib.mobile.ui.screens.wheel.WheelScreen
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import androidx.compose.foundation.background
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val Home = "home"
    const val Catalog = "catalog"
    const val Search = "search"
    const val Bookmarks = "bookmarks"
    const val Library = "library"
    const val Chats = "chats"
    const val Profile = "profile"
    const val Offline = "offline"
    const val Leaders = "leaders"
    const val Notifications = "notifications"
    const val History = "history"
    const val Admin = "admin"
    const val Login = "login"
    const val Premium = "premium"
    const val Updates = "updates"
    const val Friends = "friends"
    const val Quests = "quests"
    const val Hub = "hub"
    const val Wheel = "wheel"
    const val Shop = "shop"
    const val Games = "games"
    const val Title = "title/{key}"
    const val Reader = "reader/{chapterId}?offline={offline}&titleId={titleId}"
    /** title в path — надёжнее, чем query, для кириллицы */
    const val ChatThread = "chat/{id}/{title}"
    const val User = "user/{id}"

    fun catalog(genre: String? = null) =
        if (genre.isNullOrBlank()) "catalog" else "catalog?genre=${enc(genre)}"
    fun title(key: String) = "title/${enc(key)}"
    fun reader(chapterId: String, offline: Boolean = false, titleId: String? = null) =
        "reader/${enc(chapterId)}?offline=$offline&titleId=${enc(titleId.orEmpty())}"
    fun chat(id: String, title: String) =
        "chat/${enc(id)}/${enc(title.ifBlank { "Чат" })}"
    fun user(id: String) = "user/${enc(id)}"

    fun enc(v: String) = URLEncoder.encode(v, StandardCharsets.UTF_8.toString()).replace("+", "%20")
    fun dec(v: String) = URLDecoder.decode(v, StandardCharsets.UTF_8.toString())
}

@Composable
fun TomiloNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRaw = backStack?.destination?.route.orEmpty()
    val current = if (currentRaw.startsWith(Routes.Catalog)) Routes.Catalog else currentRaw
    val contentSettings by container.contentPrefs.settingsFlow.collectAsState(initial = ContentSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val tabs = listOf(
        TomiloTabItem(
            route = Routes.Home,
            label = "Лента",
            icon = Icons.Outlined.AutoStories,
            selectedIcon = Icons.Filled.AutoStories,
        ),
        TomiloTabItem(
            route = Routes.Catalog,
            label = "Каталог",
            icon = Icons.Outlined.Explore,
            selectedIcon = Icons.Filled.Explore,
        ),
        TomiloTabItem(
            route = Routes.Library,
            label = "Полка",
            icon = Icons.AutoMirrored.Outlined.MenuBook,
            selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        ),
        TomiloTabItem(
            route = Routes.Chats,
            label = "Чаты",
            icon = Icons.AutoMirrored.Outlined.Chat,
            selectedIcon = Icons.AutoMirrored.Filled.Chat,
        ),
        TomiloTabItem(
            route = Routes.Profile,
            label = "Я",
            icon = Icons.Outlined.Person,
            selectedIcon = Icons.Filled.Person,
        ),
    )
    val tabRoutes = tabs.map { it.route }.toSet()
    val showBottomBar = current in tabRoutes

    fun goLogin() = navController.navigate(Routes.Login)

    /** Строит предсказуемую иерархию Home → Title независимо от точки входа в читалку. */
    fun navigateTitleRoot(id: String) {
        if (id.isBlank()) return
        navController.navigate(Routes.title(id)) {
            popUpTo(Routes.Home) { inclusive = false }
            launchSingleTop = true
        }
    }

    fun openDeepLink(rawLink: String) {
        val path = rawLink
            .substringAfter("tomilo-lib.ru", rawLink)
            .substringBefore('?')
            .trim('/')
        val parts = path.split('/').filter { it.isNotBlank() }
        when {
            parts.firstOrNull() == "titles" && parts.size >= 4 && parts[2] == "chapter" ->
                navController.navigate(Routes.reader(parts[3], titleId = parts.getOrNull(1)))
            parts.firstOrNull() == "titles" && parts.size >= 2 ->
                navController.navigate(Routes.title(parts[1]))
            parts.firstOrNull() == "browse" && parts.size >= 4 && parts[2] == "chapter" ->
                navController.navigate(Routes.reader(parts[3], titleId = parts.getOrNull(1)))
            parts.firstOrNull() == "browse" && parts.size >= 2 ->
                navController.navigate(Routes.title(parts[1]))
            parts.firstOrNull() in setOf("profile", "users", "user") && parts.size >= 2 ->
                navController.navigate(Routes.user(parts[1]))
            path == "friends" -> navController.navigate(Routes.Friends)
            path in setOf("updates", "latest-updates") -> navController.navigate(Routes.Updates)
            path in setOf("leaderboard", "leaders") -> navController.navigate(Routes.Leaders)
            path in setOf("daily-quests", "quests") -> navController.navigate(Routes.Quests)
            path == "history" -> navController.navigate(Routes.History)
            path == "offline" -> navController.navigate(Routes.Offline)
            path == "premium" -> navController.navigate(Routes.Premium)
            path == "tomilo-shop" -> navController.navigate(Routes.Shop)
            path == "games" -> navController.navigate(Routes.Games)
            path == "notifications" -> navController.navigate(Routes.Notifications)
            else -> runCatching {
                val url = if (rawLink.startsWith("http")) rawLink
                else "${ru.tomilo.lib.mobile.BuildConfig.SITE_URL}/${path}"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    }

    val pendingNotification by container.pendingNotificationOpen.collectAsState()
    LaunchedEffect(pendingNotification) {
        val nav = pendingNotification ?: return@LaunchedEffect
        container.pendingNotificationOpen.value = null
        when {
            !nav.chapterId.isNullOrBlank() -> navController.navigate(
                Routes.reader(nav.chapterId, titleId = nav.titleId),
            )
            !nav.titleId.isNullOrBlank() -> navController.navigate(Routes.title(nav.titleId))
            !nav.linkUrl.isNullOrBlank() -> openDeepLink(nav.linkUrl)
            nav.openList -> navController.navigate(Routes.Notifications)
        }
    }

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateHomeRoot() {
        // На странице тайтла «Домой» означает именно корень приложения, а не
        // восстановление сохранённого вложенного стека текущей вкладки.
        if (!navController.popBackStack(Routes.Home, inclusive = false)) {
            navController.navigate(Routes.Home) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    if (!contentSettings.ageGateAnswered) {
        AgeGateDialog(
            onAdult = { scope.launch { container.contentPrefs.answerAgeGate(isAdult = true) } },
            onMinor = { scope.launch { container.contentPrefs.answerAgeGate(isAdult = false) } },
        )
    }

    // Floating bottom bar overlays content (like site/IG) — lists use ScreenPadding
    // so last items can scroll clear of the pill. No Scaffold bottom slot (it clips logo).
    Box(
        Modifier
            .fillMaxSize()
            .background(TomiloBg),
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.Home,
            modifier = Modifier
                .fillMaxSize()
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
                    authRepository = container.authRepository,
                    historyRepository = container.historyRepository,
                    socialRepository = container.socialRepository,
                    onOpenTitle = { id, slug ->
                        navController.navigate(Routes.title(slug?.takeIf { it.isNotBlank() } ?: id))
                    },
                    onOpenCatalog = { navigateTab(Routes.catalog()) },
                    onOpenGenre = { genre ->
                        navController.navigate(Routes.catalog(genre)) {
                            launchSingleTop = true
                        }
                    },
                    onOpenSearch = { navController.navigate(Routes.Search) },
                    onOpenUpdates = { navController.navigate(Routes.Updates) },
                    onOpenHistory = { navController.navigate(Routes.History) },
                    onOpenQuests = { navController.navigate(Routes.Quests) },
                    onOpenFriends = { navController.navigate(Routes.Friends) },
                    onOpenOffline = { navController.navigate(Routes.Offline) },
                    onOpenGames = { navController.navigate(Routes.Games) },
                    onContinueReading = { titleId, chapterId ->
                        navController.navigate(Routes.reader(chapterId, offline = false, titleId = titleId))
                    },
                )
            }
            composable(Routes.Search) {
                SearchScreen(
                    catalogRepository = container.catalogRepository,
                    searchHistoryPrefs = container.searchHistoryPrefs,
                    onBack = { navController.popBackStack() },
                    onOpenTitle = { id, slug ->
                        navController.navigate(Routes.title(slug?.takeIf { it.isNotBlank() } ?: id))
                    },
                )
            }
            composable(
                "catalog?genre={genre}",
                arguments = listOf(
                    navArgument("genre") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    },
                ),
            ) { entry ->
                val genre = entry.arguments?.getString("genre")
                    ?.let { raw -> raw.takeIf { it.isNotBlank() }?.let(Routes::dec) }
                    ?.takeIf { it.isNotBlank() }
                androidx.compose.runtime.key(genre.orEmpty()) {
                CatalogScreen(
                    catalogRepository = container.catalogRepository,
                    contentPrefs = container.contentPrefs,
                    initialGenre = genre,
                    onOpenTitle = { id, slug ->
                        navController.navigate(Routes.title(slug?.takeIf { it.isNotBlank() } ?: id))
                    },
                )
                }
            }
            composable(Routes.Library) {
                LibraryScreen(
                    authRepository = container.authRepository,
                    socialRepository = container.socialRepository,
                    historyRepository = container.historyRepository,
                    offlineRepository = container.offlineRepository,
                    onLogin = { goLogin() },
                    onOpenTitle = { id, slug ->
                        navController.navigate(Routes.title(slug?.takeIf { it.isNotBlank() } ?: id))
                    },
                    onContinue = { titleId, chapterId, offline ->
                        navController.navigate(Routes.reader(chapterId, offline = offline, titleId = titleId))
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
                    onOpenFriends = { navController.navigate(Routes.Friends) },
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
                    onOpenFriends = { navController.navigate(Routes.Friends) },
                    onOpenQuests = { navController.navigate(Routes.Quests) },
                    onOpenUpdates = { navController.navigate(Routes.Updates) },
                    onOpenHub = { navController.navigate(Routes.Hub) },
                    onOpenWheel = { navController.navigate(Routes.Wheel) },
                    onOpenShop = { navController.navigate(Routes.Shop) },
                    onOpenGames = { navController.navigate(Routes.Games) },
                    onOpenMyPublicProfile = { id -> navController.navigate(Routes.user(id)) },
                )
            }
            composable(Routes.Updates) {
                UpdatesScreen(
                    catalogRepository = container.catalogRepository,
                    contentPrefs = container.contentPrefs,
                    onBack = { navController.popBackStack() },
                    onOpenTitle = { id, slug ->
                        navController.navigate(Routes.title(slug?.takeIf { it.isNotBlank() } ?: id))
                    },
                )
            }
            composable(Routes.Friends) {
                FriendsScreen(
                    socialRepository = container.socialRepository,
                    onBack = { navController.popBackStack() },
                    onOpenUser = { id -> navController.navigate(Routes.user(id)) },
                    onOpenChat = { id, title -> navController.navigate(Routes.chat(id, title)) },
                )
            }
            composable(Routes.Quests) {
                QuestsScreen(
                    authRepository = container.authRepository,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.Wheel) {
                WheelScreen(
                    authRepository = container.authRepository,
                    onBack = { navController.popBackStack() },
                    onLogin = { goLogin() },
                )
            }
            composable(Routes.Hub) {
                TomiloHubScreen(
                    onBack = { navController.popBackStack() },
                    onOpenShop = { navController.navigate(Routes.Shop) },
                    onOpenGames = { navController.navigate(Routes.Games) },
                )
            }
            composable(Routes.Games) {
                GamesScreen(
                    authRepository = container.authRepository,
                    gamesRepository = container.gamesRepository,
                    onBack = { navController.popBackStack() },
                    onLogin = { goLogin() },
                    onOpenQuests = { navController.navigate(Routes.Quests) },
                    onOpenWheel = { navController.navigate(Routes.Wheel) },
                    onOpenWebTab = { tab ->
                        val url = "${ru.tomilo.lib.mobile.BuildConfig.SITE_URL}/games?tab=${Routes.enc(tab)}"
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    },
                )
            }
            composable(Routes.Shop) {
                ShopScreen(
                    authRepository = container.authRepository,
                    socialRepository = container.socialRepository,
                    onBack = { navController.popBackStack() },
                    onLogin = { goLogin() },
                )
            }
            composable(Routes.Premium) {
                PremiumScreen(
                    authRepository = container.authRepository,
                    paymentsRepository = container.paymentsRepository,
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
                    onOpenUser = { id -> navController.navigate(Routes.user(id)) },
                )
            }
            composable(Routes.Offline) {
                OfflineLibraryScreen(
                    offlineRepository = container.offlineRepository,
                    historyRepository = container.historyRepository,
                    authRepository = container.authRepository,
                    readingPrefs = container.readingPrefs,
                    downloadManager = container.downloadManager,
                    onBack = { navController.popBackStack() },
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
                    onBack = { navController.popBackStack() },
                    onOpenUser = { id -> navController.navigate(Routes.user(id)) },
                )
            }
            composable(Routes.Notifications) {
                NotificationsScreen(
                    authRepository = container.authRepository,
                    socialRepository = container.socialRepository,
                    onBack = { navController.popBackStack() },
                    onLogin = { goLogin() },
                    onOpenTitle = { id -> navController.navigate(Routes.title(id)) },
                    onOpenChapter = { titleId, chapterId ->
                        navController.navigate(Routes.reader(chapterId, titleId = titleId))
                    },
                    onOpenLink = { rawLink -> openDeepLink(rawLink) },
                )
            }
            composable(Routes.Login) {
                LoginScreen(
                    authRepository = container.authRepository,
                    onBack = { navController.popBackStack() },
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
                    onOpenHome = { navigateHomeRoot() },
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
                    socialRepository = container.socialRepository,
                    readingPrefs = container.readingPrefs,
                    authRepository = container.authRepository,
                    chapterTransitionAds = container.chapterTransitionAds,
                    onBack = { navController.popBackStack() },
                    onOpenTitle = { id -> navigateTitleRoot(id) },
                    onOpenUser = { id -> navController.navigate(Routes.user(id)) },
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
                    onOpenFriends = { navController.navigate(Routes.Friends) },
                    onOpenChat = { convId, title ->
                        navController.navigate(Routes.chat(convId, title))
                    },
                )
            }
        }

        if (showBottomBar) {
            TomiloBottomBar(
                tabs = tabs,
                currentRoute = current,
                onTabClick = ::navigateTab,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        RewardNotificationHost(Modifier.align(Alignment.TopCenter))
    }
}
