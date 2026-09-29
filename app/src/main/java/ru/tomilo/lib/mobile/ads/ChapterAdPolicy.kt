package ru.tomilo.lib.mobile.ads

/**
 * Решение, показывать ли межглавный interstitial.
 * До 1.3.5 попытка была при включённом блоке, а не только когда объявление
 * уже лежало в кеше: иначе первый переход сессии и медленная загрузка
 * молча пропускали показ.
 */
internal object ChapterAdPolicy {
    const val COUNTDOWN_SECONDS = 5

    fun countdownTicks(): IntProgression = COUNTDOWN_SECONDS downTo 1

    fun shouldAttempt(
        premium: Boolean,
        cooldownElapsed: Boolean,
        interstitialEnabled: Boolean,
    ): Boolean {
        if (premium || !cooldownElapsed) return false
        return interstitialEnabled
    }
}
