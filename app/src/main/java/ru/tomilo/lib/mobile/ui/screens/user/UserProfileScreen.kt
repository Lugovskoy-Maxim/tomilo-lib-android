package ru.tomilo.lib.mobile.ui.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import ru.tomilo.lib.mobile.core.MediaUrl
import ru.tomilo.lib.mobile.core.Premium
import ru.tomilo.lib.mobile.data.api.PublicUserDto
import ru.tomilo.lib.mobile.data.repo.AuthRepository
import ru.tomilo.lib.mobile.data.repo.SocialRepository
import ru.tomilo.lib.mobile.ui.components.ErrorBox
import ru.tomilo.lib.mobile.ui.components.LoadingBox
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    authRepository: AuthRepository,
    socialRepository: SocialRepository,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenChat: (conversationId: String, title: String) -> Unit,
) {
    val me by authRepository.userFlow.collectAsState(initial = null)
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var user by remember { mutableStateOf<PublicUserDto?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        loading = true
        error = null
        socialRepository.publicUser(userId)
            .onSuccess { user = it }
            .onFailure { error = it.message }
        loading = false
    }

    Scaffold(
        containerColor = TomiloBg,
        topBar = {
            TopAppBar(
                title = { Text(user?.username ?: "Профиль") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (me != null && me!!.stableId() != userId) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    socialRepository.openConversationWith(userId)
                                        .onSuccess {
                                            onOpenChat(
                                                it.stableId(),
                                                user?.username ?: "Чат",
                                            )
                                        }
                                        .onFailure { error = it.message }
                                }
                            },
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Написать")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TomiloBg),
            )
        },
    ) { padding ->
        when {
            loading -> LoadingBox(Modifier.padding(padding))
            error != null && user == null -> Column(Modifier.padding(padding)) {
                ErrorBox(error ?: "Ошибка")
            }
            user != null -> {
                val u = user!!
                Column(
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AsyncImage(
                        model = MediaUrl.resolve(u.avatar),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(TomiloSurface2),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(u.username ?: "user", style = MaterialTheme.typography.headlineMedium)
                    if (Premium.isActive(u.subscriptionExpiresAt)) {
                        Text("Premium", color = TomiloPremium)
                    }
                    Text("Уровень ${u.level ?: 0}", color = TomiloMuted)
                    if (!u.bio.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(u.bio!!, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(16.dp))
                    Stat("Глав прочитано", u.chaptersRead)
                    Stat("Комментарии", u.commentsCount)
                    Stat("Лайки", u.likesReceivedCount)
                    Stat("Стрик", u.currentStreak)
                    Stat("Тайтлов", u.titlesReadCount)
                    Stat("Завершено", u.completedTitlesCount)
                    Stat("Время чтения, мин", u.readingTimeMinutes)

                    if (me == null) {
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
                            Text("Войти, чтобы написать")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: Int?) {
    if (value == null) return
    RowStat(label, value.toString())
}

@Composable
private fun RowStat(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(label, color = TomiloMuted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
