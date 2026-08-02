package ru.tomilo.lib.mobile.ui.screens.bookmarks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
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
import ru.tomilo.lib.mobile.data.api.BookmarkEntryDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.components.TitleListRow
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted

private val CATEGORIES = listOf(
    null to "Все",
    "reading" to "Читаю",
    "planned" to "В планах",
    "completed" to "Прочитано",
    "favorites" to "Избранное",
    "dropped" to "Брошено",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    onLogin: () -> Unit,
    onOpenTitle: (id: String, slug: String?) -> Unit,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    var catIndex by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<BookmarkEntryDto>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(user?.stableId(), catIndex, reload) {
        if (user == null) {
            items = emptyList()
            return@LaunchedEffect
        }
        loading = true
        error = null
        val cat = CATEGORIES[catIndex].first
        socialRepository.bookmarks(cat)
            .onSuccess { items = it }
            .onFailure { error = it.message }
        loading = false
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = { Text("Закладки") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TomiloBg),
            )
        },
    ) { padding ->
        if (user == null) {
            ErrorBox("Войдите, чтобы видеть закладки", onRetry = onLogin)
            return@Scaffold
        }
        Column(Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = catIndex,
                edgePadding = 12.dp,
                containerColor = TomiloBg,
                divider = {},
            ) {
                CATEGORIES.forEachIndexed { i, pair ->
                    FilterChip(
                        selected = catIndex == i,
                        onClick = { catIndex = i },
                        label = { Text(pair.second) },
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
            when {
                loading -> LoadingBox()
                error != null && items.isEmpty() -> ErrorBox(error ?: "Ошибка") { reload += 1 }
                items.isEmpty() -> Text(
                    "Пусто",
                    color = TomiloMuted,
                    modifier = Modifier.padding(16.dp),
                )
                else -> LazyColumn(contentPadding = ScreenPadding) {
                    items(items, key = { it.resolvedTitleId() + (it.category ?: "") }) { bm ->
                        val titleId = bm.resolvedTitleId()
                        val name = bm.title?.name ?: "Тайтл"
                        val meta = listOfNotNull(
                            categoryLabel(bm.category),
                            bm.title?.type,
                            bm.title?.totalChapters?.let { "$it гл." },
                        ).joinToString(" · ")
                        TitleListRow(
                            title = name,
                            cover = bm.title?.coverImage,
                            meta = meta,
                            onClick = {
                                if (titleId.isNotBlank()) {
                                    onOpenTitle(titleId, bm.title?.slug)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun categoryLabel(c: String?): String = when (c) {
    "reading" -> "Читаю"
    "planned" -> "В планах"
    "completed" -> "Прочитано"
    "favorites" -> "Избранное"
    "dropped" -> "Брошено"
    else -> c.orEmpty()
}
