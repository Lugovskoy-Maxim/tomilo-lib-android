package ru.tomilo.lib.mobile.ui.screens.games

import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.tomilo.lib.mobile.core.MatchThreeEngine
import ru.tomilo.lib.mobile.core.MatchThreeMotion
import ru.tomilo.lib.mobile.core.MatchThreeEngine.Obstacle
import ru.tomilo.lib.mobile.data.api.UserDto
import ru.tomilo.lib.mobile.data.api.PillMatchAdminLevelDto
import ru.tomilo.lib.mobile.data.api.PillMatchAdminLevelRequest
import ru.tomilo.lib.mobile.data.api.PillMatchStateDto
import ru.tomilo.lib.mobile.data.api.PillMatchSessionDto
import ru.tomilo.lib.mobile.data.api.PillMatchLeaderboardUserDto
import ru.tomilo.lib.mobile.data.local.MatchThreeCustomLevel
import ru.tomilo.lib.mobile.data.local.MatchThreeLocalSession
import ru.tomilo.lib.mobile.data.local.MatchThreeProgressStore
import ru.tomilo.lib.mobile.data.local.MatchThreeRecord
import ru.tomilo.lib.mobile.data.local.MatchThreeSave
import ru.tomilo.lib.mobile.data.repo.GamesRepository
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloText

private enum class MatchThreeTab { PLAY, RANK, EDITOR }
private enum class MotionKind { SETTLE, SWAP, POP, FALL, ENTER }
private enum class GardenBackdrop(val title: String, val drawable: Int) {
    GARDEN("Оранжерея", ru.tomilo.lib.mobile.R.drawable.pill_garden_backdrop),
    LAB("Лаборатория", ru.tomilo.lib.mobile.R.drawable.pill_lab_backdrop),
    CAVE("Кристальная пещера", ru.tomilo.lib.mobile.R.drawable.pill_crystal_cave_backdrop),
}
private enum class RankPeriod(val title: String, val apiValue: String) { WEEK("Неделя", "week"), MONTH("Месяц", "month"), ALL("Всё время", "all") }
private val gemColors = listOf(Color(0xFFFF6F7D), Color(0xFF47D4E7), Color(0xFFFFC64F), Color(0xFFB18AFF), Color(0xFF73D99A))
private val gemMarks = listOf("✦", "◆", "☾", "✿", "●")
private val gemSprites = listOf(
    ru.tomilo.lib.mobile.R.drawable.pill_gem_coral,
    ru.tomilo.lib.mobile.R.drawable.pill_gem_cyan,
    ru.tomilo.lib.mobile.R.drawable.pill_gem_lemon,
    ru.tomilo.lib.mobile.R.drawable.pill_gem_violet,
    ru.tomilo.lib.mobile.R.drawable.pill_gem_mint,
)
private enum class GardenFxKind { MATCH, RAINBOW, WIN, HAMMER, SHUFFLE, ICE, CHAIN, ROCK }

private data class GardenFx(val kind: GardenFxKind, val cells: Set<Int>)

private data class GardenFxPlay(val token: Int, val effects: List<GardenFx>)

private data class FxPose(
    val scaleX: Float,
    val scaleY: Float,
    val rotation: Float,
    val alpha: Float,
    val nudgeX: Float,
    val nudgeY: Float,
    val box: Float,
)

private const val GARDEN_FX_MS = 440

private val matchFxFrames = listOf(
    ru.tomilo.lib.mobile.R.drawable.pill_fx_match_1,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_match_2,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_match_3,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_match_4,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_match_5,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_match_6,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_match_7,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_match_8,
)
private val rainbowFxFrames = listOf(
    ru.tomilo.lib.mobile.R.drawable.pill_fx_rainbow_1,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_rainbow_2,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_rainbow_3,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_rainbow_4,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_rainbow_5,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_rainbow_6,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_rainbow_7,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_rainbow_8,
)
private val winFxFrames = listOf(
    ru.tomilo.lib.mobile.R.drawable.pill_fx_win_1,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_win_2,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_win_3,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_win_4,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_win_5,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_win_6,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_win_7,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_win_8,
)

private fun gardenDrawable(kind: GardenFxKind, life: Float): Int {
    val frames = when (kind) {
        GardenFxKind.MATCH -> matchFxFrames
        GardenFxKind.RAINBOW -> rainbowFxFrames
        GardenFxKind.WIN -> winFxFrames
        GardenFxKind.HAMMER, GardenFxKind.SHUFFLE, GardenFxKind.ICE, GardenFxKind.CHAIN, GardenFxKind.ROCK -> null
    }
    if (frames != null) {
        val index = (life.coerceIn(0f, 1f) * frames.lastIndex).roundToInt().coerceIn(0, frames.lastIndex)
        return frames[index]
    }
    return when (kind) {
        GardenFxKind.HAMMER -> ru.tomilo.lib.mobile.R.drawable.pill_fx_hammer
        GardenFxKind.SHUFFLE -> ru.tomilo.lib.mobile.R.drawable.pill_fx_shuffle
        GardenFxKind.ICE -> ru.tomilo.lib.mobile.R.drawable.pill_fx_ice
        GardenFxKind.CHAIN -> ru.tomilo.lib.mobile.R.drawable.pill_fx_chain
        GardenFxKind.ROCK -> ru.tomilo.lib.mobile.R.drawable.pill_fx_rock
        GardenFxKind.MATCH, GardenFxKind.RAINBOW, GardenFxKind.WIN -> matchFxFrames.first()
    }
}

