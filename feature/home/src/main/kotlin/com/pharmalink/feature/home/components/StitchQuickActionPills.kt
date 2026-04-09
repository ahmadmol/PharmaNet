package com.pharmalink.feature.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.feature.home.R

private data class QuickPill(
    val id: String,
    val labelRes: Int,
    val icon: ImageVector,
    val isPrimary: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun StitchQuickActionPills(
    onCreateRequest: () -> Unit,
    onOpenWarehouses: () -> Unit,
    onOpenOrders: () -> Unit,
    onEmergencyRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val pills = listOf(
        QuickPill("new", R.string.home_quick_new, Icons.Outlined.AddCircle, true, onCreateRequest),
        QuickPill("wh", R.string.home_quick_warehouses, Icons.Outlined.Warehouse, false, onOpenWarehouses),
        QuickPill("ord", R.string.home_quick_orders, Icons.AutoMirrored.Outlined.ListAlt, false, onOpenOrders),
        QuickPill("em", R.string.home_quick_emergency, Icons.Outlined.Bolt, false, onEmergencyRequest),
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(d.spaceS),
    ) {
        Text(
            text = stringResource(R.string.home_quick_actions_row_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        LazyRow(
            contentPadding = PaddingValues(),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            items(items = pills, key = { it.id }) { pill ->
                StitchQuickPill(
                    label = stringResource(pill.labelRes),
                    icon = pill.icon,
                    isPrimary = pill.isPrimary,
                    onClick = pill.onClick,
                )
            }
        }
    }
}

@Composable
private fun StitchQuickPill(
    label: String,
    icon: ImageVector,
    isPrimary: Boolean,
    onClick: () -> Unit,
) {
    val d = MaterialTheme.dimens
    val bg = if (isPrimary) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val fg = if (isPrimary) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val border = if (isPrimary) {
        0.dp
    } else {
        1.dp
    }
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = bg,
        tonalElevation = if (isPrimary) d.cardElevation else 0.dp,
        shadowElevation = if (isPrimary) d.cardElevation else 0.dp,
        border = if (isPrimary) {
            null
        } else {
            BorderStroke(
                border,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            )
        },
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = MaterialTheme.colorScheme.primary),
                    role = Role.Button,
                    onClick = onClick,
                )
                .padding(horizontal = d.spaceL, vertical = d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(d.iconS),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = fg,
            )
        }
    }
}
