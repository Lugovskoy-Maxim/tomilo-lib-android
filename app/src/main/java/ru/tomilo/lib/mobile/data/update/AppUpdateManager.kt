package ru.tomilo.lib.mobile.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.tomilo.lib.mobile.BuildConfig
import ru.tomilo.lib.mobile.data.api.NetworkModule
import java.io.File
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

class AppUpdateManager(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val repo = BuildConfig.GITHUB_REPO

    fun canInstallInPlace(): Boolean = !BuildConfig.DEBUG

    fun hasInstallPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun installSettingsIntent(): Intent {
        val uri = Uri.parse("package:${context.packageName}")
        return Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, uri)
    }

    suspend fun fetchLatest(): Result<AppRelease> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$repo/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "TOMILO-LIB-Android/${BuildConfig.VERSION_NAME}")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error(githubError(response.code))
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) error("Пустой ответ GitHub")
                val release = NetworkModule.json.decodeFromString(GithubReleaseDto.serializer(), body)
                val asset = pickApk(release.assets) ?: error("В релизе нет APK")
                val versionName = release.tagName.orEmpty().removePrefix("v").ifBlank {
                    release.name?.substringAfterLast(' ') ?: "0"
                }
                AppRelease(
                    versionName = versionName,
                    versionCode = Regex("""versionCode\s+(\d+)""")
                        .find(release.body.orEmpty())
                        ?.groupValues?.getOrNull(1)
                        ?.toIntOrNull(),
                    notes = release.body.orEmpty().trim().ifBlank { "Новая версия $versionName" },
                    htmlUrl = release.htmlUrl.orEmpty()
                        .ifBlank { "https://github.com/$repo/releases/latest" },
                    apkName = asset.name.orEmpty(),
                    apkUrl = asset.downloadUrl.orEmpty(),
                    apkSize = asset.size,
                ).also {
                    if (it.apkUrl.isBlank()) error("Нет ссылки на APK")
                }
            }
        }
    }

    fun isNewer(release: AppRelease): Boolean {
        val localName = BuildConfig.VERSION_NAME.substringBefore('-')
        val remoteCode = release.versionCode
        if (remoteCode != null) return remoteCode > BuildConfig.VERSION_CODE
        return compareSemver(release.versionName, localName) > 0
    }

    suspend fun download(
        release: AppRelease,
        onProgress: (Float) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val dest = File(dir, "tomilo-update.apk")
            if (dest.exists()) dest.delete()
            val request = Request.Builder()
                .url(release.apkUrl)
                .header("User-Agent", "TOMILO-LIB-Android/${BuildConfig.VERSION_NAME}")
                .header("Accept", "application/octet-stream")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Не удалось скачать APK (${response.code})")
                val body = response.body ?: error("Пустой файл обновления")
                val total = body.contentLength().takeIf { it > 0 } ?: release.apkSize
                body.byteStream().use { input ->
                    dest.outputStream().use { output ->
                        val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                        var read = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            output.write(buf, 0, n)
                            read += n
                            if (total > 0) onProgress((read.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            if (!dest.isFile || dest.length() < 1024) {
                dest.delete()
                error("Скачанный APK повреждён")
            }
            dest
        }
    }

    fun installIntent(apk: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
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
