package com.pharmalink.feature.cart.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.theme.dimens

/**
 * Quantity Selector Component
 * Premium quantity controls with increment/decrement buttons
 */
@Composable
fun QuantitySelector(
    quantity: Int,
    unit: String,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isOutOfStock: Boolean = false,
    minValue: Int = 1,
    maxValue: Int = 999,
) {
    val d = MaterialTheme.dimens
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (isOutOfStock) 0.9f else 1f,
        animationSpec = tween(200),
        label = "scale"
    )
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Decrement Button
        QuantityButton(
            onClick = { 
                if (quantity > minValue && !isOutOfStock) {
                    onQuantityChange(quantity - 1)
                }
            },
            icon = Icons.Outlined.Remove,
            enabled = quantity > minValue && !isOutOfStock,
            interactionSource = interactionSource,
        )
        
        // Quantity Display
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(48.dp)
                .background(
                    color = if (isOutOfStock) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isOutOfStock) "—" else "$quantity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isOutOfStock) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
        
        // Unit Label
        Text(
            text = unit,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        
        // Increment Button
        QuantityButton(
            onClick = { 
                if (quantity < maxValue && !isOutOfStock) {
                    onQuantityChange(quantity + 1)
                }
            },
            icon = Icons.Outlined.Add,
            enabled = quantity < maxValue && !isOutOfStock,
            interactionSource = interactionSource,
        )
    }
}

@Composable
private fun QuantityButton(
    onClick: () -> Unit,
    icon: ImageVector,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    
    Box(
        modifier = modifier
            .size(48.dp)
            .background(
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                },
                shape = CircleShape
            )
            .clip(CircleShape)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(24.dp),
        )
    }
}
