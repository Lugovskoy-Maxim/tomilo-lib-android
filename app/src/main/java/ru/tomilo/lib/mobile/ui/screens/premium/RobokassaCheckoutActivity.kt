package ru.tomilo.lib.mobile.ui.screens.premium

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloTheme

/**
 * Открывает платёжную форму Robokassa (POST) и ловит возврат
 * на tomilo-lib.ru/premium?payment=success|failed.
 */
class RobokassaCheckoutActivity : ComponentActivity() {

    private val loading = mutableStateOf(true)
    private var finished = false

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val paymentUrl = intent.getStringExtra(EXTRA_PAYMENT_URL).orEmpty()
        val invId = intent.getStringExtra(EXTRA_INV_ID).orEmpty()
        val fieldsJson = intent.getStringExtra(EXTRA_FIELDS_JSON).orEmpty()
        val fields = parseFields(fieldsJson)

        if (paymentUrl.isBlank() || fields.isEmpty()) {
            finishWith(RESULT_FAILED, invId)
            return
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finishWith(RESULT_CANCELLED, invId)
                }
            },
        )

        setContent {
            TomiloTheme {
                Scaffold(
                    containerColor = TomiloBg,
                    topBar = {
                        TopAppBar(
                            title = { Text(if (loading.value) "Открываем оплату…" else "Оплата") },
                            navigationIcon = {
                                IconButton(onClick = { finishWith(RESULT_CANCELLED, invId) }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                                }
                            },
                            colors = tomiloTopBarColors(),
                        )
                    },
                ) { padding ->
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                    ): Boolean {
                                        val url = request?.url?.toString().orEmpty()
                                        return handleReturn(url, invId)
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        loading.value = false
                                        url?.let { handleReturn(it, invId) }
                                    }
                                }
                                loadDataWithBaseURL(
                                    paymentUrl,
                                    buildAutoSubmitHtml(paymentUrl, fields),
                                    "text/html",
                                    Charsets.UTF_8.name(),
                                    null,
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    private fun handleReturn(url: String, fallbackInvId: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val host = uri.host.orEmpty()
        val path = uri.path.orEmpty()
        if (host != "tomilo-lib.ru" && host != "www.tomilo-lib.ru") return false
        if (!path.startsWith("/premium") && !path.contains("/payments/robokassa/")) return false

        val payment = uri.getQueryParameter("payment")
        val invoice = uri.getQueryParameter("invoice")
            ?: uri.getQueryParameter("InvId")
            ?: fallbackInvId
        return when (payment) {
            "success" -> {
                finishWith(RESULT_SUCCESS, invoice)
                true
            }
            "failed" -> {
                finishWith(RESULT_FAILED, invoice)
                true
            }
            else -> if (path.contains("/payments/robokassa/fail")) {
                finishWith(RESULT_FAILED, invoice)
                true
            } else if (path.contains("/payments/robokassa/success")) {
                finishWith(RESULT_SUCCESS, invoice)
                true
            } else {
                false
            }
        }
    }

    private fun finishWith(status: String, invId: String) {
        if (finished) return
        finished = true
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtra(EXTRA_STATUS, status)
                .putExtra(EXTRA_INV_ID, invId),
        )
        finish()
    }

    companion object {
        const val EXTRA_PAYMENT_URL = "payment_url"
        const val EXTRA_FIELDS_JSON = "fields_json"
        const val EXTRA_INV_ID = "inv_id"
        const val EXTRA_STATUS = "status"
        const val RESULT_SUCCESS = "success"
        const val RESULT_FAILED = "failed"
        const val RESULT_CANCELLED = "cancelled"

        fun intent(
            context: Context,
            paymentUrl: String,
            invId: String,
            fieldsJson: String,
        ): Intent = Intent(context, RobokassaCheckoutActivity::class.java)
            .putExtra(EXTRA_PAYMENT_URL, paymentUrl)
            .putExtra(EXTRA_INV_ID, invId)
            .putExtra(EXTRA_FIELDS_JSON, fieldsJson)
    }
}

private fun parseFields(json: String): Map<String, String> {
    if (json.isBlank()) return emptyMap()
    return runCatching {
        ru.tomilo.lib.mobile.data.api.NetworkModule.json.decodeFromString<Map<String, String>>(json)
    }.getOrDefault(emptyMap())
}

private fun buildAutoSubmitHtml(action: String, fields: Map<String, String>): String {
    val inputs = fields.entries.joinToString("") { (name, value) ->
        """<input type="hidden" name="${htmlEscape(name)}" value="${htmlEscape(value)}">"""
    }
    return """
        <!DOCTYPE html>
        <html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        </head><body>
        <form id="pay" method="POST" action="${htmlEscape(action)}">$inputs</form>
        <script>document.getElementById('pay').submit();</script>
        </body></html>
    """.trimIndent()
}

private fun htmlEscape(value: String): String = buildString(value.length) {
    value.forEach { ch ->
        when (ch) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(ch)
        }
    }
}
