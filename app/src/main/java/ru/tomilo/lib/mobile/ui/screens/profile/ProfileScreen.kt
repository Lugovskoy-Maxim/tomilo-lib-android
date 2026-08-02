package ru.tomilo.lib.mobile.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    onLogin: () -> Unit,
    onOpenOffline: () -> Unit,
) {
    val user by authRepository.userFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    LaunchedEffect(user?.stableId()) {
        if (user != null) {
            authRepository.refreshProfile()
        }
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TomiloBg),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp),
        ) {
            if (user == null) {
                Text("Вы не вошли", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Войдите, чтобы синхронизировать аккаунт и скачивать главы офлайн (Premium).",
                    color = TomiloMuted,
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
                    Text("Войти")
                }
            } else {
                Text(user!!.username ?: "Пользователь", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(user!!.email.orEmpty(), color = TomiloMuted)
                Spacer(Modifier.height(12.dp))
                val premium = Premium.isActive(user!!.subscriptionExpiresAt)
                Text(
                    if (premium) "Premium активен" else "Без Premium",
                    color = if (premium) TomiloPremium else TomiloMuted,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (premium && !user!!.subscriptionExpiresAt.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("до ${user!!.subscriptionExpiresAt}", color = TomiloMuted, style = MaterialTheme.typography.bodySmall)
                } else if (!premium) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Офлайн-скачивание глав доступно только с Premium.",
                        color = TomiloMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = onOpenOffline, modifier = Modifier.fillMaxWidth()) {
                    Text("Офлайн-библиотека")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { scope.launch { authRepository.logout() } },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Выйти")
                }
            }
        }
    }
}
