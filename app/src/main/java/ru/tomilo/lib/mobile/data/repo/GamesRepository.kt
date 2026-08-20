package ru.tomilo.lib.mobile.data.repo

import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import ru.tomilo.lib.mobile.data.api.GameAlchemyStatusDto
import ru.tomilo.lib.mobile.data.api.GameCardsDto
import ru.tomilo.lib.mobile.data.api.GameDisciplesDto
import ru.tomilo.lib.mobile.data.api.GameInventoryItemDto
import ru.tomilo.lib.mobile.data.api.TomiloApi

data class GamesDashboard(
    val inventory: List<GameInventoryItemDto> = emptyList(),
    val disciples: GameDisciplesDto = GameDisciplesDto(),
    val cards: GameCardsDto = GameCardsDto(),
    val alchemy: GameAlchemyStatusDto = GameAlchemyStatusDto(),
    val warnings: List<String> = emptyList(),
)

class GamesRepository(private val api: TomiloApi) {
    /** Независимые игровые блоки загружаются параллельно и не ломают всю страницу при частичном сбое. */
    suspend fun dashboard(): Result<GamesDashboard> = supervisorScope {
        runCatching {
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
