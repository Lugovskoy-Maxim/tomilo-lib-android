package ru.tomilo.mangalink

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(model: CatalogModel, reader: Reader) {
    val initial = model.position(reader.chapter.id).coerceIn(0, reader.pages.lastIndex)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initial)
    BackHandler { model.back() }
    LaunchedEffect(reader.chapter.id) {
        snapshotFlow { state.firstVisibleItemIndex }.distinctUntilChanged().collect {
            model.savePosition(reader.chapter.id, it)
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = {
            Column {
                Text(reader.chapter.name, maxLines = 1, style = MaterialTheme.typography.titleMedium)
                Text("Страница ${state.firstVisibleItemIndex + 1} из ${reader.pages.size}", style = MaterialTheme.typography.bodySmall)
            }
        }, navigationIcon = { IconButton(onClick = model::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад к источникам") } }) },
        bottomBar = {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { model.adjacentChapter(-1) }, enabled = !model.loading && model.hasAdjacent(-1)) { Text("Предыдущая") }
                TextButton(onClick = { model.adjacentChapter(1) }, enabled = !model.loading && model.hasAdjacent(1)) { Text("Следующая") }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (model.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            model.error?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
            LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(reader.pages, key = { index, _ -> index }) { index, url ->
                    ReaderPage(url, reader.headers, index + 1)
                }
            }
        }
    }
}

@Composable
private fun ReaderPage(url: String, headers: Map<String, String>, number: Int) {
    var ratio by remember(url) { mutableFloatStateOf(0.7f) }
    var failed by remember(url) { mutableStateOf(false) }
    var loaded by remember(url) { mutableStateOf(false) }
    var attempt by remember(url) { mutableIntStateOf(0) }
    val context = LocalContext.current
    val request = remember(url, attempt) {
        ImageRequest.Builder(context).data(url).size(coil.size.Dimension.Pixels(1440), coil.size.Dimension.Undefined).apply { headers.forEach { (key, value) -> addHeader(key, value) } }.build()
    }
    Box(Modifier.fillMaxWidth().aspectRatio(ratio), contentAlignment = Alignment.Center) {
        AsyncImage(model = request, contentDescription = "Страница $number", contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxSize(),
            onSuccess = {
                val drawable = it.result.drawable
                if (drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0)
                    ratio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
                loaded = true; failed = false
            },
            onError = { failed = true })
        if (failed) Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Не удалось загрузить страницу $number")
            TextButton(onClick = { failed = false; attempt++ }) { Text("Повторить") }
        } else if (!loaded) CircularProgressIndicator()
    }
}
