package ru.tomilo.lib.mobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.tomilo.lib.mobile.R
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
    /** Center logo button like site mobile-footer main */
    val isMain: Boolean = false,
)

private val BarShape = RoundedCornerShape(20.dp)
private val ItemShape = RoundedCornerShape(12.dp)

/**
 * Floating glass bottom nav — overlays content (content scrolls underneath).
 * Center logo sits in its own layer so it is never clipped by the pill.
 */
@Composable
fun TomiloBottomBar(
    tabs: List<TomiloTabItem>,
    currentRoute: String,
    onTabClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mainTab = tabs.firstOrNull { it.isMain }
    val sideTabs = tabs.filter { !it.isMain }
    // Split side tabs around center logo: left half / right half
    val leftTabs = if (mainTab != null) sideTabs.take(sideTabs.size / 2) else sideTabs
    val rightTabs = if (mainTab != null) sideTabs.drop(sideTabs.size / 2) else emptyList()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 14.dp, end = 14.dp, top = 20.dp, bottom = 10.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Glass pill — no clip of children that stick out; logo drawn above
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = BarShape,
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.45f),
                )
                .clip(BarShape)
                .background(TomiloGlass)
                .border(1.dp, TomiloGlassBorder, BarShape)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leftTabs.forEach { tab ->
                NavTabItem(
                    label = tab.label,
                    icon = if (currentRoute == tab.route) tab.selectedIcon else tab.icon,
                    selected = currentRoute == tab.route,
                    onClick = { onTabClick(tab.route) },
                    modifier = Modifier.weight(1f),
                )
            }
            // Reserve center slot for raised logo (empty in-row)
            if (mainTab != null) {
                Spacer(Modifier.weight(1f).height(44.dp))
            }
            rightTabs.forEach { tab ->
                NavTabItem(
                    label = tab.label,
                    icon = if (currentRoute == tab.route) tab.selectedIcon else tab.icon,
                    selected = currentRoute == tab.route,
                    onClick = { onTabClick(tab.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Raised center logo — outside the clipped glass row so it never clips
        if (mainTab != null) {
            MainLogoTab(
                selected = currentRoute == mainTab.route,
                contentDescription = mainTab.label,
                onClick = { onTabClick(mainTab.route) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 0.dp),
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
        targetValue = if (selected) 1.06f else 1f,
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
            .padding(horizontal = 2.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier
                .size(22.dp)
                .scale(scale),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.1.sp,
        )
    }
}

@Composable
private fun MainLogoTab(
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "mainScale",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) TomiloPrimary else TomiloPrimary.copy(alpha = 0.4f),
        label = "mainBorder",
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(58.dp)
                    .background(
                        TomiloPrimary.copy(alpha = 0.2f),
                        CircleShape,
                    ),
            )
        }
        Box(
            modifier = Modifier
                .size(50.dp)
                .scale(scale)
                .shadow(
                    elevation = if (selected) 12.dp else 8.dp,
                    shape = CircleShape,
                    ambientColor = TomiloPrimary.copy(alpha = 0.35f),
                    spotColor = TomiloPrimary.copy(alpha = 0.4f),
                )
                .clip(CircleShape)
                .background(TomiloSurface)
                .border(2.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_tomilo_ring),
                contentDescription = contentDescription,
                modifier = Modifier.size(42.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

/** Approximate height reserved under scroll content so last items clear the floating bar. */
val TomiloBottomBarContentGap = 96.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun tomiloTopBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = TomiloBg,
    scrolledContainerColor = TomiloSurface.copy(alpha = 0.96f),
    titleContentColor = TomiloText,
    actionIconContentColor = TomiloText,
    navigationIconContentColor = TomiloText,
)
