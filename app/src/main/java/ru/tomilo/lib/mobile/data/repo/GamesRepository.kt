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
import ru.tomilo.lib.mobile.data.api.GameCardTradesDto
import ru.tomilo.lib.mobile.data.api.GameCraftRequest
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
    suspend fun cards(): Result<GameCardsDto> = runCatching {
        val response = api.gameCards()
        if (!response.success) error(response.message ?: "Не удалось загрузить коллекцию карточек")
        response.data ?: GameCardsDto()
    }

    suspend fun pillMatchState(): Result<PillMatchStateDto> = runCatching {
        val response = api.pillMatchState()
        if (!response.success) error(response.message ?: "Не удалось загрузить прогресс")
        response.data ?: error("Сервер не вернул прогресс")
    }
    suspend fun completePillMatchLevel(level: Int): Result<PillMatchCompleteDto> = runCatching {
        val response = api.completePillMatchLevel(PillMatchCompleteRequest(level))
        if (!response.success) error(response.message ?: "Не удалось сохранить прохождение")
        response.data ?: error("Сервер не вернул награду")
    }
    suspend fun cardDecks(): Result<List<GameCardDeckDto>> = runCatching {
        val response = api.gameCardDecks()
        if (!response.success) error(response.message ?: "Не удалось загрузить наборы")
        response.data.orEmpty()
    }

    suspend fun cardCatalog(): Result<List<GameCardCatalogItemDto>> = runCatching {
        val response = api.gameCardCatalog(limit = 1_000)
        if (!response.success) error(response.message ?: "Не удалось загрузить каталог карточек")
        response.data?.cards.orEmpty()
    }

    suspend fun pullCard(): Result<Unit> = runCatching {
        val response = api.pullGameCard()
        if (!response.success) error(response.message ?: "Не удалось получить карту")
    }

    suspend fun openCardDeck(deckId: String): Result<Unit> = runCatching {
        val response = api.openGameCardDeck(deckId)
        if (!response.success) error(response.message ?: "Не удалось открыть набор")
    }

    suspend fun cardTrades(): Result<GameCardTradesDto> = runCatching {
        val response = api.gameCardTrades(); if (!response.success) error(response.message ?: "Не удалось загрузить обмены")
        response.data ?: GameCardTradesDto()
    }
    suspend fun acceptCardTrade(id: String): Result<Unit> = runCatching { val r = api.acceptGameCardTrade(id); if (!r.success) error(r.message ?: "Не удалось принять обмен") }
    suspend fun craftCards(cardIds: List<String>, targetCardId: String? = null): Result<Unit> = runCatching {
        val r = api.craftGameCards(GameCraftRequest(cardIds, targetCardId))
        if (!r.success) error(r.message ?: "Не удалось перековать карточки")
    }
    suspend fun disciples(): Result<GameDisciplesDto> = runCatching {
        val response = api.gameDisciples()
        if (!response.success) error(response.message ?: "Не удалось обновить секту")
        response.data ?: error("Сервер не вернул данные секты")
    }

    suspend fun train(characterId: String): Result<GameTrainResultDto> = runCatching {
        val response = api.gameTrainDisciple(GameCharacterRequest(characterId))
        if (!response.success) error(response.message ?: "Тренировка не удалась")
        response.data ?: GameTrainResultDto()
    }

    suspend fun setPrimary(characterId: String): Result<Unit> = runCatching {
        val response = api.gameSetPrimaryDisciple(GameCharacterRequest(characterId))
        if (!response.success) error(response.message ?: "Не удалось назначить основного ученика")
    }

    suspend fun setWarehouse(characterId: String, inWarehouse: Boolean): Result<Unit> = runCatching {
        val response = api.gameSetDiscipleWarehouse(GameWarehouseRequest(characterId, inWarehouse))
        if (!response.success) error(response.message ?: "Не удалось изменить состав секты")
    }

    suspend fun saveBattleSquad(characterIds: List<String>): Result<List<String>> = runCatching {
        val response = api.gameSetBattleSquad(GameBattleSquadRequest(characterIds))
        if (!response.success) error(response.message ?: "Не удалось сохранить боевой отряд")
        response.data?.battleSquadCharacterIds ?: characterIds
    }

    suspend fun findOpponent(): Result<GameBattleMatchDto?> = runCatching {
        val response = api.gameBattleMatch()
        if (!response.success) error(response.message ?: "Не удалось найти соперника")
        response.data
    }

    suspend fun battle(opponentUserId: String, characterIds: List<String>): Result<GameBattleResultDto> = runCatching {
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
        runCatching {
            if (!includeAdminModes) return@runCatching GamesDashboard()
            val inventoryCall = async {
                runCatching {
                    val response = api.gameInventory()
                    if (!response.success) error(response.message ?: "Не удалось загрузить хранилище")
                    response.data.orEmpty().filter { it.itemId.isNotBlank() && it.count > 0 }
                }
            }
            val disciplesCall = async {
                runCatching {
                    val response = api.gameDisciples()
                    if (!response.success) error(response.message ?: "Не удалось загрузить секту")
                    response.data ?: GameDisciplesDto()
                }
            }
            val cardsCall = async {
                runCatching {
                    val response = api.gameCards()
                    if (!response.success) error(response.message ?: "Не удалось загрузить карты")
                    response.data ?: GameCardsDto()
                }
            }
            val alchemyCall = async {
                runCatching {
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
