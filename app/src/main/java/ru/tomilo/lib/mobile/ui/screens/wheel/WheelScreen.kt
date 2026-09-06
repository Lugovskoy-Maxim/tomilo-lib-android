package ru.tomilo.lib.mobile.ui.screens.wheel

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import ru.tomilo.lib.mobile.ui.components.RewardNotifications
import ru.tomilo.lib.mobile.ui.components.StatusPill
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import ru.tomilo.lib.mobile.ui.theme.TomiloText
import java.time.Instant
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Luxurious casino colors for alternating sectors
private val CasinoWheelColors = listOf(
    Color(0xFFE53935), // Ruby Red
    Color(0xFF283593), // Midnight Blue
    Color(0xFF8E24AA), // Royal Amethyst
    Color(0xFF1E88E5), // Sapphire
    Color(0xFF43A047), // Emerald Green
    Color(0xFFFB8C00), // Rich Amber
    Color(0xFF00ACC1), // Deep Cyan
    Color(0xFFD81B60), // Vibrant Rose
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
    val haptics = LocalHapticFeedback.current
    val rotation = remember { Animatable(0f) }
    var wheel by remember { mutableStateOf<WheelDto?>(null) }
    var winners by remember { mutableStateOf<List<WheelRecentWinDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var spinning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<WheelSpinResultDto?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }

    // Pointer flap animation during spinning
    var pointerFlap by remember { mutableStateOf(0f) }

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

    // Dynamic haptic ticks as wheel rotates past pegs
    LaunchedEffect(spinning) {
        if (!spinning) return@LaunchedEffect
        val count = wheel?.segments?.size?.coerceAtLeast(1) ?: 8
        val slice = 360f / count
        var lastTickIndex = -1
        snapshotFlow { (rotation.value / slice).toInt() }
            .collect { tickIndex ->
                if (tickIndex != lastTickIndex) {
                    lastTickIndex = tickIndex
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    // Flap pointer slightly
                    pointerFlap = if (pointerFlap > 0f) -8f else 8f
                }
            }
    }

    fun runSpin(skipCooldown: Boolean) {
        val current = wheel ?: return
        if (spinning) return
        scope.launch {
            spinning = true
            result = null
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
                    rotation.animateTo(
                        target,
                        tween(
                            durationMillis = 6_200,
                            easing = CubicBezierEasing(0.08f, 0.62f, 0.08f, 1f),
                        ),
                    )
                    pointerFlap = 0f
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    RewardNotifications.show(
                        experience = won.expGained ?: 0,
                        coins = (won.coinsGained ?: 0) + (won.compensationCoins ?: 0),
                        source = "Колесо судьбы",
                    )
                    authRepository.refreshProfile()
                    result = won
                    wheel = authRepository.wheel().getOrDefault(
                        current.copy(
                            balance = won.balance ?: current.balance,
                            canSpin = false,
                            nextSpinAt = won.nextSpinAt ?: current.nextSpinAt,
                        ),
                    )
                    winners = authRepository.wheelRecentWins().getOrNull()?.let { data ->
                        listOfNotNull(data.highlight) + data.recent
                    }.orEmpty().distinctBy { it.username + it.wonAt + it.label }
                }
                .onFailure {
                    snackbar.showSnackbar(it.message ?: "Не удалось запустить колесо")
                }
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
                title = {
                    Text(
                        "Колесо судьбы",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
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
                message = "Войдите в аккаунт, чтобы крутить колесо и получать ежедневные призы.",
                icon = Icons.Default.Casino,
                actionLabel = "Войти",
                onAction = onLogin,
                modifier = Modifier.padding(padding),
            )
            loading && wheel == null -> LoadingBox(Modifier.padding(padding), "Готовим колесо призов…")
            error != null && wheel == null -> ErrorBox(error ?: "Ошибка", Modifier.padding(padding)) { reload += 1 }
            wheel == null -> EmptyState(
                "Колесо недоступно",
                "Попробуйте обновить страницу позже.",
                Modifier.padding(padding),
                Icons.Default.Casino,
            )
            else -> {
                val data = wheel!!
                val cooldown = countdown(data.nextSpinAt, nowMs)
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Header Balance Card
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = TomiloSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                TomiloPrimary.copy(alpha = 0.12f),
                                                Color.Transparent,
                                            ),
                                        ),
                                    )
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        "Ваш баланс",
                                        color = TomiloMuted,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.MonetizationOn,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "${data.balance} монет",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                        )
                                    }
                                }

                                StatusPill(
                                    text = if (cooldown == null) "Бесплатный спин" else "Откат $cooldown",
                                    color = if (cooldown == null) Color(0xFF54C798) else TomiloMuted,
                                )
                            }
                        }
                    }

                    // Daily Streak & Jackpot Boost Banner
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = TomiloSurface2,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.25f)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFFFFD700).copy(alpha = 0.12f),
                                                Color(0xFFE5A60D).copy(alpha = 0.04f),
                                            ),
                                        ),
                                    )
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFD700).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("🔥", fontSize = 16.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Ежедневная серия спинов",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFFFDF70),
                                    )
                                    Text(
                                        "Крутите каждый день для повышенного шанса на джекпот!",
                                        color = TomiloMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    }

                    // Casino Wheel Showcase
                    item {
                        CasinoWheelPanel(
                            segments = data.segments,
                            rotation = rotation.value,
                            spinning = spinning,
                            pointerFlap = pointerFlap,
                        )
                    }

                    // Spin Actions Controller
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = TomiloSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        ) {
                            Column(Modifier.padding(18.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(
                                            "Обычный спин",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            if (data.spinCostCoins == 0) "Бесплатно" else "${data.spinCostCoins} монет",
                                            color = TomiloPremium,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                    if (cooldown != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = TomiloMuted,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                cooldown,
                                                color = TomiloMuted,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                // Main Spin Button
                                Button(
                                    onClick = { runSpin(false) },
                                    enabled = data.canSpin && !spinning,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TomiloPrimary,
                                        disabledContainerColor = TomiloPrimary.copy(alpha = 0.35f),
                                    ),
                                ) {
                                    if (spinning) {
                                        CircularProgressIndicator(
                                            Modifier.size(22.dp),
                                            strokeWidth = 2.5.dp,
                                            color = Color.White,
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text("Судьба выбирает…", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    } else {
                                        Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(22.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (cooldown == null) "Крутить колесо" else "Дождитесь отката",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                        )
                                    }
                                }

                                // Instant Spin option to skip timer
                                AnimatedVisibility(visible = cooldown != null) {
                                    Column {
                                        Spacer(Modifier.height(10.dp))
                                        OutlinedButton(
                                            onClick = { runSpin(true) },
                                            enabled = data.canInstantSpin && !spinning,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            shape = RoundedCornerShape(14.dp),
                                        ) {
                                            Icon(Icons.Default.Bolt, contentDescription = null, tint = TomiloPremium)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Крутить мгновенно · ${data.instantSpinCostCoins ?: (data.spinCostCoins * 2)} монет",
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Available Prizes Section
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Секторы и награды",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${data.segments.size} призов",
                                color = TomiloMuted,
                                fontSize = 12.sp,
                            )
                        }
                    }

                    items(data.segments, key = { it.rewardType + it.label + it.hashCode() }) { segment ->
                        PrizeRow(segment)
                    }

                    // Recent Winners Ticker
                    if (winners.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Недавние победители",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        items(winners.take(8), key = { it.username + it.wonAt + it.label }) { win ->
                            WinnerRow(win)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CasinoWheelPanel(
    segments: List<WheelSegmentDto>,
    rotation: Float,
    spinning: Boolean,
    pointerFlap: Float,
) {
    // LED Bulbs animation
    val infiniteTransition = rememberInfiniteTransition()
    val bulbOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (spinning) 700 else 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bulbOffset",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.radialGradient(
                    listOf(
                        Color(0xFF2C2016),
                        Color(0xFF13100E),
                    ),
                ),
            )
            .border(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFD56B).copy(alpha = 0.4f),
                        Color(0xFF8B6B23).copy(alpha = 0.2f),
                    ),
                ),
                RoundedCornerShape(32.dp),
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CasinoFortuneWheelCanvas(
            segments = segments,
            rotationValue = rotation,
            bulbOffset = bulbOffset.toInt(),
            isSpinning = spinning,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )

        // Center 3D Casino Medallion
        Box(
            Modifier
                .size(76.dp)
                .shadow(16.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFFFE89A),
                            Color(0xFFE5A817),
                            Color(0xFF8A5B03),
                        ),
                    ),
                )
                .border(3.dp, Color(0xFFFFF4D1), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Casino,
                contentDescription = null,
                tint = Color(0xFF3F2500),
                modifier = Modifier.size(36.dp),
            )
        }

        // Top Golden Pointer with Ruby Gem
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 2.dp)
                .rotate(pointerFlap),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(width = 38.dp, height = 30.dp)) {
                val path = Path().apply {
                    moveTo(size.width / 2f, size.height) // tip pointing down
                    lineTo(0f, 0f)
                    lineTo(size.width, 0f)
                    close()
                }
                // Gold drop shadow
                drawPath(path, Color(0xFF7A5408))
                // Golden pointer needle
                drawPath(
                    path,
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFF0B8), Color(0xFFDCA11E)),
                    ),
                )
                // Ruby Center Jewel
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0xFFFF5252), Color(0xFFB71C1C)),
                    ),
                    radius = 4.dp.toPx(),
                    center = Offset(size.width / 2f, 8.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun CasinoFortuneWheelCanvas(
    segments: List<WheelSegmentDto>,
    rotationValue: Float,
    bulbOffset: Int,
    isSpinning: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val n = segments.size.coerceAtLeast(1)
        val diameter = min(size.width, size.height) * 0.88f
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val rect = Rect(topLeft, androidx.compose.ui.geometry.Size(diameter, diameter))
        val sweep = 360f / n

        // Outer Casino Metallic Ring
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF2C241B), Color(0xFF14100C)),
            ),
            radius = diameter / 2f + 16.dp.toPx(),
            center = center,
        )
        // Outer Gold Bevel
        drawCircle(
            brush = Brush.sweepGradient(
                listOf(
                    Color(0xFFE5B03C),
                    Color(0xFFFFF2BD),
                    Color(0xFFB57D18),
                    Color(0xFFE5B03C),
                ),
            ),
            radius = diameter / 2f + 12.dp.toPx(),
            center = center,
            style = Stroke(width = 4.dp.toPx()),
        )
        // Dark LED track ring
        drawCircle(
            color = Color(0xFF0F0C0A),
            radius = diameter / 2f + 7.dp.toPx(),
            center = center,
            style = Stroke(width = 6.dp.toPx()),
        )

        // Marquee LED Lights (24 Bulbs)
        val totalBulbs = 24
        repeat(totalBulbs) { index ->
            val angle = Math.toRadians((-90.0 + index * (360.0 / totalBulbs)))
            val r = diameter / 2f + 7.dp.toPx()
            val bulbPos = Offset(center.x + cos(angle).toFloat() * r, center.y + sin(angle).toFloat() * r)
            val isChased = (index + bulbOffset) % 3 == 0

            val bulbColor = when {
                isChased -> Color(0xFFFFFAED)
                index % 2 == 0 -> Color(0xFFFFD54F)
                else -> Color(0xFFFFA726)
            }
            val glowRadius = if (isChased) 3.5.dp.toPx() else 2.5.dp.toPx()
            drawCircle(bulbColor, glowRadius, bulbPos)
        }

        // Inner Wheel Segments with Rotation
        rotate(rotationValue, center) {
            repeat(n) { index ->
                val sliceColor = CasinoWheelColors[index % CasinoWheelColors.size]
                // Segment background
                drawArc(
                    color = sliceColor,
                    startAngle = -90f + index * sweep,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = rect.topLeft,
                    size = rect.size,
                )
                // Gold divider line
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color(0xFFFFE082), Color(0xFFBCAAA4)),
                    ),
                    startAngle = -90f + index * sweep,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = Stroke(1.5.dp.toPx()),
                )
            }

            // Radial labels with Paint & shadow
            val radius = diameter * 0.33f
            val textPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textAlign = Paint.Align.CENTER
                textSize = 11.5.dp.toPx()
                isFakeBoldText = true
                setShadowLayer(3f, 0f, 1f, android.graphics.Color.argb(180, 0, 0, 0))
            }

            segments.forEachIndexed { index, segment ->
                val sliceAngle = -90f + (index + 0.5f) * sweep
                withTransform({
                    rotate(sliceAngle, center)
                }) {
                    drawContext.canvas.nativeCanvas.drawText(
                        shortReward(segment),
                        center.x,
                        center.y - radius,
                        textPaint,
                    )
                }
            }
        }

        // Inner center shadow
        drawCircle(Color.Black.copy(alpha = 0.28f), diameter * 0.16f, center)
    }
}

