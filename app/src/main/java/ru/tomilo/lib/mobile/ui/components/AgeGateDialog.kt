package ru.tomilo.lib.mobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary

/**
 * Первый запуск: подтверждение возраста.
 * 18+ по умолчанию выключен; взрослый может включить в профиле/каталоге.
 */
@Composable
fun AgeGateDialog(
    onAdult: () -> Unit,
    onMinor: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { /* обязательный ответ */ },
        icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = TomiloPrimary) },
        title = {
            Text("Подтверждение возраста", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "Вам уже исполнилось 18 лет?",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Контент 18+ по умолчанию скрыт. Если вам есть 18, вы сможете " +
                        "включить его позже в профиле или фильтрах каталога.",
                    color = TomiloMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAdult,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            ) {
                Text("Мне есть 18")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onMinor,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            ) {
                Text("Мне нет 18")
            }
        },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        containerColor = TomiloSurface2,
    )
}
