package ru.tomilo.lib.mobile.ui.screens.title

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Home
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.ads.RewardedAdManager
import ru.tomilo.lib.mobile.BuildConfig
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
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import java.util.Locale

private enum class TitlePageTab(val label: String) {
    About("Описание"),
    Chapters("Главы"),
    Comments("Комментарии"),
}

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
    onOpenHome: () -> Unit,
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
    var minimizedDownloadBatch by remember { mutableStateOf<String?>(null) }
    var sort by remember { mutableStateOf(ChapterSort.NumberAsc) }
    var myRating by remember { mutableIntStateOf(0) }
    var readChapterIds by remember { mutableStateOf(setOf<String>()) }
    var continueChapterId by remember { mutableStateOf<String?>(null) }
    /** Главы, ждущие просмотр рекламы перед скачиванием */
    var pendingAdChapters by remember { mutableStateOf<List<ChapterDto>?>(null) }
    var adBusy by remember { mutableStateOf(false) }
    var showBookmarkCategories by remember { mutableStateOf(false) }
    var titleDetailsExpanded by remember { mutableStateOf(true) }
    var pageTab by remember { mutableStateOf(TitlePageTab.Chapters) }

    BackHandler {
        if (selectMode) {
            selectMode = false
            selected = emptySet()
        } else {
            onOpenHome()
        }
    }

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
    val downloadBatchKey = remember(downloadState.items) {
        downloadState.items.joinToString("|") { it.chapterId }
    }
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
        minimizedDownloadBatch = null
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

    LaunchedEffect(title?.stableId(), user?.stableId()) {
        val tid = title?.stableId().orEmpty()
        if (tid.isBlank() || user == null) {
            myRating = 0
            return@LaunchedEffect
        }
        historyRepository.myTitleRating(tid)
            .onSuccess { myRating = it ?: 0 }
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

    LaunchedEffect(downloadBatchKey) {
        if (downloadBatchKey.isNotBlank() && minimizedDownloadBatch != downloadBatchKey) {
            showDownloadSheet = true
        } else if (downloadBatchKey.isBlank()) {
            minimizedDownloadBatch = null
        }
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
                        } else onOpenHome()
                    }) {
                        Icon(
                            if (selectMode) Icons.Default.Close else Icons.Default.Home,
                            contentDescription = if (selectMode) "Закрыть выбор" else "На главную",
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
                        IconButton(
                            onClick = {
                                val t = title ?: return@IconButton
                                val key = t.slug?.takeIf { it.isNotBlank() } ?: t.stableId()
                                val url = "${BuildConfig.SITE_URL}/titles/$key"
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, t.name ?: "Тайтл в tomilo-lib")
                                    putExtra(Intent.EXTRA_TEXT, "${t.name ?: "Смотрите в tomilo-lib"}\n$url")
                                }
                                context.startActivity(Intent.createChooser(send, "Поделиться тайтлом"))
                            },
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Поделиться")
                        }
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
                                        showBookmarkCategories = true
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
                colors = tomiloTopBarColors(),
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
            } else if (continueChapterId != null && title != null) {
                val readId = continueChapterId!!
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(TomiloSurface2)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            onOpenChapter(
                                title!!.stableId(),
                                readId,
                                readId in downloadedIds,
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            if (readChapterIds.isEmpty()) "Читать"
                            else "Продолжить",
                            style = MaterialTheme.typography.titleMedium,
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
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), TomiloSurface2.copy(alpha = 0.78f))))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), RoundedCornerShape(24.dp))
                                .padding(16.dp),
                        ) {
                            AsyncImage(
                                model = MediaUrl.resolve(t.coverImage),
                                contentDescription = t.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(110.dp)
                                    .aspectRatio(2f / 3f)
                                    .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.35f))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(TomiloSurface2),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(t.name.orEmpty(), style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                val meta = listOfNotNull(
                                    t.type?.let(::titleTypeLabel),
                                    t.status?.let(::titleStatusLabel),
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
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            onOpenChapter(
                                                t.stableId(),
                                                continueChapterId!!,
                                                continueChapterId in downloadedIds,
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                    ) {
                                        Text(
                                            if (readChapterIds.isEmpty()) "Читать"
                                            else "Продолжить чтение",
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                    }
                                }
                            }
                        }
                        TabRow(
                            selectedTabIndex = pageTab.ordinal,
                            containerColor = Color.Transparent,
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            TitlePageTab.entries.forEach { tab ->
                                Tab(
                                    selected = pageTab == tab,
                                    onClick = { pageTab = tab },
                                    text = { Text(tab.label) },
                                )
                            }
                        }
                    }
                    if (pageTab == TitlePageTab.About) {
                        item(key = "about") {
                            TitleDetailsCard(
                                title = t,
                                chaptersCount = chapters.size,
                                readCount = readChapterIds.size,
                                offlineCount = downloadedIds.size,
                                expanded = titleDetailsExpanded,
                                onToggle = { titleDetailsExpanded = !titleDetailsExpanded },
                            )
                            TitleRatingBlock(
                                average = t.averageRating,
                                total = t.totalRatings,
                                myRating = myRating,
                                onRate = { star ->
                                    if (user == null) {
                                        onLogin()
                                        return@TitleRatingBlock
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
                            )
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                    if (pageTab == TitlePageTab.Comments) {
                        item(key = "comments") {
                            CommentsSection(
                                entityType = "title",
                                entityId = t.stableId(),
                                socialRepository = socialRepository,
                                isLoggedIn = user != null,
                                onLoginRequired = onLogin,
                                onOpenUser = onOpenUser,
                            )
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                    if (pageTab == TitlePageTab.Chapters) {
                    item(key = "chapters-head") {
                        Text(
                            "Главы",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
                        )
                        Text(
                            buildString {
                                append("${chapters.size} глав")
                                if (readChapterIds.isNotEmpty()) append(" · ${readChapterIds.size} прочитано")
                                if (downloadedIds.isNotEmpty()) append(" · ${downloadedIds.size} офлайн")
                                chapters.maxByOrNull { it.releaseDate.orEmpty() }
                                    ?.releaseDate?.let { append(" · новая ${chapterDateLabel(it)}") }
                            },
                            color = TomiloMuted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp),
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
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(17.dp))
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                                        isRead -> MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                                        else -> TomiloSurface2.copy(alpha = 0.58f)
                                    },
                                )
                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else TomiloBorder.copy(alpha = 0.52f), RoundedCornerShape(17.dp))
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
                                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                        chapter.name
                                            ?.takeIf {
                                                it.isNotBlank() &&
                                                    !it.equals("Глава ${chapter.numberLabel()}", ignoreCase = true)
                                            }
                                            ?.let { append(" · $it") }
                                        if (isRead) append("  ✓")
                                        if (paidLocked) append("  · закрыта")
                                        else if (chapter.isPaid == true && (isPremium || canOpenPaid)) {
                                            append("  · Premium")
                                        }
                                    },
                                    color = when {
                                        isRead -> MaterialTheme.colorScheme.primary
                                        paidLocked -> TomiloMuted
                                        else -> MaterialTheme.colorScheme.onBackground
                                    },
                                )
                                val accessHint = when {
                                    paidLocked -> ChapterAccess.lockHint(
                                        chapter.isPaid,
                                        chapter.freeAt,
                                        chapter.unlockPrice,
                                        isPremiumUser = false,
                                    )
                                    else -> null
                                }
                                val facts = listOfNotNull(
                                    chapter.releaseDate?.let(::chapterDateLabel),
                                    chapter.views?.toString()?.trim('"')
                                        ?.toLongOrNull()?.let(::chapterViewsLabel),
                                    (chapter.pagesCount ?: chapter.pages?.size)
                                        ?.takeIf { it > 0 }?.let { "$it стр." },
                                    chapter.status?.takeIf { it.isNotBlank() }
                                        ?.let(::chapterStatusLabel),
                                    if (isOffline) "офлайн" else null,
                                    if (isRead) "прочитано" else null,
                                ).distinct().joinToString(" · ")
                                if (facts.isNotBlank()) {
                                    Text(
                                        facts,
                                        color = if (isRead) MaterialTheme.colorScheme.primary else TomiloMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (!accessHint.isNullOrBlank()) {
                                    Text(
                                        accessHint,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelSmall,
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
                    item(key = "chapters-end") {
                        Spacer(Modifier.height(88.dp))
                    }
                    }
                    }
            }
        }
    }

    if (showDownloadSheet && downloadState.items.isNotEmpty()) {
        DownloadProgressSheet(
            state = downloadState,
            onCancel = { downloadManager.cancel() },
            onRetryFailed = { downloadManager.retryFailed() },
            onDismiss = {
                showDownloadSheet = false
                minimizedDownloadBatch = downloadBatchKey
                if (downloadState.finished) downloadManager.clear()
            },
            onContinueInBackground = {
                // закрываем sheet — сервис + уведомление продолжают качать
                minimizedDownloadBatch = downloadBatchKey
                showDownloadSheet = false
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


    if (showBookmarkCategories) {
        AlertDialog(
            onDismissRequest = { showBookmarkCategories = false },
            title = { Text("Закладка") },
            text = {
                Column {
                    listOf(
                        "reading" to "Читаю",
                        "planned" to "В планах",
                        "completed" to "Прочитано",
                        "favorites" to "Избранное",
                        "dropped" to "Брошено",
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = bookmarkCategory == value,
                            onClick = {
                                val tid = title?.stableId().orEmpty()
                                scope.launch {
                                    socialRepository.updateBookmarkCategory(tid, value)
                                        .onSuccess {
                                            bookmarkCategory = value
                                            showBookmarkCategories = false
                                            snackbar.showSnackbar("Категория: $label")
                                        }
                                        .onFailure { snackbar.showSnackbar(it.message ?: "Ошибка") }
                                }
                            },
                            label = { Text(label) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        val tid = title?.stableId().orEmpty()
                        scope.launch {
                            socialRepository.removeBookmark(tid)
                                .onSuccess {
                                    bookmarked = false
                                    bookmarkCategory = null
                                    showBookmarkCategories = false
                                    snackbar.showSnackbar("Убрано из закладок")
                                }
                                .onFailure { snackbar.showSnackbar(it.message ?: "Ошибка") }
                        }
                    },
                ) { Text("Удалить") }
            },
        )
    }
}

@Composable
private fun TitleRatingBlock(
    average: Double?,
    total: Int?,
    myRating: Int,
    onRate: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = TomiloSurface2,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TomiloBorder.copy(alpha = 0.62f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Оценка", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (total != null && total > 0) "$total оценок" else "Пока нет оценок",
                        color = TomiloMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    if (average != null && average > 0) String.format(Locale.ROOT, "%.1f", average) else "—",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TomiloPremium,
                )
                Text(
                    " / 10",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 2.dp, top = 6.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                (1..10).forEach { star ->
                    Icon(
                        imageVector = if (star <= myRating) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "$star",
                        tint = if (star <= myRating) TomiloPremium else TomiloMuted,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onRate(star) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (myRating > 0) "Ваша оценка: $myRating из 10" else "Нажмите звезду, чтобы оценить",
                color = if (myRating > 0) TomiloPremium else TomiloMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun TitleDetailsCard(
    title: TitleDetailDto,
    chaptersCount: Int,
    readCount: Int,
    offlineCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val details = listOfNotNull(
        title.author?.takeIf { it.isNotBlank() }?.let { "Автор" to it },
        title.artist?.takeIf { it.isNotBlank() }?.let { "Художник" to it },
        title.type?.takeIf { it.isNotBlank() }?.let { "Тип" to titleTypeLabel(it) },
        title.status?.takeIf { it.isNotBlank() }?.let { "Статус" to titleStatusLabel(it) },
        title.releaseYear?.let { "Год выпуска" to it.toString() },
        title.ageLimit?.let { "Возраст" to ru.tomilo.lib.mobile.core.GenreLabels.age(it) },
        title.averageRating?.let { "Рейтинг" to "★ %.1f из 10".format(it) },
        title.totalRatings?.takeIf { it > 0 }?.let { "Оценки" to "$it пользовательских" },
        title.views?.let { "Просмотры" to titleViewsLabel(it) },
        listOfNotNull(
            title.dayViews?.let { "сегодня ${compactNumber(it)}" },
            title.weekViews?.let { "за неделю ${compactNumber(it)}" },
            title.monthViews?.let { "за месяц ${compactNumber(it)}" },
        ).takeIf { it.isNotEmpty() }?.let { "Активность" to it.joinToString(" · ") },
        (title.totalChapters ?: chaptersCount.takeIf { it > 0 })
            ?.let { "Главы" to "$it опубликовано" },
        title.updatedAt?.let { "Обновлено" to titleDateLabel(it) },
        title.isPublished?.let { "Публикация" to if (it) "Опубликован" else "Скрыт" },
        title.altNames?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
            ?.let { "Другие названия" to it.joinToString(" · ") },
        title.chaptersRemovedByCopyrightHolder?.takeIf { it }
            ?.let { "Доступность" to "Часть глав удалена по требованию правообладателя" },
    )
    val hasExtendedContent = details.isNotEmpty() ||
        !title.genres.isNullOrEmpty() ||
        !title.tags.isNullOrEmpty() ||
        readCount > 0 || offlineCount > 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = TomiloSurface2.copy(alpha = 0.72f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TomiloBorder.copy(alpha = 0.62f)),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "О тайтле",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (hasExtendedContent) {
                    TextButton(onClick = onToggle) {
                        Text(if (expanded) "Скрыть" else "Подробнее")
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                        )
                    }
                }
            }

            if (!title.description.isNullOrBlank()) {
                Text(
                    title.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    "Описание пока не добавлено",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TomiloMuted,
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    if (details.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = TomiloBorder.copy(alpha = 0.55f),
                        )
                        details.forEachIndexed { index, (label, value) ->
                            TitleDetailRow(label, value)
                            if (index != details.lastIndex) Spacer(Modifier.height(10.dp))
                        }
                    }

                    title.genres?.mapNotNull { ru.tomilo.lib.mobile.core.GenreLabels.ruOrHide(it) }
                        ?.distinct()
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { genres ->
                        Spacer(Modifier.height(14.dp))
                        Text("Жанры", style = MaterialTheme.typography.labelLarge, color = TomiloMuted)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 6.dp),
                        ) {
                            genres.forEach { genre ->
                                DetailChip(genre)
                                Spacer(Modifier.width(6.dp))
                            }
                        }
                    }

                    title.tags?.mapNotNull { ru.tomilo.lib.mobile.core.GenreLabels.ruOrHide(it) }
                        ?.distinct()
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { tags ->
                        Spacer(Modifier.height(12.dp))
                        Text("Теги", style = MaterialTheme.typography.labelLarge, color = TomiloMuted)
                        Text(
                            tags.joinToString("  ·  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }

                    if (readCount > 0 || offlineCount > 0) {
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = TomiloBorder.copy(alpha = 0.55f))
                        Text(
                            listOfNotNull(
                                readCount.takeIf { it > 0 }?.let { "$it прочитано" },
                                offlineCount.takeIf { it > 0 }?.let { "$it сохранено офлайн" },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TitleDetailRow(label: String, value: String) {
    if (value.length > 52) {
        Column(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TomiloMuted)
            Spacer(Modifier.height(3.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    } else {
        Row(Modifier.fillMaxWidth()) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = TomiloMuted,
                modifier = Modifier.width(112.dp),
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DetailChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

private fun titleTypeLabel(type: String): String = ru.tomilo.lib.mobile.core.GenreLabels.type(type)

private fun titleStatusLabel(status: String): String = ru.tomilo.lib.mobile.core.GenreLabels.status(status)

private fun titleViewsLabel(views: Long): String = "${compactNumber(views)} всего"

private fun compactNumber(value: Long): String = when {
    value >= 1_000_000 -> "%.1f млн".format(value / 1_000_000.0)
    value >= 1_000 -> "%.1f тыс.".format(value / 1_000.0)
    else -> value.toString()
}

private fun titleDateLabel(raw: String): String = chapterDateLabel(raw)

private fun chapterDateLabel(raw: String): String {
    val date = raw.take(10)
    val parts = date.split('-')
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else date
}

private fun chapterViewsLabel(views: Long): String = when {
    views >= 1_000_000 -> "%.1f млн просм.".format(views / 1_000_000.0)
    views >= 1_000 -> "%.1f тыс. просм.".format(views / 1_000.0)
    else -> "$views просм."
}

private fun chapterStatusLabel(status: String): String =
    ru.tomilo.lib.mobile.core.GenreLabels.status(status).lowercase()
