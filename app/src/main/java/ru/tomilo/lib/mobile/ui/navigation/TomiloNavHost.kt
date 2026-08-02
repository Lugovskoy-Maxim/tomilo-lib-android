package ru.tomilo.lib.mobile.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.tomilo.lib.mobile.AppContainer
import ru.tomilo.lib.mobile.ui.screens.auth.LoginScreen
import ru.tomilo.lib.mobile.ui.screens.home.HomeScreen
import ru.tomilo.lib.mobile.ui.screens.offline.OfflineLibraryScreen
import ru.tomilo.lib.mobile.ui.screens.profile.ProfileScreen
import ru.tomilo.lib.mobile.ui.screens.reader.ReaderScreen
import ru.tomilo.lib.mobile.ui.screens.search.SearchScreen
import ru.tomilo.lib.mobile.ui.screens.title.TitleScreen
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val Home = "home"
    const val Search = "search"
    const val Offline = "offline"
    const val Profile = "profile"
    const val Login = "login"
    const val Title = "title/{key}"
    const val Reader = "reader/{chapterId}?offline={offline}"

    fun title(key: String) = "title/${enc(key)}"
    fun reader(chapterId: String, offline: Boolean = false) =
        "reader/${enc(chapterId)}?offline=$offline"

    private fun enc(v: String) = URLEncoder.encode(v, StandardCharsets.UTF_8.toString())
    fun dec(v: String) = URLDecoder.decode(v, StandardCharsets.UTF_8.toString())
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun TomiloNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route.orEmpty()

    val tabs = listOf(
        Tab(Routes.Home, "Главная", Icons.Default.Home),
        Tab(Routes.Search, "Поиск", Icons.Default.Search),
        Tab(Routes.Offline, "Офлайн", Icons.Default.OfflinePin),
        Tab(Routes.Profile, "Профиль", Icons.Default.Person),
    )
    val showBottomBar = tabs.any { current == it.route || current.startsWith(it.route) }

    Scaffold(
        containerColor = TomiloBg,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = TomiloBg) {
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
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.Home) {
                HomeScreen(
                    catalogRepository = container.catalogRepository,
                    onOpenTitle = { id, slug ->
                        navController.navigate(Routes.title(slug?.takeIf { it.isNotBlank() } ?: id))
                    },
                )
            }
            composable(Routes.Search) {
                SearchScreen(
                    catalogRepository = container.catalogRepository,
                    onOpenTitle = { id, slug ->
                        navController.navigate(Routes.title(slug?.takeIf { it.isNotBlank() } ?: id))
                    },
                )
            }
            composable(Routes.Offline) {
                OfflineLibraryScreen(
                    offlineRepository = container.offlineRepository,
                    onOpenChapter = { chapterId ->
                        navController.navigate(Routes.reader(chapterId, offline = true))
                    },
                )
            }
            composable(Routes.Profile) {
                ProfileScreen(
                    authRepository = container.authRepository,
                    onLogin = { navController.navigate(Routes.Login) },
                    onOpenOffline = {
                        navController.navigate(Routes.Offline) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.Login) {
                LoginScreen(
                    authRepository = container.authRepository,
                    onSuccess = {
                        navController.popBackStack()
                    },
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
                    onBack = { navController.popBackStack() },
                    onOpenChapter = { _, chapterId, offline ->
                        navController.navigate(Routes.reader(chapterId, offline))
                    },
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
                ),
            ) { entry ->
                val chapterId = Routes.dec(entry.arguments?.getString("chapterId").orEmpty())
                val offline = entry.arguments?.getBoolean("offline") == true
                ReaderScreen(
                    chapterId = chapterId,
                    preferOffline = offline,
                    catalogRepository = container.catalogRepository,
                    offlineRepository = container.offlineRepository,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
