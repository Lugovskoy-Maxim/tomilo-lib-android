package ru.tomilo.lib.mobile.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Единый переключатель для настроек во всех темах приложения. */
@Composable
fun TomiloSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onPrimary,
            checkedTrackColor = colors.primary,
            checkedBorderColor = colors.primary,
            uncheckedThumbColor = colors.onSurfaceVariant,
            uncheckedTrackColor = colors.surfaceVariant,
            uncheckedBorderColor = colors.outline,
            disabledCheckedThumbColor = colors.onPrimary,
            disabledCheckedTrackColor = colors.primary.copy(alpha = 0.38f),
            disabledUncheckedThumbColor = colors.onSurfaceVariant.copy(alpha = 0.38f),
            disabledUncheckedTrackColor = colors.surfaceVariant.copy(alpha = 0.38f),
            disabledUncheckedBorderColor = colors.outline.copy(alpha = 0.38f),
        ),
    )
}
