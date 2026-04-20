package com.pharmalink.feature.home.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.components.StatusChip
import com.pharmalink.designsystem.components.StatusTone
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.AppNotification
import com.pharmalink.domain.model.NotificationType
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.RequestPriority
import com.pharmalink.domain.model.RequestStatus
import com.pharmalink.feature.home.R

@Composable
fun StitchActivityFeed(
    recentNotifications: List<AppNotification>,
    recentRequests: List<Request>,
    onOpenRequest: (String) -> Unit,
    onOpenOrders: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        if (recentNotifications.isNotEmpty()) {
            Text(
                text = stringResource(R.string.home_section_notifications),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            recentNotifications.forEach { notification ->
                key(notification.id) {
                    StitchNotificationRow(
                        notification = notification,
                        onClick = onOpenNotifications,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.home_section_activity_stitch),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        recentRequests.forEach { request ->
            key(request.id) {
                StitchRequestActivityRow(
                    request = request,
                    onClick = { onOpenRequest(request.id) },
                )
            }
        }
        Text(
            text = stringResource(R.string.home_go_orders),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = MaterialTheme.colorScheme.primary),
                    role = Role.Button,
                    onClick = onOpenOrders,
                ),
        )
    }
}

@Composable
private fun StitchNotificationRow(
    notification: AppNotification,
    onClick: () -> Unit,
) {
    val d = MaterialTheme.dimens
    val icon = when (notification.type) {
        NotificationType.ALERT -> Icons.Outlined.WarningAmber
        else -> Icons.Outlined.Notifications
    }
    val tint = when (notification.type) {
        NotificationType.ALERT -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = d.cardElevation,
        shadowElevation = d.cardElevation,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onClick,
                )
                .padding(d.spaceM),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = tint.copy(alpha = 0.12f),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .padding(d.spaceS)
                        .size(d.iconM),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(d.spaceXXS))
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(d.spaceXXS))
                Text(
                    text = notification.createdAtLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(d.iconS),
            )
        }
    }
}

@Composable
private fun StitchRequestActivityRow(
    request: Request,
    onClick: () -> Unit,
) {
    val d = MaterialTheme.dimens
    val (chipLabel, chipTone) = requestChipSpec(request)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = d.cardElevation,
        shadowElevation = d.cardElevation,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onClick,
                )
                .padding(d.spaceM),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Assignment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(d.spaceS)
                        .size(d.iconM),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.medicineName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(d.spaceXXS))
                Text(
                    text = buildString {
                        append(request.warehouseName)
                        append(" • ")
                        append(request.updatedAtLabel)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusChip(label = chipLabel, tone = chipTone)
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(d.iconM),
            )
        }
    }
}

@Composable
private fun requestChipSpec(request: Request): Pair<String, StatusTone> {
    val urgentActive = request.priority == RequestPriority.URGENT &&
        request.status != RequestStatus.FULFILLED &&
        request.status != RequestStatus.REJECTED &&
        request.status != RequestStatus.CANCELLED
    if (urgentActive) {
        return stringResource(R.string.home_priority_urgent_chip) to StatusTone.Urgent
    }
    val label = when (request.status) {
        RequestStatus.DRAFT -> stringResource(R.string.home_status_draft)
        RequestStatus.PENDING -> stringResource(R.string.home_status_submitted)
        RequestStatus.IN_PROGRESS -> stringResource(R.string.home_status_under_review)
        RequestStatus.ACCEPTED -> stringResource(R.string.home_status_approved)
        RequestStatus.FULFILLED -> stringResource(R.string.home_status_completed)
        RequestStatus.REJECTED -> stringResource(R.string.home_status_rejected)
        RequestStatus.CANCELLED -> stringResource(R.string.home_status_rejected) // Reuse rejected string for cancelled
    }
    val tone = when (request.status) {
        RequestStatus.FULFILLED -> StatusTone.Success
        RequestStatus.REJECTED -> StatusTone.Warning
        RequestStatus.CANCELLED -> StatusTone.Warning
        RequestStatus.DRAFT -> StatusTone.Neutral
        else -> StatusTone.Pending
    }
    return label to tone
}
