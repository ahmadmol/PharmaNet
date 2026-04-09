package com.pharmalink.feature.tracking.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.feature.tracking.R

@Composable
fun ExtraInfoCard(
    orderNumber: String?,
    warehouseName: String?,
    departureTime: String?,
    deliveryNotes: String?,
    modifier: Modifier = Modifier,
) {
    val hasAnyInfo = orderNumber != null || warehouseName != null || departureTime != null || deliveryNotes != null
    
    if (!hasAnyInfo) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.dimens.spaceL),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM)
        ) {
            Text(
                text = stringResource(R.string.extra_info_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            // Order number
            orderNumber?.let { orderNum ->
                InfoRow(
                    icon = Icons.Outlined.Receipt,
                    label = stringResource(R.string.extra_info_order_number),
                    value = orderNum,
                )
                Spacer(Modifier.height(MaterialTheme.dimens.spaceS))
            }

            // Warehouse name
            warehouseName?.let { warehouse ->
                InfoRow(
                    icon = Icons.Outlined.Warehouse,
                    label = stringResource(R.string.extra_info_warehouse),
                    value = warehouse,
                )
                Spacer(Modifier.height(MaterialTheme.dimens.spaceS))
            }

            // Departure time
            departureTime?.let { time ->
                InfoRow(
                    icon = Icons.Outlined.Schedule,
                    label = stringResource(R.string.extra_info_departure_time),
                    value = time,
                )
                Spacer(Modifier.height(MaterialTheme.dimens.spaceS))
            }

            // Delivery notes
            deliveryNotes?.let { notes ->
                InfoRow(
                    icon = Icons.Outlined.Info,
                    label = stringResource(R.string.extra_info_delivery_notes),
                    value = notes,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
