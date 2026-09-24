package ru.tomilo.lib.mobile.data.repo

import kotlinx.coroutines.flow.Flow
import ru.tomilo.lib.mobile.TokenBridge
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.data.api.ApiResponse
import ru.tomilo.lib.mobile.data.api.AuthPayload
import ru.tomilo.lib.mobile.data.api.LoginRequest
import ru.tomilo.lib.mobile.data.api.TomiloApi
import ru.tomilo.lib.mobile.data.api.UserDto
import ru.tomilo.lib.mobile.data.api.VkIdLoginRequest
import ru.tomilo.lib.mobile.data.api.YandexTokenRequest
import ru.tomilo.lib.mobile.data.api.DailyQuestsDto
import ru.tomilo.lib.mobile.data.api.DailyBonusResultDto
import ru.tomilo.lib.mobile.data.api.QuestClaimRequest
import ru.tomilo.lib.mobile.data.api.QuestClaimResultDto
import ru.tomilo.lib.mobile.data.api.WheelDto
import ru.tomilo.lib.mobile.data.api.WheelRecentWinsDto
import ru.tomilo.lib.mobile.data.api.WheelSpinRequest
import ru.tomilo.lib.mobile.data.api.WheelSpinResultDto
import ru.tomilo.lib.mobile.data.local.AuthStore

class AuthRepository(
    private val api: TomiloApi,
    private val authStore: AuthStore,
) {
    val tokenFlow: Flow<String?> = authStore.tokenFlow
    val userFlow: Flow<UserDto?> = authStore.userFlow

    suspend fun login(email: String, password: String): Result<UserDto> = runCatchingCancellable {
        persist(api.login(LoginRequest(email.trim(), password)))
    }

    suspend fun loginYandex(accessToken: String): Result<UserDto> = runCatchingCancellable {
        persist(api.loginYandexToken(YandexTokenRequest(accessToken)))
    }

    suspend fun loginVkId(
        code: String,
        codeVerifier: String,
        deviceId: String,
        state: String,
    ): Result<UserDto> = runCatchingCancellable {
        persist(
            api.loginVkId(
                VkIdLoginRequest(
                    code = code,
                    codeVerifier = codeVerifier,
                    deviceId = deviceId,
                    state = state,
                ),
            ),
        )
    }

    private suspend fun persist(res: ApiResponse<AuthPayload>): UserDto {
        val payload = res.data
            ?: error(res.message ?: res.errors?.firstOrNull() ?: "Ошибка входа")
        if (!res.success || payload.accessToken.isBlank()) {
            error(res.message ?: "Ошибка авторизации")
        }
        authStore.saveSession(payload.accessToken, payload.refreshToken, payload.user)
        // Publish both credentials together only after the durable session write succeeds.
        TokenBridge.setCachedSession(payload.accessToken, payload.refreshToken)
        return payload.user
    }

    suspend fun refreshProfile(): Result<UserDto> = runCatchingCancellable {
        val res = api.profile()
        val user = res.data ?: error(res.message ?: "Не удалось загрузить профиль")
        authStore.updateUser(user)
        user
    }

    suspend fun logout() {
        // Remove in-memory credentials before the suspendable DataStore clear, so an
        // in-flight 401 refresh cannot restore a session after logout.
        TokenBridge.clearCachedSession()
        authStore.clear()
    }

    suspend fun isPremium(): Boolean = Premium.isActive(authStore.user()?.subscriptionExpiresAt)

    suspend fun isLoggedIn(): Boolean = !authStore.token().isNullOrBlank()

    suspend fun dailyQuests(): Result<DailyQuestsDto> = runCatchingCancellable {
        val res = api.dailyQuests()
        if (!res.success) error(res.message ?: res.errors?.firstOrNull() ?: "Не удалось загрузить задания")
        res.data ?: DailyQuestsDto()
    }

    suspend fun claimDailyBonus(): Result<DailyBonusResultDto> = runCatchingCancellable {
        val res = api.claimDailyBonus()
        if (!res.success) error(res.message ?: res.errors?.firstOrNull() ?: "Бонус уже получен")
        val data = res.data ?: error(res.message ?: "Бонус не получен")
        refreshProfile()
        data
    }

    suspend fun claimQuest(questId: String): Result<QuestClaimResultDto> = runCatchingCancellable {
        val res = api.claimDailyQuest(QuestClaimRequest(questId))
        if (!res.success) error(res.message ?: res.errors?.firstOrNull() ?: "Награда недоступна")
        val data = res.data ?: QuestClaimResultDto()
        refreshProfile()
        data
    }

    suspend fun claimAllQuests(): Result<QuestClaimResultDto> = runCatchingCancellable {
        val res = api.claimAllDailyQuests()
        if (!res.success) error(res.message ?: res.errors?.firstOrNull() ?: "Нет доступных наград")
        val data = res.data ?: QuestClaimResultDto()
        refreshProfile()
        data
    }

    suspend fun wheel(): Result<WheelDto> = runCatchingCancellable {
        val res = api.wheel()
        if (!res.success) error(res.message ?: res.errors?.firstOrNull() ?: "Не удалось загрузить колесо")
        res.data ?: error("Колесо временно недоступно")
    }

    suspend fun spinWheel(skipCooldown: Boolean = false): Result<WheelSpinResultDto> = runCatchingCancellable {
        val res = api.spinWheel(WheelSpinRequest(skipCooldown.takeIf { it }))
        if (!res.success) error(res.message ?: res.errors?.firstOrNull() ?: "Не удалось запустить колесо")
        val result = res.data ?: error("Сервер не вернул награду")
        refreshProfile()
        result
    }

    suspend fun wheelRecentWins(): Result<WheelRecentWinsDto> = runCatchingCancellable {
        val res = api.wheelRecentWins()
        if (!res.success) error(res.message ?: "Не удалось загрузить победителей")
        res.data ?: WheelRecentWinsDto()
    }
}
