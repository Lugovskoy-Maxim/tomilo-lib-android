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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import android.app.Activity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.ads.RewardedAdManager
import ru.tomilo.lib.mobile.core.ChapterAccess
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.data.api.ChapterDto
import ru.tomilo.lib.mobile.data.api.TitleDetailDto
import ru.tomilo.lib.mobile.data.download.DownloadManager
import ru.tomilo.lib.mobile.data.local.AdRewardStore
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.data.repo.HistoryRepository
import ru.tomilo.lib.mobile.data.repo.OfflineRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.CommentsSection
import ru.tomilo.lib.mobile.ui.components.DownloadProgressSheet
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

private enum class ChapterSort(val label: String) {
    NumberAsc("№ ↑"),
    NumberDesc("№ ↓"),
    DateNew("Новые"),
    DateOld("Старые"),
    Views("Просмотры"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleScreen(
    titleKey: String,
    catalogRepository: CatalogRepository,
    offlineRepository: OfflineRepository,
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    historyRepository: HistoryRepository,
    downloadManager: DownloadManager,
    rewardedAdManager: RewardedAdManager,
    adRewardStore: AdRewardStore,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenChapter: (titleId: String, chapterId: String, offline: Boolean) -> Unit,
    onOpenUser: (userId: String) -> Unit,
    onOpenPremium: () -> Unit = {},
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    val offlineCredits by adRewardStore.offlineCreditsFlow.collectAsState(initial = 0)
    val isPremium = Premium.isActive(user?.subscriptionExpiresAt)
    val context = LocalContext.current
    val activity = context as? Activity
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf<TitleDetailDto?>(null) }
    var chapters by remember { mutableStateOf<List<ChapterDto>>(emptyList()) }
    var bookmarked by remember { mutableStateOf(false) }
    var bookmarkCategory by remember { mutableStateOf<String?>(null) }
    var selectMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var showDownloadSheet by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(ChapterSort.NumberAsc) }
    var myRating by remember { mutableIntStateOf(0) }
    var readChapterIds by remember { mutableStateOf(setOf<String>()) }
    var continueChapterId by remember { mutableStateOf<String?>(null) }
    /** Главы, ждущие просмотр рекламы перед скачиванием */
    var pendingAdChapters by remember { mutableStateOf<List<ChapterDto>?>(null) }
    var adBusy by remember { mutableStateOf(false) }

    val sortedChapters = remember(chapters, sort) {
        when (sort) {
            ChapterSort.NumberAsc -> chapters.sortedBy { it.chapterNumberAsDouble() ?: Double.MAX_VALUE }
            ChapterSort.NumberDesc -> chapters.sortedByDescending { it.chapterNumberAsDouble() ?: -1.0 }
            ChapterSort.DateNew -> chapters.sortedByDescending { it.releaseDate.orEmpty() }
            ChapterSort.DateOld -> chapters.sortedBy { it.releaseDate.orEmpty() }
            ChapterSort.Views -> chapters.sortedByDescending {
                it.views?.toString()?.trim('"')?.toDoubleOrNull() ?: 0.0
            }
        }
    }

