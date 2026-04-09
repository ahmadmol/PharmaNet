package com.pharmalink.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.theme.PharmaSuccess
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.feature.home.R

@Composable
fun StitchKpiGrid(
    activeOrders: Int,
    urgentRequests: Int,
    completedToday: Int,
    lowStockAlerts: Int,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            StitchKpiCell(
                label = stringResource(R.string.home_kpi_active),
                value = activeOrders.toString(),
                icon = Icons.AutoMirrored.Outlined.Assignment,
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            StitchKpiCell(
                label = stringResource(R.string.home_kpi_urgent),
                value = urgentRequests.toString(),
                icon = Icons.Outlined.Bolt,
                accent = PremiumUrgent,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            StitchKpiCell(
                label = stringResource(R.string.home_kpi_completed),
                value = completedToday.toString(),
                icon = Icons.Outlined.TaskAlt,
                accent = PharmaSuccess,
                modifier = Modifier.weight(1f),
            )
            StitchKpiCell(
                label = stringResource(R.string.home_kpi_stock),
                value = lowStockAlerts.toString(),
                icon = Icons.Outlined.Inventory2,
                accent = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StitchKpiCell(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = d.cardElevation,
        shadowElevation = d.cardElevation,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceM),
            verticalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accent.copy(alpha = 0.12f), MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(d.iconM),
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
