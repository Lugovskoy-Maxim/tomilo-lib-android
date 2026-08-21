package ru.tomilo.lib.mobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

data class RewardNotice(
    val experience: Int = 0,
    val coins: Int = 0,
    val source: String = "Награда получена",
)

/** Общая очередь наград: экран может смениться, а начисление всё равно будет показано. */
object RewardNotifications {
    private val queue = Channel<RewardNotice>(
        capacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    internal val events = queue.receiveAsFlow()

    fun show(experience: Int = 0, coins: Int = 0, source: String) {
        if (experience == 0 && coins == 0) return
        queue.trySend(RewardNotice(experience, coins, source))
    }
}

@Composable
fun RewardNotificationHost(modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf<RewardNotice?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        RewardNotifications.events.collect { reward ->
            current = reward
            visible = true
            delay(3_400)
            visible = false
            delay(280)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .zIndex(50f),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = visible && current != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        ) {
            current?.let { reward ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .shadow(18.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF342B14), TomiloSurface2, Color(0xFF152D2B)),
                            ),
                        )
                        .border(1.dp, TomiloPremium.copy(alpha = 0.48f), RoundedCornerShape(22.dp))
                        .padding(horizontal = 15.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(43.dp).clip(CircleShape)
                            .background(TomiloPremium.copy(alpha = 0.17f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Bolt, null, tint = TomiloPremium)
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            reward.source,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.76f),
                            maxLines = 1,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (reward.experience != 0) {
                                Text(
                                    "${signed(reward.experience)} XP",
                                    color = TomiloPrimary,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                            if (reward.coins != 0) {
                                if (reward.experience != 0) Spacer(Modifier.width(12.dp))
                                Icon(Icons.Default.MonetizationOn, null, tint = TomiloPremium, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    signed(reward.coins),
                                    color = TomiloPremium,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()
