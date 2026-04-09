package com.pharmalink.feature.resources.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Warehouse Status Types
 */
enum class WarehouseStatusType {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK,
    COLD_CHAIN,
    FAST_DELIVERY,
    HIGH_RELIABILITY
}

/**
 * Warehouse Status Chip Component
 * Reusable status chips with soft fills and icons
 */
@Composable
fun WarehouseStatusChip(
    status: WarehouseStatusType,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when (status) {
        WarehouseStatusType.IN_STOCK -> Color(0xFF4CAF50)
        WarehouseStatusType.LOW_STOCK -> Color(0xFFFF9800)
        WarehouseStatusType.OUT_OF_STOCK -> Color(0xFFE57373)
        WarehouseStatusType.COLD_CHAIN -> Color(0xFF2196F3)
        WarehouseStatusType.FAST_DELIVERY -> Color(0xFF4CAF50)
        WarehouseStatusType.HIGH_RELIABILITY -> Color(0xFF4CAF50)
    }
    
    val textColor = when (status) {
        WarehouseStatusType.IN_STOCK -> Color(0xFF2E7D32)
        WarehouseStatusType.LOW_STOCK -> Color(0xFFF57C00)
        WarehouseStatusType.OUT_OF_STOCK -> Color(0xFFD32F2F)
        WarehouseStatusType.COLD_CHAIN -> Color(0xFF1976D2)
        WarehouseStatusType.FAST_DELIVERY -> Color(0xFF2E7D32)
        WarehouseStatusType.HIGH_RELIABILITY -> Color(0xFF2E7D32)
    }
    
    val icon = when (status) {
        WarehouseStatusType.IN_STOCK -> Icons.Outlined.Star
        WarehouseStatusType.LOW_STOCK -> Icons.Outlined.Speed
        WarehouseStatusType.OUT_OF_STOCK -> Icons.Outlined.AcUnit
        WarehouseStatusType.COLD_CHAIN -> Icons.Outlined.AcUnit
        WarehouseStatusType.FAST_DELIVERY -> Icons.Outlined.LocalShipping
        WarehouseStatusType.HIGH_RELIABILITY -> Icons.Outlined.Star
    }
    
    val statusText = when (status) {
        WarehouseStatusType.IN_STOCK -> "متوفر"
        WarehouseStatusType.LOW_STOCK -> "مخزون منخفض"
        WarehouseStatusType.OUT_OF_STOCK -> "نفد المخزون"
        WarehouseStatusType.COLD_CHAIN -> "سلسلة باردة"
        WarehouseStatusType.FAST_DELIVERY -> "توصيل سريع"
        WarehouseStatusType.HIGH_RELIABILITY -> "موثوقية عالية"
    }
    
    Box(
        modifier = modifier
            .background(
                color = backgroundColor.copy(alpha = 0.12f), // Soft fill
                shape = RoundedCornerShape(50.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = textColor,
            )
        }
    }
}
