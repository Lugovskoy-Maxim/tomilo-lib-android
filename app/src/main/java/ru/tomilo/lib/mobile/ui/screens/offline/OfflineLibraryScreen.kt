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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import ru.tomilo.lib.mobile.data.local.OfflineChapterMeta
import ru.tomilo.lib.mobile.data.local.OfflineTitleEntity
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.HistoryRepository
import ru.tomilo.lib.mobile.data.repo.OfflineRepository
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

private data class OfflineChapterRow(
    val chapterId: String,
    val chapterNumber: String,
    val chapterName: String?,
    val pageCount: Int?,
    val isDownloaded: Boolean,
    val entity: OfflineChapterEntity?,
)

private data class OfflineTitleGroup(
    val titleId: String,
    val titleName: String,
    val titleSlug: String,
    val titleCover: String?,
    val totalChapters: Int,
    val chapters: List<OfflineChapterRow>,
    val bytesTotal: Long,
    val lastActivityAt: Long,
    val downloadedCount: Int,
    val readCount: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineLibraryScreen(
    offlineRepository: OfflineRepository,
    historyRepository: HistoryRepository,
    authRepository: AuthRepository,
    onOpenChapter: (chapterId: String, titleId: String) -> Unit,
    onOpenTitle: ((titleId: String, slug: String?) -> Unit)? = null,
) {
    val flat by offlineRepository.observeAll().collectAsState(initial = emptyList())
    val titlesMeta by offlineRepository.observeTitles().collectAsState(initial = emptyList())
    val user by authRepository.userFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(setOf<String>()) }
    var readByTitle by remember { mutableStateOf(mapOf<String, Set<String>>()) }
    var refreshing by remember { mutableStateOf(false) }
    var refreshMsg by remember { mutableStateOf<String?>(null) }

    // При открытии — обновить устаревшие каталоги (новые главы)
    LaunchedEffect(Unit) {
        refreshing = true
        val n = runCatching { offlineRepository.refreshStaleTitles() }.getOrDefault(0)
        refreshMsg = if (n > 0) "Обновлено тайтлов: $n" else null
        refreshing = false
    }

    val groups = remember(flat, titlesMeta, readByTitle) {
        buildGroups(flat, titlesMeta, readByTitle, offlineRepository)
    }

    LaunchedEffect(groups.map { it.titleId }, user?.stableId()) {
        if (expanded.isEmpty() && groups.isNotEmpty()) {
            expanded = setOf(groups.first().titleId)
        }
        if (user == null) {
            readByTitle = emptyMap()
            return@LaunchedEffect
        }
        val map = mutableMapOf<String, Set<String>>()
        groups.forEach { g ->
            if (g.titleId.isNotBlank()) {
                historyRepository.readIds(g.titleId)
                    .onSuccess { map[g.titleId] = it }
            }
        }
        readByTitle = map
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Офлайн")
                        if (groups.isNotEmpty()) {
                            val dl = groups.sumOf { it.downloadedCount }
                            val rd = groups.sumOf { it.readCount }
                            Text(
                                "${groups.size} тайтл. · скачано $dl" +
                                    if (rd > 0) " · прочитано $rd" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = TomiloMuted,
                            )
                        } else if (refreshing) {
                            Text("Синхронизация…", style = MaterialTheme.typography.bodySmall, color = TomiloMuted)
                        }
                        refreshMsg?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                refreshing = true
                                refreshMsg = null
                                val n = offlineRepository.refreshStaleTitles(maxAgeMs = 0)
                                refreshMsg = if (n > 0) "Обновлено: $n" else "Уже актуально"
                                refreshing = false
                            }
                        },
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить каталог")
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
                    "Откройте тайтл → выберите главы → «Скачать». Нужен Premium. " +
                        "При скачивании сохраняется весь список глав тайтла — видно, что скачано и прочитано.",
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
                    val readIds = readByTitle[group.titleId] ?: emptySet()
                    OfflineTitleBlock(
                        group = group.copy(
                            chapters = group.chapters.map { row ->
                                row.copy(
                                    // read marker applied in UI via readIds
                                )
                            },
                            readCount = group.chapters.count { it.chapterId in readIds },
                        ),
                        expanded = isOpen,
                        readChapterIds = readIds,
                        onToggle = {
                            expanded = if (isOpen) expanded - group.titleId
                            else expanded + group.titleId
                        },
                        onOpenChapter = { row ->
                            if (row.isDownloaded) {
                                onOpenChapter(row.chapterId, group.titleId)
                            } else {
                                onOpenTitle?.invoke(group.titleId, group.titleSlug.ifBlank { null })
                            }
                        },
                        onDeleteChapter = { row ->
                            row.entity?.let { ch ->
                                scope.launch { offlineRepository.deleteChapter(ch.chapterId) }
                            }
                        },
                        onDeleteTitle = {
                            scope.launch { offlineRepository.deleteTitle(group.titleId) }
                        },
                        onOpenTitleOnline = {
                            onOpenTitle?.invoke(group.titleId, group.titleSlug.ifBlank { null })
                        },
                    )
                }
            }
        }
    }
}

