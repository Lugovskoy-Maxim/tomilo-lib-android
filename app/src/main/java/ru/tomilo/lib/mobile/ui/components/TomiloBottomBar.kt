package ru.tomilo.lib.mobile.ui.components

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Search
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

private val BarShape = RoundedCornerShape(38.dp)
private val ItemShape = RoundedCornerShape(30.dp)
private val MoreShape = RoundedCornerShape(38.dp)

@Composable
fun TomiloBottomBar(
    tabs: List<TomiloTabItem>,
    moreItems: List<TomiloTabItem> = emptyList(),
    currentRoute: String,
    onTabClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    var moreOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 4.dp)
            .widthIn(max = 520.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .selectableGroup()
                .shadow(
                    elevation = 14.dp,
                    shape = BarShape,
                    ambientColor = Color.Black.copy(alpha = 0.62f),
                    spotColor = Color.Black.copy(alpha = 0.42f),
                )
                .clip(BarShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF2A2B2D),
                            Color(0xFF232426),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.10f),
                    shape = BarShape,
                )
                .padding(horizontal = 5.dp, vertical = 3.dp),
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

        Box {
            MoreNavItem(onClick = { moreOpen = true })
            DropdownMenu(
                expanded = moreOpen,
                onDismissRequest = { moreOpen = false },
                offset = DpOffset(0.dp, 36.dp),
                shape = RoundedCornerShape(20.dp),
                containerColor = TomiloSurface,
                tonalElevation = 12.dp,
                shadowElevation = 16.dp,
            ) {
                val catalog = tabs.firstOrNull { it.label == "Каталог" } ?: tabs.firstOrNull()
                if (catalog != null) {
                    DropdownMenuItem(
                        text = { Text("Поиск", fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        onClick = {
                            moreOpen = false
                            onTabClick(catalog.route)
                        },
                    )
                }
                moreItems.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.label, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(item.icon, contentDescription = null) },
                        onClick = {
                            moreOpen = false
                            onTabClick(item.route)
                        },
                    )
                }
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
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else TomiloText.copy(alpha = 0.70f),
        animationSpec = if (motionEnabled) spring(stiffness = Spring.StiffnessMediumLow) else snap(),
        label = "tabColor",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected && motionEnabled) 1.08f else 1f,
        animationSpec = if (motionEnabled) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            )
        } else snap(),
        label = "tabScale",
    )
    val bg by animateColorAsState(
        targetValue = if (selected) Color(0xFF824A4B) else Color.Transparent,
        animationSpec = if (motionEnabled) spring(stiffness = Spring.StiffnessMediumLow) else snap(),
        label = "tabBg",
    )
    val border by animateColorAsState(
        targetValue = if (selected) TomiloPrimary.copy(alpha = 0.58f) else Color.Transparent,
        animationSpec = if (motionEnabled) spring(stiffness = Spring.StiffnessMediumLow) else snap(),
        label = "tabBorder",
    )

    Box(
        modifier = modifier
            .height(64.dp)
            .clip(if (selected) RoundedCornerShape(29.dp) else ItemShape)
            .background(bg)
            .border(1.dp, border, if (selected) RoundedCornerShape(29.dp) else ItemShape)
            .clickable(
                role = Role.Tab,
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = TomiloPrimary),
            )
            .padding(horizontal = 2.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier
                        .size(if (selected) 26.dp else 24.dp)
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

            Text(
                text = label,
                color = if (selected) Color(0xFFFF8B86) else TomiloText.copy(alpha = 0.70f),
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MoreNavItem(onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .height(64.dp)
            .width(72.dp)
            .shadow(14.dp, MoreShape, ambientColor = Color.Black.copy(alpha = 0.62f), spotColor = Color.Black.copy(alpha = 0.42f))
            .clip(MoreShape)
            .background(Color(0xFF27282A))
            .border(1.dp, Color.White.copy(alpha = 0.10f), MoreShape)
            .clickable(
                role = Role.Tab,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = TomiloPrimary),
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Ещё",
            tint = TomiloText.copy(alpha = 0.70f),
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = "Ещё",
            color = TomiloText.copy(alpha = 0.70f),
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

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
