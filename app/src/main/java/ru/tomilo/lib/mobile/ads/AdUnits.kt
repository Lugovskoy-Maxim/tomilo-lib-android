package ru.tomilo.lib.mobile.ads

import ru.tomilo.lib.mobile.BuildConfig

object AdUnits {
    /** Rewarded РСЯ: R-M-19689456-1 (валюта Reward, сумма 1 → 1 офлайн-кредит). */
    val rewarded: String = BuildConfig.YANDEX_REWARDED_AD_UNIT_ID

    /** Interstitial между главами: R-M-19689456-2 */
    val interstitial: String = BuildConfig.YANDEX_INTERSTITIAL_AD_UNIT_ID.trim()

    const val DEMO_REWARDED = "demo-rewarded-yandex"
    const val DEMO_INTERSTITIAL = "demo-interstitial-yandex"
}
