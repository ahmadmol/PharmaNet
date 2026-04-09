package com.pharmalink.feature.resources.presentation

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
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Warehouse
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
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.Warehouse

/**
 * Enhanced Warehouse Card Component
 * Premium warehouse card with rich information and clean hierarchy
 */
@Composable
fun EnhancedWarehouseCard(
    warehouse: Warehouse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRecommended: Boolean = false,
) {
    val d = MaterialTheme.dimens
    
    // Animate stock level indicator
    val stockProgress by animateFloatAsState(
        targetValue = warehouse.inStockPercent / 100f,
        animationSpec = tween(300),
        label = "stockProgress"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL)
        ) {
            // Header with name and location
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warehouse,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        
                        Column {
                            Text(
                                text = warehouse.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${warehouse.city} • ${warehouse.district}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    
                    // Recommended badge
                    if (isRecommended) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFFFFD54F).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(50.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "موصى به",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFD54F),
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(d.spaceM))
            
            // Stock Availability Section
            Column {
                Text(
                    text = "توافر المخزون",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                
                // Stock Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clip(RoundedCornerShape(4.dp)),
                ) {
                    LinearProgressIndicator(
                        progress = stockProgress,
                        modifier = Modifier.fillMaxWidth(),
                        color = when {
                            warehouse.inStockPercent > 70 -> Color(0xFF4CAF50) // Green
                            warehouse.inStockPercent > 30 -> Color(0xFFFF9800) // Orange
                            else -> Color(0xFFE57373) // Red
                        },
                        trackColor = Color.Transparent,
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                // Stock Status Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (warehouse.inStockPercent > 70) {
                        WarehouseStatusChip(
                            status = WarehouseStatusType.IN_STOCK,
                        )
                    }
                    if (warehouse.lowStockCount > 0) {
                        WarehouseStatusChip(
                            status = WarehouseStatusType.LOW_STOCK,
                        )
                    }
                    if (warehouse.outOfStockCount > 0) {
                        WarehouseStatusChip(
                            status = WarehouseStatusType.OUT_OF_STOCK,
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(d.spaceM))
            
            // Features and Services
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (warehouse.supportsColdChain) {
                            WarehouseStatusChip(
                                status = WarehouseStatusType.COLD_CHAIN,
                            )
                        }
                        WarehouseStatusChip(
                            status = WarehouseStatusType.FAST_DELIVERY,
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(d.spaceM))
            
            // Delivery and Contact Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = warehouse.estimatedDeliveryLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "${warehouse.distanceLabel} • ${warehouse.lastUpdatedLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                
                // CTA Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Icon(
                        imageVector = Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
