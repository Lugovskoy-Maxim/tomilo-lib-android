package ru.tomilo.lib.mobile.core

/**
 * Единый форматтер названий глав для устранения дублирования ("Глава 1 · Глава 1", "Глава 1 · Глава 1: Пролог").
 */
fun formatChapterTitle(numberLabel: String, rawName: String?): String {
    val cleanNum = numberLabel.trim()
    val numPrefix = if (cleanNum.isNotBlank() && cleanNum != "?") "Глава $cleanNum" else "Глава"
    val trimmedName = rawName?.trim().orEmpty()

    if (trimmedName.isBlank()) {
        return numPrefix
    }

    // Если имя главы это просто то же самое "Глава 1", "глава 1", "1", "1.0", "Глава 1.0"
    if (trimmedName.equals(numPrefix, ignoreCase = true) ||
        trimmedName.equals(cleanNum, ignoreCase = true) ||
        trimmedName.equals("Глава $cleanNum.0", ignoreCase = true)
    ) {
        return numPrefix
    }

    // Если имя главы уже содержит префикс "Глава X" или "Глава X: " или "Глава X - "
    var normalized = trimmedName
    val regexPrefix = Regex("^глава\\s*${Regex.escape(cleanNum)}[\\s:·—\\-]*", RegexOption.IGNORE_CASE)
    if (regexPrefix.containsMatchIn(normalized)) {
        normalized = regexPrefix.replace(normalized, "").trim()
    } else {
        val generalPrefix = Regex("^глава[\\s:·—\\-]+", RegexOption.IGNORE_CASE)
        if (generalPrefix.containsMatchIn(normalized)) {
            normalized = generalPrefix.replace(normalized, "").trim()
        }
    }

    // Убираем ведущие знаки препинания вроде ":", "-", "—", "·", "."
    normalized = normalized.trimStart(':', '-', '—', '·', '.', ' ').trim()

    return if (normalized.isBlank() || normalized.equals(cleanNum, ignoreCase = true)) {
        numPrefix
    } else {
        "$numPrefix · $normalized"
    }
}