@Composable
private fun PrizeRow(segment: WheelSegmentDto) {
    val color = rarityColor(segment.rarity)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = TomiloSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.24f)),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(rewardIcon(segment.rewardType), null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    segment.label.ifBlank { rewardTypeLabel(segment.rewardType) },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    segment.rewardMeta?.valueText ?: rewardTypeLabel(segment.rewardType),
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            segment.rarity?.let { StatusPill(rarityLabel(it), color) }
        }
    }
}

@Composable
private fun WinnerRow(win: WheelRecentWinDto) {
    val color = rarityColor(win.rarity)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = TomiloSurface2.copy(alpha = 0.6f),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.EmojiEvents, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    win.username.ifBlank { "Читатель" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Text(
                    win.label,
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }
            Text(timeAgo(win.wonAt), color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun RewardDialog(won: WheelSpinResultDto, onDismiss: () -> Unit) {
    val twist = won.twistOfFate
    val color = if (twist) Color(0xFFF07832) else TomiloPremium

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.16f))
                    .border(2.dp, color.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (twist) Icons.Default.Bolt else Icons.Default.EmojiEvents,
                    null,
                    tint = color,
                    modifier = Modifier.size(40.dp),
                )
            }
        },
        title = {
            Text(
                if (twist) "Обман судьбы!" else "Поздравляем!",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    won.label.ifBlank { "Награда получена" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                )
                Spacer(Modifier.height(10.dp))
                val details = buildList {
                    won.coinsGained?.takeIf { it != 0 }?.let { add("+$it монет") }
                    won.expGained?.takeIf { it != 0 }?.let { add("+$it опыта") }
                    won.compensationCoins?.takeIf { it != 0 }?.let { add("Компенсация +$it монет") }
                    won.itemsGained.forEach { add("${it.name ?: it.itemId} ×${it.count}") }
                }
                details.forEach {
                    Text(
                        it,
                        color = TomiloPrimary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TomiloPrimary),
            ) {
                Text("Забрать награду", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = TomiloSurface2,
    )
}

private fun shortReward(s: WheelSegmentDto): String = when (s.rewardType.lowercase()) {
    "coins" -> s.rewardMeta?.valueText ?: "Монеты"
    "exp", "experience" -> s.rewardMeta?.valueText ?: "Опыт"
    "premium" -> "Premium"
    "item" -> s.label.take(10)
    "nothing", "empty" -> "Пусто"
    else -> s.label.take(10).ifBlank { "Приз" }
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
