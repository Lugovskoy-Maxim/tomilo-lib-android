package ru.tomilo.lib.mobile.data.repo

import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import ru.tomilo.lib.mobile.data.api.GameAlchemyStatusDto
import ru.tomilo.lib.mobile.data.api.GameBattleMatchDto
import ru.tomilo.lib.mobile.data.api.GameBattleRequest
import ru.tomilo.lib.mobile.data.api.GameBattleResultDto
import ru.tomilo.lib.mobile.data.api.GameBattleSquadRequest
import ru.tomilo.lib.mobile.data.api.GameCardsDto
import ru.tomilo.lib.mobile.data.api.GameCardDeckDto
import ru.tomilo.lib.mobile.data.api.GameCardCatalogItemDto
import ru.tomilo.lib.mobile.data.api.GameCardCatalogDto
import ru.tomilo.lib.mobile.data.api.GameCardTradesDto
import ru.tomilo.lib.mobile.data.api.GameCardTradeCatalogDto
import ru.tomilo.lib.mobile.data.api.GameCardTradeCreateRequest
import ru.tomilo.lib.mobile.data.api.GameCardOpenResultDto
import ru.tomilo.lib.mobile.data.api.GameCraftRequest
import ru.tomilo.lib.mobile.data.api.GameCraftResultDto
import ru.tomilo.lib.mobile.data.api.GameCardSellRequest
import ru.tomilo.lib.mobile.data.api.GameCardSellResultDto
import ru.tomilo.lib.mobile.data.api.GameCharacterRequest
import ru.tomilo.lib.mobile.data.api.GameDisciplesDto
import ru.tomilo.lib.mobile.data.api.GameInventoryItemDto
import ru.tomilo.lib.mobile.data.api.GameTrainResultDto
import ru.tomilo.lib.mobile.data.api.GameWarehouseRequest
import ru.tomilo.lib.mobile.data.api.PillMatchCompleteDto
import ru.tomilo.lib.mobile.data.api.PillMatchCompleteRequest
import ru.tomilo.lib.mobile.data.api.PillMatchStateDto
import ru.tomilo.lib.mobile.data.api.TomiloApi

data class GamesDashboard(
    val inventory: List<GameInventoryItemDto> = emptyList(),
    val disciples: GameDisciplesDto = GameDisciplesDto(),
    val cards: GameCardsDto = GameCardsDto(),
    val alchemy: GameAlchemyStatusDto = GameAlchemyStatusDto(),
    val warnings: List<String> = emptyList(),
)

class GamesRepository(private val api: TomiloApi) {
    suspend fun cards(): Result<GameCardsDto> = runCatchingCancellable {
        val response = api.gameCards()
        if (!response.success) error(response.message ?: "Не удалось загрузить коллекцию карточек")
        response.data ?: GameCardsDto()
    }

    suspend fun pillMatchState(): Result<PillMatchStateDto> = runCatchingCancellable {
        val response = api.pillMatchState()
        if (!response.success) error(response.message ?: "Не удалось загрузить прогресс")
        response.data ?: error("Сервер не вернул прогресс")
    }
    suspend fun completePillMatchLevel(level: Int): Result<PillMatchCompleteDto> = runCatchingCancellable {
        val response = api.completePillMatchLevel(PillMatchCompleteRequest(level))
        if (!response.success) error(response.message ?: "Не удалось сохранить прохождение")
        response.data ?: error("Сервер не вернул награду")
    }
    suspend fun cardDecks(): Result<List<GameCardDeckDto>> = runCatchingCancellable {
        val response = api.gameCardDecks()
        if (!response.success) error(response.message ?: "Не удалось загрузить паки")
        response.data?.decks.orEmpty()
    }

