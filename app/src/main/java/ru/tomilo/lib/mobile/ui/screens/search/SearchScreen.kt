package ru.tomilo.lib.mobile.ui.screens.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.tomilo.lib.mobile.data.api.SearchHitDto
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.components.TitleSearchCard
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    catalogRepository: CatalogRepository,
    onOpenTitle: (id: String, slug: String?) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<SearchHitDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) {
            results = emptyList()
            error = null
            loading = false
            return@LaunchedEffect
        }
        delay(280)
        loading = true
        error = null
        val res = catalogRepository.search(q)
        loading = false
        res.onSuccess { results = it }
            .onFailure { error = it.message ?: "Ошибка поиска" }
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = { Text("Поиск") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TomiloBg),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text("Название тайтла…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )
            when {
                loading -> LoadingBox()
                error != null -> Text(
                    error ?: "",
                    color = TomiloMuted,
                    modifier = Modifier.padding(16.dp),
                )
                query.trim().length < 2 -> Text(
                    "Введите минимум 2 символа",
                    color = TomiloMuted,
                    modifier = Modifier.padding(16.dp),
                )
                results.isEmpty() -> Text(
                    "Ничего не найдено",
                    color = TomiloMuted,
                    modifier = Modifier.padding(16.dp),
                )
                else -> LazyColumn(contentPadding = ScreenPadding) {
                    items(results, key = { it.id ?: it.displayTitle() }) { hit ->
                        TitleSearchCard(
                            title = hit.displayTitle(),
                            cover = hit.cover,
                            type = hit.type,
                            rating = hit.rating,
                            totalChapters = hit.totalChapters,
                            year = hit.releaseYear,
                            onClick = {
                                val id = hit.id ?: return@TitleSearchCard
                                onOpenTitle(id, hit.slug)
                            },
                        )
                    }
                }
            }
        }
    }
}
