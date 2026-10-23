package ru.tomilo.lib.mobile.data.repo

import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.tomilo.lib.mobile.data.api.ApiResponse
import ru.tomilo.lib.mobile.data.api.CultivationMeditationResultDto
import ru.tomilo.lib.mobile.data.api.TomiloApi

class GamesRepositoryCultivationTest {
    @Test
    fun claimMeditationReturnsServerConfirmedCultivationExperience() = runBlocking {
        val api = claimApi(
            ApiResponse(
                success = true,
                data = CultivationMeditationResultDto(
                    characterId = "disciple-1",
                    ticks = 1,
                    cultivationExpGained = 2,
                ),
            ),
        )

        val result = GamesRepository(api).claimMeditation("disciple-1")

        assertEquals(2, result.getOrNull())
    }

    @Test
    fun claimMeditationFailsWhenSuccessResponseHasNoRewardPayload() = runBlocking {
        val api = claimApi(ApiResponse(success = true))

        val result = GamesRepository(api).claimMeditation("disciple-1")

        assertEquals("Сервер не вернул результат медитации", result.exceptionOrNull()?.message)
    }

    private fun claimApi(response: ApiResponse<CultivationMeditationResultDto>): TomiloApi =
        Proxy.newProxyInstance(
            TomiloApi::class.java.classLoader,
            arrayOf(TomiloApi::class.java),
        ) { _, method, args ->
            assertEquals("claimDiscipleMeditation", method.name)
            assertEquals("disciple-1", args?.getOrNull(0))
            response
        } as TomiloApi
}
