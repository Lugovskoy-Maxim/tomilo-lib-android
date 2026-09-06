package ru.tomilo.lib.mobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.tomilo.lib.mobile.ui.theme.TomiloActiveBorder
import ru.tomilo.lib.mobile.ui.theme.TomiloActivePill
import ru.tomilo.lib.mobile.ui.theme.TomiloBg
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
    val badgeCount: Int? = null,
    val hasBadgeDot: Boolean = false,
)

private val BarShape = RoundedCornerShape(28.dp)
private val ItemShape = RoundedCornerShape(20.dp)

@Composable
fun TomiloBottomBar(
    tabs: List<TomiloTabItem>,
    currentRoute: String,
    onTabClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .shadow(
                    elevation = 22.dp,
                    shape = BarShape,
                    ambientColor = Color.Black.copy(alpha = 0.72f),
                    spotColor = TomiloPrimary.copy(alpha = 0.35f),
                )
                .clip(BarShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xF21C1816),
                            Color(0xF713100E),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.03f),
                        ),
                    ),
                    shape = BarShape,
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                NavTabItem(
                    label = tab.label,
                    icon = if (selected) tab.selectedIcon else tab.icon,
                    selected = selected,
                    badgeCount = tab.badgeCount,
                    hasBadgeDot = tab.hasBadgeDot,
                    onClick = {
                        if (!selected) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onTabClick(tab.route)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavTabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    badgeCount: Int? = null,
    hasBadgeDot: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) TomiloPrimary else TomiloMuted.copy(alpha = 0.82f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabColor",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "tabScale",
    )
    val bg by animateColorAsState(
        targetValue = if (selected) TomiloActivePill.copy(alpha = 0.38f) else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabBg",
    )
    val border by animateColorAsState(
        targetValue = if (selected) TomiloActiveBorder.copy(alpha = 0.42f) else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabBorder",
    )

    Column(
        modifier = modifier
            .clip(ItemShape)
            .background(bg)
            .border(1.dp, border, ItemShape)
            .clickable(
                role = Role.Tab,
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = TomiloPrimary),
            )
            .padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier
                    .size(23.dp)
                    .scale(scale),
            )

            if (badgeCount != null && badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(start = 14.dp, bottom = 10.dp)
                        .clip(CircleShape)
                        .background(TomiloPrimary)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else if (hasBadgeDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(TomiloPrimary),
                )
            }
        }

        Spacer(Modifier.height(3.dp))

        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // Micro glowing pill beneath the active tab
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut() + scaleOut(),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .width(14.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                TomiloPrimary.copy(alpha = 0.5f),
                                TomiloPrimary,
                                TomiloPrimary.copy(alpha = 0.5f),
                            ),
                        ),
                    ),
            )
        }
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
