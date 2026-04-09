package com.pharmalink.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.dimens

enum class StatusTone {
    Pending,
    Success,
    Warning,
    Urgent,
    Neutral,
}

@Composable
fun StatusChip(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val (bg, fg) = when (tone) {
        StatusTone.Pending -> Color(0xFFE0F5F3) to Color(0xFF006B62)
        StatusTone.Success -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        StatusTone.Warning -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        StatusTone.Urgent -> Color(0xFFFEE2E2) to PremiumUrgent
        StatusTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        modifier = modifier
            .background(bg, MaterialTheme.shapes.medium)
            .padding(horizontal = d.spaceM, vertical = d.spaceXS),
    )
}
