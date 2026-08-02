package ru.tomilo.lib.mobile.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.contentDataStore by preferencesDataStore("tomilo_content")

/**
 * 18+ и возрастной гейт первого запуска.
 * ageGateAnswered=false → показать диалог.
 * showAdult=false → скрывать 18+ в каталоге/поиске.
 */
data class ContentSettings(
    val ageGateAnswered: Boolean = false,
    /** null = не отвечал; true = 18+; false = младше 18 */
    val isAdultUser: Boolean? = null,
    val showAdultContent: Boolean = false,
)

class ContentPrefs(private val context: Context) {
    private val answeredKey = booleanPreferencesKey("age_gate_answered")
    private val adultUserKey = intPreferencesKey("is_adult_user") // 0 unknown, 1 yes, 2 no
    private val showAdultKey = booleanPreferencesKey("show_adult")

    val settingsFlow: Flow<ContentSettings> = context.contentDataStore.data.map { p ->
        val adultFlag = p[adultUserKey]
        ContentSettings(
            ageGateAnswered = p[answeredKey] == true,
            isAdultUser = when (adultFlag) {
                1 -> true
                2 -> false
                else -> null
            },
            showAdultContent = p[showAdultKey] == true,
        )
    }

    suspend fun settings(): ContentSettings = settingsFlow.first()

    suspend fun answerAgeGate(isAdult: Boolean) {
        context.contentDataStore.edit { prefs ->
            prefs[answeredKey] = true
            prefs[adultUserKey] = if (isAdult) 1 else 2
            // по умолчанию 18+ выкл; взрослый может включить позже
            prefs[showAdultKey] = false
        }
    }

    suspend fun setShowAdult(show: Boolean) {
        context.contentDataStore.edit { prefs ->
            // младше 18 не даём включить
            val adultFlag = prefs[adultUserKey]
            if (adultFlag == 2 && show) return@edit
            prefs[showAdultKey] = show
        }
    }
}
