package com.pharmalink.designsystem.stitch.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.stitch.StitchTheme
import com.pharmalink.designsystem.stitch.theme.dimens

@Composable
fun StitchChip(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    val dimens = MaterialTheme.dimens
    Box(
        modifier = modifier
            .background(
                color = containerColor,
                shape = RoundedCornerShape(dimens.radiusS)
            )
            .padding(horizontal = dimens.spaceS, vertical = dimens.spaceXS)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StitchChipPreview() {
    StitchTheme {
        StitchChip(
            text = "متاح",
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.primary
        )
        StitchChip(
            text = "مخزون منخفض",
            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.error
        )
    }
}
