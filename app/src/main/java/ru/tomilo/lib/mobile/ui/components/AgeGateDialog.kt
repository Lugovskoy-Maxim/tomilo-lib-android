package ru.tomilo.lib.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloPremium
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimaryDim
import ru.tomilo.lib.mobile.ui.theme.TomiloText

/** Обязательное подтверждение 18+ при первом запуске. */
@Composable
fun AgeGateDialog(
    onAdult: () -> Unit,
    onExit: () -> Unit,
) {
    Dialog(
        onDismissRequest = { /* обязательный ответ */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(listOf(Color(0xFF1C1010), TomiloBg)),
                    shape = RoundedCornerShape(24.dp),
                )
                .border(
                    width = 1.dp,
                    color = TomiloPrimaryDim.copy(alpha = 0.42f),
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(TomiloPremium.copy(alpha = 0.28f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = TomiloPremium,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Возрастное ограничение",
                color = TomiloPremium,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(18.dp))
            Text("Сайт содержит материалы 18+", color = TomiloText, fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                "На Tomilo-lib есть произведения с возрастным ограничением. Продолжая, вы подтверждаете, что вам исполнилось 18 лет.",
                color = TomiloText,
                fontSize = 16.sp,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121111), RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Подтверждение сохраняется только на этом устройстве. Если вам нет 18 лет, выберите выход с сайта.",
                    color = TomiloText,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onAdult,
                    modifier = Modifier.fillMaxWidth(0.64f).height(44.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, TomiloBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TomiloText),
                ) {
                    Text("Мне исполнилось 18 лет", fontSize = 14.sp)
                }
                Button(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth(0.28f).height(40.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TomiloPrimaryDim),
                ) {
                    Text("Выйти", fontSize = 14.sp)
                }
            }
        }
    }
}
