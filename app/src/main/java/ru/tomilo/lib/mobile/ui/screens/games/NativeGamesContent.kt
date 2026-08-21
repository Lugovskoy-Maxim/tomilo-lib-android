package ru.tomilo.lib.mobile.ui.screens.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.api.GameBattleOpponentDto
import ru.tomilo.lib.mobile.data.api.GameBattleResultDto
import ru.tomilo.lib.mobile.data.api.GameDiscipleDto
import ru.tomilo.lib.mobile.data.api.GameDisciplesDto
import ru.tomilo.lib.mobile.data.repo.GamesRepository
import ru.tomilo.lib.mobile.ui.components.RewardNotifications
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

private val SectPurple = Color(0xFF9B8CFF)
private val ArenaRed = Color(0xFFE98273)
private val ArenaGreen = Color(0xFF65B985)

@Composable
internal fun SectContent(
    disciples: GameDisciplesDto,
    gamesRepository: GamesRepository,
    onOpenArena: () -> Unit,
    onChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var busyKey by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var noticeIsError by remember { mutableStateOf(false) }

    fun runAction(key: String, action: suspend () -> Result<String>) {
        if (busyKey != null) return
        scope.launch {
            busyKey = key
            action()
                .onSuccess {
                    notice = it
                    noticeIsError = false
                    onChanged()
                }
                .onFailure {
                    notice = it.message ?: "Действие не выполнено"
                    noticeIsError = true
                }
            busyKey = null
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectHero(disciples = disciples, onOpenArena = onOpenArena)
        }
        notice?.let { message ->
            item(key = "sect_notice_$message") {
                GameNotice(message = message, isError = noticeIsError)
            }
        }
        item {
            Column(Modifier.padding(top = 6.dp, bottom = 2.dp)) {
                Text("Ученики", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Выберите основного ученика, развивайте отряд и освобождайте место через казарму.",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (disciples.disciples.isEmpty()) {
            item {
                GameEmptyCard(
                    icon = Icons.Default.Groups,
                    title = "В секте пока нет учеников",
                    text = "Получите первого ученика на сайте — после этого управление будет доступно прямо здесь.",
                )
            }
        } else {
            items(
                items = disciples.disciples,
                key = { it.characterId.ifBlank { "${it.displayName()}_${it.hashCode()}" } },
            ) { disciple ->
                val isPrimary = disciple.characterId == disciples.primaryDiscipleCharacterId
                SectDiscipleCard(
                    disciple = disciple,
                    isPrimary = isPrimary,
                    trainCost = disciples.trainCostCoins ?: 0,
                    busy = busyKey != null,
                    activeAction = busyKey,
                    onSetPrimary = {
                        runAction("primary_${disciple.characterId}") {
                            gamesRepository.setPrimary(disciple.characterId)
                                .map { "${disciple.displayName()} теперь основной ученик" }
                        }
                    },
                    onTrain = {
                        runAction("train_${disciple.characterId}") {
                            gamesRepository.train(disciple.characterId).map { result ->
                                if (result.outcome == "fail") {
                                    "Тренировка завершена, но прорыва пока нет"
                                } else {
                                    "Тренировка успешна · ${disciple.displayName()} стал сильнее"
                                }
                            }
                        }
                    },
                    onToggleWarehouse = {
                        val movingToWarehouse = disciple.inWarehouse != true
                        runAction("warehouse_${disciple.characterId}") {
                            gamesRepository.setWarehouse(disciple.characterId, movingToWarehouse)
                                .map {
                                    if (movingToWarehouse) "Ученик отправлен в казарму"
                                    else "Ученик возвращён в строй"
                                }
                        }
                    },
                    canTrain = disciples.canTrain && disciple.inWarehouse != true && disciple.inMeditation != true,
                )
            }
        }
    }
}

@Composable
private fun SectHero(disciples: GameDisciplesDto, onOpenArena: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF34266B), Color(0xFF1B1731))))
            .padding(17.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(50.dp).clip(RoundedCornerShape(17.dp)).background(SectPurple.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Groups, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    disciples.sectLevelLabel ?: "Секта · уровень ${disciples.sectLevel}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text("Все ключевые действия — в приложении", color = Color.White.copy(alpha = 0.7f))
            }
        }
        Spacer(Modifier.height(15.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GameMetric("${disciples.disciples.count { it.inWarehouse != true }}", "в строю", Modifier.weight(1f))
            GameMetric("${disciples.disciples.count { it.inWarehouse == true }}", "в казарме", Modifier.weight(1f))
            GameMetric("${disciples.spiritStones}", "камни", Modifier.weight(1f))
        }
        Spacer(Modifier.height(13.dp))
        Button(onClick = onOpenArena, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.MilitaryTech, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text("Перейти на арену")
        }
    }
}

