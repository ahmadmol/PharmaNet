package com.pharmalink.feature.orders.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.theme.dimens

/**
 * Order Status enum for segmented control
 */
enum class OrderFilterStatus {
    ALL,
    PENDING,
    APPROVED,
    IN_TRANSIT,
    DELIVERED,
    REJECTED
}

/**
 * Status Segmented Control Component
 * Modern tab-like control for filtering orders by status
 */
@Composable
fun OrdersSegmentedControl(
    selectedStatus: OrderFilterStatus,
    onStatusSelected: (OrderFilterStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(50.dp)
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OrderFilterStatus.values().forEach { status ->
            val isSelected = status == selectedStatus
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                },
                animationSpec = tween<Color>(300),
                label = "selectedColor"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                animationSpec = tween<Color>(300),
                label = "textColor"
            )
            
            val statusText = when (status) {
                OrderFilterStatus.ALL -> "كل الطلبات"
                OrderFilterStatus.PENDING -> "قيد المراجعة"
                OrderFilterStatus.APPROVED -> "تم التأكيد"
                OrderFilterStatus.IN_TRANSIT -> "قيد التوصيل"
                OrderFilterStatus.DELIVERED -> "تم التوصيل"
                OrderFilterStatus.REJECTED -> "مرفوض"
            }
            
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(25.dp)
                    )
                    .clickable { onStatusSelected(status) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = textColor,
                )
            }
        }
    }
}
