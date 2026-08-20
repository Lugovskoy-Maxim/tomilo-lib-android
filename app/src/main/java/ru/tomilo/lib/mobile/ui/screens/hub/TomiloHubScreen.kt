package ru.tomilo.lib.mobile.ui.screens.hub

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.tomilo.lib.mobile.BuildConfig
import ru.tomilo.lib.mobile.ui.components.ActionRow
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.components.PageIntro
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary

private data class HubLink(
    val title: String,
    val subtitle: String,
    val path: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
)

private val contentLinks = listOf(
    HubLink("Подборки", "Тематические коллекции от сообщества", "/collections", Icons.Default.CollectionsBookmark, TomiloPrimary),
    HubLink("Новости", "Обновления проекта и анонсы", "/news", Icons.Default.Newspaper, Color(0xFF62B8FF)),
    HubLink("Гайды", "Полезные материалы для читателей", "/guides", Icons.AutoMirrored.Filled.MenuBook, Color(0xFF57C7B8)),
    HubLink("Магазин Tomilo", "Украшения профиля и игровые предметы", "/tomilo-shop", Icons.Default.ShoppingBag, TomiloPremium),
    HubLink("Игры", "Активные режимы публичной беты", "/games", Icons.Default.SportsEsports, Color(0xFF9B8CFF)),
    HubLink("Благодарности", "Участники развития библиотеки", "/thanks", Icons.Default.VolunteerActivism, Color(0xFFF06E9C)),
)

private val helpLinks = listOf(
    HubLink("Частые вопросы", "Ответы по аккаунту, чтению и Premium", "/faq", Icons.AutoMirrored.Filled.ContactSupport, TomiloPrimary),
    HubLink("О проекте", "Команда и история tomilo-lib", "/about", Icons.AutoMirrored.Filled.Article, Color(0xFF62B8FF)),
    HubLink("Условия и документы", "Правила, конфиденциальность и оферта", "/terms-of-use", Icons.Default.Gavel, TomiloMuted),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TomiloHubScreen(
    onBack: () -> Unit,
    onOpenShop: () -> Unit = {},
    onOpenGames: () -> Unit = {},
) {
    val context = LocalContext.current
    fun open(path: String) {
        if (path == "/games") {
            onOpenGames()
            return
        }
        if (path == "/tomilo-shop") {
            onOpenShop()
            return
        }
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.SITE_URL + path))) }
    }
    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = { Column { Text("Мир Tomilo"); Text("Возможности оригинального сайта", color = TomiloMuted, style = MaterialTheme.typography.labelSmall) } },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            PageIntro(
                title = "Весь мир tomilo-lib",
                subtitle = "Контент, сообщество, игры и помощь в одном месте",
                icon = Icons.Default.CollectionsBookmark,
                accent = TomiloPremium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text("Открывайте новое", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 3.dp))
            contentLinks.forEach { link -> ActionRow(link.icon, link.title, { open(link.path) }, subtitle = link.subtitle, iconTint = link.color) }
            Text("Помощь и информация", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp, bottom = 3.dp))
            helpLinks.forEach { link -> ActionRow(link.icon, link.title, { open(link.path) }, subtitle = link.subtitle, iconTint = link.color) }
        }
    }
}
