package ru.tomilo.lib.mobile.ui.components

import android.animation.ValueAnimator
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.tomilo.lib.mobile.R
import ru.tomilo.lib.mobile.data.local.SeasonalTheme
import kotlin.math.roundToInt

@Composable
fun HalloweenPumpkin(modifier: Modifier = Modifier) {
    SeasonalSprite(R.drawable.halloween_pumpkin_glow, 600L, 22.dp, 24.dp, modifier)
}

@Composable
fun HalloweenCornerWeb(modifier: Modifier = Modifier) {
    SeasonalSprite(R.drawable.halloween_web_glint, 600L, 110.dp, 146.dp, modifier)
}

@Composable
fun SeasonalLogoMark(theme: SeasonalTheme, modifier: Modifier = Modifier) {
    when (theme) {
        SeasonalTheme.HALLOWEEN -> HalloweenPumpkin(modifier)
        SeasonalTheme.NEW_YEAR -> SeasonalSprite(R.drawable.seasonal_new_year_snowflake_strip, 640L, 22.dp, 29.dp, modifier)
        SeasonalTheme.SUMMER -> SeasonalSprite(R.drawable.seasonal_summer_firefly_strip, 520L, 22.dp, 29.dp, modifier)
        SeasonalTheme.AUTUMN -> SeasonalSprite(R.drawable.seasonal_autumn_leaf_strip, 640L, 22.dp, 29.dp, modifier)
        SeasonalTheme.NONE -> Unit
    }
}

@Composable
fun SeasonalCornerDecoration(theme: SeasonalTheme, modifier: Modifier = Modifier) {
    when (theme) {
        SeasonalTheme.HALLOWEEN -> HalloweenCornerWeb(modifier)
        SeasonalTheme.NONE -> Unit
        else -> SeasonalParticleDecoration(theme, modifier)
    }
}

@Composable
private fun SeasonalParticleDecoration(theme: SeasonalTheme, modifier: Modifier) {
    val (resourceId, frameDelay, placements) = when (theme) {
        SeasonalTheme.NEW_YEAR -> Triple(
            R.drawable.seasonal_new_year_snowflake_strip,
            640L,
            listOf(
                SpritePlacement(22f, 26f, 30f, 40f, -8f, 0.78f),
                SpritePlacement(69f, 50f, 26f, 35f, 14f, 0.56f),
                SpritePlacement(36f, 101f, 29f, 39f, 6f, 0.66f),
            ),
        )
        SeasonalTheme.SUMMER -> Triple(
            R.drawable.seasonal_summer_firefly_strip,
            520L,
            listOf(
                SpritePlacement(24f, 27f, 28f, 38f, -10f, 0.88f),
                SpritePlacement(72f, 49f, 25f, 34f, 12f, 0.72f),
                SpritePlacement(37f, 91f, 26f, 35f, -3f, 0.78f),
                SpritePlacement(79f, 116f, 22f, 30f, 18f, 0.62f),
            ),
        )
        SeasonalTheme.AUTUMN -> Triple(
            R.drawable.seasonal_autumn_leaf_strip,
            640L,
            listOf(
                SpritePlacement(20f, 24f, 27f, 36f, -24f, 0.80f),
                SpritePlacement(65f, 42f, 25f, 33f, 32f, 0.72f),
                SpritePlacement(32f, 85f, 29f, 39f, -8f, 0.76f),
                SpritePlacement(76f, 111f, 25f, 34f, 20f, 0.64f),
            ),
        )
        else -> return
    }
    val strip = ImageBitmap.imageResource(resourceId)
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    var frame by remember(theme) { mutableIntStateOf(0) }
    LaunchedEffect(theme, motionEnabled) {
        if (!motionEnabled) {
            frame = 0
            return@LaunchedEffect
        }
        while (true) {
            delay(frameDelay)
            frame = (frame + 1) % 4
        }
    }

    Canvas(modifier.size(110.dp, 146.dp)) {
        val sourceWidth = strip.width / 4
        placements.forEachIndexed { index, placement ->
            val center = Offset(placement.x.dp.toPx(), placement.y.dp.toPx())
            val widthPx = placement.width.dp.toPx()
            val heightPx = placement.height.dp.toPx()
            val sway = if (motionEnabled) floatArrayOf(-4f, 0f, 4f, 0f)[(frame + index) % 4] else 0f
            rotate(placement.rotation + sway, center) {
                drawImage(
                    image = strip,
                    srcOffset = IntOffset(((frame + index) % 4) * sourceWidth, 0),
                    srcSize = IntSize(sourceWidth, strip.height),
                    dstOffset = IntOffset((center.x - widthPx / 2f).roundToInt(), (center.y - heightPx / 2f).roundToInt()),
                    dstSize = IntSize(widthPx.roundToInt(), heightPx.roundToInt()),
                    alpha = placement.alpha,
                )
            }
        }
    }
}

private data class SpritePlacement(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val alpha: Float,
)

@Composable
private fun SeasonalSprite(
    @DrawableRes resourceId: Int,
    frameDelayMillis: Long,
    width: Dp,
    height: Dp,
    modifier: Modifier,
) {
    val strip = ImageBitmap.imageResource(resourceId)
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    var frame by remember(resourceId) { mutableIntStateOf(0) }

    LaunchedEffect(resourceId, motionEnabled) {
        if (!motionEnabled) {
            frame = 0
            return@LaunchedEffect
        }
        while (true) {
            delay(frameDelayMillis)
            frame = (frame + 1) % 4
        }
    }

    Canvas(modifier.size(width, height)) {
        drawImage(
            image = strip,
            srcOffset = IntOffset(frame * strip.width / 4, 0),
            srcSize = IntSize(strip.width / 4, strip.height),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        )
    }
}