private fun buildGroups(
    flat: List<OfflineChapterEntity>,
    titlesMeta: List<OfflineTitleEntity>,
    readByTitle: Map<String, Set<String>>,
    offlineRepository: OfflineRepository,
): List<OfflineTitleGroup> {
    val byTitle = flat.groupBy { it.titleId.ifBlank { it.titleSlug.ifBlank { it.titleName } } }
    val metaById = titlesMeta.associateBy { it.titleId }
    val titleIds = (byTitle.keys + metaById.keys).filter { it.isNotBlank() }.toSet()

    return titleIds.map { tid ->
        val downloaded = byTitle[tid].orEmpty()
        val sample = downloaded.firstOrNull()
        val meta = metaById[tid]
        val name = meta?.name?.takeIf { it.isNotBlank() }
            ?: sample?.titleName?.takeIf { it.isNotBlank() }
            ?: "Тайтл"
        val slug = meta?.slug?.takeIf { it.isNotBlank() } ?: sample?.titleSlug.orEmpty()
        val cover = meta?.coverImage ?: sample?.titleCover
        val dlMap = downloaded.associateBy { it.chapterId }

        val metaChapters: List<OfflineChapterMeta> = meta?.let {
            offlineRepository.parseChapterMeta(it.chaptersJson)
        }.orEmpty()

        val rows: List<OfflineChapterRow> = if (metaChapters.isNotEmpty()) {
            metaChapters.map { m ->
                val ent = dlMap[m.chapterId]
                OfflineChapterRow(
                    chapterId = m.chapterId,
                    chapterNumber = m.chapterNumber,
                    chapterName = m.name ?: ent?.chapterName,
                    pageCount = m.pagesCount ?: ent?.pageCount,
                    isDownloaded = ent != null,
                    entity = ent,
                )
            } + downloaded.filter { d -> metaChapters.none { it.chapterId == d.chapterId } }.map { d ->
                OfflineChapterRow(
                    chapterId = d.chapterId,
                    chapterNumber = d.chapterNumber,
                    chapterName = d.chapterName,
                    pageCount = d.pageCount,
                    isDownloaded = true,
                    entity = d,
                )
            }
        } else {
            downloaded.map { d ->
                OfflineChapterRow(
                    chapterId = d.chapterId,
                    chapterNumber = d.chapterNumber,
                    chapterName = d.chapterName,
                    pageCount = d.pageCount,
                    isDownloaded = true,
                    entity = d,
                )
            }
        }.sortedWith(
            compareBy(
                { it.chapterNumber.toDoubleOrNull() ?: Double.MAX_VALUE },
                { it.chapterNumber },
            ),
        )

        val readIds = readByTitle[tid] ?: emptySet()
        OfflineTitleGroup(
            titleId = tid,
            titleName = name,
            titleSlug = slug,
            titleCover = cover,
            totalChapters = meta?.totalChapters ?: rows.size,
            chapters = rows,
            bytesTotal = downloaded.sumOf { it.bytesTotal },
            lastActivityAt = downloaded.maxOfOrNull { it.downloadedAt }
                ?: meta?.lastSyncedAt
                ?: 0L,
            downloadedCount = rows.count { it.isDownloaded },
            readCount = rows.count { it.chapterId in readIds },
        )
    }.filter { it.downloadedCount > 0 || it.chapters.isNotEmpty() }
        .sortedByDescending { it.lastActivityAt }
}

@Composable
private fun OfflineTitleBlock(
    group: OfflineTitleGroup,
    expanded: Boolean,
    readChapterIds: Set<String>,
    onToggle: () -> Unit,
    onOpenChapter: (OfflineChapterRow) -> Unit,
    onDeleteChapter: (OfflineChapterRow) -> Unit,
    onDeleteTitle: () -> Unit,
    onOpenTitleOnline: () -> Unit,
) {
    val readCount = group.chapters.count { it.chapterId in readChapterIds }
    val dlCount = group.downloadedCount
    val total = group.chapters.size.coerceAtLeast(group.totalChapters)

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
                    buildString {
                        append("скачано $dlCount")
                        if (total > 0) append(" / $total")
                        append(" · ${formatBytes(group.bytesTotal)}")
                        if (readCount > 0) append(" · ✓ $readCount")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TomiloMuted,
                )
                if (total > 0 && (dlCount > 0 || readCount > 0)) {
                    val pctDl = (100 * dlCount / total).coerceIn(0, 100)
                    val pctRead = if (dlCount > 0) (100 * readCount / dlCount).coerceIn(0, 100) else 0
                    Text(
                        "Загрузка $pctDl% · прочитано из скачанного $pctRead%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onDeleteTitle) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить тайтл", tint = TomiloMuted)
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TomiloMuted,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(bottom = 6.dp)) {
                Text(
                    "Открыть тайтл онлайн →",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(start = 78.dp, end = 12.dp, bottom = 6.dp)
                        .clickable(onClick = onOpenTitleOnline),
                )
                group.chapters.forEach { ch ->
                    val isRead = ch.chapterId in readChapterIds
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenChapter(ch) }
                            .padding(start = 78.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (ch.isDownloaded) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (ch.isDownloaded) MaterialTheme.colorScheme.primary else TomiloMuted,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Глава ${ch.chapterNumber}" + when {
                                    isRead -> "  ✓"
                                    !ch.isDownloaded -> "  · не скачана"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    isRead -> MaterialTheme.colorScheme.primary
                                    !ch.isDownloaded -> TomiloMuted
                                    else -> MaterialTheme.colorScheme.onBackground
                                },
                            )
                            val sub = listOfNotNull(
                                ch.chapterName?.takeIf {
                                    it.isNotBlank() && !it.equals("Глава ${ch.chapterNumber}", true)
                                },
                                ch.pageCount?.let { "$it стр." },
                                when {
                                    isRead -> "прочитано"
                                    ch.isDownloaded -> "скачано"
                                    else -> "нажмите, чтобы скачать на странице тайтла"
                                },
                            ).joinToString(" · ")
                            if (sub.isNotBlank()) {
                                Text(sub, style = MaterialTheme.typography.bodySmall, color = TomiloMuted)
                            }
                        }
                        if (ch.isDownloaded) {
                            IconButton(onClick = { onDeleteChapter(ch) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить главу", tint = TomiloMuted)
                            }
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