private fun gardenPose(kind: GardenFxKind, life: Float): FxPose {
    val t = life.coerceIn(0f, 1f)
    val fade = if (t < 0.72f) 1f else ((1f - t) / 0.28f).coerceIn(0f, 1f)
    return when (kind) {
        GardenFxKind.MATCH -> FxPose(0.72f + t * 0.5f, 0.72f + t * 0.5f, 0f, fade, 0f, 0f, 1.55f)
        GardenFxKind.RAINBOW -> FxPose(0.65f + t * 0.55f, 0.65f + t * 0.55f, 0f, fade, 0f, 0f, 1.9f)
        GardenFxKind.WIN -> FxPose(0.7f + t * 0.45f, 0.7f + t * 0.45f, 0f, if (t < 0.75f) 1f else ((1f - t) / 0.25f).coerceIn(0f, 1f), 0f, 0.4f - 0.7f * t, 2.6f)
        GardenFxKind.HAMMER -> {
            val strike = (t / 0.42f).coerceIn(0f, 1f)
            val punch = if (t < 0.42f) 1f else 1f + 0.18f * sin(PI * ((t - 0.42f) / 0.2f).coerceIn(0f, 1f)).toFloat()
            FxPose(punch, punch, -28f + 36f * strike, fade, -0.9f * (1f - strike), -1.05f * (1f - strike), 2.1f)
        }
        GardenFxKind.SHUFFLE -> {
            val pulse = sin(PI * t).toFloat()
            FxPose(0.45f + 0.7f * pulse, 0.45f + 0.7f * pulse, t * 360f, pulse, 0f, 0f, 3.4f)
        }
        GardenFxKind.ICE -> {
            val shake = if (t < 0.35f) sin(t * 48.0).toFloat() * (1f - t / 0.35f) * 0.07f else 0f
            val grow = if (t < 0.35f) 1f else 1f + 0.45f * ((t - 0.35f) / 0.65f)
            FxPose(grow, grow, 0f, fade, shake, 0f, 1.45f)
        }
        GardenFxKind.CHAIN -> FxPose(1f + 0.4f * t, 1f, 0f, fade, 0f, 0f, 1.45f)
        GardenFxKind.ROCK -> {
            val shake = if (t < 0.4f) sin(t * 52.0).toFloat() * (1f - t / 0.4f) * 0.05f else 0f
            FxPose(1f + 0.12f * t, 1f + 0.12f * t, 0f, fade, shake, shake * 0.4f, 1.45f)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MatchThreeScreen(
    user: UserDto?,
    gamesRepository: GamesRepository,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onLogin: () -> Unit = {},
    onOpenPremium: () -> Unit = {},
) {
    val context = LocalContext.current
    val store = remember(context) { MatchThreeProgressStore(context) }
    val scope = rememberCoroutineScope()
    val premium = ru.tomilo.lib.mobile.core.Premium.isActive(user?.subscriptionExpiresAt)
    val admin = user?.isAdmin() == true
    val capacity = if (premium) 8 else 5
    var saved by remember { mutableStateOf(MatchThreeSave()) }
    var selectedTab by rememberSaveable { mutableStateOf(MatchThreeTab.PLAY) }
    var selectedLevel by rememberSaveable { mutableIntStateOf(1) }
    var playing by rememberSaveable { mutableStateOf(false) }
    var board by remember { mutableStateOf<List<Int>>(emptyList()) }
    var visualGems by remember { mutableStateOf<List<MatchThreeMotion.Sprite>>(emptyList()) }
    var motionPhase by remember { mutableIntStateOf(0) }
    var motionKind by remember { mutableStateOf(MotionKind.SETTLE) }
    var busy by remember { mutableStateOf(false) }
    var hintCells by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var nudge by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var nudgeToken by remember { mutableIntStateOf(0) }
    var dealing by remember { mutableStateOf(false) }
    var dealToken by remember { mutableIntStateOf(0) }
    var obstacles by remember { mutableStateOf<Map<Int, Obstacle>>(emptyMap()) }
    var moves by rememberSaveable { mutableIntStateOf(0) }
    var collected by rememberSaveable { mutableIntStateOf(0) }
    var target by rememberSaveable { mutableIntStateOf(18) }
    var targetColor by rememberSaveable { mutableIntStateOf(0) }
    var boosterHammer by rememberSaveable { mutableIntStateOf(2) }
    var boosterRainbow by rememberSaveable { mutableIntStateOf(1) }
    var boosterShuffle by rememberSaveable { mutableIntStateOf(1) }
    var activeBooster by rememberSaveable { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var rejectedCells by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var rejectionSequence by remember { mutableIntStateOf(0) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var won by rememberSaveable { mutableStateOf(false) }
    var remoteError by remember { mutableStateOf<String?>(null) }
    var rankPeriod by rememberSaveable { mutableStateOf(RankPeriod.WEEK) }
    var gardenBackdrop by rememberSaveable { mutableStateOf(GardenBackdrop.GARDEN) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var saving by rememberSaveable { mutableStateOf(false) }
    var editorObstacle by rememberSaveable { mutableStateOf(Obstacle.ROCK) }
    var editorTarget by rememberSaveable { mutableIntStateOf(22) }
    var editorMoves by rememberSaveable { mutableIntStateOf(24) }
    var editorColor by rememberSaveable { mutableIntStateOf(0) }
    var editorSeed by rememberSaveable { mutableIntStateOf(810) }
    var editorObstacleCount by rememberSaveable { mutableIntStateOf(8) }
    var editorLevelNumber by rememberSaveable { mutableIntStateOf(1) }
    var editorLevelPinned by rememberSaveable { mutableStateOf(false) }
    var editorPublished by rememberSaveable { mutableStateOf(true) }
    var editorMix by rememberSaveable { mutableStateOf(true) }
    var editorErase by rememberSaveable { mutableStateOf(false) }
    var editorEditingLevel by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingDelete by remember { mutableStateOf<Int?>(null) }
    var trial by remember { mutableStateOf(false) }
    var editorObstacles by remember { mutableStateOf<Map<Int, Obstacle>>(emptyMap()) }
    var publishedLevels by remember { mutableStateOf<List<PillMatchAdminLevelDto>>(emptyList()) }
    var adminLevels by remember { mutableStateOf<List<PillMatchAdminLevelDto>>(emptyList()) }
    var rankUsers by remember { mutableStateOf<List<PillMatchLeaderboardUserDto>>(emptyList()) }
    var rankLoading by remember { mutableStateOf(false) }
    var rankError by remember { mutableStateOf<String?>(null) }
    var remoteMode by rememberSaveable { mutableStateOf(false) }
    var remoteLives by remember { mutableIntStateOf(-1) }
    var remoteCapacity by remember { mutableIntStateOf(capacity) }
    var remoteNextLifeAt by remember { mutableStateOf<String?>(null) }
    var remoteCompleted by remember { mutableStateOf<List<Int>>(emptyList()) }
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    var gardenFxPlay by remember { mutableStateOf(GardenFxPlay(0, emptyList())) }
    val haptics = LocalHapticFeedback.current
    val completedLevels = (saved.records.map { it.level } + remoteCompleted).toSet()
    val nextUnlockedLevel = MatchThreeEngine.nextUnlockedLevel(completedLevels)
    val furthestCompleted = nextUnlockedLevel - 1
    val remoteMinutes = remoteNextLifeAt?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }
        ?.let { ((it - now + 59_999L) / 60_000L).coerceAtLeast(0).toInt() } ?: 0

    LaunchedEffect(Unit) {
        saved = store.read().normalized(capacity)
        store.write(saved)
        if (user?.stableId().isNullOrBlank()) {
            selectedLevel = (saved.records.maxOfOrNull { it.level } ?: 0) + 1
            saved.activeSession?.takeIf { it.board.size == 64 && it.obstacles.size == 64 && it.level > 0 && it.moves > 0 }?.let { session ->
                selectedLevel = session.level
                board = session.board
                obstacles = session.obstacles.mapIndexedNotNull { index, kind ->
                    if (kind < 0) null else index to Obstacle.entries.getOrElse(kind) { Obstacle.ROCK }
                }.toMap()
                moves = session.moves
                collected = session.collected
                target = session.target
                targetColor = session.targetColor
                boosterHammer = session.hammer
                boosterRainbow = session.rainbow
                boosterShuffle = session.shuffle
                finished = false
                won = session.collected >= session.target
                playing = true
            }
        }
    }
    LaunchedEffect(user?.stableId(), capacity) {
        if (!user?.stableId().isNullOrBlank()) {
            playing = false
            remoteMode = false
            gamesRepository.pillMatchState().onSuccess { state ->
                remoteLives = state.lives.current
                remoteCapacity = state.lives.capacity
                remoteNextLifeAt = state.lives.nextLifeAt
                remoteCompleted = state.completedLevels
                selectedLevel = MatchThreeEngine.nextUnlockedLevel(state.completedLevels)
                val session = state.session
                if (session != null && !session.finished && (session.moves > 0 || session.collected >= session.target)) {
                    board = session.board.map { color -> listOf("coral", "cyan", "lemon", "violet", "mint").indexOf(color).coerceAtLeast(0) }
                    obstacles = session.obstacles.mapIndexedNotNull { i, kind -> if (kind < 0) null else i to Obstacle.entries.getOrElse(kind) { Obstacle.ROCK } }.toMap()
                    moves = session.moves; collected = session.collected; target = session.target; targetColor = session.targetColor
                    boosterHammer = session.boosters.hammer; boosterRainbow = session.boosters.rainbow; boosterShuffle = session.boosters.shuffle
                    remoteNextLifeAt = state.lives.nextLifeAt
                    selectedLevel = session.level
                    finished = false
                    won = session.collected >= session.target
                    playing = true
                    remoteMode = true
                }
            }
        } else {
            remoteMode = false
            remoteLives = -1
            remoteNextLifeAt = null
            remoteCompleted = emptyList()
        }
    }
    LaunchedEffect(capacity) {
        val refreshed = store.read().normalized(capacity)
        saved = refreshed
        store.write(refreshed)
    }
    LaunchedEffect(rejectionSequence) {
        if (rejectedCells.isNotEmpty()) { delay(450); rejectedCells = emptySet() }
    }
    LaunchedEffect(Unit) {
        gamesRepository.pillMatchPublishedLevels().onSuccess { publishedLevels = it }
    }
    LaunchedEffect(selectedTab, rankPeriod, user?.stableId()) {
        if (selectedTab == MatchThreeTab.RANK && !user?.stableId().isNullOrBlank()) {
            rankLoading = true
            rankError = null
            gamesRepository.pillMatchLeaderboard(rankPeriod.apiValue)
                .onSuccess { rankUsers = it.users }
                .onFailure { rankUsers = emptyList(); rankError = it.message ?: "Не удалось загрузить рейтинг" }
            rankLoading = false
        }
        if (selectedTab == MatchThreeTab.EDITOR && admin) {
            gamesRepository.pillMatchAdminLevels()
                .onSuccess { levels ->
                    adminLevels = levels.sortedBy { it.level }
                    if (!editorLevelPinned && editorEditingLevel == null) {
                        editorLevelNumber = (adminLevels.maxOfOrNull { it.level } ?: 0) + 1
                        editorLevelPinned = true
                    }
                }
                .onFailure { notice = it.message ?: "Не удалось загрузить уровни администратора" }
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = System.currentTimeMillis()
            val latest = store.read().refreshed(capacity, now)
            if (latest != saved) { saved = latest; store.write(latest) }
            if (!user?.stableId().isNullOrBlank()) {
                gamesRepository.pillMatchState().onSuccess { state ->
                    remoteLives = state.lives.current
                    remoteCapacity = state.lives.capacity
                    remoteNextLifeAt = state.lives.nextLifeAt
                    remoteCompleted = state.completedLevels
                }
            }
        }
    }

    fun persistGuestSession() {
        if (remoteMode || trial) return
        val snapshot = MatchThreeLocalSession(
            level = selectedLevel,
            board = board,
            obstacles = List(64) { obstacles[it]?.ordinal ?: -1 },
            moves = moves,
            collected = collected,
            target = target,
            targetColor = targetColor,
            hammer = boosterHammer,
            rainbow = boosterRainbow,
            shuffle = boosterShuffle,
        )
        val next = saved.copy(activeSession = snapshot)
        saved = next
        scope.launch { store.write(next) }
    }

    fun levelSpec(number: Int): MatchThreeEngine.Level {
        publishedLevels.firstOrNull { it.level == number }?.let { published ->
            val remoteObstacles = published.obstacles.mapIndexedNotNull { index, kind ->
                if (kind < 0) null else index to Obstacle.entries.getOrElse(kind) { Obstacle.ROCK }
            }.toMap()
            return MatchThreeEngine.Level(number, published.target, published.moves, published.targetColor, remoteObstacles, published.seed)
        }
        val custom = saved.customLevels.firstOrNull { it.number == number } ?: return MatchThreeEngine.level(number)
        val map = custom.obstacleKinds.mapIndexedNotNull { index, kind ->
            if (kind < 0) null else index to Obstacle.entries.getOrElse(kind) { Obstacle.ROCK }
        }.toMap()
        return MatchThreeEngine.Level(number, custom.target, custom.moves, custom.color, map, custom.seed)
    }

    fun startLevel(number: Int) {
        val spec = levelSpec(number)
        val refreshed = saved.refreshed(capacity, System.currentTimeMillis())
        if (user?.stableId().isNullOrBlank() && refreshed.lives <= 0 && saved.activeSession?.level != number) { notice = "Сердца восстановятся через ${refreshed.nextLifeMinutes(capacity)} мин."; return }
        if (user?.stableId().isNullOrBlank() && saved.activeSession?.level == number) {
            saved.activeSession?.let { session ->
                visualGems = emptyList()
                hintCells = emptySet()
                nudge = null
                rejectedCells = emptySet()
                board = session.board; obstacles = session.obstacles.mapIndexedNotNull { i, kind -> if (kind < 0) null else i to Obstacle.entries[kind] }.toMap()
                moves = session.moves; collected = session.collected; target = session.target; targetColor = session.targetColor
                boosterHammer = session.hammer; boosterRainbow = session.rainbow; boosterShuffle = session.shuffle
                selectedLevel = number; playing = true; finished = false; won = session.collected >= session.target; remoteMode = false
            }
            return
        }
        if (spec.number < 1 || spec.target !in 1..40 || spec.moves !in 1..35 || spec.color !in 0 until MatchThreeEngine.COLORS || spec.obstacles.keys.any { it !in 0 until 64 }) {
            notice = "Параметры главы повреждены. Откройте другую главу."; return
        }
        val initialBoard = runCatching { MatchThreeEngine.createBoard(spec.seed, spec.obstacles) }
            .getOrElse { notice = it.message ?: "Невозможно создать поле для этой расстановки."; return }
        selectedLevel = number
        notice = null
        remoteError = null
        activeBooster = ""
        finished = false
        won = false
        if (!user?.stableId().isNullOrBlank()) {
            saving = true
            scope.launch {
                try {
                    gamesRepository.startPillMatchLevel(number).onSuccess { response ->
                        val session = response.session
                        if (session == null) {
                            notice = "Сервер не открыл главу. Попробуйте ещё раз."
                            return@onSuccess
                        }
                        visualGems = emptyList()
                        hintCells = emptySet()
                        nudge = null
                        rejectedCells = emptySet()
                        board = session.board.map { color -> listOf("coral", "cyan", "lemon", "violet", "mint").indexOf(color).coerceAtLeast(0) }
                        obstacles = session.obstacles.mapIndexedNotNull { i, kind -> if (kind < 0) null else i to Obstacle.entries.getOrElse(kind) { Obstacle.ROCK } }.toMap()
                        moves = session.moves
                        collected = session.collected
                        target = session.target
                        targetColor = session.targetColor
                        boosterHammer = session.boosters.hammer
                        boosterRainbow = session.boosters.rainbow
                        boosterShuffle = session.boosters.shuffle
                        selectedLevel = session.level
                        remoteMode = true
                        remoteLives = response.lives.current
                        remoteCapacity = response.lives.capacity
                        remoteNextLifeAt = response.lives.nextLifeAt
                        remoteCompleted = response.completedLevels
                        playing = true
                    }.onFailure {
                        notice = it.message ?: "Не удалось открыть главу. Проверьте соединение и число сердец."
                    }
                } finally {
                    saving = false
                }
            }
        } else {
            visualGems = emptyList()
            hintCells = emptySet()
            nudge = null
            rejectedCells = emptySet()
            board = initialBoard
            obstacles = spec.obstacles
            target = spec.target
            targetColor = spec.color
            moves = spec.moves
            collected = 0
            boosterHammer = 2
            boosterRainbow = 1
            boosterShuffle = 1
            remoteMode = false
            playing = true
            val nextSave = refreshed.copy(lives = refreshed.lives - 1, lifeStamp = if (refreshed.lives == capacity) System.currentTimeMillis() else refreshed.lifeStamp)
            saved = nextSave
            persistGuestSession()
        }
    }

    fun playGardenFx(effects: List<GardenFx>) {
        if (effects.isEmpty()) return
        gardenFxPlay = GardenFxPlay(gardenFxPlay.token + 1, effects)
    }

    fun decodeObstacles(kinds: List<Int>): Map<Int, Obstacle> = kinds.mapIndexedNotNull { i, kind ->
        if (kind < 0) null else i to Obstacle.entries.getOrElse(kind) { Obstacle.ROCK }
    }.toMap()

    suspend fun applyRemoteState(state: ru.tomilo.lib.mobile.data.api.PillMatchActionDto, session: PillMatchSessionDto = state.session) {
        board = session.board.map { color -> listOf("coral", "cyan", "lemon", "violet", "mint").indexOf(color).coerceAtLeast(0) }
        obstacles = session.obstacles.mapIndexedNotNull { i, kind -> if (kind < 0) null else i to Obstacle.entries.getOrElse(kind) { Obstacle.ROCK } }.toMap()
        moves = session.moves; collected = session.collected; target = session.target; targetColor = session.targetColor
        boosterHammer = session.boosters.hammer; boosterRainbow = session.boosters.rainbow; boosterShuffle = session.boosters.shuffle
        remoteLives = state.lives.current; remoteCapacity = state.lives.capacity; remoteCompleted = state.completedLevels
        remoteNextLifeAt = state.lives.nextLifeAt
        if (collected >= target) {
            if (!won) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                playGardenFx(listOf(GardenFx(GardenFxKind.WIN, emptySet())))
                delay(if (reduceMotion) 0L else GARDEN_FX_MS.toLong())
            }
            won = true
            activeBooster = ""
        } else if (moves <= 0) {
            finished = true
            activeBooster = ""
            notice = "Ходы закончились. Начните главу заново."
        }
    }

    fun recordWin() {
        if (trial) {
            finished = true
            notice = "Проба пройдена. В прогресс игроков она не пишется."
            return
        }
        if (finished) return
        if (saving) return
        if (remoteMode) {
            scope.launch {
                saving = true
                gamesRepository.completePillMatchLevel(selectedLevel).onSuccess { result ->
                    finished = true
                    remoteError = null
                    remoteCompleted = (remoteCompleted + selectedLevel).distinct()
                    notice = if (result.awarded) "Уровень пройден! +${result.xpGained} опыта ✨" else "Уровень пройден! Новая глава уже открыта ✨"
                }.onFailure { remoteError = it.message ?: "Не удалось сохранить прохождение"; notice = "Не удалось сохранить победу. Повторите сохранение." }
                saving = false
            }
            return
        }
        finished = true
        val record = MatchThreeRecord(selectedLevel, System.currentTimeMillis())
        val next = saved.copy(records = (saved.records + record).distinctBy { it.level to it.at }.takeLast(2000), activeSession = null)
        saved = next
        scope.launch { store.write(next) }
        notice = "Уровень пройден! Новая глава уже открыта ✨"
    }

    fun decodeBoard(colors: List<String>) = colors.map { name ->
        listOf("coral", "cyan", "lemon", "violet", "mint").indexOf(name).coerceAtLeast(0)
    }

    fun rejectSwipe(from: Int, to: Int, message: String, wiggle: Boolean = true) {
        rejectedCells = setOf(from, to)
        if (wiggle) {
            nudge = from to to
            nudgeToken++
        }
        rejectionSequence++
        notice = message
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    suspend fun showGems(kind: MotionKind, gems: List<MatchThreeMotion.Sprite>, waitMs: Int) {
        visualGems = gems
        motionKind = kind
        motionPhase += 1
        delay(if (reduceMotion) 0L else waitMs.toLong())
    }

    suspend fun reshuffleGuestBoard() {
        val mixed = runCatching { MatchThreeEngine.shuffle(board, obstacles, System.nanoTime().toInt()) }.getOrNull()
        if (mixed != null) {
            board = mixed
            playGardenFx(listOf(GardenFx(GardenFxKind.SHUFFLE, emptySet())))
            showGems(MotionKind.ENTER, MatchThreeMotion.settle(mixed).map { it.copy(enter = true) }, MatchThreeMotion.POP_MS)
            notice = "Хода не было — самоцветы перемешаны"
            persistGuestSession()
        } else {
            finished = true
            notice = "Хода не осталось. Начните главу заново."
        }
    }

    fun rescueStuckGuest() {
        if (busy || dealing || saving || remoteMode || !playing || finished || won || moves <= 0 || board.size != 64) return
        if (boosterHammer + boosterRainbow + boosterShuffle > 0) return
        if (MatchThreeEngine.hint(board, obstacles) != null) return
        busy = true
        hintCells = emptySet()
        scope.launch {
            try {
                reshuffleGuestBoard()
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(board, obstacles, busy, dealing, playing, won, finished, activeBooster, remoteMode) {
        hintCells = emptySet()
        if (!playing || busy || dealing || won || finished || activeBooster.isNotEmpty() || board.size != 64) return@LaunchedEffect
        val hint = MatchThreeEngine.hint(board, obstacles)
        if (hint == null) {
            if (moves > 0) {
                val charges = boosterHammer + boosterRainbow + boosterShuffle
                if (charges == 0) {
                    if (remoteMode) {
                        finished = true
                        activeBooster = ""
                        notice = "Хода и бустеров не осталось. Начните главу заново."
                    } else {
                        rescueStuckGuest()
                    }
                } else {
                    notice = "Нет обмена. Молот, Радуга или Микс откроют ход."
                }
            }
            return@LaunchedEffect
        }
        delay(if (reduceMotion) 0L else 3200L)
        if (busy || dealing || activeBooster.isNotEmpty() || finished || won) return@LaunchedEffect
        hintCells = setOf(hint.first, hint.second)
    }

    LaunchedEffect(board, obstacles) {
        if (board.size != MatchThreeEngine.SIZE * MatchThreeEngine.SIZE) return@LaunchedEffect
        snapshotFlow { busy }.first { !it }
        if (busy || board.size != MatchThreeEngine.SIZE * MatchThreeEngine.SIZE) return@LaunchedEffect
        val current = visualGems
        val settled = MatchThreeMotion.settle(board, current)
        if (current.isEmpty() && playing && !won && !finished && !reduceMotion) {
            val token = dealToken + 1
            dealToken = token
            dealing = true
            try {
                showGems(MotionKind.ENTER, settled.map { it.copy(enter = true) }, MatchThreeMotion.POP_MS)
            } finally {
                if (dealToken == token) dealing = false
            }
            if (busy) return@LaunchedEffect
        }
        val resting = settled.map { it.copy(fromCell = it.cell, pop = false, enter = false, spawnRows = 0) }
        val same = current.isNotEmpty() && current.size == resting.size && current.zip(resting).all { (sprite, rest) ->
            sprite.id == rest.id && sprite.cell == rest.cell && sprite.color == rest.color && sprite.fromCell == rest.cell && !sprite.pop && !sprite.enter
        }
        if (!same) {
            visualGems = resting
            motionKind = MotionKind.SETTLE
            motionPhase += 1
        }
    }

    suspend fun playSwap(from: Int, to: Int) {
        if (visualGems.isEmpty()) visualGems = MatchThreeMotion.settle(board)
        val swapped = visualGems.map { gem ->
            when (gem.cell) {
                from -> gem.copy(fromCell = from, cell = to, pop = false, enter = false, spawnRows = 0)
                to -> gem.copy(fromCell = to, cell = from, pop = false, enter = false, spawnRows = 0)
                else -> gem.copy(fromCell = gem.cell, cell = gem.cell, pop = false, enter = false, spawnRows = 0)
            }
        }
        showGems(MotionKind.SWAP, swapped, MatchThreeMotion.SWAP_MS)
        visualGems = swapped.map { it.copy(fromCell = it.cell) }
    }

    suspend fun playSteps(startBoard: List<Int>, steps: List<MatchThreeEngine.CascadeStep>, score: Boolean, accent: GardenFx? = null) {
        var look = startBoard
        var gems = visualGems.ifEmpty { MatchThreeMotion.settle(startBoard) }
        var covered = obstacles
        var first = true
        for (step in steps) {
            if (score) {
                val gained = step.matched.count { look.getOrNull(it) == targetColor }
                if (gained > 0) collected = (collected + gained).coerceAtMost(target)
            }
            if (step.matched.isNotEmpty()) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            val wave = MatchThreeMotion.wave(look, step.matched, step.board, step.obstacles, gems)
            val broken = covered.filter { (cell, kind) ->
                step.obstacles[cell] != kind && (kind == Obstacle.ICE || kind == Obstacle.CHAIN)
            }
            obstacles = step.obstacles
            playGardenFx(buildList {
                if (step.matched.isNotEmpty()) add(GardenFx(GardenFxKind.MATCH, step.matched))
                broken.forEach { (cell, kind) ->
                    add(GardenFx(if (kind == Obstacle.ICE) GardenFxKind.ICE else GardenFxKind.CHAIN, setOf(cell)))
                }
                if (first && accent != null) {
                    val cells = accent.cells.ifEmpty { step.matched }
                    if (cells.isNotEmpty()) add(accent.copy(cells = cells))
                }
            })
            showGems(MotionKind.POP, wave.popping, MatchThreeMotion.POP_MS)
            showGems(MotionKind.FALL, wave.landing, MatchThreeMotion.FALL_MS)
            look = step.board
            gems = wave.landing.map { it.copy(fromCell = it.cell, pop = false, enter = false, spawnRows = 0) }
            covered = step.obstacles
            first = false
        }
        visualGems = gems
        motionKind = MotionKind.SETTLE
    }

    suspend fun playCrossfade(before: List<Int>, after: List<Int>, extra: List<GardenFx> = emptyList(), burst: Boolean = true) {
        val wave = MatchThreeMotion.crossfade(before, after, visualGems.ifEmpty { MatchThreeMotion.settle(before) })
        if (wave.popping.any { it.pop }) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val popping = wave.popping.mapNotNull { gem -> gem.cell.takeIf { gem.pop && it in 0 until 64 } }.toSet()
        playGardenFx(buildList {
            if (burst && popping.isNotEmpty()) add(GardenFx(GardenFxKind.MATCH, popping))
            extra.forEach { fx ->
                val cells = if (fx.kind == GardenFxKind.SHUFFLE || fx.kind == GardenFxKind.WIN) fx.cells else fx.cells.ifEmpty { popping }
                if (fx.kind == GardenFxKind.SHUFFLE || fx.kind == GardenFxKind.WIN || cells.isNotEmpty()) add(fx.copy(cells = cells))
            }
        })
        showGems(MotionKind.POP, wave.popping, MatchThreeMotion.POP_MS)
        showGems(MotionKind.ENTER, wave.landing, MatchThreeMotion.POP_MS)
        visualGems = wave.landing.map { it.copy(fromCell = it.cell, pop = false, enter = false, spawnRows = 0) }
        motionKind = MotionKind.SETTLE
    }

    fun noteIfStuck() {
        if (won || finished || moves <= 0 || board.size != 64) return
        if (MatchThreeEngine.hint(board, obstacles) != null) return
        val charges = boosterHammer + boosterRainbow + boosterShuffle
        if (charges == 0) {
            finished = true
            activeBooster = ""
            notice = "Хода и бустеров не осталось. Начните главу заново."
        } else {
            notice = "Нет обмена. Молот, Радуга или Микс откроют ход."
        }
    }

    suspend fun settleGuest(gained: Int, consumesMove: Boolean) {
        if (consumesMove) moves = (moves - 1).coerceAtLeast(0)
        collected = (collected + gained).coerceAtMost(target)
        activeBooster = ""
        when {
            collected >= target -> {
                if (!won) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    playGardenFx(listOf(GardenFx(GardenFxKind.WIN, emptySet())))
                    delay(if (reduceMotion) 0L else GARDEN_FX_MS.toLong())
                }
                won = true
                persistGuestSession()
            }
            moves <= 0 -> {
                finished = true
                if (trial) {
                    notice = "Ходы пробы закончились."
                } else {
                    val next = saved.copy(activeSession = null)
                    saved = next
                    scope.launch { store.write(next) }
                    notice = "Ходы закончились. Начните главу заново."
                }
            }
            MatchThreeEngine.hint(board, obstacles) == null && boosterHammer + boosterRainbow + boosterShuffle == 0 -> reshuffleGuestBoard()
            MatchThreeEngine.hint(board, obstacles) == null -> {
                notice = "Нет обмена. Молот, Радуга или Микс откроют ход."
                persistGuestSession()
            }
            else -> persistGuestSession()
        }
    }

    fun armBooster(name: String) {
        if (busy || dealing || saving || finished || won || moves <= 0) return
        activeBooster = if (activeBooster == name) "" else name
        hintCells = emptySet()
        notice = when (activeBooster) {
            "hammer" -> "Молот: коснитесь клетки"
            "rainbow" -> "Радуга: коснитесь самоцвета"
            else -> null
        }
    }

    fun playCell(index: Int) {
        if (!playing || finished || won || moves <= 0 || saving || busy || dealing) return
        if (activeBooster.isEmpty()) return
        if (index !in board.indices || activeBooster == "rainbow" && index in obstacles) {
            notice = "Выберите клетку с самоцветом"
            return
        }
        if (activeBooster == "hammer" && boosterHammer <= 0 || activeBooster == "rainbow" && boosterRainbow <= 0) {
            notice = "Заряды бустера закончились"
            activeBooster = ""
            return
        }
        val booster = activeBooster
        val startBoard = board
        busy = true
        hintCells = emptySet()
        if (!remoteMode) notice = null
        scope.launch {
            try {
                if (remoteMode) {
                    saving = true
                    notice = "Применяем бустер…"
                    gamesRepository.pillMatchBooster(booster, index).onSuccess { action ->
                        val nextBoard = decodeBoard(action.session.board)
                        val nextObstacles = decodeObstacles(action.session.obstacles)
                        val broken = obstacles.mapNotNull { (cell, kind) ->
                            if (nextObstacles[cell] == kind) null
                            else when (kind) {
                                Obstacle.ICE -> GardenFx(GardenFxKind.ICE, setOf(cell))
                                Obstacle.CHAIN -> GardenFx(GardenFxKind.CHAIN, setOf(cell))
                                Obstacle.ROCK -> GardenFx(GardenFxKind.ROCK, setOf(cell))
                            }
                        }
                        val accent = when (booster) {
                            "hammer" -> GardenFx(GardenFxKind.HAMMER, setOf(index))
                            "rainbow" -> GardenFx(GardenFxKind.RAINBOW, emptySet())
                            else -> null
                        }
                        playCrossfade(startBoard, nextBoard, extra = listOfNotNull(accent) + broken)
                        applyRemoteState(action)
                        if (notice == "Применяем бустер…") notice = null
                        noteIfStuck()
                    }.onFailure { notice = it.message ?: "Бустер не сработал. Попробуйте ещё раз." }
                    saving = false
                } else if (booster == "hammer" && boosterHammer > 0) {
                    val result = MatchThreeEngine.clearAt(startBoard, obstacles, index, targetColor, System.nanoTime().toInt())
                    if (result != null) {
                        boosterHammer--
                        if (result.steps.isEmpty()) {
                            val broken = obstacles[index]
                            obstacles = result.obstacles
                            playGardenFx(buildList {
                                when (broken) {
                                    Obstacle.ROCK -> add(GardenFx(GardenFxKind.ROCK, setOf(index)))
                                    Obstacle.ICE -> add(GardenFx(GardenFxKind.ICE, setOf(index)))
                                    Obstacle.CHAIN -> add(GardenFx(GardenFxKind.CHAIN, setOf(index)))
                                    null -> Unit
                                }
                                add(GardenFx(GardenFxKind.HAMMER, setOf(index)))
                            })
                            notice = "Препятствие снято"
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } else playSteps(startBoard, result.steps, score = true, accent = GardenFx(GardenFxKind.HAMMER, setOf(index)))
                        board = result.board
                        obstacles = result.obstacles
                        settleGuest(0, consumesMove = false)
                    }
                } else if (booster == "rainbow" && boosterRainbow > 0 && index !in obstacles) {
                    val color = startBoard.getOrNull(index)
                    if (color != null) {
                        boosterRainbow--
                        val result = MatchThreeEngine.clearColor(startBoard, obstacles, color, targetColor, System.nanoTime().toInt())
                        playSteps(startBoard, result.steps, score = true, accent = GardenFx(GardenFxKind.RAINBOW, emptySet()))
                        board = result.board
                        obstacles = result.obstacles
                        settleGuest(0, consumesMove = false)
                    }
                }
            } finally {
                busy = false
                activeBooster = ""
            }
        }
    }

    fun playSwipe(from: Int, to: Int) {
        if (!playing || finished || won || moves <= 0 || saving || busy || dealing || activeBooster.isNotEmpty()) return
        if (!MatchThreeEngine.adjacent(from, to) || from in obstacles || to in obstacles) {
            rejectSwipe(from, to, "Камень, лёд и цепь блокируют обмен", wiggle = false)
            return
        }
        val startBoard = board
        val startObstacles = obstacles
        val guestMove = if (remoteMode) null else MatchThreeEngine.swap(startBoard, startObstacles, from, to, targetColor, System.nanoTime().toInt())
        if (!remoteMode && guestMove == null) {
            rejectSwipe(from, to, "Так ряд не соберётся. Нужны три одинаковых самоцвета.")
            return
        }
        if (!remoteMode) {
            moves = (moves - 1).coerceAtLeast(0)
            notice = null
        }
        busy = true
        hintCells = emptySet()
        scope.launch {
            try {
                playSwap(from, to)
                val swapped = startBoard.toMutableList().also { cells ->
                    val held = cells[from]
                    cells[from] = cells[to]
                    cells[to] = held
                }
                if (remoteMode) {
                    saving = true
                    notice = "Считаем ход…"
                    gamesRepository.pillMatchMove(from, to).onSuccess { action ->
                        if (!action.validMove) {
                            playSwap(to, from)
                            rejectSwipe(from, to, "Нужно собрать три или больше самоцветов.", wiggle = false)
                        } else {
                            playCrossfade(swapped, decodeBoard(action.session.board))
                            applyRemoteState(action)
                            if (notice == "Считаем ход…") notice = null
                            noteIfStuck()
                        }
                    }.onFailure {
                        playSwap(to, from)
                        notice = it.message ?: "Не удалось отправить ход"
                    }
                    saving = false
                } else if (guestMove != null) {
                    playSteps(swapped, guestMove.steps, score = true)
                    board = guestMove.board
                    obstacles = guestMove.obstacles
                    settleGuest(0, consumesMove = false)
                }
            } finally {
                busy = false
            }
        }
    }

    fun mixBoard() {
        if (busy || dealing || saving || finished || won || moves <= 0 || boosterShuffle <= 0) return
        busy = true
        hintCells = emptySet()
        val startBoard = board
        scope.launch {
            try {
                if (remoteMode) {
                    saving = true
                    notice = "Перемешиваем…"
                    gamesRepository.pillMatchBooster("shuffle").onSuccess { action ->
                        playCrossfade(startBoard, decodeBoard(action.session.board), extra = listOf(GardenFx(GardenFxKind.SHUFFLE, emptySet())), burst = false)
                        applyRemoteState(action)
                        notice = "Самоцветы перемешаны"
                        noteIfStuck()
                    }.onFailure { notice = it.message ?: "Не удалось перемешать поле" }
                    saving = false
                } else {
                    val mixed = runCatching { MatchThreeEngine.shuffle(startBoard, obstacles, System.nanoTime().toInt()) }.getOrElse {
                        notice = it.message ?: "Невозможно перемешать эту расстановку."
                        return@launch
                    }
                    boosterShuffle--
                    board = mixed
                    playGardenFx(listOf(GardenFx(GardenFxKind.SHUFFLE, emptySet())))
                    showGems(MotionKind.ENTER, MatchThreeMotion.settle(mixed).map { gem -> gem.copy(enter = true) }, MatchThreeMotion.POP_MS)
                    notice = "Самоцветы перемешаны"
                    persistGuestSession()
                }
            } finally {
                busy = false
            }
        }
    }

    fun paintEditorCell(cell: Int) {
        val updated = editorObstacles.toMutableMap()
        if (editorErase || updated[cell] == editorObstacle) updated.remove(cell)
        else {
            if (cell !in updated && updated.size >= 24) {
                notice = "На поле можно разместить не больше 24 препятствий"
                return
            }
            updated[cell] = editorObstacle
        }
        editorObstacles = updated
        editorObstacleCount = updated.size
        editorLevelPinned = true
        notice = null
    }

    fun loadEditorLevel(level: PillMatchAdminLevelDto) {
        editorEditingLevel = level.level
        editorLevelNumber = level.level
        editorLevelPinned = true
        editorTarget = level.target.coerceIn(3, 60)
        editorMoves = level.moves.coerceIn(5, 60)
        editorColor = level.targetColor.coerceIn(0, 4)
        editorSeed = level.seed.coerceAtLeast(0)
        editorPublished = level.published
        editorObstacles = level.obstacles.mapIndexedNotNull { index, kind ->
            if (kind < 0) null else index to Obstacle.entries.getOrElse(kind) { Obstacle.ROCK }
        }.toMap()
        editorObstacleCount = editorObstacles.size
        editorErase = false
        notice = if (level.published) "Глава ${level.level} открыта" else "Черновик главы ${level.level} открыт"
    }

    fun startNewEditorLevel() {
        editorEditingLevel = null
        editorLevelNumber = (adminLevels.maxOfOrNull { it.level } ?: 0) + 1
        editorLevelPinned = true
        editorPublished = true
        editorObstacles = emptyMap()
        editorObstacleCount = 0
        editorErase = false
        editorSeed = (System.nanoTime().toInt() and Int.MAX_VALUE).coerceAtLeast(1)
        notice = "Новая глава $editorLevelNumber. Нарисуйте поле или сгенерируйте его."
    }

    fun copyEditorLevel() {
        val source = editorLevelNumber
        editorEditingLevel = null
        editorLevelNumber = (adminLevels.maxOfOrNull { it.level } ?: 0) + 1
        editorLevelPinned = true
        editorPublished = false
        notice = "Копия главы $source станет главой $editorLevelNumber и пока останется черновиком."
    }

    fun generateEditor(fullChapter: Boolean) {
        if (saving) return
        val count = if (fullChapter) (editorLevelNumber / 2).coerceIn(0, 16) else editorObstacleCount.coerceIn(0, 24)
        val startSeed = if (fullChapter) (editorLevelNumber.coerceAtLeast(1) * 431) else editorSeed.coerceAtLeast(0)
        val kind = if (fullChapter || editorMix) null else editorObstacle
        val drafted = if (fullChapter) MatchThreeEngine.level(editorLevelNumber.coerceAtLeast(1)) else null
        saving = true
        scope.launch {
            val found = withContext(Dispatchers.Default) { MatchThreeEngine.generatePlayable(startSeed, count, kind) }
            if (found == null) {
                notice = "Не нашлось расстановки с ходом. Уменьшите число препятствий."
            } else {
                drafted?.let { spec ->
                    editorTarget = spec.target.coerceIn(3, 60)
                    editorMoves = spec.moves.coerceIn(5, 60)
                    editorColor = spec.color
                }
                editorSeed = found.first
                editorObstacles = found.second
                editorObstacleCount = found.second.size
                editorLevelPinned = true
                notice = "Глава $editorLevelNumber собрана. Seed ${found.first}, препятствий ${found.second.size}."
            }
            saving = false
        }
    }

    fun saveEditorLevel() {
        val levelNo = editorLevelNumber.coerceIn(1, 100_000)
        editorSeed = editorSeed.coerceAtLeast(0)
        if (editorTarget !in 3..60 || editorMoves !in 5..60 || editorColor !in 0..4) {
            notice = "Цель от 3 до 60, ходы от 5 до 60."
            return
        }
        if (!MatchThreeEngine.acceptsLayout(editorSeed, editorObstacles)) {
            notice = "Эту расстановку нельзя сохранить: нет допустимого хода."
            return
        }
        val request = PillMatchAdminLevelRequest(
            level = levelNo,
            target = editorTarget,
            moves = editorMoves,
            targetColor = editorColor,
            obstacles = List(64) { editorObstacles[it]?.ordinal ?: -1 },
            seed = editorSeed,
            published = editorPublished,
        )
        scope.launch {
            saving = true
            gamesRepository.savePillMatchAdminLevel(request)
                .onSuccess { remote ->
                    adminLevels = (adminLevels.filterNot { it.level == remote.level } + remote).sortedBy { it.level }
                    publishedLevels = if (remote.published) {
                        (publishedLevels.filterNot { it.level == remote.level } + remote).sortedBy { it.level }
                    } else {
                        publishedLevels.filterNot { it.level == remote.level }
                    }
                    val local = MatchThreeCustomLevel(remote.level, remote.target, remote.moves, remote.targetColor, remote.obstacles, remote.seed)
                    saved = saved.copy(
                        customLevels = if (remote.published) {
                            (saved.customLevels.filterNot { it.number == remote.level } + local).takeLast(100)
                        } else {
                            saved.customLevels.filterNot { it.number == remote.level }
                        },
                    )
                    store.write(saved)
                    editorEditingLevel = remote.level
                    editorLevelNumber = remote.level
                    editorLevelPinned = true
                    notice = if (remote.published) "Глава ${remote.level} опубликована" else "Глава ${remote.level} сохранена как черновик"
                }
                .onFailure { notice = it.message ?: "Не удалось сохранить главу" }
            saving = false
        }
    }

    fun deleteEditorLevel(level: Int) {
        scope.launch {
            saving = true
            gamesRepository.deletePillMatchAdminLevel(level)
                .onSuccess {
                    adminLevels = adminLevels.filterNot { it.level == level }
                    publishedLevels = publishedLevels.filterNot { it.level == level }
                    saved = saved.copy(customLevels = saved.customLevels.filterNot { it.number == level })
                    store.write(saved)
                    if (editorLevelNumber == level) startNewEditorLevel()
                    notice = "Глава $level удалена"
                }
                .onFailure { notice = it.message ?: "Не удалось удалить главу" }
            saving = false
            pendingDelete = null
        }
    }

    fun beginTrial() {
        val kinds = IntArray(64) { editorObstacles[it]?.ordinal ?: -1 }
        val clientOk = runCatching { MatchThreeEngine.createBoard(editorSeed, editorObstacles) }.isSuccess
        if (editorObstacles.size > 24 || !clientOk || !MatchThreeEngine.serverBoardPlayable(editorSeed, kinds)) {
            notice = "Проба доступна, когда у расстановки есть ход."
            return
        }
        trial = true
        remoteMode = false
        visualGems = emptyList()
        hintCells = emptySet()
        nudge = null
        rejectedCells = emptySet()
        board = MatchThreeEngine.serverOpening(editorSeed, kinds)
        obstacles = editorObstacles
        moves = editorMoves
        collected = 0
        target = editorTarget
        targetColor = editorColor
        boosterHammer = 2
        boosterRainbow = 1
        boosterShuffle = 1
        activeBooster = ""
        finished = false
        won = false
        selectedLevel = editorLevelNumber
        notice = "Проба главы $editorLevelNumber. Сердце не тратится."
        selectedTab = MatchThreeTab.PLAY
        playing = true
    }

    fun endTrial() {
        trial = false
        playing = false
        finished = false
        won = false
        visualGems = emptyList()
        activeBooster = ""
        remoteMode = !user?.stableId().isNullOrBlank()
        selectedTab = MatchThreeTab.EDITOR
        notice = "Черновик главы $editorLevelNumber на месте"
    }

    val activePlay = selectedTab == MatchThreeTab.PLAY && playing
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF101624),
        topBar = {
          if (!activePlay && !(selectedTab == MatchThreeTab.EDITOR && admin)) {
            TopAppBar(
                title = { Text("Сад созвездий", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF101624),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
          }
        },
    ) { screenPadding ->
      Box(Modifier.fillMaxSize().padding(screenPadding)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x8817243B), Color(0xC6171A32), Color(0xE0101624)))))
        StarryBackdrop(gardenBackdrop, Modifier.fillMaxSize())
        if (activePlay) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (trial) endTrial() else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (trial) "К редактору" else "Назад", tint = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Сад созвездий", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(if (trial) "ПРОБА · ГЛАВА $selectedLevel" else "ГЛАВА $selectedLevel", color = Color(0xFFFFD689), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    }
                    if (!trial) IconButton(onClick = { selectedTab = MatchThreeTab.RANK }) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Лидеры", tint = Color(0xFFFFD689))
                    }
                    if (admin && !trial) IconButton(onClick = { selectedTab = MatchThreeTab.EDITOR }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Редактор уровней", tint = Color(0xFFFFD689))
                    }
                    StatPill("♥ ${if (remoteLives >= 0) remoteLives else saved.lives}/${if (remoteLives >= 0) remoteCapacity else capacity}", if (premium) Color(0xFFFF84A4) else Color(0xFFDA7894))
                }
                MissionPanel(color = targetColor, collected = collected, target = target, moves = moves)
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val boardWidth = minOf(maxWidth, maxHeight)
                    val boardHeight = minOf(maxHeight, boardWidth * 1.22f)
                    Box(Modifier.width(boardWidth).height(boardHeight)) {
                        MatchBoard(
                            board = board,
                            obstacles = obstacles,
                            gems = visualGems,
                            motionPhase = motionPhase,
                            motionKind = motionKind,
                            hintCells = hintCells,
                            nudge = nudge,
                            nudgeToken = nudgeToken,
                            busy = busy || saving || dealing,
                            rejectedCells = rejectedCells,
                            fx = gardenFxPlay.effects,
                            fxToken = gardenFxPlay.token,
                            allowTap = activeBooster.isNotEmpty() && !busy && !saving,
                            onCell = ::playCell,
                            onSwipe = ::playSwipe,
                            modifier = Modifier.fillMaxSize(),
                            stretchHeight = true,
                        )
                        if (won || finished) {
                            val dismissRipple = remember { MutableInteractionSource() }
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .background(Color(0xCC101624))
                                    .clickable(interactionSource = dismissRipple, indication = null) {},
                                contentAlignment = Alignment.Center,
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF20263A)),
                                    shape = RoundedCornerShape(22.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFFD689).copy(alpha = .45f)),
                                ) {
                                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            when {
                                                won -> "Созвездие собрано"
                                                moves <= 0 -> "Ходы закончились"
                                                else -> "Нет обмена"
                                            },
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            when {
                                                trial && won -> "Проба собрана. Прогресс игроков не меняется."
                                                trial -> notice ?: "Проба окончена."
                                                finished && won -> "Глава $selectedLevel открыла следующую."
                                                won -> "Сохраните прохождение, чтобы идти дальше."
                                                else -> notice ?: "Начните главу заново."
                                            },
                                            color = Color(0xFFD5DCEC),
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center,
                                        )
                                        if (trial) {
                                            Button(onClick = ::endTrial, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("К редактору") }
                                            OutlinedButton(onClick = ::beginTrial, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("Ещё раз") }
                                        } else if (finished && won) {
                                            Button(onClick = { playing = false; selectedLevel = nextUnlockedLevel }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("Следующая глава") }
                                        } else if (won) {
                                            Button(onClick = ::recordWin, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                                                Text(if (saving) "Сохраняем…" else if (remoteError != null) "Повторить сохранение" else "Завершить главу")
                                            }
                                        } else {
                                            OutlinedButton(onClick = { playing = false; startLevel(selectedLevel) }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("Повторить главу") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val boostersReady = !saving && !busy && !dealing && !finished && !won && moves > 0
                    BoosterButton(ru.tomilo.lib.mobile.R.drawable.pill_booster_hammer, "Молот", boosterHammer, activeBooster == "hammer", Modifier.weight(1f), enabled = boostersReady, onClick = { armBooster("hammer") })
                    BoosterButton(ru.tomilo.lib.mobile.R.drawable.pill_booster_rainbow, "Радуга", boosterRainbow, activeBooster == "rainbow", Modifier.weight(1f), enabled = boostersReady, onClick = { armBooster("rainbow") })
                    BoosterButton(ru.tomilo.lib.mobile.R.drawable.pill_booster_shuffle, "Микс", boosterShuffle, false, Modifier.weight(1f), enabled = boostersReady, onClick = ::mixBoard)
                }
                Text(
                    notice ?: "Перетащите самоцвет к соседнему",
                    Modifier.fillMaxWidth().height(32.dp),
                    textAlign = TextAlign.Center,
                    color = if (notice == null) Color(0xFF8E9BB3) else Color(0xFFFFDC9C),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                )
            }
        } else if (selectedTab == MatchThreeTab.EDITOR && admin) {
            val layoutBoard = remember(editorSeed, editorObstacles) {
                runCatching { MatchThreeEngine.createBoard(editorSeed, editorObstacles) }.getOrNull()
            }
            val serverOk = remember(editorSeed, editorObstacles) {
                editorObstacles.size <= 24 && MatchThreeEngine.serverBoardPlayable(editorSeed, IntArray(64) { editorObstacles[it]?.ordinal ?: -1 })
            }
            val canSave = layoutBoard != null && serverOk && !saving
            val replacing = adminLevels.any { it.level == editorLevelNumber }
            val layoutMessage = when {
                editorObstacles.size > 24 -> "Максимум 24 препятствия."
                layoutBoard == null -> "Нет хода. Уберите препятствия или соберите поле."
                !serverOk -> "Сервер не примет эту расстановку."
                replacing -> "Глава $editorLevelNumber · замена · препятствий ${editorObstacles.size}"
                else -> "Глава $editorLevelNumber · новая · препятствий ${editorObstacles.size}"
            }
            val fieldPlan = editorObstacleCount.coerceIn(0, 24)
            Column(
                Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
                    }
                    Column(Modifier.weight(1f).padding(end = 4.dp)) {
                        Text("Мастер уровней", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            notice ?: layoutMessage,
                            color = if (notice != null) Color(0xFFFFD689) else if (canSave) Color(0xFF9DDCB4) else Color(0xFFFF99A9),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { selectedTab = MatchThreeTab.PLAY }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Игра", tint = Color(0xFFFFD689))
                    }
                    IconButton(onClick = { selectedTab = MatchThreeTab.RANK }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Лидеры", tint = Color(0xFFFFD689))
                    }
                }
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF10192A).copy(alpha = .94f)).border(1.dp, Color(0xFFFFD689).copy(alpha = .35f), RoundedCornerShape(16.dp)).padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    EditorNudge("−") { editorLevelNumber = (editorLevelNumber - 1).coerceAtLeast(1); editorLevelPinned = true }
                    Text(
                        "$editorLevelNumber",
                        Modifier.widthIn(min = 24.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    EditorNudge("+") { editorLevelNumber = (editorLevelNumber + 1).coerceAtMost(100_000); editorLevelPinned = true }
                    BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                        Row(
                            Modifier.fillMaxHeight().horizontalScroll(rememberScrollState()).widthIn(min = maxWidth),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                        ) {
                            EditorTextButton("Новая", onClick = ::startNewEditorLevel)
                            EditorTextButton("Копия", onClick = ::copyEditorLevel)
                            EditorTextButton(if (editorPublished) "Игрокам" else "Черновик", selected = editorPublished) { editorPublished = !editorPublished }
                            if (replacing) EditorNudge("×", description = "Удалить главу") { pendingDelete = editorLevelNumber }
                        }
                    }
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val side = minOf(maxWidth, maxHeight)
                    MatchBoard(
                        board = layoutBoard ?: List(64) { index -> index % 5 },
                        obstacles = editorObstacles,
                        editor = true,
                        allowTap = !saving,
                        onCell = ::paintEditorCell,
                        onSwipe = { _, _ -> },
                        modifier = Modifier.size(side).graphicsLayer { alpha = if (layoutBoard == null) 0.45f else 1f },
                        stretchHeight = true,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().height(40.dp).horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    gemColors.indices.forEach { index ->
                        EditorBrush(selected = editorColor == index, description = "Цвет цели: ${colorName(index)}", onClick = { editorColor = index }) {
                            Image(painter = painterResource(gemSprites[index]), contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                    Box(Modifier.padding(horizontal = 2.dp).width(1.dp).height(22.dp).background(Color(0xFFFFD689).copy(alpha = .45f)))
                    Obstacle.entries.forEach { obs ->
                        val icon = when (obs) {
                            Obstacle.ROCK -> ru.tomilo.lib.mobile.R.drawable.pill_obstacle_rock
                            Obstacle.ICE -> ru.tomilo.lib.mobile.R.drawable.pill_obstacle_ice
                            Obstacle.CHAIN -> ru.tomilo.lib.mobile.R.drawable.pill_obstacle_chain
                        }
                        EditorBrush(selected = !editorErase && editorObstacle == obs, description = obsLabel(obs), onClick = { editorErase = false; editorObstacle = obs }) {
                            Image(painter = painterResource(icon), contentDescription = null, modifier = Modifier.padding(3.dp).fillMaxSize())
                        }
                    }
                    EditorBrush(selected = editorErase, description = "Ластик", onClick = { editorErase = !editorErase }) {
                        Text("✕", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    EditorBrush(selected = false, description = "Очистить поле", onClick = { editorObstacles = emptyMap(); editorObstacleCount = 0; editorLevelPinned = true; notice = null }) {
                        Text("∅", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                BoxWithConstraints(Modifier.fillMaxWidth().height(52.dp)) {
                    Row(
                        Modifier.fillMaxHeight().horizontalScroll(rememberScrollState()).widthIn(min = maxWidth),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        EditorMeter("Цель", editorTarget, onMinus = { editorTarget = (editorTarget - 1).coerceAtLeast(3) }, onPlus = { editorTarget = (editorTarget + 1).coerceAtMost(60) })
                        EditorMeter("Ходы", editorMoves, onMinus = { editorMoves = (editorMoves - 1).coerceAtLeast(5) }, onPlus = { editorMoves = (editorMoves + 1).coerceAtMost(60) })
                        EditorMeter("Поле", fieldPlan, onMinus = { editorObstacleCount = (editorObstacleCount - 1).coerceAtLeast(0) }, onPlus = { editorObstacleCount = (editorObstacleCount + 1).coerceAtMost(24) }, warn = fieldPlan != editorObstacles.size.coerceAtMost(24))
                        EditorTextButton(if (editorMix) "Смесь" else "Один тип", selected = editorMix) { editorMix = !editorMix }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EditorAction("Собрать", filled = true, enabled = !saving, modifier = Modifier.weight(1f)) { generateEditor(fullChapter = true) }
                    EditorAction("Заново", filled = false, enabled = !saving, modifier = Modifier.weight(1f)) { generateEditor(fullChapter = false) }
                    EditorAction("Проба", filled = false, enabled = canSave, modifier = Modifier.weight(1f)) { beginTrial() }
                    EditorAction(if (replacing) "Сохранить" else "Создать", filled = true, enabled = canSave, modifier = Modifier.weight(1f)) { saveEditorLevel() }
                }
                if (adminLevels.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                    ) {
                        items(adminLevels, key = { it.level }) { level ->
                            ChapterDot(number = level.level, selected = level.level == editorLevelNumber, published = level.published, onClick = { loadEditorLevel(level) })
                        }
                    }
                }
                pendingDelete?.let { level ->
                    AlertDialog(
                        onDismissRequest = { if (!saving) pendingDelete = null },
                        containerColor = Color(0xFF20263A),
                        titleContentColor = Color.White,
                        textContentColor = Color(0xFFD5DCEC),
                        title = { Text("Удалить главу $level?") },
                        text = { Text("Игроки снова получат автоматическую расстановку этой главы.") },
                        confirmButton = { TextButton(onClick = { deleteEditorLevel(level) }, enabled = !saving) { Text("Удалить", color = Color(0xFFFF99A9)) } },
                        dismissButton = { TextButton(onClick = { pendingDelete = null }, enabled = !saving) { Text("Оставить", color = Color(0xFFFFD689)) } },
                    )
                }
            }
        } else {
        LazyColumn(
            Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 14.dp, top = 10.dp, end = 14.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF11192C).copy(alpha = .76f)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(MatchThreeTab.PLAY to "✦ Играть", MatchThreeTab.RANK to "♛ Лидеры").forEach { (tab, label) ->
                        FilterChip(selected = selectedTab == tab, onClick = { selectedTab = tab }, label = { Text(label) }, modifier = Modifier.weight(1f))
                    }
                    if (admin) FilterChip(selected = selectedTab == MatchThreeTab.EDITOR, onClick = { selectedTab = MatchThreeTab.EDITOR }, label = { Text("⚙ Уровни") })
                }
            }
            when (selectedTab) {
                MatchThreeTab.PLAY -> {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GardenBackdrop.entries.forEach { backdrop ->
                                FilterChip(selected = gardenBackdrop == backdrop, onClick = { gardenBackdrop = backdrop }, label = { Text(backdrop.title, style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    item {
                        MatchThreeHero(
                            level = selectedLevel, lives = if (remoteLives >= 0) remoteLives else saved.lives, capacity = if (remoteLives >= 0) remoteCapacity else capacity, premium = premium, minutes = if (remoteLives >= 0) remoteMinutes else saved.nextLifeMinutes(capacity),
                            completed = (saved.records.map { it.level } + remoteCompleted).distinct().size, onPremium = onOpenPremium,
                        )
                    }
                    if (!playing) item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF20263A).copy(alpha = .92f)), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color(0xFFB7C5EF).copy(alpha = .15f))) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                                Text("САД СОЗВЕЗДИЙ", color = Color(0xFFFFD689), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                Text("Глава $selectedLevel · ${movesLabel(levelSpec(selectedLevel).moves)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Соберите ${levelSpec(selectedLevel).target} ${colorName(levelSpec(selectedLevel).color)} фишек. Свайпните самоцвет к соседнему. Камень блокирует клетку, лёд и цепь исчезают, если рядом очистить совпадение. Молот убирает клетку, Радуга очищает цвет, Микс перемешивает поле. Бустеры не тратят ход.", color = Color(0xFFB8C3D8), style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Text("🪨 Камень", color = Color(0xFFE4D5C5), style = MaterialTheme.typography.labelSmall)
                                    Text("❄ Лёд", color = Color(0xFFADF1FF), style = MaterialTheme.typography.labelSmall)
                                    Text("⛓ Цепь", color = Color(0xFFFFD481), style = MaterialTheme.typography.labelSmall)
                                }
                                val availableLives = if (remoteLives >= 0) remoteLives else saved.lives
                                Button(onClick = { startLevel(selectedLevel) }, enabled = availableLives > 0 && !saving, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp)) {
                                    Text(when {
                                        saving -> "Открываем главу…"
                                        availableLives > 0 -> "Войти в сад  ·  ♥ $availableLives"
                                        else -> "Ждём новое сердце"
                                    })
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Пройдено: ${(saved.records.map { it.level } + remoteCompleted).distinct().size} глав", color = Color(0xFFBDC7DB), style = MaterialTheme.typography.labelMedium)
                                    TextButton(onClick = { selectedLevel = (selectedLevel - 1).coerceAtLeast(1) }) { Text("‹ Глава") }
                                    TextButton(onClick = { selectedLevel = (selectedLevel + 1).coerceAtMost(furthestCompleted + 1) }) { Text("Следующая ›") }
                                }
                                notice?.let { Text(it, color = Color(0xFFFFD689), style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
                MatchThreeTab.RANK -> {
                    item { Text("Пьедестал исследователей", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                    item {
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Color(0xFF222941)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            RankPeriod.entries.forEach { period -> FilterChip(selected = period == rankPeriod, onClick = { rankPeriod = period }, label = { Text(period.title) }, modifier = Modifier.weight(1f)) }
                        }
                    }
                    if (user?.stableId().isNullOrBlank()) item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF20263A).copy(alpha = .96f)), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Рейтинг исследователей", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Войдите, чтобы сохранять прохождения на сервере и участвовать в общем рейтинге.", color = Color(0xFFBAC5D6), style = MaterialTheme.typography.bodyMedium)
                                Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Войти") }
                            }
                        }
                    }
                    if (rankLoading && !user?.stableId().isNullOrBlank()) item { Text("Загружаем таблицу лидеров…", color = Color(0xFFBAC5D6)) }
                    rankError?.let { message -> item { Text(message, color = Color(0xFFFF99A9)) } }
                    if (!rankLoading && rankError == null && rankUsers.isEmpty() && !user?.stableId().isNullOrBlank()) item {
                        Text("В этом периоде пока нет прохождений. Локально пройдено: ${saved.records.map { it.level }.distinct().size} глав.", color = Color(0xFFBAC5D6))
                    }
                    items(rankUsers, key = { "rank_${rankPeriod.apiValue}_${it.userId}" }) { entry ->
                        Surface(color = Color(0xFF20263A).copy(alpha = .94f), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, if (entry.rank == 1) Color(0xFFFFD36A).copy(alpha = .5f) else Color.White.copy(alpha = .08f))) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("${entry.rank}", color = if (entry.rank == 1) Color(0xFFFFD36A) else Color(0xFFB9C5D6), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                                Column(Modifier.weight(1f)) {
                                    Text(entry.username, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Лучший уровень ${entry.highestLevel}", color = Color(0xFFB9C5D6), style = MaterialTheme.typography.labelSmall)
                                }
                                Text("${entry.completedLevels} глав", color = Color(0xFFFFD689), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (!user?.stableId().isNullOrBlank()) item { Text("Рейтинг синхронизируется с сервером. Период: ${rankPeriod.title.lowercase()}.", color = Color(0xFF909CB4), style = MaterialTheme.typography.bodySmall) }
                }
                MatchThreeTab.EDITOR -> {
                    item { Text("Раздел доступен только администратору.", color = Color.White) }
                }
            }
        }
        }
      }
    }
}

private fun MatchThreeSave.normalized(capacity: Int): MatchThreeSave = refreshed(capacity, System.currentTimeMillis())
private fun MatchThreeSave.refreshed(capacity: Int, now: Long): MatchThreeSave {
    val current = lives.coerceIn(0, capacity)
    if (current >= capacity) return copy(lives = capacity, lifeStamp = now)
    val stamp = lifeStamp.takeIf { it > 0 } ?: now
    val elapsed = ((now - stamp) / 1_800_000L).coerceAtLeast(0).toInt()
    val restored = (current + elapsed).coerceAtMost(capacity)
    return copy(lives = restored, lifeStamp = if (restored >= capacity) now else stamp + elapsed * 1_800_000L)
}
private fun MatchThreeSave.nextLifeMinutes(capacity: Int): Int = if (lives >= capacity) 0 else (((lifeStamp + 1_800_000L - System.currentTimeMillis()).coerceAtLeast(0) + 59_999) / 60_000).toInt()
private fun colorName(index: Int) = listOf("коралловых", "лазурных", "солнечных", "аметистовых", "мятных")[index.coerceIn(0, 4)]
private fun movesLabel(moves: Int): String {
    val tail = moves % 100
    val word = when {
        tail in 11..14 -> "ходов"
        tail % 10 == 1 -> "ход"
        tail % 10 in 2..4 -> "хода"
        else -> "ходов"
    }
    return "$moves $word"
}
private fun obsLabel(obs: Obstacle) = when (obs) { Obstacle.ROCK -> "Камень"; Obstacle.ICE -> "Лёд"; Obstacle.CHAIN -> "Цепь" }

private class CellAnchors {
    var boardRoot = Offset.Unspecified
    val roots = Array(64) { Offset.Unspecified }
    var width = 0
    var height = 0
    fun ready() = boardRoot != Offset.Unspecified && roots.all { it != Offset.Unspecified }
}

@Composable private fun MatchThreeHero(level: Int, lives: Int, capacity: Int, premium: Boolean, minutes: Int, completed: Int, onPremium: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(25.dp)) {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF344D66), Color(0xFF644F78), Color(0xFF8C6377)))).padding(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Звёздная мастерская", color = Color(0xFFFFE1A2), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp); Text("Сад созвездий", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) }
                    Text("✦", color = Color(0xFFFFE1A2), fontSize = 44.sp)
                }
                Text("Меняйте самоцветы свайпом и собирайте созвездия.", color = Color(0xFFF0EAF4), style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Favorite, null, tint = Color(0xFFFF8DAB), modifier = Modifier.size(19.dp))
                    Text("$lives / $capacity", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(if (lives < capacity) "· следующее через $minutes мин" else "· $completed глав открыто", color = Color(0xFFE3D8E7), style = MaterialTheme.typography.labelSmall)
                    if (!premium) TextButton(onClick = onPremium, contentPadding = PaddingValues(horizontal = 2.dp)) { Text("Премиум 8", color = Color(0xFFFFE1A2), style = MaterialTheme.typography.labelSmall) }
                }
                LinearProgressIndicator(progress = { lives.toFloat() / capacity }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape), color = Color(0xFFFFA0B9), trackColor = Color.White.copy(alpha = .2f))
                Text("Глава $level", color = Color(0xFFFFE4AE), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable private fun StatPill(text: String, tint: Color) {
    Surface(color = tint.copy(alpha = .17f), contentColor = Color.White, shape = CircleShape, border = BorderStroke(1.dp, tint.copy(alpha = .5f))) { Text(text, Modifier.padding(horizontal = 13.dp, vertical = 8.dp), fontWeight = FontWeight.Bold) }
}

@Composable private fun MissionPanel(color: Int, collected: Int, target: Int, moves: Int) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(Color(0xFF10192A).copy(alpha = .94f)).border(1.dp, Color(0xFFFFD689).copy(alpha = .35f), RoundedCornerShape(19.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Gem(color, Modifier.size(39.dp))
        Column(Modifier.weight(1f)) { Text("Соберите ${colorName(color)} самоцветы", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium); LinearProgressIndicator(progress = { (collected.toFloat() / target).coerceIn(0f, 1f) }, Modifier.fillMaxWidth().padding(top = 7.dp).height(5.dp).clip(CircleShape), color = gemColors[color], trackColor = Color.White.copy(alpha = .13f)) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$collected/$target", color = Color(0xFFFFD689), fontWeight = FontWeight.Black); Text(movesLabel(moves), color = Color(0xFFBAC5D6), style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable private fun MatchBoard(
    board: List<Int>,
    obstacles: Map<Int, Obstacle>,
    gems: List<MatchThreeMotion.Sprite>? = null,
    motionPhase: Int = 0,
    motionKind: MotionKind = MotionKind.SETTLE,
    hintCells: Set<Int> = emptySet(),
    nudge: Pair<Int, Int>? = null,
    nudgeToken: Int = 0,
    busy: Boolean = false,
    editor: Boolean = false,
    rejectedCells: Set<Int> = emptySet(),
    fx: List<GardenFx> = emptyList(),
    fxToken: Int = 0,
    allowTap: Boolean = editor,
    onCell: (Int) -> Unit,
    onSwipe: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    stretchHeight: Boolean = false,
) {
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val onCellState = rememberUpdatedState(onCell)
    val onSwipeState = rememberUpdatedState(onSwipe)
    val busyState = rememberUpdatedState(busy)
    var dragFrom by remember { mutableIntStateOf(-1) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var releaseFrom by remember { mutableIntStateOf(-1) }
    var releaseX by remember { mutableFloatStateOf(0f) }
    var releaseY by remember { mutableFloatStateOf(0f) }
    var releaseScale by remember { mutableFloatStateOf(0f) }
    var releaseToken by remember { mutableIntStateOf(0) }
    var travel by remember { mutableFloatStateOf(1f) }
    var seenPhase by remember { mutableIntStateOf(motionPhase) }
    var nudgeTravel by remember { mutableFloatStateOf(0f) }
    var fxTravel by remember { mutableFloatStateOf(1f) }
    var seenFx by remember { mutableIntStateOf(fxToken) }
    if (seenFx != fxToken) {
        seenFx = fxToken
        fxTravel = if (reduceMotion || fx.isEmpty()) 1f else 0f
    }
    if (seenPhase != motionPhase) {
        seenPhase = motionPhase
        travel = if (reduceMotion || motionKind == MotionKind.SETTLE) 1f else 0f
    }
    LaunchedEffect(busy) {
        if (busy) {
            dragFrom = -1
            dragX = 0f
            dragY = 0f
            releaseScale = 0f
            releaseFrom = -1
        }
    }
    LaunchedEffect(releaseToken) {
        if (releaseToken == 0 || reduceMotion) {
            releaseScale = 0f
            return@LaunchedEffect
        }
        val anim = Animatable(1f)
        anim.animateTo(0f, tween(MatchThreeMotion.NUDGE_MS, easing = FastOutSlowInEasing)) { releaseScale = value }
        releaseScale = 0f
        releaseFrom = -1
    }
    LaunchedEffect(motionPhase) {
        if (reduceMotion || motionKind == MotionKind.SETTLE) {
            travel = 1f
            return@LaunchedEffect
        }
        val ms = when (motionKind) {
            MotionKind.SWAP -> MatchThreeMotion.SWAP_MS
            MotionKind.POP -> MatchThreeMotion.POP_MS
            MotionKind.FALL -> MatchThreeMotion.FALL_MS
            MotionKind.ENTER -> MatchThreeMotion.POP_MS
            MotionKind.SETTLE -> 0
        }
        val anim = Animatable(0f)
        anim.animateTo(1f, tween(ms, easing = FastOutSlowInEasing)) { travel = value }
    }
    LaunchedEffect(nudgeToken) {
        if (nudge == null || reduceMotion) {
            nudgeTravel = 0f
            return@LaunchedEffect
        }
        val anim = Animatable(0f)
        anim.animateTo(1f, tween(MatchThreeMotion.NUDGE_MS, easing = FastOutSlowInEasing)) {
            nudgeTravel = sin(PI * value).toFloat()
        }
        nudgeTravel = 0f
    }
    LaunchedEffect(fxToken) {
        if (reduceMotion || fx.isEmpty()) {
            fxTravel = 1f
            return@LaunchedEffect
        }
        val anim = Animatable(0f)
        anim.animateTo(1f, tween(GARDEN_FX_MS, easing = FastOutSlowInEasing)) { fxTravel = value }
    }
    val hintTransition = rememberInfiniteTransition(label = "match-hint")
    val hintPulse = hintTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "hint-pulse",
    )
    val hintScale = if (hintCells.isEmpty() || busy || motionKind != MotionKind.SETTLE) 1f else hintPulse.value
    val sprites = when {
        gems == null -> MatchThreeMotion.settle(board)
        else -> gems.distinctBy { it.id }
    }
    val boardModifier = if (stretchHeight) modifier else modifier.fillMaxWidth().aspectRatio(1f)
    val anchors = remember { CellAnchors() }
    var placed by remember { mutableStateOf<Array<Offset>?>(null) }
    var measuredCell by remember { mutableStateOf(IntSize.Zero) }
    fun publishAnchors() {
        if (!anchors.ready()) return
        val next = Array(64) { index -> anchors.roots[index] - anchors.boardRoot }
        val prev = placed
        if (prev != null && measuredCell.width == anchors.width && measuredCell.height == anchors.height && next.indices.all { next[it] == prev[it] }) return
        placed = next
        measuredCell = IntSize(anchors.width, anchors.height)
    }
    BoxWithConstraints(
        boardModifier
            .clip(RoundedCornerShape(21.dp))
            .background(Color(0xFF17223B).copy(alpha = .97f))
            .border(1.dp, Color(0xFFFFD689).copy(alpha = .48f), RoundedCornerShape(21.dp))
            .onGloballyPositioned { coords ->
                anchors.boardRoot = coords.positionInRoot()
                publishAnchors()
            },
    ) {
        val density = LocalDensity.current
        val padPx = with(density) { 6.dp.toPx() }
        val gapPx = with(density) { 3.dp.toPx() }
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val cellWpx = if (measuredCell.width > 0) measuredCell.width.toFloat() else (widthPx - padPx * 2f - gapPx * 7f) / 8f
        val cellHpx = if (measuredCell.height > 0) measuredCell.height.toFloat() else (heightPx - padPx * 2f - gapPx * 7f) / 8f
        val cellW = with(density) { cellWpx.toDp() }
        val cellH = with(density) { cellHpx.toDp() }
        val strideY = cellHpx + gapPx
        fun originOf(cell: Int): Offset {
            val known = placed
            if (known != null && cell in known.indices) return known[cell]
            val row = cell / 8
            val col = cell % 8
            return Offset(padPx + col * (cellWpx + gapPx), padPx + row * strideY)
        }
        val grid: @Composable (interactive: Boolean) -> Unit = { interactive ->
            Column(Modifier.padding(6.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(8) { row ->
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(8) { col ->
                            val index = row * 8 + col
                            val description = "Ряд ${row + 1}, столбец ${col + 1}: ${obstacles[index]?.let(::obsLabel) ?: board.getOrNull(index)?.takeIf { it >= 0 }?.let { "${colorName(it)} самоцвет" } ?: "пусто"}"
                            val swipeModifier = if (interactive && !editor && !allowTap) {
                                Modifier.pointerInput(index) {
                                    var movedX = 0f
                                    var movedY = 0f
                                    var sent = false
                                    detectDragGestures(
                                        onDragStart = {
                                            movedX = 0f
                                            movedY = 0f
                                            sent = false
                                            if (!busyState.value) dragFrom = index
                                        },
                                        onDragEnd = {
                                            if (!sent && dragFrom >= 0 && (abs(dragX) > 0.5f || abs(dragY) > 0.5f)) {
                                                releaseFrom = dragFrom
                                                releaseX = dragX
                                                releaseY = dragY
                                                releaseToken++
                                            }
                                            dragFrom = -1
                                            dragX = 0f
                                            dragY = 0f
                                        },
                                        onDragCancel = {
                                            dragFrom = -1
                                            dragX = 0f
                                            dragY = 0f
                                            releaseScale = 0f
                                            releaseFrom = -1
                                        },
                                    ) { change, amount ->
                                        if (busyState.value) {
                                            change.consume()
                                            return@detectDragGestures
                                        }
                                        movedX += amount.x
                                        movedY += amount.y
                                        dragFrom = index
                                        dragX = movedX
                                        dragY = movedY
                                        val horizontal = abs(movedX) >= abs(movedY)
                                        val threshold = (if (horizontal) size.width else size.height) * 0.28f
                                        if (!sent && (abs(movedX) >= threshold || abs(movedY) >= threshold)) {
                                            val target = MatchThreeEngine.swipeTarget(index, movedX, movedY, threshold)
                                            if (target != null) {
                                                onSwipeState.value(index, target)
                                                sent = true
                                                dragFrom = -1
                                                dragX = 0f
                                                dragY = 0f
                                            }
                                        }
                                        change.consume()
                                    }
                                }
                            } else Modifier
                            val tapModifier = if (interactive && allowTap) {
                                Modifier.clickable(
                                    interactionSource = remember(index) { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onCellState.value(index) },
                                )
                            } else Modifier
                            val tone = when {
                                index in rejectedCells -> Color(0xFF8B3448)
                                (row + col) % 2 == 0 -> Color(0xFF29354E)
                                else -> Color(0xFF222E46)
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .then(if (interactive) swipeModifier.then(tapModifier).semantics { contentDescription = description } else Modifier)
                                    .then(if (interactive) Modifier else Modifier.background(tone, RoundedCornerShape(10.dp)).onGloballyPositioned { coords ->
                                        anchors.roots[index] = coords.positionInRoot()
                                        anchors.width = coords.size.width
                                        anchors.height = coords.size.height
                                        publishAnchors()
                                    }),
                            )
                        }
                    }
                }
            }
        }
        if (cellWpx > 1f && cellHpx > 1f) {
            grid(false)
            val pullX: Float
            val pullY: Float
            val pullFrom: Int
            if (!busy && dragFrom >= 0 && motionKind == MotionKind.SETTLE) {
                pullFrom = dragFrom
                pullX = dragX
                pullY = dragY
            } else if (!busy && releaseFrom >= 0 && releaseScale > 0f && motionKind == MotionKind.SETTLE) {
                pullFrom = releaseFrom
                pullX = releaseX * releaseScale
                pullY = releaseY * releaseScale
            } else {
                pullFrom = -1
                pullX = 0f
                pullY = 0f
            }
            val follow = if (pullFrom < 0) Offset.Zero else {
                val horizontal = abs(pullX) >= abs(pullY)
                Offset(
                    if (horizontal) pullX.coerceIn(-cellWpx, cellWpx) else 0f,
                    if (horizontal) 0f else pullY.coerceIn(-cellHpx, cellHpx),
                )
            }
            val dragNeighbor = if (follow != Offset.Zero) MatchThreeEngine.swipeTarget(pullFrom, pullX, pullY, 0.5f) else null
            val nudgeShift = if (nudge == null || nudgeTravel == 0f) Offset.Zero else (originOf(nudge.second) - originOf(nudge.first)) * (0.22f * nudgeTravel)
            Box(Modifier.fillMaxSize()) {
                sprites.forEach { gem ->
                    if (gem.cell < 0) return@forEach
                    val dest = originOf(gem.cell)
                    val start = when {
                        gem.fromCell < 0 -> dest - Offset(0f, gem.spawnRows.coerceAtLeast(1) * strideY)
                        gem.fromCell == gem.cell -> dest
                        else -> originOf(gem.fromCell)
                    }
                    val topLeft = if (gem.pop || start == dest) dest else lerp(start, dest, travel.coerceIn(0f, 1f))
                    val extra = when (gem.cell) {
                        pullFrom -> follow
                        dragNeighbor -> follow * -0.22f
                        nudge?.first -> nudgeShift
                        nudge?.second -> nudgeShift * -1f
                        else -> Offset.Zero
                    }
                    val hinted = gem.cell in hintCells && hintScale != 1f && !gem.pop && !gem.enter
                    val swapLift = if (motionKind == MotionKind.SWAP && gem.fromCell != gem.cell) 1f + 0.06f * sin(PI * travel.coerceIn(0f, 1f)).toFloat() else 1f
                    val scale = when {
                        gem.pop -> (1f - travel).coerceIn(0f, 1f)
                        gem.enter && gem.fromCell >= 0 -> travel.coerceIn(0f, 1f)
                        else -> 1f
                    } * (if (hinted) hintScale else 1f) * swapLift
                    val alpha = when {
                        gem.pop -> (1f - travel).coerceIn(0f, 1f)
                        gem.enter && gem.fromCell >= 0 -> travel.coerceIn(0f, 1f)
                        else -> 1f
                    }
                    key(gem.id) {
                        Box(
                            Modifier
                                .offset { IntOffset((topLeft.x + extra.x).roundToInt(), (topLeft.y + extra.y).roundToInt()) }
                                .size(cellW, cellH)
                                .zIndex(when {
                                    gem.cell == pullFrom && pullFrom >= 0 -> 3f
                                    gem.pop -> 2f
                                    gem.cell == dragNeighbor -> 1f
                                    else -> 0f
                                }),
                        ) {
                            Gem(
                                gem.color,
                                Modifier.align(Alignment.Center).fillMaxSize(0.88f).graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                },
                                decorative = true,
                            )
                        }
                    }
                }
            }
            obstacles.forEach { (cell, obs) ->
                if (cell !in 0 until 64) return@forEach
                val topLeft = originOf(cell)
                val icon = when (obs) {
                    Obstacle.ROCK -> ru.tomilo.lib.mobile.R.drawable.pill_obstacle_rock
                    Obstacle.ICE -> ru.tomilo.lib.mobile.R.drawable.pill_obstacle_ice
                    Obstacle.CHAIN -> ru.tomilo.lib.mobile.R.drawable.pill_obstacle_chain
                }
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.offset { IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()) }.size(cellW, cellH).padding(2.dp),
                )
            }
            if (!reduceMotion && fx.isNotEmpty() && fxTravel < 1f) {
                GardenFxLayer(
                    fx = fx,
                    life = fxTravel.coerceIn(0f, 1f),
                    originOf = { cell -> originOf(cell) },
                    cellWpx = cellWpx,
                    cellHpx = cellHpx,
                    boardWpx = widthPx,
                    boardHpx = heightPx,
                )
            }
            grid(true)
        }
    }
}

