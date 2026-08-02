package ru.tomilo.lib.mobile.ui.screens.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import ru.tomilo.lib.mobile.data.api.CatalogTitleDto
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.components.ScreenPadding
import ru.tomilo.lib.mobile.ui.components.SectionTitle
import ru.tomilo.lib.mobile.ui.components.TitleCoverCard
import ru.tomilo.lib.mobile.ui.theme.TomiloBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    catalogRepository: CatalogRepository,
    onOpenTitle: (id: String, slug: String?) -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var updates by remember { mutableStateOf<List<CatalogTitleDto>>(emptyList()) }
    var popular by remember { mutableStateOf<List<CatalogTitleDto>>(emptyList()) }
    var reloadToken by remember { mutableStateOf(0) }

    LaunchedEffect(reloadToken) {
        loading = true
        error = null
        val u = catalogRepository.latestUpdates()
        val p = catalogRepository.popular()
        if (u.isFailure && p.isFailure) {
            error = u.exceptionOrNull()?.message ?: "Не удалось загрузить"
            loading = false
            return@LaunchedEffect
        }
        updates = u.getOrDefault(emptyList())
        popular = p.getOrDefault(emptyList())
        loading = false
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = { Text("Tomilo") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TomiloBg),
            )
        },
    ) { padding ->
        when {
            loading -> LoadingBox(Modifier.padding(padding))
            error != null && updates.isEmpty() && popular.isEmpty() -> {
                ErrorBox(error ?: "Ошибка") {
                    reloadToken += 1
                }
            }
            else -> {
                Column(
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(ScreenPadding),
                ) {
                    if (updates.isNotEmpty()) {
                        SectionTitle("Обновления")
                        Row(
                            Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp),
                        ) {
                            updates.forEach { item ->
                                TitleCoverCard(
                                    title = item.displayTitle(),
                                    cover = item.coverPath(),
                                    subtitle = item.chapter ?: item.type,
                                    onClick = {
                                        onOpenTitle(item.stableId(), item.slug)
                                    },
                                )
                            }
                        }
                    }
                    if (popular.isNotEmpty()) {
                        SectionTitle("Популярное")
                        Row(
                            Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp),
                        ) {
                            popular.forEach { item ->
                                val rating = item.displayRating()?.let { "★ %.1f".format(it) }
                                TitleCoverCard(
                                    title = item.displayTitle(),
                                    cover = item.coverPath(),
                                    subtitle = rating ?: item.type,
                                    onClick = {
                                        onOpenTitle(item.stableId(), item.slug)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
