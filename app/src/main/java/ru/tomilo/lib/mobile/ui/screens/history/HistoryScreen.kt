package ru.tomilo.lib.mobile.ui.screens.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.tomilo.lib.mobile.data.api.HistoryEntryDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.HistoryRepository
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.components.TitleSearchCard
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    authRepository: AuthRepository,
    historyRepository: HistoryRepository,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenTitle: (id: String, slug: String?) -> Unit,
    onOpenChapter: (titleId: String, chapterId: String) -> Unit,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    var authReady by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<HistoryEntryDto>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(40)
        authReady = true
    }

    LaunchedEffect(user?.stableId(), reload, authReady) {
        if (!authReady) return@LaunchedEffect
        if (user == null) {
            items = emptyList()
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        historyRepository.history()
            .onSuccess { items = it }
            .onFailure { error = it.message }
        loading = false
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = { Text("История чтения") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TomiloBg),
            )
        },
    ) { padding ->
        if (!authReady || (loading && user == null)) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        if (user == null) {
            Column(Modifier.padding(padding)) {
                ErrorBox("Войдите, чтобы видеть историю", onRetry = onLogin)
            }
            return@Scaffold
        }
        when {
            loading -> LoadingBox(Modifier.padding(padding))
            error != null && items.isEmpty() -> Column(Modifier.padding(padding)) {
                ErrorBox(error ?: "Ошибка") { reload += 1 }
            }
            items.isEmpty() -> Text(
                "История пуста. Откройте главу — прогресс сохранится.",
                color = TomiloMuted,
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            else -> LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = ScreenPadding,
            ) {
                items(
                    items,
                    key = { h ->
                        h.titleKey() + "|" + h.chapterKey() + "|" + (h.readAt ?: "")
                    },
                ) { h ->
                    val tid = h.titleKey()
                    val cid = h.chapterKey()
                    val count = h.chaptersCount
                    TitleSearchCard(
                        title = h.displayTitle(),
                        cover = h.coverPath(),
                        type = h.type(),
                        subtitle = buildString {
                            append(h.chapterLabel())
                            if (count != null && count > 1) append(" · всего $count гл.")
                            h.readAtLabel()?.let { append(" · $it") }
                        },
                        onClick = {
                            when {
                                tid.isNotBlank() && cid.isNotBlank() -> onOpenChapter(tid, cid)
                                tid.isNotBlank() -> onOpenTitle(tid, h.slug())
                            }
                        },
                    )
                }
            }
        }
    }
}
