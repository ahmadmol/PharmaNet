package com.pharmalink.feature.tracking.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.feature.tracking.R
import com.pharmalink.domain.model.DeliveryStatus

@Composable
fun DeliveryTimeline(
    currentStatus: DeliveryStatus,
    modifier: Modifier = Modifier,
) {
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
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceL),
        ) {
            Text(
                text = stringResource(R.string.timeline_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            val timelineSteps = trackingTimelineSteps()
            
            timelineSteps.forEachIndexed { index, step ->
                TimelineItem(
                    step = step,
                    isCompleted = step.status <= currentStatus,
                    isCurrent = step.status == currentStatus,
                    showLine = index < timelineSteps.size - 1,
                )
            }
        }
    }
}

@Composable
private fun TimelineItem(
    step: TimelineStep,
    isCompleted: Boolean,
    isCurrent: Boolean,
    showLine: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isCompleted -> {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.White,
                            )
                        }
                    }
                    isCurrent -> {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.White,
                            )
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Connecting line
            if (showLine) {
                Spacer(Modifier.height(4.dp))
                Column(
                    modifier = Modifier.width(2.dp).height(40.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        },
                        trackColor = Color.Transparent,
                        progress = { 1f },
                    )
                }
            }
        }

        // Step text
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isCompleted || isCurrent -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            step.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class TimelineStep(
    val status: DeliveryStatus,
    val title: String,
    val description: String?,
    val icon: ImageVector,
)

@Composable
private fun trackingTimelineSteps(): List<TimelineStep> {
    return listOf(
        TimelineStep(
            status = DeliveryStatus.PREPARING,
            title = stringResource(R.string.delivery_status_preparing),
            description = stringResource(R.string.timeline_preparing_desc),
            icon = Icons.Outlined.Schedule,
        ),
        TimelineStep(
            status = DeliveryStatus.ASSIGNED,
            title = stringResource(R.string.delivery_status_assigned),
            description = stringResource(R.string.timeline_assigned_desc),
            icon = Icons.Outlined.Person,
        ),
        TimelineStep(
            status = DeliveryStatus.PICKED_UP,
            title = stringResource(R.string.delivery_status_picked_up),
            description = stringResource(R.string.timeline_picked_up_desc),
            icon = Icons.Outlined.LocalShipping,
        ),
        TimelineStep(
            status = DeliveryStatus.IN_TRANSIT,
            title = stringResource(R.string.tracking_timeline_step_in_transit_short),
            description = stringResource(R.string.timeline_in_transit_desc),
            icon = Icons.Outlined.LocalShipping,
        ),
        TimelineStep(
            status = DeliveryStatus.ARRIVING,
            title = stringResource(R.string.delivery_status_arriving),
            description = stringResource(R.string.timeline_arriving_desc),
            icon = Icons.Outlined.Home,
        ),
        TimelineStep(
            status = DeliveryStatus.DELIVERED,
            title = stringResource(R.string.delivery_status_delivered),
            description = stringResource(R.string.timeline_delivered_desc),
            icon = Icons.Outlined.Flag,
        ),
    )
}
