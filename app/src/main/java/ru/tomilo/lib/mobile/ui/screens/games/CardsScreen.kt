package ru.tomilo.lib.mobile.ui.screens.games

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.core.CardEconomy
import ru.tomilo.lib.mobile.core.CardForgeSelection
import ru.tomilo.lib.mobile.core.CardTradeChoiceLayout
import ru.tomilo.lib.mobile.core.toUserFacingError
import ru.tomilo.lib.mobile.data.api.GameCardDeckDto
import ru.tomilo.lib.mobile.data.api.GameCardCatalogItemDto
import ru.tomilo.lib.mobile.data.api.GameCardDto
import ru.tomilo.lib.mobile.data.api.GameCardTradeDto
import ru.tomilo.lib.mobile.data.api.GameCardTradeCatalogItemDto
import ru.tomilo.lib.mobile.data.api.GameCardTradeCreateRequest
import ru.tomilo.lib.mobile.data.local.CardWantPrefs
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.GamesRepository
import ru.tomilo.lib.mobile.ui.components.CardsGridSkeleton
import ru.tomilo.lib.mobile.ui.components.CardCollectionSkeleton
import ru.tomilo.lib.mobile.ui.components.CardShopSkeleton
import ru.tomilo.lib.mobile.ui.components.CardTradesSkeleton
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.SkeletonBox
import ru.tomilo.lib.mobile.ui.components.TomiloBottomBarContentGap
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.core.userFacingError
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

private enum class CardTab(val label: String, val pageTitle: String) {
    Album("Альбом", "Альбом карт"),
    Shop("Магазин", "Магазин карт"),
    Trade("Обмен", "Обмен карт"),
    Forge("Кузница", "Кузница"),
}

private enum class ForgeMode(val count: Int, val label: String) {
    Random(3, "Случайная · 3"), Choose(6, "Выбор · 6"),
}

private enum class CardCatalogSort(val label: String) {
    Popular("По популярности"), Name("По имени"), Rank("По рангу"),
}

private enum class CardCatalogRank(val rarity: String?, val label: String) {
    All(null, "Все"), Common("common", "F"), Rare("rare", "C"), Epic("epic", "B"), Legendary("legendary", "S"),
}

private data class CardTitleOffer(
    val key: String,
    val title: String,
    val imageUrl: String?,
    val poolSize: Int?,
    val roulette: GameCardDeckDto? = null,
    val pack: GameCardDeckDto? = null,
)

