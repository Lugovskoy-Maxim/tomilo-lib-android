package ru.tomilo.lib.mobile.ui.screens.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.util.Base64
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import ru.tomilo.lib.mobile.core.OAuthConfig
import ru.tomilo.lib.mobile.core.OAuthRedirectPolicy
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloTheme
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * In-app WebView OAuth для Яндекс (implicit token) и VK ID (PKCE).
 * Результат возвращается через setResult.
 */
class OAuthWebActivity : ComponentActivity() {

    private val loading = mutableStateOf(true)
    private val loadError = mutableStateOf<String?>(null)
    private var webView: WebView? = null
    private var authUrl = ""

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val provider = intent.getStringExtra(EXTRA_PROVIDER) ?: PROVIDER_YANDEX
        val title = if (provider == PROVIDER_VK) "Вход через VK" else "Вход через Яндекс"

        authUrl = savedInstanceState?.getString("auth_url") ?: if (provider == PROVIDER_VK) {
            buildVkAuthUrl()
        } else {
            buildYandexAuthUrl()
        }

        setContent {
            TomiloTheme {
                Scaffold(
                    containerColor = TomiloBg,
                    topBar = {
                        TopAppBar(
                            title = { Text(title) },
                            navigationIcon = {
                                IconButton(onClick = {
                                    setResult(Activity.RESULT_CANCELED)
                                    finish()
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                                }
                            },
                        )
                    },
                ) { padding ->
                    LaunchedEffect(loading.value, loadError.value) {
                        if (loading.value && loadError.value == null) {
                            delay(30_000)
                            if (loading.value) loadError.value = "Страница входа не отвечает. Проверьте подключение и повторите попытку."
                        }
                    }
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize(),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    webView = this
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    CookieManager.getInstance().setAcceptCookie(true)
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                    settings.setSupportMultipleWindows(true)
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                                            if (!isUserGesture) return false
                                            val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                            val parent = this@apply
                                            val popup = WebView(ctx)
                                            popup.webViewClient = object : WebViewClient() {
                                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                                    val uri = request?.url ?: return false
                                                    if (uri.scheme == "https") parent.loadUrl(uri.toString())
                                                    else showLoadError()
                                                    popup.destroy()
                                                    return true
                                                }
                                            }
                                            transport.webView = popup
                                            resultMsg.sendToTarget()
                                            return true
                                        }
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            loading.value = true
                                            url?.let { maybeHandle(it) }
                                        }

                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                        ): Boolean {
                                            if (request?.isForMainFrame != true) return false
                                            val u = request.url.toString()
                                            if (maybeHandle(u)) return true
                                            if (request.url.scheme !in listOf("https", "http")) {
                                                loadError.value = "Не удалось открыть переход VK. Вернитесь назад и повторите вход через веб-форму."
                                                loading.value = false
                                                return true
                                            }
                                            return false
                                        }

                                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                            if (request?.isForMainFrame == true) showLoadError()
                                        }

                                        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                                            if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 400) showLoadError()
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            loading.value = false
                                            url?.let { maybeHandle(it) }
                                        }
                                    }
                                    loadUrl(authUrl)
                                }
                            },
                        )
                        if (loadError.value != null) {
                            Column(
                                Modifier.fillMaxSize().background(TomiloBg),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(loadError.value.orEmpty(), modifier = Modifier.padding(24.dp))
                                Button(onClick = {
                                    loadError.value = null
                                    loading.value = true
                                    authUrl = if (provider == PROVIDER_VK) buildVkAuthUrl() else buildYandexAuthUrl()
                                    webView?.loadUrl(authUrl)
                                }) { Text("Повторить") }
                            }
                        } else if (loading.value) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
        }
    }

    private fun showLoadError() {
        loading.value = false
        loadError.value = "Не удалось загрузить страницу входа. Проверьте подключение и повторите попытку."
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("auth_url", authUrl)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    private fun maybeHandle(url: String): Boolean {
        return when {
            OAuthRedirectPolicy.matches(url, OAuthConfig.YANDEX_REDIRECT) -> {
                handleYandex(url)
            }
            OAuthRedirectPolicy.matches(url, OAuthConfig.VK_REDIRECT) -> {
                handleVk(url)
            }
            else -> false
        }
    }

    private fun handleYandex(url: String): Boolean {
        // Implicit: ...#access_token=...&token_type=bearer
        val fragment = url.substringAfter('#', missingDelimiterValue = "")
        val query = if (fragment.isNotBlank()) fragment else url.substringAfter('?', "")
        val params = parseParams(query)
        val token = params["access_token"]
        if (!token.isNullOrBlank()) {
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(EXTRA_PROVIDER, PROVIDER_YANDEX)
                    .putExtra(EXTRA_ACCESS_TOKEN, token),
            )
            finish()
            return true
        }
        if (params["error"] != null) {
            setResult(
                Activity.RESULT_CANCELED,
                Intent().putExtra(EXTRA_ERROR, params["error_description"] ?: params["error"]),
            )
            finish()
            return true
        }
        return false
    }

    private fun handleVk(url: String): Boolean {
        val uri = Uri.parse(url)
        val code = uri.getQueryParameter("code")
        val deviceId = uri.getQueryParameter("device_id").orEmpty()
        val state = uri.getQueryParameter("state").orEmpty()
        val error = uri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            setResult(
                Activity.RESULT_CANCELED,
                Intent().putExtra(EXTRA_ERROR, uri.getQueryParameter("error_description") ?: error),
            )
            finish()
            return true
        }
        if (!code.isNullOrBlank()) {
            val verifier = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(KEY_VK_VERIFIER, "").orEmpty()
            val expectedState = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(KEY_VK_STATE, "").orEmpty()
            if (!OAuthRedirectPolicy.validVkResponse(verifier, expectedState, state, deviceId)) {
                setResult(Activity.RESULT_CANCELED, Intent().putExtra(EXTRA_ERROR, "Не удалось подтвердить вход через VK. Повторите попытку."))
                finish()
                return true
            }
            setResult(
                Activity.RESULT_OK,
                Intent()
                    .putExtra(EXTRA_PROVIDER, PROVIDER_VK)
                    .putExtra(EXTRA_CODE, code)
                    .putExtra(EXTRA_CODE_VERIFIER, verifier)
                    .putExtra(EXTRA_DEVICE_ID, deviceId)
                    .putExtra(EXTRA_STATE, state),
            )
            finish()
            return true
        }
        return false
    }

    private fun buildYandexAuthUrl(): String {
        val redirect = Uri.encode(OAuthConfig.YANDEX_REDIRECT)
        return "https://oauth.yandex.ru/authorize?response_type=token" +
            "&client_id=${OAuthConfig.YANDEX_CLIENT_ID}" +
            "&redirect_uri=$redirect"
    }

    private fun buildVkAuthUrl(): String {
        val verifier = randomUrlSafe(64)
        val state = randomUrlSafe(43)
        val challenge = sha256Base64Url(verifier)
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putString(KEY_VK_VERIFIER, verifier)
            .putString(KEY_VK_STATE, state)
            .apply()

        val params = Uri.Builder()
            .scheme("https")
            .authority("id.vk.ru")
            .path("/authorize")
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", OAuthConfig.VK_APP_ID)
            .appendQueryParameter("redirect_uri", OAuthConfig.VK_REDIRECT)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("scope", "email")
            .build()
        return params.toString()
    }

    private fun parseParams(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return raw.split('&').mapNotNull { part ->
            val i = part.indexOf('=')
            if (i <= 0) null
            else {
                val k = Uri.decode(part.substring(0, i))
                val v = Uri.decode(part.substring(i + 1))
                k to v
            }
        }.toMap()
    }

    private fun randomUrlSafe(len: Int): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-"
        val rnd = SecureRandom()
        return buildString(len) {
            repeat(len) { append(chars[rnd.nextInt(chars.length)]) }
        }
    }

    private fun sha256Base64Url(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    companion object {
        const val EXTRA_PROVIDER = "provider"
        const val EXTRA_ACCESS_TOKEN = "access_token"
        const val EXTRA_CODE = "code"
        const val EXTRA_CODE_VERIFIER = "code_verifier"
        const val EXTRA_DEVICE_ID = "device_id"
        const val EXTRA_STATE = "state"
        const val EXTRA_ERROR = "error"
        const val PROVIDER_YANDEX = "yandex"
        const val PROVIDER_VK = "vk"

        private const val PREFS = "tomilo_oauth"
        private const val KEY_VK_VERIFIER = "vk_verifier"
        private const val KEY_VK_STATE = "vk_state"

        fun intent(context: Context, provider: String): Intent =
            Intent(context, OAuthWebActivity::class.java).putExtra(EXTRA_PROVIDER, provider)
    }
}
