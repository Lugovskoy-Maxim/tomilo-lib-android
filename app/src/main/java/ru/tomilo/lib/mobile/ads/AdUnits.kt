package ru.tomilo.lib.mobile.ads

import ru.tomilo.lib.mobile.BuildConfig

object AdUnits {
    /**
     * Rewarded РСЯ. Оба блока активны в кабинете и грузятся отдельно:
     * R-M-19689456-1 (02-08-2026) и R-M-19689456-3 (31-08-2026).
     * Валюта Reward, сумма 1 → 1 офлайн-кредит.
     */
    val rewardedUnits: List<String> = listOf(
        BuildConfig.YANDEX_REWARDED_AD_UNIT_ID,
        BuildConfig.YANDEX_REWARDED_AD_UNIT_ID_2,
    ).map { it.trim() }.filter { it.isNotBlank() }.distinct()

    /** Первый rewarded-блок. Показ выбирает любой уже загруженный из [rewardedUnits]. */
    val rewarded: String = rewardedUnits.firstOrNull().orEmpty()

    /** Interstitial между главами: R-M-19689456-2 */
    val interstitial: String = BuildConfig.YANDEX_INTERSTITIAL_AD_UNIT_ID.trim()

    /** Нативное объявление в каталоге: R-M-19689456-4 */
    const val nativeCatalog: String = "R-M-19689456-4"

    const val DEMO_REWARDED = "demo-rewarded-yandex"
    const val DEMO_INTERSTITIAL = "demo-interstitial-yandex"
}
