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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.api.GameInventoryItemDto
import ru.tomilo.lib.mobile.data.api.GameInventoryUseDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.GamesRepository
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface

@Composable
internal fun InventoryNativeScreen(
    gamesRepository: GamesRepository,
    authRepository: AuthRepository,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var inventory by remember { mutableStateOf<List<GameInventoryItemDto>>(emptyList()) }
    var uses by remember { mutableStateOf<List<GameInventoryUseDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busyItem by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    suspend fun loadData() {
        loading = true
        val inventoryResult = gamesRepository.gameInventory()
        val usesResult = gamesRepository.gameInventoryUses()
        inventoryResult.onSuccess { inventory = it }.onFailure {
            message = it.message ?: "Не удалось загрузить инвентарь"
            isError = true
        }
        usesResult.onSuccess { uses = it }.onFailure {
            message = it.message ?: "Не удалось проверить действия с предметами"
            isError = true
            uses = emptyList()
        }
        loading = false
    }
    val refresh: () -> Unit = { scope.launch { loadData() } }
    LaunchedEffect(Unit) { loadData() }

    fun useItem(item: GameInventoryItemDto, count: Int) {
        if (busyItem != null || count < 1 || count > item.count) return
        scope.launch {
            busyItem = item.itemId
            gamesRepository.useGameInventoryItem(item.itemId, count)
                .onSuccess { result ->
                    authRepository.refreshProfile()
                    loadData()
                    message = result.message.ifBlank { "Предмет использован" }
                    isError = false
                }
                .onFailure {
                    message = it.message ?: "Не удалось использовать предмет"
                    isError = true
                }
            busyItem = null
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Surface(color = TomiloSurface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, TomiloPrimary.copy(alpha = 0.25f))) {
                Column(Modifier.fillMaxWidth().padding(15.dp)) {
                    Text("Хранилище", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text("Предметы и доступные действия. Использование подтверждает сервер.", color = TomiloMuted)
                    OutlinedButton(onClick = refresh, enabled = !loading, modifier = Modifier.padding(top = 8.dp)) {
                        Text(if (loading) "Обновляем…" else "Обновить")
                    }
                }
            }
        }
        message?.let { text ->
            item(key = "inventory_notice_$text") {
                Surface(color = (if (isError) Color(0xFFDA716A) else Color(0xFF63A982)).copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)) {
                    Text(text, Modifier.fillMaxWidth().padding(12.dp), color = if (isError) Color(0xFFDA716A) else TomiloMuted)
                }
            }
        }
        if (loading && inventory.isEmpty()) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
        } else if (!loading && inventory.isEmpty()) {
            item {
                Surface(color = TomiloSurface, shape = RoundedCornerShape(16.dp)) {
                    Text("Инвентарь пуст", Modifier.padding(18.dp), color = TomiloMuted)
                }
            }
        } else {
            items(inventory, key = { it.itemId }) { item ->
                val useInfo = uses.firstOrNull { it.itemId == item.itemId }
                InventoryNativeItem(
                    item = item,
                    useInfo = useInfo,
                    busy = busyItem == item.itemId,
                    anyBusy = busyItem != null,
                    onUse = { count -> useItem(item, count) },
                )
            }
        }
    }
}

@Composable
private fun InventoryNativeItem(
    item: GameInventoryItemDto,
    useInfo: GameInventoryUseDto?,
    busy: Boolean,
    anyBusy: Boolean,
    onUse: (Int) -> Unit,
) {
    var useCount by remember(item.itemId, item.count) { mutableIntStateOf(1) }
    Surface(color = TomiloSurface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, TomiloBorder)) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!item.icon.isNullOrBlank()) {
                    AsyncImage(
                        model = MediaUrl.resolve(item.icon),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = TomiloPrimary, modifier = Modifier.size(42.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.name?.takeIf(String::isNotBlank) ?: item.itemId, fontWeight = FontWeight.SemiBold)
                    Text("ID: ${item.itemId}", color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
                }
                Text("×${item.count}", fontWeight = FontWeight.Bold)
            }
            if (useInfo != null) {
                Text("Доступно: ${useInfo.contexts.map(::useContextLabel).distinct().joinToString(" · ")}", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
            }
            when {
                useInfo == null -> Text("Действия с предметом не подтверждены сервером", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                useInfo.canUseFromInventory -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (item.count > 1) {
                            OutlinedButton(onClick = { useCount = (useCount - 1).coerceAtLeast(1) }, enabled = !anyBusy && useCount > 1, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("−") }
                            Text("$useCount", fontWeight = FontWeight.Bold)
                            OutlinedButton(onClick = { useCount = (useCount + 1).coerceAtMost(item.count) }, enabled = !anyBusy && useCount < item.count, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("+") }
                        }
                        Button(onClick = { onUse(useCount) }, enabled = !anyBusy && item.count > 0) {
                            if (busy) CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp) else Text("Использовать ×$useCount")
                        }
                        if (item.count > 1) {
                            OutlinedButton(onClick = { onUse(item.count) }, enabled = !anyBusy) { Text("Все ×${item.count}") }
                        }
                    }
                }
                useInfo.canUseInBattle -> Text("Предмет применяется перед боем на арене", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                else -> Text("Используется в игровых режимах; напрямую из сумки не применяется", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun useContextLabel(context: String): String = when (context) {
    "battle" -> "Арена"
    "alchemy_ingredient" -> "Алхимия"
    "alchemy_craft" -> "Результат варки"
    "exchange" -> "Обмен"
    "cultivation" -> "Культивация"
    "wheel" -> "Колесо"
    "reading" -> "Чтение"
    "quest" -> "Поручения"
    "expedition" -> "Вылазка"
    "realm_map" -> "Тайный мир"
    "inventory_use" -> "Можно использовать"
    else -> context
}
