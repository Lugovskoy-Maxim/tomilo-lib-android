package ru.tomilo.lib.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface

/** Полноширинный арт профиля. Лежит позади стеклянной карточки, как ковёр в макете. */
@Composable
fun ProfileDecorationLayer(imageUrl: String?, modifier: Modifier = Modifier) {
    Box(modifier) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = MediaUrl.resolve(imageUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(TomiloPrimary.copy(alpha = 0.55f), TomiloBg),
                        ),
                    ),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.68f to Color.Transparent,
                        1f to TomiloBg,
                    ),
                ),
        )
    }
}

/** Стеклянная карточка поверх арта: сверху просвечивает декорация, снизу текст читается. */
@Composable
fun profileHeaderGlass(): Brush = Brush.linearGradient(
    0f to TomiloPrimary.copy(alpha = 0.18f),
    0.42f to TomiloSurface.copy(alpha = 0.55f),
    1f to TomiloSurface.copy(alpha = 0.82f),
)