@Composable
private fun SectDiscipleCard(
    disciple: GameDiscipleDto,
    isPrimary: Boolean,
    trainCost: Int,
    busy: Boolean,
    activeAction: String?,
    canTrain: Boolean,
    onSetPrimary: () -> Unit,
    onTrain: () -> Unit,
    onToggleWarehouse: () -> Unit,
) {
    Surface(
        color = TomiloSurface,
        shape = RoundedCornerShape(21.dp),
        border = BorderStroke(1.dp, if (isPrimary) SectPurple.copy(alpha = 0.65f) else TomiloBorder),
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = MediaUrl.resolve(disciple.avatar),
                    contentDescription = disciple.displayName(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(SectPurple.copy(alpha = 0.13f)),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            disciple.displayName(),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (isPrimary) {
                            Spacer(Modifier.width(6.dp))
                            GameTag("ОСНОВНОЙ", SectPurple)
                        }
                    }
                    Text(
                        listOfNotNull(disciple.titleName, disciple.rank?.let { "Ранг $it" }, disciple.level?.let { "$it ур." }).joinToString(" · "),
                        color = TomiloMuted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Сила ${disciple.cp ?: (disciple.attack + disciple.defense + disciple.speed)} · " +
                            when {
                                disciple.inMeditation == true -> "медитация"
                                disciple.inWarehouse == true -> "в казарме"
                                else -> "в строю"
                            },
                        color = if (disciple.inMeditation == true) TomiloPremium else SectPurple,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            disciple.expToNext?.takeIf { it > 0 }?.let { expToNext ->
                Spacer(Modifier.height(11.dp))
                val progress = ((disciple.exp ?: 0).toFloat() / expToNext.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = SectPurple,
                    trackColor = TomiloSurface2,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(
                    onClick = onSetPrimary,
                    enabled = !busy && !isPrimary && disciple.inWarehouse != true,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 7.dp),
                ) {
                    ActionContent(activeAction == "primary_${disciple.characterId}", "Основной")
                }
                Button(
                    onClick = onTrain,
                    enabled = !busy && canTrain,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 7.dp),
                ) {
                    ActionContent(
                        activeAction == "train_${disciple.characterId}",
                        if (trainCost > 0) "Тренировать · $trainCost" else "Тренировать",
                    )
                }
            }
            Spacer(Modifier.height(7.dp))
            OutlinedButton(
                onClick = onToggleWarehouse,
                enabled = !busy && !isPrimary && disciple.inMeditation != true,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ActionContent(
                    activeAction == "warehouse_${disciple.characterId}",
                    if (disciple.inWarehouse == true) "Вернуть в строй" else "Отправить в казарму",
                )
            }
        }
    }
}

