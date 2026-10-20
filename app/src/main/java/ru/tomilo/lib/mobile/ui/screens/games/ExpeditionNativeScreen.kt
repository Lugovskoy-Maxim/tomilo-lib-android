package ru.tomilo.lib.mobile.ui.screens.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.api.ExpeditionStatusDto
import ru.tomilo.lib.mobile.data.api.GameInventoryItemDto
import ru.tomilo.lib.mobile.data.api.RealmMapCategoryDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.GamesRepository
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface

private data class ExpeditionDifficulty(val id: String, val label: String, val detail: String)

private val expeditionDifficulties = listOf(
    ExpeditionDifficulty("easy", "Лёгкая", "Короткий поход, низкий риск, скромная добыча"),
    ExpeditionDifficulty("normal", "Обычная", "Средняя стоимость и награды"),
    ExpeditionDifficulty("hard", "Тяжёлая", "Дороже и рискованнее, возможен редкий лут"),
)

@Composable
internal fun ExpeditionNativeScreen(
    gamesRepository: GamesRepository,
    authRepository: AuthRepository,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<ExpeditionStatusDto?>(null) }
    var categories by remember { mutableStateOf<List<RealmMapCategoryDto>>(emptyList()) }
    var inventory by remember { mutableStateOf<List<GameInventoryItemDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busyAction by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var syncedResultAt by remember { mutableStateOf<String?>(null) }

    suspend fun loadStatus() {
        gamesRepository.expeditionStatus().onSuccess { next ->
            status = next
            isError = false
            message = null
            val resultAt = next.lastResult?.at?.takeIf(String::isNotBlank)
            if (resultAt != null && resultAt != syncedResultAt) {
                syncedResultAt = resultAt
                authRepository.refreshProfile()
            }
        }.onFailure {
            message = it.message ?: "Не удалось обновить статус вылазки"
            isError = true
        }
    }

    suspend fun loadAll() {
        loading = true
        loadStatus()
        gamesRepository.gameInventory().onSuccess { inventory = it }.onFailure {
            message = it.message ?: "Не удалось загрузить ключи и талисманы"
            isError = true
        }
        gamesRepository.realmMapsCatalog().onSuccess { categories = it.categories }.onFailure {
            message = it.message ?: "Не удалось загрузить карты тайных миров"
            isError = true
        }
        loading = false
    }

    fun refresh() { scope.launch { loadAll() } }
    LaunchedEffect(Unit) { loadAll() }
    LaunchedEffect(status?.inProgress, status?.completesAt, status?.nextExpeditionAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    LaunchedEffect(status?.inProgress) {
        while (status?.inProgress == true) {
            delay(5_000)
            loadStatus()
        }
    }

    fun runAction(key: String, action: suspend () -> Result<String>) {
        if (busyAction != null) return
        scope.launch {
            busyAction = key
            action().onSuccess { result ->
                loadAll()
                if (!isError) message = result
            }.onFailure {
                message = it.message ?: "Действие не выполнено"
                isError = true
            }
            busyAction = null
        }
    }

    val data = status
    val targetMillis = parseInstant(data?.completesAt ?: data?.nextExpeditionAt)
    val talismanCount = inventory.firstOrNull { it.itemId == "expedition_talisman" }?.count ?: 0
    val activeRealmLabel = categories.firstOrNull { it.id == data?.pendingRealmCategoryId }?.label

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(color = TomiloSurface, shape = RoundedCornerShape(19.dp), border = BorderStroke(1.dp, TomiloPrimary.copy(alpha = 0.25f))) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(Icons.Default.Explore, null, tint = TomiloPrimary)
                        Text(" Вылазка секты", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Text("Отправьте учеников за монетами, опытом профиля и предметами. Результат рассчитывает сервер.", color = TomiloMuted)
                    OutlinedButton(onClick = { refresh() }, enabled = !loading, modifier = Modifier.padding(top = 4.dp)) { Text(if (loading) "Обновляем…" else "Обновить статус") }
                }
            }
        }
        message?.let { text ->
            item(key = "expedition_notice_$text") {
                Surface(color = (if (isError) Color(0xFFDA716A) else Color(0xFF63A982)).copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)) {
                    Text(text, Modifier.fillMaxWidth().padding(12.dp), color = if (isError) Color(0xFFDA716A) else TomiloMuted)
                }
            }
        }
        if (loading && data == null) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
        } else if (data != null) {
            item {
                Surface(color = TomiloSurface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, TomiloBorder)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Статус: ${expeditionStatusLabel(data)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Баланс: ${data.balance} монет · риск засады: ${data.ambushRiskPercent ?: 0}% · талисманы: $talismanCount", color = TomiloMuted)
                        when {
                            data.inProgress -> {
                                val completesAt = parseInstant(data.completesAt)
                                val remaining = ((completesAt ?: now) - now).coerceAtLeast(0L)
                                Text("Завершение через ${formatRemaining(remaining)}", color = TomiloMuted)
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                            !data.canStart && targetMillis != null -> Text("Кулдаун: ${formatRemaining((targetMillis - now).coerceAtLeast(0L))}", color = TomiloMuted)
                            data.actionBattleInProgress -> Text("Сначала завершите активный бой.", color = TomiloMuted)
                            !data.hasDisciples -> Text("Для вылазки нужен хотя бы один активный ученик вне медитации.", color = TomiloMuted)
                            else -> Text("Ученики готовы. Выберите сложность.", color = TomiloMuted)
                        }
                        if (data.arenaStatBonusBattlesRemaining > 0) Text("Бонус арены: усиление на ${data.arenaStatBonusBattlesRemaining} бой", color = TomiloMuted)
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Карты тайных миров", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(activeRealmLabel?.let { "Активна карта: $it" } ?: "Ключ передаст бонус следующей вылазке.", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                    if (categories.isEmpty()) {
                        Text("Каталог карт пока недоступен.", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    categories.forEach { category ->
                        val count = inventory.firstOrNull { it.itemId == category.entryItemId }?.count ?: 0
                        if (count > 0) {
                            Surface(color = TomiloSurface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, TomiloBorder)) {
                                Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    androidx.compose.material3.Icon(Icons.Default.Inventory2, null, tint = TomiloPrimary)
                                    Column(Modifier.weight(1f)) {
                                        Text(category.label, fontWeight = FontWeight.SemiBold)
                                        Text("×$count · ${category.description}", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                                    }
                                    OutlinedButton(
                                        onClick = { runAction("realm_${category.entryItemId}") {
                                            gamesRepository.enterRealm(category.entryItemId).map { it.message.ifBlank { "Карта активирована" } }
                                        } },
                                        enabled = busyAction == null && data.pendingRealmCategoryId.isNullOrBlank(),
                                    ) { Text(if (busyAction == "realm_${category.entryItemId}") "…" else "Активировать") }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text("Сложность", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(expeditionDifficulties, key = { "difficulty_${it.id}" }) { difficulty ->
                val cost = when (difficulty.id) {
                    "easy" -> data.costs.easy
                    "normal" -> data.costs.normal
                    else -> data.costs.hard
                }
                Surface(color = TomiloSurface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, TomiloBorder)) {
                    Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(difficulty.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(difficulty.detail, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = { runAction("start_${difficulty.id}") {
                                gamesRepository.startExpedition(difficulty.id).map { "Вылазка отправлена. Результат появится после завершения похода." }
                            } },
                            enabled = data.canStart && data.hasDisciples && !data.actionBattleInProgress && !data.inProgress && data.balance >= cost && busyAction == null,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (busyAction == "start_${difficulty.id}") "Отправляем…" else "Отправить · $cost монет") }
                    }
                }
            }
            data.lastResult?.let { result ->
                item(key = "expedition_result_${result.at}") {
                    Surface(color = TomiloSurface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, TomiloPrimary.copy(alpha = 0.25f))) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(if (result.success) "Вылазка завершена" else "Вылазка не удалась", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("+${result.coinsGained} монет · +${result.expGained} опыта профиля", color = TomiloMuted)
                            result.ambush?.takeIf { it.happened }?.let { ambush ->
                                Text(if (ambush.preventedByTalisman) "Засада отражена талисманом" else "Засада: часть добычи потеряна", color = TomiloMuted)
                            }
                            result.itemsGained.forEach { item ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    if (!item.icon.isNullOrBlank()) AsyncImage(model = MediaUrl.resolve(item.icon), contentDescription = null, modifier = Modifier.size(28.dp))
                                    Text("${item.name?.takeIf(String::isNotBlank) ?: item.itemId} ×${item.count}")
                                }
                            }
                            result.log.take(6).forEach { Text("• $it", color = TomiloMuted, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }
    }
}

private fun expeditionStatusLabel(status: ExpeditionStatusDto): String = when {
    status.inProgress -> "В пути"
    status.actionBattleInProgress -> "Идёт бой"
    status.canStart -> "Готова к отправке"
    else -> "Кулдаун"
}

private fun parseInstant(value: String?): Long? = runCatching {
    java.time.Instant.parse(value).toEpochMilli()
}.getOrNull()

private fun formatRemaining(ms: Long): String {
    val totalSeconds = (ms / 1_000).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600
    val minutes = totalSeconds % 3_600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "${hours}ч ${minutes}м ${seconds}с" else "${minutes}м ${seconds}с"
}
