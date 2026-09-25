package ru.tomilo.lib.mobile.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.R
import ru.tomilo.lib.mobile.data.api.GameInventoryItemDto
import ru.tomilo.lib.mobile.data.repo.GamesRepository
import ru.tomilo.lib.mobile.core.toUserFacingError
import ru.tomilo.lib.mobile.core.PillMatchRules
import ru.tomilo.lib.mobile.ui.components.TomiloBottomBarContentGap
import ru.tomilo.lib.mobile.ui.theme.*

@Composable fun InventoryScreen(items: List<GameInventoryItemDto>) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { Text("Материалы и расходники", color = TomiloMuted) }
        items(items, key = { it.itemId }) { item -> Surface(color = TomiloSurface, shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, TomiloBorder)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Inventory2, null, tint = TomiloPrimary); Spacer(Modifier.width(12.dp)); Text(item.name?.ifBlank { item.itemId } ?: item.itemId, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Text("×${item.count}", fontWeight = FontWeight.Bold) } } }
    }
}

/** Уровень 3-в-ряд: тот же рецепт, поле и прогресс профиля, что и в веб-версии. */
@Composable fun AlchemyScreen(gamesRepository: GamesRepository, modifier: Modifier = Modifier) {
    var moves by rememberSaveable { mutableIntStateOf(22) }; var target by rememberSaveable { mutableIntStateOf(0) }; var flashedCells by remember { mutableStateOf<Set<Int>>(emptySet()) }; var flashSequence by remember { mutableIntStateOf(0) }; var effect by remember { mutableStateOf<String?>(null) }; var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    var completion by rememberSaveable { mutableStateOf<String?>(null) }; var completionError by remember { mutableStateOf<String?>(null) }; var saving by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    var profileLevel by rememberSaveable { mutableIntStateOf(1) }; var profileXp by rememberSaveable { mutableIntStateOf(0) }; var profileXpNext by rememberSaveable { mutableIntStateOf(100) }
    LaunchedEffect(Unit) { gamesRepository.pillMatchState().onSuccess { profileLevel = it.profileLevel; profileXp = it.experience; profileXpNext = it.experienceToNext } }
    LaunchedEffect(flashSequence) { if (flashSequence > 0) { delay(550); flashedCells = emptySet() } }
    LaunchedEffect(effect) { if (effect != null) { delay(550); effect = null } }
    val colors = listOf(Color(0xFFE84B68), Color(0xFF35BADA), Color(0xFFFFCD4A), Color(0xFF9255DD), Color(0xFF4EC68D))
    val pillNames = listOf("коралловая", "голубая", "жёлтая", "фиолетовая", "зелёная")
    var board by rememberSaveable { mutableStateOf(PillMatchRules.initialBoard) }
    val onPillClick: (Int) -> Unit = { index ->
        val first = selected
        if (first == null || !PillMatchRules.isAdjacent(first, index)) {
            selected = index
        } else {
            val move = PillMatchRules.swap(board.toList(), first, index, refillSeed = moves)
            if (move != null) {
                board = move.board
                moves--
                target = (target + move.coralMatched).coerceAtMost(16)
                flashedCells = if (move.matchedCells.size >= 4) move.matchedCells else emptySet()
                flashSequence++
                effect = if (move.matchedCells.size >= 5) "burst" else if (move.matchedCells.size == 4) "line" else null
            }
            selected = null
        }
    }
    Box(modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.pill_lab_backdrop), null, Modifier.fillMaxSize().alpha(.25f), contentScale = ContentScale.Crop)
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = TomiloBottomBarContentGap),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("УРОВЕНЬ 18 · профиль $profileLevel ур. · $profileXp / $profileXpNext XP", Modifier.weight(1f), color = Color(0xFFFFD36E)); Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$moves", color = Color(0xFFFFD36E), fontWeight = FontWeight.Bold); Text("хода", color = TomiloMuted) } }
            Surface(color = Color(0xFF120D2B).copy(alpha = .9f), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFFFD36E).copy(alpha = .5f))) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Pill(Color(0xFFE84B68)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("Соберите коралловые пилюли", fontWeight = FontWeight.Bold); Text("3 в ряд — сбор · 4 — линия · 5+ — вспышка", color = TomiloMuted) }; Text("$target/16", color = Color(0xFFFFD36E), fontWeight = FontWeight.Bold) } }
            Surface(Modifier.fillMaxWidth(), color = Color(0xFF0B0824).copy(alpha = .9f), shape = RoundedCornerShape(22.dp), border = BorderStroke(2.dp, Color(0xFFFFD36E).copy(alpha = .65f))) { Box { Column(Modifier.padding(7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { repeat(8) { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { repeat(8) { column -> val index = row * 8 + column; val color = board[index]; val shape = pillShape(color); Box(Modifier.weight(1f).aspectRatio(1f).clickable(enabled = moves > 0 && completion == null) { onPillClick(index) }.semantics { contentDescription = "${pillNames[color]} пилюля, ряд ${row + 1}, столбец ${column + 1}"; role = Role.Button; this.selected = selected == index }.background(colors[color], shape).border(if (selected == index) 3.dp else 2.dp, if (selected == index) Color(0xFFFFF3A6) else Color.White.copy(alpha = .42f), shape)) { if (color == 1) Box(Modifier.fillMaxWidth(.6f).height(2.dp).align(Alignment.Center).background(Color.White.copy(alpha = .72f), CircleShape)); if (color == 0) Box(Modifier.fillMaxHeight(.58f).width(2.dp).align(Alignment.Center).background(Color.White.copy(alpha = .58f))); if (index in flashedCells) Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.align(Alignment.Center)) } } } } }; if (effect == "line") Box(Modifier.fillMaxWidth(.88f).height(9.dp).align(Alignment.Center).background(Color(0xFFFFF6B5).copy(alpha = .9f), CircleShape)); if (effect == "burst") Box(Modifier.size(118.dp).align(Alignment.Center).background(Brush.radialGradient(listOf(Color(0xFFFFF8C0), Color(0xFFFF7D8C).copy(alpha = .55f), Color.Transparent)), CircleShape)) } }
            if (target >= 16 && completion == null) {
                Button(
                    onClick = {
                        scope.launch {
                            saving = true
                            completionError = null
                            try {
                                gamesRepository.completePillMatchLevel(18)
                                    .onSuccess {
                                        profileLevel = it.profileLevel
                                        profileXp = it.experience
                                        profileXpNext = it.experienceToNext
                                        completion = if (it.awarded) {
                                            "Рецепт готов · +${it.xpGained} XP профиля"
                                        } else {
                                            "Этот уровень уже пройден"
                                        }
                                    }
                                    .onFailure {
                                        completionError = it.toUserFacingError("Не удалось сохранить награду.")
                                    }
                            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                throw cancelled
                            } catch (error: Throwable) {
                                completionError = error.toUserFacingError("Не удалось сохранить награду.")
                            } finally {
                                saving = false
                            }
                        }
                    },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (saving) "Сохраняем…" else "Завершить рецепт · +56 XP")
                }
            }
            completionError?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            completion?.let { Text(it, color = Color(0xFFFFE7A3), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally)) }
        }
    }
}
@Composable private fun Pill(color: Color) = Box(Modifier.size(34.dp).background(color, CircleShape).border(2.dp, Color.White.copy(alpha = .45f), CircleShape))
private fun pillShape(color: Int) = when (color) { 0 -> RoundedCornerShape(50); 1 -> RoundedCornerShape(25); 2 -> RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomEndPercent = 34, bottomStartPercent = 34); 3 -> RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 3.dp, bottomStart = 14.dp); else -> RoundedCornerShape(topStartPercent = 42, topEndPercent = 58, bottomEndPercent = 52, bottomStartPercent = 48) }
