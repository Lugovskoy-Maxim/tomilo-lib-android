package ru.tomilo.mangalink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFFA99BFF), background = Color(0xFF10131B),
                surface = Color(0xFF191E2A), secondaryContainer = Color(0xFF34304D)
            )) { MangaLink() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MangaLink(model: CatalogModel = viewModel()) {
    var settings by rememberSaveable { mutableStateOf(false) }
    var serverInput by rememberSaveable { mutableStateOf(model.server) }
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }
    val detail = model.detail
    val reader = model.reader
    if (reader != null) {
        key(reader.chapter.id) { ReaderScreen(model, reader) }
        return
    }
    BackHandler(detail != null) { model.back() }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (detail == null) "MangaLink" else "О тайтле", fontWeight = FontWeight.Bold) },
                navigationIcon = { if (detail != null) IconButton(onClick = model::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
                actions = { IconButton(onClick = { serverInput = model.server; settings = true }) { Icon(Icons.Default.Settings, "Настройки сервера") } })
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (model.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            model.error?.let { message ->
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(onClick = { if (detail != null) model.open(detail.id) else model.search() }) { Text("Повторить") }
                    }
                }
            }
            if (detail == null) {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text("Все истории. Ваш выбор источника.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(model.query, { model.query = it }, Modifier.fillMaxWidth(),
                        placeholder = { Text("Название манги или манхвы") }, singleLine = true,
                        trailingIcon = { IconButton(onClick = { model.search() }) { Icon(Icons.Default.Search, "Найти") } })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !onlyFavorites, onClick = { onlyFavorites = false }, label = { Text("Каталог") })
                        FilterChip(selected = onlyFavorites, onClick = { onlyFavorites = true }, label = { Text("Избранное") })
                        TextButton(onClick = { model.search() }) { Text("Обновить") }
                    }
                }
                val titles = if (onlyFavorites) model.favoriteTitles.filter { it.name.contains(model.query, ignoreCase = true) } else model.titles
                LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (model.server.isBlank()) item {
                        EmptyCard("Подключите каталог", "Укажите адрес вашего сервера в настройках. Тайтлы появятся после импорта источников.")
                        Button(onClick = { settings = true }) { Text("Указать сервер") }
                    } else if (titles.isEmpty() && !model.loading) item {
                        EmptyCard(if (onlyFavorites) "Здесь пока нет избранного" else "Тайтлы не найдены", "Добавьте тайтлы через сервер или измените поисковый запрос.")
                    }
                    items(titles, key = { it.id }) { title ->
                        Card(Modifier.fillMaxWidth().clickable { model.open(title.id) }) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Cover(title.cover, Modifier.width(64.dp).height(90.dp))
                                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                                    Text(title.name, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Источников: ${title.sourceCount}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { model.toggleFavorite(title.id) }) {
                                    Icon(if (title.id in model.favorites) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "В избранное")
                                }
                            }
                        }
                    }
                    if (model.more && !onlyFavorites) item { TextButton(onClick = { model.search(append = true) }, enabled = !model.loading) { Text("Загрузить ещё") } }
                }
            } else {
                val selected = detail.sources.firstOrNull { it.id == model.sourceId }
                LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            Cover(detail.cover, Modifier.width(110.dp).height(156.dp))
                            Column(Modifier.weight(1f)) {
                                Text(detail.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(detail.genres.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = { model.toggleFavorite(detail.id) }) {
                                    Text(if (detail.id in model.favorites) "В избранном ✓" else "В избранное")
                                }
                            }
                        }
                    }
                    if (detail.description.isNotBlank()) item { Text(detail.description, style = MaterialTheme.typography.bodyMedium) }
                    item {
                        Text("Источник глав", style = MaterialTheme.typography.titleLarge)
                        Text("Страницы загружаются прямо из выбранного источника", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(detail.sources, key = { it.id }) { source ->
                                FilterChip(selected = source.id == model.sourceId, onClick = { model.selectSource(source.id) },
                                    label = { Text("${source.name} · ${source.chapters.size}") })
                            }
                        }
                    }
                    if (selected?.error != null) item { Text("Не удалось обновить источник. Показан сохранённый список глав.", color = MaterialTheme.colorScheme.error) }
                    items(selected?.chapters ?: emptyList(), key = { it.id }) { chapter ->
                        ListItem(
                            headlineContent = { Text(chapter.name) },
                            supportingContent = { Text(if (chapter.id in model.read) "Вы уже открывали эту главу" else selected?.name ?: "") },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.MenuBook, "Читать главу") },
                            modifier = Modifier.clickable {
                                model.openChapter(chapter)
                            }
                        )
                    }
                }
            }
        }
    }
    if (settings) AlertDialog(onDismissRequest = { settings = false }, title = { Text("Сервер каталога") },
        text = { Column {
            Text("Адрес API вашего MangaLink. Для эмулятора в debug-сборке: http://10.0.2.2:3100")
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(serverInput, { serverInput = it }, singleLine = true, label = { Text("Адрес сервера") })
        } },
        confirmButton = { TextButton(onClick = { model.connectServer(serverInput); settings = false }) { Text("Подключить") } },
        dismissButton = { TextButton(onClick = { settings = false }) { Text("Отмена") } })
}

@Composable
private fun Cover(url: String?, modifier: Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        if (url != null) AsyncImage(model = url, contentDescription = "Обложка", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}
@Composable
private fun EmptyCard(title: String, text: String) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } }
}