@Composable
internal fun ArenaContent(
    disciples: GameDisciplesDto,
    gamesRepository: GamesRepository,
    onOpenSect: () -> Unit,
    onChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val squadSize = (disciples.arenaBattleSquadSize ?: disciples.maxBattleSquadSize ?: 5).coerceAtLeast(1)
    val eligible = disciples.disciples.filter { it.inMeditation != true }
    val maxBattles = disciples.maxBattlesPerDay.takeIf { it > 0 } ?: 3
    val battlesLeft = (maxBattles - disciples.dailyBattlesCount).coerceAtLeast(0)
    val serverAllowsBattle = disciples.canBattle || disciples.maxBattlesPerDay <= 0
    val arenaAvailable = serverAllowsBattle && disciples.canEnterArena != false
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var opponent by remember { mutableStateOf<GameBattleOpponentDto?>(null) }
    var opponentIsBot by remember { mutableStateOf(false) }
    var battleResult by remember { mutableStateOf<GameBattleResultDto?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var noticeIsError by remember { mutableStateOf(false) }

    LaunchedEffect(
        disciples.battleSquadCharacterIds,
        disciples.battleSquadDiscipleKeys,
        eligible.map { it.characterId },
        squadSize,
    ) {
        val preferred = (disciples.battleSquadDiscipleKeys + disciples.battleSquadCharacterIds).distinct()
        val validPreferred = preferred.filter { id -> eligible.any { it.characterId == id } }.take(squadSize)
        selectedIds = (validPreferred + eligible.map { it.characterId }.filter { it !in validPreferred })
            .take(squadSize)
            .toSet()
    }

    fun findMatch() {
        if (busy != null || selectedIds.size != squadSize) return
        scope.launch {
            busy = "match"
            notice = null
            battleResult = null
            gamesRepository.saveBattleSquad(selectedIds.toList())
                .fold(
                    onSuccess = {
                        gamesRepository.findOpponent()
                            .onSuccess { match ->
                                val serverOpponent = match?.opponent
                                if (serverOpponent != null) {
                                    opponent = serverOpponent
                                    opponentIsBot = match.isBot || serverOpponent.userId.startsWith("bot:")
                                } else if (arenaAvailable && battlesLeft > 0) {
                                    val squad = eligible.filter { it.characterId in selectedIds }.take(squadSize)
                                    opponent = GameBattleOpponentDto(
                                        userId = "bot:casual",
                                        username = "Тень соперника",
                                        combatRating = disciples.combatRating,
                                        disciples = squad,
                                        battleSquad = squad,
                                    )
                                    opponentIsBot = true
                                    notice = "Живой соперник не найден — доступен серверный тренировочный бой."
                                    noticeIsError = false
                                } else {
                                    opponent = null
                                    notice = if (!arenaAvailable) {
                                        "Арена сейчас недоступна для этого состава. Проверьте готовность учеников."
                                    } else {
                                        "Подходящий соперник пока не найден. Попробуйте ещё раз."
                                    }
                                    noticeIsError = true
                                }
                                onChanged()
                            }
                            .onFailure {
                                notice = it.message ?: "Не удалось найти соперника"
                                noticeIsError = true
                            }
                    },
                    onFailure = {
                        notice = it.message ?: "Не удалось сохранить боевой отряд"
                        noticeIsError = true
                    },
                )
            busy = null
        }
    }

    fun startBattle() {
        val target = opponent ?: return
        if (busy != null) return
        scope.launch {
            busy = "battle"
            notice = null
            gamesRepository.battle(target.userId, selectedIds.toList())
                .onSuccess { result ->
                    battleResult = result
                    RewardNotifications.show(
                        experience = result.expGained ?: 0,
                        coins = result.coinsGained,
                        source = if (result.win) "Победа на арене" else "Награда за бой",
                    )
                    noticeIsError = false
                    onChanged()
                }
                .onFailure {
                    notice = it.message ?: "Бой не удалось завершить"
                    noticeIsError = true
                }
            busy = null
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ArenaHero(
                rating = disciples.combatRating,
                battlesLeft = battlesLeft,
                maxBattles = maxBattles,
                selected = selectedIds.size,
                required = squadSize,
            )
        }
        battleResult?.let { result ->
            item(key = "battle_result_${result.hashCode()}") {
                BattleResultCard(
                    result = result,
                    onAgain = {
                        battleResult = null
                        opponent = null
                        findMatch()
                    },
                )
            }
        }
        notice?.let { message ->
            item(key = "arena_notice_$message") { GameNotice(message, noticeIsError) }
        }
        opponent?.takeIf { battleResult == null }?.let { target ->
            item(key = "opponent_${target.userId}") {
                OpponentCard(
                    opponent = target,
                    isBot = opponentIsBot,
                    loading = busy == "battle",
                    canBattle = battlesLeft > 0,
                    onBattle = ::startBattle,
                    onSearchAgain = {
                        opponent = null
                        findMatch()
                    },
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Боевой отряд", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Выбрано ${selectedIds.size} из $squadSize · ученики в медитации недоступны",
                        color = TomiloMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                GameTag("${selectedIds.size}/$squadSize", if (selectedIds.size == squadSize) ArenaGreen else ArenaRed)
            }
        }
        if (eligible.size < squadSize) {
            item {
                GameNotice(
                    message = "Для арены нужно $squadSize готовых учеников. Сейчас доступно ${eligible.size}.",
                    isError = true,
                )
            }
            item {
                OutlinedButton(onClick = onOpenSect, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Groups, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Управлять сектой")
                }
            }
        }
        items(
            items = eligible,
            key = { "arena_${it.characterId.ifBlank { it.hashCode().toString() }}" },
        ) { disciple ->
            val selected = disciple.characterId in selectedIds
            ArenaDiscipleCard(
                disciple = disciple,
                selected = selected,
                enabled = selected || selectedIds.size < squadSize,
                onClick = {
                    opponent = null
                    battleResult = null
                    selectedIds = if (selected) selectedIds - disciple.characterId else selectedIds + disciple.characterId
                },
            )
        }
        if (eligible.isEmpty()) {
            item {
                GameEmptyCard(
                    icon = Icons.Default.MilitaryTech,
                    title = "Нет готовых бойцов",
                    text = "Проверьте медитацию учеников или соберите секту.",
                )
            }
        }
        item {
            Button(
                onClick = ::findMatch,
                enabled = busy == null && selectedIds.size == squadSize && battlesLeft > 0 && arenaAvailable,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                ActionContent(busy == "match", if (opponent == null) "Найти соперника" else "Сменить соперника")
            }
            if (battlesLeft <= 0) {
                Text(
                    "Дневной лимит арены исчерпан. Новые бои станут доступны после сброса лимита.",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ArenaHero(rating: Int, battlesLeft: Int, maxBattles: Int, selected: Int, required: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF552A32), Color(0xFF21171E))))
            .padding(17.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(50.dp).clip(RoundedCornerShape(17.dp)).background(ArenaRed.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.MilitaryTech, null, tint = Color.White, modifier = Modifier.size(29.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Арена наставника", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Честный бой через сервер tomilo", color = Color.White.copy(alpha = 0.68f))
            }
        }
        Spacer(Modifier.height(15.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GameMetric("$rating", "рейтинг", Modifier.weight(1f))
            GameMetric("$battlesLeft/$maxBattles", "боёв", Modifier.weight(1f))
            GameMetric("$selected/$required", "отряд", Modifier.weight(1f))
        }
    }
}

@Composable
private fun ArenaDiscipleCard(disciple: GameDiscipleDto, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        color = if (selected) SectPurple.copy(alpha = 0.10f) else TomiloSurface,
        shape = RoundedCornerShape(19.dp),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) SectPurple else TomiloBorder),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = MediaUrl.resolve(disciple.avatar),
                contentDescription = disciple.displayName(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(SectPurple.copy(alpha = 0.13f)),
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(disciple.displayName(), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(disciple.rank?.let { "Ранг $it" }, disciple.level?.let { "$it ур." }).joinToString(" · "),
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (disciple.inWarehouse == true) Text("Доступен из казармы", color = SectPurple, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${disciple.cp ?: (disciple.attack + disciple.defense + disciple.speed)}", color = if (selected) SectPurple else TomiloMuted, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.size(24.dp).clip(CircleShape)
                        .background(if (selected) SectPurple else TomiloSurface2)
                        .then(if (!selected) Modifier.background(TomiloSurface2) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) Icon(Icons.Default.TaskAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    else Text("+", color = if (enabled) TomiloMuted else TomiloBorder, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun OpponentCard(
    opponent: GameBattleOpponentDto,
    isBot: Boolean,
    loading: Boolean,
    canBattle: Boolean,
    onBattle: () -> Unit,
    onSearchAgain: () -> Unit,
) {
    Surface(color = TomiloSurface, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, ArenaRed.copy(alpha = 0.5f))) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = MediaUrl.resolve(opponent.avatar),
                    contentDescription = opponent.username,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(54.dp).clip(CircleShape).background(ArenaRed.copy(alpha = 0.12f)),
                )
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(opponent.username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (isBot) {
                            Spacer(Modifier.width(6.dp))
                            GameTag("БОТ", ArenaRed)
                        }
                    }
                    Text("Сила ${opponent.combatRating}", color = TomiloMuted)
                }
                Icon(Icons.Default.MilitaryTech, null, tint = ArenaRed)
            }
            val squad = opponent.battleSquad.ifEmpty { opponent.disciples }.take(5)
            if (squad.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    squad.forEach { member ->
                        AsyncImage(
                            model = MediaUrl.resolve(member.avatar),
                            contentDescription = member.displayName(),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(TomiloSurface2),
                        )
                    }
                }
            }
            Spacer(Modifier.height(13.dp))
            Button(onClick = onBattle, enabled = !loading && canBattle, modifier = Modifier.fillMaxWidth()) {
                ActionContent(loading, "Начать бой")
            }
            OutlinedButton(onClick = onSearchAgain, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Другой соперник")
            }
        }
    }
}

