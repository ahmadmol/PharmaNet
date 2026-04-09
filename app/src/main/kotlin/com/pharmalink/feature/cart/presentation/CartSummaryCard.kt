package com.pharmalink.feature.cart.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.pharmalink.R
import com.pharmalink.designsystem.theme.dimens

/**
 * Cart Summary Card Component
 * Premium summary with order totals and delivery info
 */
@Composable
fun CartSummaryCard(
    totalItems: Int,
    selectedWarehouseCount: Int,
    estimatedDeliveryTime: String?,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val progress by animateFloatAsState(
        targetValue = if (selectedWarehouseCount > 0) 1f else 0f,
        animationSpec = tween(300),
        label = "progress"
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
        ) {
            // Header
            Text(
                text = stringResource(R.string.cart_summary_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = d.spaceM),
            )
            
            // Progress Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(3.dp)
                    )
                    .clip(RoundedCornerShape(3.dp)),
            ) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                )
            }
            
            Spacer(Modifier.height(d.spaceL))
            
            // Summary Items
            SummaryItem(
                icon = Icons.Outlined.Store,
                label = stringResource(R.string.cart_summary_items_total),
                value = "$totalItems",
                color = MaterialTheme.colorScheme.onSurface,
            )
            
            SummaryItem(
                icon = Icons.Outlined.LocalShipping,
                label = stringResource(R.string.cart_summary_warehouse_count),
                value = "$selectedWarehouseCount",
                color = MaterialTheme.colorScheme.onSurface,
            )
            
            estimatedDeliveryTime?.let { deliveryTime ->
                SummaryItem(
                    icon = Icons.Outlined.AccessTime,
                    label = stringResource(R.string.cart_summary_eta),
                    value = deliveryTime,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
) {
    val d = MaterialTheme.dimens
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = d.spaceS),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}
