package com.pharmalink.feature.cart.presentation

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
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.domain.model.CartItem
import com.pharmalink.domain.model.StockStatus
import com.pharmalink.designsystem.components.MedicineImagePlaceholder
import com.pharmalink.designsystem.theme.dimens

/**
 * Cart Item Card Component
 * Premium card for each medicine item in cart with quantity controls
 */
@Composable
fun CartItemCard(
    item: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val scale by animateFloatAsState(
        targetValue = if (item.stockStatus == StockStatus.OUT_OF_STOCK) 0.95f else 1f,
        animationSpec = tween(200),
        label = "scale"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { /* TODO: Navigate to medicine details */ },
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
            // Header with medicine info and remove button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                // Medicine Info
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                ) {
                    // Medicine Image
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        MedicineImagePlaceholder(
                            imageUrl = item.medicineImageUrl,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    
                    // Medicine Details
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.medicineName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        
                        item.medicineSubtitle.takeIfNotEmpty()?.let { subtitle ->
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        
                        Spacer(Modifier.height(d.spaceS))
                        
                        // Warehouse and Supplier Info
                        Text(
                            text = "${item.selectedWarehouseName} • ${item.selectedSupplierName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                        
                        // Status Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp),
                        ) {
                            CartStatusChip(
                                status = item.stockStatus,
                            )
                            if (item.isColdChain) {
                                CartStatusChip(
                                    text = stringResource(R.string.cart_chip_cold_chain),
                                    color = Color(0xFF2196F3),
                                )
                            }
                            if (item.isFastDelivery) {
                                CartStatusChip(
                                    text = stringResource(R.string.cart_chip_fast_delivery),
                                    color = Color(0xFF4CAF50),
                                )
                            }
                        }
                    }
                }
                
                // Remove Button
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Remove,
                        contentDescription = stringResource(R.string.cart_remove_cd),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            
            Spacer(Modifier.height(d.spaceM))
            
            // Quantity Controls and Additional Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Batch/Packaging Info
                Column(modifier = Modifier.weight(1f)) {
                    item.batchInfo?.let { batch ->
                        Text(
                            text = stringResource(R.string.cart_batch_label, batch),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item.packagingInfo?.let { packaging ->
                        Text(
                            text = stringResource(R.string.cart_packaging_label, packaging),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                
                // Quantity Selector
                QuantitySelector(
                    quantity = item.quantity,
                    unit = item.unit,
                    onQuantityChange = onQuantityChange,
                    isOutOfStock = item.stockStatus == StockStatus.OUT_OF_STOCK,
                    modifier = Modifier.width(160.dp),
                )
            }
        }
    }
}

@Composable
private fun CartStatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = color,
        )
    }
}

@Composable
private fun CartStatusChip(
    status: StockStatus,
    modifier: Modifier = Modifier,
) {
    val (text, color) = when (status) {
        StockStatus.IN_STOCK -> stringResource(R.string.cart_stock_in_stock) to Color(0xFF4CAF50)
        StockStatus.LOW_STOCK -> stringResource(R.string.cart_stock_low) to Color(0xFFFF9800)
        StockStatus.OUT_OF_STOCK -> stringResource(R.string.cart_stock_out) to Color(0xFFE57373)
    }
    
    CartStatusChip(
        text = text,
        color = color,
        modifier = modifier,
    )
}

private fun String.takeIfNotEmpty(): String? = if (this.isNotEmpty()) this else null
