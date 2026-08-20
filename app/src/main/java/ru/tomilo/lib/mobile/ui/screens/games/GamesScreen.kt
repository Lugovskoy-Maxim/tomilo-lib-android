package ru.tomilo.lib.mobile.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.api.GameCardDto
import ru.tomilo.lib.mobile.data.api.GameDiscipleDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.GamesDashboard
import ru.tomilo.lib.mobile.data.repo.GamesRepository
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

private val GamesPurple = Color(0xFF9B8CFF)
private val GamesCyan = Color(0xFF55C7D9)
private val GamesGreen = Color(0xFF65B985)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(
    authRepository: AuthRepository,
    gamesRepository: GamesRepository,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenQuests: () -> Unit,
    onOpenWheel: () -> Unit,
    onOpenWebTab: (String) -> Unit,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    var dashboard by remember { mutableStateOf<GamesDashboard?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(user?.stableId(), reload) {
        if (user == null) {
            dashboard = null
            error = null
            return@LaunchedEffect
        }
        loading = true
        error = null
        gamesRepository.dashboard()
            .onSuccess { dashboard = it }
            .onFailure { error = it.message ?: "Игровой профиль пока недоступен" }
        loading = false
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Игры")
                        Text("Арена наставника · бета", color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (user != null) {
                        IconButton(onClick = { reload += 1 }, enabled = !loading) {
                            Icon(Icons.Default.Refresh, contentDescription = "Обновить игровой профиль")
                        }
                    }
                },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        when {
            user == null -> GamesGuest(
                modifier = Modifier.padding(padding),
                onLogin = onLogin,
            )
            loading && dashboard == null -> LoadingBox(
                modifier = Modifier.padding(padding),
                message = "Собираем данные секты…",
            )
            error != null && dashboard == null -> ErrorBox(
                message = error.orEmpty(),
                modifier = Modifier.padding(padding),
                onRetry = { reload += 1 },
            )
            else -> PullToRefreshBox(
                isRefreshing = loading,
                onRefresh = { reload += 1 },
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
                GamesContent(
                    dashboard = dashboard ?: GamesDashboard(),
                    profileBalance = user?.balance ?: 0,
                    onOpenQuests = onOpenQuests,
                    onOpenWheel = onOpenWheel,
                    onOpenWebTab = onOpenWebTab,
                )
            }
        }
    }
}

@Composable
private fun GamesGuest(modifier: Modifier, onLogin: () -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(listOf(GamesPurple, TomiloPrimary))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.SportsEsports, null, tint = Color.White, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Арена наставника", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Войдите, чтобы развивать секту, собирать карты духа, выполнять поручения и получать награды.",
            color = TomiloMuted,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(22.dp))
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Войти и начать") }
    }
}

