package ru.tomilo.lib.mobile.ui.screens.games

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.api.GameCardDto
import ru.tomilo.lib.mobile.data.repo.GamesRepository
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    gamesRepository: GamesRepository,
    onBack: () -> Unit,
    onOpenSubmit: () -> Unit,
    onOpenWebTab: (String) -> Unit,
) {
    var cards by remember { mutableStateOf<List<GameCardDto>>(emptyList()) }
    var decks by remember { mutableStateOf(emptyList<ru.tomilo.lib.mobile.data.api.GameCardDeckDto>()) }
    var tab by remember { mutableStateOf("album") }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        gamesRepository.dashboard()
            .onSuccess { cards = it.cards.cards; error = null }
            .onFailure { error = it.message ?: "Не удалось загрузить карточки" }
        gamesRepository.cardDecks().onSuccess { decks = it }
        loading = false
    }
    Column(Modifier.fillMaxSize().background(TomiloBg)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
            Surface(color = TomiloSurface, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, TomiloBorder)) {
                Text("Карты", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenSubmit) { Icon(Icons.Default.Add, "Предложить карточку", tint = TomiloPrimary) }
        }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            CardTab("Альбом", tab == "album") { tab = "album" }
            CardTab("Магазин", tab == "shop") { tab = "shop" }
            CardTab("Обмен", tab == "trade") { tab = "trade" }
            CardTab("Кузница", tab == "forge") { tab = "forge" }
        }
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Загружаем альбом…", color = TomiloMuted) }
        } else if (error != null) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text(error.orEmpty(), color = TomiloMuted, textAlign = TextAlign.Center) }
        } else if (tab == "shop") {
            LazyVerticalGrid(GridCells.Fixed(2), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Column { Text("Наборы карт", fontWeight = FontWeight.Bold); Text("Открывайте наборы за монеты активности", color = TomiloMuted, fontSize = 12.sp); actionMessage?.let { Text(it, color = TomiloPrimary, fontSize = 12.sp) } }
                }
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Button(onClick = { scope.launch { gamesRepository.pullCard().onSuccess { actionMessage = "Карта получена — обновите альбом" }.onFailure { actionMessage = it.message } } }, modifier = Modifier.fillMaxWidth()) { Text("Получить случайную карту") }
                }
                items(decks, key = { it.stableId() }) { deck ->
                    Surface(onClick = { scope.launch { gamesRepository.openCardDeck(deck.stableId()).onSuccess { actionMessage = "Набор открыт — обновите альбом" }.onFailure { actionMessage = it.message } } }, color = TomiloSurface, shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, TomiloPremium.copy(alpha = .7f))) {
                        Column(Modifier.padding(12.dp)) { AsyncImage(model = MediaUrl.resolve(deck.imageUrl), contentDescription = deck.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(10.dp))); Text(deck.name, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(top = 8.dp)); Text("${deck.price} монет · ${deck.cardsPerOpen} карт", color = TomiloMuted, fontSize = 11.sp) }
                    }
                }
            }
        } else if (tab == "trade" || tab == "forge") {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Style, null, tint = TomiloPremium, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(12.dp))
                Text(if (tab == "trade") "Обмен карточками" else "Кузница", fontWeight = FontWeight.Bold)
                Text(if (tab == "trade") "Выберите дубликаты в альбоме — создание и принятие сделок появятся после синхронизации торгового API." else "Выберите дубликаты в альбоме для перековки. Рецепты и выбор результата станут доступны вместе с API кузницы.", color = TomiloMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            }
        } else if (cards.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Collections, null, tint = TomiloPremium, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("Альбом пока пуст", fontWeight = FontWeight.Bold)
                Text("Открывайте паки, читайте главы или предложите свою карточку", color = TomiloMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                Button(onClick = onOpenSubmit, modifier = Modifier.padding(top = 18.dp)) { Icon(Icons.Default.Add, null); Text("Предложить карточку", modifier = Modifier.padding(start = 6.dp)) }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = rememberLazyGridState(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                    Text("В коллекции ${cards.sumOf { it.copies.coerceAtLeast(1) }} · уникальных ${cards.size}", color = TomiloMuted, fontSize = 12.sp)
                }
                items(cards, key = { it.id.ifBlank { it.name } }) { CardAlbumItem(it) }
            }
        }
    }
}

@Composable private fun CardTab(title: String, selected: Boolean, onClick: () -> Unit) = Text(title, color = if (selected) TomiloPrimary else TomiloMuted, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.clickable(onClick = onClick).padding(vertical = 10.dp), fontSize = 13.sp)

@Composable private fun CardAlbumItem(card: GameCardDto) {
    val rank = card.currentStage?.uppercase().orEmpty().ifBlank { card.rarity.uppercase().take(3) }
    Surface(color = TomiloSurface, shape = RoundedCornerShape(13.dp), border = androidx.compose.foundation.BorderStroke(1.dp, TomiloPremium.copy(alpha = 0.75f))) {
        Column {
            Box(Modifier.fillMaxWidth().height(148.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))) {
                AsyncImage(model = MediaUrl.resolve(card.stageImageUrl ?: card.imageUrl), contentDescription = card.characterName ?: card.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Text(rank, color = TomiloPremium, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(6.dp).clip(RoundedCornerShape(8.dp)).background(TomiloBg.copy(alpha = .8f)).padding(horizontal = 5.dp, vertical = 2.dp))
                if (card.copies > 1) Text("×${card.copies}", color = androidx.compose.ui.graphics.Color.White, fontSize = 10.sp, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
            Text(card.characterName ?: card.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 7.dp, end = 7.dp, top = 7.dp))
            Text(card.titleName ?: "Без тайтла", color = TomiloMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(7.dp))
        }
    }
}
