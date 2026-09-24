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
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

private enum class GamesPage { HUB, CARDS }

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
    var page by remember { mutableStateOf(GamesPage.HUB) }

    BackHandler(enabled = page != GamesPage.HUB) { page = GamesPage.HUB }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (page == GamesPage.HUB) "Игры и награды" else "Карточки")
                        Text(
                            if (page == GamesPage.HUB) "Колесо и коллекция" else "Декоративная коллекция",
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
            user == null -> GamesGuest(Modifier.padding(padding), onLogin)
            page == GamesPage.HUB -> GamesContent(
                balance = user?.balance ?: 0,
                onOpenQuests = onOpenQuests,
                onOpenWheel = onOpenWheel,
                onOpenCards = { page = GamesPage.CARDS },
                modifier = Modifier.padding(padding),
            )
            else -> CardsScreen(
                gamesRepository = gamesRepository,
                onBack = { page = GamesPage.HUB },
                onOpenSubmit = { onOpenWebTab("cards/submit") },
                onOpenWebTab = onOpenWebTab,
            )
        }
    }
}

@Composable
private fun GamesGuest(modifier: Modifier, onLogin: () -> Unit) {
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
            "Войдите, чтобы собирать декоративные карточки, открывать рулетку и получать награды за чтение.",
            color = TomiloMuted,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(22.dp))
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Войти") }
    }
}

@Composable
private fun GamesContent(
    balance: Int,
    onOpenQuests: () -> Unit,
    onOpenWheel: () -> Unit,
    onOpenCards: () -> Unit,
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Коллекция и задания", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                GameModeRow(
                    icon = Icons.Default.Collections,
                    title = "Карточки тайтлов",
                    subtitle = "Декоративные карточки, наборы и обмен",
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
