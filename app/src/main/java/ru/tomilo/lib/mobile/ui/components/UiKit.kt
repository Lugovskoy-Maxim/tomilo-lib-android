package ru.tomilo.lib.mobile.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import kotlin.math.roundToInt

class SwipeRevealCoordinator {
    var openKey by mutableStateOf<String?>(null)
        private set

    fun open(key: String) {
        openKey = key
    }

    fun close(key: String? = null) {
        if (key == null || openKey == key) openKey = null
    }
}

@Composable
fun rememberSwipeRevealCoordinator(): SwipeRevealCoordinator = remember { SwipeRevealCoordinator() }

/**
 * Свайп как в Telegram: короткое движение влево фиксирует кнопку.
 * Карточка непрозрачная, отпускать и держать не нужно.
 */
@Composable
fun SwipeActionContainer(
    actionLabel: String,
    actionIcon: ImageVector,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionColor: Color,
    enabled: Boolean = true,
    revealKey: String? = null,
    coordinator: SwipeRevealCoordinator? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val actionWidth = 84.dp
    val actionPx = with(density) { actionWidth.toPx() }
    val offset = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    val revealed = offset.value <= -actionPx * 0.5f

    LaunchedEffect(coordinator?.openKey, revealKey) {
        if (revealKey != null && coordinator != null && coordinator.openKey != revealKey && offset.value != 0f) {
            offset.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
        }
    }

    fun snap(open: Boolean) {
        scope.launch {
            offset.animateTo(
                if (open) -actionPx else 0f,
                spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow),
            )
            if (revealKey != null && coordinator != null) {
                if (open) coordinator.open(revealKey) else coordinator.close(revealKey)
            }
        }
    }

    val dragState = rememberDraggableState { delta ->
        if (!enabled) return@rememberDraggableState
        scope.launch {
            offset.snapTo((offset.value + delta).coerceIn(-actionPx, 0f))
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(IntrinsicSize.Min)
            .clipToBounds(),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(actionWidth)
                .padding(vertical = 6.dp, horizontal = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(actionColor)
                .clickable(enabled = enabled && revealed) {
                    onAction()
                    snap(false)
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(actionIcon, contentDescription = actionLabel, tint = Color.White)
                Spacer(Modifier.height(2.dp))
                Text(
                    actionLabel,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    onDragStarted = { dragging = true },
                    onDragStopped = { velocity ->
                        dragging = false
                        val shouldOpen = offset.value < -actionPx * 0.28f || velocity < -700f
                        snap(shouldOpen)
                    },
                ),
        ) {
            content()
            if (dragging || offset.value < -4f) {
                Box(
                    Modifier
                        .matchParentSize()
                        .clickable { snap(false) },
                )
            }
        }
    }
}

@Composable
fun LoadingBox(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    val pulse by rememberInfiniteTransition(label = "loadingPulse").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "loadingPulseAlpha",
    )
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(TomiloPrimary.copy(alpha = 0.10f * pulse))
                    .border(1.dp, TomiloPrimary.copy(alpha = 0.20f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.8.dp,
                    color = TomiloPrimary,
                    modifier = Modifier.size(31.dp),
                )
            }
            if (!message.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    message,
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
    }
}

/** Нижняя полоска «подгружаем ещё…» для каталога / лент. */
@Composable
fun LoadingMoreBar(
    visible: Boolean,
    message: String = "Подгружаем ещё тайтлы…",
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp)),
            color = TomiloPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(message, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ErrorBox(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(26.dp), ambientColor = Color.Black.copy(alpha = 0.2f))
                .clip(RoundedCornerShape(26.dp))
                .background(TomiloSurface)
                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.18f), RoundedCornerShape(26.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text("Не удалось загрузить", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
            if (onRetry != null) {
                Spacer(Modifier.height(18.dp))
                Button(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Попробовать снова")
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .size(76.dp)
                    .shadow(18.dp, RoundedCornerShape(26.dp), ambientColor = TomiloPrimary.copy(alpha = 0.20f))
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(TomiloPrimary.copy(alpha = 0.22f), TomiloPrimary.copy(alpha = 0.08f)),
                        ),
                    )
                    .border(1.dp, TomiloPrimary.copy(alpha = 0.24f), RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TomiloPrimary,
                    modifier = Modifier.size(33.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TomiloMuted,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            ) { Text(actionLabel) }
        }
    }
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(4.dp, 21.dp).clip(CircleShape).background(TomiloPrimary))
        Spacer(Modifier.width(9.dp))
        Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = TomiloPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(TomiloPrimary.copy(alpha = 0.09f))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun ActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: String? = null,
    iconTint: Color = TomiloPrimary,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        shape = RoundedCornerShape(18.dp),
        color = TomiloSurface,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, TomiloBorder.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(iconTint.copy(alpha = 0.20f), iconTint.copy(alpha = 0.08f)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        subtitle,
                        color = TomiloMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                    )
                }
            }
            if (!badge.isNullOrBlank()) {
                Text(
                    badge,
                    color = iconTint,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.12f))
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TomiloMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Bottom inset so scroll content clears the floating glass tab bar
 * (content draws under the bar; last items need this gap).
 */
val ScreenPadding = PaddingValues(bottom = 100.dp)

@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.error) }
        },
        title = { Text(title) },
        text = { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = TomiloSurface2,
    )
}

/** Вводный блок страницы: единая визуальная иерархия для вторичных экранов. */
@Composable
fun PageIntro(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Outlined.AutoAwesome,
    accent: Color = TomiloPrimary,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(9.dp, RoundedCornerShape(26.dp), ambientColor = accent.copy(alpha = 0.16f))
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.22f), TomiloSurface, TomiloSurface2),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.26f), RoundedCornerShape(26.dp)),
    ) {
        Box(
            Modifier
                .size(96.dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.18f), Color.Transparent)),
                ),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(50.dp)
                    .shadow(7.dp, RoundedCornerShape(17.dp), ambientColor = accent.copy(alpha = 0.22f))
                    .clip(RoundedCornerShape(17.dp))
                    .background(accent.copy(alpha = 0.18f))
                    .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(17.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = accent, modifier = Modifier.size(25.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TomiloMuted)
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color = TomiloPrimary,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.20f), RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
    )
}
