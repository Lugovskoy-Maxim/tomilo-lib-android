package ru.tomilo.lib.mobile.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Согласие пользователя на рекламу.
 * UNKNOWN — выбор ещё не сделан, рекламный SDK не инициализируется.
 * DENIED — реклама полностью отключена, SDK не инициализируется.
 * CONTEXTUAL — только неперсонализированная контекстная реклама.
 * GRANTED — персонализированная реклама.
 */
enum class AdsConsent {
    UNKNOWN,
    DENIED,
    CONTEXTUAL,
    GRANTED,
    ;

    /** Можно запрашивать и показывать рекламу (контекстную или персонализированную). */
    val allowsAds: Boolean get() = this == CONTEXTUAL || this == GRANTED

    /** Разрешена персонализация рекламных объявлений. */
    val personalized: Boolean get() = this == GRANTED

    companion object {
        /**
         * Чистое разбор сохранённого значения (для unit-тестов без Android-контекста).
         * Legacy-значение "granted" трактуем как персонализированное согласие.
         */
        fun parse(raw: String?): AdsConsent = when (raw) {
            "denied" -> DENIED
            "contextual" -> CONTEXTUAL
            "granted", "personalized" -> GRANTED
            else -> UNKNOWN
        }
    }
}

class AdsConsentStore(context: Context) {
    private val prefs = context.getSharedPreferences("ads_consent", Context.MODE_PRIVATE)
    private val _consent = MutableStateFlow(AdsConsent.parse(prefs.getString(KEY, null)))
    val consent: StateFlow<AdsConsent> = _consent.asStateFlow()

    fun set(consent: AdsConsent) {
        _consent.value = consent
        prefs.edit().putString(KEY, consent.name.lowercase()).apply()
    }

    companion object {
        private const val KEY = "consent"
    }
}
