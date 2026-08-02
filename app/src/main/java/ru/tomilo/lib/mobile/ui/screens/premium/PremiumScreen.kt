package ru.tomilo.lib.mobile.ui.screens.premium

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

private data class Plan(
    val label: String,
    val price: String,
    val period: String,
    val accent: Boolean = false,
    val badge: String? = null,
)

private val PLANS = listOf(
    Plan("1 месяц", "150", "30 дней премиума"),
    Plan("3 месяца", "400", "90 дней — выгоднее помесячно", accent = true, badge = "Популярно"),
    Plan("6 месяцев", "700", "180 дней — максимальная экономия"),
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
    var copiedHint by remember { mutableStateOf<String?>(null) }
    val isPremium = Premium.isActive(user?.subscriptionExpiresAt)

    fun copy(text: String, label: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        copiedHint = "Скопировано: $label"
    }

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = { Text("Премиум") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TomiloBg),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = TomiloPremium)
                Spacer(Modifier.padding(4.dp))
                Text(
                    "Премиум-подписка",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Ранний доступ к платным главам, офлайн-скачивание в приложении. " +
                    "Активация вручную в течение 1–24 часов после оплаты.",
                color = TomiloMuted,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(12.dp))
            Text(
                if (isPremium) "Premium активен ✓" else "Без Premium",
                color = if (isPremium) TomiloPremium else TomiloMuted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            user?.subscriptionExpiresAt?.takeIf { isPremium }?.let {
                Text("до $it", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(20.dp))
            Text("Что даёт", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Benefit("Платные главы без ожидания")
            Benefit("Офлайн-библиотека без лимита (без рекламы)")
            Benefit("Без рекламных блоков на сайте")
            Benefit("Длинные пакеты дешевле помесячно")
            Spacer(Modifier.height(8.dp))
            Text(
                "Без Premium можно скачать 1 главу офлайн за просмотр рекламы с вознаграждением.",
                color = TomiloMuted,
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(24.dp))
            Text("Тарифы", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            PLANS.forEach { plan ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .then(
                            if (plan.accent) {
                                Modifier.border(1.dp, TomiloPremium.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            } else Modifier
                        ),
                    colors = CardDefaults.cardColors(containerColor = TomiloSurface),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        plan.badge?.let {
                            Text(
                                it.uppercase(),
                                color = TomiloPremium,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        Text(plan.label, color = TomiloMuted, style = MaterialTheme.typography.labelLarge)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                plan.price,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(" ₽", style = MaterialTheme.typography.titleLarge, color = TomiloMuted)
                        }
                        Text(plan.period, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 6.dp))
                            Text("Платные главы + офлайн", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Как оплатить", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "После входа скопируйте данные аккаунта и вставьте в комментарий к платежу " +
                    "(Boosty) или в сообщение получателю (Т‑Банк). Без данных перевод считается донатом без премиума.",
                color = TomiloMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))

            if (user == null) {
                Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
                    Text("Войти, чтобы скопировать данные")
                }
            } else {
                val fullBlock = buildString {
                    appendLine("ID аккаунта: ${user!!.stableId()}")
                    appendLine("Никнейм: ${user!!.username.orEmpty()}")
                    append("Email: ${user!!.email.orEmpty()}")
                }
                val tbankBlock = buildString {
                    appendLine("Никнейм: ${user!!.username.orEmpty()}")
                    append("Уровень: ${user!!.level ?: 0}")
                }

                IdentityBlock(
                    title = "Для Boosty (ID + ник + email)",
                    body = fullBlock,
                    onCopy = { copy(fullBlock, "данные для Boosty") },
                )
                Spacer(Modifier.height(10.dp))
                IdentityBlock(
                    title = "Для Т‑Банка (ник + уровень)",
                    body = tbankBlock,
                    onCopy = { copy(tbankBlock, "данные для Т‑Банка") },
                )
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { openUrl(BOOSTY_URL) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                Spacer(Modifier.padding(4.dp))
                Text("Открыть Boosty")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { copy(TBANK_CARD, "номер карты") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.padding(4.dp))
                Text("Карта Т‑Банк: ${formatCard(TBANK_CARD)}")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { openUrl(SITE_PREMIUM) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Страница на сайте")
            }

            copiedHint?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "После оплаты премиум подключают вручную обычно от 1 часа до 24 часов. " +
                    "Затем обновите профиль в приложении.",
                color = TomiloMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Benefit(text: String) {
    Row(
        Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun IdentityBlock(title: String, body: String, onCopy: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TomiloSurface2)
            .padding(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = TomiloMuted)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onCopy, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.ContentCopy, contentDescription = null)
            Spacer(Modifier.padding(4.dp))
            Text("Скопировать")
        }
    }
}

private fun formatCard(pan: String): String =
    pan.chunked(4).joinToString(" ")
