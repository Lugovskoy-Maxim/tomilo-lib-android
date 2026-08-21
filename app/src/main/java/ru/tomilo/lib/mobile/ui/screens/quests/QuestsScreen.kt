package ru.tomilo.lib.mobile.ui.screens.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.data.api.DailyQuestDto
import ru.tomilo.lib.mobile.data.api.DailyQuestsDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.components.PageIntro
import ru.tomilo.lib.mobile.ui.components.RewardNotifications
import ru.tomilo.lib.mobile.ui.components.StatusPill
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestsScreen(authRepository: AuthRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var data by remember { mutableStateOf(DailyQuestsDto()) }
    var loading by remember { mutableStateOf(true) }
    var actionBusy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    fun message(text: String) { scope.launch { snackbar.showSnackbar(text) } }

    LaunchedEffect(reload) {
        loading = true
        error = null
        authRepository.dailyQuests()
            .onSuccess { data = it }
            .onFailure { error = it.message }
        loading = false
    }

    Scaffold(
        containerColor = TomiloBg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Задания и награды") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
                actions = { IconButton(enabled = !loading, onClick = { reload += 1 }) { Icon(Icons.Default.Refresh, "Обновить") } },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        when {
            loading -> LoadingBox(Modifier.padding(padding), "Загружаем задания…")
            error != null -> ErrorBox(error ?: "Ошибка", Modifier.padding(padding)) { reload += 1 }
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 10.dp, 16.dp, 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    val completed = data.quests.count { it.completed }
                    PageIntro(
                        title = "Прогресс дня",
                        subtitle = "Завершено $completed из ${data.quests.size} заданий",
                        icon = Icons.Outlined.TaskAlt,
                        trailing = { StatusPill("$completed/${data.quests.size}", TomiloPrimary) },
                    )
                }
                item {
                    DailyBonusCard(
                        busy = actionBusy,
                        onClaim = {
                            scope.launch {
                                actionBusy = true
                                authRepository.claimDailyBonus()
                                    .onSuccess { result ->
                                        RewardNotifications.show(
                                            experience = result.experienceGained,
                                            coins = result.coinsGained,
                                            source = "Ежедневный бонус",
                                        )
                                        authRepository.refreshProfile()
                                        message("+${result.experienceGained} опыта · +${result.coinsGained} монет")
                                        reload += 1
                                    }
                                    .onFailure { message(it.message ?: "Бонус уже получен") }
                                actionBusy = false
                            }
                        },
                    )
                }
                if (data.quests.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Заданий сегодня нет",
                            message = "Новый список появится после ежедневного обновления.",
                            icon = Icons.Outlined.TaskAlt,
                        )
                    }
                } else {
                    item {
                        val available = data.quests.count { it.completed && it.claimedAt == null }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Ежедневные задания", style = MaterialTheme.typography.titleLarge)
                                Text("Выполняйте их на сайте и в приложении", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            if (available > 1) Button(
                                enabled = !actionBusy,
                                onClick = {
                                    scope.launch {
                                        actionBusy = true
                                        authRepository.claimAllQuests()
                                            .onSuccess { result ->
                                                RewardNotifications.show(
                                                    experience = result.expGained,
                                                    coins = result.coinsGained,
                                                    source = "${result.claimedCount} заданий выполнено",
                                                )
                                                authRepository.refreshProfile()
                                                message("Получено: +${result.expGained} XP · +${result.coinsGained} монет")
                                                reload += 1
                                            }
                                            .onFailure { message(it.message ?: "Награды недоступны") }
                                        actionBusy = false
                                    }
                                },
                            ) { Text("Забрать все") }
                        }
                    }
                    items(data.quests, key = { it.id }) { quest ->
                        QuestCard(
                            quest = quest,
                            busy = actionBusy,
                            onClaim = {
                                scope.launch {
                                    actionBusy = true
                                    authRepository.claimQuest(quest.id)
                                        .onSuccess { result ->
                                            RewardNotifications.show(
                                                experience = result.expGained,
                                                coins = result.coinsGained,
                                                source = quest.name,
                                            )
                                            authRepository.refreshProfile()
                                            message("+${result.expGained} XP · +${result.coinsGained} монет")
                                            reload += 1
                                        }
                                        .onFailure { message(it.message ?: "Награда недоступна") }
                                    actionBusy = false
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyBonusCard(busy: Boolean, onClaim: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = TomiloPremium.copy(alpha = 0.12f)), shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(TomiloPremium.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.LocalFireDepartment, null, tint = TomiloPremium)
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Ежедневный бонус", style = MaterialTheme.typography.titleLarge)
                Text("Заходите каждый день и увеличивайте серию", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(enabled = !busy, onClick = onClaim) { Icon(Icons.Default.CardGiftcard, null); Spacer(Modifier.size(6.dp)); Text("Получить") }
        }
    }
}

@Composable
private fun QuestCard(quest: DailyQuestDto, busy: Boolean, onClaim: () -> Unit) {
    val progress = if (quest.target > 0) (quest.progress.toFloat() / quest.target).coerceIn(0f, 1f) else if (quest.completed) 1f else 0f
    Card(colors = CardDefaults.cardColors(containerColor = TomiloSurface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(quest.name, style = MaterialTheme.typography.titleMedium)
                    if (!quest.description.isNullOrBlank()) Text(quest.description, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                }
                if (quest.claimedAt != null) Icon(Icons.Default.Check, "Получено", tint = TomiloPrimary)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${quest.progress.coerceAtMost(quest.target)} / ${quest.target}", color = TomiloMuted, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                Text("+${quest.rewardExp} XP", color = TomiloPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                if (quest.rewardCoins > 0) { Spacer(Modifier.size(8.dp)); Text("+${quest.rewardCoins} мон.", color = TomiloPremium, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                if (quest.completed && quest.claimedAt == null) { Spacer(Modifier.size(10.dp)); Button(enabled = !busy, onClick = onClaim) { Text("Забрать") } }
            }
        }
    }
}
