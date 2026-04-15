package com.pharmalink.designsystem.stitch.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pharmalink.designsystem.R
import com.pharmalink.designsystem.stitch.StitchTheme
import com.pharmalink.designsystem.stitch.theme.dimens

@Composable
fun StitchOrderItemCard(
    orderNumber: String,
    date: String,
    status: String,
    totalAmount: String,
    deliveryDate: String? = null,
    warehouseName: String? = null,
    deliveryAddress: String? = null,
    modifier: Modifier = Modifier
) {
    val d = MaterialTheme.dimens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(d.radiusM)
            )
            .padding(d.spaceM)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Numbers,
                contentDescription = stringResource(R.string.order_number_icon_description),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(d.iconM)
            )
            Spacer(modifier = Modifier.width(d.spaceS))
            Text(
                text = "طلب رقم: $orderNumber",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            StitchChip(
                text = status,
                containerColor = when (status) {
                    "قيد التنفيذ" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                    "مكتمل" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    "ملغي" -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                },
                contentColor = when (status) {
                    "قيد التنفيذ" -> MaterialTheme.colorScheme.tertiary
                    "مكتمل" -> MaterialTheme.colorScheme.primary
                    "ملغي" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Spacer(modifier = Modifier.height(d.spaceS))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = stringResource(R.string.order_date_icon_description),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(d.iconM)
            )
            Spacer(modifier = Modifier.width(d.spaceXS))
            Text(
                text = "التاريخ: $date",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(d.spaceM))
            Text(
                text = "الإجمالي: $totalAmount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        deliveryDate?.let { date ->
            Spacer(modifier = Modifier.height(d.spaceS))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = stringResource(R.string.delivery_date_icon_description),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(d.iconM)
                )
                Spacer(modifier = Modifier.width(d.spaceXS))
                Text(
                    text = "تاريخ التسليم: $date",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        warehouseName?.let { name ->
            deliveryAddress?.let { address ->
                Spacer(modifier = Modifier.height(d.spaceS))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = stringResource(R.string.location_icon_description),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(d.iconM)
                    )
                    Spacer(modifier = Modifier.width(d.spaceXS))
                    Column {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StitchOrderItemCardPreview() {
    StitchTheme {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.spaceM)) {
            StitchOrderItemCard(
                orderNumber = "#12345",
                date = "2024-04-10",
                status = "قيد التنفيذ",
                totalAmount = "150.00 ر.س",
                deliveryDate = "2024-04-15",
                warehouseName = "المستودع المركزي",
                deliveryAddress = "شارع التحلية، الرياض"
            )
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.spaceS))
            StitchOrderItemCard(
                orderNumber = "#67890",
                date = "2024-04-01",
                status = "مكتمل",
                totalAmount = "300.00 ر.س"
            )
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.spaceS))
            StitchOrderItemCard(
                orderNumber = "#54321",
                date = "2024-03-25",
                status = "ملغي",
                totalAmount = "75.00 ر.س"
            )
        }
    }
}
