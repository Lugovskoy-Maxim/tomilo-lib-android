package ru.tomilo.lib.mobile.data.repo

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class PaymentsRepositoryUserMessageTest {
    @Test
    fun preservesActionableBackendMessage() {
        val error = httpError(400, """{"message":"Недостаточно монет"}""")

        assertEquals("Недостаточно монет", PaymentsRepository.userMessage(error))
    }

    @Test
    fun hidesTechnicalBackendMessage() {
        val error = httpError(400, """{"message":"IllegalStateException: internal payment provider error"}""")

        assertEquals(
            "Произошла техническая ошибка. Попробуйте снова.",
            PaymentsRepository.userMessage(error),
        )
    }

    @Test
    fun givesPaymentSpecificServerFailureCopy() {
        assertEquals(
            "Онлайн-оплата временно недоступна",
            PaymentsRepository.userMessage(httpError(503, """{"message":"database timeout"}""")),
        )
    }

    private fun httpError(code: Int, body: String): HttpException = HttpException(
        Response.error<Unit>(
            code,
            body.toResponseBody("application/json".toMediaType()),
        ),
    )
}
