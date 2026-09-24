package ru.tomilo.lib.mobile.ui.screens.games

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.api.GameCardDeckDto
import ru.tomilo.lib.mobile.data.api.GameCardCatalogItemDto
import ru.tomilo.lib.mobile.data.api.GameCardDto
import ru.tomilo.lib.mobile.data.api.GameCardTradeDto
import ru.tomilo.lib.mobile.data.api.GameCardTradeCatalogItemDto
import ru.tomilo.lib.mobile.data.api.GameCardTradeCreateRequest
import ru.tomilo.lib.mobile.data.repo.GamesRepository
import ru.tomilo.lib.mobile.ui.components.CardsGridSkeleton
import ru.tomilo.lib.mobile.ui.components.CardShopSkeleton
import ru.tomilo.lib.mobile.ui.components.CardTradesSkeleton
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.SkeletonBox
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.components.userFacingError
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

private enum class CardTab(val label: String) {
    Album("Альбом"), Shop("Рулетка"), Trade("Обмен"), Forge("Кузница"),
}

private enum class ForgeMode(val count: Int, val label: String) {
    Random(3, "Случайная · 3"), Choose(6, "Выбор · 6"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    gamesRepository: GamesRepository,
    onBack: () -> Unit,
    onOpenSubmit: () -> Unit,
    onOpenWebTab: (String) -> Unit,
) {
    var cards by remember { mutableStateOf<List<GameCardDto>>(emptyList()) }
    var shopRewardCards by remember { mutableStateOf<List<GameCardDto>>(emptyList()) }
    var decks by remember { mutableStateOf<List<GameCardDeckDto>>(emptyList()) }
    var trades by remember { mutableStateOf<List<GameCardTradeDto>>(emptyList()) }
    var tradeCatalog by remember { mutableStateOf<List<GameCardTradeCatalogItemDto>>(emptyList()) }
    var tradeCatalogLoading by remember { mutableStateOf(false) }
    var tradeCatalogError by remember { mutableStateOf<String?>(null) }
    var catalog by remember { mutableStateOf<List<GameCardCatalogItemDto>>(emptyList()) }
    var catalogLoading by remember { mutableStateOf(false) }
    var catalogError by remember { mutableStateOf<String?>(null) }
    val forgeSelection = remember { mutableStateListOf<String>() }
    var forgeTargetId by remember { mutableStateOf<String?>(null) }
    var forgedCard by remember { mutableStateOf<GameCardDto?>(null) }
    var tab by remember { mutableStateOf(CardTab.Album) }
    var forgeMode by remember { mutableStateOf(ForgeMode.Random) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var action by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var deckError by remember { mutableStateOf<String?>(null) }
    var tradeError by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    suspend fun refresh(showLoading: Boolean = false) {
        if (showLoading) loading = true else refreshing = true
        error = null
        val (result, decksResult, tradesResult) = supervisorScope {
            val cardsRequest = async { gamesRepository.cards() }
            val decksRequest = async { gamesRepository.cardDecks() }
            val tradesRequest = async { gamesRepository.cardTrades() }
            Triple(cardsRequest.await(), decksRequest.await(), tradesRequest.await())
        }
        result.onSuccess { cards = it.cards }
            .onFailure { error = it.message ?: "Не удалось загрузить коллекцию карточек" }
        decksResult.onSuccess { decks = it; deckError = null }
            .onFailure { deckError = it.message ?: "Не удалось загрузить магазин карточек" }
        tradesResult.onSuccess { trades = it.offers; tradeError = null }
            .onFailure { tradeError = it.message ?: "Не удалось загрузить обмены" }
        loading = false
        refreshing = false
    }

    fun loadTradeCatalog() {
        if (tradeCatalogLoading) return
        scope.launch {
            tradeCatalogLoading = true
            tradeCatalogError = null
            gamesRepository.cardTradeCatalog()
                .onSuccess { tradeCatalog = it.cards }
                .onFailure { tradeCatalogError = it.message ?: "Не удалось загрузить каталог для обмена" }
            tradeCatalogLoading = false
        }
    }

    LaunchedEffect(Unit) { refresh(showLoading = true) }
    LaunchedEffect(notice) { notice?.let { snackbar.showSnackbar(it); notice = null } }
    LaunchedEffect(tab == CardTab.Forge && forgeMode == ForgeMode.Choose) {
        if (tab == CardTab.Forge && forgeMode == ForgeMode.Choose) {
            catalogLoading = true
            catalogError = null
            gamesRepository.cardCatalog().onSuccess { catalog = it }
                .onFailure { catalogError = it.message ?: "Не удалось загрузить каталог карточек" }
            catalogLoading = false
        }
    }

    val selectedCards = forgeSelection.mapNotNull { id -> cards.firstOrNull { it.id == id } }
    val selectedRanks = selectedCards.map(::cardRank).distinct()
    val targetRank = if (selectedRanks.size == 1 && forgeSelection.size == forgeMode.count) nextForgeRank(selectedRanks.single()) else null
    val ownedIds = cards.mapNotNullTo(hashSetOf()) { it.id.takeIf(String::isNotBlank) }
    val materialIds = forgeSelection.toSet()
    val commonTitle = selectedCards.map { it.titleId.orEmpty() }.distinct().singleOrNull()?.takeIf { it.isNotBlank() }
    val targetCards = catalog.filter { item ->
        item.id.isNotBlank() && item.id !in ownedIds && item.id !in materialIds &&
            targetRank != null && cardRank(item) == targetRank &&
            (commonTitle == null || item.titleId.isNullOrBlank() || item.titleId == commonTitle)
    }

    fun <T> launchAction(
        key: String,
        success: String,
        operation: suspend () -> Result<T>,
        onSuccess: (T) -> String? = { null },
    ) {
        if (action != null) return
        scope.launch {
            action = key
            operation().onSuccess { result ->
                notice = onSuccess(result)?.takeIf(String::isNotBlank) ?: success
                if (key.startsWith("trade:")) trades = trades.filterNot { it.id == key.removePrefix("trade:") }
                refresh()
                if (key == "forge") forgeSelection.clear()
            }.onFailure { notice = it.message?.takeIf(String::isNotBlank) ?: "Не удалось выполнить действие. Попробуйте ещё раз." }
            action = null
        }
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Карточки", style = MaterialTheme.typography.titleLarge)
                        Text("Декоративные карточки · рулетка", style = MaterialTheme.typography.labelSmall, color = TomiloMuted)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
                },
                actions = {
                    IconButton(onClick = { scope.launch { refresh() } }, enabled = !refreshing && action == null) {
                        if (refreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Refresh, contentDescription = "Обновить карточки")
                    }
                    IconButton(onClick = onOpenSubmit) { Icon(Icons.Default.Add, contentDescription = "Предложить карточку", tint = TomiloPrimary) }
                },
                colors = tomiloTopBarColors(),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(CardTab.entries.size) { index ->
                    val item = CardTab.entries[index]
                    FilterChip(selected = tab == item, onClick = { tab = item }, label = { Text(item.label) })
                }
            }
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val duration = if (ValueAnimator.areAnimatorsEnabled()) 150 else 0
                    fadeIn(tween(durationMillis = duration)) togetherWith fadeOut(tween(durationMillis = duration))
                },
                label = "cardTabContent",
            ) { currentTab ->
                when {
                    loading && currentTab == CardTab.Album -> CardsGridSkeleton(Modifier.fillMaxSize())
                    currentTab == CardTab.Album && error != null && cards.isEmpty() -> ErrorBox(
                        message = error ?: "Не удалось загрузить коллекцию карточек.",
                        modifier = Modifier.fillMaxSize(),
                        onRetry = { scope.launch { refresh(showLoading = true) } },
                    )
                    currentTab == CardTab.Shop -> ShopTab(
                    decks = decks,
                    loading = loading && decks.isEmpty(),
                    revealedCards = shopRewardCards,
                    randomCardPrice = decks.firstOrNull { it.kind == "roulette" }?.price
                        ?: decks.firstOrNull { !it.isTitleDeck }?.price
                        ?: 250,
                    error = deckError,
                    onRetry = { scope.launch { refresh(showLoading = true) } },
                    action = action,
                    onPull = {
                        launchAction(
                            key = "pull",
                            success = "Случайная карточка получена. Альбом обновлён.",
                            operation = { gamesRepository.pullCard() },
                            onSuccess = { result ->
                                shopRewardCards = result.openedCards.mapNotNull { it.card }
                                rewardNotice(shopRewardCards)
                            },
                        )
                    },
                    onOpenDeck = { deck ->
                        launchAction(
                            key = "deck:${deck.stableId()}",
                            success = "Набор «${deck.name}» открыт. Альбом обновлён.",
                            operation = { gamesRepository.openCardDeck(deck.stableId()) },
                            onSuccess = { result ->
                                shopRewardCards = result.openedCards.mapNotNull { it.card }
                                rewardNotice(shopRewardCards)
                            },
                        )
                    },
                    )
                    currentTab == CardTab.Trade -> TradeTab(
                    trades = trades,
                    cards = cards,
                    loading = loading && trades.isEmpty(),
                    action = action,
                    error = tradeError,
                    tradeCatalog = tradeCatalog,
                    tradeCatalogLoading = tradeCatalogLoading,
                    tradeCatalogError = tradeCatalogError,
                    onPrepareCreate = ::loadTradeCatalog,
                    onRetryCatalog = ::loadTradeCatalog,
                    onCreate = { request, onComplete ->
                        if (action == null) scope.launch {
                            action = "trade:create"
                            gamesRepository.createCardTrade(request)
                                .onSuccess {
                                    notice = "Предложение обмена выставлено."
                                    onComplete()
                                    refresh()
                                }
                                .onFailure { notice = it.message?.takeIf(String::isNotBlank) ?: "Не удалось выставить обмен. Попробуйте ещё раз." }
                            action = null
                        }
                    },
                    onRetry = { scope.launch { refresh(showLoading = true) } },
                    onAccept = { trade ->
                        launchAction(
                            key = "trade:${trade.id}",
                            success = "Обмен завершён. Коллекция обновлена.",
                            operation = { gamesRepository.acceptCardTrade(trade.id) },
                        )
                    },
                    onCancel = { trade ->
                        launchAction(
                            key = "trade:${trade.id}",
                            success = "Предложение снято. Карты возвращены в коллекцию.",
                            operation = { gamesRepository.cancelCardTrade(trade.id) },
                        )
                    },
                    )
                    currentTab == CardTab.Forge -> ForgeTab(
                    cards = cards,
                    collectionLoading = loading && cards.isEmpty(),
                    collectionError = error,
                    onRetryCollection = { scope.launch { refresh(showLoading = true) } },
                    selectedIds = forgeSelection,
                    mode = forgeMode,
                    action = action,
                    targetRank = targetRank,
                    targetCards = targetCards,
                    catalogLoading = catalogLoading,
                    catalogError = catalogError,
                    onRetryCatalog = {
                        scope.launch {
                            catalogLoading = true
                            catalogError = null
                            gamesRepository.cardCatalog().onSuccess { catalog = it }
                                .onFailure { catalogError = it.message ?: "Не удалось загрузить каталог карточек" }
                            catalogLoading = false
                        }
                    },
                    selectedTargetId = forgeTargetId,
                    onSelectTarget = { forgeTargetId = it },
                    onMode = { mode ->
                        forgeMode = mode
                        while (forgeSelection.size > mode.count) forgeSelection.removeAt(forgeSelection.lastIndex)
                        if (mode == ForgeMode.Random) forgeTargetId = null
                    },
                    onToggle = { card ->
                        val id = card.id.trim()
                        if (id.isBlank()) {
                            notice = "Эту карточку нельзя использовать: сервер не вернул её ID. Обновите коллекцию."
                        } else {
                            val rank = cardRank(card)
                            val existing = forgeSelection.lastIndexOf(id)
                            val selectedCopies = forgeSelection.count { it == id }
                            val availableCopies = card.copies.coerceAtLeast(0)
                            if (existing >= 0 && (forgeSelection.size >= forgeMode.count || selectedCopies >= availableCopies)) {
                                forgeSelection.removeAt(existing)
                                forgeTargetId = null
                            }
                            else if (forgeSelection.size < forgeMode.count && selectedCopies < availableCopies && (forgeSelection.isEmpty() || forgeSelection.all { selected -> cards.firstOrNull { it.id == selected }?.let(::cardRank) == rank })) {
                                forgeSelection.add(id)
                                forgeTargetId = null
                            } else if (forgeSelection.isNotEmpty() && forgeSelection.any { selected -> cards.firstOrNull { it.id == selected }?.let(::cardRank) != rank }) {
                                notice = "Для перековки выберите карточки одного ранга."
                            } else if (forgeSelection.count { it == id } >= card.copies.coerceAtLeast(0)) {
                                notice = "В коллекции нет дополнительных копий этой карточки."
                            }
                        }
                    },
                    onForge = {
                        if (action == null) scope.launch {
                            action = "forge"
                            gamesRepository.craftCards(
                                cardIds = forgeSelection.toList(),
                                targetCardId = forgeTargetId.takeIf { forgeMode == ForgeMode.Choose },
                            ).onSuccess { result ->
                                forgedCard = result.granted?.card
                                val cardName = result.granted?.card?.let { card ->
                                    card.characterName?.takeIf(String::isNotBlank)
                                        ?: card.name.takeIf(String::isNotBlank)
                                }
                                notice = cardName?.let { "Получена карточка «$it»." }
                                    ?: "Карты перекованы. Коллекция обновлена."
                                forgeSelection.clear()
                                refresh()
                            }.onFailure {
                                notice = it.message?.takeIf(String::isNotBlank)
                                    ?: "Не удалось перековать карточки. Попробуйте ещё раз."
                            }
                            action = null
                        }
                    },
                    resultCard = forgedCard,
                    )
                    currentTab == CardTab.Album && cards.isEmpty() -> EmptyState(
                    title = "Альбом пока пуст",
                    message = "Ищите карточки в наградах, наборах и во время чтения.",
                    icon = Icons.Default.Collections,
                    actionLabel = "Предложить карточку",
                    onAction = onOpenSubmit,
                    )
                    currentTab == CardTab.Album -> AlbumTab(cards)
                }
            }
        }
    }
}

