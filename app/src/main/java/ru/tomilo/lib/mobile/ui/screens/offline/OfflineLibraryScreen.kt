package ru.tomilo.lib.mobile.ui.screens.offline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.local.OfflineChapterEntity
import ru.tomilo.lib.mobile.data.repo.OfflineRepository
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

private data class OfflineTitleGroup(
    val titleId: String,
    val titleName: String,
    val titleSlug: String,
    val titleCover: String?,
    val chapters: List<OfflineChapterEntity>,
    val bytesTotal: Long,
    val lastDownloadedAt: Long,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineLibraryScreen(
    offlineRepository: OfflineRepository,
    onOpenChapter: (chapterId: String, titleId: String) -> Unit,
) {
    val flat by offlineRepository.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(setOf<String>()) }

    val groups = remember(flat) {
        flat
            .groupBy { it.titleId.ifBlank { it.titleSlug.ifBlank { it.titleName } } }
            .map { (key, chapters) ->
                val sample = chapters.first()
                OfflineTitleGroup(
                    titleId = sample.titleId.ifBlank { key },
                    titleName = sample.titleName.ifBlank { "Тайтл" },
                    titleSlug = sample.titleSlug,
                    titleCover = sample.titleCover,
                    chapters = chapters.sortedWith(
                        compareBy(
                            { it.chapterNumber.toDoubleOrNull() ?: Double.MAX_VALUE },
                            { it.chapterNumber },
                        ),
                    ),
                    bytesTotal = chapters.sumOf { it.bytesTotal },
                    lastDownloadedAt = chapters.maxOfOrNull { it.downloadedAt } ?: 0L,
                )
            }
            .sortedByDescending { it.lastDownloadedAt }
    }

    // Expand first group by default when data appears
    androidx.compose.runtime.LaunchedEffect(groups.map { it.titleId }) {
        if (expanded.isEmpty() && groups.isNotEmpty()) {
            expanded = setOf(groups.first().titleId)
        }
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Офлайн")
                        if (groups.isNotEmpty()) {
                            Text(
                                "${groups.size} тайтл. · ${flat.size} гл.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TomiloMuted,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TomiloBg),
            )
        },
    ) { padding ->
        if (groups.isEmpty()) {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(20.dp),
            ) {
                Text("Нет скачанных глав", color = TomiloMuted)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Откройте тайтл → выберите главы → «Скачать». Нужен Premium.",
                    color = TomiloMuted,
                )
            }
        } else {
            LazyColumn(
                Modifier.padding(padding),
                contentPadding = ScreenPadding,
            ) {
                items(groups, key = { it.titleId }) { group ->
                    val isOpen = group.titleId in expanded
                    OfflineTitleBlock(
                        group = group,
                        expanded = isOpen,
                        onToggle = {
                            expanded = if (isOpen) expanded - group.titleId
                            else expanded + group.titleId
                        },
                        onOpenChapter = { ch ->
                            onOpenChapter(ch.chapterId, group.titleId)
                        },
                        onDeleteChapter = { ch ->
                            scope.launch { offlineRepository.deleteChapter(ch.chapterId) }
                        },
                        onDeleteTitle = {
                            scope.launch { offlineRepository.deleteTitle(group.titleId) }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OfflineTitleBlock(
    group: OfflineTitleGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenChapter: (OfflineChapterEntity) -> Unit,
    onDeleteChapter: (OfflineChapterEntity) -> Unit,
    onDeleteTitle: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(TomiloSurface2.copy(alpha = 0.7f)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = MediaUrl.resolve(group.titleCover),
                contentDescription = group.titleName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 56.dp, height = 78.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TomiloSurface2),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    group.titleName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${group.chapters.size} гл. · ${formatBytes(group.bytesTotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TomiloMuted,
                )
            }
            IconButton(onClick = onDeleteTitle) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить тайтл",
                    tint = TomiloMuted,
                )
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Свернуть" else "Развернуть",
                tint = TomiloMuted,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(bottom = 6.dp)) {
                group.chapters.forEach { ch ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenChapter(ch) }
                            .padding(start = 78.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Глава ${ch.chapterNumber}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            val sub = listOfNotNull(
                                ch.chapterName?.takeIf {
                                    it.isNotBlank() && !it.equals("Глава ${ch.chapterNumber}", true)
                                },
                                "${ch.pageCount} стр.",
                                formatBytes(ch.bytesTotal).takeIf { ch.bytesTotal > 0 },
                            ).joinToString(" · ")
                            if (sub.isNotBlank()) {
                                Text(sub, style = MaterialTheme.typography.bodySmall, color = TomiloMuted)
                            }
                        }
                        IconButton(onClick = { onDeleteChapter(ch) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить главу",
                                tint = TomiloMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "—"
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.0f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.2f GB".format(mb / 1024.0)
}
