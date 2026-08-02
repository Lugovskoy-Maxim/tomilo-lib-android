package ru.tomilo.lib.mobile.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.data.repo.CatalogRepository
import ru.tomilo.lib.mobile.data.repo.OfflineRepository
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    chapterId: String,
    preferOffline: Boolean,
    catalogRepository: CatalogRepository,
    offlineRepository: OfflineRepository,
    onBack: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var pages by remember { mutableStateOf<List<String>>(emptyList()) }
    var title by remember { mutableStateOf("Глава") }
    var offline by remember { mutableStateOf(false) }

    LaunchedEffect(chapterId, preferOffline) {
        loading = true
        error = null
        if (preferOffline) {
            val local = offlineRepository.getLocalPages(chapterId)
            if (!local.isNullOrEmpty()) {
                pages = local.map { File(it).toURI().toString() }
                offline = true
                val meta = offlineRepository.observeAll() // not ideal; fetch entity
                title = "Глава (офлайн)"
                loading = false
                return@LaunchedEffect
            }
        }
        val localFallback = offlineRepository.getLocalPages(chapterId)
        if (!localFallback.isNullOrEmpty()) {
            pages = localFallback.map { File(it).toURI().toString() }
            offline = true
            title = "Глава (офлайн)"
            loading = false
            return@LaunchedEffect
        }
        val res = catalogRepository.chapter(chapterId)
        res.onSuccess { ch ->
            title = ch.name?.ifBlank { "Глава ${ch.numberLabel()}" } ?: "Глава ${ch.numberLabel()}"
            pages = ch.pages.orEmpty().map { MediaUrl.resolve(it) }
            offline = false
            if (pages.isEmpty()) error = "Страницы недоступны"
        }.onFailure {
            error = it.message ?: "Не удалось открыть главу"
        }
        loading = false
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text(if (offline) "$title · offline" else title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.85f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        when {
            loading -> LoadingBox(Modifier.padding(padding))
            error != null -> Box(Modifier.padding(padding)) { ErrorBox(error ?: "Ошибка") }
            else -> LazyColumn(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                itemsIndexed(pages) { index, page ->
                    AsyncImage(
                        model = page,
                        contentDescription = "Стр. ${index + 1}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Страниц: ${pages.size}",
                            color = Color.Gray,
                        )
                    }
                }
            }
        }
    }
}
