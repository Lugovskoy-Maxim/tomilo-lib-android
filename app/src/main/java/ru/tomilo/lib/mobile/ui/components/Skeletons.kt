package ru.tomilo.lib.mobile.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslation",
    )
    return Brush.linearGradient(
        colors = listOf(
            TomiloSurface2.copy(alpha = 0.55f),
            TomiloSurface2.copy(alpha = 0.85f),
            Color.White.copy(alpha = 0.12f),
            TomiloSurface2.copy(alpha = 0.85f),
            TomiloSurface2.copy(alpha = 0.55f),
        ),
        start = Offset(translateAnim - 250f, translateAnim - 250f),
        end = Offset(translateAnim + 250f, translateAnim + 250f),
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4.4f)
                .clip(RoundedCornerShape(18.dp))
                .background(TomiloSurface2)
                .background(rememberShimmerBrush())
                .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(18.dp)),
        ) {
            // Top badge placeholder (e.g. status badge)
            SkeletonBox(
                Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart)
                    .size(width = 54.dp, height = 18.dp),
                radius = 7.dp,
            )
            // Bottom rating placeholder
            SkeletonBox(
                Modifier
                    .padding(8.dp)
                    .align(Alignment.BottomEnd)
                    .size(width = 38.dp, height = 18.dp),
                radius = 7.dp,
            )
        }
        Spacer(Modifier.height(9.dp))
        // Title lines
        SkeletonBox(Modifier.fillMaxWidth(0.92f).height(13.dp), radius = 6.dp)
        Spacer(Modifier.height(5.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SkeletonBox(Modifier.width(42.dp).height(10.dp), radius = 4.dp)
            SkeletonBox(Modifier.width(36.dp).height(10.dp), radius = 4.dp)
        }
    }
}

@Composable
fun ListCardSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(TomiloSurface)
            .border(1.dp, TomiloBorder, RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBox(Modifier.size(width = 68.dp, height = 96.dp), radius = 14.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).fillMaxWidth()) {
            SkeletonBox(Modifier.fillMaxWidth(0.85f).height(16.dp), radius = 6.dp)
            Spacer(Modifier.height(8.dp))
            SkeletonBox(Modifier.fillMaxWidth(0.5f).height(12.dp), radius = 6.dp)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonBox(Modifier.size(width = 50.dp, height = 18.dp), radius = 6.dp)
                SkeletonBox(Modifier.size(width = 44.dp, height = 18.dp), radius = 6.dp)
            }
        }
    }
}

@Composable
fun CatalogGridSkeleton(modifier: Modifier = Modifier, rows: Int = 3) {
    Column(
        modifier
            .fillMaxSize()
            .padding(start = 14.dp, top = 8.dp, end = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        repeat(rows) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(2) {
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
            Modifier
                .fillMaxWidth()
                .height(460.dp),
            radius = 0.dp,
        )

        Spacer(Modifier.height(14.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(4) {
                SkeletonBox(Modifier.size(width = 72.dp, height = 32.dp), radius = 16.dp)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(3) {
                SkeletonBox(Modifier.width(110.dp).height(34.dp), radius = 10.dp)
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SkeletonBox(Modifier.width(140.dp).height(20.dp), radius = 6.dp)
            SkeletonBox(Modifier.width(50.dp).height(16.dp), radius = 6.dp)
        }

        Spacer(Modifier.height(12.dp))

        Column(
            Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(2) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(2) {
                        PosterSkeleton(Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Premium Promo Banner skeleton
        SkeletonBox(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(130.dp),
            radius = 24.dp,
        )
    }
}

@Composable
fun ListCardsSkeleton(modifier: Modifier = Modifier, count: Int = 6) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(count) { ListCardSkeleton() }
    }
}

@Composable
fun LeaderboardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(bottom = 110.dp),
    ) {
        // Period switch skeleton
        SkeletonBox(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(44.dp),
            radius = 20.dp,
        )

        // Categories row skeleton
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(3) {
                SkeletonBox(Modifier.size(width = 110.dp, height = 34.dp), radius = 16.dp)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Olympic podium skeleton
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            color = TomiloSurface.copy(alpha = 0.65f),
            shape = RoundedCornerShape(26.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                // 2nd
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(54.dp).clip(CircleShape).background(TomiloSurface2).background(rememberShimmerBrush()))
                    Spacer(Modifier.height(6.dp))
                    SkeletonBox(Modifier.width(60.dp).height(12.dp), radius = 6.dp)
                    Spacer(Modifier.height(8.dp))
                    SkeletonBox(Modifier.fillMaxWidth().height(60.dp), radius = 14.dp)
                }
                // 1st
                Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(70.dp).clip(CircleShape).background(TomiloSurface2).background(rememberShimmerBrush()))
                    Spacer(Modifier.height(6.dp))
                    SkeletonBox(Modifier.width(75.dp).height(14.dp), radius = 6.dp)
                    Spacer(Modifier.height(8.dp))
                    SkeletonBox(Modifier.fillMaxWidth().height(88.dp), radius = 14.dp)
                }
                // 3rd
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(50.dp).clip(CircleShape).background(TomiloSurface2).background(rememberShimmerBrush()))
                    Spacer(Modifier.height(6.dp))
                    SkeletonBox(Modifier.width(55.dp).height(12.dp), radius = 6.dp)
                    Spacer(Modifier.height(8.dp))
                    SkeletonBox(Modifier.fillMaxWidth().height(48.dp), radius = 14.dp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Rest list items skeleton
        Column(
            Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(4) {
                SkeletonBox(Modifier.fillMaxWidth().height(58.dp), radius = 18.dp)
            }
        }
    }
}

@Composable
fun ProfileScreenSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
    ) {
        // Profile Hero Header Skeleton
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = TomiloSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, TomiloBorder),
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(78.dp)
                            .clip(CircleShape)
                            .background(TomiloSurface2)
                            .background(rememberShimmerBrush()),
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        SkeletonBox(Modifier.fillMaxWidth(0.6f).height(20.dp), radius = 8.dp)
                        Spacer(Modifier.height(6.dp))
                        SkeletonBox(Modifier.fillMaxWidth(0.4f).height(14.dp), radius = 6.dp)
                        Spacer(Modifier.height(8.dp))
                        SkeletonBox(Modifier.size(width = 80.dp, height = 22.dp), radius = 8.dp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                SkeletonBox(Modifier.fillMaxWidth().height(52.dp), radius = 14.dp)
                Spacer(Modifier.height(12.dp))
                SkeletonBox(Modifier.fillMaxWidth().height(40.dp), radius = 12.dp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Stats grid skeleton (2 rows x 3 cols)
        repeat(2) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(3) {
                    SkeletonBox(Modifier.weight(1f).height(74.dp), radius = 16.dp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Services quick cards
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(3) {
                SkeletonBox(Modifier.weight(1f).height(90.dp), radius = 18.dp)
            }
        }
    }
}