    suspend fun cardCatalog(): Result<List<GameCardCatalogItemDto>> = runCatchingCancellable {
        val pageSize = 1_000
        val firstResponse = api.gameCardCatalog(page = 1, limit = pageSize)
        if (!firstResponse.success) error(firstResponse.message ?: "Не удалось загрузить каталог карточек")
        val firstPage = firstResponse.data ?: GameCardCatalogDto()
        val catalog = firstPage.cards.toMutableList()
        val totalPages = ((firstPage.total.coerceAtLeast(catalog.size) + pageSize - 1) / pageSize)
            .coerceIn(1, 10)
        for (page in 2..totalPages) {
            val response = api.gameCardCatalog(page = page, limit = pageSize)
            if (!response.success) error(response.message ?: "Не удалось загрузить каталог карточек")
            val data = response.data ?: break
            if (data.cards.isEmpty()) break
            catalog += data.cards
        }
        catalog.distinctBy { it.id }
    }

    suspend fun pullCard(): Result<GameCardOpenResultDto> = runCatchingCancellable {
        val response = api.pullGameCard()
        if (!response.success) error(response.message ?: "Не удалось получить карту")
        response.data ?: error("Сервер не вернул полученную карту")
    }

    suspend fun openCardDeck(deckId: String): Result<GameCardOpenResultDto> = runCatchingCancellable {
        val response = api.openGameCardDeck(deckId)
        if (!response.success) error(response.message ?: "Не удалось открыть пак")
        response.data ?: error("Сервер не вернул карты из пака")
    }

    suspend fun cardTrades(): Result<GameCardTradesDto> = runCatchingCancellable {
        val response = api.gameCardTrades(); if (!response.success) error(response.message ?: "Не удалось загрузить обмены")
        response.data ?: GameCardTradesDto()
    }
    suspend fun cardTradeCatalog(query: String? = null): Result<GameCardTradeCatalogDto> = runCatchingCancellable {
        val response = api.gameCardTradeCatalog(query?.trim()?.takeIf(String::isNotBlank))
        if (!response.success) error(response.message ?: "Не удалось загрузить каталог для обмена")
        response.data ?: GameCardTradeCatalogDto()
    }
    suspend fun createCardTrade(body: GameCardTradeCreateRequest): Result<Unit> = runCatchingCancellable {
        val response = api.createGameCardTrade(body)
        if (!response.success) error(response.message ?: "Не удалось выставить обмен")
    }
    suspend fun acceptCardTrade(id: String): Result<Unit> = runCatchingCancellable { val r = api.acceptGameCardTrade(id); if (!r.success) error(r.message ?: "Не удалось принять обмен") }
    suspend fun cancelCardTrade(id: String): Result<Unit> = runCatchingCancellable { val r = api.cancelGameCardTrade(id); if (!r.success) error(r.message ?: "Не удалось снять предложение обмена") }
    suspend fun craftCards(cardIds: List<String>, targetCardId: String? = null): Result<GameCraftResultDto> = runCatchingCancellable {
        val r = api.craftGameCards(GameCraftRequest(cardIds, targetCardId))
        if (!r.success) error(r.message ?: "Не удалось перековать карточки")
        r.data ?: error("Сервер не вернул результат перековки")
    }
    suspend fun sellCardCopy(cardId: String): Result<GameCardSellResultDto> = runCatchingCancellable {
        if (cardId.isBlank()) error("Не удалось определить карточку")
        val response = api.sellGameCard(GameCardSellRequest(cardId = cardId, copies = 1))
        if (!response.success) error(response.message ?: "Не удалось продать карточку")
        response.data ?: error("Сервер не вернул результат продажи")
    }
    suspend fun disciples(): Result<GameDisciplesDto> = runCatchingCancellable {
        val response = api.gameDisciples()
        if (!response.success) error(response.message ?: "Не удалось обновить секту")
        response.data ?: error("Сервер не вернул данные секты")
    }

    suspend fun train(characterId: String): Result<GameTrainResultDto> = runCatchingCancellable {
        val response = api.gameTrainDisciple(GameCharacterRequest(characterId))
        if (!response.success) error(response.message ?: "Тренировка не удалась")
        response.data ?: GameTrainResultDto()
    }

