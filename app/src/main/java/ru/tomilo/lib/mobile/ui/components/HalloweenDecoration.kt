package ru.tomilo.lib.mobile.ui.components

import android.animation.ValueAnimator
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
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
    HalloweenSprite(R.drawable.halloween_pumpkin_glow, 600L, 22.dp, 29.dp, modifier)
}

@Composable
fun HalloweenCornerWeb(modifier: Modifier = Modifier) {
    HalloweenSprite(R.drawable.halloween_web_glint, 600L, 110.dp, 146.dp, modifier)
}

@Composable
fun SeasonalLogoMark(theme: SeasonalTheme, modifier: Modifier = Modifier) {
    when (theme) {
        SeasonalTheme.HALLOWEEN -> HalloweenPumpkin(modifier)
        SeasonalTheme.NEW_YEAR -> Text("✳", modifier = modifier, color = Color(0xFFB8E7FF), fontWeight = FontWeight.Bold)
        SeasonalTheme.SUMMER -> Text("☀", modifier = modifier, color = Color(0xFFFFD166), fontWeight = FontWeight.Bold)
        SeasonalTheme.AUTUMN -> Text("🍂", modifier = modifier)
        SeasonalTheme.NONE -> Unit
    }
}

@Composable
fun SeasonalCornerDecoration(theme: SeasonalTheme, modifier: Modifier = Modifier) {
    if (theme == SeasonalTheme.HALLOWEEN) {
        HalloweenCornerWeb(modifier)
    } else if (theme != SeasonalTheme.NONE) {
        val motionEnabled = ValueAnimator.areAnimatorsEnabled()
        var frame by remember(theme) { mutableIntStateOf(0) }
        LaunchedEffect(theme, motionEnabled) {
            if (!motionEnabled) {
                frame = 0
                return@LaunchedEffect
            }
            while (true) {
                delay(700L)
                frame = (frame + 1) % 4
            }
        }
        Canvas(modifier.size(110.dp, 146.dp)) {
            when (theme) {
                SeasonalTheme.NEW_YEAR -> {
                    val snow = Color(0xFFB8E7FF)
                    val centers = listOf(Offset(23.dp.toPx(), 26.dp.toPx()), Offset(63.dp.toPx(), 50.dp.toPx()), Offset(31.dp.toPx(), 91.dp.toPx()))
                    centers.forEachIndexed { index, center ->
                        val radius = (5 + index).dp.toPx()
                        val pulse = floatArrayOf(0f, 1.5f, 3f, 1.5f)[frame]
                        drawCircle(snow.copy(alpha = 0.20f), radius = radius * 1.8f + pulse.dp.toPx(), center = center)
                        rotate(frame * 5f, center) {
                            repeat(3) { spoke ->
                                val angle = Math.toRadians((spoke * 60).toDouble())
                                val dx = kotlin.math.cos(angle).toFloat() * radius
                                val dy = kotlin.math.sin(angle).toFloat() * radius
                                drawLine(snow.copy(alpha = 0.72f), Offset(center.x - dx, center.y - dy), Offset(center.x + dx, center.y + dy), strokeWidth = 1.5.dp.toPx())
                            }
                        }
                    }
                }
                SeasonalTheme.SUMMER -> {
                    val sun = Color(0xFFFFC857)
                    val center = Offset(25.dp.toPx(), 25.dp.toPx())
                    val pulse = floatArrayOf(0f, 2f, 4f, 2f)[frame]
                    drawCircle(sun.copy(alpha = 0.12f), radius = (29f + pulse).dp.toPx(), center = center)
                    drawCircle(sun.copy(alpha = 0.34f), radius = 16.dp.toPx(), center = center)
                    repeat(8) { ray ->
                        rotate(frame * 1.5f, center) {
                            val angle = Math.toRadians((ray * 45).toDouble())
                            val start = 22.dp.toPx()
                            val end = (30f + pulse).dp.toPx()
                            drawLine(
                                sun.copy(alpha = 0.58f),
                                Offset(center.x + kotlin.math.cos(angle).toFloat() * start, center.y + kotlin.math.sin(angle).toFloat() * start),
                                Offset(center.x + kotlin.math.cos(angle).toFloat() * end, center.y + kotlin.math.sin(angle).toFloat() * end),
                                strokeWidth = 2.dp.toPx(),
                            )
                        }
                    }
                }
                SeasonalTheme.AUTUMN -> {
                    val leaves = listOf(
                        Triple(Offset(21.dp.toPx(), 23.dp.toPx()), 0f, Color(0xFFFF754C)),
                        Triple(Offset(61.dp.toPx(), 43.dp.toPx()), 42f, Color(0xFFFFB347)),
                        Triple(Offset(31.dp.toPx(), 79.dp.toPx()), -28f, Color(0xFFE56B43)),
                        Triple(Offset(75.dp.toPx(), 99.dp.toPx()), 18f, Color(0xFFD9A441)),
                    )
                    val sway = floatArrayOf(-3f, 0f, 3f, 0f)[frame]
                    leaves.forEachIndexed { index, (center, angle, color) ->
                        rotate(angle + if (index % 2 == 0) sway else -sway, center) {
                            drawOval(color.copy(alpha = 0.58f), topLeft = Offset(center.x - 5.dp.toPx(), center.y - 10.dp.toPx()), size = androidx.compose.ui.geometry.Size(10.dp.toPx(), 20.dp.toPx()))
                            drawLine(color.copy(alpha = 0.72f), Offset(center.x, center.y - 7.dp.toPx()), Offset(center.x, center.y + 8.dp.toPx()), strokeWidth = 1.dp.toPx())
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun HalloweenSprite(
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