@Composable private fun GardenFxLayer(
    fx: List<GardenFx>,
    life: Float,
    originOf: (Int) -> Offset,
    cellWpx: Float,
    cellHpx: Float,
    boardWpx: Float,
    boardHpx: Float,
) {
    val density = LocalDensity.current
    fx.forEach { effect ->
        val pose = gardenPose(effect.kind, life)
        if (pose.alpha <= 0.01f) return@forEach
        val anchors = if (effect.kind == GardenFxKind.SHUFFLE || effect.kind == GardenFxKind.WIN) {
            listOf(Offset(boardWpx / 2f, boardHpx / 2f))
        } else {
            effect.cells.filter { it in 0 until 64 }.map { cell -> originOf(cell) + Offset(cellWpx / 2f, cellHpx / 2f) }
        }
        if (anchors.isEmpty()) return@forEach
        val painter = painterResource(gardenDrawable(effect.kind, life))
        val boxW = cellWpx * pose.box
        val boxH = cellHpx * pose.box
        anchors.forEach { anchor ->
            val left = anchor.x - boxW / 2f + pose.nudgeX * cellWpx
            val top = anchor.y - boxH / 2f + pose.nudgeY * cellHpx
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                    .size(with(density) { boxW.toDp() }, with(density) { boxH.toDp() })
                    .graphicsLayer {
                        scaleX = pose.scaleX
                        scaleY = pose.scaleY
                        rotationZ = pose.rotation
                        this.alpha = pose.alpha
                    },
            )
        }
    }
}

