package ru.tomilo.lib.mobile.core

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ChatTime {
    private val ru = Locale.forLanguageTag("ru")
    private val thisYear = DateTimeFormatter.ofPattern("d MMM, HH:mm", ru)
    private val full = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm")
    private val clock = DateTimeFormatter.ofPattern("HH:mm")
    private val localNaive = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    fun label(raw: String?, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): String? {
        val instant = parseInstant(raw) ?: return null
        val dt = instant.atZone(zone)
        val today = now.atZone(zone).toLocalDate()
        val day = dt.toLocalDate()
        val time = dt.format(clock)
        return when {
            day == today -> time
            day == today.minusDays(1) -> "вчера $time"
            day.year == today.year -> dt.format(thisYear)
            else -> dt.format(full)
        }
    }

    fun parseInstant(raw: String?): Instant? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        value.toLongOrNull()?.let { n ->
            return if (n < 1_000_000_000_000L) Instant.ofEpochSecond(n) else Instant.ofEpochMilli(n)
        }
        runCatching { Instant.parse(value) }.getOrNull()?.let { return it }
        val iso = value.replace(' ', 'T')
        val withZone = if (iso.endsWith("Z") || iso.contains('+') || iso.indexOf('-', 10) >= 0) {
            iso
        } else {
            iso + "Z"
        }
        runCatching { Instant.parse(withZone) }.getOrNull()?.let { return it }
        val naive = iso.take(19)
        return runCatching {
            LocalDateTime.parse(naive, localNaive).toInstant(ZoneOffset.UTC)
        }.getOrNull()
    }

    fun nowIso(): String = ZonedDateTime.now(ZoneOffset.UTC).toInstant().toString()
}
