package ru.tomilo.lib.mobile.ui.screens.games

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.data.api.GameDisciplesDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.GamesRepository
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloText

private enum class GamesPage { HUB, CARDS, CULTIVATION, ARENA, INVENTORY, EXPEDITION }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(
    authRepository: AuthRepository,
    gamesRepository: GamesRepository,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenMatch: () -> Unit,
    onOpenTraining: () -> Unit,
    onOpenQuests: () -> Unit,
    onOpenWheel: () -> Unit,
    onOpenWebTab: (String) -> Unit,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    var page by rememberSaveable { mutableStateOf(GamesPage.HUB) }

    BackHandler(enabled = page != GamesPage.HUB) { page = GamesPage.HUB }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            when (page) {
                                GamesPage.HUB -> "Игры и награды"
                                GamesPage.CARDS -> "Карточки"
                                GamesPage.CULTIVATION -> "Путь культивации"
                                GamesPage.ARENA -> "Арена секты"
                                GamesPage.INVENTORY -> "Хранилище"
                                GamesPage.EXPEDITION -> "Вылазка секты"
                            },
                        )
                        Text(
                            when (page) {
                                GamesPage.HUB -> "Колесо и коллекция"
                                GamesPage.CARDS -> "Декоративная коллекция"
                                GamesPage.CULTIVATION -> "Медитация, путь Дао и прорыв"
                                GamesPage.ARENA -> "Состав отряда и серверные бои"
                                GamesPage.INVENTORY -> "Предметы и серверные действия"
                                GamesPage.EXPEDITION -> "Тайные миры, риск и серверные награды"
                            },
                            color = TomiloMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (page == GamesPage.HUB) onBack() else page = GamesPage.HUB }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        when {
            user == null && page != GamesPage.CARDS -> GamesGuest(
                modifier = Modifier.padding(padding),
                onLogin = onLogin,
                onExploreCards = { page = GamesPage.CARDS },
                onExploreMatch = onOpenMatch,
                onExploreTraining = onOpenTraining,
            )
            page == GamesPage.HUB -> GamesContent(
                balance = user?.balance ?: 0,
                isAdmin = user?.isAdmin() == true,
                onOpenQuests = onOpenQuests,
                onOpenWheel = onOpenWheel,
                onOpenCards = { page = GamesPage.CARDS },
                onOpenMatch = onOpenMatch,
                onOpenTraining = onOpenTraining,
                onOpenCultivation = { page = GamesPage.CULTIVATION },
                onOpenArena = { page = GamesPage.ARENA },
                onOpenInventory = { page = GamesPage.INVENTORY },
                onOpenExpedition = { page = GamesPage.EXPEDITION },
                modifier = Modifier.padding(padding),
            )
            page == GamesPage.CARDS -> key(user?.stableId()) {
                CardsScreen(
                    gamesRepository = gamesRepository,
                    authRepository = authRepository,
                    isAuthenticated = user != null,
                    onBack = { page = GamesPage.HUB },
                    onLogin = onLogin,
                    onOpenSubmit = { onOpenWebTab("cards/submit") },
                    onOpenWebTab = onOpenWebTab,
                )
            }
            page == GamesPage.CULTIVATION -> key(user?.stableId()) {
                CultivationProgressionScreen(
                    gamesRepository = gamesRepository,
                    modifier = Modifier.padding(padding),
                )
            }
            page == GamesPage.INVENTORY -> key(user?.stableId()) {
                InventoryNativeScreen(
                    gamesRepository = gamesRepository,
                    authRepository = authRepository,
                    modifier = Modifier.padding(padding),
                )
            }
            page == GamesPage.EXPEDITION -> key(user?.stableId()) {
                ExpeditionNativeScreen(
                    gamesRepository = gamesRepository,
                    authRepository = authRepository,
                    modifier = Modifier.padding(padding),
                )
            }
            page == GamesPage.ARENA -> key(user?.stableId()) {
                NativeDisciplesPage(
                    page = page,
                    gamesRepository = gamesRepository,
                    modifier = Modifier.padding(padding),
                    onOpenCultivation = { page = GamesPage.CULTIVATION },
                    onOpenArena = { page = GamesPage.ARENA },
                )
            }
        }
    }
}

@Composable
private fun GamesGuest(
    modifier: Modifier,
    onLogin: () -> Unit,
    onExploreCards: () -> Unit,
    onExploreMatch: () -> Unit,
    onExploreTraining: () -> Unit,
) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(TomiloPrimary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Casino, contentDescription = null, tint = TomiloPrimary, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Игры и награды", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Альбом и магазин декоративных карточек доступны без входа. Чтобы собирать карты, открывать рулетку и получать награды, войдите в аккаунт.",
            color = TomiloMuted,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(22.dp))
        Button(onClick = onExploreCards, modifier = Modifier.fillMaxWidth()) { Text("Открыть каталог карт") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onExploreMatch, modifier = Modifier.fillMaxWidth()) { Text("Играть в сад созвездий") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onExploreTraining, modifier = Modifier.fillMaxWidth()) { Text("Тропа духозверей · тренировка") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Войти в аккаунт") }
    }
}

