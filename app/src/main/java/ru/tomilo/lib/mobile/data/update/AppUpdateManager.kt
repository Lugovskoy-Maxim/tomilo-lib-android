package ru.tomilo.lib.mobile.data.update

import android.content.Intent
import android.net.Uri
import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.tomilo.lib.mobile.BuildConfig
import ru.tomilo.lib.mobile.data.api.NetworkModule
import java.util.concurrent.TimeUnit

@Serializable
data class GithubReleaseDto(
    @SerialName("tag_name") val tagName: String? = null,
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    val assets: List<GithubAssetDto> = emptyList(),
)

@Serializable
data class GithubAssetDto(
    val name: String? = null,
    @SerialName("browser_download_url") val downloadUrl: String? = null,
    val size: Long = 0,
)

data class AppRelease(
    val versionName: String,
    val versionCode: Int?,
    val notes: String,
    val htmlUrl: String,
    val apkName: String,
    val apkUrl: String,
    val apkSize: Long,
)

class AppUpdateManager {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val repo = BuildConfig.GITHUB_REPO

    suspend fun fetchLatest(): Result<AppRelease> = withContext(Dispatchers.IO) {
        runCatching { fetchLatestFromApi() }
            .recoverCatching { fetchLatestFromGithubWeb() }
    }

    fun isNewer(release: AppRelease): Boolean {
        val localName = BuildConfig.VERSION_NAME.substringBefore('-')
        val remoteCode = release.versionCode
        if (remoteCode != null) return remoteCode > BuildConfig.VERSION_CODE
        return compareSemver(release.versionName, localName) > 0
    }

    fun openReleaseIntent(url: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(url))

    private fun pickApk(assets: List<GithubAssetDto>): GithubAssetDto? {
        val apks = assets.filter { asset ->
            val name = asset.name.orEmpty().lowercase()
            name.endsWith(".apk") &&
                !name.endsWith(".apk.idsig") &&
                !name.contains("debug") &&
                !asset.downloadUrl.isNullOrBlank()
        }
        return apks.firstOrNull { it.name.orEmpty().contains("tomilo-lib", ignoreCase = true) }
            ?: apks.firstOrNull()
    }

    private fun fetchLatestFromApi(): AppRelease {
        val request = githubRequest("https://api.github.com/repos/$repo/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error(githubError(response.code))
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) error("Пустой ответ GitHub")
            val release = NetworkModule.json.decodeFromString(GithubReleaseDto.serializer(), body)
            val asset = pickApk(release.assets) ?: error("В релизе нет APK")
            val versionName = release.tagName.orEmpty().removePrefix("v").ifBlank {
                release.name?.substringAfterLast(' ') ?: "0"
            }
            return AppRelease(
                versionName = versionName,
                versionCode = parseVersionCode(release.body.orEmpty()),
                notes = release.body.orEmpty().trim().ifBlank { "Новая версия $versionName" },
                htmlUrl = release.htmlUrl.orEmpty()
                    .ifBlank { "https://github.com/$repo/releases/latest" },
                apkName = asset.name.orEmpty(),
                apkUrl = asset.downloadUrl.orEmpty(),
                apkSize = asset.size,
            ).also { if (it.apkUrl.isBlank()) error("Нет ссылки на APK") }
        }
    }

    /**
     * GitHub REST имеет небольшой анонимный rate limit. Официальные release redirect,
     * Atom feed и expanded-assets остаются доступны и дают тот же latest release без токена.
     */
    private fun fetchLatestFromGithubWeb(): AppRelease {
        val latestUrl = "https://github.com/$repo/releases/latest"
        val (tag, htmlUrl) = client.newCall(githubRequest(latestUrl).build()).execute().use { response ->
            if (!response.isSuccessful) error("GitHub Releases ответил ${response.code}")
            val finalUrl = response.request.url.toString()
            val resolvedTag = response.request.url.pathSegments.lastOrNull().orEmpty()
            if (resolvedTag.isBlank() || "/tag/" !in finalUrl) error("GitHub не вернул latest tag")
            resolvedTag to finalUrl
        }

        val feed = client.newCall(
            githubRequest("https://github.com/$repo/releases.atom").build(),
        ).execute().use { response ->
            if (!response.isSuccessful) error("Лента GitHub ответила ${response.code}")
            response.body?.string().orEmpty()
        }
        val entry = Regex("""<entry>(.*?)</entry>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(feed)
            .map { it.groupValues[1] }
            .firstOrNull { it.contains("/releases/tag/$tag") }
            .orEmpty()
        val rawContent = Regex("""<content[^>]*>(.*?)</content>""", RegexOption.DOT_MATCHES_ALL)
            .find(entry)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
        val notes = htmlText(rawContent)

        val assetsHtml = client.newCall(
            githubRequest("https://github.com/$repo/releases/expanded_assets/$tag").build(),
        ).execute().use { response ->
            if (!response.isSuccessful) error("Файлы релиза GitHub недоступны (${response.code})")
            response.body?.string().orEmpty()
        }
        val apkPaths = Regex("""href="([^"]+\.apk(?:\?[^"]*)?)""", RegexOption.IGNORE_CASE)
            .findAll(assetsHtml)
            .map { it.groupValues[1].replace("&amp;", "&") }
            .filterNot { it.contains("debug", ignoreCase = true) || it.endsWith(".idsig", true) }
            .toList()
        val apkPath = apkPaths.firstOrNull { it.contains("tomilo-lib", ignoreCase = true) }
            ?: apkPaths.firstOrNull()
            ?: error("В релизе $tag нет APK")
        val apkUrl = if (apkPath.startsWith("http")) apkPath else "https://github.com$apkPath"
        val apkName = apkPath.substringBefore('?').substringAfterLast('/')
        val versionName = tag.removePrefix("v").ifBlank {
            Regex("""\d+\.\d+(?:\.\d+)?""").find(notes)?.value ?: "0"
        }
        return AppRelease(
            versionName = versionName,
            versionCode = parseVersionCode(notes),
            notes = notes.ifBlank { "Новая версия $versionName" },
            htmlUrl = htmlUrl,
            apkName = apkName,
            apkUrl = apkUrl,
            apkSize = 0,
        )
    }

    private fun githubRequest(url: String): Request.Builder = Request.Builder()
        .url(url)
        .header("User-Agent", "TOMILO-LIB-Android/${BuildConfig.VERSION_NAME}")

    private fun parseVersionCode(text: String): Int? =
        Regex("""versionCode\s*[:=]?\s*(\d+)""", RegexOption.IGNORE_CASE)
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

    private fun htmlText(value: String): String {
        val xmlDecoded = value
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
        return Html.fromHtml(xmlDecoded, Html.FROM_HTML_MODE_LEGACY).toString().trim()
    }

    private fun githubError(code: Int): String = when (code) {
        403, 429 -> "GitHub временно ограничил запросы. Повторите через минуту."
        404 -> "Релиз на GitHub не найден."
        else -> "GitHub ответил $code"
    }

    private fun compareSemver(a: String, b: String): Int {
        fun parts(v: String) = v.split('.', '-', '_')
            .mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
        val left = parts(a)
        val right = parts(b)
        val n = maxOf(left.size, right.size)
        for (i in 0 until n) {
            val d = left.getOrElse(i) { 0 } - right.getOrElse(i) { 0 }
            if (d != 0) return d
        }
        return 0
    }
}
