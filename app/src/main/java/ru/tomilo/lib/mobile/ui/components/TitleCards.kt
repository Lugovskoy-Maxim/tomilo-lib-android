package ru.tomilo.lib.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import coil.compose.AsyncImage
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import ru.tomilo.lib.mobile.ui.theme.TomiloText
import java.util.Locale

private val CardRadius = 20.dp
private val CoverShape = RoundedCornerShape(CardRadius)

fun formatRating(value: Double): String =
    String.format(Locale.forLanguageTag("ru"), "%.2f", value)

/** Обложка с автоматическим переключением между S3 и CDN, если один источник недоступен. */
@Composable
fun TomiloCoverImage(
    source: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val candidates = remember(source) { MediaUrl.candidates(source) }
    var attempt by remember(source) { mutableIntStateOf(0) }
    AsyncImage(
        model = candidates.getOrNull(attempt),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        onError = {
            if (attempt < candidates.lastIndex) attempt++
        },
    )
}

@Composable
fun MetaChip(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = Color.Black.copy(alpha = 0.55f),
    content: Color = Color.White,
) {
    Text(
        text = text,
        color = content,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

/**
 * Карточка тайтла для горизонтальных лент (главная) и сетки каталога.
 */
@Composable
fun TitlePosterCard(
    title: String,
    cover: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = 132.dp,
    type: String? = null,
    rating: Double? = null,
    totalChapters: Int? = null,
    chapterBadge: String? = null,
    status: String? = null,
    isAdult: Boolean = false,
    year: Int? = null,
    compact: Boolean = false,
    rank: Int? = null,
    plain: Boolean = false,
    showCoverType: Boolean = true,
    showCoverChapter: Boolean = true,
    showFooterRating: Boolean = true,
    subtitle: String? = null,
    footerTrailing: String? = null,
    footerTrailingAccent: Boolean = false,
) {
    val coverRadius = if (plain) 16.dp else CardRadius
    val coverShape = RoundedCornerShape(coverRadius)
    val base = if (width != null) modifier.width(width) else modifier.fillMaxWidth()
    val chrome = if (plain) {
        base.clickable(onClick = onClick)
    } else {
        base
            .shadow(7.dp, CoverShape, ambientColor = Color.Black.copy(alpha = 0.20f))
            .clip(CoverShape)
            .background(
                Brush.verticalGradient(
                    listOf(TomiloSurface2, TomiloSurface2.copy(alpha = 0.76f)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.075f), CoverShape)
            .clickable(onClick = onClick)
            .padding(bottom = 9.dp)
    }
    Column(modifier = chrome) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(coverShape)
                .background(TomiloSurface2)
                .then(if (plain) Modifier else Modifier.border(1.dp, Color.White.copy(alpha = 0.06f), coverShape)),
        ) {
            TomiloCoverImage(
                source = cover,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // bottom gradient for readability of badges
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                        ),
                    ),
            )
            if (rank != null) {
                // Премиальный бейдж ранга в топе
                val rankBrush = when (rank) {
                    1 -> Brush.linearGradient(listOf(Color(0xFFFFE066), Color(0xFFE5A60D), Color(0xFFB8860B)))
                    2 -> Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD4D8E2), Color(0xFFA6ADBB)))
                    3 -> Brush.linearGradient(listOf(Color(0xFFFFB076), Color(0xFFD97706), Color(0xFF8C3E00)))
                    else -> Brush.linearGradient(listOf(Color.Black.copy(alpha = 0.82f), Color.Black.copy(alpha = 0.92f)))
                }
                val rankTextColor = when (rank) {
                    1 -> Color(0xFF241500)
                    2 -> Color(0xFF141923)
                    3 -> Color(0xFF260F00)
                    else -> Color.White
                }
                val rankBorder = when (rank) {
                    1 -> Color(0xFFFFDF70)
                    2 -> Color(0xFFE2E8F0)
                    3 -> Color(0xFFFFC08A)
                    else -> Color.White.copy(alpha = 0.25f)
                }
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(rankBrush)
                        .border(0.8.dp, rankBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when (rank) {
                        1 -> {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = rankTextColor,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(3.dp))
                        }
                        2, 3 -> {
                            Icon(
                                Icons.Default.MilitaryTech,
                                contentDescription = null,
                                tint = rankTextColor,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(3.dp))
                        }
                    }
                    Text(
                        "#$rank",
                        color = rankTextColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 12.sp,
                    )
                }
            } else if (showCoverType) {
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (!type.isNullOrBlank()) {
                        MetaChip(typeLabel(type), container = TomiloPrimary.copy(alpha = 0.9f))
                    }
                    if (isAdult) {
                        MetaChip("18+", container = Color(0xFFB33A3A).copy(alpha = 0.92f))
                    }
                }
            } else if (isAdult) {
                MetaChip(
                    "18+",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    container = Color(0xFFB33A3A).copy(alpha = 0.92f),
                )
            }
            if (rating != null && rating > 0) {
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.76f))
                        .border(0.8.dp, TomiloPremium.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Рейтинг",
                        tint = TomiloPremium,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        formatRating(rating),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            // Индикатор статуса на карточке (например 'Онгоинг' с зелёной точкой или 'Завершено' с синей)
            if (!status.isNullOrBlank()) {
                val sLabel = statusLabel(status)
                val sColor = statusColor(status)
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.82f))
                        .border(0.8.dp, sColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(sColor),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = sLabel,
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            val bottomEnd = if (showCoverChapter) chapterBadge ?: totalChapters?.let { "$it гл." } else null
            if (!bottomEnd.isNullOrBlank()) {
                MetaChip(
                    bottomEnd,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                    container = Color.Black.copy(alpha = 0.68f),
                )
            }
        }
        Spacer(Modifier.height(if (plain) 8.dp else 8.dp))
        Text(
            text = title,
            style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            minLines = if (compact) 1 else 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = if (compact) 16.sp else 18.sp,
            color = TomiloText,
            modifier = Modifier.padding(horizontal = if (plain) 2.dp else 10.dp),
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TomiloMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = if (plain) 2.dp else 10.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = if (plain) 2.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val typeText = type?.let { typeLabel(it) }
            val leftMeta = when {
                plain -> typeText.orEmpty()
                rank != null -> listOfNotNull(typeText, year?.toString()).joinToString(" · ")
                else -> listOfNotNull(if (showCoverType) null else typeText, year?.toString()).joinToString(" · ")
            }
            Text(
                text = leftMeta.ifBlank { " " },
                style = MaterialTheme.typography.labelSmall,
                color = if (plain && !typeText.isNullOrBlank()) TomiloPrimary else TomiloMuted,
                maxLines = 1,
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = if (plain) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f, fill = false),
            )

            val rightMeta = footerTrailing
                ?: if (chapterBadge.isNullOrBlank() && !plain) {
                    totalChapters?.let { "$it гл." }.orEmpty()
                } else {
                    ""
                }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showFooterRating && rating != null && rating > 0) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = TomiloPremium,
                        modifier = Modifier.size(11.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        formatRating(rating),
                        style = MaterialTheme.typography.labelSmall,
                        color = TomiloPremium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    )
                    if (rightMeta.isNotBlank()) {
                        Text(
                            " · ",
                            style = MaterialTheme.typography.labelSmall,
                            color = TomiloMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
                if (rightMeta.isNotBlank()) {
                    Text(
                        text = rightMeta,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (footerTrailingAccent) TomiloPrimary else TomiloMuted,
                        maxLines = 1,
                        fontWeight = if (plain) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = if (compact) 10.sp else 11.sp,
                    )
                }
            }
        }
    }
}

