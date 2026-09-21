package ru.tomilo.lib.mobile.ui.screens.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.tomilo.lib.mobile.data.api.GameAlchemyStatusDto
import ru.tomilo.lib.mobile.data.api.GameInventoryItemDto
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface

@Composable
fun InventoryScreen(items: List<GameInventoryItemDto>) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { Text("Материалы и расходники", color = TomiloMuted) }
        if (items.isEmpty()) item { EmptyGameUtility("Хранилище пусто", "Материалы можно получить в заданиях, колесе и за чтение.") }
        items(items, key = { it.itemId }) { item ->
            Surface(color = TomiloSurface, shape = RoundedCornerShape(17.dp), border = androidx.compose.foundation.BorderStroke(1.dp, TomiloBorder)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Inventory2, null, tint = TomiloPrimary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(item.name?.ifBlank { item.itemId } ?: item.itemId, fontWeight = FontWeight.SemiBold); Text("Материал для игровых улучшений", color = TomiloMuted, maxLines = 1) }
                    Text("×${item.count}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AlchemyScreen(status: GameAlchemyStatusDto) {
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Text("Котёл и рецепты", color = TomiloMuted)
        listOf(
            "Уровень котла" to "${status.cauldronTier}",
            "Уровень алхимика" to "${status.alchemyLevel}",
            "Попыток сегодня" to "${status.attemptsLeft}/${status.craftsPerDay}",
            "Опыт алхимии" to "${status.alchemyExp}/${status.alchemyExpToNext}",
        ).forEach { (label, value) ->
            Surface(color = TomiloSurface, shape = RoundedCornerShape(17.dp), border = androidx.compose.foundation.BorderStroke(1.dp, TomiloBorder)) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFCC78E8))
                    Spacer(Modifier.width(12.dp)); Text(label, modifier = Modifier.weight(1f)); Text(value, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (!status.canCraft) Text("Новые рецепты станут доступны с материалами и следующими уровнями.", color = TomiloMuted)
    }
}

@Composable private fun EmptyGameUtility(title: String, text: String) = Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(title, fontWeight = FontWeight.Bold); Text(text, color = TomiloMuted, modifier = Modifier.padding(top = 6.dp)) }
