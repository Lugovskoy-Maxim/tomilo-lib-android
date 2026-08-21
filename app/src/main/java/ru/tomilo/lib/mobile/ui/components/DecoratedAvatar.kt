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
    val effectiveAvatar = avatarDecorationUrl ?: decorations?.avatarUrl() ?: avatarUrl
    val effectiveFrame = frameUrl ?: decorations?.frameUrl()
    val effectiveBadge = badgeUrl ?: decorations?.badgeUrl()
    val inset = if (effectiveFrame != null) size * 0.105f else 2.dp
    val initial = username?.trim()?.firstOrNull()?.uppercase() ?: "T"

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
                        listOf(TomiloPrimary.copy(alpha = 0.42f), TomiloSurface2),
                    ),
                )
                .border(1.5.dp, if (effectiveFrame == null) ringColor else Color.White.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initial,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            AsyncImage(
                model = MediaUrl.resolve(effectiveAvatar),
                contentDescription = "Аватар ${username.orEmpty()}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
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
