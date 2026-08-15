package ru.tomilo.lib.mobile.ui.screens.auth

import android.app.Activity
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
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.ui.components.TomiloBrandHeader
import ru.tomilo.lib.mobile.ui.components.TomiloLoginIllustration
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
    var error by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val passwordFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    fun submit() {
        if (loading || email.isBlank() || password.isBlank()) return
        scope.launch {
            loading = true
            error = null
            focusManager.clearFocus()
            authRepository.login(email, password)
                .onSuccess { onSuccess() }
                .onFailure { error = it.message ?: "Ошибка входа" }
            loading = false
        }
    }

    val oauthLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            val msg = result.data?.getStringExtra(OAuthWebActivity.EXTRA_ERROR)
            if (!msg.isNullOrBlank()) error = msg
            return@rememberLauncherForActivityResult
        }
        val data = result.data ?: return@rememberLauncherForActivityResult
        val provider = data.getStringExtra(OAuthWebActivity.EXTRA_PROVIDER)
        scope.launch {
            loading = true
            error = null
            val res = when (provider) {
                OAuthWebActivity.PROVIDER_YANDEX -> {
                    val token = data.getStringExtra(OAuthWebActivity.EXTRA_ACCESS_TOKEN).orEmpty()
                    authRepository.loginYandex(token)
                }
                OAuthWebActivity.PROVIDER_VK -> {
                    authRepository.loginVkId(
                        code = data.getStringExtra(OAuthWebActivity.EXTRA_CODE).orEmpty(),
                        codeVerifier = data.getStringExtra(OAuthWebActivity.EXTRA_CODE_VERIFIER).orEmpty(),
                        deviceId = data.getStringExtra(OAuthWebActivity.EXTRA_DEVICE_ID).orEmpty(),
                        state = data.getStringExtra(OAuthWebActivity.EXTRA_STATE).orEmpty(),
                    )
                }
                else -> Result.failure(IllegalStateException("Неизвестный провайдер"))
            }
            res.onSuccess { onSuccess() }
                .onFailure { error = it.message ?: "Ошибка входа" }
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
            TomiloBrandHeader(subtitle = "Тот же аккаунт, что на tomilo-lib.ru")
            Spacer(Modifier.height(8.dp))
            TomiloLoginIllustration(height = 140.dp)
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        oauthLauncher.launch(
                            OAuthWebActivity.intent(context, OAuthWebActivity.PROVIDER_YANDEX),
                        )
                    },
                    enabled = !loading,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 13.dp),
                ) { Text("Яндекс", fontWeight = FontWeight.SemiBold) }
                OutlinedButton(
                    onClick = {
                        oauthLauncher.launch(
                            OAuthWebActivity.intent(context, OAuthWebActivity.PROVIDER_VK),
                        )
                    },
                    enabled = !loading,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 13.dp),
                ) { Text("VK", fontWeight = FontWeight.SemiBold) }
            }

            Spacer(Modifier.height(20.dp))
            Text("или email", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))

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
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error!!, color = TomiloDanger)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { submit() },
                enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Text(if (loading) "Входим…" else "Войти", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