@Composable
private fun ShopTab(
    decks: List<GameCardDeckDto>,
    loading: Boolean,
    revealedCards: List<GameCardDto>,
    randomCardPrice: Int,
    action: String?,
    error: String?,
    onRetry: () -> Unit,
    onPull: () -> Unit,
    onOpenDeck: (GameCardDeckDto) -> Unit,
) {
    if (loading) {
        CardShopSkeleton(Modifier.fillMaxSize())
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(148.dp),
        contentPadding = PaddingValues(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (revealedCards.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            CardRewardReveal(revealedCards)
        }
        if (error != null) item(span = { GridItemSpan(maxLineSpan) }) {
            InlineLoadError(error, onRetry)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Рулетка карточек", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Случайная декоративная карточка или набор по тайтлу за монеты активности.", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Surface(color = TomiloSurface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, TomiloBorder)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Один запуск рулетки", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Случайная карточка · $randomCardPrice монет активности", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onPull, enabled = action == null, modifier = Modifier.fillMaxWidth()) {
                        if (action == "pull") {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text("Покупаем…")
                            }
                        } else Text("Крутить рулетку")
                    }
                }
            }
        }
        if (decks.isEmpty() && error == null) item(span = { GridItemSpan(maxLineSpan) }) {
            EmptyState("Наборов пока нет", "Загляните позже — новые наборы появятся в магазине.", icon = Icons.Default.Collections)
        }
        items(decks, key = { it.stableId() }) { deck ->
            val id = deck.stableId()
            val pending = action == "deck:$id"
            Surface(
                onClick = { if (deck.isAvailable && !pending) onOpenDeck(deck) },
                enabled = deck.isAvailable && action == null,
                color = TomiloSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, if (deck.isAvailable) TomiloPremium.copy(alpha = .6f) else TomiloBorder),
            ) {
                Column(Modifier.padding(10.dp)) {
                    AsyncImage(MediaUrl.resolve(deck.imageUrl), deck.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(154.dp).clip(RoundedCornerShape(12.dp)))
                    Text(deck.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 9.dp))
                    Text(if (deck.isAvailable) "${deck.price} монет · ${deck.cardsPerOpen} карт" else "Сейчас недоступен", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                    if (pending) {
                        Row(
                            Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Открываем…", color = TomiloMuted, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardRewardReveal(cards: List<GameCardDto>) {
    androidx.compose.animation.AnimatedVisibility(
        visible = cards.isNotEmpty(),
        enter = if (ValueAnimator.areAnimatorsEnabled()) {
            fadeIn(tween(220)) + scaleIn(initialScale = .9f, animationSpec = tween(320))
        } else fadeIn(tween(0)),
    ) {
        Surface(
            color = TomiloSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, TomiloPrimary.copy(alpha = .65f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (cards.size == 1) "Новая карточка" else "Карточки из набора · ${cards.size}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(cards, key = { it.id.ifBlank { it.name } }) { card ->
                        Column(Modifier.width(100.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            AsyncImage(
                                MediaUrl.resolve(card.stageImageUrl?.takeIf(String::isNotBlank) ?: card.imageUrl),
                                card.characterName ?: card.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(132.dp).clip(RoundedCornerShape(10.dp)).background(TomiloBg),
                            )
                            Text(card.characterName ?: card.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${cardRank(card)} ранг", color = rarityColor(card), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineLoadError(message: String, onRetry: () -> Unit) {
    Surface(
        color = TomiloSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .4f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(userFacingError(message), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onRetry) { Text("Повторить") }
        }
    }
}

private fun rewardNotice(cards: List<GameCardDto>): String? = when (cards.size) {
    0 -> null
    1 -> "Получена карточка «${cards.first().characterName?.takeIf(String::isNotBlank) ?: cards.first().name}»."
    else -> "Получено карточек: ${cards.size}. Альбом обновлён."
}

@Composable
private fun TradeTab(
    trades: List<GameCardTradeDto>,
    cards: List<GameCardDto>,
    loading: Boolean,
    action: String?,
    error: String?,
    tradeCatalog: List<GameCardTradeCatalogItemDto>,
    tradeCatalogLoading: Boolean,
    tradeCatalogError: String?,
    onPrepareCreate: () -> Unit,
    onRetryCatalog: () -> Unit,
    onCreate: (GameCardTradeCreateRequest, () -> Unit) -> Unit,
    onRetry: () -> Unit,
    onAccept: (GameCardTradeDto) -> Unit,
    onCancel: (GameCardTradeDto) -> Unit,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    if (loading) {
        CardTradesSkeleton(Modifier.fillMaxSize())
        return
    }
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Обмен карт", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = {
                        showCreateDialog = true
                        if (tradeCatalog.isEmpty() && !tradeCatalogLoading) onPrepareCreate()
                    },
                    enabled = action == null,
                ) { Text("Предложить обмен") }
            }
        }
        if (error != null) item { InlineLoadError(error, onRetry) }
        if (trades.isEmpty()) item {
            if (error == null) {
                EmptyState(
                    title = "Нет открытых обменов",
                    message = "Выставите предложение или загляните позже — здесь появятся карты других игроков.",
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    icon = Icons.Default.Collections,
                )
            }
        }
        items(trades.size, key = { trades[it].id }) { index ->
            val trade = trades[index]
            Surface(color = TomiloSurface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, TomiloBorder)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (trade.mine) "Ваше предложение" else trade.fromUsername, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TradeCardFace(trade.offerCard.name, trade.offerCard.imageUrl, "Отдаёт · ${trade.offerCopies} шт.", Modifier.weight(1f))
                        Text("⇄", color = TomiloPrimary, style = MaterialTheme.typography.titleLarge)
                        TradeCardFace(trade.wantCard.name, trade.wantCard.imageUrl, "Хочет · ${trade.wantCopies} шт.", Modifier.weight(1f))
                    }
                    trade.note?.takeIf { it.isNotBlank() }?.let { Text(it, color = TomiloMuted, style = MaterialTheme.typography.bodySmall) }
                    if (trade.mine) OutlinedButton(
                        onClick = { onCancel(trade) },
                        enabled = action == null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (action == "trade:${trade.id}") {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Снимаем предложение…")
                        } else Text("Снять предложение")
                    } else Button(
                        onClick = { onAccept(trade) },
                        enabled = action == null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (action == "trade:${trade.id}") {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Принимаем…")
                        } else Text("Обменять")
                    }
                }
            }
        }
    }
    if (showCreateDialog) {
        CardTradeCreateDialog(
            ownedCards = cards,
            catalog = tradeCatalog,
            catalogLoading = tradeCatalogLoading,
            catalogError = tradeCatalogError,
            action = action,
            onRetryCatalog = onRetryCatalog,
            onDismiss = { showCreateDialog = false },
            onCreate = { request -> onCreate(request) { showCreateDialog = false } },
        )
    }
}

