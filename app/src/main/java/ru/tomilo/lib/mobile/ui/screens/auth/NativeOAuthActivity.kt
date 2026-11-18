package ru.tomilo.lib.mobile.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.vk.id.AccessToken
import com.vk.id.VKID
import com.vk.id.VKIDAuthFail
import com.vk.id.auth.AuthCodeData
import com.vk.id.auth.VKIDAuthCallback
import com.vk.id.auth.VKIDAuthParams
import com.yandex.authsdk.YandexAuthSdk
import com.yandex.authsdk.YandexAuthOptions
import com.yandex.authsdk.YandexAuthLoginOptions
import com.yandex.authsdk.YandexAuthResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.BuildConfig
import ru.tomilo.lib.mobile.core.OAuthPkce
import ru.tomilo.lib.mobile.ui.theme.TomiloTheme

/** Provider SDKs own the sign-in UI and callbacks. No website callback or embedded login WebView. */
class NativeOAuthActivity : ComponentActivity() {
    private var completed = false
    private val verifier = OAuthPkce.randomValue()
    private val authState = OAuthPkce.randomValue()
    private val provider get() = intent.getStringExtra(EXTRA_PROVIDER)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val yandex = YandexAuthSdk.create(YandexAuthOptions(this))
        // Register unconditionally so the registry can deliver the Yandex result after recreation.
        val yandexLauncher = registerForActivityResult(yandex.contract) { result ->
            when (result) {
                is YandexAuthResult.Success -> finishSuccess(
                    Intent().putExtra(EXTRA_ACCESS_TOKEN, result.token.value),
                )
                is YandexAuthResult.Failure -> finishError("Не удалось войти через Яндекс. Проверьте соединение и повторите попытку.")
                YandexAuthResult.Cancelled -> finishCancelled()
            }
        }
        setContent {
            TomiloTheme {
                androidx.compose.material3.Surface {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(20.dp))
                        Text(if (provider == PROVIDER_VK) "Вход через VK ID…" else "Вход через Яндекс…")
                        TextButton(onClick = { finishCancelled() }) { Text("Отмена") }
                    }
                }
            }
        }
        if (savedInstanceState != null) {
            // VK callbacks are in-memory. Never restart a lost transaction with another verifier.
            if (provider == PROVIDER_VK) finishError("Вход прерван. Нажмите «Войти через VK» ещё раз.")
            return
        }
        when (provider) {
            PROVIDER_YANDEX -> try {
                yandexLauncher.launch(YandexAuthLoginOptions())
            } catch (_: Exception) {
                finishError("Не удалось открыть вход через Яндекс. Повторите попытку.")
            }
            PROVIDER_VK -> startVkLogin()
            else -> finishError("Неизвестный способ входа.")
        }
    }

    private fun startVkLogin() {
        if (!BuildConfig.HAS_VKID_CONFIG) {
            finishError("Вход через VK пока не настроен. Используйте Яндекс или email.")
            return
        }
        lifecycleScope.launch {
            try {
                VKID.instance.authorize(
                    callback = object : VKIDAuthCallback {
                        // With an external challenge the SDK checks state and returns only a code.
                        override fun onAuthCode(data: AuthCodeData, isCompletion: Boolean) {
                            if (!isCompletion || data.code.isBlank() || data.deviceId.isBlank()) {
                                finishError("VK не вернул данные для входа. Повторите попытку.")
                                return
                            }
                            finishSuccess(
                                Intent().putExtra(EXTRA_CODE, data.code)
                                    .putExtra(EXTRA_CODE_VERIFIER, verifier)
                                    .putExtra(EXTRA_DEVICE_ID, data.deviceId)
                                    .putExtra(EXTRA_STATE, authState)
                                    .putExtra(EXTRA_REDIRECT_URI, "vk${BuildConfig.VKID_CLIENT_ID}://vk.ru/blank.html"),
                            )
                        }

                        override fun onAuth(accessToken: AccessToken) {
                            finishError("Не удалось завершить вход через VK. Повторите попытку.")
                        }

                        override fun onFail(fail: VKIDAuthFail) {
                            if (fail is VKIDAuthFail.Canceled) finishCancelled()
                            else finishError("Не удалось войти через VK. Проверьте соединение и повторите попытку.")
                        }
                    },
                    params = VKIDAuthParams {
                        state = authState
                        codeChallenge = OAuthPkce.challenge(verifier)
                        scopes = setOf("email")
                    },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                finishError("Не удалось открыть вход через VK. Повторите попытку.")
            }
        }
    }

    private fun finishSuccess(data: Intent) = complete(Activity.RESULT_OK, data.putExtra(EXTRA_PROVIDER, provider))
    private fun finishError(message: String) = complete(Activity.RESULT_CANCELED, Intent().putExtra(EXTRA_ERROR, message))
    private fun finishCancelled() = complete(Activity.RESULT_CANCELED, null)

    private fun complete(code: Int, data: Intent?) {
        runOnUiThread {
            if (!completed && !isFinishing && !isDestroyed) {
                completed = true
                setResult(code, data)
                finish()
            }
        }
    }

    companion object {
        const val PROVIDER_VK = "vk"
        const val PROVIDER_YANDEX = "yandex"
        const val EXTRA_PROVIDER = "provider"
        const val EXTRA_ERROR = "error"
        const val EXTRA_ACCESS_TOKEN = "access_token"
        const val EXTRA_CODE = "code"
        const val EXTRA_CODE_VERIFIER = "code_verifier"
        const val EXTRA_DEVICE_ID = "device_id"
        const val EXTRA_STATE = "state"
        const val EXTRA_REDIRECT_URI = "redirect_uri"

        fun intent(context: Context, provider: String) = Intent(context, NativeOAuthActivity::class.java)
            .putExtra(EXTRA_PROVIDER, provider)
    }
}
