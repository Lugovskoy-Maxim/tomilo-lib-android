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
import ru.tomilo.lib.mobile.ui.components.TitleListRow
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
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<HistoryEntryDto>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(user?.stableId(), reload) {
        if (user == null) {
            items = emptyList()
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
                "История пуста",
                color = TomiloMuted,
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            else -> LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = ScreenPadding,
            ) {
                items(items, key = { it.titleKey() + it.chapterKey() + (it.readAt ?: "") }) { h ->
                    TitleListRow(
                        title = h.displayTitle(),
                        cover = h.coverPath(),
                        meta = listOfNotNull(
                            h.chapterLabel(),
                            h.readAt?.take(16)?.replace('T', ' '),
                        ).joinToString(" · "),
                        onClick = {
                            val tid = h.titleKey()
                            val cid = h.chapterKey()
                            when {
                                tid.isNotBlank() && cid.isNotBlank() -> onOpenChapter(tid, cid)
                                tid.isNotBlank() -> onOpenTitle(tid, h.titleSlug)
                            }
                        },
                    )
                }
            }
        }
    }
}
