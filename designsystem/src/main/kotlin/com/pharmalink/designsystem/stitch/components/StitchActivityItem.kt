package com.pharmalink.designsystem.stitch.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.stitch.StitchTheme
import com.pharmalink.designsystem.stitch.theme.dimens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement

@Composable
fun StitchActivityItem(
    icon: ImageVector,
    title: String,
    time: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val d = MaterialTheme.dimens

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = d.spaceM), // 16dp whitespace as separator
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(d.iconM)
            )
            Spacer(modifier = Modifier.width(d.spaceM))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ArrowForwardIos,
            contentDescription = "Details",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(d.iconS)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StitchActivityItemPreview() {
    StitchTheme {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            StitchActivityItem(
                icon = Icons.Default.ReceiptLong,
                title = "تم استلام طلب جديد #1234",
                time = "منذ 5 دقائق"
            )
            StitchActivityItem(
                icon = Icons.Default.ReceiptLong,
                title = "تم تحديث حالة طلب #5678",
                time = "قبل ساعة"
            )
            StitchActivityItem(
                icon = Icons.Default.ReceiptLong,
                title = "رسالة جديدة من الدعم الفني",
                time = "أمس"
            )
        }
    }
}