@Composable
private fun GamesContent(
    dashboard: GamesDashboard,
    profileBalance: Int,
    onOpenQuests: () -> Unit,
    onOpenWheel: () -> Unit,
    onOpenWebTab: (String) -> Unit,
) {
    val totalItems = dashboard.inventory.sumOf { it.count }
    val disciples = dashboard.disciples
    val cards = dashboard.cards
    val alchemy = dashboard.alchemy
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 14.dp,
            top = 12.dp,
            end = 14.dp,
            bottom = 110.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            GamesHero(
                balance = disciples.balance.takeIf { it > 0 } ?: profileBalance,
                combatRating = disciples.combatRating,
                sectLevel = disciples.sectLevel,
                itemCount = totalItems,
            )
        }
        item {
            Text("Цикл наставника", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Зарабатывайте → собирайте ресурсы → усиливайте доступные разделы секты.",
                color = TomiloMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
            Surface(
                color = GamesPurple.copy(alpha = 0.09f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GamesPurple.copy(alpha = 0.22f)),
            ) {
                Text(
                    "Показаны только механики, уже включённые в публичной бете сайта. Остальные режимы появятся здесь после их запуска.",
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            GameModeCard(
                icon = Icons.Default.TaskAlt,
                title = "Поручения",
                subtitle = "Ежедневные задания, опыт и монеты",
                badge = "Нативно",
                accent = GamesGreen,
                onClick = onOpenQuests,
            )
        }
        item {
            GameModeCard(
                icon = Icons.Default.Inventory2,
                title = "Хранилище",
                subtitle = if (totalItems > 0) "$totalItems предметов · ${dashboard.inventory.size} видов" else "Собирайте материалы и расходники",
                badge = if (totalItems > 0) "$totalItems" else null,
                accent = GamesCyan,
                external = true,
                onClick = { onOpenWebTab("inventory") },
            )
        }
        item {
            GameModeCard(
                icon = Icons.Default.Groups,
                title = "Секта",
                subtitle = "${disciples.sectLevelLabel ?: "Уровень ${disciples.sectLevel}"} · сила ${disciples.combatRating}",
                badge = "${disciples.disciples.size}/${disciples.maxDisciples.coerceAtLeast(disciples.disciples.size)}",
                accent = GamesPurple,
                external = true,
                onClick = { onOpenWebTab("disciples") },
            )
        }
        item {
            GameModeCard(
                icon = Icons.Default.Style,
                title = "Карты духа",
                subtitle = "Коллекция персонажей и усиление учеников",
                badge = cards.stats.total.takeIf { it > 0 }?.toString(),
                accent = TomiloPremium,
                external = true,
                onClick = { onOpenWebTab("cards") },
            )
        }
        item {
            GameModeCard(
                icon = Icons.Default.Science,
                title = "Алхимия",
                subtitle = "Котёл ${alchemy.cauldronTier} ур. · алхимик ${alchemy.alchemyLevel} ур.",
                badge = "${alchemy.attemptsLeft}/${alchemy.craftsPerDay}",
                accent = Color(0xFFCC78E8),
                external = true,
                onClick = { onOpenWebTab("alchemy") },
            )
        }
        item {
            GameModeCard(
                icon = Icons.Default.Casino,
                title = "Судьба",
                subtitle = "Колесо наград, монеты и редкие предметы",
                badge = "Нативно",
                accent = TomiloPrimary,
                onClick = onOpenWheel,
            )
        }
        if (dashboard.inventory.isNotEmpty()) {
            item { GamesSectionTitle("В хранилище", "Полный инвентарь", { onOpenWebTab("inventory") }) }
            item { InventoryPreview(dashboard) }
        }
        if (disciples.disciples.isNotEmpty()) {
            item { GamesSectionTitle("Ученики секты", "Управлять", { onOpenWebTab("disciples") }) }
            items(disciples.disciples.take(3), key = { it.characterId.ifBlank { it.displayName() } }) {
                DiscipleRow(it)
            }
        }
        val previewCards = cards.showcase.ifEmpty { cards.cards }.take(6)
        if (previewCards.isNotEmpty()) {
            item { GamesSectionTitle("Карты духа", "Коллекция", { onOpenWebTab("cards") }) }
            item { CardsPreview(previewCards) }
        }
        if (dashboard.warnings.isNotEmpty()) {
            item {
                Surface(
                    color = TomiloSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TomiloBorder),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Часть данных обновится позже", fontWeight = FontWeight.SemiBold)
                        Text(dashboard.warnings.joinToString(" · "), color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun GamesHero(balance: Int, combatRating: Int, sectLevel: Int, itemCount: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF35276A), Color(0xFF171329), TomiloSurface)))
            .border(1.dp, GamesPurple.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(GamesPurple.copy(alpha = 0.24f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.SportsEsports, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Арена наставника", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(7.dp))
                    Text("БЕТА", color = GamesPurple, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Text("Пока вы читаете, секта растёт", color = Color.White.copy(alpha = 0.68f))
            }
            Icon(Icons.Default.AutoAwesome, null, tint = TomiloPremium)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            HeroStat(Icons.Default.MonetizationOn, "$balance", "монет", Modifier.weight(1f))
            HeroStat(Icons.Default.MilitaryTech, "$combatRating", "сила", Modifier.weight(1f))
            HeroStat(Icons.Default.Groups, "$sectLevel", "секта", Modifier.weight(1f))
            HeroStat(Icons.Default.Inventory2, "$itemCount", "вещи", Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroStat(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .padding(vertical = 9.dp, horizontal = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = GamesPurple, modifier = Modifier.size(16.dp))
        Text(value, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun GameModeCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String?,
    accent: Color,
    external: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = TomiloSurface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TomiloBorder.copy(alpha = 0.8f)),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = TomiloMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (!badge.isNullOrBlank()) {
                Text(
                    badge,
                    color = accent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(CircleShape).background(accent.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 5.dp),
                )
                Spacer(Modifier.width(7.dp))
            }
            Icon(if (external) Icons.AutoMirrored.Filled.OpenInNew else Icons.Default.AutoAwesome, null, tint = TomiloMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun GamesSectionTitle(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onAction) {
            Text(action)
            Spacer(Modifier.width(5.dp))
            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun InventoryPreview(dashboard: GamesDashboard) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        dashboard.inventory.take(8).forEach { item ->
            Surface(color = TomiloSurface2, shape = RoundedCornerShape(17.dp), modifier = Modifier.width(126.dp)) {
                Column(Modifier.padding(12.dp)) {
                    AsyncImage(
                        model = MediaUrl.resolve(item.icon),
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(TomiloBg),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(item.name ?: item.itemId.replace('_', ' '), maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    Text("×${item.count}", color = GamesCyan, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DiscipleRow(disciple: GameDiscipleDto) {
    Surface(color = TomiloSurface, shape = RoundedCornerShape(19.dp), border = androidx.compose.foundation.BorderStroke(1.dp, TomiloBorder)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = MediaUrl.resolve(disciple.avatar),
                contentDescription = disciple.displayName(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(GamesPurple.copy(alpha = 0.13f)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(disciple.displayName(), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(disciple.titleName, disciple.rank?.let { "ранг $it" }, disciple.level?.let { "$it ур." }).joinToString(" · "),
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${disciple.cp ?: (disciple.attack + disciple.defense + disciple.speed)}", color = GamesPurple, fontWeight = FontWeight.Bold)
                Text(if (disciple.inMeditation == true) "медитация" else if (disciple.inWarehouse == true) "склад" else "в строю", color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CardsPreview(cards: List<GameCardDto>) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        cards.forEach { card ->
            Surface(color = TomiloSurface2, shape = RoundedCornerShape(18.dp), modifier = Modifier.width(142.dp)) {
                Column {
                    AsyncImage(
                        model = MediaUrl.resolve(card.stageImageUrl ?: card.imageUrl),
                        contentDescription = card.characterName ?: card.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(158.dp).background(TomiloBg),
                    )
                    Column(Modifier.padding(10.dp)) {
                        Text(card.characterName?.takeIf { it.isNotBlank() } ?: card.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(
                            listOfNotNull(card.currentStage?.let { "Этап $it" }, card.titleName).joinToString(" · "),
                            color = TomiloMuted,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