/**
 * Широкая list-карточка для поиска / истории / закладок.
 */
@Composable
fun TitleSearchCard(
    title: String,
    cover: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: String? = null,
    rating: Double? = null,
    totalChapters: Int? = null,
    year: Int? = null,
    status: String? = null,
    subtitle: String? = null,
    /** Например: «Прочитано 12 / 48 гл. · 25%» */
    progressLine: String? = null,
    isAdult: Boolean = false,
    /** Отдельное действие справа, например переход с чтения на страницу тайтла. */
    secondaryActionIcon: ImageVector? = null,
    secondaryActionDescription: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        color = TomiloSurface2,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 0.dp,
        shadowElevation = 5.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.09f),
        ),
    ) {
        Row(
            Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(TomiloPrimary.copy(alpha = 0.055f), Color.Transparent),
                    ),
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(width = 68.dp, height = 98.dp)
                    .shadow(7.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.26f))
                    .clip(RoundedCornerShape(16.dp))
                    .background(TomiloSurface2)
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
            ) {
                TomiloCoverImage(
                    source = cover,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (isAdult) {
                    MetaChip(
                        "18+",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        container = Color(0xFFB33A3A).copy(alpha = 0.92f),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!type.isNullOrBlank()) {
                        MetaChip(
                            typeLabel(type),
                            container = TomiloPrimary.copy(alpha = 0.22f),
                            content = TomiloPrimary,
                        )
                    }
                    if (!status.isNullOrBlank()) {
                        MetaChip(
                            statusLabel(status),
                            container = statusColor(status).copy(alpha = 0.22f),
                            content = statusColor(status),
                        )
                    }
                    if (year != null) {
                        Text("$year", style = MaterialTheme.typography.labelSmall, color = TomiloMuted)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (rating != null && rating > 0) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = TomiloPremium,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "%.1f".format(rating),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    if (!progressLine.isNullOrBlank()) {
                        Text(
                            progressLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    } else if (totalChapters != null) {
                        Text(
                            "$totalChapters гл.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TomiloMuted,
                        )
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            if (onSecondaryAction != null) {
                IconButton(onClick = onSecondaryAction) {
                    Icon(
                        secondaryActionIcon ?: Icons.Default.ChevronRight,
                        contentDescription = secondaryActionDescription,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(21.dp),
                    )
                }
            } else {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color.White.copy(alpha = 0.055f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TomiloMuted.copy(alpha = 0.82f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** @deprecated use TitlePosterCard */
@Composable
fun TitleCoverCard(
    title: String,
    cover: String?,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TitlePosterCard(
        title = title,
        cover = cover,
        onClick = onClick,
        modifier = modifier,
        chapterBadge = subtitle,
    )
}

/** @deprecated use TitleSearchCard */
@Composable
fun TitleListRow(
    title: String,
    cover: String?,
    meta: String?,
    onClick: () -> Unit,
) {
    TitleSearchCard(
        title = title,
        cover = cover,
        onClick = onClick,
        subtitle = meta,
    )
}

private fun typeLabel(raw: String): String = ru.tomilo.lib.mobile.core.GenreLabels.type(raw)

fun statusLabel(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return when (raw.trim().lowercase()) {
        "ongoing", "publishing", "выходит", "онгоинг" -> "Онгоинг"
        "completed", "finished", "complete", "завершено", "завершён" -> "Завершено"
        "pause", "hiatus", "paused", "пауза" -> "Пауза"
        "cancelled", "canceled", "dropped", "отменён", "отменен" -> "Отменён"
        "announced", "анонс" -> "Анонс"
        else -> ru.tomilo.lib.mobile.core.GenreLabels.status(raw).let {
            when (it.lowercase()) {
                "выходит" -> "Онгоинг"
                "завершён", "завершено" -> "Завершено"
                else -> it.ifBlank { raw }
            }
        }
    }
}

fun statusColor(raw: String?): Color {
    if (raw.isNullOrBlank()) return Color(0xFF8E8E93)
    return when (raw.trim().lowercase()) {
        "ongoing", "publishing", "выходит", "онгоинг" -> Color(0xFF34C759) // Vibrant Green
        "completed", "finished", "complete", "завершено", "завершён" -> Color(0xFF3897F0) // Vibrant Blue
        "pause", "hiatus", "paused", "пауза" -> Color(0xFFFF9500) // Amber
        "cancelled", "canceled", "dropped", "отменён", "отменен" -> Color(0xFFFF3B30) // Red
        "announced", "анонс" -> Color(0xFFAF52DE) // Purple
        else -> Color(0xFF8E8E93)
    }
}
