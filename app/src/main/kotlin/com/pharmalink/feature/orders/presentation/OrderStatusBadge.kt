package com.pharmalink.feature.orders.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pharmalink.domain.model.OrderStatus

/**
 * Order Status Badge Component
 * Reusable status badge with soft fill and clear typography
 */
@Composable
fun OrderStatusBadge(
    status: OrderStatus,
    modifier: Modifier = Modifier,
) {
    val (backgroundColor, textColor) = when (status) {
        OrderStatus.PENDING -> Color(0xFF6B7280) to Color(0xFF5C6BC0) // Muted blue to calm blue
        OrderStatus.APPROVED -> Color(0xFF4CAF50) to Color(0xFF2E7D32) // Green tones
        OrderStatus.DELIVERED -> Color(0xFF4CAF50) to Color(0xFF2E7D32) // Green tones
        OrderStatus.REJECTED -> Color(0xFFE57373) to Color(0xFFEF9A9A) // Soft clinical red
    }
    
    Box(
        modifier = modifier
            .background(
                color = backgroundColor.copy(alpha = 0.12f), // 10-15% opacity
                shape = RoundedCornerShape(50.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = when (status) {
                OrderStatus.PENDING -> "قيد الانتظار"
                OrderStatus.APPROVED -> "موافق عليه"
                OrderStatus.DELIVERED -> "تم التسليم"
                OrderStatus.REJECTED -> "مرفوض"
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = textColor,
        )
    }
}
