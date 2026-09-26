package ru.tomilo.lib.mobile.ui.screens.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
private enum class RankPeriod(val title: String, val apiValue: String) { WEEK("Неделя", "week"), MONTH("Месяц", "month"), ALL("Всё время", "all") }
private val gemColors = listOf(Color(0xFFFF6F7D), Color(0xFF47D4E7), Color(0xFFFFC64F), Color(0xFFB18AFF), Color(0xFF73D99A))
private val gemMarks = listOf("✦", "◆", "☾", "✿", "●")

@Composable
fun MatchThreeScreen(user: UserDto?, gamesRepository: GamesRepository, modifier: Modifier = Modifier) {
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
    var obstacles by remember { mutableStateOf<Map<Int, Obstacle>>(emptyMap()) }
    var moves by rememberSaveable { mutableIntStateOf(0) }
    var collected by rememberSaveable { mutableIntStateOf(0) }
    var target by rememberSaveable { mutableIntStateOf(18) }
    var targetColor by rememberSaveable { mutableIntStateOf(0) }
    var boosterHammer by rememberSaveable { mutableIntStateOf(2) }
    var boosterRainbow by rememberSaveable { mutableIntStateOf(1) }
    var boosterShuffle by rememberSaveable { mutableIntStateOf(1) }
    var selectedCell by rememberSaveable { mutableIntStateOf(-1) }
    var activeBooster by rememberSaveable { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var rankPeriod by rememberSaveable { mutableStateOf(RankPeriod.WEEK) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var saving by rememberSaveable { mutableStateOf(false) }
    var editorObstacle by rememberSaveable { mutableStateOf(Obstacle.ROCK) }
    var editorTarget by rememberSaveable { mutableIntStateOf(22) }
    var editorMoves by rememberSaveable { mutableIntStateOf(24) }
    var editorColor by rememberSaveable { mutableIntStateOf(0) }
    var editorSeed by rememberSaveable { mutableIntStateOf(810) }
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
    val furthestCompleted = (saved.records.map { it.level } + remoteCompleted).maxOrNull() ?: 0
    val remoteMinutes = remoteNextLifeAt?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }
        ?.let { ((it - now + 59_999L) / 60_000L).coerceAtLeast(0).toInt() } ?: 0

    LaunchedEffect(Unit) {
        saved = store.read().normalized(capacity)
        store.write(saved)
        if (user?.stableId().isNullOrBlank()) selectedLevel = (saved.records.maxOfOrNull { it.level } ?: 0) + 1
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
                selectedLevel = (state.completedLevels.maxOrNull() ?: 0) + 1
                val session = state.session
                if (session != null && !session.finished && session.moves > 0) {
                    board = session.board.map { color -> listOf("coral", "cyan", "lemon", "violet", "mint").indexOf(color).coerceAtLeast(0) }
                    obstacles = session.obstacles.mapIndexedNotNull { i, kind -> if (kind < 0) null else i to Obstacle.entries.getOrElse(kind) { Obstacle.ROCK } }.toMap()
                    moves = session.moves; collected = session.collected; target = session.target; targetColor = session.targetColor
                    boosterHammer = session.boosters.hammer; boosterRainbow = session.boosters.rainbow; boosterShuffle = session.boosters.shuffle
                    remoteNextLifeAt = state.lives.nextLifeAt
                    selectedLevel = session.level
                    finished = false
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
    LaunchedEffect(Unit) {
        gamesRepository.pillMatchPublishedLevels().onSuccess { publishedLevels = it }
    }
    LaunchedEffect(selectedTab, rankPeriod, user?.stableId()) {
        if (selectedTab == MatchThreeTab.RANK) {
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
        if (user?.stableId().isNullOrBlank() && refreshed.lives <= 0) { notice = "Сердца восстановятся через ${refreshed.nextLifeMinutes(capacity)} мин."; return }
        selectedLevel = number
        board = MatchThreeEngine.createBoard(spec.seed, spec.obstacles)
        obstacles = spec.obstacles
        target = spec.target
        targetColor = spec.color
        moves = spec.moves
        collected = 0
        boosterHammer = 2; boosterRainbow = 1; boosterShuffle = 1
        selectedCell = -1; activeBooster = ""; finished = false; playing = true; notice = null; remoteMode = false
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
            scope.launch { store.write(nextSave) }
        }
    }

    fun applyRemoteState(state: ru.tomilo.lib.mobile.data.api.PillMatchActionDto, session: PillMatchSessionDto = state.session) {
        board = session.board.map { color -> listOf("coral", "cyan", "lemon", "violet", "mint").indexOf(color).coerceAtLeast(0) }
        obstacles = session.obstacles.mapIndexedNotNull { i, kind -> if (kind < 0) null else i to Obstacle.entries.getOrElse(kind) { Obstacle.ROCK } }.toMap()
        moves = session.moves; collected = session.collected; target = session.target; targetColor = session.targetColor
        boosterHammer = session.boosters.hammer; boosterRainbow = session.boosters.rainbow; boosterShuffle = session.boosters.shuffle
        remoteLives = state.lives.current; remoteCapacity = state.lives.capacity; remoteCompleted = state.completedLevels
        remoteNextLifeAt = state.lives.nextLifeAt
    }

    fun recordWin() {
        if (finished) return
        finished = true
        if (remoteMode) {
            remoteMode = true
            scope.launch {
                saving = true
                gamesRepository.completePillMatchLevel(selectedLevel).onSuccess { result ->
                    remoteCompleted = (remoteCompleted + selectedLevel).distinct()
                    notice = if (result.awarded) "Уровень пройден! +${result.xpGained} опыта ✨" else "Уровень пройден! Новая глава уже открыта ✨"
                }.onFailure { finished = false; notice = it.message ?: "Не удалось сохранить прохождение. Нажмите кнопку повтора." }
                saving = false
            }
            return
        }
        val record = MatchThreeRecord(selectedLevel, System.currentTimeMillis())
        val next = saved.copy(records = (saved.records + record).distinctBy { it.level to it.at }.takeLast(2000))
        saved = next
        scope.launch { store.write(next) }
        notice = "Уровень пройден! Новая глава уже открыта ✨"
    }

    fun applyMove(result: MatchThreeEngine.Move) {
        board = result.board; obstacles = result.obstacles
        collected = (collected + result.collected).coerceAtMost(target)
        moves = (moves - 1).coerceAtLeast(0)
        selectedCell = -1
        if (collected >= target) recordWin() else if (moves == 0) notice = "Ходы закончились. Можно начать эту главу заново."
    }

    fun playCell(index: Int) {
        if (!playing || finished || moves <= 0 || saving) return
        if (activeBooster.isNotEmpty()) {
            if (remoteMode) {
                val booster = activeBooster
                scope.launch { saving = true; gamesRepository.pillMatchBooster(booster, index).onSuccess { applyRemoteState(it) }.onFailure { notice = it.message }; saving = false }
                activeBooster = ""; return
            }
            when (activeBooster) {
                "hammer" -> if (boosterHammer > 0) MatchThreeEngine.clearAt(board, obstacles, index, targetColor, now.toInt())?.let { applyMove(it); boosterHammer-- }
                "rainbow" -> if (boosterRainbow > 0) { val color = board.getOrNull(index) ?: return; applyMove(MatchThreeEngine.clearColor(board, obstacles, color, targetColor, now.toInt())); boosterRainbow-- }
            }
            activeBooster = ""; return
        }
        if (index in obstacles) { selectedCell = index; return }
        if (selectedCell < 0 || !MatchThreeEngine.adjacent(selectedCell, index)) { selectedCell = index; return }
        if (remoteMode) {
            val from = selectedCell
            scope.launch { saving = true; gamesRepository.pillMatchMove(from, index).onSuccess { action ->
                if (action.validMove) {
                    applyRemoteState(action)
                    if (action.session.collected >= action.session.target) recordWin()
                } else notice = "Нужно собрать три или больше самоцветов."
                selectedCell = -1
            }.onFailure { notice = it.message ?: "Не удалось отправить ход" }; saving = false }
        } else MatchThreeEngine.swap(board, obstacles, selectedCell, index, targetColor, now.toInt())?.let(::applyMove)
            ?: run { selectedCell = index; notice = "Свайпните соседние камни, чтобы собрать три в ряд." }
    }

    Box(modifier.fillMaxSize()) {
        Image(
            painter = painterResource(ru.tomilo.lib.mobile.R.drawable.pill_lab_backdrop),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x8817243B), Color(0xC6171A32), Color(0xE0101624)))))
        StarryBackdrop(Modifier.fillMaxSize())
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
                        MatchThreeHero(
                            level = selectedLevel, lives = if (remoteLives >= 0) remoteLives else saved.lives, capacity = if (remoteLives >= 0) remoteCapacity else capacity, premium = premium, minutes = if (remoteLives >= 0) remoteMinutes else saved.nextLifeMinutes(capacity),
                            completed = (saved.records.map { it.level } + remoteCompleted).distinct().size, onPremium = { notice = "Премиум увеличивает запас сердец до 8." },
                        )
                    }
                    if (!playing) item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF20263A).copy(alpha = .92f)), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color(0xFFB7C5EF).copy(alpha = .15f))) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                                Text("САД СОЗВЕЗДИЙ", color = Color(0xFFFFD689), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                Text("Глава $selectedLevel · ${levelSpec(selectedLevel).moves} хода", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Соберите ${levelSpec(selectedLevel).target} ${colorName(levelSpec(selectedLevel).color)} фишек. Камни мешают обмену, лёд трескается, цепи отпускают самоцветы.", color = Color(0xFFB8C3D8), style = MaterialTheme.typography.bodyMedium)
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
                            MatchBoard(board, obstacles, selectedCell, selectedTab == MatchThreeTab.EDITOR, onCell = ::playCell)
                            Spacer(Modifier.height(5.dp))
                            Text("Перетащите самоцвет в соседнюю клетку", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color(0xFFB9C4D8), style = MaterialTheme.typography.labelSmall)
                        }
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BoosterButton("🔨", "Молот", boosterHammer, activeBooster == "hammer", Modifier.weight(1f), enabled = !finished && moves > 0) { activeBooster = if (activeBooster == "hammer") "" else "hammer" }
                                BoosterButton("🌈", "Радуга", boosterRainbow, activeBooster == "rainbow", Modifier.weight(1f), enabled = !finished && moves > 0) { activeBooster = if (activeBooster == "rainbow") "" else "rainbow" }
                                BoosterButton("🌀", "Микс", boosterShuffle, false, Modifier.weight(1f), enabled = !finished && moves > 0) {
                                    if (remoteMode) scope.launch { saving = true; gamesRepository.pillMatchBooster("shuffle").onSuccess { applyRemoteState(it); notice = "Самоцветы перемешаны" }.onFailure { notice = it.message }; saving = false }
                                    else if (boosterShuffle > 0) { board = MatchThreeEngine.shuffle(board, obstacles, now.toInt()); boosterShuffle--; notice = "Самоцветы перемешаны" }
                                }
                            }
                        }
                        item {
                            if (finished) Button(onClick = { playing = false; selectedLevel++ }, modifier = Modifier.fillMaxWidth(), enabled = !saving) { Text(if (saving) "Сохраняем награду…" else "Следующая глава →") }
                            else if (collected >= target) Button(onClick = ::recordWin, modifier = Modifier.fillMaxWidth(), enabled = !saving) { Text(if (saving) "Сохраняем прохождение…" else "Завершить главу") }
                            else if (moves == 0) OutlinedButton(onClick = { playing = false; startLevel(selectedLevel) }, modifier = Modifier.fillMaxWidth()) { Text("Повторить главу") }
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
                    if (rankLoading) item { Text("Загружаем таблицу лидеров…", color = Color(0xFFBAC5D6)) }
                    rankError?.let { message -> item { Text(message, color = Color(0xFFFF99A9)) } }
                    if (!rankLoading && rankError == null && rankUsers.isEmpty()) item {
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
                    item { Text("Рейтинг синхронизируется с сервером. Период: ${rankPeriod.title.lowercase()}.", color = Color(0xFF909CB4), style = MaterialTheme.typography.bodySmall) }
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
                                        TextButton(onClick = { editorEditingLevel = null; editorObstacles = emptyMap(); editorSeed++ }) { Text("Новый уровень") }
                                    }
                                    Text("Параметры: цель $editorTarget · $editorMoves ходов · ${colorName(editorColor)}", color = Color(0xFFCCD3E1), style = MaterialTheme.typography.bodySmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(onClick = { editorTarget = (editorTarget - 2).coerceAtLeast(8) }) { Text("Цель −") }
                                        OutlinedButton(onClick = { editorTarget = (editorTarget + 2).coerceAtMost(40) }) { Text("Цель +") }
                                        OutlinedButton(onClick = { editorMoves = (editorMoves - 1).coerceAtLeast(12) }) { Text("Ходы −") }
                                        OutlinedButton(onClick = { editorMoves = (editorMoves + 1).coerceAtMost(35) }) { Text("Ходы +") }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { gemColors.indices.forEach { i -> FilterChip(selected = editorColor == i, onClick = { editorColor = i }, label = { Text(gemMarks[i]) }) } }
                                    Text("Препятствие", color = Color(0xFFBFC9DB), style = MaterialTheme.typography.labelMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Obstacle.entries.forEach { obs -> FilterChip(selected = editorObstacle == obs, onClick = { editorObstacle = obs }, label = { Text(obsLabel(obs)) }) }
                                        TextButton(onClick = { editorObstacles = emptyMap() }) { Text("Очистить") }
                                    }
                                    Button(onClick = {
                                        editorSeed += 1
                                        val rng = kotlin.random.Random(editorSeed)
                                        val count = (editorSeed % 11) + 4
                                        editorObstacles = buildMap { repeat(count) { var cell = rng.nextInt(64); while (cell in this || cell in setOf(0,7,56,63,27,28,35,36)) cell = rng.nextInt(64); put(cell, Obstacle.entries.random(rng)) } }
                                    }, modifier = Modifier.fillMaxWidth()) { Text("✦ Сгенерировать расстановку") }
                                    Text("Нажмите на клетки поля, чтобы поставить или убрать выбранное препятствие.", color = Color(0xFF9DAAC2), style = MaterialTheme.typography.bodySmall)
                                    MatchBoard(MatchThreeEngine.createBoard(editorSeed, editorObstacles), editorObstacles, -1, true) { cell -> editorObstacles = editorObstacles.toMutableMap().apply { if (containsKey(cell)) remove(cell) else put(cell, editorObstacle) } }
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
                                    }, modifier = Modifier.fillMaxWidth()) { Text(editorEditingLevel?.let { "Сохранить главу $it" } ?: "Сохранить главу") }
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
private fun obsLabel(obs: Obstacle) = when (obs) { Obstacle.ROCK -> "🪨 Камень"; Obstacle.ICE -> "❄ Лёд"; Obstacle.CHAIN -> "⛓ Цепь" }

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

