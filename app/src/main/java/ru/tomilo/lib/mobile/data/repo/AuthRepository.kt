package ru.tomilo.lib.mobile.data.repo

import kotlinx.coroutines.flow.Flow
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.data.api.LoginRequest
import ru.tomilo.lib.mobile.data.api.TomiloApi
import ru.tomilo.lib.mobile.data.api.UserDto
import ru.tomilo.lib.mobile.data.local.AuthStore

class AuthRepository(
    private val api: TomiloApi,
    private val authStore: AuthStore,
) {
    val tokenFlow: Flow<String?> = authStore.tokenFlow
    val userFlow: Flow<UserDto?> = authStore.userFlow

    suspend fun login(email: String, password: String): Result<UserDto> = runCatching {
        val res = api.login(LoginRequest(email.trim(), password))
        val payload = res.data
            ?: error(res.message ?: res.errors?.firstOrNull() ?: "Ошибка входа")
        if (!res.success || payload.accessToken.isBlank()) {
            error(res.message ?: "Неверный email или пароль")
        }
        authStore.saveSession(payload.accessToken, payload.user)
        payload.user
    }

    suspend fun refreshProfile(): Result<UserDto> = runCatching {
        val res = api.profile()
        val user = res.data ?: error(res.message ?: "Не удалось загрузить профиль")
        authStore.updateUser(user)
        user
    }

    suspend fun logout() = authStore.clear()

    suspend fun isPremium(): Boolean = Premium.isActive(authStore.user()?.subscriptionExpiresAt)

    suspend fun isLoggedIn(): Boolean = !authStore.token().isNullOrBlank()
}
