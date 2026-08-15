package ru.tomilo.lib.mobile.ui.screens.premium

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class Plan(
    val months: Int,
    val label: String,
    val price: Int,
    val pricePerMonth: Int,
    val badge: String? = null,
)

private val plans = listOf(
    Plan(months = 1, label = "1 месяц", price = 150, pricePerMonth = 150),
    Plan(months = 3, label = "3 месяца", price = 400, pricePerMonth = 133, badge = "Популярный"),
    Plan(months = 6, label = "6 месяцев", price = 700, pricePerMonth = 117, badge = "Выгодно"),
)

private const val BOOSTY_URL = "https://boosty.to/tomilolib/donate"
private const val TBANK_CARD = "4377727806508201"
private const val SITE_PREMIUM = "https://tomilo-lib.ru/premium"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    authRepository: AuthRepository,
    onBack: () -> Unit,
    onLogin: () -> Unit,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var selectedMonths by rememberSaveable { mutableStateOf(3) }
    var alternativesExpanded by rememberSaveable { mutableStateOf(false) }
    val selectedPlan = plans.first { it.months == selectedMonths }
    val isPremium = Premium.isActive(user?.subscriptionExpiresAt)

    fun notify(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    fun copy(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        notify("$label скопирован")
    }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        runCatching { context.startActivity(intent) }
            .onFailure { notify("Не удалось открыть страницу") }
    }

    Scaffold(
        containerColor = TomiloBg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Premium") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            PremiumHero(
                isPremium = isPremium,
                expiresAt = user?.subscriptionExpiresAt,
            )

            SectionHeading(
                title = "Всё для комфортного чтения",
                subtitle = "Меньше ограничений — больше любимых историй.",
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BenefitCard(
                    icon = Icons.Default.LockOpen,
                    title = "Премиум-главы без ожидания",
                    text = "Читайте закрытые главы сразу после публикации.",
                )
                BenefitCard(
                    icon = Icons.Default.Download,
                    title = "Полный офлайн-доступ",
                    text = "Загружайте главы и читайте без интернета и лимитов.",
                )
                BenefitCard(
                    icon = Icons.Default.VisibilityOff,
                    title = "Чтение без рекламы",
                    text = "Никаких рекламных блоков и пауз между главами.",
                )
            }

            SectionHeading(
                title = if (isPremium) "Продлить Premium" else "Выберите тариф",
                subtitle = "Чем дольше период, тем ниже стоимость месяца.",
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                plans.forEach { plan ->
                    PlanCard(
                        plan = plan,
                        selected = selectedMonths == plan.months,
                        onClick = { selectedMonths = plan.months },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            if (user == null) {
                Button(
                    onClick = onLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    Text("Войти и оформить Premium")
                }
                Text(
                    text = "Подписка привязывается к вашему аккаунту.",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                )
            } else {
                Button(
                    onClick = { openUrl(SITE_PREMIUM) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TomiloPremium,
                        contentColor = Color(0xFF2A2108),
                    ),
                ) {
                    Text(
                        text = "Оформить за ${selectedPlan.price} ₽",
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.size(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = "Оплата откроется на защищённой странице tomilo-lib.ru",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                )
            }

            Spacer(Modifier.height(20.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = TomiloSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { alternativesExpanded = !alternativesExpanded }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Другие способы оплаты",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "Boosty или перевод на карту",
                                color = TomiloMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = if (alternativesExpanded) "Свернуть" else "Развернуть",
                            tint = TomiloMuted,
                            modifier = Modifier.rotate(if (alternativesExpanded) 180f else 0f),
                        )
                    }
                    AnimatedVisibility(visible = alternativesExpanded) {
                        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                            HorizontalDivider(color = TomiloBorder)
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "При ручной оплате добавьте данные аккаунта в комментарий. " +
                                    "Активация занимает от 1 до 24 часов.",
                                color = TomiloMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(Modifier.height(12.dp))
                            if (user == null) {
                                OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
                                    Text("Войти для получения данных")
                                }
                            } else {
                                val accountBlock = buildString {
                                    appendLine("ID аккаунта: ${user!!.stableId()}")
                                    appendLine("Никнейм: ${user!!.username.orEmpty()}")
                                    append("Email: ${user!!.email.orEmpty()}")
                                }
                                AccountDataBlock(
                                    body = accountBlock,
                                    onCopy = { copy(accountBlock, "Данные аккаунта") },
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { openUrl(BOOSTY_URL) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("Открыть Boosty")
                            }
                            TextButton(
                                onClick = { copy(TBANK_CARD, "Номер карты") },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("Скопировать карту • ${formatCard(TBANK_CARD)}")
                            }
                        }
                    }
                }
            }

            Text(
                text = "Premium не продлевается автоматически. Вы сами решаете, когда оформить следующий период.",
                color = TomiloMuted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 24.dp),
            )
        }
    }
}

@Composable
private fun PremiumHero(isPremium: Boolean, expiresAt: String?) {
    val container = if (isPremium) TomiloPremium.copy(alpha = 0.14f) else TomiloSurface
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(TomiloPremium.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPremium) Icons.Default.CheckCircle else Icons.Default.Star,
                    contentDescription = null,
                    tint = TomiloPremium,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (isPremium) "Premium активен" else "Читайте без ограничений",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isPremium) {
                    formatExpiry(expiresAt)?.let { "Все возможности доступны до $it." }
                        ?: "Все возможности Premium доступны."
                } else {
                    "Премиум-главы, офлайн-библиотека и никакой рекламы."
                },
                color = TomiloMuted,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(Modifier.padding(top = 28.dp, bottom = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun BenefitCard(icon: ImageVector, title: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TomiloSurface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(TomiloPremium.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = TomiloPremium, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(text, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PlanCard(plan: Plan, selected: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        targetValue = if (selected) TomiloPremium.copy(alpha = 0.12f) else TomiloSurface,
        label = "planContainer",
    )
    val border by animateColorAsState(
        targetValue = if (selected) TomiloPremium else TomiloBorder,
        label = "planBorder",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(container)
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(2.dp, border, CircleShape)
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.fillMaxSize().background(TomiloPremium, CircleShape))
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(plan.label, style = MaterialTheme.typography.titleMedium)
                plan.badge?.let {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = it.uppercase(),
                        color = TomiloPremium,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TomiloPremium.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
            }
            Text(
                "${plan.pricePerMonth} ₽ в месяц",
                color = TomiloMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            "${plan.price} ₽",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AccountDataBlock(body: String, onCopy: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TomiloSurface2)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Данные аккаунта", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(5.dp))
                Text(body, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Скопировать")
            }
        }
    }
}

private fun formatExpiry(value: String?): String? = runCatching {
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
    Instant.parse(value).atZone(ZoneId.systemDefault()).format(formatter)
}.getOrNull()

private fun formatCard(pan: String): String = pan.chunked(4).joinToString(" ")
