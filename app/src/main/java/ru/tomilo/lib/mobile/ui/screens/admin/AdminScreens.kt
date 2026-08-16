package ru.tomilo.lib.mobile.ui.screens.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.data.api.AdminCommentDto
import ru.tomilo.lib.mobile.data.api.AdminDashboardDto
import ru.tomilo.lib.mobile.data.api.AdminReportDto
import ru.tomilo.lib.mobile.data.api.AdminSiteSettingsDto
import ru.tomilo.lib.mobile.data.api.AdminSiteSettingsUpdate
import ru.tomilo.lib.mobile.data.api.AdminTitleDto
import ru.tomilo.lib.mobile.data.api.AdminTitleUpdateRequest
import ru.tomilo.lib.mobile.data.api.AdminUserDto
import ru.tomilo.lib.mobile.data.api.AutoParseJobDto
import ru.tomilo.lib.mobile.data.api.SourceCandidateDto
import ru.tomilo.lib.mobile.data.repo.AdminRepository
import ru.tomilo.lib.mobile.ui.components.EmptyState
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.PageIntro
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.components.TitleSearchCard
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloDanger
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium

private enum class AdminTab(val label: String) {
    Reports("Жалобы"),
    Users("Люди"),
    AutoSearch("Автопоиск"),
    Site("Сайт"),
    Titles("Тайтлы"),
    Dashboard("Обзор"),
    Comments("Комменты"),
    Tools("Ещё"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    adminRepository: AdminRepository,
    onBack: () -> Unit,
    onOpenTitle: (id: String, slug: String?) -> Unit,
    onOpenUser: (id: String) -> Unit = {},
) {
    var tab by remember { mutableStateOf(AdminTab.Reports) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var dashboard by remember { mutableStateOf<AdminDashboardDto?>(null) }
    var users by remember { mutableStateOf<List<AdminUserDto>>(emptyList()) }
    var comments by remember { mutableStateOf<List<AdminCommentDto>>(emptyList()) }
    var titles by remember { mutableStateOf<List<AdminTitleDto>>(emptyList()) }
    var reports by remember { mutableStateOf<List<AdminReportDto>>(emptyList()) }
    var jobs by remember { mutableStateOf<List<AutoParseJobDto>>(emptyList()) }
    var settings by remember { mutableStateOf<AdminSiteSettingsDto?>(null) }
    var activity by remember { mutableStateOf<List<String>>(emptyList()) }
    var userSearch by remember { mutableStateOf("") }
    var titleSearch by remember { mutableStateOf("") }
    var toolMsg by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    fun flash(msg: String?) { error = msg }

    fun load() {
        scope.launch {
            loading = true
            error = null
            when (tab) {
                AdminTab.Reports -> adminRepository.reports()
                    .onSuccess { reports = it }
                    .onFailure { error = it.message }
                AdminTab.Users -> adminRepository.users(search = userSearch.ifBlank { null })
                    .onSuccess { users = it }
                    .onFailure { error = it.message }
                AdminTab.AutoSearch -> adminRepository.autoJobs()
                    .onSuccess { jobs = it }
                    .onFailure { error = it.message }
                AdminTab.Site -> adminRepository.siteSettings()
                    .onSuccess { settings = it }
                    .onFailure { error = it.message }
                AdminTab.Titles -> adminRepository.titles(search = titleSearch.ifBlank { null })
                    .onSuccess { titles = it }
                    .onFailure { error = it.message }
                AdminTab.Dashboard -> {
                    adminRepository.dashboard().onSuccess { dashboard = it }.onFailure { error = it.message }
                    adminRepository.activity().onSuccess { activity = it }
                }
                AdminTab.Comments -> adminRepository.comments()
                    .onSuccess { comments = it }
                    .onFailure { error = it.message }
                AdminTab.Tools -> Unit
            }
            loading = false
        }
    }

    LaunchedEffect(tab, reload) { load() }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = { Text("Админка") },
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
            PageIntro(
                title = "Как на сайте",
                subtitle = "Жалобы, премиум, автопоиск, настройки и правка тайтлов",
                icon = Icons.Default.AdminPanelSettings,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                AdminTab.entries.forEach { t ->
                    FilterChip(
                        selected = tab == t,
                        onClick = { tab = t },
                        label = { Text(t.label) },
                        modifier = Modifier.padding(horizontal = 3.dp),
                    )
                }
            }
            if (error != null && !loading) {
                Text(error!!, color = TomiloDanger, modifier = Modifier.padding(horizontal = 16.dp))
            }
            when {
                loading -> LoadingBox()
                tab == AdminTab.Reports -> ReportsPane(
                    reports = reports,
                    onResolve = { id, msg ->
                        scope.launch {
                            adminRepository.resolveReport(id, msg)
                                .onFailure { flash(it.message) }
                            reload += 1
                        }
                    },
                    onDelete = { id ->
                        scope.launch {
                            adminRepository.deleteReport(id).onFailure { flash(it.message) }
                            reload += 1
                        }
                    },
                    onOpenTitle = onOpenTitle,
                )
                tab == AdminTab.Users -> UsersPane(
                    users = users,
                    search = userSearch,
                    onSearchChange = { userSearch = it },
                    onSearch = { reload += 1 },
                    onOpenUser = onOpenUser,
                    onBan = { id, ban ->
                        scope.launch {
                            val r = if (ban) adminRepository.banUser(id) else adminRepository.unbanUser(id)
                            r.onFailure { flash(it.message) }
                            reload += 1
                        }
                    },
                    onRole = { id, role ->
                        scope.launch {
                            adminRepository.setRole(id, role).onFailure { flash(it.message) }
                            reload += 1
                        }
                    },
                    onPremiumDays = { id, days, current ->
                        scope.launch {
                            adminRepository.grantPremiumDays(id, days, current)
                                .onFailure { flash(it.message) }
                                .onSuccess { flash("Premium +$days дн") }
                            reload += 1
                        }
                    },
                    onPremiumClear = { id ->
                        scope.launch {
                            adminRepository.setPremiumUntil(id, null)
                                .onFailure { flash(it.message) }
                            reload += 1
                        }
                    },
                    onBalance = { id, amount ->
                        scope.launch {
                            adminRepository.changeBalance(id, amount, "Админка Android")
                                .onFailure { flash(it.message) }
                            reload += 1
                        }
                    },
                )
                tab == AdminTab.AutoSearch -> AutoSearchPane(
                    jobs = jobs,
                    onToggle = { id, enabled ->
                        scope.launch {
                            adminRepository.setAutoJobEnabled(id, enabled).onFailure { flash(it.message) }
                            reload += 1
                        }
                    },
                    onRun = { id ->
                        scope.launch {
                            adminRepository.runAutoJob(id)
                                .onSuccess { flash(it) }
                                .onFailure { flash(it.message) }
                            reload += 1
                        }
                    },
                    onDelete = { id ->
                        scope.launch {
                            adminRepository.deleteAutoJob(id).onFailure { flash(it.message) }
                            reload += 1
                        }
                    },
                    onCreate = { titleId, url ->
                        scope.launch {
                            adminRepository.createAutoJob(titleId, url)
                                .onFailure { flash(it.message) }
                                .onSuccess { flash("Задание создано") }
                            reload += 1
                        }
                    },
                    onSearchSources = { titleId -> adminRepository.searchSources(titleId) },
                    onImportUrl = { url ->
                        scope.launch {
                            adminRepository.importByUrl(url)
                                .onSuccess { flash(it) }
                                .onFailure { flash(it.message) }
                        }
                    },
                    onOpenTitle = onOpenTitle,
                )
                tab == AdminTab.Site -> SitePane(
                    settings = settings,
                    onSave = { body ->
                        scope.launch {
                            adminRepository.updateSiteSettings(body)
                                .onSuccess {
                                    settings = it
                                    flash("Настройки сохранены")
                                }
                                .onFailure { flash(it.message) }
                        }
                    },
                )
                tab == AdminTab.Titles -> TitlesPane(
                    titles = titles,
                    search = titleSearch,
                    onSearchChange = { titleSearch = it },
                    onSearch = { reload += 1 },
                    onOpen = onOpenTitle,
                    onSave = { id, body ->
                        scope.launch {
                            adminRepository.updateTitle(id, body)
                                .onFailure { flash(it.message) }
                                .onSuccess { flash("Тайтл сохранён") }
                            reload += 1
                        }
                    },
                    onDelete = { id ->
                        scope.launch {
                            adminRepository.deleteTitle(id).onFailure { flash(it.message) }
                            reload += 1
                        }
                    },
                )
                tab == AdminTab.Dashboard -> DashboardPane(dashboard, activity)
                tab == AdminTab.Comments -> CommentsPane(
                    comments = comments,
                    onHide = { id, hide ->
                        scope.launch {
                            adminRepository.hideComment(id, hide).onFailure { flash(it.message) }
                            reload += 1
                        }
                    },
                    onDelete = { id ->
                        scope.launch {
                            adminRepository.deleteComment(id).onFailure { flash(it.message) }
                            reload += 1
                        }
                    },
                )
                tab == AdminTab.Tools -> ToolsPane(
                    message = toolMsg,
                    onClearCache = {
                        scope.launch {
                            adminRepository.clearCache()
                                .onSuccess { toolMsg = it }
                                .onFailure { toolMsg = it.message }
                        }
                    },
                    onRefresh = { reload += 1 },
                )
            }
        }
    }
}

