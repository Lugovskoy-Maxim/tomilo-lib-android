package ru.tomilo.lib.mobile.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.tomilo.lib.mobile.R
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloText

private data class OnboardingPage(
    val title: String,
    val description: String,
    val imageRes: Int,
)

private val pages = listOf(
    OnboardingPage(
        "Мир историй всегда рядом",
        "Манга, манхва и маньхуа в одной библиотеке — с персональными рекомендациями и свежими главами.",
        R.drawable.illust_onboarding_covers,
    ),
    OnboardingPage(
        "Закладки и библиотека",
        "Сохраняйте любимые тайтлы, распределяйте их по спискам и продолжайте с нужной главы.",
        R.drawable.illust_onboarding_library,
    ),
    OnboardingPage(
        "Читайте офлайн",
        "Загружайте главы заранее и возвращайтесь к историям даже без подключения к интернету.",
        R.drawable.illust_onboarding_offline,
    ),
    OnboardingPage(
        "Обсуждайте прочитанное",
        "Комментарии, реакции и чаты помогают делиться впечатлениями с другими читателями.",
        R.drawable.illust_onboarding_community,
    ),
    OnboardingPage(
        "Ваш профиль — ваша история",
        "Следите за прогрессом, серией чтения, уровнем и собирайте награды за активность.",
        R.drawable.illust_onboarding_profile,
    ),
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onLogin: () -> Unit = onComplete,
    onRegister: () -> Unit = onComplete,
) {
    var page by remember { mutableIntStateOf(0) }
    val current = pages[page]

    Box(
        Modifier
            .fillMaxSize()
            .background(TomiloBg)
            .pointerInput(page) {
                var drag = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, amount -> drag += amount },
                    onDragEnd = {
                        when {
                            drag < -80f && page < pages.lastIndex -> page += 1
                            drag > 80f && page > 0 -> page -= 1
                        }
                        drag = 0f
                    },
                )
            },
    ) {
        AnimatedContent(
            targetState = current,
            transitionSpec = { fadeIn(tween(360)) togetherWith fadeOut(tween(260)) },
            label = "onboarding-background",
            modifier = Modifier.fillMaxSize(),
        ) { item ->
            Image(
                painter = painterResource(item.imageRes),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.14f),
                        0.48f to Color.Black.copy(alpha = 0.08f),
                        0.70f to TomiloBg.copy(alpha = 0.80f),
                        1f to TomiloBg,
                    ),
                ),
        )
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = onComplete) {
                    Text("Пропустить", color = TomiloText, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            AnimatedContent(
                targetState = current,
                transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
                label = "onboarding-page",
                modifier = Modifier.fillMaxWidth(),
            ) { item ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        item.title,
                        color = TomiloText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        item.description,
                        color = TomiloText.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pages.indices.forEach { index ->
                    Box(
                        Modifier
                            .size(if (index == page) 22.dp else 6.dp, 6.dp)
                            .clip(CircleShape)
                            .background(if (index == page) TomiloPrimary else TomiloBorder),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (page == pages.lastIndex) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onLogin,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                    ) { Text("Войти") }
                    OutlinedButton(
                        onClick = onRegister,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                    ) { Text("Регистрация") }
                }
                Spacer(Modifier.height(10.dp))
            }
            Button(
                onClick = {
                    if (page == pages.lastIndex) onComplete() else page += 1
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TomiloPrimary),
            ) {
                Text(if (page == pages.lastIndex) "Начать" else "Далее", fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}