@Composable private fun MatchBoard(board: List<Int>, obstacles: Map<Int, Obstacle>, selected: Int, editor: Boolean = false, onCell: (Int) -> Unit) {
    var startCell by remember { mutableIntStateOf(-1) }
    var dragX by remember { mutableStateOf(0f) }; var dragY by remember { mutableStateOf(0f) }
    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(21.dp)).background(Color(0xFF17223B).copy(alpha = .97f)).border(1.dp, Color(0xFFFFD689).copy(alpha = .48f), RoundedCornerShape(21.dp)).padding(6.dp)) {
        Column(Modifier.fillMaxSize().pointerInput(board, obstacles, editor) {
            detectDragGestures(onDragStart = { point ->
                val unit = size.width / 8f
                startCell = ((point.y / unit).toInt().coerceIn(0, 7) * 8 + (point.x / unit).toInt().coerceIn(0, 7))
                dragX = 0f; dragY = 0f
            }, onDragEnd = {
                if (startCell >= 0 && (kotlin.math.abs(dragX) + kotlin.math.abs(dragY) > size.width / 24f)) {
                    val next = when { kotlin.math.abs(dragX) > kotlin.math.abs(dragY) -> startCell + if (dragX > 0) 1 else -1; else -> startCell + if (dragY > 0) 8 else -8 }
                    if (next in 0..63 && MatchThreeEngine.adjacent(startCell, next)) {
                        if (editor) onCell(next) else { onCell(startCell); onCell(next) }
                    }
                }
                startCell = -1
            }) { change, amount -> dragX += amount.x; dragY += amount.y; change.consume() }
        }, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(8) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(8) { col ->
                        val index = row * 8 + col
                        Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(if ((row + col) % 2 == 0) Color(0xFF29354E) else Color(0xFF222E46)).clickable { onCell(index) }.border(if (selected == index) 2.dp else 0.dp, if (selected == index) Color(0xFFFFEB91) else Color.Transparent, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Gem(board.getOrElse(index) { 0 }, Modifier.fillMaxSize(.77f).padding(1.dp), highlight = selected == index)
                            obstacles[index]?.let { obs ->
                                Box(Modifier.fillMaxSize().background(Color(0x990E1728), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Text(when (obs) { Obstacle.ROCK -> "⬟"; Obstacle.ICE -> "❄"; Obstacle.CHAIN -> "⛓" }, color = when (obs) { Obstacle.ROCK -> Color(0xFFD5C3B8); Obstacle.ICE -> Color(0xFFB7F4FF); Obstacle.CHAIN -> Color(0xFFFFD489) }, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun Gem(color: Int, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val safe = color.coerceIn(0, 4)
    Box(modifier.clip(RoundedCornerShape(11.dp)).background(Brush.linearGradient(listOf(gemColors[safe].copy(alpha = .82f), gemColors[safe], gemColors[safe].copy(alpha = .68f)))).border(if (highlight) 2.dp else 1.dp, if (highlight) Color(0xFFFFF0A6) else Color.White.copy(alpha = .48f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = .17f), radius = size.minDimension * .44f)
            drawCircle(Color.White.copy(alpha = .48f), radius = size.minDimension * .075f, center = androidx.compose.ui.geometry.Offset(size.width * .29f, size.height * .25f))
        }
        Text(gemMarks[safe], color = Color.White.copy(alpha = .95f), fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}

@Composable private fun BoosterButton(symbol: String, label: String, count: Int, active: Boolean, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(modifier = modifier.height(60.dp).clickable(enabled = enabled && count > 0, onClick = onClick), shape = RoundedCornerShape(16.dp), color = if (active) Color(0xFF655281) else Color(0xFF212A40), border = BorderStroke(1.dp, if (active) Color(0xFFFFD689) else Color.White.copy(alpha = .12f))) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text("$symbol $count", color = if (count > 0) Color.White else Color(0xFF78849A), fontWeight = FontWeight.Bold); Text(label, color = Color(0xFFD1D8E4), style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable private fun StarryBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val points = listOf(.08f to .06f, .88f to .1f, .72f to .23f, .14f to .35f, .93f to .43f, .06f to .68f, .78f to .81f, .91f to .92f)
        points.forEachIndexed { i, point -> drawCircle(Color(0xFFFFD689).copy(alpha = if (i % 2 == 0) .33f else .16f), radius = if (i % 3 == 0) 3f else 1.5f, center = androidx.compose.ui.geometry.Offset(size.width * point.first, size.height * point.second)) }
    }
}