    suspend fun setPrimary(characterId: String): Result<Unit> = runCatchingCancellable {
        val response = api.gameSetPrimaryDisciple(GameCharacterRequest(characterId))
        if (!response.success) error(response.message ?: "Не удалось назначить основного ученика")
    }

    suspend fun setWarehouse(characterId: String, inWarehouse: Boolean): Result<Unit> = runCatchingCancellable {
        val response = api.gameSetDiscipleWarehouse(GameWarehouseRequest(characterId, inWarehouse))
        if (!response.success) error(response.message ?: "Не удалось изменить состав секты")
    }

    suspend fun saveBattleSquad(characterIds: List<String>): Result<List<String>> = runCatchingCancellable {
        val response = api.gameSetBattleSquad(GameBattleSquadRequest(characterIds))
        if (!response.success) error(response.message ?: "Не удалось сохранить боевой отряд")
        response.data?.battleSquadCharacterIds ?: characterIds
    }

    suspend fun findOpponent(): Result<GameBattleMatchDto?> = runCatchingCancellable {
        val response = api.gameBattleMatch()
        if (!response.success) error(response.message ?: "Не удалось найти соперника")
        response.data
    }

    suspend fun battle(opponentUserId: String, characterIds: List<String>): Result<GameBattleResultDto> = runCatchingCancellable {
        val response = api.gameBattle(
            GameBattleRequest(
                opponentUserId = opponentUserId,
                myDiscipleIds = characterIds,
            ),
        )
        if (!response.success) error(response.message ?: "Не удалось провести бой")
        response.data ?: error("Сервер не вернул результат боя")
    }

    /** Независимые игровые блоки загружаются параллельно и не ломают всю страницу при частичном сбое. */
    suspend fun dashboard(includeAdminModes: Boolean): Result<GamesDashboard> = supervisorScope {
        runCatchingCancellable {
            if (!includeAdminModes) return@runCatchingCancellable GamesDashboard()
            val inventoryCall = async {
                runCatchingCancellable {
                    val response = api.gameInventory()
                    if (!response.success) error(response.message ?: "Не удалось загрузить хранилище")
                    response.data.orEmpty().filter { it.itemId.isNotBlank() && it.count > 0 }
                }
            }
            val disciplesCall = async {
                runCatchingCancellable {
                    val response = api.gameDisciples()
                    if (!response.success) error(response.message ?: "Не удалось загрузить секту")
                    response.data ?: GameDisciplesDto()
                }
            }
            val cardsCall = async {
                runCatchingCancellable {
                    val response = api.gameCards()
                    if (!response.success) error(response.message ?: "Не удалось загрузить карты")
                    response.data ?: GameCardsDto()
                }
            }
            val alchemyCall = async {
                runCatchingCancellable {
                    val response = api.gameAlchemyStatus()
                    if (!response.success) error(response.message ?: "Не удалось загрузить алхимию")
                    response.data ?: GameAlchemyStatusDto()
                }
            }

            val inventory = inventoryCall.await()
            val disciples = disciplesCall.await()
            val cards = cardsCall.await()
            val alchemy = alchemyCall.await()
            val calls = listOf(inventory, disciples, cards, alchemy)
            if (calls.all { it.isFailure }) {
                throw calls.firstNotNullOfOrNull { it.exceptionOrNull() }
                    ?: IllegalStateException("Игровой профиль недоступен")
            }
            GamesDashboard(
                inventory = inventory.getOrDefault(emptyList()),
                disciples = disciples.getOrDefault(GameDisciplesDto()),
                cards = cards.getOrDefault(GameCardsDto()),
                alchemy = alchemy.getOrDefault(GameAlchemyStatusDto()),
                warnings = calls.mapNotNull { it.exceptionOrNull()?.message }.distinct(),
            )
        }
    }
}