@Composable
private fun GamesContent(
    balance: Int,
    isAdmin: Boolean,
    onOpenQuests: () -> Unit,
    onOpenWheel: () -> Unit,
    onOpenCards: () -> Unit,
    onOpenMatch: () -> Unit,
    onOpenTraining: () -> Unit,
    onOpenCultivation: () -> Unit,
    onOpenArena: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenExpedition: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Surface(
                color = TomiloSurface,
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TomiloPrimary.copy(alpha = 0.24f)),
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFF32201F), Color(0xFF211B1C), TomiloSurface)))
                        .padding(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Casino, contentDescription = null, tint = TomiloPrimary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Колесо наград", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Попробуйте удачу за монеты активности", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = TomiloPremium, modifier = Modifier.size(20.dp))
                            Text("$balance", fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("монет", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = onOpenWheel) { Text("Открыть") }
                    }
                }
            }
        }
        item {
            GameModeRow(
                icon = Icons.Default.AutoAwesome,
                title = "Сад созвездий",
                subtitle = if (isAdmin) "Три в ряд с серверным прогрессом и наградами" else "Три в ряд · локальная тренировка без наград",
                onClick = onOpenMatch,
            )
        }
        item {
            GameModeRow(
                icon = Icons.Default.Collections,
                title = "Хранилище",
                subtitle = "Инвентарь и применение разрешённых предметов",
                onClick = onOpenInventory,
            )
        }
        item {
            GameModeRow(
                icon = Icons.Default.Explore,
                title = "Вылазка секты",
                subtitle = "Отправляйте учеников за опытом и добычей",
                onClick = onOpenExpedition,
            )
        }
        item {
            GameModeRow(
                icon = Icons.Default.AutoAwesome,
                title = "Тропа духозверей",
                subtitle = "Боевые комбинации и испытание памяти · работает без сети",
                onClick = onOpenTraining,
            )
        }
        item {
            GameModeRow(
                icon = Icons.Default.Groups,
                title = "Путь культивации",
                subtitle = "Медитация, путь Дао, прорывы стадий и развитие учеников",
                onClick = onOpenCultivation,
            )
        }
        item {
            GameModeRow(
                icon = Icons.Default.MilitaryTech,
                title = "Арена секты",
                subtitle = "Подберите состав и проведите бой с серверной наградой",
                onClick = onOpenArena,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Коллекция и задания", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                GameModeRow(
                    icon = Icons.Default.Collections,
                    title = "Альбом карт",
                    subtitle = "Коллекция, магазин, обмен и улучшение карт",
                    onClick = onOpenCards,
                )
                GameModeRow(
                    icon = Icons.Default.TaskAlt,
                    title = "Поручения",
                    subtitle = "Задания, опыт и монеты активности",
                    onClick = onOpenQuests,
                )
            }
        }
    }
}

@Composable
private fun NativeDisciplesPage(
    page: GamesPage,
    gamesRepository: GamesRepository,
    modifier: Modifier = Modifier,
    onOpenCultivation: () -> Unit,
    onOpenArena: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var disciples by remember { mutableStateOf<GameDisciplesDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val refresh: () -> Unit = {
        scope.launch {
            loading = true
            gamesRepository.disciples()
                .onSuccess {
                    disciples = it
                    error = null
                }
                .onFailure {
                    error = it.message?.takeIf(String::isNotBlank)
                        ?: "Не удалось загрузить данные секты"
                }
            loading = false
        }
    }
    LaunchedEffect(page) { refresh() }

    val state = disciples
    Box(modifier.fillMaxSize()) {
        when {
            state != null && page == GamesPage.CULTIVATION -> SectContent(
                disciples = state,
                gamesRepository = gamesRepository,
                onOpenArena = onOpenArena,
                onChanged = refresh,
            )
            state != null && page == GamesPage.ARENA -> ArenaContent(
                disciples = state,
                gamesRepository = gamesRepository,
                onOpenSect = onOpenCultivation,
                onChanged = refresh,
            )
            else -> Column(
                Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator()
                    Text("Загружаем состояние секты…", color = TomiloMuted)
                } else {
                    Text(error ?: "Данные секты пока недоступны", color = TomiloMuted)
                    Button(onClick = refresh) { Text("Повторить") }
                }
            }
        }
    }
}

@Composable
private fun GameModeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = TomiloSurface,
        shape = RoundedCornerShape(17.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TomiloBorder),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(TomiloPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = TomiloPrimary, modifier = Modifier.size(22.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TomiloMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text("›", color = TomiloMuted, fontSize = 24.sp)
        }
    }
}