private fun groupCardTitleOffers(decks: List<GameCardDeckDto>): List<CardTitleOffer> {
    val grouped = linkedMapOf<String, CardTitleOffer>()
    decks.forEach { deck ->
        val id = deck.stableId()
        val titleKey = deck.titleId?.takeIf(String::isNotBlank) ?: "deck:$id"
        val title = deck.titleName.ifBlank { deck.name }
        val current = grouped[titleKey] ?: CardTitleOffer(
            key = titleKey,
            title = title,
            imageUrl = deck.imageUrl,
            poolSize = deck.poolSize,
        )
        val offer = current.copy(
            imageUrl = current.imageUrl?.takeIf(String::isNotBlank) ?: deck.imageUrl,
            poolSize = listOfNotNull(current.poolSize, deck.poolSize).maxOrNull(),
        )
        grouped[titleKey] = when (deck.kind?.lowercase() ?: if (deck.cardsPerOpen == 1) "roulette" else "pack") {
            "roulette" -> offer.copy(roulette = deck)
            else -> offer.copy(pack = deck)
        }
    }
    return grouped.values.toList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    gamesRepository: GamesRepository,
    authRepository: AuthRepository,
    isAuthenticated: Boolean,
    onBack: () -> Unit,
    onLogin: () -> Unit,
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
    val context = LocalContext.current
    val cardWantPrefs = remember(context) { CardWantPrefs(context) }
    var wantedCardIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var wantsLoaded by remember { mutableStateOf(false) }
    val forgeSelection = remember { mutableStateListOf<String>() }
    var forgeTargetId by remember { mutableStateOf<String?>(null) }
    var forgedCard by remember { mutableStateOf<GameCardDto?>(null) }
    var cardToSell by remember { mutableStateOf<GameCardDto?>(null) }
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
    val refreshMutex = remember { Mutex() }

    LaunchedEffect(Unit) {
        wantedCardIds = cardWantPrefs.cardIds()
        wantsLoaded = true
    }
    LaunchedEffect(wantedCardIds, wantsLoaded) {
        if (wantsLoaded) cardWantPrefs.saveCardIds(wantedCardIds)
    }

    suspend fun refresh(showLoading: Boolean = false) {
        refreshMutex.withLock {
            try {
                if (showLoading) loading = true else refreshing = true
                error = null
                val (cardsResult, decksResult, tradesResult) = supervisorScope {
                    val cardsRequest = async { if (isAuthenticated) gamesRepository.cards() else null }
                    val decksRequest = async { gamesRepository.cardDecks() }
                    val tradesRequest = async { if (isAuthenticated) gamesRepository.cardTrades() else null }
                    Triple(cardsRequest.await(), decksRequest.await(), tradesRequest.await())
                }
                cardsResult?.onSuccess { response ->
                    cards = response.cards
                    val availableCopies = response.cards.associate { it.id.trim() to it.copies.coerceAtLeast(0) }
                    val reconciledSelection = CardForgeSelection.reconcile(forgeSelection.toList(), availableCopies)
                    if (reconciledSelection != forgeSelection.toList()) {
                        forgeSelection.clear()
                        forgeSelection.addAll(reconciledSelection)
                        forgeTargetId = null
                    }
                }
                    ?.onFailure { error = it.toUserFacingError("Не удалось загрузить коллекцию карточек") }
                    ?: run { cards = emptyList(); error = null }
                decksResult.onSuccess { decks = it; deckError = null }
                    .onFailure { deckError = it.toUserFacingError("Не удалось загрузить магазин карточек") }
                tradesResult?.onSuccess { trades = it.offers; tradeError = null }
                    ?.onFailure { tradeError = it.toUserFacingError("Не удалось загрузить обмены") }
                    ?: run { trades = emptyList(); tradeError = null }
            } finally {
                loading = false
                refreshing = false
            }
        }
    }

    suspend fun loadCardCatalog(force: Boolean = false) {
        if (catalogLoading || (!force && catalog.isNotEmpty())) return
        catalogLoading = true
        catalogError = null
        try {
            gamesRepository.cardCatalog().onSuccess { catalog = it }
                .onFailure { catalogError = it.toUserFacingError("Не удалось загрузить каталог карточек") }
        } finally {
            catalogLoading = false
        }
    }

    fun loadTradeCatalog() {
        if (tradeCatalogLoading) return
        tradeCatalogLoading = true
        scope.launch {
            tradeCatalogError = null
            try {
                gamesRepository.cardTradeCatalog()
                    .onSuccess { tradeCatalog = it.cards }
                    .onFailure { tradeCatalogError = it.toUserFacingError("Не удалось загрузить каталог для обмена") }
            } finally {
                tradeCatalogLoading = false
            }
        }
    }

    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated && tab != CardTab.Album && tab != CardTab.Shop) tab = CardTab.Album
        refresh(showLoading = true)
    }
    LaunchedEffect(notice) { notice?.let { snackbar.showSnackbar(it); notice = null } }
    LaunchedEffect(tab, forgeMode) {
        if (tab == CardTab.Album || tab == CardTab.Forge && forgeMode == ForgeMode.Choose) {
            loadCardCatalog()
        }
    }

    val selectedCards = forgeSelection.mapNotNull { id -> cards.firstOrNull { it.id == id } }
    val selectedRanks = selectedCards.map(::cardRank).distinct()
    val targetRank = if (selectedRanks.size == 1 && forgeSelection.size == forgeMode.count) CardEconomy.nextForgeRank(selectedRanks.single()) else null
    val ownedIds = cards.mapNotNullTo(hashSetOf()) { it.id.takeIf(String::isNotBlank) }
    val materialIds = forgeSelection.toSet()
    val commonTitle = selectedCards.map { it.titleId.orEmpty() }.distinct().singleOrNull()?.takeIf { it.isNotBlank() }
    val targetCards = catalog.filter { item ->
        item.id.isNotBlank() && item.id !in ownedIds && item.id !in materialIds &&
            targetRank != null && cardRank(item) == targetRank &&
            (commonTitle == null || item.titleId.isNullOrBlank() || item.titleId == commonTitle)
    }
    val visibleTabs = if (isAuthenticated) CardTab.entries else listOf(CardTab.Album, CardTab.Shop)

    fun <T> launchAction(
        key: String,
        success: String,
        operation: suspend () -> Result<T>,
        onSuccess: (T) -> String? = { null },
    ) {
        if (action != null) return
        scope.launch {
            action = key
            try {
                val result: Result<T> = try {
                    operation()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (exception: Exception) {
                    Result.failure(exception)
                }
                result.onSuccess { value ->
                    notice = onSuccess(value)?.takeIf(String::isNotBlank) ?: success
                    if (key.startsWith("trade:")) trades = trades.filterNot { it.id == key.removePrefix("trade:") }
                    refresh()
                    if (key == "forge") forgeSelection.clear()
                }.onFailure { exception ->
                    notice = exception.message?.takeIf(String::isNotBlank)?.let(::userFacingError)
                        ?: "Не удалось выполнить действие. Попробуйте ещё раз."
                }
            } finally {
                action = null
            }
        }
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(tab.pageTitle, style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
                },
                actions = {
                    IconButton(onClick = { scope.launch { refresh() } }, enabled = !refreshing && action == null) {
                        if (refreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Refresh, contentDescription = "Обновить карточки")
                    }
                    IconButton(onClick = { if (isAuthenticated) onOpenSubmit() else onLogin() }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = if (isAuthenticated) "Предложить карточку" else "Войти, чтобы предложить обмен",
                            tint = TomiloPrimary,
                        )
                    }
                },
                colors = tomiloTopBarColors(),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { insets ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(bottom = TomiloBottomBarContentGap),
        ) {
            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visibleTabs.size) { index ->
                    val item = visibleTabs[index]
                    FilterChip(selected = tab == item, onClick = { tab = item }, label = { Text(item.label) })
                }
            }
            AnimatedContent(
                targetState = tab,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val duration = if (ValueAnimator.areAnimatorsEnabled()) 150 else 0
                    fadeIn(tween(durationMillis = duration)) togetherWith fadeOut(tween(durationMillis = duration))
                },
                label = "cardTabContent",
            ) { currentTab ->
                when {
                    currentTab == CardTab.Album && catalogLoading && catalog.isEmpty() && cards.isEmpty() -> CardsGridSkeleton(Modifier.fillMaxSize())
                    currentTab == CardTab.Album && loading && cards.isEmpty() && catalog.isNotEmpty() -> CardsGridSkeleton(Modifier.fillMaxSize())
                    currentTab == CardTab.Album && catalogError != null && catalog.isEmpty() && cards.isEmpty() -> ErrorBox(
                        message = catalogError ?: "Не удалось загрузить каталог карточек.",
                        modifier = Modifier.fillMaxSize(),
                        onRetry = { scope.launch { loadCardCatalog(force = true) } },
                    )
                    currentTab == CardTab.Shop -> ShopTab(
                    decks = decks,
                    loading = loading && decks.isEmpty(),
                    revealedCards = shopRewardCards,
                    isAuthenticated = isAuthenticated,
                    onLogin = onLogin,
                    randomCardPrice = decks.firstOrNull { it.kind == "roulette" }?.price
                        ?: decks.firstOrNull { !it.isTitleDeck }?.price
                        ?: 250,
                    error = deckError,
                    onRetry = { scope.launch { refresh(showLoading = true) } },
                    action = action,
                    onPull = {
                        if (!isAuthenticated) onLogin()
                        else {
                            shopRewardCards = emptyList()
                            launchAction(
                                key = "pull",
                                success = "Случайная карточка получена. Альбом обновлён.",
                                operation = {
                                    gamesRepository.pullCard().also { result ->
                                        if (result.isSuccess) authRepository.refreshProfile()
                                    }
                                },
                                onSuccess = { result ->
                                    shopRewardCards = result.openedCards.mapNotNull { it.card }
                                    rewardNotice(shopRewardCards)
                                },
                            )
                        }
                    },
                    onOpenDeck = { deck ->
                        if (!isAuthenticated) onLogin()
                        else {
                            shopRewardCards = emptyList()
                            launchAction(
                                key = "deck:${deck.stableId()}",
                                success = "Пак «${deck.name}» открыт. Альбом обновлён.",
                                operation = {
                                    gamesRepository.openCardDeck(deck.stableId()).also { result ->
                                        if (result.isSuccess) authRepository.refreshProfile()
                                    }
                                },
                                onSuccess = { result ->
                                    shopRewardCards = result.openedCards.mapNotNull { it.card }
                                    rewardNotice(shopRewardCards)
                                },
                            )
                        }
                    },
                    )
                    currentTab == CardTab.Trade -> TradeTab(
                    trades = trades,
                    cards = cards,
                    wantedCardIds = wantedCardIds,
                    loading = loading && trades.isEmpty(),
                    action = action,
                    error = tradeError,
                    tradeCatalog = tradeCatalog,
                    tradeCatalogLoading = tradeCatalogLoading,
                    tradeCatalogError = tradeCatalogError,
                    onPrepareCreate = ::loadTradeCatalog,
                    onRetryCatalog = ::loadTradeCatalog,
                    onCreate = { request, onComplete ->
                        launchAction(
                            key = "trade:create",
                            success = "Предложение обмена выставлено.",
                            operation = { gamesRepository.createCardTrade(request) },
                            onSuccess = {
                                onComplete()
                                null
                            },
                        )
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
                    roulettePrice = decks.firstOrNull { it.kind == "roulette" }?.price ?: 250,
                    catalogLoading = catalogLoading,
                    catalogError = catalogError,
                    onRetryCatalog = { scope.launch { loadCardCatalog(force = true) } },
                    selectedTargetId = forgeTargetId,
                    onSelectTarget = { forgeTargetId = it },
                    onMode = { mode ->
                        forgeMode = mode
                        while (forgeSelection.size > mode.count) forgeSelection.removeAt(forgeSelection.lastIndex)
                        if (mode == ForgeMode.Random) forgeTargetId = null
                    },
                    onToggle = { card ->
                        val currentSelection = forgeSelection.toList()
                        val update = CardForgeSelection.toggle(
                            selectedIds = currentSelection,
                            ranksByCardId = cards.associate { it.id.trim() to cardRank(it) },
                            cardId = card.id,
                            cardRank = cardRank(card),
                            availableCopies = card.copies,
                            requiredMaterials = forgeMode.count,
                        )
                        if (update.cardIds != currentSelection) {
                            forgeSelection.clear()
                            forgeSelection.addAll(update.cardIds)
                            forgeTargetId = null
                        }
                        notice = update.message
                    },
                    onSell = { card -> cardToSell = card },
                    onForge = {
                        launchAction(
                            key = "forge",
                            success = "Карты перекованы. Коллекция обновлена.",
                            operation = {
                                gamesRepository.craftCards(
                                    cardIds = forgeSelection.toList(),
                                    targetCardId = forgeTargetId.takeIf { forgeMode == ForgeMode.Choose },
                                )
                            },
                            onSuccess = { result ->
                                forgedCard = result.granted?.card
                                val cardName = result.granted?.card?.let { card ->
                                    card.characterName?.takeIf(String::isNotBlank)
                                        ?: card.name.takeIf(String::isNotBlank)
                                }
                                cardName?.let { "Получена карточка «$it»." }
                            },
                        )
                    },
                    resultCard = forgedCard,
                    )
                    currentTab == CardTab.Album -> CardCatalogTab(
                        catalog = catalog,
                        ownedCards = cards,
                        wantedCardIds = wantedCardIds,
                        catalogError = catalogError,
                        collectionError = error,
                        action = action,
                        roulettePrice = decks.firstOrNull { it.kind == "roulette" }?.price ?: 250,
                        onRetryCatalog = { scope.launch { loadCardCatalog(force = true) } },
                        onRetryCollection = { scope.launch { refresh(showLoading = true) } },
                        onWant = { id ->
                            wantedCardIds = if (id in wantedCardIds) wantedCardIds - id else wantedCardIds + id
                        },
                        onSell = { card -> cardToSell = card },
                    )
                }
            }
        }
    }

    cardToSell?.let { card ->
        val cardName = card.characterName?.takeIf(String::isNotBlank) ?: card.name
        val sellPrice = cardSellPrice(card, decks.firstOrNull { it.kind == "roulette" }?.price ?: 250)
        val lastCopy = card.copies <= 1
        AlertDialog(
            onDismissRequest = { if (action == null) cardToSell = null },
            title = { Text(if (lastCopy) "Продать карточку?" else "Продать копию?") },
            text = {
                Text(
                    if (lastCopy) "Продать «$cardName» за $sellPrice монет? Это последняя копия — карточка исчезнет из коллекции."
                    else "Продать копию «$cardName» за $sellPrice монет? В коллекции останется ${card.copies - 1}.",
                )
            },
            confirmButton = {
                TextButton(
                    enabled = action == null,
                    onClick = {
                        cardToSell = null
                        launchAction(
                            key = "sell:${card.id}",
                            success = "Копия продана.",
                            operation = {
                                gamesRepository.sellCardCopy(card.id).onSuccess { authRepository.refreshProfile() }
                            },
                            onSuccess = { result ->
                                forgeSelection.removeAll { it == card.id }
                                if (result.copies == 1 && lastCopy) "Карточка продана · +${result.coins} монет"
                                else "Копия продана · +${result.coins} монет"
                            },
                        )
                    },
                ) { Text("Продать · $sellPrice") }
            },
            dismissButton = {
                TextButton(onClick = { cardToSell = null }, enabled = action == null) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun ShopTab(
    decks: List<GameCardDeckDto>,
    loading: Boolean,
    revealedCards: List<GameCardDto>,
    isAuthenticated: Boolean,
    onLogin: () -> Unit,
    randomCardPrice: Int,
    action: String?,
    error: String?,
    onRetry: () -> Unit,
    onPull: () -> Unit,
    onOpenDeck: (GameCardDeckDto) -> Unit,
) {
    var titleQuery by remember { mutableStateOf("") }
    val titleOffers = remember(decks, titleQuery) {
        val normalizedQuery = titleQuery.trim().lowercase()
        groupCardTitleOffers(decks).filter { normalizedQuery.isBlank() || it.title.lowercase().contains(normalizedQuery) }
    }
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
        item(key = "reward_reveal", span = { GridItemSpan(maxLineSpan) }) {
            CardRewardReveal(revealedCards)
        }
        if (error != null) item(span = { GridItemSpan(maxLineSpan) }) {
            InlineLoadError(error, onRetry)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Магазин карт", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Случайная карта или пак по тайтлу за монеты активности.", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Surface(color = TomiloSurface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, TomiloBorder)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Случайная карта", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Покупка без пака. Шансы от F к SSS жёсткие, номерная почти не выпадает. " +
                            "Дубликаты остаются в пуле — закрытая низкая коллекция не гарантирует " +
                            "старший ранг. Премиум чуть поднимает удачу на старших рангах.",
                        color = TomiloMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = { if (isAuthenticated) onPull() else onLogin() },
                        enabled = action == null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (action == "pull") {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Крутим…")
                        } else Text(
                            if (isAuthenticated) "Купить за $randomCardPrice монет"
                            else "Войти · $randomCardPrice монет",
                        )
                    }
                }
            }
        }
        if (decks.isEmpty() && error == null) item(span = { GridItemSpan(maxLineSpan) }) {
            EmptyState("Паков по тайтлам пока нет", "Случайная карта выше всё равно доступна.", icon = Icons.Default.Collections)
        }
        if (decks.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = titleQuery,
                onValueChange = { titleQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Найти тайтл") },
                shape = RoundedCornerShape(16.dp),
            )
        }
        if (decks.isNotEmpty() && titleOffers.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            EmptyState("Тайтлы не найдены", "Измените запрос и попробуйте снова.", icon = Icons.Default.Collections)
        }
        items(titleOffers, key = { it.key }) { offer ->
            Surface(
                color = TomiloSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, TomiloPremium.copy(alpha = .6f)),
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AsyncImage(
                        MediaUrl.resolve(offer.imageUrl),
                        offer.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(154.dp).clip(RoundedCornerShape(12.dp)),
                    )
                    Text(offer.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("В пуле ${offer.poolSize?.toString() ?: "?"} карт", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                    offer.roulette?.let { deck ->
                        val id = deck.stableId()
                        val pending = action == "deck:$id"
                        val available = deck.isAvailable && (deck.poolSize ?: 0) > 0
                        Button(
                            onClick = { if (isAuthenticated) onOpenDeck(deck) else onLogin() },
                            enabled = available && action == null,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (pending) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Открываем…")
                            } else {
                                Text(
                                    when {
                                        !available && deck.isAvailable -> "Нет карт в пуле"
                                        !deck.isAvailable -> "Сейчас недоступно"
                                        isAuthenticated -> "Прокрутка · ${deck.price} монет"
                                        else -> "Войти · прокрутка ${deck.price} монет"
                                    },
                                )
                            }
                        }
                    }
                    offer.pack?.let { deck ->
                        val id = deck.stableId()
                        val pending = action == "deck:$id"
                        val available = deck.isAvailable && (deck.poolSize ?: 0) > 0
                        Button(
                            onClick = { if (isAuthenticated) onOpenDeck(deck) else onLogin() },
                            enabled = available && action == null,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (pending) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Открываем…")
                            } else {
                                Text(
                                    when {
                                        !available && deck.isAvailable -> "Нет карт в пуле"
                                        !deck.isAvailable -> "Сейчас недоступно"
                                        isAuthenticated -> "Пак ${deck.cardsPerOpen} · ${deck.price} монет"
                                        else -> "Войти · пак ${deck.cardsPerOpen} · ${deck.price} монет"
                                    },
                                )
                            }
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
        exit = if (ValueAnimator.areAnimatorsEnabled()) fadeOut(tween(160)) else fadeOut(tween(0)),
    ) {
        Surface(
            color = TomiloSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, TomiloPrimary.copy(alpha = .65f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (cards.size == 1) "Новая карта" else "Карты из пака · ${cards.size}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(cards, key = { index, card -> "${card.id.ifBlank { card.name }}:$index" }) { _, card ->
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
    wantedCardIds: Set<String>,
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
    var showWantedOnly by remember { mutableStateOf(true) }
    var tradeQuery by remember { mutableStateOf("") }
    val visibleTrades = remember(trades, wantedCardIds, showWantedOnly, tradeQuery) {
        val query = tradeQuery.trim().lowercase()
        trades.filter { trade ->
            val matchesWants = !showWantedOnly || wantedCardIds.isEmpty() ||
                trade.offerCardId in wantedCardIds || trade.wantCardId in wantedCardIds
            val matchesQuery = query.isBlank() || listOfNotNull(
                trade.fromUsername,
                trade.note,
                trade.offerCard.name,
                trade.wantCard.name,
            ).any { it.lowercase().contains(query) }
            matchesWants && matchesQuery
        }
    }
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
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (wantedCardIds.isNotEmpty()) {
                    FilterChip(
                        selected = showWantedOnly,
                        onClick = { showWantedOnly = !showWantedOnly },
                        label = { Text(if (showWantedOnly) "По закладкам" else "Все обмены") },
                    )
                } else {
                    Text("Закладки ставятся кнопкой «Хочу» в альбоме.", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = tradeQuery,
                    onValueChange = { tradeQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Поиск по игроку или карте") },
                    shape = RoundedCornerShape(16.dp),
                )
            }
        }
        if (error != null) item { InlineLoadError(error, onRetry) }
        if (visibleTrades.isEmpty()) item {
            if (error == null) {
                EmptyState(
                    title = if (trades.isEmpty()) "Нет открытых обменов" else "Обмены не найдены",
                    message = when {
                        trades.isEmpty() -> "Выставите предложение или загляните позже — здесь появятся карты других игроков."
                        tradeQuery.isNotBlank() -> "Измените запрос и попробуйте снова."
                        wantedCardIds.isNotEmpty() && showWantedOnly -> "По карточкам из закладок открытых обменов пока нет."
                        else -> "Попробуйте изменить фильтр."
                    },
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    icon = Icons.Default.Collections,
                )
            }
        }
        items(visibleTrades, key = { it.id }) { trade ->
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
    var offerQuery by remember { mutableStateOf("") }
    var wantQuery by remember { mutableStateOf("") }
    var offerCopies by remember { mutableIntStateOf(1) }
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
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        repeat(2) {
                            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                SkeletonBox(Modifier.fillMaxWidth(.36f).height(15.dp), radius = 5.dp)
                                SkeletonBox(Modifier.fillMaxWidth().height(56.dp), radius = 14.dp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    repeat(2) {
                                        SkeletonBox(
                                            Modifier.weight(1f).aspectRatio(3f / 4.15f),
                                            radius = 14.dp,
                                        )
                                    }
                                }
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
                        CardTradeChoicePicker(
                            title = "Вы отдаёте",
                            choices = offerChoices,
                            selectedId = offerId,
                            query = offerQuery,
                            searchPlaceholder = "Найти свою карту",
                            onQueryChange = { offerQuery = it },
                            onSelect = {
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
                        CardTradeChoicePicker(
                            title = "Вы хотите получить",
                            choices = wantChoices.filterNot { it.id == offerId },
                            selectedId = wantId,
                            query = wantQuery,
                            searchPlaceholder = "Найти желаемую карту",
                            onQueryChange = { wantQuery = it },
                            onSelect = { wantId = it },
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it.take(80) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Подпись (необязательно)") },
                            placeholder = { Text("Подпись, например: меняю на Лукизм") },
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
    query: String,
    searchPlaceholder: String,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
) {
    val fontScale = LocalConfiguration.current.fontScale
    val choiceWidth = CardTradeChoiceLayout.widthDp(fontScale).dp
    val labelLines = CardTradeChoiceLayout.labelLines(fontScale)
    val visibleChoices = remember(choices, query) {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isEmpty()) choices else choices.filter {
            it.name.lowercase().contains(normalizedQuery) || it.subtitle.lowercase().contains(normalizedQuery)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(searchPlaceholder) },
            shape = RoundedCornerShape(14.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(visibleChoices, key = { it.id }) { choice ->
                val isSelected = choice.id == selectedId
                Surface(
                    onClick = { onSelect(choice.id) },
                    modifier = Modifier.semantics { selected = isSelected },
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .28f) else TomiloSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isSelected) TomiloPrimary else TomiloBorder),
                ) {
                    Column(Modifier.width(choiceWidth).padding(7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SubcomposeAsyncImage(
                            model = MediaUrl.resolve(choice.imageUrl),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4.15f)
                                .clip(RoundedCornerShape(9.dp)).background(TomiloBg),
                            loading = { SkeletonBox(Modifier.fillMaxSize(), radius = 9.dp) },
                            error = { Box(Modifier.fillMaxSize().background(TomiloSurface2)) },
                        )
                        Text(choice.name, maxLines = labelLines, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                        Text(choice.subtitle, maxLines = labelLines, overflow = TextOverflow.Ellipsis, color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        if (visibleChoices.isEmpty()) {
            Text("Подходящие карты не найдены.", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
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
    roulettePrice: Int,
    onMode: (ForgeMode) -> Unit, onToggle: (GameCardDto) -> Unit, onForge: () -> Unit,
    onSell: (GameCardDto) -> Unit,
    resultCard: GameCardDto?,
) {
    val selectedRank = selectedIds.firstOrNull()?.let { id -> cards.firstOrNull { it.id == id }?.let(::cardRank) }
    val validCount = selectedIds.size == mode.count
    val sameRank = selectedIds.all { id -> cards.firstOrNull { it.id == id }?.let(::cardRank) == selectedRank }
    if (collectionLoading) {
        CardCollectionSkeleton(Modifier.fillMaxSize())
        return
    }
    if (collectionError != null && cards.isEmpty()) {
        ErrorBox(message = collectionError, onRetry = onRetryCollection)
        return
    }
    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            val forgeMotionEnabled = ValueAnimator.areAnimatorsEnabled()
            Column(
                Modifier.then(if (forgeMotionEnabled) Modifier.animateContentSize() else Modifier),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Горн", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "3 карты одного ранга дадут случайную карту следующего (F→C→B→A→S→SSS). 6 карт — выбор конкретной карты следующего ранга. Копии одной карты можно положить в несколько слотов.",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                    enter = if (forgeMotionEnabled) {
                        fadeIn(tween(220)) + scaleIn(initialScale = .88f, animationSpec = tween(320))
                    } else fadeIn(tween(0)),
                    exit = fadeOut(tween(if (forgeMotionEnabled) 120 else 0)),
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
        if (cards.isEmpty()) {
            item {
                Text("Коллекция пуста — откройте пак или каталог.", color = TomiloMuted, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Продажа карточек", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Любую карту из коллекции можно продать. Цена: половина прокрутки минус 25 монет, дальше ×3 за ранг.",
                        color = TomiloMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        items(cards.size, key = { cards[it].id.ifBlank { cards[it].name } }) { index ->
            val card = cards[index]
            val id = card.id
            val selectedCopies = selectedIds.count { it == id }
            val isRankValid = selectedRank == null || cardRank(card) == selectedRank
            val motionEnabled = ValueAnimator.areAnimatorsEnabled()
            val selectedColor by animateColorAsState(
                targetValue = if (selectedCopies > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .28f) else TomiloSurface,
                animationSpec = if (motionEnabled) tween(durationMillis = 160) else snap(),
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
                        Text(card.characterName ?: card.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("Ранг ${cardRank(card)} · копий ${card.copies.coerceAtLeast(0)}", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                        TextButton(
                            onClick = { onSell(card) },
                            enabled = action == null && selectedCopies == 0 && card.copies > 0,
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                        ) {
                            Text(
                                if (action == "sell:$id") "Продаём…" else "Продать копию · ${cardSellPrice(card, roulettePrice)} монет",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(if (selectedCopies > 0) "×$selectedCopies" else "＋", color = TomiloPrimary, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun CardCatalogTab(
    catalog: List<GameCardCatalogItemDto>,
    ownedCards: List<GameCardDto>,
    wantedCardIds: Set<String>,
    catalogError: String?,
    collectionError: String?,
    action: String?,
    roulettePrice: Int,
    onRetryCatalog: () -> Unit,
    onRetryCollection: () -> Unit,
    onWant: (String) -> Unit,
    onSell: (GameCardDto) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var ownedFirst by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(CardCatalogSort.Popular) }
    var selectedRank by remember { mutableStateOf(CardCatalogRank.All) }
    val owned = remember(ownedCards) { ownedCards.associateBy(GameCardDto::id) }
    val entries = remember(catalog, ownedCards, query, ownedFirst, sort, selectedRank) {
        val normalizedQuery = query.trim().lowercase()
        val allEntries = if (catalog.isNotEmpty()) catalog else ownedCards.map { card ->
            GameCardCatalogItemDto(
                id = card.id,
                name = card.name,
                characterName = card.characterName,
                imageUrl = card.stageImageUrl ?: card.imageUrl,
                rarity = card.rarity,
                rank = card.forgeRank ?: card.currentStage.orEmpty(),
                titleId = card.titleId,
                titleName = card.titleName,
            )
        }
        allEntries.filter { card ->
            (normalizedQuery.isBlank() || listOfNotNull(card.characterName, card.name, card.titleName)
                .any { it.lowercase().contains(normalizedQuery) }) &&
                (selectedRank.rarity == null || card.rarity.equals(selectedRank.rarity, ignoreCase = true))
        }.sortedWith(
            compareBy<GameCardCatalogItemDto> { if (ownedFirst && it.id in owned) 0 else 1 }
                .thenBy {
                    when (sort) {
                        CardCatalogSort.Popular -> 0
                        CardCatalogSort.Rank -> listOf("F", "C", "B", "A", "S", "SSS").indexOf(CardEconomy.rank(it.rank, it.rarity)).let { rank -> if (rank < 0) Int.MAX_VALUE else rank }
                        CardCatalogSort.Name -> 0
                    }
                }
                .thenBy { if (sort == CardCatalogSort.Name) it.characterName ?: it.name else "" },
        )
    }
    val copies = ownedCards.sumOf { it.copies.coerceAtLeast(0) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(146.dp),
        state = rememberLazyGridState(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val catalogCount = catalog.size.takeIf { it > 0 }?.let { " · в каталоге $it" }.orEmpty()
                Text(
                    "В коллекции: $copies копий · ${ownedCards.size} уникальных$catalogCount",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Затемнение означает, что карты ещё нет в коллекции. «Хочу» — закладка, чтобы найти её в обменах.",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Найти карточку или тайтл") },
                    shape = RoundedCornerShape(16.dp),
                )
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = ownedFirst,
                        onClick = { ownedFirst = !ownedFirst },
                        label = { Text("Показать сначала") },
                    )
                    CardCatalogSort.entries.forEach { option ->
                        FilterChip(selected = sort == option, onClick = { sort = option }, label = { Text(option.label) })
                    }
                }
                Text("Ранг карточки", color = TomiloMuted, style = MaterialTheme.typography.labelMedium)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CardCatalogRank.entries.forEach { rank ->
                        FilterChip(
                            selected = selectedRank == rank,
                            onClick = { selectedRank = rank },
                            label = { Text(rank.label) },
                        )
                    }
                }
                if (catalogError != null) InlineLoadError(catalogError, onRetryCatalog)
                if (collectionError != null) InlineLoadError(collectionError, onRetryCollection)
            }
        }
        if (entries.isEmpty() && catalogError == null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    title = if (query.isBlank()) "Карточек пока нет" else "Ничего не найдено",
                    message = if (query.isBlank()) "Карточки появятся в каталоге по мере публикации." else "Попробуйте другое название или тайтл.",
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    icon = Icons.Default.Collections,
                )
            }
        }
        items(entries, key = { it.id.ifBlank { it.name } }) { entry ->
            val ownedCard = owned[entry.id]
            val wanted = entry.id in wantedCardIds
            CardCatalogItem(
                entry = entry,
                ownedCard = ownedCard,
                isWanted = wanted,
                action = action,
                roulettePrice = roulettePrice,
                onWant = { onWant(entry.id) },
                onSell = { ownedCard?.let(onSell) },
            )
        }
    }
}

@Composable
private fun CardCatalogItem(
    entry: GameCardCatalogItemDto,
    ownedCard: GameCardDto?,
    isWanted: Boolean,
    action: String?,
    roulettePrice: Int,
    onWant: () -> Unit,
    onSell: () -> Unit,
) {
    Surface(
        color = TomiloSurface,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, rarityColor(entry.rarity, entry.rank).copy(alpha = if (ownedCard != null) .72f else .3f)),
    ) {
        Column(Modifier.padding(7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.fillMaxWidth().height(164.dp).clip(RoundedCornerShape(10.dp)).background(TomiloBg)) {
                SubcomposeAsyncImage(
                    model = MediaUrl.resolve(entry.imageUrl),
                    contentDescription = entry.characterName?.takeIf(String::isNotBlank) ?: entry.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { SkeletonBox(Modifier.fillMaxSize(), radius = 0.dp) },
                    error = { Box(Modifier.fillMaxSize().background(TomiloSurface2)) },
                )
                if (ownedCard == null) {
                    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .32f)))
                    Text("ЕЩЁ НЕТ", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(8.dp)).background(TomiloBg.copy(alpha = .84f)).padding(horizontal = 8.dp, vertical = 5.dp))
                }
                Text(CardEconomy.rank(entry.rank, entry.rarity), color = rarityColor(entry.rarity, entry.rank), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.TopStart).clip(RoundedCornerShape(6.dp)).background(TomiloBg.copy(alpha = .9f)).padding(horizontal = 6.dp, vertical = 3.dp))
                ownedCard?.let { card ->
                    Text("×${card.copies}", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(6.dp)).background(TomiloBg.copy(alpha = .9f)).padding(horizontal = 6.dp, vertical = 3.dp))
                }
            }
            Text(entry.characterName?.takeIf(String::isNotBlank) ?: entry.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(entry.titleName ?: "Без тайтла", color = TomiloMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            FilterChip(
                selected = isWanted,
                onClick = onWant,
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                label = { Text(if (isWanted) "Хочу · добавлено" else "Хочу", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = {
                    Icon(if (isWanted) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                },
            )
            if (ownedCard != null) {
                TextButton(
                    onClick = onSell,
                    enabled = action == null,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                ) {
                    Text(
                        if (action == "sell:${ownedCard.id}") "Продаём…" else "Продать · ${cardSellPrice(ownedCard, roulettePrice)} монет",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
private fun cardRank(card: GameCardDto): String = CardEconomy.rank(card.forgeRank ?: card.currentStage, card.rarity)

private fun cardRank(card: GameCardCatalogItemDto): String = CardEconomy.rank(card.rank, card.rarity)

/** Mirrors the site's card-economy.ts sell formula; the server response remains authoritative. */
private fun cardSellPrice(card: GameCardDto, roulettePrice: Int): Int =
    CardEconomy.sellPrice(roulettePrice, cardRank(card))

private fun rarityColor(card: GameCardDto) = when (cardRank(card)) {
    "SSS" -> TomiloPremium
    "S", "A", "B" -> TomiloPrimary
    else -> TomiloBorder
}

private fun rarityColor(rarity: String, rank: String) = when (CardEconomy.rank(rank, rarity)) {
    "SSS" -> TomiloPremium
    "S", "A", "B" -> TomiloPrimary
    else -> TomiloBorder
}
