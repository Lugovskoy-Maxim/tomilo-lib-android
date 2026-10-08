package ru.tomilo.lib.mobile.ui.screens.games

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContextWrapper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayInputStream
import java.net.URI

internal object SectTrainingAssetPolicy {
    const val ENTRY_URL = "https://appassets.androidplatform.net/assets/sect-training/index.html"
    fun assetPath(url: String): String? = runCatching {
        val uri = URI(url)
        val path = uri.rawPath ?: return null
        if (uri.scheme != "https" || uri.host != "appassets.androidplatform.net" ||
            uri.port != -1 || uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null ||
            !path.startsWith("/assets/sect-training/") || !path.matches(Regex("[a-zA-Z0-9_./-]+")) ||
            path.split('/').any { it == "." || it == ".." }) return null
        path.removePrefix("/assets/")
    }.getOrNull()
}
private const val PAUSE_SCRIPT = "window.dispatchEvent(new Event('tomilo-host-pause'));"

/** Standalone practice, including progress and memory challenges, works offline. */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectTrainingScreen(onBack: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val textZoom = (LocalConfiguration.current.fontScale * 100).toInt().coerceIn(100, 200)
    val view = LocalView.current
    DisposableEffect(view) {
        val activity = generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().firstOrNull()
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (previous != null) controller.isAppearanceLightStatusBars = previous }
    }
    val holder = remember { arrayOfNulls<WebView>(1) }
    fun back() {
        val view = holder[0]
        if (view == null) onBack()
        else view.evaluateJavascript("(()=>{const d=document.getElementById('settings');if(d?.open){d.close();return true}return false})()") { result ->
            if (result != "true") onBack()
        }
    }
    BackHandler { back() }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            val view = holder[0] ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_PAUSE -> view.evaluateJavascript(PAUSE_SCRIPT) {
                    if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) view.onPause()
                }
                Lifecycle.Event.ON_RESUME -> view.onResume()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Scaffold(containerColor = Color(0xFF081813), topBar = {
        TopAppBar(title = { Text("Тропа духозверей") }, navigationIcon = {
            IconButton(onClick = { back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF081813), titleContentColor = Color(0xFFF3EEE1), navigationIconContentColor = Color(0xFFF3EEE1)))
    }) { padding ->
        AndroidView(modifier = Modifier.fillMaxSize().padding(padding), factory = { context ->
            WebView(context).apply {
                holder[0] = this
                setBackgroundColor(android.graphics.Color.rgb(8, 24, 19))
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.textZoom = textZoom
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.blockNetworkLoads = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.setSupportMultipleWindows(false)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = request.url.toString() != SectTrainingAssetPolicy.ENTRY_URL
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                        val path = SectTrainingAssetPolicy.assetPath(request.url.toString())
                        if (request.method != "GET" || path == null) return rejected()
                        val mime = when (path.substringAfterLast('.')) {
                            "html" -> "text/html"
                            "js" -> "text/javascript"
                            "css" -> "text/css"
                            "json" -> "application/json"
                            "webp" -> "image/webp"
                            "jpeg", "jpg" -> "image/jpeg"
                            "png" -> "image/png"
                            "svg" -> "image/svg+xml"
                            else -> "text/plain"
                        }
                        return runCatching { WebResourceResponse(mime, "UTF-8", context.assets.open(path)) }.getOrElse { rejected() }
                    }
                    private fun rejected() = WebResourceResponse("text/plain", "UTF-8", 403, "Forbidden", emptyMap(), ByteArrayInputStream(ByteArray(0)))
                }
                loadUrl(SectTrainingAssetPolicy.ENTRY_URL)
            }
        }, update = { it.settings.textZoom = textZoom }, onRelease = { view ->
            holder[0] = null
            view.evaluateJavascript(PAUSE_SCRIPT) {
                view.stopLoading()
                view.destroy()
            }
        })
    }
}
