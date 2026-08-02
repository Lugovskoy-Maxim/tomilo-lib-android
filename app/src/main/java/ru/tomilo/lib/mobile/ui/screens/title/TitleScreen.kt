package ru.tomilo.lib.mobile.ui.screens.title

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.api.ChapterDto
import ru.tomilo.lib.mobile.data.api.TitleDetailDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.data.repo.OfflineRepository
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleScreen(
    titleKey: String,
    catalogRepository: CatalogRepository,
    offlineRepository: OfflineRepository,
    authRepository: AuthRepository,
    onBack: () -> Unit,
    onOpenChapter: (titleId: String, chapterId: String, offline: Boolean) -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf<TitleDetailDto?>(null) }
    var chapters by remember { mutableStateOf<List<ChapterDto>>(emptyList()) }
    val offlineAll by offlineRepository.observeAll().collectAsState(initial = emptyList())
    val downloadedIds = remember(offlineAll, title?.stableId()) {
        val tid = title?.stableId().orEmpty()
        offlineAll.filter { tid.isNotBlank() && it.titleId == tid }.map { it.chapterId }.toSet()
    }
    val downloading = remember { mutableStateMapOf<String, Boolean>() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(titleKey) {
        loading = true
        error = null
        val t = catalogRepository.title(titleKey)
        t.onFailure {
            error = it.message
            loading = false
            return@LaunchedEffect
        }
        val detail = t.getOrThrow()
        title = detail
        val ch = catalogRepository.chapters(detail.stableId())
        ch.onSuccess { chapters = it }
            .onFailure { error = it.message }
        loading = false
    }

    Scaffold(
        containerColor = TomiloBg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title?.name ?: "Тайтл",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TomiloBg),
            )
        },
    ) { padding ->
        when {
            loading -> LoadingBox(Modifier.padding(padding))
            error != null && title == null -> ErrorBox(error ?: "Ошибка", onRetry = null)
            title != null -> {
                val t = title!!
                LazyColumn(
                    Modifier
                        .padding(padding)
                        .fillMaxSize(),
                ) {
                    item {
                        Row(Modifier.padding(16.dp)) {
                            AsyncImage(
                                model = MediaUrl.resolve(t.coverImage),
                                contentDescription = t.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(110.dp)
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(TomiloSurface2),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(t.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(6.dp))
                                val meta = listOfNotNull(
                                    t.type,
                                    t.status,
                                    t.releaseYear?.toString(),
                                    t.averageRating?.let { "★ %.1f".format(it) },
                                    t.totalChapters?.let { "$it гл." },
                                ).joinToString(" · ")
                                Text(meta, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                                if (!t.author.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("Автор: ${t.author}", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        if (!t.description.isNullOrBlank()) {
                            Text(
                                t.description!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TomiloMuted,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(
                            "Главы",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(chapters, key = { it.stableId() }) { chapter ->
                        val id = chapter.stableId()
                        val isOffline = id in downloadedIds
                        val isLoading = downloading[id] == true
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpenChapter(t.stableId(), id, isOffline)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Глава ${chapter.numberLabel()}")
                                if (!chapter.name.isNullOrBlank() && chapter.name != "Глава ${chapter.numberLabel()}") {
                                    Text(chapter.name!!, color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (isLoading) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            if (isOffline) {
                                                offlineRepository.deleteChapter(id)
                                                snackbar.showSnackbar("Удалено из офлайн")
                                                return@launch
                                            }
                                            downloading[id] = true
                                            val result = offlineRepository.downloadChapter(
                                                titleId = t.stableId(),
                                                titleName = t.name.orEmpty(),
                                                titleSlug = t.slug.orEmpty(),
                                                titleCover = t.coverImage,
                                                chapterId = id,
                                            )
                                            downloading[id] = false
                                            result
                                                .onSuccess { snackbar.showSnackbar("Скачано для офлайн") }
                                                .onFailure {
                                                    snackbar.showSnackbar(it.message ?: "Ошибка скачивания")
                                                }
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = if (isOffline) Icons.Default.DownloadDone else Icons.Default.CloudDownload,
                                        contentDescription = if (isOffline) "Удалить офлайн" else "Скачать",
                                        tint = if (isOffline) MaterialTheme.colorScheme.primary else TomiloMuted,
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