@Composable
private fun BattleResultCard(result: GameBattleResultDto, onAgain: () -> Unit) {
    val accent = if (result.win) ArenaGreen else ArenaRed
    Surface(color = accent.copy(alpha = 0.10f), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))) {
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(if (result.win) Icons.Default.AutoAwesome else Icons.Default.MilitaryTech, null, tint = accent, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(if (result.win) "Победа" else "Поражение", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = accent)
            Spacer(Modifier.height(6.dp))
            val ratingDelta = result.combatRatingDelta ?: result.ratingDelta ?: 0
            Text(
                buildList {
                    if (result.coinsGained != 0) add("${if (result.coinsGained > 0) "+" else ""}${result.coinsGained} монет")
                    result.expGained?.takeIf { it != 0 }?.let { add("+$it опыта") }
                    if (ratingDelta != 0) add("${if (ratingDelta > 0) "+" else ""}$ratingDelta рейтинга")
                }.joinToString(" · ").ifBlank { "Результат сохранён на сервере" },
                color = TomiloMuted,
            )
            Spacer(Modifier.height(13.dp))
            Button(onClick = onAgain, modifier = Modifier.fillMaxWidth()) { Text("Следующий бой") }
        }
    }
}

@Composable
private fun GameMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.07f)).padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = Color.White.copy(alpha = 0.58f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun GameTag(text: String, color: Color) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.13f)).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun GameNotice(message: String, isError: Boolean) {
    val accent = if (isError) ArenaRed else ArenaGreen
    Surface(color = accent.copy(alpha = 0.09f), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp), color = if (isError) accent else TomiloMuted)
    }
}

@Composable
private fun GameEmptyCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(TomiloSurface).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = TomiloMuted, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(9.dp))
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(text, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ActionContent(loading: Boolean, label: String) {
    if (loading) {
        CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(7.dp))
    }
    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
}
