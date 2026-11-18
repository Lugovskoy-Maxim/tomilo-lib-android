package ru.tomilo.lib.mobile.ui.screens.auth

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import ru.tomilo.lib.mobile.BuildConfig
import ru.tomilo.lib.mobile.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.toUserFacingError
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.ui.components.TomiloBrandHeader
import ru.tomilo.lib.mobile.ui.components.tomiloTopBarColors
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloDanger
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var oauthPending by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val passwordFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    fun submit() {
        if (loading || oauthPending || email.isBlank() || password.isBlank()) return
        scope.launch {
            loading = true
            error = null
            focusManager.clearFocus()
            authRepository.login(email, password)
                .onSuccess { onSuccess() }
                .onFailure { error = it.toUserFacingError("Не удалось войти.") }
            loading = false
        }
    }

    val oauthLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        oauthPending = false
        if (result.resultCode != Activity.RESULT_OK) {
            val msg = result.data?.getStringExtra(NativeOAuthActivity.EXTRA_ERROR)
            if (!msg.isNullOrBlank()) error = msg
            return@rememberLauncherForActivityResult
        }
        val data = result.data ?: return@rememberLauncherForActivityResult
        val provider = data.getStringExtra(NativeOAuthActivity.EXTRA_PROVIDER)
        scope.launch {
            loading = true
            error = null
            val res = when (provider) {
                NativeOAuthActivity.PROVIDER_YANDEX -> {
                    val token = data.getStringExtra(NativeOAuthActivity.EXTRA_ACCESS_TOKEN).orEmpty()
                    authRepository.loginYandex(token)
                }
                NativeOAuthActivity.PROVIDER_VK -> {
                    authRepository.loginVkId(
                        code = data.getStringExtra(NativeOAuthActivity.EXTRA_CODE).orEmpty(),
                        codeVerifier = data.getStringExtra(NativeOAuthActivity.EXTRA_CODE_VERIFIER).orEmpty(),
                        deviceId = data.getStringExtra(NativeOAuthActivity.EXTRA_DEVICE_ID).orEmpty(),
                        state = data.getStringExtra(NativeOAuthActivity.EXTRA_STATE).orEmpty(),
                        redirectUri = data.getStringExtra(NativeOAuthActivity.EXTRA_REDIRECT_URI),
                    )
                }
                else -> Result.failure(IllegalStateException("Неизвестный провайдер"))
            }
            res.onSuccess { onSuccess() }
                .onFailure { error = it.toUserFacingError("Не удалось войти.") }
            loading = false
        }
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = { Text("Вход") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = tomiloTopBarColors(),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TomiloBrandHeader(subtitle = null)
                Text(
                    "Войдите, чтобы открыть свою полку и продолжить чтение.",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(24.dp))

            fun launchProvider(provider: String) {
                if (loading || oauthPending) return
                error = null
                focusManager.clearFocus()
                oauthPending = true
                try {
                    val intent = if (provider == NativeOAuthActivity.PROVIDER_VK && !BuildConfig.HAS_VKID_CONFIG) {
                        OAuthWebActivity.intent(context, OAuthWebActivity.PROVIDER_VK)
                    } else {
                        NativeOAuthActivity.intent(context, provider)
                    }
                    oauthLauncher.launch(intent)
                } catch (_: android.content.ActivityNotFoundException) {
                    oauthPending = false
                    error = "Не удалось открыть вход. Повторите попытку."
                }
            }

            ProviderButton("Войти через VK", !loading && !oauthPending, {
                launchProvider(NativeOAuthActivity.PROVIDER_VK)
            }) {
                Image(painterResource(R.drawable.ic_login_vk), null, Modifier.size(24.dp))
            }
            Spacer(Modifier.height(8.dp))
            ProviderButton("Войти через Яндекс", !loading && !oauthPending, {
                launchProvider(NativeOAuthActivity.PROVIDER_YANDEX)
            }) {
                Box(
                    Modifier.size(24.dp).background(Color(0xFFFC3F1D), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Я", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("или по электронной почте", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
                shape = RoundedCornerShape(18.dp),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                singleLine = true,
                visualTransformation = if (showPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Скрыть пароль" else "Показать пароль",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().focusRequester(passwordFocus),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                shape = RoundedCornerShape(18.dp),
            )
            error?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(message, color = TomiloDanger)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { submit() },
                enabled = !loading && !oauthPending && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Text(if (loading) "Входим…" else "Войти", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProviderButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    logo: @Composable () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            logo()
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    }
}