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
}
