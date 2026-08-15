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

    suspend fun login(email: String, password: String): Result<UserDto> = runCatching {
        persist(api.login(LoginRequest(email.trim(), password)))
    }

    suspend fun loginYandex(accessToken: String): Result<UserDto> = runCatching {
        persist(api.loginYandexToken(YandexTokenRequest(accessToken)))
    }

    suspend fun loginVkId(
        code: String,
        codeVerifier: String,
        deviceId: String,
        state: String,
    ): Result<UserDto> = runCatching {
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
        // Сразу кладём токен — иначе первый GET (чаты/закладки) уходит без Authorization
        TokenBridge.setCached(payload.accessToken)
        authStore.saveSession(payload.accessToken, payload.user)
        return payload.user
    }

    suspend fun refreshProfile(): Result<UserDto> = runCatching {
        val res = api.profile()
        val user = res.data ?: error(res.message ?: "Не удалось загрузить профиль")
        authStore.updateUser(user)
        user
    }

    suspend fun logout() {
        TokenBridge.setCached(null)
        authStore.clear()
    }

    suspend fun isPremium(): Boolean = Premium.isActive(authStore.user()?.subscriptionExpiresAt)

    suspend fun isLoggedIn(): Boolean = !authStore.token().isNullOrBlank()

    suspend fun dailyQuests(): Result<DailyQuestsDto> = runCatching {
        val res = api.dailyQuests()
        if (!res.success) error(res.message ?: res.errors?.firstOrNull() ?: "Не удалось загрузить задания")
        res.data ?: DailyQuestsDto()
    }

    suspend fun claimDailyBonus(): Result<DailyBonusResultDto> = runCatching {
        val res = api.claimDailyBonus()
        if (!res.success) error(res.message ?: res.errors?.firstOrNull() ?: "Бонус уже получен")
        val data = res.data ?: error(res.message ?: "Бонус не получен")
        refreshProfile()
        data
    }

    suspend fun claimQuest(questId: String): Result<QuestClaimResultDto> = runCatching {
        val res = api.claimDailyQuest(QuestClaimRequest(questId))
        if (!res.success) error(res.message ?: res.errors?.firstOrNull() ?: "Награда недоступна")
        val data = res.data ?: QuestClaimResultDto()
        refreshProfile()
        data
    }

    suspend fun claimAllQuests(): Result<QuestClaimResultDto> = runCatching {
        val res = api.claimAllDailyQuests()
        if (!res.success) error(res.message ?: res.errors?.firstOrNull() ?: "Нет доступных наград")
        val data = res.data ?: QuestClaimResultDto()
        refreshProfile()
        data
    }

    suspend fun wheel(): Result<WheelDto> = runCatching {
        val res = api.wheel()
        if (!res.success) error(res.message ?: res.errors?.firstOrNull() ?: "Не удалось загрузить колесо")
        res.data ?: error("Колесо временно недоступно")
    }

    suspend fun spinWheel(skipCooldown: Boolean = false): Result<WheelSpinResultDto> = runCatching {
        val res = api.spinWheel(WheelSpinRequest(skipCooldown.takeIf { it }))
        if (!res.success) error(res.message ?: res.errors?.firstOrNull() ?: "Не удалось запустить колесо")
        val result = res.data ?: error("Сервер не вернул награду")
        refreshProfile()
        result
    }

    suspend fun wheelRecentWins(): Result<WheelRecentWinsDto> = runCatching {
        val res = api.wheelRecentWins()
        if (!res.success) error(res.message ?: "Не удалось загрузить победителей")
        res.data ?: WheelRecentWinsDto()
    }
}
