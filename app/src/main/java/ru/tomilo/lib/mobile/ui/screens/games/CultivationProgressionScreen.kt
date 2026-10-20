package ru.tomilo.lib.mobile.ui.screens.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.data.api.CultivationDiscipleDto
import ru.tomilo.lib.mobile.data.api.CultivationStageDto
import ru.tomilo.lib.mobile.data.api.GameInventoryItemDto
import ru.tomilo.lib.mobile.data.repo.GamesRepository
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface

private data class CultivationPathOption(val id: String, val label: String, val hint: String)

private val cultivationPaths = listOf(
    CultivationPathOption("fire", "Пламя", "+12% урона техник"),
    CultivationPathOption("water", "Вода", "+6% урона и защита"),
    CultivationPathOption("earth", "Земля", "+4% урона и стойкость"),
    CultivationPathOption("wood", "Древо", "+5% урона и рост"),
    CultivationPathOption("metal", "Металл", "+10% урона"),
)

@Composable
internal fun CultivationProgressionScreen(gamesRepository: GamesRepository, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var disciples by remember { mutableStateOf<List<CultivationDiscipleDto>>(emptyList()) }
    var stages by remember { mutableStateOf<List<CultivationStageDto>>(emptyList()) }
    var inventory by remember { mutableStateOf<List<GameInventoryItemDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busyCharacter by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    suspend fun loadData() {
        loading = true
        gamesRepository.discipleProgression()
            .onSuccess { overview ->
                disciples = overview.disciples.filter { !it.inWarehouse.orFalse() }
                stages = overview.cultivationStages.sortedBy { it.order }
            }
            .onFailure {
                message = it.message ?: "Не удалось загрузить культивацию"
                isError = true
            }
        gamesRepository.gameInventory().onSuccess { inventory = it }
        loading = false
    }
    val refresh: () -> Unit = {
        scope.launch { loadData() }
    }
    LaunchedEffect(Unit) { loadData() }

    fun runAction(characterId: String, action: suspend () -> Result<String>) {
        if (busyCharacter != null) return
        scope.launch {
            busyCharacter = characterId
            action().onSuccess { result ->
                isError = false
                loadData()
                message = result
            }.onFailure {
                message = it.message ?: "Действие не выполнено"
                isError = true
            }
            busyCharacter = null
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(
                color = TomiloSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, TomiloPrimary.copy(alpha = 0.28f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Культивация учеников", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text("Медитация приносит опыт, путь Дао меняет боевой стиль, а прорыв открывает следующую стадию.", color = TomiloMuted)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = refresh, enabled = !loading) { Text(if (loading) "Обновляем…" else "Обновить данные") }
                }
            }
        }
        message?.let { notice ->
            item(key = "cultivation_notice_$notice") {
                Surface(
                    color = (if (isError) Color(0xFFDA716A) else Color(0xFF63A982)).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(notice, Modifier.fillMaxWidth().padding(12.dp), color = if (isError) Color(0xFFDA716A) else TomiloMuted)
                }
            }
        }
        if (loading && disciples.isEmpty()) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
        } else if (!loading && disciples.isEmpty()) {
            item {
                Surface(color = TomiloSurface, shape = RoundedCornerShape(16.dp)) {
                    Text("Активных учеников пока нет. Получите ученика или верните его из казармы.", Modifier.padding(16.dp), color = TomiloMuted)
                }
            }
        } else {
            items(disciples, key = { it.characterId.ifBlank { "${it.internalSlot}:${it.displayName()}" } }) { disciple ->
                CultivationDiscipleCard(
                    disciple = disciple,
                    stages = stages,
                    inventory = inventory,
                    busy = busyCharacter == disciple.characterId,
                    anyBusy = busyCharacter != null,
                    onStart = { runAction(disciple.characterId) {
                        gamesRepository.startMeditation(disciple.characterId).map { "${disciple.displayName()} начал медитацию" }
                    } },
                    onClaim = { runAction(disciple.characterId) {
                        gamesRepository.claimMeditation(disciple.characterId).map { "Медитация завершена: +$it опыта культивации" }
                    } },
                    onStop = { runAction(disciple.characterId) {
                        gamesRepository.stopMeditation(disciple.characterId).map { "Медитация остановлена" }
                    } },
                    onSetPath = { path -> runAction(disciple.characterId) {
                        gamesRepository.setCultivationPath(disciple.characterId, path.id).map { "Путь Дао: ${path.label}" }
                    } },
                    onBreakthrough = { runAction(disciple.characterId) {
                        gamesRepository.breakthrough(disciple.characterId).map { result ->
                            result.message ?: if (result.success) "Прорыв успешен: ${stageLabel(stages, result.cultivationStageKey)}" else "Прорыв не удался, потеряно опыта культивации: ${result.cultivationExpLost ?: 0}"
                        }
                    } },
                )
            }
        }
    }
}

