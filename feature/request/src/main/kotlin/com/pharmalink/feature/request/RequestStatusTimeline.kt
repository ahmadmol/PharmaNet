package com.pharmalink.feature.request

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.RequestStatus

/**
 * Timeline step data class
 */
data class TimelineStep(
    val status: RequestStatus,
    val title: String,
    val subtitle: String = "",
    val isCompleted: Boolean = false,
    val isCurrent: Boolean = false,
    val icon: ImageVector,
)

/**
 * Request Status Timeline Component
 * Displays the progression of request status with a visual timeline
 */
@Composable
fun RequestStatusTimeline(
    currentStatus: RequestStatus,
    modifier: Modifier = Modifier,
) {
    val steps = timelineSteps(currentStatus)
    val d = MaterialTheme.dimens

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(d.spaceL),
    ) {
        steps.forEachIndexed { index, step ->
            TimelineStepItem(
                step = step,
                isLast = index == steps.size - 1,
            )
        }
    }
}

@Composable
private fun TimelineStepItem(
    step: TimelineStep,
    isLast: Boolean,
) {
    val d = MaterialTheme.dimens
    val progress by animateFloatAsState(
        targetValue = if (step.isCompleted) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "timeline_progress",
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            step.isCurrent -> primaryColor
                            step.isCompleted -> primaryColor
                            else -> surfaceVariantColor
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = when {
                        step.isCurrent || step.isCompleted -> onPrimaryColor
                        else -> onSurfaceVariantColor
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = d.spaceM),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        step.isCurrent || step.isCompleted -> onSurfaceColor
                        else -> onSurfaceVariantColor
                    },
                )
                if (step.subtitle.isNotEmpty()) {
                    Text(
                        text = step.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariantColor,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            if (!isLast) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .padding(top = d.spaceS),
                ) {
                    val strokeWidth = 2.dp.toPx()
                    val progressWidth = size.width * progress

                    if (progress > 0) {
                        drawLine(
                            start = Offset(0f, size.height / 2),
                            end = Offset(progressWidth, size.height / 2),
                            color = primaryColor,
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                        )
                    }

                    if (progress < 1f) {
                        drawLine(
                            start = Offset(progressWidth, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            color = surfaceVariantColor,
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun timelineSteps(currentStatus: RequestStatus): List<TimelineStep> {
    val allSteps = listOf(
        TimelineStep(
            status = RequestStatus.PENDING,
            title = stringResource(R.string.request_timeline_submitted_title),
            subtitle = stringResource(R.string.request_timeline_submitted_subtitle),
            isCompleted = currentStatus != RequestStatus.DRAFT && currentStatus != RequestStatus.CANCELLED,
            isCurrent = currentStatus == RequestStatus.PENDING,
            icon = Icons.Outlined.Schedule,
        ),
        TimelineStep(
            status = RequestStatus.ACCEPTED,
            title = stringResource(R.string.request_timeline_approved_title),
            subtitle = stringResource(R.string.request_timeline_approved_subtitle),
            isCompleted = currentStatus == RequestStatus.IN_PROGRESS || currentStatus == RequestStatus.FULFILLED,
            isCurrent = currentStatus == RequestStatus.ACCEPTED,
            icon = Icons.Outlined.Check,
        ),
        TimelineStep(
            status = RequestStatus.IN_PROGRESS,
            title = stringResource(R.string.request_timeline_review_title),
            subtitle = stringResource(R.string.request_timeline_review_subtitle),
            isCompleted = currentStatus == RequestStatus.FULFILLED,
            isCurrent = currentStatus == RequestStatus.IN_PROGRESS,
            icon = Icons.Outlined.Check,
        ),
        TimelineStep(
            status = RequestStatus.FULFILLED,
            title = stringResource(R.string.request_timeline_completed_title),
            subtitle = stringResource(R.string.request_timeline_completed_subtitle),
            isCompleted = currentStatus == RequestStatus.FULFILLED,
            isCurrent = currentStatus == RequestStatus.FULFILLED,
            icon = Icons.Outlined.Inventory2,
        ),
    )

    return if (currentStatus == RequestStatus.REJECTED) {
        listOf(
            TimelineStep(
                status = RequestStatus.PENDING,
                title = stringResource(R.string.request_timeline_submitted_title),
                subtitle = stringResource(R.string.request_timeline_submitted_subtitle),
                isCompleted = true,
                isCurrent = false,
                icon = Icons.Outlined.Schedule,
            ),
            TimelineStep(
                status = RequestStatus.REJECTED,
                title = stringResource(R.string.request_timeline_rejected_title),
                subtitle = stringResource(R.string.request_timeline_rejected_subtitle),
                isCompleted = false,
                isCurrent = true,
                icon = Icons.Outlined.Cancel,
            ),
        )
    } else {
        allSteps.filter { it.status != RequestStatus.REJECTED }
    }
}
