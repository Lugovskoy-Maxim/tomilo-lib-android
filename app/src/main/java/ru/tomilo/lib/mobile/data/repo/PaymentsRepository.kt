package ru.tomilo.lib.mobile.data.repo

import retrofit2.HttpException
import ru.tomilo.lib.mobile.data.api.ApiResponse
import ru.tomilo.lib.mobile.data.api.CoinPremiumPurchaseRequest
import ru.tomilo.lib.mobile.data.api.CoinPremiumPurchaseResultDto
import ru.tomilo.lib.mobile.data.api.CreateTbankPaymentRequest
import ru.tomilo.lib.mobile.data.api.PremiumPaymentHistoryItemDto
import ru.tomilo.lib.mobile.data.api.RobokassaPaymentFormDto
import ru.tomilo.lib.mobile.data.api.RobokassaPaymentStatusDto
import ru.tomilo.lib.mobile.data.api.TomiloApi
import java.util.UUID

class PaymentsRepository(private val api: TomiloApi) {

    suspend fun createTbankPayment(planId: String): Result<RobokassaPaymentFormDto> =
        runCatching {
            unwrap(api.createTbankPayment(CreateTbankPaymentRequest(planId)))
        }

    suspend fun createAdminTestPayment(): Result<RobokassaPaymentFormDto> = runCatching {
        unwrap(api.createAdminRobokassaTestPayment())
    }

    suspend fun paymentStatus(invId: String): Result<RobokassaPaymentStatusDto> = runCatching {
        unwrap(api.robokassaPaymentStatus(invId))
    }

    suspend fun history(): Result<List<PremiumPaymentHistoryItemDto>> = runCatching {
        val res = api.paymentHistory()
        if (!res.success) error(messageOf(res))
        res.data.orEmpty()
    }

    suspend fun buyPremiumWithCoins(): Result<CoinPremiumPurchaseResultDto> = runCatching {
        unwrap(api.purchasePremiumWithCoins(CoinPremiumPurchaseRequest(UUID.randomUUID().toString())))
    }

    private fun <T> unwrap(res: ApiResponse<T>): T {
        if (!res.success || res.data == null) error(messageOf(res))
        return res.data
    }

    private fun messageOf(res: ApiResponse<*>): String =
        res.message ?: res.errors?.firstOrNull() ?: "Не удалось выполнить запрос"

    companion object {
        fun userMessage(error: Throwable): String {
            if (error is HttpException) {
                val raw = runCatching { error.response()?.errorBody()?.string() }.getOrNull()
                if (!raw.isNullOrBlank()) {
                    Regex("\"message\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
                        .find(raw)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.replace("\\\"", "\"")
                        ?.replace("\\\\", "\\")
                        ?.takeIf { it.isNotBlank() }
                        ?.let { return it }
                }
                if (error.code() == 401) return "Войдите в аккаунт, чтобы оплатить"
                if (error.code() in 500..599) return "Онлайн-оплата временно недоступна"
            }
            return error.message?.takeIf { it.isNotBlank() }
                ?: "Не удалось открыть оплату. Попробуйте ещё раз."
        }
    }
}