private data class CardTradeChoice(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val subtitle: String,
)

@Composable
private fun CardTradeCreateDialog(
    ownedCards: List<GameCardDto>,
    catalog: List<GameCardTradeCatalogItemDto>,
    catalogLoading: Boolean,
    catalogError: String?,
    action: String?,
    onRetryCatalog: () -> Unit,
    onDismiss: () -> Unit,
    onCreate: (GameCardTradeCreateRequest) -> Unit,
) {
    val catalogIds = remember(catalog) { catalog.mapTo(hashSetOf()) { it.id } }
    val offerChoices = remember(ownedCards, catalogIds) {
        ownedCards.filter { it.id.isNotBlank() && it.copies > 0 && it.id in catalogIds }.map {
            CardTradeChoice(
                id = it.id,
                name = it.characterName?.takeIf(String::isNotBlank) ?: it.name,
                imageUrl = it.stageImageUrl?.takeIf(String::isNotBlank) ?: it.imageUrl,
                subtitle = "${it.copies} коп. · ${it.titleName ?: "Без тайтла"}",
            )
        }
    }
    val wantChoices = remember(catalog) {
        catalog.filter { it.id.isNotBlank() }.map {
            CardTradeChoice(it.id, it.name, it.imageUrl, it.titleName.ifBlank { "Без тайтла" })
        }
    }
    var offerId by remember { mutableStateOf("") }
    var wantId by remember { mutableStateOf("") }
    var offerCopies by remember { mutableStateOf(1) }
    var note by remember { mutableStateOf("") }
    LaunchedEffect(offerChoices) {
        if (offerChoices.none { it.id == offerId }) offerId = offerChoices.firstOrNull()?.id.orEmpty()
    }
    LaunchedEffect(wantChoices, offerId) {
        if (wantChoices.none { it.id == wantId } || wantId == offerId) {
            wantId = wantChoices.firstOrNull { it.id != offerId }?.id.orEmpty()
        }
    }
    val selectedOffer = offerChoices.firstOrNull { it.id == offerId }
    val canCreate = selectedOffer != null && wantId.isNotBlank() && offerId != wantId && action != "trade:create"
    AlertDialog(
        onDismissRequest = { if (action != "trade:create") onDismiss() },
        title = { Text("Предложить обмен") },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when {
                    catalogLoading -> Column(
                        Modifier.fillMaxWidth().height(230.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        repeat(2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                repeat(2) { SkeletonBox(Modifier.weight(1f).height(102.dp), radius = 12.dp) }
                            }
                        }
                    }
                    catalogError != null -> ErrorBox(
                        message = catalogError,
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                        onRetry = onRetryCatalog,
                    )
                    offerChoices.isEmpty() -> Text("Нет доступных карт с копиями для обмена.", color = TomiloMuted)
                    wantChoices.size < 2 -> Text("Для обмена пока недостаточно доступных карт.", color = TomiloMuted)
                    else -> {
                        CardTradeChoicePicker("Вы отдаёте", offerChoices, offerId, onSelect = {
                            offerId = it
                            offerCopies = offerCopies.coerceAtMost(offerChoices.firstOrNull { card -> card.id == it }?.let { card ->
                                ownedCards.firstOrNull { owned -> owned.id == card.id }?.copies
                            } ?: 1).coerceAtLeast(1)
                        })
                        selectedOffer?.let { offer ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Количество копий", style = MaterialTheme.typography.labelLarge)
                                IconButton(
                                    onClick = { offerCopies = (offerCopies - 1).coerceAtLeast(1) },
                                    enabled = offerCopies > 1,
                                ) { Icon(Icons.Default.Remove, contentDescription = "Уменьшить количество") }
                                Text("$offerCopies / ${ownedCards.firstOrNull { it.id == offer.id }?.copies ?: 1}")
                                IconButton(
                                    onClick = {
                                        offerCopies = (offerCopies + 1).coerceAtMost(
                                            ownedCards.firstOrNull { it.id == offer.id }?.copies ?: offerCopies,
                                        )
                                    },
                                    enabled = offerCopies < (ownedCards.firstOrNull { it.id == offer.id }?.copies ?: 1),
                                ) { Icon(Icons.Default.Add, contentDescription = "Увеличить количество") }
                            }
                        }
                        CardTradeChoicePicker("Вы хотите получить", wantChoices.filterNot { it.id == offerId }, wantId, onSelect = { wantId = it })
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it.take(80) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Комментарий (необязательно)") },
                            supportingText = { Text("${note.length}/80") },
                            maxLines = 2,
                        )
                        Text("В ответ вы получите 1 копию выбранной карты.", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        GameCardTradeCreateRequest(
                            offerCardId = offerId,
                            wantCardId = wantId,
                            offerCopies = offerCopies,
                            note = note.trim().takeIf(String::isNotBlank),
                        ),
                    )
                },
                enabled = canCreate,
            ) {
                if (action == "trade:create") CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Выставить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = action != "trade:create") { Text("Отмена") } },
    )
}