@Composable
private fun CultivationDiscipleCard(
    disciple: CultivationDiscipleDto,
    stages: List<CultivationStageDto>,
    inventory: List<GameInventoryItemDto>,
    busy: Boolean,
    anyBusy: Boolean,
    onStart: () -> Unit,
    onClaim: () -> Unit,
    onStop: () -> Unit,
    onSetPath: (CultivationPathOption) -> Unit,
    onBreakthrough: () -> Unit,
) {
    val currentIndex = stages.indexOfFirst { it.stageKey == disciple.cultivationStageKey }
    val currentStage = stages.getOrNull(currentIndex)
    val nextStage = stages.getOrNull(currentIndex + 1)
    val requirement = nextStage?.breakthroughFromPrevious
    val meditating = !disciple.meditation.startedAt.isNullOrBlank()
    val level = disciple.level ?: 1
    val missingLevel = requirement?.minDiscipleLevel?.let { level < it } == true
    val missingExp = requirement?.minCultivationExp?.let { disciple.cultivationExp < it } == true
    val missingItems = requirement?.itemCosts.orEmpty().filter { cost ->
        inventory.firstOrNull { it.itemId.equals(cost.itemId, ignoreCase = true) }?.count.orZero() < cost.count
    }
    val ready = requirement != null && !missingLevel && !missingExp && missingItems.isEmpty()
    val chance = requirement?.let {
        previewBreakthroughChance(level, disciple.cultivationExp, it.minDiscipleLevel, it.minCultivationExp, nextStage.order)
    }

    Surface(color = TomiloSurface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, TomiloBorder)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(disciple.displayName(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${stageLabel(stages, disciple.cultivationStageKey)} · уровень ${level} · звёзды ${disciple.starLevel}", color = TomiloMuted)
            Text("Опыт культивации: ${disciple.cultivationExp}", fontWeight = FontWeight.SemiBold)

            Text("Путь Дао", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            cultivationPaths.forEach { path ->
                val selected = disciple.path.equals(path.id, ignoreCase = true)
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !anyBusy && !selected) { onSetPath(path) },
                    color = if (selected) TomiloPrimary.copy(alpha = 0.12f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (selected) TomiloPrimary.copy(alpha = 0.45f) else TomiloBorder),
                ) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${if (selected) "✓ " else ""}${path.label}", fontWeight = FontWeight.Medium)
                        Text(path.hint, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Text(if (meditating) "Медитация начата: ${disciple.meditation.startedAt}" else "Медитация не начата", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (meditating) {
                    OutlinedButton(onClick = onClaim, enabled = !anyBusy) { Text("Собрать опыт") }
                    OutlinedButton(onClick = onStop, enabled = !anyBusy) { Text("Остановить") }
                } else {
                    Button(onClick = onStart, enabled = !anyBusy && !disciple.characterId.isBlank()) { Text("Начать медитацию") }
                }
                if (busy) CircularProgressIndicator(modifier = Modifier.padding(10.dp).width(22.dp), strokeWidth = 2.dp)
            }

            if (nextStage != null && requirement != null) {
                Text("Следующая стадия: ${stageLabel(stages, nextStage.stageKey)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Для прорыва: уровень ${requirement.minDiscipleLevel} (${level}), опыт ${requirement.minCultivationExp} (${disciple.cultivationExp})", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                missingItems.forEach { cost ->
                    val have = inventory.firstOrNull { it.itemId.equals(cost.itemId, ignoreCase = true) }?.count.orZero()
                    Text("${inventory.firstOrNull { it.itemId.equals(cost.itemId, true) }?.name ?: cost.itemId}: $have / ${cost.count}", color = Color(0xFFDA716A), style = MaterialTheme.typography.bodySmall)
                }
                if (ready) Text("Шанс успеха: ${chance?.times(100)?.toInt()}%", color = TomiloMuted)
                Button(onClick = onBreakthrough, enabled = ready && !anyBusy && !meditating && !disciple.characterId.isBlank()) {
                    Text("Прорыв")
                }
            } else if (currentStage != null && currentIndex == stages.lastIndex) {
                Text("Достигнута последняя доступная стадия", color = TomiloMuted)
            }
        }
    }
}

private fun stageLabel(stages: List<CultivationStageDto>, key: String): String =
    stages.firstOrNull { it.stageKey == key }?.displayName?.takeIf(String::isNotBlank) ?: key.ifBlank { "Начальная стадия" }

private fun previewBreakthroughChance(level: Int, exp: Int, minLevel: Int, minExp: Int, order: Int): Double {
    var chance = 0.9 - order.coerceIn(0, 9) * 0.05
    chance += ((level - minLevel).coerceAtLeast(0) * 0.015).coerceAtMost(0.1)
    if (minExp > 0) chance += (((exp - minExp).coerceAtLeast(0).toDouble() / minExp) * 0.12).coerceAtMost(0.08)
    return (kotlin.math.round(chance.coerceIn(0.2, 0.92) * 100) / 100.0)
}

private fun Boolean?.orFalse() = this == true
private fun Int?.orZero() = this ?: 0