@Composable
private fun ReportsPane(
    reports: List<AdminReportDto>,
    onResolve: (id: String, message: String?) -> Unit,
    onDelete: (id: String) -> Unit,
    onOpenTitle: (id: String, slug: String?) -> Unit,
) {
    var replyId by remember { mutableStateOf<String?>(null) }
    var replyText by remember { mutableStateOf("") }
    if (reports.isEmpty()) {
        EmptyState(title = "Жалоб нет", message = "Новые обращения появятся здесь.")
        return
    }
    LazyColumn(contentPadding = ScreenPadding) {
        items(reports, key = { it.stableId() }) { r ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth()) {
                Text("${r.typeLabel()} · ${r.authorName()}", style = MaterialTheme.typography.titleMedium)
                Text(r.content.orEmpty(), maxLines = 5)
                Text(
                    listOfNotNull(r.entityType, r.createdAt?.take(16)?.replace('T', ' ')).joinToString(" · "),
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (r.resolved()) Text("закрыта", color = TomiloMuted, style = MaterialTheme.typography.labelSmall)
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!r.resolved()) {
                        OutlinedButton(onClick = {
                            replyId = r.stableId()
                            replyText = ""
                        }) { Text("Закрыть") }
                    }
                    r.titleId?.takeIf { it.isNotBlank() }?.let { tid ->
                        OutlinedButton(onClick = { onOpenTitle(tid, null) }) { Text("Тайтл") }
                    }
                    OutlinedButton(onClick = { onDelete(r.stableId()) }) { Text("Удалить") }
                }
            }
        }
    }
    if (replyId != null) {
        AlertDialog(
            onDismissRequest = { replyId = null },
            title = { Text("Ответ на жалобу") },
            text = {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Можно пусто") },
                )
            },
            confirmButton = {
                Button(onClick = {
                    onResolve(replyId!!, replyText)
                    replyId = null
                }) { Text("Закрыть жалобу") }
            },
            dismissButton = { TextButton(onClick = { replyId = null }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun UsersPane(
    users: List<AdminUserDto>,
    search: String,
    onSearchChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenUser: (String) -> Unit,
    onBan: (id: String, ban: Boolean) -> Unit,
    onRole: (id: String, role: String) -> Unit,
    onPremiumDays: (id: String, days: Int, current: String?) -> Unit,
    onPremiumClear: (id: String) -> Unit,
    onBalance: (id: String, amount: Int) -> Unit,
) {
    var expanded by remember { mutableStateOf<String?>(null) }
    var balanceText by remember { mutableStateOf("100") }
    Column {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = search,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Ник или email") },
            )
            Spacer(Modifier.padding(4.dp))
            Button(onClick = onSearch) { Text("OK") }
        }
        if (users.isEmpty()) {
            EmptyState(title = "Никого", message = "Другой запрос или нет прав.")
            return
        }
        LazyColumn(contentPadding = ScreenPadding) {
            items(users, key = { it.stableId() }) { u ->
                val id = u.stableId()
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(u.username ?: "user", style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOfNotNull(
                            u.email,
                            u.role,
                            u.level?.let { "lv $it" },
                            u.balance?.let { "$it монет" },
                            u.subscriptionExpiresAt?.take(10)?.let { "premium $it" },
                        ).joinToString(" · "),
                        color = TomiloMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (u.banned()) Text("ЗАБАНЕН", color = TomiloDanger)
                    Row(
                        Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        OutlinedButton(onClick = { expanded = if (expanded == id) null else id }) {
                            Text(if (expanded == id) "Свернуть" else "Управлять")
                        }
                        OutlinedButton(onClick = { onOpenUser(id) }) { Text("Профиль") }
                    }
                    if (expanded == id) {
                        Text("Premium", color = TomiloPremium, modifier = Modifier.padding(top = 10.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(7, 30, 90, 365).forEach { d ->
                                Button(onClick = { onPremiumDays(id, d, u.subscriptionExpiresAt) }) {
                                    Text(if (d == 365) "+год" else "+$d дн")
                                }
                            }
                            OutlinedButton(onClick = { onPremiumClear(id) }) { Text("Снять") }
                        }
                        Text("Роль", modifier = Modifier.padding(top = 10.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("user", "translator", "moderator", "admin").forEach { role ->
                                OutlinedButton(onClick = { onRole(id, role) }) { Text(role) }
                            }
                        }
                        Text("Бан / монеты", modifier = Modifier.padding(top = 10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { onBan(id, !u.banned()) }) {
                                Text(if (u.banned()) "Разбан" else "Бан")
                            }
                            Spacer(Modifier.padding(6.dp))
                            OutlinedTextField(
                                value = balanceText,
                                onValueChange = { balanceText = it.filter(Char::isDigit).take(7) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("Монеты") },
                            )
                            Spacer(Modifier.padding(4.dp))
                            OutlinedButton(onClick = {
                                val n = balanceText.toIntOrNull() ?: return@OutlinedButton
                                onBalance(id, n)
                            }) { Text("+") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoSearchPane(
    jobs: List<AutoParseJobDto>,
    onToggle: (id: String, enabled: Boolean) -> Unit,
    onRun: (id: String) -> Unit,
    onDelete: (id: String) -> Unit,
    onCreate: (titleId: String, url: String?) -> Unit,
    onSearchSources: suspend (titleId: String) -> Result<List<SourceCandidateDto>>,
    onImportUrl: (url: String) -> Unit,
    onOpenTitle: (id: String, slug: String?) -> Unit,
) {
    var titleId by remember { mutableStateOf("") }
    var sourceUrl by remember { mutableStateOf("") }
    var importUrl by remember { mutableStateOf("") }
    var candidates by remember { mutableStateOf<List<SourceCandidateDto>>(emptyList()) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("Новое задание", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = titleId,
                onValueChange = { titleId = it },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                singleLine = true,
                label = { Text("ID тайтла") },
            )
            OutlinedTextField(
                value = sourceUrl,
                onValueChange = { sourceUrl = it },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                singleLine = true,
                label = { Text("URL источника") },
            )
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onCreate(titleId.trim(), sourceUrl.trim().ifBlank { null }) },
                    enabled = titleId.isNotBlank(),
                ) { Text("Создать") }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            onSearchSources(titleId.trim()).onSuccess { candidates = it }
                        }
                    },
                    enabled = titleId.isNotBlank(),
                ) { Text("Найти источники") }
            }
            candidates.forEach { c ->
                Text(
                    "${c.site}: ${c.title} (${c.score ?: 0})",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!c.url.isNullOrBlank()) {
                    TextButton(onClick = { sourceUrl = c.url.orEmpty() }) { Text("Подставить URL") }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = importUrl,
                onValueChange = { importUrl = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Импорт тайтла по URL") },
            )
            OutlinedButton(
                onClick = { onImportUrl(importUrl.trim()) },
                enabled = importUrl.isNotBlank(),
                modifier = Modifier.padding(top = 6.dp),
            ) { Text("Парсить и создать") }
            Spacer(Modifier.height(12.dp))
            Text("Задания (${jobs.size})", style = MaterialTheme.typography.titleMedium)
        }
        if (jobs.isEmpty()) {
            EmptyState(title = "Нет заданий", message = "Создайте автопоиск по ID тайтла.")
            return
        }
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = ScreenPadding) {
            items(jobs, key = { it.stableId() }) { job ->
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(job.titleName(), style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOfNotNull(
                            if (job.enabled == true) "вкл" else "выкл",
                            job.frequency,
                            job.scheduleHour?.let { "UTC $it:${job.scheduleMinute ?: 0}" },
                            job.sources?.firstOrNull(),
                        ).joinToString(" · "),
                        color = TomiloMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        OutlinedButton(onClick = { onToggle(job.stableId(), job.enabled != true) }) {
                            Text(if (job.enabled == true) "Выкл" else "Вкл")
                        }
                        OutlinedButton(onClick = { onRun(job.stableId()) }) { Text("Проверить главы") }
                        OutlinedButton(onClick = { onOpenTitle(job.titleKey(), null) }) { Text("Тайтл") }
                        OutlinedButton(onClick = { onDelete(job.stableId()) }) { Text("Удалить") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SitePane(
    settings: AdminSiteSettingsDto?,
    onSave: (AdminSiteSettingsUpdate) -> Unit,
) {
    if (settings == null) {
        ErrorBox("Нет настроек")
        return
    }
    var maintenance by remember(settings) { mutableStateOf(settings.maintenanceMode == true) }
    var maintenanceMsg by remember(settings) { mutableStateOf(settings.maintenanceMessage.orEmpty()) }
    var registration by remember(settings) { mutableStateOf(settings.registrationEnabled != false) }
    var comments by remember(settings) { mutableStateOf(settings.commentsEnabled != false) }
    var ratings by remember(settings) { mutableStateOf(settings.ratingsEnabled != false) }
    var adult by remember(settings) { mutableStateOf(settings.adultContentEnabled == true) }
    var adultAuth by remember(settings) { mutableStateOf(settings.adultContentRequiresAuth == true) }
    var siteName by remember(settings) { mutableStateOf(settings.siteName.orEmpty()) }
    var siteDesc by remember(settings) { mutableStateOf(settings.siteDescription.orEmpty()) }
    var contact by remember(settings) { mutableStateOf(settings.contactEmail.orEmpty()) }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 80.dp),
    ) {
        ToggleRow("Техработы", maintenance) { maintenance = it }
        OutlinedTextField(
            value = maintenanceMsg,
            onValueChange = { maintenanceMsg = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            label = { Text("Сообщение техработ") },
        )
        ToggleRow("Регистрация", registration) { registration = it }
        ToggleRow("Комментарии", comments) { comments = it }
        ToggleRow("Оценки", ratings) { ratings = it }
        ToggleRow("18+ контент", adult) { adult = it }
        ToggleRow("18+ только после входа", adultAuth) { adultAuth = it }
        OutlinedTextField(value = siteName, onValueChange = { siteName = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Имя сайта") })
        OutlinedTextField(value = siteDesc, onValueChange = { siteDesc = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Описание") })
        OutlinedTextField(value = contact, onValueChange = { contact = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Email") })
        Button(
            onClick = {
                onSave(
                    AdminSiteSettingsUpdate(
                        maintenanceMode = maintenance,
                        maintenanceMessage = maintenanceMsg,
                        registrationEnabled = registration,
                        commentsEnabled = comments,
                        ratingsEnabled = ratings,
                        adultContentEnabled = adult,
                        adultContentRequiresAuth = adultAuth,
                        siteName = siteName.ifBlank { null },
                        siteDescription = siteDesc.ifBlank { null },
                        contactEmail = contact.ifBlank { null },
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) { Text("Сохранить") }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun DashboardPane(d: AdminDashboardDto?, activity: List<String>) {
    LazyColumn(contentPadding = ScreenPadding) {
        item {
            Text("Статистика", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
            if (d == null) {
                Text("Нет данных", color = TomiloMuted, modifier = Modifier.padding(16.dp))
            } else {
                Stat("Пользователи", d.totalUsers ?: d.users)
                Stat("Тайтлы", d.totalTitles ?: d.titles)
                Stat("Главы", d.totalChapters ?: d.chapters)
                Stat("Комментарии", d.totalComments ?: d.comments)
                Stat("Просмотры", d.totalViews?.toInt())
                Stat("Premium", d.premiumUsers)
                Stat("Новые за день", d.newUsersToday)
                Stat("Новые за неделю", d.newUsersWeek)
                Stat("Активные", d.activeUsers)
            }
            Spacer(Modifier.height(12.dp))
            Text("Активность", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
        }
        items(activity) { line ->
            Text(line, color = TomiloMuted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun Stat(label: String, value: Int?) {
    if (value == null) return
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(label, color = TomiloMuted, modifier = Modifier.weight(1f))
        Text(value.toString(), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun CommentsPane(
    comments: List<AdminCommentDto>,
    onHide: (id: String, hide: Boolean) -> Unit,
    onDelete: (id: String) -> Unit,
) {
    if (comments.isEmpty()) {
        EmptyState(title = "Комментариев нет", message = "Модерация пуста.")
        return
    }
    LazyColumn(contentPadding = ScreenPadding) {
        items(comments, key = { it.stableId() }) { c ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(c.authorName(), style = MaterialTheme.typography.labelLarge)
                Text(c.content.orEmpty(), maxLines = 4)
                if (c.hidden()) Text("скрыт", color = TomiloDanger, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.padding(top = 6.dp)) {
                    OutlinedButton(onClick = { onHide(c.stableId(), !c.hidden()) }) {
                        Text(if (c.hidden()) "Показать" else "Скрыть")
                    }
                    Spacer(Modifier.padding(4.dp))
                    OutlinedButton(onClick = { onDelete(c.stableId()) }) { Text("Удалить") }
                }
            }
        }
    }
}

@Composable
private fun TitlesPane(
    titles: List<AdminTitleDto>,
    search: String,
    onSearchChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpen: (id: String, slug: String?) -> Unit,
    onSave: (id: String, body: AdminTitleUpdateRequest) -> Unit,
    onDelete: (id: String) -> Unit,
) {
    var edit by remember { mutableStateOf<AdminTitleDto?>(null) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var published by remember { mutableStateOf(true) }
    Column {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = search,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Поиск тайтла") },
            )
            Spacer(Modifier.padding(4.dp))
            Button(onClick = onSearch) { Text("OK") }
        }
        if (titles.isEmpty()) {
            EmptyState(title = "Пусто", message = "Другой запрос.")
            return
        }
        LazyColumn(contentPadding = ScreenPadding) {
            items(titles, key = { it.stableId() }) { t ->
                Column {
                    TitleSearchCard(
                        title = t.displayName(),
                        cover = t.coverPath(),
                        type = t.type,
                        totalChapters = t.totalChapters,
                        subtitle = if (t.isPublished == false) "Черновик"
                        else t.status?.let { ru.tomilo.lib.mobile.core.GenreLabels.status(it) },
                        onClick = { onOpen(t.stableId(), t.slug) },
                    )
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(onClick = {
                            edit = t
                            name = t.displayName()
                            type = t.type.orEmpty()
                            status = t.status.orEmpty()
                            published = t.isPublished != false
                        }) { Text("Править") }
                        OutlinedButton(onClick = { onDelete(t.stableId()) }) { Text("Удалить") }
                    }
                }
            }
        }
    }
    val current = edit
    if (current != null) {
        AlertDialog(
            onDismissRequest = { edit = null },
            title = { Text("Тайтл") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Тип: manga / manhwa") }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                    OutlinedTextField(value = status, onValueChange = { status = it }, label = { Text("Статус: ongoing / completed") }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                    ToggleRow("Опубликован", published) { published = it }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSave(
                        current.stableId(),
                        AdminTitleUpdateRequest(
                            name = name.ifBlank { null },
                            type = type.ifBlank { null },
                            status = status.ifBlank { null },
                            isPublished = published,
                        ),
                    )
                    edit = null
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { edit = null }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun ToolsPane(
    message: String?,
    onClearCache: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(Modifier.padding(20.dp)) {
        Text("Сервер", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onClearCache, modifier = Modifier.fillMaxWidth()) {
            Text("Очистить серверный кеш")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Text("Обновить данные")
        }
        if (message != null) {
            Spacer(Modifier.height(12.dp))
            Text(message, color = TomiloMuted)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Игры, магазин, промокоды и арена — на сайте. Здесь модерация и контент.",
            color = TomiloMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
