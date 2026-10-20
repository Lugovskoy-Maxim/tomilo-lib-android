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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.tomilo.lib.mobile.R
import java.util.Calendar
import kotlin.math.roundToInt

fun isHalloweenSeason(): Boolean = Calendar.getInstance().get(Calendar.MONTH) == Calendar.OCTOBER

@Composable
fun HalloweenPumpkin(modifier: Modifier = Modifier) {
    HalloweenSprite(R.drawable.halloween_pumpkin_glow, 600L, 22.dp, 29.dp, modifier)
}

@Composable
fun HalloweenCornerWeb(modifier: Modifier = Modifier) {
    HalloweenSprite(R.drawable.halloween_web_glint, 600L, 110.dp, 146.dp, modifier)
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
