package ru.tomilo.lib.mobile.ui.components

import android.animation.ValueAnimator
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

private val LocalShimmerShift = compositionLocalOf<Float?> { null }

private fun Modifier.loadingSemantics(label: String): Modifier = semantics {
    contentDescription = label
    stateDescription = "Загрузка"
    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
}

@Composable
private fun ShimmerScope(content: @Composable () -> Unit) {
    val screenStarted = rememberScreenStarted()
    if (!ValueAnimator.areAnimatorsEnabled() || !screenStarted) {
        CompositionLocalProvider(LocalShimmerShift provides null, content = content)
        return
    }
    val transition = rememberInfiniteTransition(label = "skeletonShimmer")
    val shift by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeletonShimmerShift",
    )
    CompositionLocalProvider(LocalShimmerShift provides shift, content = content)
}

@Composable
fun rememberShimmerBrush(): Brush {
    val translateAnim = LocalShimmerShift.current
        ?: return Brush.linearGradient(listOf(TomiloSurface2, TomiloSurface2))
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

/** Loading placeholders match the card album's catalog tiles and filters. */
@Composable
fun CardsGridSkeleton(modifier: Modifier = Modifier) {
    ShimmerScope {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(146.dp),
            modifier = modifier.loadingSemantics("Загрузка альбома карт"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBox(Modifier.fillMaxWidth(.58f).height(13.dp), radius = 5.dp)
                    SkeletonBox(Modifier.fillMaxWidth().height(56.dp), radius = 16.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { SkeletonBox(Modifier.width(94.dp).height(40.dp), radius = 20.dp) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(4) { SkeletonBox(Modifier.width(48.dp).height(40.dp), radius = 20.dp) }
                    }
                }
            }
            items((1..9).toList()) {
                Column(
                    Modifier.clip(RoundedCornerShape(15.dp)).background(TomiloSurface).padding(7.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    SkeletonBox(Modifier.fillMaxWidth().height(164.dp), radius = 10.dp)
                    SkeletonBox(Modifier.fillMaxWidth(.86f).height(14.dp), radius = 5.dp)
                    SkeletonBox(Modifier.fillMaxWidth(.58f).height(12.dp), radius = 5.dp)
                    SkeletonBox(Modifier.fillMaxWidth().height(36.dp), radius = 18.dp)
                    SkeletonBox(Modifier.fillMaxWidth(.72f).height(32.dp), radius = 16.dp)
                }
            }
        }
    }
}

/** Loading placeholders follow the forge's selectable, one-card-per-row collection. */
@Composable
fun CardCollectionSkeleton(modifier: Modifier = Modifier) {
    ShimmerScope {
        Column(
            modifier.fillMaxSize().loadingSemantics("Загрузка коллекции карт")
                .verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SkeletonBox(Modifier.fillMaxWidth(.24f).height(18.dp), radius = 6.dp)
            SkeletonBox(Modifier.fillMaxWidth().height(40.dp), radius = 6.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBox(Modifier.width(126.dp).height(40.dp), radius = 20.dp)
                SkeletonBox(Modifier.width(112.dp).height(40.dp), radius = 20.dp)
            }
            SkeletonBox(Modifier.fillMaxWidth(.64f).height(20.dp), radius = 6.dp)
            SkeletonBox(Modifier.fillMaxWidth().height(48.dp), radius = 22.dp)
            repeat(6) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(TomiloSurface).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SkeletonBox(Modifier.size(width = 52.dp, height = 68.dp), radius = 9.dp)
                    SkeletonBox(Modifier.size(24.dp), radius = 4.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        SkeletonBox(Modifier.fillMaxWidth(.84f).height(15.dp), radius = 5.dp)
                        SkeletonBox(Modifier.fillMaxWidth(.64f).height(12.dp), radius = 5.dp)
                        SkeletonBox(Modifier.fillMaxWidth(.76f).height(30.dp), radius = 16.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun CardShopSkeleton(modifier: Modifier = Modifier, singleColumn: Boolean = false) {
    val fontScale = LocalConfiguration.current.fontScale.coerceIn(1f, 1.5f)
    val descriptionLineCount = kotlin.math.ceil(fontScale * 4).toInt().coerceIn(4, 6)
    val descriptionLineWidths = listOf(.98f, .91f, .96f, .82f, .73f, .58f)
    ShimmerScope {
        LazyVerticalGrid(
            columns = if (singleColumn) GridCells.Fixed(1) else GridCells.Adaptive(148.dp),
            modifier = modifier.loadingSemantics("Загрузка магазина карт"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SkeletonBox(Modifier.fillMaxWidth(.55f).height(18.dp), radius = 6.dp)
                    SkeletonBox(Modifier.fillMaxWidth(.82f).height(13.dp), radius = 5.dp)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(TomiloSurface).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SkeletonBox(Modifier.fillMaxWidth(.42f).height(15.dp), radius = 5.dp)
                    repeat(descriptionLineCount) { line ->
                        SkeletonBox(
                            Modifier.fillMaxWidth(descriptionLineWidths[line]).height(16.dp * fontScale),
                            radius = 5.dp,
                        )
                    }
                    SkeletonBox(Modifier.fillMaxWidth().height(40.dp), radius = 20.dp)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SkeletonBox(Modifier.fillMaxWidth().height(56.dp), radius = 16.dp)
            }
            items((1..6).toList()) {
                Column(
                    Modifier.clip(RoundedCornerShape(18.dp)).background(TomiloSurface).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SkeletonBox(Modifier.fillMaxWidth().height(154.dp), radius = 12.dp)
                    SkeletonBox(Modifier.fillMaxWidth(.85f).height(14.dp), radius = 5.dp)
                    SkeletonBox(Modifier.fillMaxWidth(.56f).height(14.dp), radius = 5.dp)
                    SkeletonBox(Modifier.fillMaxWidth(.6f).height(12.dp), radius = 5.dp)
                    SkeletonBox(Modifier.fillMaxWidth().height(40.dp), radius = 20.dp)
                    SkeletonBox(Modifier.fillMaxWidth().height(40.dp), radius = 20.dp)
                }
            }
        }
    }
}

@Composable
fun CardTradesSkeleton(modifier: Modifier = Modifier, hasBookmarkFilter: Boolean = false) {
    ShimmerScope {
        Column(
            modifier.fillMaxSize().loadingSemantics("Загрузка предложений обмена").padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SkeletonBox(Modifier.fillMaxWidth(.38f).height(18.dp), radius = 6.dp)
                SkeletonBox(Modifier.size(width = 142.dp, height = 40.dp), radius = 20.dp)
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasBookmarkFilter) {
                    SkeletonBox(Modifier.width(128.dp).height(40.dp), radius = 20.dp)
                } else {
                    SkeletonBox(Modifier.fillMaxWidth(.72f).height(14.dp), radius = 5.dp)
                }
                SkeletonBox(Modifier.fillMaxWidth().height(56.dp), radius = 14.dp)
            }
            repeat(3) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(TomiloSurface).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SkeletonBox(Modifier.fillMaxWidth(.4f).height(15.dp), radius = 5.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(2) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                SkeletonBox(Modifier.fillMaxWidth().height(128.dp), radius = 12.dp)
                                SkeletonBox(Modifier.fillMaxWidth(.85f).height(13.dp), radius = 5.dp)
                                SkeletonBox(Modifier.fillMaxWidth(.55f).height(11.dp), radius = 5.dp)
                            }
                        }
                    }
                    SkeletonBox(Modifier.fillMaxWidth().height(40.dp), radius = 20.dp)
                }
            }
        }
    }
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
fun CatalogGridSkeleton(
    modifier: Modifier = Modifier,
    rows: Int = 3,
    columns: Int = 2,
    list: Boolean = false,
) {
    ShimmerScope {
        Column(
            modifier
                .fillMaxSize()
                .loadingSemantics("Загрузка каталога произведений")
                .padding(start = 14.dp, top = 8.dp, end = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (list) {
                repeat(rows) {
                    ListCardSkeleton()
                }
            } else {
                repeat(rows) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        repeat(columns.coerceIn(2, 3)) {
                            PosterSkeleton(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeFeedSkeleton(modifier: Modifier = Modifier) {
    val heroHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.62f).coerceIn(440.dp, 580.dp)
    ShimmerScope {
        Column(
            modifier
                .fillMaxSize()
                .loadingSemantics("Загрузка ленты")
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .background(TomiloSurface2)
                    .background(rememberShimmerBrush()),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SkeletonBox(Modifier.width(94.dp).height(34.dp), radius = 20.dp)
                        Spacer(Modifier.weight(1f))
                        SkeletonBox(Modifier.width(84.dp).height(36.dp), radius = 20.dp)
                    }

                    Spacer(Modifier.height(18.dp))
                    SkeletonBox(Modifier.width(136.dp).height(28.dp), radius = 20.dp)
                    Spacer(Modifier.weight(1f))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SkeletonBox(Modifier.weight(1f).height(44.dp), radius = 22.dp)
                        SkeletonBox(Modifier.weight(1f).height(44.dp), radius = 22.dp)
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SkeletonBox(Modifier.fillMaxWidth(0.68f).height(22.dp), radius = 6.dp)
                            SkeletonBox(Modifier.width(112.dp).height(14.dp), radius = 5.dp)
                        }
                        SkeletonBox(Modifier.width(92.dp).height(28.dp), radius = 16.dp)
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        repeat(5) { index ->
                            SkeletonBox(
                                Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(width = if (index == 0) 18.dp else 6.dp, height = 5.dp),
                                radius = 3.dp,
                            )
                        }
                    }
                }
            }

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
}

/** Detail-page loading shape follows the title hero, page tabs, and about content. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun TitleDetailSkeleton(modifier: Modifier = Modifier) {
    ShimmerScope {
        Column(
            modifier
                .fillMaxSize()
                .loadingSemantics("Загрузка информации о произведении")
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(TomiloSurface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBox(Modifier.size(width = 58.dp, height = 34.dp), radius = 22.dp)
                    Spacer(Modifier.weight(1f))
                    SkeletonBox(Modifier.size(width = 84.dp, height = 34.dp), radius = 22.dp)
                }
                Spacer(Modifier.height(6.dp))
                SkeletonBox(
                    Modifier.align(Alignment.CenterHorizontally).size(width = 176.dp, height = 258.dp),
                    radius = 12.dp,
                )
                Spacer(Modifier.height(12.dp))
                SkeletonBox(Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.82f).height(22.dp), radius = 7.dp)
                Spacer(Modifier.height(5.dp))
                SkeletonBox(Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(0.52f).height(16.dp), radius = 6.dp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SkeletonBox(Modifier.weight(1f).height(48.dp), radius = 19.dp)
                    SkeletonBox(Modifier.weight(1f).height(48.dp), radius = 19.dp)
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(44.dp, 64.dp, 48.dp, 72.dp, 64.dp).forEach { chipWidth ->
                        SkeletonBox(Modifier.size(width = chipWidth, height = 48.dp), radius = 9.dp)
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(3) {
                    SkeletonBox(Modifier.weight(1f).height(38.dp), radius = 18.dp)
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(TomiloSurface2.copy(alpha = 0.72f))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBox(Modifier.width(92.dp).height(20.dp), radius = 6.dp)
                    Spacer(Modifier.weight(1f))
                    SkeletonBox(Modifier.width(52.dp).height(32.dp), radius = 14.dp)
                }
                Spacer(Modifier.height(10.dp))
                repeat(3) { index ->
                    SkeletonBox(
                        Modifier.fillMaxWidth(if (index == 1) 0.88f else 1f).height(14.dp),
                        radius = 6.dp,
                    )
                    if (index < 2) Spacer(Modifier.height(7.dp))
                }
            }

            SkeletonBox(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).height(126.dp),
                radius = 20.dp,
            )
        }
    }
}

@Composable
fun ListCardsSkeleton(modifier: Modifier = Modifier, count: Int = 6) {
    ShimmerScope {
        Column(
            modifier
                .fillMaxWidth()
                .loadingSemantics("Загрузка списка")
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(count) { ListCardSkeleton() }
        }
    }
}

@Composable
fun LeaderboardSkeleton(modifier: Modifier = Modifier) {
    ShimmerScope {
        Column(
            modifier
                .fillMaxSize()
                .loadingSemantics("Загрузка рейтинга")
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
}

@Composable
fun ProfileScreenSkeleton(modifier: Modifier = Modifier) {
    ShimmerScope {
        Column(
            modifier
                .fillMaxSize()
                .loadingSemantics("Загрузка профиля")
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
}
