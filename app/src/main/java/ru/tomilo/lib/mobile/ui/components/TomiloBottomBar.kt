package ru.tomilo.lib.mobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.tomilo.lib.mobile.ui.theme.TomiloActiveBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloActivePill
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
import ru.tomilo.lib.mobile.ui.theme.TomiloGlass
import ru.tomilo.lib.mobile.ui.theme.TomiloGlassBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloMuted
import ru.tomilo.lib.mobile.ui.theme.TomiloPrimary
import ru.tomilo.lib.mobile.ui.theme.TomiloSurface
import ru.tomilo.lib.mobile.ui.theme.TomiloText

data class TomiloTabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val isMain: Boolean = false,
)

private val BarShape = RoundedCornerShape(22.dp)
private val ItemShape = RoundedCornerShape(14.dp)

@Composable
fun TomiloBottomBar(
    tabs: List<TomiloTabItem>,
    currentRoute: String,
    onTabClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 10.dp)
            .shadow(18.dp, BarShape, ambientColor = Color.Black.copy(alpha = 0.4f))
            .clip(BarShape)
            .background(TomiloGlass)
            .border(1.dp, TomiloGlassBorder, BarShape)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            NavTabItem(
                label = tab.label,
                icon = if (currentRoute == tab.route) tab.selectedIcon else tab.icon,
                selected = currentRoute == tab.route,
                onClick = { onTabClick(tab.route) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NavTabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) TomiloPrimary else TomiloMuted,
        label = "tabColor",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabScale",
    )
    val bg by animateColorAsState(
        targetValue = if (selected) TomiloActivePill else Color.Transparent,
        label = "tabBg",
    )
    val border by animateColorAsState(
        targetValue = if (selected) TomiloActiveBorder else Color.Transparent,
        label = "tabBorder",
    )

    Column(
        modifier = modifier
            .clip(ItemShape)
            .background(bg)
            .border(1.dp, border, ItemShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp).scale(scale),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

val TomiloBottomBarContentGap = 88.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun tomiloTopBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = TomiloBg,
    scrolledContainerColor = TomiloSurface.copy(alpha = 0.96f),
    titleContentColor = TomiloText,
    actionIconContentColor = TomiloText,
    navigationIconContentColor = TomiloText,
)
