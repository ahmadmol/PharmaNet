package com.pharmalink.designsystem.stitch.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.stitch.StitchTheme
import com.pharmalink.designsystem.stitch.theme.dimens
import com.pharmalink.designsystem.R

// TODO: Define actual string resources in R.string
// For now, using hardcoded strings in preview and placeholder for actual usage

@Composable
fun StitchQuickActionItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val d = MaterialTheme.dimens
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // No ripple for this custom clickable
                onClick = onClick
            )
            .padding(d.spaceXS), // Small padding around the item
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Circular background for the icon
        Column(
            modifier = Modifier
                .size(56.dp) // Fixed size for the circular background
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = CircleShape
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(d.iconL) // Icon size
            )
        }
        Spacer(modifier = Modifier.height(d.spaceXS))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun StitchQuickActions(
    onQuickOrderClick: () -> Unit,
    onAddMedicineClick: () -> Unit,
    onContactClick: () -> Unit,
    onReportsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val d = MaterialTheme.dimens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = d.spaceM, vertical = d.spaceS),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Top
    ) {
        StitchQuickActionItem(
            icon = Icons.Default.FastRewind, // Placeholder for quick order
            text = stringResource(R.string.quick_order_action),
            onClick = onQuickOrderClick
        )
        StitchQuickActionItem(
            icon = Icons.Default.Add, // Placeholder for add medicine
            text = stringResource(R.string.add_medicine_action),
            onClick = onAddMedicineClick
        )
        StitchQuickActionItem(
            icon = Icons.Default.Chat, // Placeholder for contact
            text = stringResource(R.string.contact_action),
            onClick = onContactClick
        )
        StitchQuickActionItem(
            icon = Icons.Default.Newspaper, // Placeholder for reports
            text = stringResource(R.string.reports_action),
            onClick = onReportsClick
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StitchQuickActionsPreview() {
    StitchTheme {
        Column {
            Text(
                text = "StitchQuickActions Preview",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            StitchQuickActions(
                onQuickOrderClick = { /*TODO*/ },
                onAddMedicineClick = { /*TODO*/ },
                onContactClick = { /*TODO*/ },
                onReportsClick = { /*TODO*/ }
            )
        }
    }
}
