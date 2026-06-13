package com.pharmalink.feature.orders.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.Schedule
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
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import com.pharmalink.designsystem.theme.dimens

/**
 * Enhanced Order Card Component
 * Production-ready order card with clean hierarchy and status progression
 */
@Composable
fun EnhancedOrderCard(
    order: Order,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val progress by animateFloatAsState(
        targetValue = when (order.status) {
            OrderStatus.PENDING,
            OrderStatus.QUOTE_PENDING -> 0.25f
            OrderStatus.CONFIRMED,
            OrderStatus.IN_PROGRESS,
            OrderStatus.READY_FOR_PICKUP,
            OrderStatus.OUT_FOR_DELIVERY -> 0.5f
            OrderStatus.DELIVERED -> 1.0f
            OrderStatus.REJECTED,
            OrderStatus.CANCELLED -> 0.0f
        },
        animationSpec = tween(300),
        label = "progress"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL)
        ) {
            // Header with ID and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "طلب #${order.id}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = order.requestId.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                
                Box(
                    modifier = Modifier.width(80.dp),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    OrderStatusBadge(
                        status = order.status,
                        modifier = Modifier.padding(start = d.spaceS),
                    )
                }
            }
            
            Spacer(Modifier.height(d.spaceM))
            
            // Medicine Info
            Text(
                text = order.medicineName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
            
            Spacer(Modifier.height(d.spaceS))
            
            // Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "${order.quantity} ${order.unit}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = order.warehouseName.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    if (order.isUrgent) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PriorityHigh,
                                contentDescription = null,
                                tint = Color(0xFFE57373), // Urgent red
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "عاجل",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE57373),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(d.spaceM))
            
            // Status Progress Indicator
            if (order.status != OrderStatus.REJECTED) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(2.dp)
                        )
                        .clip(RoundedCornerShape(2.dp)),
                ) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = when (order.status) {
                            OrderStatus.PENDING,
                            OrderStatus.QUOTE_PENDING -> MaterialTheme.colorScheme.primary
                            OrderStatus.CONFIRMED,
                            OrderStatus.IN_PROGRESS,
                            OrderStatus.READY_FOR_PICKUP,
                            OrderStatus.OUT_FOR_DELIVERY -> Color(0xFF4CAF50) // Success green
                            OrderStatus.DELIVERED -> Color(0xFF4CAF50) // Success green
                            OrderStatus.REJECTED,
                            OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
                        },
                    )
                }
            }
            
            Spacer(Modifier.height(d.spaceS))
            
            // Footer with ETA and Tracking
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = formatInstantToDisplay(order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    order.etaLabel?.let { eta ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = eta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
                
                if (order.status == OrderStatus.CONFIRMED || 
                    order.status == OrderStatus.IN_PROGRESS ||
                    order.status == OrderStatus.READY_FOR_PICKUP ||
                    order.status == OrderStatus.OUT_FOR_DELIVERY || 
                    order.status == OrderStatus.DELIVERED) {
                    Icon(
                        imageVector = Icons.Outlined.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

private fun formatInstantToDisplay(instant: java.time.Instant?): String {
    return instant?.let {
        val formatter = java.time.format.DateTimeFormatter
            .ofPattern("yyyy/MM/dd")
            .withZone(java.time.ZoneId.systemDefault())
        formatter.format(it)
    } ?: "-"
}
