package ru.tomilo.lib.mobile.core

/**
 * Единый форматтер названий глав для устранения дублирования ("Глава 1 · Глава 1", "Глава 1 · Глава 1: Пролог").
 */
fun compactChapterNumber(raw: String?): String? {
    val n = raw?.trim()?.trim('"')?.takeIf { it.isNotBlank() && it != "null" } ?: return null
    return n.removeSuffix(".0")
}

fun stripTitleFromChapterName(raw: String, titleName: String?): String {
    val title = titleName?.trim().orEmpty()
    var cleaned = raw.trim()
    if (title.isNotBlank()) {
        cleaned = cleaned.replace(title, "", ignoreCase = true)
    }
    return cleaned.trim(' ', '-', '—', '·', ':', '.', '|', '/', '\\')
}

/** Короткий бейдж на карточке: «Гл. 12», без названия тайтла. */
fun chapterBadgeText(
    chapter: String?,
    chapterNumber: String?,
    titleName: String? = null,
    totalChapters: Int? = null,
): String? {
    compactChapterNumber(chapterNumber)?.let { return "Гл. $it" }
    val raw = chapter?.trim()?.takeIf { it.isNotBlank() }
        ?: return totalChapters?.let { "$it гл." }
    val cleaned = stripTitleFromChapterName(raw, titleName)
    if (cleaned.isBlank()) return totalChapters?.let { "$it гл." }
    val numbered = Regex("""^(?:глава|гл\.?)\s*([\d.]+)""", RegexOption.IGNORE_CASE).find(cleaned)
        ?: Regex("""^([\d.]+)\s*$""").find(cleaned)
    if (numbered != null) {
        return "Гл. ${numbered.groupValues[1].removeSuffix(".0")}"
    }
    return if (cleaned.length <= 16) cleaned else totalChapters?.let { "$it гл." }
}

/** Строка обновления: «Новая глава 12», без повтора названия тайтла. */
fun chapterUpdateSubtitle(
    chapter: String?,
    chapterNumber: String?,
    titleName: String? = null,
): String {
    val fromField = compactChapterNumber(chapterNumber)
    val cleaned = chapter?.trim()?.takeIf { it.isNotBlank() }
        ?.let { stripTitleFromChapterName(it, titleName) }
        ?.takeIf { it.isNotBlank() }
    val fromName = cleaned
        ?.let { Regex("""(?:глава|гл\.?)\s*([\d.]+)""", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1) }
        ?.removeSuffix(".0")
    val num = fromField ?: fromName
    val nameOnly = if (num != null && cleaned != null) {
        Regex("""^(?:глава|гл\.?)?\s*${Regex.escape(num)}(?:\.0)?[\s:·—\-]*""", RegexOption.IGNORE_CASE)
            .replace(cleaned, "")
            .trim()
    } else {
        cleaned
    }
    return when {
        num != null -> formatChapterTitle(num, nameOnly).replaceFirst("Глава", "Новая глава")
        !cleaned.isNullOrBlank() -> "Новая глава · $cleaned"
        else -> "Недавно обновлено"
    }
}

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
