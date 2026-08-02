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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

private val CardRadius = 14.dp
private val CoverShape = RoundedCornerShape(CardRadius)

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
            .clip(RoundedCornerShape(6.dp))
            .background(container)
            .padding(horizontal = 6.dp, vertical = 2.dp),
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
) {
    val base = if (width != null) modifier.width(width) else modifier.fillMaxWidth()
    Column(
        modifier = base
            .clip(RoundedCornerShape(CardRadius))
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(CoverShape)
                .background(TomiloSurface2)
                .border(1.dp, Color.White.copy(alpha = 0.06f), CoverShape),
        ) {
            AsyncImage(
                model = MediaUrl.resolve(cover),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // bottom gradient for readability of badges
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                        ),
                    ),
            )
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
            if (rating != null && rating > 0) {
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = TomiloPremium,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "%.1f".format(rating),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            val bottomLeft = chapterBadge ?: totalChapters?.let { "$it гл." }
            if (!bottomLeft.isNullOrBlank()) {
                MetaChip(
                    bottomLeft,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                    container = Color.Black.copy(alpha = 0.65f),
                )
            }
            if (!status.isNullOrBlank()) {
                MetaChip(
                    statusLabel(status),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                    container = statusColor(status).copy(alpha = 0.88f),
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        val footer = listOfNotNull(
            year?.toString(),
            if (chapterBadge == null) totalChapters?.let { "$it гл." } else null,
        ).joinToString(" · ")
        if (footer.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                footer,
                style = MaterialTheme.typography.labelSmall,
                color = TomiloMuted,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
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
    isAdult: Boolean = false,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = TomiloSurface2.copy(alpha = 0.65f),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp,
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(width = 64.dp, height = 90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TomiloSurface2)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp)),
            ) {
                AsyncImage(
                    model = MediaUrl.resolve(cover),
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
                    if (totalChapters != null) {
                        Text(
                            "$totalChapters гл.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TomiloMuted,
                        )
                    }
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

private fun typeLabel(raw: String): String = when (raw.lowercase()) {
    "manga", "манга" -> "Манга"
    "manhwa" -> "Манхва"
    "manhua" -> "Маньхуа"
    "comic" -> "Комикс"
    else -> raw.replaceFirstChar { it.uppercase() }
}

private fun statusLabel(raw: String): String = when (raw.lowercase()) {
    "ongoing" -> "Онгоинг"
    "completed" -> "Завершён"
    "pause" -> "Пауза"
    "cancelled" -> "Отменён"
    else -> raw
}

private fun statusColor(raw: String): Color = when (raw.lowercase()) {
    "ongoing" -> Color(0xFF3D9A6A)
    "completed" -> Color(0xFF4A7FD4)
    "pause" -> Color(0xFFC49A3C)
    "cancelled" -> Color(0xFF9A4A4A)
    else -> Color.Gray
}
