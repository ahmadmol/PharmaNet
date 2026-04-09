package com.pharmalink.feature.main.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pharmalink.core.navigation.topLevelDestinations
import com.pharmalink.designsystem.theme.PharmaBlue100
import com.pharmalink.designsystem.theme.PharmaBlue500
import com.pharmalink.designsystem.theme.PharmaNeutral400
import com.pharmalink.designsystem.theme.StitchShellTokens
import com.pharmalink.designsystem.theme.dimens

@Composable
fun PharmaBottomNavigation(
    selectedItem: Int,
    onTabSelected: (route: String, index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = StitchShellTokens.navBarTopRadius,
            topEnd = StitchShellTokens.navBarTopRadius,
        ),
        color = StitchShellTokens.navBarSurface,
        shadowElevation = StitchShellTokens.navBarElevation,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(d.navBarHeight)
                .padding(horizontal = d.navBarPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            topLevelDestinations.take(2).forEachIndexed { index, item ->
                NavTabItem(
                    modifier = Modifier.weight(1f),
                    label = stringResource(item.labelRes),
                    icon = item.icon,
                    isSelected = selectedItem == index,
                    onClick = { onTabSelected(item.route, index) },
                )
            }
            Spacer(modifier = Modifier.width(d.navCenterSpacer))
            topLevelDestinations.takeLast(2).forEachIndexed { index, item ->
                NavTabItem(
                    modifier = Modifier.weight(1f),
                    label = stringResource(item.labelRes),
                    icon = item.icon,
                    isSelected = selectedItem == index + 2,
                    onClick = { onTabSelected(item.route, index + 2) },
                )
            }
        }
    }
}

@Composable
private fun NavTabItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) PharmaBlue500 else PharmaNeutral400,
        animationSpec = tween(250),
        label = "tab_color",
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tab_scale",
    )
    val d = MaterialTheme.dimens

    Column(
        modifier = modifier
            .fillMaxHeight()
            .scale(animatedScale)
            .background(
                color = if (isSelected) PharmaBlue100.copy(alpha = 0.55f) else Color.Transparent,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = d.spaceS, vertical = d.spaceXS),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = animatedColor,
            modifier = Modifier.size(d.navIconSize),
        )
        Spacer(modifier = Modifier.height(d.spaceXS))
        Text(
            text = label,
            color = animatedColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}