@Composable private fun Gem(color: Int, modifier: Modifier = Modifier, decorative: Boolean = false) {
    val safe = color.coerceIn(0, 4)
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(gemSprites[safe]),
            contentDescription = if (decorative) null else "${colorName(safe)} самоцвет",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun EditorNudge(symbol: String, size: Dp = 32.dp, description: String? = null, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF212A40))
            .border(1.dp, Color.White.copy(alpha = .12f), CircleShape)
            .then(if (description == null) Modifier else Modifier.semantics { contentDescription = description })
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun EditorBrush(selected: Boolean, description: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(11.dp)
    Box(
        Modifier
            .size(30.dp)
            .clip(shape)
            .background(if (selected) Color(0xFF655281) else Color(0xFF212A40))
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Color(0xFFFFD689) else Color.White.copy(alpha = .12f), shape)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun EditorTextButton(text: String, selected: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Text(
        text,
        Modifier
            .clip(shape)
            .background(if (selected) Color(0xFF655281) else Color(0xFF212A40))
            .border(1.dp, if (selected) Color(0xFFFFD689) else Color.White.copy(alpha = .12f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        color = Color.White,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
    )
}

@Composable
private fun EditorMeter(label: String, value: Int, onMinus: () -> Unit, onPlus: () -> Unit, warn: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFFBAC5D6), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            EditorNudge("−", 28.dp) { onMinus() }
            Text(
                "$value",
                color = if (warn) Color(0xFFFF99A9) else Color(0xFFFFD689),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.widthIn(min = 20.dp),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            EditorNudge("+", 28.dp) { onPlus() }
        }
    }
}

@Composable
private fun EditorAction(text: String, filled: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = modifier.height(44.dp).clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        color = when {
            !enabled -> Color(0xFF1A2233)
            filled -> Color(0xFF655281)
            else -> Color(0xFF212A40)
        },
        border = BorderStroke(1.dp, if (enabled && filled) Color(0xFFFFD689) else Color.White.copy(alpha = .12f)),
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
            Text(
                text,
                color = if (enabled) Color.White else Color(0xFF78849A),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ChapterDot(number: Int, selected: Boolean, published: Boolean, onClick: () -> Unit) {
    val width = when {
        number >= 10000 -> 58.dp
        number >= 1000 -> 48.dp
        number >= 100 -> 40.dp
        else -> 32.dp
    }
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .height(32.dp)
            .width(width)
            .clip(shape)
            .background(if (selected) Color(0xFF655281) else Color(0xFF212A40))
            .border(1.dp, if (selected) Color(0xFFFFD689) else if (published) Color.White.copy(.16f) else Color(0xFFFFD689).copy(.35f), shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = if (published) "Глава $number" else "Черновик главы $number" },
        contentAlignment = Alignment.Center,
    ) {
        Text("$number", color = if (published) Color.White else Color(0xFFFFD689), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
    }
}

@Composable private fun BoosterButton(icon: Int, label: String, count: Int, active: Boolean, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(modifier = modifier.height(60.dp).semantics { contentDescription = "$label: $count зарядов, ${if (!enabled || count == 0) "недоступен" else if (active) "выбран, коснитесь клетки" else "не выбран"}" }.clickable(enabled = enabled && count > 0, onClick = onClick), shape = RoundedCornerShape(16.dp), color = if (active) Color(0xFF655281) else Color(0xFF212A40), border = BorderStroke(1.dp, if (active) Color(0xFFFFD689) else Color.White.copy(alpha = .12f))) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(32.dp))
                Text(" $count", color = if (count > 0) Color.White else Color(0xFF78849A), fontWeight = FontWeight.Bold)
            }
            Text(label, color = Color(0xFFD1D8E4), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable private fun StarryBackdrop(backdrop: GardenBackdrop, modifier: Modifier = Modifier) {
    val twinkle = rememberInfiniteTransition(label = "garden-stars")
    val phase by twinkle.animateFloat(initialValue = .12f, targetValue = .42f, animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "star-twinkle")
    Image(
        painter = painterResource(backdrop.drawable),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
    Canvas(modifier) {
        val points = listOf(.08f to .06f, .88f to .1f, .72f to .23f, .14f to .35f, .93f to .43f, .06f to .68f, .78f to .81f, .91f to .92f)
        points.forEachIndexed { i, point -> drawCircle(Color(0xFFFFD689).copy(alpha = if (i % 2 == 0) phase else phase * .52f), radius = if (i % 3 == 0) 3f else 1.5f, center = androidx.compose.ui.geometry.Offset(size.width * point.first, size.height * point.second)) }
    }
}
