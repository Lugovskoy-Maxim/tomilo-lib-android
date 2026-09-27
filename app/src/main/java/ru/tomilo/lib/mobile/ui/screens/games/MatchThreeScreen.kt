package ru.tomilo.lib.mobile.ui.screens.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.MatchThreeEngine
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
private val matchFxFrames = listOf(
    ru.tomilo.lib.mobile.R.drawable.pill_fx_match_small,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_match_burst,
    ru.tomilo.lib.mobile.R.drawable.pill_fx_match_fade,
)

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
    var matchedFxCells by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var matchedFxFrame by remember { mutableIntStateOf(0) }
    var matchedFxSequence by remember { mutableIntStateOf(0) }
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
    var editorEditingLevel by rememberSaveable { mutableStateOf<Int?>(null) }
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
    LaunchedEffect(matchedFxSequence) {
        if (matchedFxCells.isNotEmpty()) {
            matchedFxFrame = 0
            repeat(matchFxFrames.size) { frame ->
                matchedFxFrame = frame
                delay(95)
            }
            matchedFxCells = emptySet()
        }
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
                .onSuccess { adminLevels = it }
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
        if (remoteMode) return
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
                board = session.board; obstacles = session.obstacles.mapIndexedNotNull { i, kind -> if (kind < 0) null else i to Obstacle.entries[kind] }.toMap()
                moves = session.moves; collected = session.collected; target = session.target; targetColor = session.targetColor
                boosterHammer = session.hammer; boosterRainbow = session.rainbow; boosterShuffle = session.shuffle
                selectedLevel = number; playing = true; finished = false; won = false; remoteMode = false
            }
            return
        }
        if (spec.number < 1 || spec.target !in 1..40 || spec.moves !in 1..35 || spec.color !in 0 until MatchThreeEngine.COLORS || spec.obstacles.keys.any { it !in 0 until 64 }) {
            notice = "Параметры главы повреждены. Откройте другую главу."; return
        }
        val initialBoard = runCatching { MatchThreeEngine.createBoard(spec.seed, spec.obstacles) }
            .getOrElse { notice = it.message ?: "Невозможно создать поле для этой расстановки."; return }
        selectedLevel = number
        board = initialBoard
        obstacles = spec.obstacles
        target = spec.target
        targetColor = spec.color
        moves = spec.moves
        collected = 0
        boosterHammer = 2; boosterRainbow = 1; boosterShuffle = 1
        activeBooster = ""; finished = false; won = false; playing = true; notice = null; remoteError = null; remoteMode = false
        if (!user?.stableId().isNullOrBlank()) scope.launch {
            saving = true
            gamesRepository.startPillMatchLevel(number).onSuccess { response ->
                response.session?.let { session ->
                    board = session.board.map { color -> listOf("coral", "cyan", "lemon", "violet", "mint").indexOf(color).coerceAtLeast(0) }
                    obstacles = session.obstacles.mapIndexedNotNull { i, kind -> if (kind < 0) null else i to Obstacle.entries.getOrElse(kind) { Obstacle.ROCK } }.toMap()
                    moves = session.moves; collected = session.collected; target = session.target; targetColor = session.targetColor
                    boosterHammer = session.boosters.hammer; boosterRainbow = session.boosters.rainbow; boosterShuffle = session.boosters.shuffle
                }
                remoteMode = true
                remoteLives = response.lives.current
                remoteCapacity = response.lives.capacity
                remoteNextLifeAt = response.lives.nextLifeAt
                remoteCompleted = response.completedLevels
            }.onFailure {
                playing = false
                notice = it.message ?: "Не удалось открыть главу. Проверьте соединение и число сердец."
            }
            saving = false
        } else {
            val nextSave = refreshed.copy(lives = refreshed.lives - 1, lifeStamp = if (refreshed.lives == capacity) System.currentTimeMillis() else refreshed.lifeStamp)
            saved = nextSave
            persistGuestSession()
        }
    }

    fun applyRemoteState(state: ru.tomilo.lib.mobile.data.api.PillMatchActionDto, session: PillMatchSessionDto = state.session) {
        board = session.board.map { color -> listOf("coral", "cyan", "lemon", "violet", "mint").indexOf(color).coerceAtLeast(0) }
        obstacles = session.obstacles.mapIndexedNotNull { i, kind -> if (kind < 0) null else i to Obstacle.entries.getOrElse(kind) { Obstacle.ROCK } }.toMap()
        moves = session.moves; collected = session.collected; target = session.target; targetColor = session.targetColor
        boosterHammer = session.boosters.hammer; boosterRainbow = session.boosters.rainbow; boosterShuffle = session.boosters.shuffle
        remoteLives = state.lives.current; remoteCapacity = state.lives.capacity; remoteCompleted = state.completedLevels
        remoteNextLifeAt = state.lives.nextLifeAt
        if (collected >= target) { won = true; activeBooster = "" }
        else if (moves <= 0) { finished = true; activeBooster = ""; notice = "Ходы закончились. Начните главу заново." }
    }

    fun recordWin() {
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

    fun triggerMatchFx(cells: Set<Int>) {
        matchedFxCells = cells
        matchedFxSequence++
    }

    fun applyMove(result: MatchThreeEngine.Move, consumesMove: Boolean = true) {
        triggerMatchFx(result.matchedCells)
        board = result.board; obstacles = result.obstacles
        collected = (collected + result.collected).coerceAtMost(target)
        if (consumesMove) moves = (moves - 1).coerceAtLeast(0)
        if (collected >= target) { won = true; activeBooster = ""; persistGuestSession() } else if (moves == 0) {
            finished = true
            activeBooster = ""
            val next = saved.copy(activeSession = null)
            saved = next
            scope.launch { store.write(next) }
            notice = "Ходы закончились. Начните главу заново."
        } else persistGuestSession()
    }

    fun playCell(index: Int) {
        if (!playing || finished || won || moves <= 0 || saving) return
        if (activeBooster.isNotEmpty()) {
            if (index !in board.indices || activeBooster == "rainbow" && index in obstacles) {
                notice = "Выберите клетку с самоцветом"; return
            }
            if (activeBooster == "hammer" && boosterHammer <= 0 || activeBooster == "rainbow" && boosterRainbow <= 0) {
                notice = "Заряды бустера закончились"; activeBooster = ""; return
            }
            if (remoteMode) {
                val booster = activeBooster
                val preview = when (booster) {
                    "hammer" -> MatchThreeEngine.clearAt(board, obstacles, index, targetColor, now.toInt())
                    "rainbow" -> board.getOrNull(index)?.let { MatchThreeEngine.clearColor(board, obstacles, it, targetColor, now.toInt()) }
                    else -> null
                }
                scope.launch {
                    saving = true
                    gamesRepository.pillMatchBooster(booster, index).onSuccess {
                        preview?.let { triggerMatchFx(it.matchedCells) }
                        applyRemoteState(it)
                        activeBooster = ""
                    }.onFailure { notice = it.message ?: "Бустер не сработал. Попробуйте ещё раз." }
                    saving = false
                }
                return
            }
            when (activeBooster) {
                "hammer" -> if (boosterHammer > 0) MatchThreeEngine.clearAt(board, obstacles, index, targetColor, now.toInt())?.let { boosterHammer--; applyMove(it, consumesMove = false) }
                "rainbow" -> if (boosterRainbow > 0 && index !in obstacles) { val color = board.getOrNull(index) ?: return; boosterRainbow--; applyMove(MatchThreeEngine.clearColor(board, obstacles, color, targetColor, now.toInt()), consumesMove = false) }
            }
            activeBooster = ""; return
        }
    }

    fun playSwipe(from: Int, to: Int) {
        if (!playing || finished || won || moves <= 0 || saving || activeBooster.isNotEmpty()) return
        if (!MatchThreeEngine.adjacent(from, to) || from in obstacles || to in obstacles) {
            rejectedCells = setOf(from, to); rejectionSequence++; notice = "Камень, лёд и цепь блокируют обмен"; return
        }
        val preview = MatchThreeEngine.swap(board, obstacles, from, to, targetColor, now.toInt())
        if (remoteMode) {
            scope.launch { saving = true; gamesRepository.pillMatchMove(from, to).onSuccess { action ->
                if (action.validMove) {
                    preview?.let { triggerMatchFx(it.matchedCells) }
                    applyRemoteState(action)
                    if (action.session.collected >= action.session.target) { won = true; activeBooster = "" }
                    else if (action.session.moves <= 0) { finished = true; activeBooster = ""; notice = "Ходы закончились. Начните главу заново." }
                } else { rejectedCells = setOf(from, to); rejectionSequence++; notice = "Нужно собрать три или больше самоцветов." }
            }.onFailure { notice = it.message ?: "Не удалось отправить ход" }; saving = false }
        } else MatchThreeEngine.swap(board, obstacles, from, to, targetColor, now.toInt())?.let(::applyMove)
            ?: run { rejectedCells = setOf(from, to); rejectionSequence++; notice = "Свайпните соседний самоцвет, чтобы собрать три в ряд." }
    }

    val activePlay = selectedTab == MatchThreeTab.PLAY && playing
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF101624),
        topBar = {
          if (!activePlay) {
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Сад созвездий", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("ГЛАВА $selectedLevel", color = Color(0xFFFFD689), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    }
                    IconButton(onClick = { selectedTab = MatchThreeTab.RANK }) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Лидеры", tint = Color(0xFFFFD689))
                    }
                    if (admin) IconButton(onClick = { selectedTab = MatchThreeTab.EDITOR }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Редактор уровней", tint = Color(0xFFFFD689))
                    }
                    StatPill("♥ ${if (remoteLives >= 0) remoteLives else saved.lives}/${if (remoteLives >= 0) remoteCapacity else capacity}", if (premium) Color(0xFFFF84A4) else Color(0xFFDA7894))
                }
                MissionPanel(color = targetColor, collected = collected, target = target, moves = moves)
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val boardWidth = minOf(maxWidth, maxHeight)
                    val boardHeight = minOf(maxHeight, boardWidth * 1.22f)
                    MatchBoard(
                        board, obstacles, matchedFxCells = matchedFxCells, matchedFxFrame = matchedFxFrame, rejectedCells = rejectedCells,
                        allowTap = activeBooster.isNotEmpty(), onCell = ::playCell, onSwipe = ::playSwipe,
                        modifier = Modifier.width(boardWidth).height(boardHeight), stretchHeight = true,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BoosterButton(ru.tomilo.lib.mobile.R.drawable.pill_booster_hammer, "Молот", boosterHammer, activeBooster == "hammer", Modifier.weight(1f), enabled = !saving && !finished && !won && moves > 0) { activeBooster = if (activeBooster == "hammer") "" else "hammer" }
                    BoosterButton(ru.tomilo.lib.mobile.R.drawable.pill_booster_rainbow, "Радуга", boosterRainbow, activeBooster == "rainbow", Modifier.weight(1f), enabled = !saving && !finished && !won && moves > 0) { activeBooster = if (activeBooster == "rainbow") "" else "rainbow" }
                    BoosterButton(ru.tomilo.lib.mobile.R.drawable.pill_booster_shuffle, "Микс", boosterShuffle, false, Modifier.weight(1f), enabled = !saving && !finished && !won && moves > 0) {
                        if (remoteMode && !saving && boosterShuffle > 0) scope.launch {
                            saving = true
                            gamesRepository.pillMatchBooster("shuffle").onSuccess { applyRemoteState(it); notice = "Самоцветы перемешаны" }.onFailure { notice = it.message }
                            saving = false
                        } else if (boosterShuffle > 0) runCatching { MatchThreeEngine.shuffle(board, obstacles, now.toInt()) }
                            .onSuccess { board = it; boosterShuffle--; persistGuestSession(); notice = "Самоцветы перемешаны" }
                            .onFailure { notice = it.message ?: "Невозможно перемешать эту расстановку." }
                    }
                }
                if (finished && won) Button(onClick = { playing = false; selectedLevel = nextUnlockedLevel }, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = !saving) { Text("Следующая глава →") }
                else if (won) Button(onClick = ::recordWin, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = !saving) { Text(if (saving) "Сохраняем…" else if (remoteError != null) "Повторить сохранение" else "Завершить главу") }
                else if (finished || moves == 0) OutlinedButton(onClick = { playing = false; startLevel(selectedLevel) }, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = !saving) { Text("Повторить главу") }
                notice?.let { Text(it, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color(0xFFFFDC9C), style = MaterialTheme.typography.labelSmall, maxLines = 1) }
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
                                Text("Глава $selectedLevel · ${levelSpec(selectedLevel).moves} хода", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Соберите ${levelSpec(selectedLevel).target} ${colorName(levelSpec(selectedLevel).color)} фишек. Свайпните самоцвет к соседнему. Камень блокирует клетку, лёд и цепь исчезают, если рядом очистить совпадение. Молот убирает клетку, Радуга очищает цвет, Микс перемешивает поле. Бустеры не тратят ход.", color = Color(0xFFB8C3D8), style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Text("🪨 Камень", color = Color(0xFFE4D5C5), style = MaterialTheme.typography.labelSmall)
                                    Text("❄ Лёд", color = Color(0xFFADF1FF), style = MaterialTheme.typography.labelSmall)
                                    Text("⛓ Цепь", color = Color(0xFFFFD481), style = MaterialTheme.typography.labelSmall)
                                }
                                val availableLives = if (remoteLives >= 0) remoteLives else saved.lives
                                Button(onClick = { startLevel(selectedLevel) }, enabled = availableLives > 0 && !saving, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp)) {
                                    Text(if (availableLives > 0) "Войти в сад  ·  ♥ $availableLives" else "Ждём новое сердце")
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
                    if (playing) {
                        item {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text("ГЛАВА $selectedLevel", color = Color(0xFFFFD689), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp); Text("Сад созвездий", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                            StatPill("♥ ${if (remoteLives >= 0) remoteLives else saved.lives}/${if (remoteLives >= 0) remoteCapacity else capacity}", if (premium) Color(0xFFFF84A4) else Color(0xFFDA7894))
                            }
                        }
                        item { MissionPanel(color = targetColor, collected = collected, target = target, moves = moves) }
                        item {
                            MatchBoard(board, obstacles, editor = selectedTab == MatchThreeTab.EDITOR, matchedFxCells = matchedFxCells, matchedFxFrame = matchedFxFrame, rejectedCells = rejectedCells, allowTap = activeBooster.isNotEmpty(), onCell = ::playCell, onSwipe = ::playSwipe)
                            Spacer(Modifier.height(5.dp))
                            Text("Перетащите самоцвет в соседнюю клетку", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color(0xFFB9C4D8), style = MaterialTheme.typography.labelSmall)
                        }
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BoosterButton(ru.tomilo.lib.mobile.R.drawable.pill_booster_hammer, "Молот", boosterHammer, activeBooster == "hammer", Modifier.weight(1f), enabled = !saving && !finished && !won && moves > 0) { activeBooster = if (activeBooster == "hammer") "" else "hammer" }
                                BoosterButton(ru.tomilo.lib.mobile.R.drawable.pill_booster_rainbow, "Радуга", boosterRainbow, activeBooster == "rainbow", Modifier.weight(1f), enabled = !saving && !finished && !won && moves > 0) { activeBooster = if (activeBooster == "rainbow") "" else "rainbow" }
                                BoosterButton(ru.tomilo.lib.mobile.R.drawable.pill_booster_shuffle, "Микс", boosterShuffle, false, Modifier.weight(1f), enabled = !saving && !finished && !won && moves > 0) {
                                    if (remoteMode && !saving && boosterShuffle > 0) scope.launch { saving = true; gamesRepository.pillMatchBooster("shuffle").onSuccess { applyRemoteState(it); notice = "Самоцветы перемешаны" }.onFailure { notice = it.message }; saving = false }
                                    else if (boosterShuffle > 0) runCatching { MatchThreeEngine.shuffle(board, obstacles, now.toInt()) }
                                        .onSuccess { board = it; boosterShuffle--; persistGuestSession(); notice = "Самоцветы перемешаны" }
                                        .onFailure { notice = it.message ?: "Невозможно перемешать эту расстановку." }
                                }
                            }
                        }
                        item {
                            if (finished && won) Button(onClick = { playing = false; selectedLevel = nextUnlockedLevel }, modifier = Modifier.fillMaxWidth(), enabled = !saving) { Text("Следующая глава →") }
                            else if (won) Button(onClick = ::recordWin, modifier = Modifier.fillMaxWidth(), enabled = !saving) { Text(if (saving) "Сохраняем прохождение…" else if (remoteError != null) "Повторить сохранение" else "Завершить главу") }
                            else if (finished || moves == 0) OutlinedButton(onClick = { playing = false; startLevel(selectedLevel) }, modifier = Modifier.fillMaxWidth(), enabled = !saving) { Text("Повторить главу") }
                            notice?.let { Text(it, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color(0xFFFFDC9C), style = MaterialTheme.typography.bodySmall) }
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
                    if (admin) {
                        item { Text("Мастер уровней", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF20263A).copy(alpha = .96f)), shape = RoundedCornerShape(22.dp)) {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Быстрая генерация", color = Color(0xFFFFD689), fontWeight = FontWeight.Bold)
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(editorEditingLevel?.let { "Редактирование главы $it" } ?: "Новая глава", color = Color(0xFFCFD6E3), style = MaterialTheme.typography.labelMedium)
                                        TextButton(onClick = { editorEditingLevel = null; editorObstacles = emptyMap(); editorObstacleCount = 8; editorSeed++ }) { Text("Новый уровень") }
                                    }
                                    Text("Параметры: цель $editorTarget · $editorMoves ходов · ${colorName(editorColor)}", color = Color(0xFFCCD3E1), style = MaterialTheme.typography.bodySmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(onClick = { editorTarget = (editorTarget - 2).coerceAtLeast(8) }) { Text("Цель −") }
                                        OutlinedButton(onClick = { editorTarget = (editorTarget + 2).coerceAtMost(40) }) { Text("Цель +") }
                                        OutlinedButton(onClick = { editorMoves = (editorMoves - 1).coerceAtLeast(12) }) { Text("Ходы −") }
                                        OutlinedButton(onClick = { editorMoves = (editorMoves + 1).coerceAtMost(35) }) { Text("Ходы +") }
                                    }
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Плотность препятствий · $editorObstacleCount", color = Color(0xFFCCD3E1), style = MaterialTheme.typography.bodySmall)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(onClick = { editorObstacleCount = (editorObstacleCount - 1).coerceAtLeast(0) }) { Text("−") }
                                            OutlinedButton(onClick = { editorObstacleCount = (editorObstacleCount + 1).coerceAtMost(20) }) { Text("+") }
                                        }
                                    }
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Seed $editorSeed", color = Color(0xFF9DAAC2), style = MaterialTheme.typography.labelSmall)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(onClick = { editorSeed = (editorSeed - 1).coerceAtLeast(0) }) { Text("Seed −") }
                                            OutlinedButton(onClick = { editorSeed = (editorSeed + 1).coerceAtMost(Int.MAX_VALUE - 1) }) { Text("Seed +") }
                                        }
                                    }
                                    Text("Одинаковые seed и плотность дают одинаковую расстановку.", color = Color(0xFF9DAAC2), style = MaterialTheme.typography.labelSmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { gemColors.indices.forEach { i -> FilterChip(selected = editorColor == i, onClick = { editorColor = i }, label = { Text(gemMarks[i]) }) } }
                                    Text("Препятствие", color = Color(0xFFBFC9DB), style = MaterialTheme.typography.labelMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Obstacle.entries.forEach { obs -> FilterChip(selected = editorObstacle == obs, onClick = { editorObstacle = obs }, label = { Text(obsLabel(obs)) }) }
                                        TextButton(onClick = { editorObstacles = emptyMap() }) { Text("Очистить") }
                                    }
                                    Button(onClick = {
                                        editorObstacles = MatchThreeEngine.generateObstacles(editorSeed, editorObstacleCount)
                                    }, modifier = Modifier.fillMaxWidth(), enabled = !saving) { Text("✦ Сгенерировать расстановку") }
                                    Text("Нажмите на клетки поля, чтобы поставить или убрать выбранное препятствие.", color = Color(0xFF9DAAC2), style = MaterialTheme.typography.bodySmall)
                                    val editorPreview = remember(editorSeed, editorObstacles) {
                                        runCatching { MatchThreeEngine.createBoard(editorSeed, editorObstacles) }.getOrNull()
                                    }
                                    if (editorPreview == null) {
                                        Text("Эта расстановка блокирует возможные ходы. Уберите несколько препятствий.", color = Color(0xFFFF99A9), style = MaterialTheme.typography.bodySmall)
                                    } else MatchBoard(editorPreview, editorObstacles, editor = true, allowTap = true, onCell = { cell ->
                                        val updated = editorObstacles.toMutableMap().apply { if (containsKey(cell)) remove(cell) else put(cell, editorObstacle) }
                                        editorObstacles = updated
                                        editorObstacleCount = updated.size
                                    }, onSwipe = { _, _ -> })
                                    Button(onClick = {
                                        val levelNo = editorEditingLevel ?: maxOf(saved.records.maxOfOrNull { it.level } ?: 0, adminLevels.maxOfOrNull { it.level } ?: 0) + 1
                                        val obstacleKinds = List(64) { editorObstacles[it]?.ordinal ?: -1 }
                                        val request = PillMatchAdminLevelRequest(levelNo, editorTarget, editorMoves, editorColor, obstacleKinds, editorSeed)
                                        scope.launch {
                                            saving = true
                                            gamesRepository.savePillMatchAdminLevel(request)
                                            .onSuccess { remote ->
                                                    adminLevels = (adminLevels.filterNot { it.level == remote.level } + remote).sortedBy { it.level }
                                                    publishedLevels = (publishedLevels.filterNot { it.level == remote.level } + remote).sortedBy { it.level }
                                                    val local = MatchThreeCustomLevel(remote.level, remote.target, remote.moves, remote.targetColor, remote.obstacles, remote.seed)
                                                    saved = saved.copy(customLevels = (saved.customLevels.filterNot { it.number == remote.level } + local).takeLast(100))
                                                    store.write(saved)
                                                    selectedLevel = remote.level
                                                    selectedTab = MatchThreeTab.PLAY
                                                    notice = "Глава ${remote.level} сохранена и опубликована для игроков"
                                                }
                                                .onFailure { notice = it.message ?: "Не удалось опубликовать уровень" }
                                            saving = false
                                        }
                                    }, modifier = Modifier.fillMaxWidth(), enabled = editorPreview != null && !saving) {
                                        Text(if (editorPreview == null) "Исправьте расстановку" else editorEditingLevel?.let { "Сохранить главу $it" } ?: "Сохранить главу")
                                    }
                                    adminLevels.forEach { level ->
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Text("Глава ${level.level} · цель ${level.target} · ${level.moves} ходов", Modifier.weight(1f), color = Color(0xFFCFD6E3), style = MaterialTheme.typography.bodySmall)
                                            TextButton(onClick = {
                                                editorEditingLevel = level.level
                                                editorTarget = level.target
                                                editorMoves = level.moves
                                                editorColor = level.targetColor
                                                editorSeed = level.seed
                                                editorObstacles = level.obstacles.mapIndexedNotNull { index, kind -> if (kind < 0) null else index to Obstacle.entries.getOrElse(kind) { Obstacle.ROCK } }.toMap()
                                                editorObstacleCount = level.obstacles.count { it >= 0 }
                                            }) { Text("Изменить") }
                                            TextButton(onClick = {
                                                scope.launch {
                                                    gamesRepository.deletePillMatchAdminLevel(level.level)
                                                        .onSuccess {
                                                            adminLevels = adminLevels.filterNot { it.level == level.level }
                                                            publishedLevels = publishedLevels.filterNot { it.level == level.level }
                                                            notice = "Глава ${level.level} удалена"
                                                        }
                                                        .onFailure { notice = it.message ?: "Не удалось удалить главу" }
                                                }
                                            }) { Text("Удалить") }
                                        }
                                    }
                                    notice?.let { Text(it, color = Color(0xFFFFD689), style = MaterialTheme.typography.bodySmall) }
                                }
                            }
                        }
                    } else item { Text("Раздел доступен только администратору.", color = Color.White) }
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
private fun obsLabel(obs: Obstacle) = when (obs) { Obstacle.ROCK -> "Камень"; Obstacle.ICE -> "Лёд"; Obstacle.CHAIN -> "Цепь" }

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
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$collected/$target", color = Color(0xFFFFD689), fontWeight = FontWeight.Black); Text("$moves ходов", color = Color(0xFFBAC5D6), style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable private fun MatchBoard(board: List<Int>, obstacles: Map<Int, Obstacle>, editor: Boolean = false, matchedFxCells: Set<Int> = emptySet(), matchedFxFrame: Int = 0, rejectedCells: Set<Int> = emptySet(), allowTap: Boolean = editor, onCell: (Int) -> Unit, onSwipe: (Int, Int) -> Unit, modifier: Modifier = Modifier, stretchHeight: Boolean = false) {
    val boardModifier = if (stretchHeight) modifier else modifier.fillMaxWidth().aspectRatio(1f)
    Box(boardModifier.clip(RoundedCornerShape(21.dp)).background(Color(0xFF17223B).copy(alpha = .97f)).border(1.dp, Color(0xFFFFD689).copy(alpha = .48f), RoundedCornerShape(21.dp)).padding(6.dp)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(8) { row ->
                val rowModifier = if (stretchHeight) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth()
                Row(rowModifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(8) { col ->
                        val index = row * 8 + col
                        val tapModifier = if (allowTap) Modifier.clickable { onCell(index) } else Modifier
                        val swipeModifier = if (!editor) Modifier.pointerInput(index, board, obstacles, onSwipe) {
                            var dragX = 0f
                            var dragY = 0f
                            var sent = false
                            detectDragGestures(
                                onDragStart = { dragX = 0f; dragY = 0f; sent = false },
                                onDragEnd = { sent = false },
                                onDragCancel = { sent = false },
                            ) { change, amount ->
                                dragX += amount.x
                                dragY += amount.y
                                val threshold = size.width * .28f
                                if (!sent && (kotlin.math.abs(dragX) >= threshold || kotlin.math.abs(dragY) >= threshold)) {
                                    MatchThreeEngine.swipeTarget(index, dragX, dragY, threshold)?.let { onSwipe(index, it) }
                                    sent = true
                                }
                                change.consume()
                            }
                        } else Modifier
                        val cellModifier = if (stretchHeight) Modifier.weight(1f).fillMaxHeight() else Modifier.weight(1f).aspectRatio(1f)
                        val description = "Ряд ${row + 1}, столбец ${col + 1}: ${obstacles[index]?.let(::obsLabel) ?: board.getOrNull(index)?.takeIf { it >= 0 }?.let { "${colorName(it)} самоцвет" } ?: "пусто"}"
                        Box(cellModifier.then(swipeModifier).then(tapModifier).semantics { contentDescription = description }.clip(RoundedCornerShape(10.dp)).background(if (index in rejectedCells) Color(0xFF8B3448) else if ((row + col) % 2 == 0) Color(0xFF29354E) else Color(0xFF222E46)), contentAlignment = Alignment.Center) {
                            val gem = board.getOrElse(index) { -1 }
                            if (gem >= 0) Gem(gem, Modifier.fillMaxSize(.94f).padding(1.dp))
                            if (index in matchedFxCells) {
                                Image(painter = painterResource(matchFxFrames[matchedFxFrame.coerceIn(matchFxFrames.indices)]), contentDescription = null, modifier = Modifier.fillMaxSize())
                            }
                            obstacles[index]?.let { obs ->
                                val obstacleIcon = when (obs) {
                                    Obstacle.ROCK -> ru.tomilo.lib.mobile.R.drawable.pill_obstacle_rock
                                    Obstacle.ICE -> ru.tomilo.lib.mobile.R.drawable.pill_obstacle_ice
                                    Obstacle.CHAIN -> ru.tomilo.lib.mobile.R.drawable.pill_obstacle_chain
                                }
                                Image(painter = painterResource(obstacleIcon), contentDescription = obsLabel(obs), contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(.9f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun Gem(color: Int, modifier: Modifier = Modifier) {
    val safe = color.coerceIn(0, 4)
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(painter = painterResource(gemSprites[safe]), contentDescription = "${colorName(safe)} самоцвет", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
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
