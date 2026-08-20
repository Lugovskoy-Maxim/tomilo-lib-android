package ru.tomilo.lib.mobile.ui.screens.premium

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.data.api.NetworkModule
import ru.tomilo.lib.mobile.data.api.PremiumPaymentHistoryItemDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.PaymentsRepository
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloSuccess
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class Plan(
    val id: String,
    val months: Int,
    val label: String,
    val price: Int,
    val pricePerMonth: Int,
    val badge: String? = null,
)

private val plans = listOf(
    Plan(id = "premium_1m", months = 1, label = "1 месяц", price = 150, pricePerMonth = 150),
    Plan(id = "premium_3m", months = 3, label = "3 месяца", price = 400, pricePerMonth = 133, badge = "Популярный"),
    Plan(id = "premium_6m", months = 6, label = "6 месяцев", price = 700, pricePerMonth = 117, badge = "Выгодно"),
    Plan(id = "premium_1y", months = 12, label = "1 год", price = 1200, pricePerMonth = 100, badge = "Год"),
)

private const val PREMIUM_COIN_PRICE = 30_000
private const val SELLER_INN = "553101511919"

private const val BOOSTY_URL = "https://boosty.to/tomilolib/donate"
private const val TBANK_CARD = "4377727806508201"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    authRepository: AuthRepository,
    paymentsRepository: PaymentsRepository,
    onBack: () -> Unit,
    onLogin: () -> Unit,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var selectedMonths by rememberSaveable { mutableStateOf(3) }
    val selectedPlan = plans.first { it.months == selectedMonths }
    val isPremium = Premium.isActive(user?.subscriptionExpiresAt)
    var paying by remember { mutableStateOf(false) }
    var pendingInvId by rememberSaveable { mutableStateOf<String?>(null) }
    var waitingPayment by rememberSaveable { mutableStateOf(false) }
    var confirmCoins by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf<List<PremiumPaymentHistoryItemDto>>(emptyList()) }

    fun notify(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    fun reloadHistory() {
        if (user == null) return
        scope.launch {
            history = paymentsRepository.history().getOrDefault(emptyList())
        }
    }

    fun watchInvoice(invId: String) {
        pendingInvId = invId
        waitingPayment = true
        scope.launch {
            repeat(15) {
                val status = paymentsRepository.paymentStatus(invId).getOrNull()
                if (status?.status == "paid") {
                    authRepository.refreshProfile()
                    reloadHistory()
                    waitingPayment = false
                    pendingInvId = null
                    notify("Premium активирован")
                    return@launch
                }
                delay(2_000)
            }
            waitingPayment = false
            notify("Платёж ещё обрабатывается. Статус обновится после подтверждения Robokassa.")
            reloadHistory()
        }
    }

    val checkoutLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val invId = result.data?.getStringExtra(RobokassaCheckoutActivity.EXTRA_INV_ID).orEmpty()
        when (result.data?.getStringExtra(RobokassaCheckoutActivity.EXTRA_STATUS)) {
            RobokassaCheckoutActivity.RESULT_SUCCESS -> {
                if (invId.isNotBlank()) watchInvoice(invId)
                else notify("Оплата принята, проверяем начисление…")
            }
            RobokassaCheckoutActivity.RESULT_FAILED -> notify("Оплата не прошла. Можно попробовать ещё раз.")
            else -> if (invId.isNotBlank()) watchInvoice(invId)
        }
    }

    fun openRobokassa(form: ru.tomilo.lib.mobile.data.api.RobokassaPaymentFormDto) {
        val fieldsJson = NetworkModule.json.encodeToString(form.fields)
        checkoutLauncher.launch(
            RobokassaCheckoutActivity.intent(
                context = context,
                paymentUrl = form.paymentUrl,
                invId = form.invId,
                fieldsJson = fieldsJson,
            ),
        )
    }

    fun startRobokassa(adminTest: Boolean = false) {
        if (user == null) {
            onLogin()
            return
        }
        if (paying) return
        paying = true
        scope.launch {
            val result = if (adminTest) {
                paymentsRepository.createAdminTestPayment()
            } else {
                paymentsRepository.createRobokassaPayment(selectedPlan.id)
            }
            paying = false
            result.fold(
                onSuccess = { openRobokassa(it) },
                onFailure = { notify(PaymentsRepository.userMessage(it)) },
            )
        }
    }

    LaunchedEffect(user?.stableId()) {
        reloadHistory()
        val invId = pendingInvId
        if (!invId.isNullOrBlank() && !waitingPayment) watchInvoice(invId)
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

            Spacer(Modifier.height(22.dp))
            PaymentMethodCard(
                index = "1",
                title = "Оплата через Robokassa",
                subtitle = "Карта, СБП и другие способы. Подписка начисляется автоматически.",
            ) {
                Text(
                    "Вы перейдёте на защищённую страницу Robokassa. Данные карты Tomilo не получает и не хранит.",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                if (user == null) {
                    Button(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        Text("Войти и перейти к оплате")
                    }
                } else {
                    Button(
                        onClick = { startRobokassa() },
                        enabled = !paying,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TomiloPremium,
                            contentColor = Color(0xFF2A2108),
                        ),
                    ) {
                        if (paying || waitingPayment) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF2A2108),
                            )
                            Spacer(Modifier.size(8.dp))
                        } else {
                            Icon(Icons.Default.Shield, null, Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                        }
                        Text(
                            when {
                                paying -> "Создаём счёт…"
                                waitingPayment -> "Проверяем оплату…"
                                else -> "Оплатить ${selectedPlan.price} ₽"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (user?.isAdmin() == true) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { startRobokassa(adminTest = true) },
                        enabled = !paying,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Админ: тестовый счёт 1 ₽")
                    }
                }
                Text(
                    "Разовая оплата, без автопродления. Срок добавится к текущей подписке.",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            PaymentMethodCard(
                index = "2",
                title = "Премиум за монеты",
                subtitle = "30 000 монет = 30 дней. Начисляется сразу.",
            ) {
                val balance = user?.balance ?: 0
                Text(
                    if (user == null) {
                        "Войдите, чтобы обменять монеты сайта на месяц Premium."
                    } else {
                        "Баланс: ${"%,d".format(Locale("ru"), balance)} монет. Нужно $PREMIUM_COIN_PRICE."
                    },
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                if (user == null) {
                    OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
                        Text("Войти")
                    }
                } else {
                    Button(
                        onClick = { confirmCoins = true },
                        enabled = !paying && balance >= PREMIUM_COIN_PRICE,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Text(
                            if (balance >= PREMIUM_COIN_PRICE) {
                                "Обменять $PREMIUM_COIN_PRICE монет"
                            } else {
                                "Недостаточно монет"
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            PaymentMethodCard(
                index = "3",
                title = "Перевод на карту Т‑Банк",
                subtitle = "В сообщении получателю — только ник и уровень. ID и почта не нужны.",
            ) {
                Text(
                    "Переведите ${selectedPlan.price} ₽ и вставьте короткий текст в сообщение получателю. " +
                        "Так платёж сопоставят с аккаунтом.",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                TBankCardVisual(pan = TBANK_CARD, onCopy = { copy(TBANK_CARD, "Номер карты") })
                Spacer(Modifier.height(12.dp))
                if (user == null) {
                    OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
                        Text("Войти, чтобы скопировать ник и уровень")
                    }
                } else {
                    val tbankBlock = "Никнейм: ${user!!.username.orEmpty()}\nУровень: ${user!!.level ?: 0}"
                    AccountDataBlock(
                        title = "Для сообщения получателю",
                        hint = "Два поля — этого достаточно для перевода на карту.",
                        rows = listOf(
                            "Ник" to user!!.username.orEmpty(),
                            "Уровень" to "${user!!.level ?: 0}",
                        ),
                        copyAll = tbankBlock,
                        onCopy = { text, label -> copy(text, label) },
                    )
                }
                Text(
                    "Перевод без ника и уровня считается добровольной поддержкой и не даёт Premium.",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            PaymentMethodCard(
                index = "4",
                title = "Boosty",
                subtitle = "В комментарии к донату нужны ID, ник и почта.",
            ) {
                Text(
                    "Укажите сумму ${selectedPlan.price} ₽ и вставьте полный блок данных в комментарий к цели или в сообщение на Boosty.",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                if (user == null) {
                    OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
                        Text("Войти для данных Boosty")
                    }
                } else {
                    val fullBlock = buildString {
                        appendLine("ID аккаунта: ${user!!.stableId()}")
                        appendLine("Никнейм: ${user!!.username.orEmpty()}")
                        append("Email: ${user!!.email.orEmpty()}")
                    }
                    AccountDataBlock(
                        title = "Для комментария Boosty",
                        hint = "Три поля: без них донат не привяжут к аккаунту.",
                        rows = listOf(
                            "ID" to user!!.stableId(),
                            "Ник" to user!!.username.orEmpty(),
                            "Почта" to user!!.email.orEmpty(),
                        ),
                        copyAll = fullBlock,
                        onCopy = { text, label -> copy(text, label) },
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { openUrl(BOOSTY_URL) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Открыть Boosty")
                }
            }

            if (history.isNotEmpty()) {
                SectionHeading(
                    title = "История оплат",
                    subtitle = "Счета Robokassa и покупки за монеты.",
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    history.take(12).forEach { item ->
                        PaymentHistoryRow(item)
                    }
                }
            }

            Text(
                text = "Premium не продлевается автоматически. Вы сами решаете, когда оформить следующий период.\nОплата: самозанятый Луговской М. Ю., ИНН $SELLER_INN. Чек приходит на email аккаунта.",
                color = TomiloMuted,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 24.dp),
            )
        }
    }

    if (confirmCoins) {
        AlertDialog(
            onDismissRequest = { if (!paying) confirmCoins = false },
            title = { Text("Обменять монеты?") },
            text = {
                Text("Спишется $PREMIUM_COIN_PRICE монет, к подписке добавятся 30 дней.")
            },
            confirmButton = {
                TextButton(
                    enabled = !paying,
                    onClick = {
                        paying = true
                        scope.launch {
                            val result = paymentsRepository.buyPremiumWithCoins()
                            paying = false
                            confirmCoins = false
                            result.fold(
                                onSuccess = {
                                    authRepository.refreshProfile()
                                    reloadHistory()
                                    notify("Premium на 30 дней активирован")
                                },
                                onFailure = { notify(PaymentsRepository.userMessage(it)) },
                            )
                        }
                    },
                ) { Text("Подтвердить") }
            },
            dismissButton = {
                TextButton(enabled = !paying, onClick = { confirmCoins = false }) {
                    Text("Отмена")
                }
            },
        )
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
private fun PaymentMethodCard(
    index: String,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(TomiloSurface)
            .border(1.dp, TomiloBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TomiloPremium.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(index, color = TomiloPremium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun TBankCardVisual(pan: String, onCopy: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF16132A))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "ПЕРЕВОД В Т‑БАНК",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Номер карты получателя",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text("Копировать", color = Color.White)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.28f))
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                formatCard(pan),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun AccountDataBlock(
    title: String,
    hint: String,
    rows: List<Pair<String, String>>,
    copyAll: String,
    onCopy: (String, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TomiloSurface2)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(3.dp))
                Text(hint, color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = { onCopy(copyAll, title) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Скопировать всё")
            }
        }
        Spacer(Modifier.height(8.dp))
        rows.forEach { (label, value) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(72.dp),
                )
                Text(
                    value.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { if (value.isNotBlank()) onCopy(value, label) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = label, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun PaymentHistoryRow(item: PremiumPaymentHistoryItemDto) {
    val statusLabel = when (item.status) {
        "paid" -> "Оплачен"
        "pending" -> "Ожидает"
        "processing" -> "В обработке"
        "cancelled" -> "Отменён"
        else -> item.status
    }
    val amountLabel = if (item.currency == "COINS") {
        "${item.amount.toInt()} монет"
    } else {
        "${item.amount.toInt()} ₽"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TomiloSurface)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.description.ifBlank { "Premium" }, style = MaterialTheme.typography.titleSmall)
                Text(
                    listOfNotNull(
                        formatExpiry(item.paidAt ?: item.createdAt),
                        item.invId?.let { "№ $it" },
                    ).joinToString(" · "),
                    color = TomiloMuted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(amountLabel, fontWeight = FontWeight.Bold)
                Text(
                    statusLabel,
                    color = if (item.status == "paid") TomiloSuccess else TomiloMuted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

private fun formatExpiry(value: String?): String? = runCatching {
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
    Instant.parse(value).atZone(ZoneId.systemDefault()).format(formatter)
}.getOrNull()

private fun formatCard(pan: String): String = pan.chunked(4).joinToString(" ")
