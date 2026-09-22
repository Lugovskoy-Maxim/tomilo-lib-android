package ru.tomilo.lib.mobile.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.R
import ru.tomilo.lib.mobile.data.api.GameAlchemyStatusDto
import ru.tomilo.lib.mobile.data.api.GameInventoryItemDto
import ru.tomilo.lib.mobile.data.repo.GamesRepository
import ru.tomilo.lib.mobile.ui.theme.*

@Composable fun InventoryScreen(items: List<GameInventoryItemDto>) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { Text("Материалы и расходники", color = TomiloMuted) }
        items(items, key = { it.itemId }) { item -> Surface(color = TomiloSurface, shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, TomiloBorder)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Inventory2, null, tint = TomiloPrimary); Spacer(Modifier.width(12.dp)); Text(item.name?.ifBlank { item.itemId } ?: item.itemId, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Text("×${item.count}", fontWeight = FontWeight.Bold) } } }
    }
}

/** Уровень 3-в-ряд: тот же рецепт, поле и прогресс профиля, что и в веб-версии. */
@Composable fun AlchemyScreen(status: GameAlchemyStatusDto, gamesRepository: GamesRepository) {
    var moves by remember { mutableIntStateOf(22) }; var target by remember { mutableIntStateOf(0) }; var flash by remember { mutableStateOf(false) }; var effect by remember { mutableStateOf<String?>(null) }; var selected by remember { mutableStateOf<Int?>(null) }
    var completion by remember { mutableStateOf<String?>(null) }; var saving by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    var profileLevel by remember { mutableIntStateOf(1) }; var profileXp by remember { mutableIntStateOf(0) }; var profileXpNext by remember { mutableIntStateOf(100) }
    LaunchedEffect(Unit) { gamesRepository.pillMatchState().onSuccess { profileLevel = it.profileLevel; profileXp = it.experience; profileXpNext = it.experienceToNext } }
    LaunchedEffect(flash) { if (flash) { delay(550); flash = false } }
    LaunchedEffect(effect) { if (effect != null) { delay(550); effect = null } }
    val colors = listOf(Color(0xFFE84B68), Color(0xFF35BADA), Color(0xFFFFCD4A), Color(0xFF9255DD), Color(0xFF4EC68D))
    val board = remember { mutableStateListOf(0,1,4,3,0,2,1,4,4,0,1,2,3,4,0,1,2,3,0,0,0,0,4,2,1,4,2,3,4,1,2,0,0,2,4,1,3,0,4,2,3,1,0,2,4,3,1,0,4,0,3,1,2,4,0,3,1,2,4,0,3,1,2,4) }
    val onPillClick: (Int) -> Unit = { index ->
        val first = selected
        if (first == null || !pillAdjacent(first, index)) {
            selected = index
        } else {
            val next = board.toMutableList()
            val held = next[first]
            next[first] = next[index]
            next[index] = held
            val matches = pillMatches(next)
            if (matches.isNotEmpty()) {
                val coral = matches.count { next[it] == 0 }
                board.indices.forEach { cell ->
                    board[cell] = if (cell in matches) (cell * 7 + matches.size) % colors.size else next[cell]
                }
                moves--
                target = (target + coral).coerceAtMost(16)
                flash = matches.size >= 4
                effect = if (matches.size >= 5) "burst" else if (matches.size == 4) "line" else null
            }
            selected = null
        }
    }
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.pill_lab_backdrop), null, Modifier.fillMaxSize().alpha(.25f), contentScale = ContentScale.Crop)
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Рецепт бодрости", fontWeight = FontWeight.Bold); Text("УРОВЕНЬ 18 · профиль $profileLevel ур. · $profileXp / $profileXpNext XP", color = Color(0xFFFFD36E)) }; Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$moves", color = Color(0xFFFFD36E), fontWeight = FontWeight.Bold); Text("хода", color = TomiloMuted) } }
            Surface(color = Color(0xFF120D2B).copy(alpha = .9f), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFFFD36E).copy(alpha = .5f))) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Pill(Color(0xFFE84B68)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("Соберите коралловые пилюли", fontWeight = FontWeight.Bold); Text("4 в ряд — линия · 5 в ряд — вспышка", color = TomiloMuted) }; Text("$target/16", color = Color(0xFFFFD36E), fontWeight = FontWeight.Bold) } }
            Surface(Modifier.fillMaxWidth(), color = Color(0xFF0B0824).copy(alpha = .9f), shape = RoundedCornerShape(22.dp), border = BorderStroke(2.dp, Color(0xFFFFD36E).copy(alpha = .65f))) { Box { Column(Modifier.padding(7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { repeat(8) { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { repeat(8) { column -> val index = row * 8 + column; val color = board[index]; val shape = pillShape(color); Box(Modifier.weight(1f).aspectRatio(1f).clickable(enabled = moves > 0 && completion == null) { onPillClick(index) }.background(colors[color], shape).border(if (selected == index) 3.dp else 2.dp, if (selected == index) Color(0xFFFFF3A6) else Color.White.copy(alpha = .42f), shape)) { if (color == 1) Box(Modifier.fillMaxWidth(.6f).height(2.dp).align(Alignment.Center).background(Color.White.copy(alpha = .72f), CircleShape)); if (color == 0) Box(Modifier.fillMaxHeight(.58f).width(2.dp).align(Alignment.Center).background(Color.White.copy(alpha = .58f))); if (flash && index % 8 == 3) Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.align(Alignment.Center)) } } } } }; if (effect == "line") Box(Modifier.fillMaxWidth(.88f).height(9.dp).align(Alignment.Center).background(Color(0xFFFFF6B5).copy(alpha = .9f), CircleShape)); if (effect == "burst") Box(Modifier.size(118.dp).align(Alignment.Center).background(Brush.radialGradient(listOf(Color(0xFFFFF8C0), Color(0xFFFF7D8C).copy(alpha = .55f), Color.Transparent)), CircleShape)) } }
            if (target >= 16 && completion == null) Button(onClick = { scope.launch { saving = true; gamesRepository.completePillMatchLevel(18).onSuccess { profileLevel = it.profileLevel; profileXp = it.experience; profileXpNext = it.experienceToNext; completion = if (it.awarded) "Рецепт готов · +${it.xpGained} XP профиля" else "Этот уровень уже пройден" }.onFailure { completion = it.message ?: "Не удалось сохранить награду" }; saving = false } }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text(if (saving) "Сохраняем…" else "Завершить рецепт · +56 XP") }
            completion?.let { Text(it, color = Color(0xFFFFE7A3), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally)) }
            Text("Перед началом выберите дополнение", color = TomiloMuted, modifier = Modifier.align(Alignment.CenterHorizontally)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) { Booster("Разбить", "×2", Color(0xFFFFC94A), Modifier.weight(1f)); Booster("Перемешать", "×1", Color(0xFF8D70EB), Modifier.weight(1f)) }
        }
    }
}
@Composable private fun Pill(color: Color) = Box(Modifier.size(34.dp).background(color, CircleShape).border(2.dp, Color.White.copy(alpha = .45f), CircleShape))
@Composable private fun Booster(title: String, amount: String, color: Color, modifier: Modifier) = Surface(modifier, color = TomiloSurface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, color.copy(alpha = .7f))) { Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Icon(Icons.Default.AutoAwesome, null, tint = color); Spacer(Modifier.width(6.dp)); Text(title); Spacer(Modifier.width(5.dp)); Text(amount, color = color, fontWeight = FontWeight.Bold) } }
private fun pillShape(color: Int) = when (color) { 0 -> RoundedCornerShape(50); 1 -> RoundedCornerShape(25); 2 -> RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomEndPercent = 34, bottomStartPercent = 34); 3 -> RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 3.dp, bottomStart = 14.dp); else -> RoundedCornerShape(topStartPercent = 42, topEndPercent = 58, bottomEndPercent = 52, bottomStartPercent = 48) }
private fun pillAdjacent(a: Int, b: Int) = kotlin.math.abs(a / 8 - b / 8) + kotlin.math.abs(a % 8 - b % 8) == 1
private fun pillMatches(board: List<Int>): Set<Int> { val found = mutableSetOf<Int>(); for (row in 0 until 8) { var start = 0; for (col in 1..8) { if (col < 8 && board[row * 8 + col] == board[row * 8 + start]) continue; if (col - start >= 3) for (x in start until col) found += row * 8 + x; start = col } }; for (col in 0 until 8) { var start = 0; for (row in 1..8) { if (row < 8 && board[row * 8 + col] == board[start * 8 + col]) continue; if (row - start >= 3) for (y in start until row) found += y * 8 + col; start = row } }; return found }
