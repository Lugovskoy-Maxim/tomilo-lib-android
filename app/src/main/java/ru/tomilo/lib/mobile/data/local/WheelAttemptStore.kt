package ru.tomilo.lib.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.wheelAttemptDataStore by preferencesDataStore("tomilo_wheel_attempts")

data class PendingWheelAttempt(
    val requestId: String,
    val useCoins: Boolean,
)

/** Stores an idempotency key before a wheel request so an uncertain result can be replayed. */
class WheelAttemptStore(private val context: Context) {
    private fun requestIdKey(userId: String) = stringPreferencesKey("pending_request_$userId")
    private fun modeKey(userId: String) = stringPreferencesKey("pending_mode_$userId")

    suspend fun read(userId: String): PendingWheelAttempt? {
        val prefs = context.wheelAttemptDataStore.data.first()
        val requestId = prefs[requestIdKey(userId)] ?: return null
        val mode = prefs[modeKey(userId)] ?: return null
        return PendingWheelAttempt(requestId = requestId, useCoins = mode == "paid")
    }

    suspend fun save(userId: String, attempt: PendingWheelAttempt) {
        context.wheelAttemptDataStore.edit { prefs ->
            prefs[requestIdKey(userId)] = attempt.requestId
            prefs[modeKey(userId)] = if (attempt.useCoins) "paid" else "free"
        }
    }

    suspend fun clear(userId: String, requestId: String) {
        context.wheelAttemptDataStore.edit { prefs ->
            if (prefs[requestIdKey(userId)] == requestId) {
                prefs.remove(requestIdKey(userId))
                prefs.remove(modeKey(userId))
            }
        }
    }
}
