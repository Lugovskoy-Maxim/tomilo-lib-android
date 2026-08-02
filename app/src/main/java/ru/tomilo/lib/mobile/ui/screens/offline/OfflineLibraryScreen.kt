package ru.tomilo.lib.mobile.ui.screens.offline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.data.repo.OfflineRepository
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.components.TitleListRow
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineLibraryScreen(
    offlineRepository: OfflineRepository,
    onOpenChapter: (chapterId: String) -> Unit,
) {
    val items by offlineRepository.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = { Text("Офлайн") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TomiloBg),
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(20.dp),
            ) {
                Text("Нет скачанных глав", color = TomiloMuted)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Откройте тайтл и нажмите иконку загрузки у главы. Нужен Premium.",
                    color = TomiloMuted,
                )
            }
        } else {
            LazyColumn(
                Modifier.padding(padding),
                contentPadding = ScreenPadding,
            ) {
                items(items, key = { it.chapterId }) { ch ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.weight(1f)) {
                            TitleListRow(
                                title = ch.titleName,
                                cover = ch.titleCover,
                                meta = "Глава ${ch.chapterNumber} · ${ch.pageCount} стр.",
                                onClick = { onOpenChapter(ch.chapterId) },
                            )
                        }
                        IconButton(
                            onClick = {
                                scope.launch { offlineRepository.deleteChapter(ch.chapterId) }
                            },
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                }
            }
        }
    }
}
