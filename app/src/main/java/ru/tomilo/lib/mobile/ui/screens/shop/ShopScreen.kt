package ru.tomilo.lib.mobile.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.api.EquippedDecorationsDto
import ru.tomilo.lib.mobile.data.api.ShopDecorationDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.DecoratedAvatar
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.StatusPill
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

private data class ShopCategory(val type: String, val label: String)

private val ShopCategories = listOf(
    ShopCategory("avatar", "Аватары"),
    ShopCategory("frame", "Рамки"),
    ShopCategory("background", "Фоны"),
    ShopCategory("badge", "Значки"),
    ShopCategory("card", "Карточки"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    onBack: () -> Unit,
    onLogin: () -> Unit,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var categoryIndex by remember { mutableIntStateOf(0) }
    var catalog by remember { mutableStateOf<List<ShopDecorationDto>>(emptyList()) }
    var owned by remember { mutableStateOf<List<ShopDecorationDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var purchaseRequest by remember { mutableStateOf<ShopDecorationDto?>(null) }
    val category = ShopCategories[categoryIndex]

    LaunchedEffect(category.type, user?.stableId(), reload) {
        loading = true
        error = null
        socialRepository.shopDecorations(category.type)
            .onSuccess { catalog = it }
            .onFailure { error = it.message }
        owned = if (user != null) {
            socialRepository.ownedDecorations().getOrDefault(emptyList())
        } else {
            emptyList()
        }
        loading = false
    }

    val ownedIds = remember(owned) { owned.map { it.stableId() }.toSet() }
    val equippedIds = remember(owned) {
        owned.filter { it.isEquipped == true }.map { it.stableId() }.toSet()
    }

    fun reloadShop(message: String? = null) {
        reload += 1
        if (message != null) scope.launch { snackbar.showSnackbar(message) }
    }

    fun purchase(item: ShopDecorationDto) {
        if (user == null) {
            onLogin()
            return
        }
        val id = item.stableId()
        busyId = id
        scope.launch {
            socialRepository.purchaseDecoration(category.type, id)
                .onSuccess {
                    authRepository.refreshProfile()
                    reloadShop("«${item.name}» добавлено в инвентарь")
                }
                .onFailure { snackbar.showSnackbar(it.message ?: "Покупка не выполнена") }
            busyId = null
        }
    }

    fun toggleEquip(item: ShopDecorationDto, equipped: Boolean) {
        val id = item.stableId()
        busyId = id
        scope.launch {
            val result = if (equipped) {
                socialRepository.unequipDecoration(category.type)
            } else {
                socialRepository.equipDecoration(category.type, id)
            }
            result
                .onSuccess {
                    authRepository.refreshProfile()
                    reloadShop(if (equipped) "Украшение снято" else "Украшение надето")
                }
                .onFailure { snackbar.showSnackbar(it.message ?: "Не удалось изменить украшение") }
            busyId = null
        }
    }

    Scaffold(
        containerColor = TomiloBg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Магазин декораций")
                        Text("Единый инвентарь с сайтом", color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.ShoppingBag, null, tint = TomiloPremium)
                Spacer(Modifier.size(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Украсьте профиль", fontWeight = FontWeight.SemiBold)
                    Text("Покупайте за монеты и сразу надевайте", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                }
                Surface(color = TomiloPremium.copy(alpha = 0.14f), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MonetizationOn, null, tint = TomiloPremium, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("${user?.balance ?: 0}", color = TomiloPremium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp),
            ) {
                ShopCategories.forEachIndexed { index, item ->
                    FilterChip(
                        selected = categoryIndex == index,
                        onClick = { categoryIndex = index },
                        label = { Text(item.label) },
                        modifier = Modifier.padding(horizontal = 3.dp),
                    )
                }
            }
            when {
                loading -> LoadingBox(message = "Загружаем украшения…")
                error != null && catalog.isEmpty() -> ErrorBox(error ?: "Ошибка") { reload += 1 }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 10.dp, top = 8.dp, end = 10.dp, bottom = 110.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    items(catalog, key = { it.stableId() }) { item ->
                        val id = item.stableId()
                        ShopDecorationCard(
                            item = item,
                            type = category.type,
                            owned = id in ownedIds,
                            equipped = id in equippedIds,
                            busy = busyId == id,
                            previewAvatar = user?.avatar,
                            previewUsername = user?.username,
                            equippedDecorations = user?.decorations(),
                            onAction = {
                                when {
                                    user == null -> onLogin()
                                    id in ownedIds -> toggleEquip(item, id in equippedIds)
                                    item.price > 0 -> purchaseRequest = item
                                    else -> purchase(item)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    purchaseRequest?.let { item ->
        AlertDialog(
            onDismissRequest = { purchaseRequest = null },
            title = { Text("Купить «${item.name}»?") },
            text = {
                Text(
                    "Стоимость: ${item.price} монет. На балансе: ${user?.balance ?: 0}. " +
                        "Украшение останется в общем инвентаре сайта и приложения.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        purchaseRequest = null
                        purchase(item)
                    },
                    enabled = (user?.balance ?: 0) >= item.price,
                ) { Text("Купить") }
            },
            dismissButton = { TextButton(onClick = { purchaseRequest = null }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun ShopDecorationCard(
    item: ShopDecorationDto,
    type: String,
    owned: Boolean,
    equipped: Boolean,
    busy: Boolean,
    previewAvatar: String?,
    previewUsername: String?,
    equippedDecorations: EquippedDecorationsDto?,
    onAction: () -> Unit,
) {
    val rarityColor = when (item.rarity.lowercase()) {
        "legendary" -> Color(0xFFFFB547)
        "epic" -> Color(0xFFB783FF)
        "rare" -> Color(0xFF60A9FF)
        else -> TomiloPrimary
    }
    Surface(
        color = TomiloSurface2.copy(alpha = 0.76f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (equipped) MaterialTheme.colorScheme.primary else TomiloBorder.copy(alpha = 0.6f),
        ),
    ) {
        Column(Modifier.padding(9.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (type == "background") 1.35f else 1f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(rarityColor.copy(alpha = 0.10f))
                    .border(1.dp, rarityColor.copy(alpha = 0.22f), RoundedCornerShape(13.dp)),
            ) {
                when (type) {
                    "frame" -> DecoratedAvatar(
                        avatarUrl = previewAvatar,
                        username = previewUsername,
                        decorations = equippedDecorations,
                        frameUrl = item.imageUrl,
                        size = 112.dp,
                        modifier = Modifier.align(Alignment.Center),
                        ringColor = rarityColor,
                    )
                    "avatar" -> DecoratedAvatar(
                        avatarUrl = previewAvatar,
                        username = previewUsername,
                        decorations = equippedDecorations,
                        avatarDecorationUrl = item.imageUrl,
                        size = 112.dp,
                        modifier = Modifier.align(Alignment.Center),
                        ringColor = rarityColor,
                    )
                    "badge" -> DecoratedAvatar(
                        avatarUrl = previewAvatar,
                        username = previewUsername,
                        decorations = equippedDecorations,
                        badgeUrl = item.imageUrl,
                        size = 112.dp,
                        modifier = Modifier.align(Alignment.Center),
                        ringColor = rarityColor,
                    )
                    else -> AsyncImage(
                        model = MediaUrl.resolve(item.imageUrl),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Surface(
                    color = rarityColor.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                ) {
                    Text(
                        rarityLabel(item.rarity),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
                if (equipped) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Надето",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(item.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!item.description.isNullOrBlank()) {
                Text(
                    item.description,
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(34.dp),
                )
            } else {
                Spacer(Modifier.height(34.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!owned) {
                    item.originalPrice?.takeIf { it > item.price }?.let {
                        Text(
                            "$it",
                            color = TomiloMuted,
                            style = MaterialTheme.typography.labelSmall,
                            textDecoration = TextDecoration.LineThrough,
                        )
                        Spacer(Modifier.size(5.dp))
                    }
                    Text(
                        if (item.price == 0) "Бесплатно" else "${item.price} монет",
                        color = TomiloPremium,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    StatusPill(if (equipped) "Надето" else "В инвентаре")
                }
            }
            Spacer(Modifier.height(8.dp))
            if (equipped) {
                OutlinedButton(onClick = onAction, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (busy) "Подождите…" else "Снять")
                }
            } else {
                Button(
                    onClick = onAction,
                    enabled = !busy && !item.soldOut(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when {
                            busy -> "Подождите…"
                            item.soldOut() -> "Распродано"
                            owned -> "Надеть"
                            item.price == 0 -> "Получить"
                            else -> "Купить"
                        },
                    )
                }
            }
        }
    }
}

private fun rarityLabel(rarity: String): String = when (rarity.lowercase()) {
    "legendary" -> "Легендарное"
    "epic" -> "Эпическое"
    "rare" -> "Редкое"
    else -> "Обычное"
}
