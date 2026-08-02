package ru.tomilo.lib.mobile.ui.screens.admin

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.data.api.AdminCommentDto
import ru.tomilo.lib.mobile.data.api.AdminDashboardDto
import ru.tomilo.lib.mobile.data.api.AdminTitleDto
import ru.tomilo.lib.mobile.data.api.AdminUserDto
import ru.tomilo.lib.mobile.data.repo.AdminRepository
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.components.TitleSearchCard
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloDanger
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted

private enum class AdminTab(val label: String) {
    Dashboard("Дашборд"),
    Users("Юзеры"),
    Comments("Комменты"),
    Titles("Тайтлы"),
    Tools("Инструменты"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    adminRepository: AdminRepository,
    onBack: () -> Unit,
    onOpenTitle: (id: String, slug: String?) -> Unit,
) {
    var tab by remember { mutableStateOf(AdminTab.Dashboard) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var dashboard by remember { mutableStateOf<AdminDashboardDto?>(null) }
    var users by remember { mutableStateOf<List<AdminUserDto>>(emptyList()) }
    var comments by remember { mutableStateOf<List<AdminCommentDto>>(emptyList()) }
    var titles by remember { mutableStateOf<List<AdminTitleDto>>(emptyList()) }
    var activity by remember { mutableStateOf<List<String>>(emptyList()) }
    var userSearch by remember { mutableStateOf("") }
    var titleSearch by remember { mutableStateOf("") }
    var toolMsg by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            loading = true
            error = null
            when (tab) {
                AdminTab.Dashboard -> {
                    adminRepository.dashboard()
                        .onSuccess { dashboard = it }
                        .onFailure { error = it.message }
                    adminRepository.activity()
                        .onSuccess { activity = it }
                        .onFailure { /* optional */ }
                }
                AdminTab.Users -> {
                    adminRepository.users(search = userSearch.ifBlank { null })
                        .onSuccess { users = it }
                        .onFailure { error = it.message }
                }
                AdminTab.Comments -> {
                    adminRepository.comments()
                        .onSuccess { comments = it }
                        .onFailure { error = it.message }
                }
                AdminTab.Titles -> {
                    adminRepository.titles(search = titleSearch.ifBlank { null })
                        .onSuccess { titles = it }
                        .onFailure { error = it.message }
                }
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TomiloBg),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
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
                Text(error!!, color = TomiloDanger, modifier = Modifier.padding(16.dp))
            }
            when {
                loading -> LoadingBox()
                tab == AdminTab.Dashboard -> DashboardPane(dashboard, activity)
                tab == AdminTab.Users -> UsersPane(
                    users = users,
                    search = userSearch,
                    onSearchChange = { userSearch = it },
                    onSearch = { reload += 1 },
                    onBan = { id, ban ->
                        scope.launch {
                            val r = if (ban) adminRepository.banUser(id) else adminRepository.unbanUser(id)
                            r.onFailure { error = it.message }
                            reload += 1
                        }
                    },
                    onRole = { id, role ->
                        scope.launch {
                            adminRepository.setRole(id, role)
                                .onFailure { error = it.message }
                            reload += 1
                        }
                    },
                )
                tab == AdminTab.Comments -> CommentsPane(
                    comments = comments,
                    onHide = { id, hide ->
                        scope.launch {
                            adminRepository.hideComment(id, hide)
                                .onFailure { error = it.message }
                            reload += 1
                        }
                    },
                    onDelete = { id ->
                        scope.launch {
                            adminRepository.deleteComment(id)
                                .onFailure { error = it.message }
                            reload += 1
                        }
                    },
                )
                tab == AdminTab.Titles -> TitlesPane(
                    titles = titles,
                    search = titleSearch,
                    onSearchChange = { titleSearch = it },
                    onSearch = { reload += 1 },
                    onOpen = onOpenTitle,
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
private fun UsersPane(
    users: List<AdminUserDto>,
    search: String,
    onSearchChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBan: (id: String, ban: Boolean) -> Unit,
    onRole: (id: String, role: String) -> Unit,
) {
    Column {
        Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = search,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Поиск") },
            )
            Spacer(Modifier.padding(4.dp))
            Button(onClick = onSearch) { Text("OK") }
        }
        LazyColumn(contentPadding = ScreenPadding) {
            items(users, key = { it.stableId() }) { u ->
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(u.username ?: "user", style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOfNotNull(u.email, u.role, u.level?.let { "lv $it" }).joinToString(" · "),
                        color = TomiloMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (u.banned()) Text("ЗАБАНЕН", color = TomiloDanger)
                    Row(Modifier.padding(top = 6.dp)) {
                        OutlinedButton(onClick = { onBan(u.stableId(), !u.banned()) }) {
                            Text(if (u.banned()) "Разбан" else "Бан")
                        }
                        Spacer(Modifier.padding(4.dp))
                        OutlinedButton(onClick = { onRole(u.stableId(), "moderator") }) {
                            Text("→ mod")
                        }
                        Spacer(Modifier.padding(4.dp))
                        OutlinedButton(onClick = { onRole(u.stableId(), "reader") }) {
                            Text("→ user")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentsPane(
    comments: List<AdminCommentDto>,
    onHide: (id: String, hide: Boolean) -> Unit,
    onDelete: (id: String) -> Unit,
) {
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
                    OutlinedButton(onClick = { onDelete(c.stableId()) }) {
                        Text("Удалить")
                    }
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
) {
    Column {
        Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
        LazyColumn(contentPadding = ScreenPadding) {
            items(titles, key = { it.stableId() }) { t ->
                TitleSearchCard(
                    title = t.displayName(),
                    cover = t.coverPath(),
                    type = t.type,
                    totalChapters = t.totalChapters,
                    subtitle = if (t.isPublished == false) "draft" else null,
                    onClick = { onOpen(t.stableId(), t.slug) },
                )
            }
        }
    }
}

@Composable
private fun ToolsPane(
    message: String?,
    onClearCache: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(Modifier.padding(20.dp)) {
        Text("Серверные инструменты", style = MaterialTheme.typography.titleLarge)
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
            "Доступ: admin (дашборд/кеш), moderator+admin (юзеры/комменты).",
            color = TomiloMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
