package ru.tomilo.lib.mobile.ads

import ru.tomilo.lib.mobile.BuildConfig

object AdUnits {
    /** Rewarded РСЯ: R-M-19689456-1 (валюта Reward, сумма 1 → 1 офлайн-кредит). */
    val rewarded: String = BuildConfig.YANDEX_REWARDED_AD_UNIT_ID

    /**
     * Interstitial между главами.
     * Создайте блок «Межстраничная» в РСЯ и пропишите ID в build.gradle
     * (YANDEX_INTERSTITIAL_AD_UNIT_ID). Пока пусто — в debug demo, в release
     * fallback на rewarded между главами.
     */
    val interstitial: String
        get() {
            val configured = BuildConfig.YANDEX_INTERSTITIAL_AD_UNIT_ID.trim()
            if (configured.isNotEmpty()) return configured
            return if (BuildConfig.DEBUG) DEMO_INTERSTITIAL else ""
        }

    const val DEMO_REWARDED = "demo-rewarded-yandex"
    const val DEMO_INTERSTITIAL = "demo-interstitial-yandex"
}
