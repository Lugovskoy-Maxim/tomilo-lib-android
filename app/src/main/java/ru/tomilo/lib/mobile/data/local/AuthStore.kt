package ru.tomilo.lib.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import ru.tomilo.lib.mobile.data.api.NetworkModule
import ru.tomilo.lib.mobile.data.api.UserDto

private val Context.authDataStore by preferencesDataStore("tomilo_auth")

class AuthStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    private val userKey = stringPreferencesKey("user_json")

    val tokenFlow: Flow<String?> = context.authDataStore.data.map { SessionSecretCipher.decrypt(it[tokenKey]) }
    val refreshTokenFlow: Flow<String?> = context.authDataStore.data.map { SessionSecretCipher.decrypt(it[refreshTokenKey]) }
    val userFlow: Flow<UserDto?> = context.authDataStore.data.map { prefs ->
        prefs[userKey]?.let {
            runCatching { NetworkModule.json.decodeFromString<UserDto>(it) }.getOrNull()
        }
    }

    suspend fun token(): String? = tokenFlow.first()
    suspend fun refreshToken(): String? = refreshTokenFlow.first()

    suspend fun user(): UserDto? = userFlow.first()

    /** Encrypts credentials written by earlier app versions before exposing them to callers. */
    suspend fun encryptLegacySessionSecrets() {
        context.authDataStore.edit { prefs ->
            prefs[tokenKey]?.let { value ->
                if (!value.startsWith("enc:v1:")) prefs[tokenKey] = SessionSecretCipher.encrypt(value)
            }
            prefs[refreshTokenKey]?.let { value ->
                if (!value.startsWith("enc:v1:")) prefs[refreshTokenKey] = SessionSecretCipher.encrypt(value)
            }
        }
    }

    suspend fun saveSession(token: String, refreshToken: String?, user: UserDto) {
        context.authDataStore.edit { prefs ->
            prefs[tokenKey] = SessionSecretCipher.encrypt(token)
            if (refreshToken.isNullOrBlank()) prefs.remove(refreshTokenKey)
            else prefs[refreshTokenKey] = SessionSecretCipher.encrypt(refreshToken)
            prefs[userKey] = NetworkModule.json.encodeToString(user)
        }
    }

    suspend fun updateTokens(token: String, refreshToken: String?) {
        context.authDataStore.edit { prefs ->
            prefs[tokenKey] = SessionSecretCipher.encrypt(token)
            if (!refreshToken.isNullOrBlank()) prefs[refreshTokenKey] = SessionSecretCipher.encrypt(refreshToken)
        }
    }

    suspend fun updateUser(user: UserDto) {
        context.authDataStore.edit { prefs ->
            prefs[userKey] = NetworkModule.json.encodeToString(user)
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
    }
}
