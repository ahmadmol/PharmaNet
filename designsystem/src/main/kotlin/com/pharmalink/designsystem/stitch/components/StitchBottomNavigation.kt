package com.pharmalink.designsystem.stitch.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.stitch.StitchTheme

@Composable
fun StitchBottomNavigation(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp, // No elevation for clean look
        content = content
    )
}

@Composable
fun RowScope.StitchBottomNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    selectedIcon: ImageVector = icon,
    alwaysShowLabel: Boolean = true,
    contentDescription: String? = null
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = if (selected) selectedIcon else icon,
                contentDescription = contentDescription ?: label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        label = { Text(label) },
        modifier = modifier,
        alwaysShowLabel = alwaysShowLabel,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = Color.Transparent // No indicator
        )
    )
}

@Preview(showBackground = true)
@Composable
fun StitchBottomNavigationPreview() {
    StitchTheme {
        StitchBottomNavigation {
            // Example usage (replace with actual navigation items)
            StitchBottomNavigationItem(
                selected = true,
                onClick = { /*TODO*/ },
                icon = Icons.Default.Home,
                label = "الرئيسية",
                contentDescription = "الرئيسية"
            )
            StitchBottomNavigationItem(
                selected = false,
                onClick = { /*TODO*/ },
                icon = Icons.Default.List,
                label = "الطلبات",
                contentDescription = "الطلبات"
            )
            StitchBottomNavigationItem(
                selected = false,
                onClick = { /*TODO*/ },
                icon = Icons.Default.Person,
                label = "الحساب",
                contentDescription = "الحساب"
            )
        }
    }
}
