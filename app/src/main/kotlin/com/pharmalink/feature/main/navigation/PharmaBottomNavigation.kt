package com.pharmalink.feature.main.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pharmalink.core.navigation.TopLevelDestination
import com.pharmalink.designsystem.theme.dimens

@Composable
fun PharmaBottomNavigation(
    items: List<TopLevelDestination>,
    selectedItem: Int,
    onTabSelected: (route: String, index: Int) -> Unit,
    modifier: Modifier = Modifier,
    labelOverrides: Map<String, String> = emptyMap(),
) {
    val d = MaterialTheme.dimens

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .padding(horizontal = d.spaceXS),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
    ) {
        items.forEachIndexed { index, item ->
            val label = labelOverrides[item.route] ?: stringResource(item.labelRes)
            NavigationBarItem(
                selected = selectedItem == index,
                onClick = { onTabSelected(item.route, index) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = label,
                    )
                },
                label = {
                    Text(
                        text = label,
                        maxLines = 2,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                    )
                },
                modifier = Modifier.weight(1f),
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}
