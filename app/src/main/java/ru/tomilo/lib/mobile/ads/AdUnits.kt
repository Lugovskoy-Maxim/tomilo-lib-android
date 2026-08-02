package ru.tomilo.lib.mobile.ads

import ru.tomilo.lib.mobile.BuildConfig

object AdUnits {
    /** Rewarded РСЯ: R-M-19689456-1 (валюта Reward, сумма 1 → 1 офлайн-кредит). */
    val rewarded: String = BuildConfig.YANDEX_REWARDED_AD_UNIT_ID

    /** Demo unit for local tests — never ship with this in release builds. */
    const val DEMO_REWARDED = "demo-rewarded-yandex"
}
