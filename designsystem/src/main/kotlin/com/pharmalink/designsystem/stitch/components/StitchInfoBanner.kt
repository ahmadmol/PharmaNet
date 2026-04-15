package com.pharmalink.designsystem.stitch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.stitch.StitchTheme
import com.pharmalink.designsystem.stitch.theme.dimens

@Composable
fun StitchInfoBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.spaceS), // Slightly rounded corners
        color = MaterialTheme.colorScheme.primaryContainer, // A distinct background color for info
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer // Text color for contrast
    ) {
        Row(
            modifier = Modifier.padding(d.spaceS),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceS)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Information",
                modifier = Modifier.size(d.iconL)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StitchInfoBannerPreview() {
    StitchTheme {
        StitchInfoBanner(
            message = "هذا شريط معلومات يوضح تفاصيل مهمة للمستخدم.",
            modifier = Modifier.padding(16.dp)
        )
    }
}
