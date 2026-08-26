package ru.tomilo.lib.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.api.EquippedDecorationsDto
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import java.util.Locale

private val AvatarGradients = listOf(
    listOf(Color(0xFFE85D55), Color(0xFF9E3D72)),
    listOf(Color(0xFF6C63E8), Color(0xFF354FB4)),
    listOf(Color(0xFF168A8A), Color(0xFF2464A8)),
    listOf(Color(0xFFB86A31), Color(0xFFE04757)),
    listOf(Color(0xFF527B46), Color(0xFF24877C)),
)

internal fun avatarInitials(username: String?): String {
    val value = username.orEmpty().trim()
    if (value.isEmpty()) return "T"
    val words = value
        .split(Regex("\\s+"))
        .map { word -> word.filter(Char::isLetterOrDigit) }
        .filter(String::isNotEmpty)
    val initials = when {
        words.size > 1 -> "${words.first().first()}${words.last().first()}"
        words.isNotEmpty() -> words.first().take(2)
        else -> "T"
    }
    return initials.uppercase(Locale.ROOT)
}

/**
 * Единый аватар tomilo-lib. Декорация avatar заменяет обычную картинку,
 * рамка рисуется поверх и никогда не обрезается родительским кругом.
 */
@Composable
fun DecoratedAvatar(
    avatarUrl: String?,
    username: String?,
    decorations: EquippedDecorationsDto? = null,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    ringColor: Color = TomiloPrimary.copy(alpha = 0.45f),
    avatarDecorationUrl: String? = null,
    frameUrl: String? = null,
    badgeUrl: String? = null,
    online: Boolean = false,
) {
    val avatarModels = listOfNotNull(
        avatarDecorationUrl?.takeIf(String::isNotBlank),
        decorations?.avatarUrl()?.takeIf(String::isNotBlank),
        avatarUrl?.takeIf(String::isNotBlank),
    ).distinct().map(MediaUrl::resolve)
    var avatarModelIndex by remember(avatarModels) { mutableIntStateOf(0) }
    val effectiveAvatar = avatarModels.getOrNull(avatarModelIndex)
    val effectiveFrame = frameUrl ?: decorations?.frameUrl()
    val effectiveBadge = badgeUrl ?: decorations?.badgeUrl()
    val inset = if (effectiveFrame != null) size * 0.105f else 2.dp
    val initials = avatarInitials(username)
    val avatarColors = AvatarGradients[
        (username.orEmpty().lowercase(Locale.ROOT).hashCode() and Int.MAX_VALUE) % AvatarGradients.size
    ]

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(inset)
                .shadow(7.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.28f))
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        avatarColors,
                    ),
                )
                .border(1.5.dp, if (effectiveFrame == null) ringColor else Color.White.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (effectiveAvatar != null) {
                AsyncImage(
                    model = effectiveAvatar,
                    contentDescription = "Аватар ${username.orEmpty()}",
                    contentScale = ContentScale.Crop,
                    onError = {
                        if (avatarModelIndex < avatarModels.lastIndex) {
                            avatarModelIndex += 1
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (!effectiveFrame.isNullOrBlank()) {
            AsyncImage(
                model = MediaUrl.resolve(effectiveFrame),
                contentDescription = "Рамка аватара",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.13f
                        scaleY = 1.13f
                    },
            )
        }

        if (!effectiveBadge.isNullOrBlank()) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.31f)
                    .shadow(5.dp, RoundedCornerShape(7.dp))
                    .clip(RoundedCornerShape(7.dp))
                    .background(TomiloSurface2)
                    .border(1.dp, TomiloPremium.copy(alpha = 0.55f), RoundedCornerShape(7.dp))
                    .padding(2.dp),
            ) {
                AsyncImage(
                    model = MediaUrl.resolve(effectiveBadge),
                    contentDescription = "Значок профиля",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (online) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.23f)
                    .clip(CircleShape)
                    .background(TomiloSurface2)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF55C98C)),
            )
        }
    }
}
