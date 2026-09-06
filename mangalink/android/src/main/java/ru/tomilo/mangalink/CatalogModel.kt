package ru.tomilo.mangalink

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class Title(val id: String, val name: String, val cover: String?, val sourceCount: Int)
data class Chapter(val id: String, val name: String, val url: String)
data class Source(val id: String, val name: String, val error: String?, val chapters: List<Chapter>)
data class Reader(val chapter: Chapter, val pages: List<String>, val headers: Map<String, String>)
data class Detail(val id: String, val name: String, val description: String, val cover: String?, val genres: List<String>, val sources: List<Source>)

class CatalogModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("catalog", 0)
    private val client = OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS).build()
    var server by mutableStateOf(prefs.getString("server", "") ?: ""); private set
    var query by mutableStateOf("")
    var titles by mutableStateOf<List<Title>>(emptyList()); private set
    var reader by mutableStateOf<Reader?>(null); private set
    var detail by mutableStateOf<Detail?>(null); private set
    var sourceId by mutableStateOf(""); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var more by mutableStateOf(false); private set
    var favorites by mutableStateOf(prefs.getStringSet("favorites", emptySet())!!.toSet()); private set
    var read by mutableStateOf(prefs.getStringSet("read", emptySet())!!.toSet()); private set
    var favoriteTitles by mutableStateOf<List<Title>>(loadFavorites()); private set
    private var loadedQuery = ""
    private var job: Job? = null
    private fun loadFavorites(): List<Title> = try {
        val a = JSONArray(prefs.getString("favoriteTitles", "[]"))
        (0 until a.length()).map { i -> a.getJSONObject(i).let {
            Title(it.getString("id"), it.getString("name"), it.nullable("coverUrl"), it.getInt("sourceCount"))
        } }
    } catch (_: Exception) { emptyList() }
    init { if (server.isNotBlank()) search() }
    private suspend fun get(path: String, params: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val url = (server.trimEnd('/') + path).toHttpUrl().newBuilder().apply {
            params.forEach { (k, v) -> addQueryParameter(k, v) }
        }.build()
        client.newCall(Request.Builder().url(url).build()).execute().use {
            if (!it.isSuccessful) error("Сервер вернул ошибку ${it.code}")
            it.body?.string() ?: error("Пустой ответ сервера")
        }
    }
    fun connectServer(value: String) {
        try {
            val url = value.trim().trimEnd('/').toHttpUrl()
            require(url.username.isEmpty() && url.password.isEmpty() && url.encodedPath == "/" && url.query == null && url.fragment == null)
            server = url.toString().trimEnd('/')
            prefs.edit().putString("server", server).apply()
            reader = null
            detail = null
            titles = emptyList()
            search()
        } catch (_: Exception) { error = "Укажите адрес сервера, например https://catalog.example.org" }
    }
    private fun runTask(block: suspend () -> Unit) {
        job?.cancel()
        job = viewModelScope.launch {
            loading = true; error = null
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "Не удалось связаться с сервером" }
            finally { loading = false }
        }
    }
    fun search(append: Boolean = false) {
        if (server.isBlank()) return
        val requestedQuery = if (append) loadedQuery else query
        runTask {
            val result = JSONArray(get("/api/titles", mapOf("q" to requestedQuery, "offset" to if (append) titles.size.toString() else "0")))
            val rows = (0 until result.length()).map { i -> result.getJSONObject(i).let {
                Title(it.getString("id"), it.getString("name"), it.nullable("coverUrl"), it.getInt("sourceCount"))
            } }
            titles = if (append) (titles + rows).distinctBy { it.id } else rows
            loadedQuery = requestedQuery
            more = rows.size == 30
        }
    }
    fun open(id: String) = runTask {
        val obj = JSONObject(get("/api/titles/$id"))
        val sources = obj.getJSONArray("sources")
        detail = Detail(obj.getString("id"), obj.getString("name"), obj.getString("description"), obj.nullable("coverUrl"),
            obj.getJSONArray("genres").let { a -> (0 until a.length()).map { a.getString(it) } },
            (0 until sources.length()).map { i -> sources.getJSONObject(i).let { s ->
                val chapters = s.getJSONArray("chapters")
                Source(s.getString("id"), s.getString("name"), s.nullable("lastError"),
                    (0 until chapters.length()).map { j -> chapters.getJSONObject(j).let {
                        Chapter(it.getString("id"), it.getString("name"), it.getString("url"))
                    } })
            } })
        sourceId = prefs.getString("source.$id", "") ?: ""
        if (detail!!.sources.none { it.id == sourceId }) sourceId = detail!!.sources.firstOrNull()?.id ?: ""
    }
    fun openChapter(chapter: Chapter) = runTask {
        val obj = JSONObject(get("/api/chapters/${chapter.id}/pages"))
        val pages = obj.getJSONArray("pages")
        val headers = obj.getJSONObject("headers")
        require(pages.length() > 0) { "Источник не вернул страницы" }
        reader = Reader(chapter, (0 until pages.length()).map { pages.getString(it) },
            headers.keys().asSequence().associateWith { headers.getString(it) })
        markRead(chapter.id)
    }
    fun adjacentChapter(delta: Int) {
        val chapters = detail?.sources?.find { it.id == sourceId }?.chapters ?: return
        val index = chapters.indexOfFirst { it.id == reader?.chapter?.id }
        chapters.getOrNull(index + delta)?.let { openChapter(it) }
    }
    fun hasAdjacent(delta: Int): Boolean {
        val chapters = detail?.sources?.find { it.id == sourceId }?.chapters ?: return false
        val index = chapters.indexOfFirst { it.id == reader?.chapter?.id }
        return index >= 0 && chapters.getOrNull(index + delta) != null
    }
    fun position(id: String) = prefs.getInt("position.$id", 0)
    fun savePosition(id: String, page: Int) { prefs.edit().putInt("position.$id", page).apply() }
    fun back() { job?.cancel(); loading = false; if (reader != null) reader = null else detail = null; error = null }
    fun selectSource(id: String) { sourceId = id; prefs.edit().putString("source.${detail?.id}", id).apply() }
    fun toggleFavorite(id: String) {
        favorites = if (id in favorites) favorites - id else favorites + id
        favoriteTitles = if (id !in favorites) favoriteTitles.filter { it.id != id } else {
            val title = titles.find { it.id == id } ?: detail?.takeIf { it.id == id }?.let { Title(it.id, it.name, it.cover, it.sources.size) }
            if (title != null) favoriteTitles.filter { it.id != id } + title else favoriteTitles
        }
        val saved = JSONArray()
        favoriteTitles.forEach { saved.put(JSONObject().put("id", it.id).put("name", it.name).put("coverUrl", it.cover).put("sourceCount", it.sourceCount)) }
        prefs.edit().putStringSet("favorites", favorites).putString("favoriteTitles", saved.toString()).apply()
    }
    fun markRead(id: String) { read = read + id; prefs.edit().putStringSet("read", read).apply() }
    fun showError(message: String) { error = message }
}
private fun JSONObject.nullable(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
