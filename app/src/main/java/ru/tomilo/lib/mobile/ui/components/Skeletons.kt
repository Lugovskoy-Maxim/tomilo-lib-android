package ru.tomilo.lib.mobile.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

@Composable
private fun rememberShimmerBrush(): Brush {
    val shift by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerShift",
    )
    val x = shift * 900f
    return Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.00f),
            Color.White.copy(alpha = 0.07f),
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.07f),
            Color.White.copy(alpha = 0.00f),
        ),
        start = Offset(x - 240f, 0f),
        end = Offset(x + 80f, 220f),
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    radius: Dp = 14.dp,
) {
    val shimmer = rememberShimmerBrush()
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(TomiloSurface2)
            .background(shimmer),
    )
}

@Composable
fun PosterSkeleton(modifier: Modifier = Modifier) {
    Column(modifier) {
        SkeletonBox(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            radius = 16.dp,
        )
        Spacer(Modifier.height(8.dp))
        SkeletonBox(Modifier.fillMaxWidth().height(12.dp), radius = 6.dp)
        Spacer(Modifier.height(6.dp))
        SkeletonBox(Modifier.fillMaxWidth(0.62f).height(10.dp), radius = 6.dp)
    }
}

@Composable
fun ListCardSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(19.dp))
            .background(TomiloSurface)
            .padding(11.dp),
    ) {
        SkeletonBox(Modifier.size(width = 64.dp, height = 92.dp), radius = 12.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).fillMaxWidth()) {
            SkeletonBox(Modifier.fillMaxWidth(0.86f).height(16.dp), radius = 6.dp)
            Spacer(Modifier.height(8.dp))
            SkeletonBox(Modifier.fillMaxWidth(0.48f).height(12.dp), radius = 6.dp)
            Spacer(Modifier.height(12.dp))
            SkeletonBox(Modifier.fillMaxWidth(0.34f).height(10.dp), radius = 6.dp)
        }
    }
}

@Composable
fun CatalogGridSkeleton(modifier: Modifier = Modifier, rows: Int = 3) {
    Column(
        modifier
            .fillMaxSize()
            .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(rows) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(3) {
                    PosterSkeleton(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun HomeFeedSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(bottom = 110.dp),
    ) {
        SkeletonBox(
            Modifier.padding(horizontal = 16.dp).width(140.dp).height(18.dp),
            radius = 6.dp,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(2) {
                Column(Modifier.weight(1f)) {
                    SkeletonBox(
                        Modifier.fillMaxWidth().aspectRatio(3f / 4f),
                        radius = 18.dp,
                    )
                    Spacer(Modifier.height(8.dp))
                    SkeletonBox(Modifier.fillMaxWidth().height(12.dp), radius = 6.dp)
                    Spacer(Modifier.height(6.dp))
                    SkeletonBox(Modifier.fillMaxWidth(0.55f).height(10.dp), radius = 6.dp)
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        SkeletonBox(
            Modifier.padding(horizontal = 16.dp).width(120.dp).height(18.dp),
            radius = 6.dp,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(3) {
                PosterSkeleton(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(22.dp))
        SkeletonBox(
            Modifier.padding(horizontal = 16.dp).width(132.dp).height(18.dp),
            radius = 6.dp,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(3) {
                PosterSkeleton(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ListCardsSkeleton(modifier: Modifier = Modifier, count: Int = 6) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(count) { ListCardSkeleton() }
    }
}
