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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.R
import com.pharmalink.designsystem.stitch.StitchTheme
import com.pharmalink.designsystem.stitch.theme.dimens

@Composable
fun StitchActivityCard(
    title: String,
    subtitle: String,
    time: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val d = MaterialTheme.dimens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(d.radiusM)
            )
            .padding(d.spaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(R.string.activity_icon_description),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(d.iconL)
        )
        Spacer(modifier = Modifier.width(d.spaceM))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StitchActivityCardPreview() {
    StitchTheme {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.spaceM)) {
            StitchActivityCard(
                title = "تمت الموافقة على طلب جديد",
                subtitle = "طلب رقم #123456789",
                time = "منذ 5 دقائق",
                icon = Icons.Default.Info
            )
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.spaceS))
            StitchActivityCard(
                title = "تم تحديث حالة المخزون",
                subtitle = "المستودع الرئيسي",
                time = "منذ ساعة",
                icon = Icons.Default.Info
            )
        }
    }
}