    val offlineAll by offlineRepository.observeAll().collectAsState(initial = emptyList())
    val downloadState by downloadManager.state.collectAsState()
    val downloadedIds = remember(offlineAll, title?.stableId()) {
        val tid = title?.stableId().orEmpty()
        offlineAll.filter { tid.isNotBlank() && it.titleId == tid }.map { it.chapterId }.toSet()
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun startDownload(chaptersToDl: List<ChapterDto>) {
        val t = title ?: return
        if (chaptersToDl.isEmpty()) return
        downloadManager.enqueue(
            titleId = t.stableId(),
            titleName = t.name.orEmpty(),
            titleSlug = t.slug.orEmpty(),
            titleCover = t.coverImage,
            chapters = chaptersToDl,
        )
        showDownloadSheet = true
        selectMode = false
        selected = emptySet()
    }

    /**
     * Premium — сразу. Иначе кредиты с рекламы: хватает → скачать;
     * не хватает → диалог «смотреть рекламу» (1 Reward = 1 глава).
     */
    fun requestDownload(chaptersToDl: List<ChapterDto>) {
        if (user == null) {
            onLogin()
            return
        }
        if (chaptersToDl.isEmpty()) {
            scope.launch { snackbar.showSnackbar("Нечего скачивать") }
            return
        }
        if (isPremium) {
            startDownload(chaptersToDl)
            return
        }
        if (offlineCredits >= chaptersToDl.size) {
            startDownload(chaptersToDl)
            return
        }
        // Одна глава без кредитов — предложить рекламу; несколько — скачать сколько есть или ad+1
        if (chaptersToDl.size == 1 && offlineCredits == 0) {
            pendingAdChapters = chaptersToDl
            return
        }
        if (offlineCredits > 0) {
            startDownload(chaptersToDl.take(offlineCredits))
            scope.launch {
                snackbar.showSnackbar(
                    "Без Premium: скачано ${minOf(offlineCredits, chaptersToDl.size)} из ${chaptersToDl.size} " +
                        "(кредиты за рекламу). Остальное — Premium или ещё реклама.",
                )
            }
            return
        }
        pendingAdChapters = chaptersToDl.take(1)
    }

    fun showRewardedForPending() {
        val pending = pendingAdChapters ?: return
        val act = activity
        if (act == null) {
            scope.launch { snackbar.showSnackbar("Не удалось открыть рекламу") }
            pendingAdChapters = null
            return
        }
        adBusy = true
        rewardedAdManager.show(
            activity = act,
            onRewarded = { amount, _ ->
                scope.launch {
                    val granted = amount.coerceAtLeast(1)
                    adRewardStore.addOfflineCredits(granted)
                    snackbar.showSnackbar("Награда: +$granted офлайн-глава")
                    // кредит начислен — OfflineRepository спишет при скачивании
                    startDownload(pending.take(granted.coerceAtLeast(1)))
                    pendingAdChapters = null
                    adBusy = false
                }
            },
            onFailed = { msg ->
                scope.launch {
                    snackbar.showSnackbar(msg)
                    adBusy = false
                }
            },
            onDismissed = {
                adBusy = false
            },
        )
    }

    LaunchedEffect(Unit) {
        rewardedAdManager.preload()
    }

    LaunchedEffect(titleKey) {
        loading = true
        error = null
        selectMode = false
        selected = emptySet()
        val t = catalogRepository.title(titleKey)
        t.onFailure {
            error = it.message
            loading = false
            return@LaunchedEffect
        }
        val detail = t.getOrThrow()
        title = detail
        catalogRepository.chaptersAll(detail.stableId())
            .onSuccess { chapters = it }
            .onFailure { error = it.message }
        loading = false
    }

    LaunchedEffect(title?.stableId(), user?.stableId()) {
        val tid = title?.stableId().orEmpty()
        if (tid.isBlank() || user == null) {
            bookmarked = false
            bookmarkCategory = null
            return@LaunchedEffect
        }
        socialRepository.bookmarkStatus(tid)
            .onSuccess {
                bookmarked = it.active()
                bookmarkCategory = it.category
            }
    }

    LaunchedEffect(title?.stableId(), user?.stableId(), chapters) {
        val tid = title?.stableId().orEmpty()
        if (tid.isBlank() || user == null) {
            readChapterIds = emptySet()
            continueChapterId = chapters.firstOrNull()?.stableId()
            return@LaunchedEffect
        }
        historyRepository.readIds(tid)
            .onSuccess { ids ->
                readChapterIds = ids
                val byNum = chapters.sortedBy { it.chapterNumberAsDouble() ?: -1.0 }
                val lastRead = byNum.lastOrNull { it.stableId() in ids }
                val nextUnread = byNum.firstOrNull {
                    val n = it.chapterNumberAsDouble()
                    val lastN = lastRead?.chapterNumberAsDouble()
                    n != null && lastN != null && n > lastN
                }
                continueChapterId = nextUnread?.stableId()
                    ?: lastRead?.stableId()
                    ?: byNum.firstOrNull()?.stableId()
            }
            .onFailure {
                continueChapterId = chapters.minByOrNull { it.chapterNumberAsDouble() ?: 0.0 }?.stableId()
            }
    }

    LaunchedEffect(downloadState.finished, downloadState.items) {
        if (downloadState.items.isNotEmpty()) showDownloadSheet = true
    }

    Scaffold(
        containerColor = TomiloBg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectMode) "Выбрано: ${selected.size}"
                        else title?.name ?: "Тайтл",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectMode) {
                            selectMode = false
                            selected = emptySet()
                        } else onBack()
                    }) {
                        Icon(
                            if (selectMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                        )
                    }
                },
                actions = {
                    if (selectMode) {
                        IconButton(onClick = {
                            val allIds = sortedChapters.map { it.stableId() }.filter { it !in downloadedIds }
                            selected = if (selected.size >= allIds.size) emptySet() else allIds.toSet()
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Выбрать все")
                        }
                    } else {
                        IconButton(onClick = { selectMode = true }) {
                            Icon(Icons.Default.CheckBoxOutlineBlank, contentDescription = "Выбор загрузки")
                        }
                        IconButton(
                            onClick = {
                                val tid = title?.stableId().orEmpty()
                                if (tid.isBlank()) return@IconButton
                                if (user == null) {
                                    onLogin()
                                    return@IconButton
                                }
                                scope.launch {
                                    if (bookmarked) {
                                        socialRepository.removeBookmark(tid)
                                            .onSuccess {
                                                bookmarked = false
                                                bookmarkCategory = null
                                                snackbar.showSnackbar("Убрано из закладок")
                                            }
                                    } else {
                                        socialRepository.addBookmark(tid, "reading")
                                            .onSuccess {
                                                bookmarked = true
                                                bookmarkCategory = "reading"
                                                snackbar.showSnackbar("В закладках")
                                            }
                                    }
                                }
                            },
                        ) {
                            Icon(
                                if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Закладка",
                                tint = if (bookmarked) MaterialTheme.colorScheme.primary else TomiloMuted,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TomiloBg),
            )
        },
        bottomBar = {
            if (selectMode) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(TomiloSurface2)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = {
                        selectMode = false
                        selected = emptySet()
                    }, modifier = Modifier.weight(1f)) { Text("Отмена") }
                    Button(
                        onClick = {
                            val toDownload = sortedChapters.filter {
                                it.stableId() in selected && it.stableId() !in downloadedIds
                            }
                            requestDownload(toDownload)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selected.isNotEmpty() && !downloadManager.isBusy() && !adBusy,
                    ) {
                        Text(
                            when {
                                isPremium -> "Скачать (${selected.size})"
                                offlineCredits > 0 -> "Скачать (${selected.size}) · $offlineCredits кр."
                                else -> "Скачать / реклама"
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        when {
            loading -> LoadingBox(Modifier.padding(padding))
            error != null && title == null -> ErrorBox(error ?: "Ошибка")
            title != null -> {
                val t = title!!
                LazyColumn(
                    Modifier.padding(padding).fillMaxSize(),
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
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    when {
                                        isPremium -> "□ — выбор глав для офлайн (Premium)"
                                        offlineCredits > 0 ->
                                            "□ — офлайн: $offlineCredits кредит(ов) за рекламу"
                                        else ->
                                            "□ — офлайн: Premium или реклама (+1 глава)"
                                    },
                                    color = TomiloMuted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                if (user != null && readChapterIds.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Прочитано: ${readChapterIds.size}" +
                                            (t.totalChapters?.let { " / $it" } ?: ""),
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                if (continueChapterId != null) {
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            onOpenChapter(
                                                t.stableId(),
                                                continueChapterId!!,
                                                continueChapterId in downloadedIds,
                                            )
                                        },
                                    ) {
                                        Text(
                                            if (readChapterIds.isEmpty()) "Начать чтение"
                                            else "Продолжить",
                                        )
                                    }
                                }
                            }
                        }
                        if (!t.description.isNullOrBlank()) {
                            Text(
                                t.description!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TomiloMuted,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                        // Rating
                        Text(
                            "Оценка",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        Row(
                            Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp),
                        ) {
                            (1..10).forEach { star ->
                                FilterChip(
                                    selected = myRating == star,
                                    onClick = {
                                        if (user == null) {
                                            onLogin()
                                            return@FilterChip
                                        }
                                        scope.launch {
                                            historyRepository.rateTitle(t.stableId(), star)
                                                .onSuccess {
                                                    myRating = star
                                                    snackbar.showSnackbar("Оценка: $star/10")
                                                }
                                                .onFailure {
                                                    snackbar.showSnackbar(it.message ?: "Ошибка")
                                                }
                                        }
                                    },
                                    label = { Text("$star") },
                                    modifier = Modifier.padding(horizontal = 2.dp),
                                )
                            }
                        }
                        Text(
                            "Главы · сортировка",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                        Row(
                            Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            ChapterSort.entries.forEach { option ->
                                FilterChip(
                                    selected = sort == option,
                                    onClick = { sort = option },
                                    label = { Text(option.label) },
                                    modifier = Modifier.padding(horizontal = 3.dp),
                                )
                            }
                        }
                    }
                    items(sortedChapters, key = { it.stableId() }) { chapter ->
                        val id = chapter.stableId()
                        val isOffline = id in downloadedIds
                        val isSelected = id in selected
                        val isRead = id in readChapterIds
                        val paidLocked = ChapterAccess.isPremiumOnly(
                            chapter.isPaid,
                            chapter.freeAt,
                            chapter.isUnlockedByActivityCoins,
                        ) && !isPremium && !isOffline
                        val canOpenPaid = ChapterAccess.userCanRead(
                            isPaid = chapter.isPaid,
                            freeAt = chapter.freeAt,
                            unlockedByActivityCoins = chapter.isUnlockedByActivityCoins,
                            subscriptionExpiresAt = user?.subscriptionExpiresAt,
                        ) || isOffline || isPremium
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectMode) {
                                        if (isOffline) return@clickable
                                        selected = if (isSelected) selected - id else selected + id
                                    } else {
                                        // Premium и офлайн — открываем; иначе тоже открываем
                                        // (ридер покажет экран Premium, если страниц нет)
                                        onOpenChapter(t.stableId(), id, isOffline)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (selectMode) {
                                Checkbox(
                                    checked = isSelected || isOffline,
                                    onCheckedChange = {
                                        if (isOffline) return@Checkbox
                                        selected = if (isSelected) selected - id else selected + id
                                    },
                                    enabled = !isOffline,
                                )
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                                Text(
                                    buildString {
                                        append("Глава ${chapter.numberLabel()}")
                                        if (isRead) append("  ✓")
                                        if (paidLocked) append("  🔒")
                                        else if (chapter.isPaid == true && (isPremium || canOpenPaid)) {
                                            append("  ★")
                                        }
                                    },
                                    color = when {
                                        isRead -> MaterialTheme.colorScheme.primary
                                        paidLocked -> TomiloMuted
                                        else -> MaterialTheme.colorScheme.onBackground
                                    },
                                )
                                val subHint = when {
                                    paidLocked -> ChapterAccess.lockHint(
                                        chapter.isPaid,
                                        chapter.freeAt,
                                        chapter.unlockPrice,
                                        isPremiumUser = false,
                                    )
                                    !chapter.name.isNullOrBlank() &&
                                        chapter.name != "Глава ${chapter.numberLabel()}" -> chapter.name
                                    isRead -> "Прочитано"
                                    chapter.isPaid == true && isPremium -> "Premium · доступна"
                                    else -> null
                                }
                                if (!subHint.isNullOrBlank()) {
                                    Text(
                                        subHint,
                                        color = if (paidLocked) MaterialTheme.colorScheme.primary
                                        else if (isRead) MaterialTheme.colorScheme.primary
                                        else TomiloMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                    )
                                }
                            }
                            if (!selectMode) {
                                IconButton(
                                    onClick = {
                                        if (isOffline) {
                                            scope.launch {
                                                offlineRepository.deleteChapter(id)
                                                snackbar.showSnackbar("Удалено из офлайн")
                                            }
                                            return@IconButton
                                        }
                                        requestDownload(listOf(chapter))
                                    },
                                ) {
                                    Icon(
                                        if (isOffline) Icons.Default.DownloadDone else Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = if (isOffline) MaterialTheme.colorScheme.primary else TomiloMuted,
                                    )
                                }
                            } else if (isOffline) {
                                Icon(
                                    Icons.Default.DownloadDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp).padding(end = 8.dp),
                                )
                            }
                        }
                    }
                    item {
                        CommentsSection(
                            entityType = "title",
                            entityId = t.stableId(),
                            socialRepository = socialRepository,
                            isLoggedIn = user != null,
                            onLoginRequired = onLogin,
                            onOpenUser = onOpenUser,
                        )
                        Spacer(Modifier.height(100.dp))
                    }
                }
            }
        }
    }

    if (showDownloadSheet && downloadState.items.isNotEmpty()) {
        DownloadProgressSheet(
            state = downloadState,
            onCancel = { downloadManager.cancel() },
            onDismiss = {
                showDownloadSheet = false
                downloadManager.clear()
            },
        )
    }

    pendingAdChapters?.let { pending ->
        AlertDialog(
            onDismissRequest = { if (!adBusy) pendingAdChapters = null },
            title = { Text("Офлайн без Premium") },
            text = {
                Text(
                    "Скачать главу ${pending.firstOrNull()?.numberLabel() ?: ""} можно после " +
                        "просмотра рекламы (+1 кредит) или с подпиской Premium (безлимит).",
                )
            },
            confirmButton = {
                Button(
                    onClick = { showRewardedForPending() },
                    enabled = !adBusy,
                ) {
                    Text(if (adBusy) "Загрузка…" else "Смотреть рекламу")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!adBusy) {
                            pendingAdChapters = null
                            onOpenPremium()
                        }
                    },
                    enabled = !adBusy,
                ) { Text("Premium") }
            },
        )
    }
}