@Composable
private fun CardTradeChoicePicker(
    title: String,
    choices: List<CardTradeChoice>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(choices, key = { it.id }) { choice ->
                val selected = choice.id == selectedId
                Surface(
                    onClick = { onSelect(choice.id) },
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .28f) else TomiloSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (selected) TomiloPrimary else TomiloBorder),
                ) {
                    Column(Modifier.width(104.dp).padding(7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SubcomposeAsyncImage(
                            model = MediaUrl.resolve(choice.imageUrl),
                            contentDescription = choice.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(108.dp).clip(RoundedCornerShape(9.dp)).background(TomiloBg),
                            loading = { SkeletonBox(Modifier.fillMaxSize(), radius = 9.dp) },
                            error = { Box(Modifier.fillMaxSize().background(TomiloSurface2)) },
                        )
                        Text(choice.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                        Text(choice.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun TradeCardFace(name: String, image: String?, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(MediaUrl.resolve(image), name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(128.dp).clip(RoundedCornerShape(12.dp)).background(TomiloBg))
        Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 5.dp))
        Text(label, color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ForgeTab(
    cards: List<GameCardDto>, collectionLoading: Boolean, collectionError: String?,
    onRetryCollection: () -> Unit, selectedIds: List<String>, mode: ForgeMode, action: String?,
    targetRank: String?, targetCards: List<GameCardCatalogItemDto>, catalogLoading: Boolean,
    catalogError: String?, onRetryCatalog: () -> Unit,
    selectedTargetId: String?, onSelectTarget: (String) -> Unit,
    onMode: (ForgeMode) -> Unit, onToggle: (GameCardDto) -> Unit, onForge: () -> Unit,
    resultCard: GameCardDto?,
) {
    val selectedRank = selectedIds.firstOrNull()?.let { id -> cards.firstOrNull { it.id == id }?.let(::cardRank) }
    val validCount = selectedIds.size == mode.count
    val sameRank = selectedIds.all { id -> cards.firstOrNull { it.id == id }?.let(::cardRank) == selectedRank }
    if (collectionLoading) {
        CardsGridSkeleton(Modifier.fillMaxSize())
        return
    }
    if (collectionError != null && cards.isEmpty()) {
        ErrorBox(message = collectionError, onRetry = onRetryCollection)
        return
    }
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Горн", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("3 карточки одного ранга дадут случайную карту следующего ранга. Для выбора результата нужны 6 карточек.", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ForgeMode.entries.forEach { option -> FilterChip(selected = mode == option, onClick = { onMode(option) }, label = { Text(option.label) }) }
                }
                Text("Выбрано: ${selectedIds.size}/${mode.count}" + (selectedRank?.let { " · ранг $it" } ?: ""), color = if (sameRank) TomiloMuted else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                val targetReady = mode == ForgeMode.Random || selectedTargetId != null
                Button(onClick = onForge, enabled = validCount && sameRank && targetReady && action == null, modifier = Modifier.fillMaxWidth()) {
                    if (action == "forge") CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(if (mode == ForgeMode.Random) "Перековать · случайный результат" else "Перековать · выбрать результат")
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = resultCard != null,
                    enter = if (ValueAnimator.areAnimatorsEnabled()) {
                        fadeIn(tween(220)) + scaleIn(initialScale = .88f, animationSpec = tween(320))
                    } else fadeIn(tween(0)),
                    exit = fadeOut(tween(if (ValueAnimator.areAnimatorsEnabled()) 120 else 0)),
                ) {
                    resultCard?.let { card ->
                        Surface(
                            color = TomiloSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, rarityColor(card)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                AsyncImage(
                                    MediaUrl.resolve(card.stageImageUrl ?: card.imageUrl),
                                    card.characterName ?: card.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(56.dp, 74.dp).clip(RoundedCornerShape(10.dp)),
                                )
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("Карта получена", color = TomiloPrimary, style = MaterialTheme.typography.labelMedium)
                                    Text(card.characterName ?: card.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("${cardRank(card)} ранг · ${card.titleName ?: "Без тайтла"}", color = TomiloMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (mode == ForgeMode.Choose && targetRank != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Выберите карточку ранга $targetRank", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    when {
                        catalogLoading -> SkeletonBox(Modifier.fillMaxWidth().height(116.dp), radius = 14.dp)
                        catalogError != null -> ErrorBox(message = catalogError, onRetry = onRetryCatalog)
                        targetCards.isEmpty() -> Text("Нет доступных карточек следующего ранга.", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                        else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(targetCards, key = { it.id }) { target ->
                                val selected = selectedTargetId == target.id
                                Surface(
                                    onClick = { onSelectTarget(target.id) },
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .3f) else TomiloSurface,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, if (selected) TomiloPrimary else TomiloBorder),
                                ) {
                                    Column(Modifier.width(112.dp).padding(7.dp)) {
                                        AsyncImage(MediaUrl.resolve(target.imageUrl), target.characterName ?: target.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(128.dp).clip(RoundedCornerShape(9.dp)))
                                        Text(target.characterName ?: target.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 5.dp))
                                        Text(target.titleName ?: "Без тайтла", maxLines = 1, overflow = TextOverflow.Ellipsis, color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (cards.isEmpty()) item { Text("В альбоме пока нет карточек для перековки.", color = TomiloMuted, style = MaterialTheme.typography.bodyMedium) }
        items(cards.size, key = { cards[it].id.ifBlank { cards[it].name } }) { index ->
            val card = cards[index]
            val id = card.id
            val selectedCopies = selectedIds.count { it == id }
            val isRankValid = selectedRank == null || cardRank(card) == selectedRank
            val selectedColor by animateColorAsState(
                targetValue = if (selectedCopies > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .28f) else TomiloSurface,
                label = "forgeCardSelection",
            )
            Surface(
                onClick = { onToggle(card) }, enabled = id.isNotBlank() && (isRankValid || selectedCopies > 0) && action == null,
                color = selectedColor,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (selectedCopies > 0) TomiloPrimary.copy(alpha = .7f) else TomiloBorder),
            ) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(MediaUrl.resolve(card.stageImageUrl ?: card.imageUrl), card.characterName ?: card.name, contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp, 68.dp).clip(RoundedCornerShape(9.dp)))
                    Checkbox(checked = selectedCopies > 0, onCheckedChange = null)
                    Column(Modifier.weight(1f)) {
                        Text(card.characterName ?: card.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Ранг ${cardRank(card)} · копий ${card.copies.coerceAtLeast(0)}", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(if (selectedCopies > 0) "×$selectedCopies" else "＋", color = TomiloPrimary, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun AlbumTab(cards: List<GameCardDto>) {
    val copies = cards.sumOf { it.copies.coerceAtLeast(0) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(108.dp), state = rememberLazyGridState(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Коллекция карточек · $copies копий · ${cards.size} уникальных", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
        }
        items(cards, key = { it.id.ifBlank { it.name } }) { card -> CardAlbumItem(card) }
    }
}

@Composable
private fun CardAlbumItem(card: GameCardDto) {
    val imageUrl = MediaUrl.resolve(card.stageImageUrl?.takeIf(String::isNotBlank) ?: card.imageUrl)
    Surface(color = TomiloSurface, shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, rarityColor(card).copy(alpha = .65f))) {
        Column {
            Box(Modifier.fillMaxWidth().height(154.dp).clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))) {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = card.characterName?.takeIf(String::isNotBlank) ?: card.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().background(TomiloBg),
                    loading = { SkeletonBox(Modifier.fillMaxSize(), radius = 0.dp) },
                    error = { Box(Modifier.fillMaxSize().background(TomiloSurface2)) },
                )
                Text(cardRank(card), color = rarityColor(card), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, modifier = Modifier.padding(6.dp).clip(RoundedCornerShape(6.dp)).background(TomiloBg.copy(alpha = .88f)).padding(horizontal = 6.dp, vertical = 3.dp))
                if (card.copies > 1) Text("×${card.copies}", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(TomiloBg.copy(alpha = .88f)).padding(horizontal = 6.dp, vertical = 3.dp))
            }
            Text(card.characterName ?: card.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 7.dp))
            Text(card.titleName ?: "Без тайтла", color = TomiloMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 3.dp))
        }
    }
}
private fun cardRank(card: GameCardDto): String = rankFrom(card.forgeRank ?: card.currentStage, card.rarity)

private fun cardRank(card: GameCardCatalogItemDto): String = rankFrom(card.rank, card.rarity)

private fun rankFrom(stageValue: String?, rarityValue: String): String = when (stageValue?.trim()?.uppercase()) {
    "SSS", "SS", "R" -> "SSS"
    "S", "A", "B" -> stageValue?.trim()?.uppercase().orEmpty()
    "C", "D" -> "C"
    "F", "E" -> "F"
    else -> when (rarityValue.lowercase()) {
        "legendary" -> "SSS"
        "epic" -> "S"
        "rare" -> "C"
        else -> "F"
    }
}

private fun nextForgeRank(rank: String): String? = when (rank) {
    "F" -> "C"
    "C" -> "B"
    "B" -> "A"
    "A" -> "S"
    "S" -> "SSS"
    else -> null
}

private fun rarityColor(card: GameCardDto) = when (cardRank(card)) {
    "SSS" -> TomiloPremium
    "S", "A", "B" -> TomiloPrimary
    else -> TomiloBorder
}
