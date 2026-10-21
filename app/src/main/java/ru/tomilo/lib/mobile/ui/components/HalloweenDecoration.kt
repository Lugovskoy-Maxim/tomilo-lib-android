package ru.tomilo.lib.mobile.ui.components

import android.animation.ValueAnimator
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
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
    SeasonalSprite(R.drawable.halloween_pumpkin_glow, 600L, 22.dp, 22.dp, modifier)
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
        SeasonalTheme.HALLOWEEN -> HalloweenAtmosphere(modifier)
        SeasonalTheme.NONE -> Unit
        else -> SeasonalParticleDecoration(theme, modifier)
    }
}

@Composable
private fun HalloweenAtmosphere(modifier: Modifier) {
    val webStrip = ImageBitmap.imageResource(R.drawable.halloween_web_glint)
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(motionEnabled) {
        if (!motionEnabled) {
            frame = 0
            return@LaunchedEffect
        }
        while (true) {
            delay(560L)
            frame = (frame + 1) % 4
        }
    }

    Canvas(modifier.fillMaxSize()) {
        val frameWidth = webStrip.width / 4
        val webPlacements = listOf(
            SpritePlacement(0f, 0f, 98f, 130f, 0f, 0.96f),
            SpritePlacement(size.width / density - 68f, 0f, 68f, 91f, 0f, 0.76f),
            SpritePlacement(size.width / density - 45f, 0f, 45f, 60f, 0f, 0.60f),
        )
        webPlacements.forEachIndexed { index, placement ->
            val left = placement.x.dp.toPx()
            val top = placement.y.dp.toPx()
            val width = placement.width.dp.toPx()
            val height = placement.height.dp.toPx()
            val drawWeb: DrawScope.() -> Unit = {
                drawImage(
                    image = webStrip,
                    srcOffset = IntOffset(((frame + index) % 4) * frameWidth, 0),
                    srcSize = IntSize(frameWidth, webStrip.height),
                    dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                    dstSize = IntSize(width.roundToInt(), height.roundToInt()),
                    alpha = placement.alpha,
                )
            }
            if (index > 0) {
                scale(scaleX = -1f, scaleY = 1f, pivot = Offset(left + width / 2f, top + height / 2f), block = drawWeb)
            } else drawWeb()
        }

        val batColor = Color(0xFF241A2A).copy(alpha = 0.92f)
        val widthPx = 28.dp.toPx()
        val heightPx = 19.dp.toPx()
        val batPositions = listOf(
            Offset(size.width * 0.64f, 38.dp.toPx()),
            Offset(size.width * 0.77f, 61.dp.toPx()),
            Offset(size.width * 0.88f, 30.dp.toPx()),
        )
        batPositions.forEachIndexed { index, center ->
            val drift = if (motionEnabled) floatArrayOf(-5f, 0f, 5f, 0f)[(frame + index) % 4] else 0f
            val wingScale = if (motionEnabled) floatArrayOf(0.78f, 1f, 0.78f, 0.56f)[(frame + index) % 4] else 0.82f
            drawBat(center + Offset(drift, 0f), widthPx, heightPx, wingScale, batColor)
        }
    }
}

private fun DrawScope.drawBat(center: Offset, width: Float, height: Float, wingScale: Float, color: Color) {
    val left = center.x - width / 2f
    val top = center.y - height / 2f
    val path = Path().apply {
        moveTo(center.x, top + height * 0.27f)
        cubicTo(left + width * 0.38f, top - height * 0.08f, left + width * 0.22f, top + height * 0.05f, left, top + height * 0.02f)
        cubicTo(left + width * 0.04f, top + height * 0.48f, left + width * 0.22f, top + height * 0.72f, left + width * 0.39f, top + height * 0.58f)
        lineTo(center.x, top + height * 0.93f)
        lineTo(left + width * 0.61f, top + height * 0.58f)
        cubicTo(left + width * 0.78f, top + height * 0.72f, left + width * 0.96f, top + height * 0.48f, left + width, top + height * 0.02f)
        cubicTo(left + width * 0.78f, top + height * 0.05f, left + width * 0.62f, top - height * 0.08f, center.x, top + height * 0.27f)
        close()
    }
    scale(scaleX = 1f, scaleY = wingScale, pivot = center) { drawPath(path, color) }
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
