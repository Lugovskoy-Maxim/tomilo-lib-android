package ru.tomilo.lib.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ru.tomilo.lib.mobile.ui.theme.TomiloBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface2

/** Compact Figma control with a full Android touch target. */
@Composable
fun TomiloSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier.size(48.dp).toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(41.dp, 20.dp).background(TomiloSurface2, CircleShape).border(1.dp, TomiloBorder, CircleShape))
        Box(
            Modifier.offset(x = if (checked) 9.5.dp else (-9.5).dp)
                .size(22.dp).background(
                    (if (checked) TomiloPrimary else TomiloMuted).copy(alpha = if (enabled) 1f else 0.4f),
                    CircleShape,
                ),
        )
    }
}
