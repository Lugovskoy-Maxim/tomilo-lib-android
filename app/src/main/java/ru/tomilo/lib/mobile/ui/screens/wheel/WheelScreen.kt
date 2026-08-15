package ru.tomilo.lib.mobile.ui.screens.wheel

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.data.api.WheelDto
import ru.tomilo.lib.mobile.data.api.WheelRecentWinDto
import ru.tomilo.lib.mobile.data.api.WheelSegmentDto
import ru.tomilo.lib.mobile.data.api.WheelSpinResultDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.PageIntro
import ru.tomilo.lib.mobile.ui.components.StatusPill
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import java.time.Instant
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val WheelColors = listOf(
    Color(0xFFF5A623), Color(0xFFE84B4B), Color(0xFF9559E8), Color(0xFF39B87F),
    Color(0xFF4285E6), Color(0xFFE05291), Color(0xFFF07832), Color(0xFF22A8A0),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelScreen(
    authRepository: AuthRepository,
    onBack: () -> Unit,
    onLogin: () -> Unit,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val rotation = remember { Animatable(0f) }
    var wheel by remember { mutableStateOf<WheelDto?>(null) }
    var winners by remember { mutableStateOf<List<WheelRecentWinDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var spinning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<WheelSpinResultDto?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1_000)
        }
    }

    LaunchedEffect(user?.stableId(), reload) {
        if (user == null) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        authRepository.wheel()
            .onSuccess { wheel = it }
            .onFailure { error = it.message }
        winners = authRepository.wheelRecentWins().getOrNull()?.let { data ->
            listOfNotNull(data.highlight) + data.recent
        }.orEmpty().distinctBy { it.username + it.wonAt + it.label }
        loading = false
    }

    fun runSpin(skipCooldown: Boolean) {
        val current = wheel ?: return
        if (spinning) return
        scope.launch {
            spinning = true
            result = null
            authRepository.spinWheel(skipCooldown)
                .onSuccess { won ->
                    val count = current.segments.size.coerceAtLeast(1)
                    val index = won.selectedSegmentIndex
                        ?.takeIf { it in 0 until count }
                        ?: current.segments.indexOfFirst { it.label == won.label }.coerceAtLeast(0)
                    val slice = 360f / count
                    val currentNormalized = ((rotation.value % 360f) + 360f) % 360f
                    val landing = 360f - (index + 0.5f) * slice
                    val target = rotation.value + 6f * 360f + (landing - currentNormalized + 360f) % 360f
                    rotation.animateTo(target, tween(7_200, easing = FastOutSlowInEasing))
                    result = won
                    wheel = authRepository.wheel().getOrDefault(current.copy(
                        balance = won.balance ?: current.balance,
                        canSpin = false,
                        nextSpinAt = won.nextSpinAt ?: current.nextSpinAt,
                    ))
                    winners = authRepository.wheelRecentWins().getOrNull()?.let { data ->
                        listOfNotNull(data.highlight) + data.recent
                    }.orEmpty().distinctBy { it.username + it.wonAt + it.label }
                }
                .onFailure { snackbar.showSnackbar(it.message ?: "Не удалось запустить колесо") }
            spinning = false
        }
    }

    result?.let { won ->
        RewardDialog(won = won, onDismiss = { result = null })
    }

    Scaffold(
        containerColor = TomiloBg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Колесо судьбы") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { reload += 1 }, enabled = !loading && !spinning) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        when {
            user == null -> EmptyState(
                title = "Испытайте судьбу",
                message = "Войдите в аккаунт, чтобы крутить колесо и получать награды.",
                icon = Icons.Default.Casino,
                actionLabel = "Войти",
                onAction = onLogin,
                modifier = Modifier.padding(padding),
            )
            loading && wheel == null -> LoadingBox(Modifier.padding(padding), "Готовим призы…")
            error != null && wheel == null -> ErrorBox(error ?: "Ошибка", Modifier.padding(padding)) { reload += 1 }
            wheel == null -> EmptyState("Колесо недоступно", "Попробуйте обновить страницу позже.", Modifier.padding(padding), Icons.Default.Casino)
            else -> {
                val data = wheel!!
                val cooldown = countdown(data.nextSpinAt, nowMs)
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        PageIntro(
                            title = "Ваш шанс на редкую награду",
                            subtitle = "Результат определяет сервер — каждый выигрыш настоящий",
                            icon = Icons.Default.AutoAwesome,
                            accent = TomiloPremium,
                            trailing = { StatusPill("${data.balance} монет", TomiloPremium) },
                        )
                    }
                    item {
                        WheelPanel(
                            segments = data.segments,
                            rotation = rotation.value,
                            spinning = spinning,
                        )
                    }
                    item {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(TomiloSurface)
                                .border(1.dp, TomiloBorder, RoundedCornerShape(22.dp)).padding(16.dp),
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Обычный спин", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${data.spinCostCoins} монет", color = TomiloPremium, style = MaterialTheme.typography.bodyMedium)
                                }
                                StatusPill(if (cooldown == null) "Доступно" else cooldown, if (cooldown == null) Color(0xFF54C798) else TomiloMuted)
                            }
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { runSpin(false) },
                                enabled = data.canSpin && !spinning,
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                            ) {
                                if (spinning) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Default.Casino, null)
                                Spacer(Modifier.width(9.dp))
                                Text(if (spinning) "Колесо вращается…" else "Крутить колесо", fontWeight = FontWeight.Bold)
                            }
                            AnimatedVisibility(cooldown != null) {
                                Column {
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = { runSpin(true) },
                                        enabled = data.canInstantSpin && !spinning,
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                    ) {
                                        Icon(Icons.Default.Bolt, null, tint = TomiloPremium)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Не ждать · ${data.instantSpinCostCoins ?: data.spinCostCoins * 2} монет")
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Text("Возможные награды", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    items(data.segments, key = { it.rewardType + it.label + it.hashCode() }) { segment ->
                        PrizeRow(segment)
                    }
                    if (winners.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            Text("Недавние победители", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        items(winners.take(8), key = { it.username + it.wonAt + it.label }) { win -> WinnerRow(win) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelPanel(segments: List<WheelSegmentDto>, rotation: Float, spinning: Boolean) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.radialGradient(listOf(TomiloPrimary.copy(alpha = 0.18f), TomiloSurface)))
            .border(1.dp, TomiloPremium.copy(alpha = 0.22f), RoundedCornerShape(28.dp)).padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        FortuneWheel(segments, rotation, Modifier.fillMaxWidth().aspectRatio(1f))
        Box(
            Modifier.align(Alignment.TopCenter).size(30.dp).clip(RoundedCornerShape(bottomStart = 15.dp, bottomEnd = 15.dp))
                .background(TomiloPremium),
        )
        if (spinning) StatusPill("Судьба выбирает…", TomiloPremium, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun FortuneWheel(segments: List<WheelSegmentDto>, rotationValue: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val n = segments.size.coerceAtLeast(1)
        val diameter = min(size.width, size.height) * 0.88f
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val rect = Rect(topLeft, androidx.compose.ui.geometry.Size(diameter, diameter))
        val sweep = 360f / n
        drawCircle(TomiloPremium.copy(alpha = 0.25f), diameter / 2f + 12.dp.toPx(), center)
        drawCircle(Color(0xFF17130A), diameter / 2f + 6.dp.toPx(), center)
        rotate(rotationValue, center) {
            repeat(n) { index ->
                drawArc(
                    color = WheelColors[index % WheelColors.size],
                    startAngle = -90f + index * sweep,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = rect.topLeft,
                    size = rect.size,
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.42f),
                    startAngle = -90f + index * sweep,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()),
                )
            }
            val radius = diameter * 0.34f
            val paint = Paint().apply {
                color = android.graphics.Color.WHITE
                textAlign = Paint.Align.CENTER
                textSize = 11.dp.toPx()
                isFakeBoldText = true
                setShadowLayer(2f, 0f, 1f, android.graphics.Color.argb(150, 0, 0, 0))
            }
            segments.forEachIndexed { index, segment ->
                val angle = Math.toRadians((-90.0 + (index + 0.5) * sweep))
                val x = center.x + cos(angle).toFloat() * radius
                val y = center.y + sin(angle).toFloat() * radius
                drawContext.canvas.nativeCanvas.drawText(shortReward(segment), x, y + paint.textSize / 3f, paint)
            }
        }
        repeat(12) { index ->
            val angle = Math.toRadians((-90.0 + index * 30.0))
            val r = diameter / 2f + 7.dp.toPx()
            val bulb = Offset(center.x + cos(angle).toFloat() * r, center.y + sin(angle).toFloat() * r)
            drawCircle(if (index % 2 == 0) Color.White else Color(0xFFFFE18B), 2.8.dp.toPx(), bulb)
        }
        drawCircle(TomiloPremium, diameter * 0.115f, center)
        drawCircle(Color(0xFFFFF3CC), diameter * 0.045f, center)
        val pointer = Path().apply {
            moveTo(center.x, topLeft.y - 2.dp.toPx())
            lineTo(center.x - 13.dp.toPx(), topLeft.y - 24.dp.toPx())
            lineTo(center.x + 13.dp.toPx(), topLeft.y - 24.dp.toPx())
            close()
        }
        drawPath(pointer, TomiloPremium)
    }
}

@Composable
private fun PrizeRow(segment: WheelSegmentDto) {
    val color = rarityColor(segment.rarity)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(TomiloSurface)
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(18.dp)).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(rewardIcon(segment.rewardType), null, tint = color)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(segment.label.ifBlank { rewardTypeLabel(segment.rewardType) }, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(segment.rewardMeta?.valueText ?: rewardTypeLabel(segment.rewardType), color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
        }
        segment.rarity?.let { StatusPill(rarityLabel(it), color) }
    }
}

@Composable
private fun WinnerRow(win: WheelRecentWinDto) {
    val color = rarityColor(win.rarity)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(TomiloSurface2.copy(alpha = 0.66f)).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(color.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.EmojiEvents, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(win.username.ifBlank { "Читатель" }, fontWeight = FontWeight.SemiBold)
            Text(win.label, color = TomiloMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        Text(timeAgo(win.wonAt), color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun RewardDialog(won: WheelSpinResultDto, onDismiss: () -> Unit) {
    val twist = won.twistOfFate
    val color = if (twist) Color(0xFFF07832) else TomiloPremium
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(Modifier.size(68.dp).clip(RoundedCornerShape(23.dp)).background(color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(if (twist) Icons.Default.Bolt else Icons.Default.EmojiEvents, null, tint = color, modifier = Modifier.size(35.dp))
            }
        },
        title = { Text(if (twist) "Обман судьбы" else "Награда ваша!", textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(won.label.ifBlank { "Награда получена" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                val details = buildList {
                    won.coinsGained?.takeIf { it != 0 }?.let { add("+$it монет") }
                    won.expGained?.takeIf { it != 0 }?.let { add("+$it опыта") }
                    won.compensationCoins?.takeIf { it != 0 }?.let { add("Компенсация +$it монет") }
                    won.itemsGained.forEach { add("${it.name ?: it.itemId} ×${it.count}") }
                }
                details.forEach { Text(it, color = TomiloMuted, textAlign = TextAlign.Center) }
            }
        },
        confirmButton = { Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Отлично") } },
        shape = RoundedCornerShape(30.dp),
        containerColor = TomiloSurface2,
    )
}

private fun shortReward(s: WheelSegmentDto): String = when (s.rewardType.lowercase()) {
    "coins" -> s.rewardMeta?.valueText ?: "Монеты"
    "exp", "experience" -> s.rewardMeta?.valueText ?: "Опыт"
    "premium" -> "Premium"
    "item" -> s.label.take(9)
    "nothing", "empty" -> "Пусто"
    else -> s.label.take(9).ifBlank { "Приз" }
}

private fun rewardTypeLabel(type: String): String = when (type.lowercase()) {
    "coins" -> "Монеты"
    "exp", "experience" -> "Опыт"
    "premium" -> "Premium"
    "item" -> "Предмет"
    "nothing", "empty" -> "Пустой сектор"
    else -> "Награда"
}

private fun rewardIcon(type: String) = when (type.lowercase()) {
    "coins" -> Icons.Default.MonetizationOn
    "exp", "experience" -> Icons.Default.Bolt
    "item", "premium" -> Icons.Default.AutoAwesome
    else -> Icons.Default.Casino
}

private fun rarityColor(rarity: String?): Color = when (rarity?.lowercase()) {
    "legendary" -> Color(0xFFFFB743)
    "epic" -> Color(0xFFB36BFF)
    "rare" -> Color(0xFF62B8FF)
    "uncommon" -> Color(0xFF54C798)
    else -> TomiloMuted
}

private fun rarityLabel(rarity: String): String = when (rarity.lowercase()) {
    "legendary" -> "Легендарное"
    "epic" -> "Эпическое"
    "rare" -> "Редкое"
    "uncommon" -> "Необычное"
    else -> "Обычное"
}

private fun countdown(nextAt: String?, nowMs: Long): String? {
    val end = runCatching { Instant.parse(nextAt).toEpochMilli() }.getOrNull() ?: return null
    val seconds = ((end - nowMs) / 1_000).coerceAtLeast(0)
    if (seconds <= 0) return null
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val secs = seconds % 60
    return if (hours > 0) "%dч %02dм".format(hours, minutes) else "%dм %02dс".format(minutes, secs)
}

private fun timeAgo(value: String): String {
    val then = runCatching { Instant.parse(value).toEpochMilli() }.getOrNull() ?: return "недавно"
    val minutes = ((System.currentTimeMillis() - then) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> "сейчас"
        minutes < 60 -> "$minutes мин"
        minutes < 1_440 -> "${minutes / 60} ч"
        else -> "${minutes / 1_440} дн"
    }
}
