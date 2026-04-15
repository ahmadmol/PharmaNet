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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pharmalink.designsystem.R
import com.pharmalink.designsystem.stitch.StitchTheme
import com.pharmalink.designsystem.stitch.theme.dimens
import androidx.compose.foundation.layout.width

@Composable
fun StitchWarehouseCard(
    name: String,
    address: String,
    status: String,
    statusColor: Color,
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
                imageVector = Icons.Default.Warehouse,
                contentDescription = stringResource(R.string.warehouse_icon_description),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(d.iconL)
            )
            Spacer(modifier = Modifier.width(d.spaceS))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            StitchChip(
                text = status,
                containerColor = statusColor.copy(alpha = 0.1f),
                contentColor = statusColor
            )
        }
        Spacer(modifier = Modifier.height(d.spaceS))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = stringResource(R.string.location_icon_description),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(d.iconM)
            )
            Spacer(modifier = Modifier.width(d.spaceXS))
            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StitchWarehouseCardPreview() {
    StitchTheme {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.spaceM)) {
            StitchWarehouseCard(
                name = "المستودع الرئيسي",
                address = "شارع الصناعة، الرياض، السعودية",
                status = stringResource(R.string.warehouse_status_available),
                statusColor = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.spaceS))
            StitchWarehouseCard(
                name = "مستودع الشمال",
                address = "طريق الملك فهد، الرياض، السعودية",
                status = stringResource(R.string.warehouse_status_low_stock),
                statusColor = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.spaceS))
            StitchWarehouseCard(
                name = "مستودع الجنوب",
                address = "طريق الخرج، الرياض، السعودية",
                status = stringResource(R.string.warehouse_status_closed),
                statusColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
