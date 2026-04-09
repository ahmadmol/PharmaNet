package com.pharmalink.feature.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.designsystem.components.PharmaOutlinedTile
import com.pharmalink.designsystem.components.PharmaScreenScaffold
import com.pharmalink.designsystem.components.PharmaScreenState
import com.pharmalink.designsystem.components.PharmaSectionHeader
import com.pharmalink.designsystem.components.PharmaStateSpec
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.components.PharmaStatusChip
import com.pharmalink.designsystem.components.StatusTone
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.AppNotification
import com.pharmalink.domain.model.NotificationCategory
import com.pharmalink.domain.model.NotificationDestination
import com.pharmalink.domain.model.NotificationType

@Composable
fun NotificationsScreen(
    onBack: () -> Unit = {},
    onNotificationOpen: (AppNotification) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val d = MaterialTheme.dimens

    PharmaScreenScaffold(
        title = stringResource(R.string.notifications_title),
        onBack = onBack,
        navigationContentDescription = stringResource(R.string.notifications_back),
        modifier = modifier,
        actions = {
            if (state.unreadCount > 0) {
                TextButton(onClick = viewModel::markAllRead) {
                    Text(stringResource(R.string.notifications_mark_all_read))
                }
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            FilterRow(
                selected = state.selectedFilter,
                counts = state.filterCounts,
                onSelected = viewModel::selectFilter,
                modifier = Modifier.padding(top = d.spaceS),
            )

            if (state.screenState != ScreenState.Loading) {
                NotificationOverviewCard(
                    state = state,
                    modifier = Modifier.padding(horizontal = d.spaceL),
                )
            }

            PharmaScreenState(
                screenState = state.screenState,
                loading = PharmaStateSpec(
                    title = stringResource(R.string.notifications_title),
                    subtitle = stringResource(R.string.notifications_loading_subtitle),
                    tone = PharmaStateTone.Loading,
                ),
                empty = PharmaStateSpec(
                    title = stringResource(R.string.notifications_empty_title),
                    subtitle = if (state.selectedFilter == NotificationFilter.All) {
                        stringResource(R.string.notifications_empty_subtitle)
                    } else {
                        stringResource(R.string.notifications_empty_filtered_subtitle)
                    },
                    actionLabel = if (state.selectedFilter != NotificationFilter.All) {
                        stringResource(R.string.notifications_show_all)
                    } else {
                        null
                    },
                ),
                error = PharmaStateSpec(
                    title = stringResource(R.string.notifications_title),
                    subtitle = stringResource(R.string.notifications_error_subtitle),
                    tone = PharmaStateTone.Error,
                ),
                offline = PharmaStateSpec(
                    title = stringResource(R.string.notifications_title),
                    subtitle = stringResource(R.string.notifications_offline_subtitle),
                    tone = PharmaStateTone.Offline,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = d.spaceL),
                onEmptyAction = if (state.selectedFilter != NotificationFilter.All) {
                    { viewModel.selectFilter(NotificationFilter.All) }
                } else {
                    null
                },
            ) { sections ->
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = d.spaceL, vertical = d.spaceXS),
                    verticalArrangement = Arrangement.spacedBy(d.spaceM),
                ) {
                    sections.forEach { section ->
                        item(key = section.type.name) {
                            PharmaSectionHeader(
                                title = sectionLabel(section.type),
                                subtitle = sectionSubtitle(section.type),
                            )
                        }
                        items(section.items, key = { it.id }) { notification ->
                            NotificationCard(
                                notification = notification,
                                onMarkRead = { viewModel.markRead(notification.id) },
                                onOpen = {
                                    if (!notification.read) {
                                        viewModel.markRead(notification.id)
                                    }
                                    onNotificationOpen(notification)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationOverviewCard(
    state: NotificationsUiState,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Text(
                text = stringResource(R.string.notifications_summary_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = overviewSubtitle(state),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                PharmaOutlinedTile(
                    title = stringResource(R.string.notifications_summary_total),
                    value = state.totalCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                PharmaOutlinedTile(
                    title = stringResource(R.string.notifications_summary_unread),
                    value = state.unreadCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                PharmaOutlinedTile(
                    title = stringResource(R.string.notifications_summary_attention),
                    value = state.attentionCount.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    selected: NotificationFilter,
    counts: Map<NotificationFilter, Int>,
    onSelected: (NotificationFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val filters = NotificationFilter.entries

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = d.spaceL),
        horizontalArrangement = Arrangement.spacedBy(d.spaceS),
    ) {
        items(filters, key = { it.name }) { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelected(filter) },
                label = { Text(filter.label(counts[filter] ?: 0)) },
            )
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    onMarkRead: () -> Unit,
    onOpen: () -> Unit,
) {
    val d = MaterialTheme.dimens
    val actionable = notification.destination != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                if (actionable) {
                    role = Role.Button
                }
            }
            .clickable(enabled = actionable, onClick = onOpen),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                Icon(
                    imageVector = notification.icon(),
                    contentDescription = null,
                    tint = if (notification.requiresAction) PremiumUrgent else MaterialTheme.colorScheme.primary,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(d.spaceXS))
                    Text(
                        text = notification.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PharmaStatusChip(
                    label = notification.stateLabel(),
                    tone = notification.stateTone(),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PharmaStatusChip(
                    label = notification.categoryLabel(),
                    tone = notification.categoryTone(),
                )
                Text(
                    text = notification.createdAtLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (!notification.read || actionable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                ) {
                    if (!notification.read) {
                        TextButton(onClick = onMarkRead) {
                            Icon(
                                imageVector = Icons.Outlined.MarkEmailRead,
                                contentDescription = null,
                            )
                            Spacer(Modifier.width(d.spaceXS))
                            Text(stringResource(R.string.notifications_mark_read))
                        }
                    }
                    if (actionable) {
                        TextButton(onClick = onOpen) {
                            Text(notification.actionLabel())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationFilter.label(count: Int): String {
    val base = when (this) {
        NotificationFilter.All -> stringResource(R.string.notifications_filter_all)
        NotificationFilter.Attention -> stringResource(R.string.notifications_filter_attention)
        NotificationFilter.Requests -> stringResource(R.string.notifications_filter_requests)
        NotificationFilter.Orders -> stringResource(R.string.notifications_filter_orders)
        NotificationFilter.Warehouses -> stringResource(R.string.notifications_filter_warehouses)
        NotificationFilter.Compliance -> stringResource(R.string.notifications_filter_compliance)
        NotificationFilter.Support -> stringResource(R.string.notifications_filter_support)
    }
    return "$base ($count)"
}

@Composable
private fun overviewSubtitle(state: NotificationsUiState): String = when {
    state.totalCount == 0 -> stringResource(R.string.notifications_empty_subtitle)
    state.selectedFilter == NotificationFilter.Attention -> stringResource(R.string.notifications_attention_subtitle)
    state.unreadCount == 0 -> stringResource(R.string.notifications_all_caught_up)
    else -> stringResource(R.string.notifications_summary_subtitle, state.unreadCount)
}

@Composable
private fun sectionLabel(section: NotificationSectionType): String = when (section) {
    NotificationSectionType.Attention -> stringResource(R.string.notifications_section_attention)
    NotificationSectionType.Unread -> stringResource(R.string.notifications_section_unread)
    NotificationSectionType.Archived -> stringResource(R.string.notifications_section_archived)
}

@Composable
private fun sectionSubtitle(section: NotificationSectionType): String = when (section) {
    NotificationSectionType.Attention -> stringResource(R.string.notifications_section_attention_subtitle)
    NotificationSectionType.Unread -> stringResource(R.string.notifications_section_unread_subtitle)
    NotificationSectionType.Archived -> stringResource(R.string.notifications_section_archived_subtitle)
}

@Composable
private fun AppNotification.categoryLabel(): String = when (category) {
    NotificationCategory.REQUESTS -> stringResource(R.string.notifications_filter_requests)
    NotificationCategory.ORDERS -> stringResource(R.string.notifications_filter_orders)
    NotificationCategory.WAREHOUSES -> stringResource(R.string.notifications_filter_warehouses)
    NotificationCategory.COMPLIANCE -> stringResource(R.string.notifications_filter_compliance)
    NotificationCategory.SUPPORT -> stringResource(R.string.notifications_filter_support)
}

@Composable
private fun AppNotification.actionLabel(): String = when (destination) {
    NotificationDestination.ORDER -> stringResource(R.string.notifications_action_open_order)
    NotificationDestination.REQUEST -> stringResource(R.string.notifications_action_open_request)
    NotificationDestination.WAREHOUSE -> stringResource(R.string.notifications_action_open_warehouse)
    NotificationDestination.COMPLIANCE -> stringResource(R.string.notifications_action_open_compliance)
    NotificationDestination.HELP -> stringResource(R.string.notifications_action_open_help)
    null -> stringResource(R.string.notifications_open)
}

@Composable
private fun AppNotification.stateLabel(): String = when {
    requiresAction -> stringResource(R.string.notifications_status_action_needed)
    read -> stringResource(R.string.notifications_status_read)
    else -> stringResource(R.string.notifications_status_new)
}

private fun AppNotification.categoryTone(): StatusTone = when (category) {
    NotificationCategory.REQUESTS -> if (requiresAction) StatusTone.Urgent else StatusTone.Warning
    NotificationCategory.ORDERS -> StatusTone.Pending
    NotificationCategory.WAREHOUSES -> StatusTone.Neutral
    NotificationCategory.COMPLIANCE -> StatusTone.Warning
    NotificationCategory.SUPPORT -> StatusTone.Success
}

private fun AppNotification.stateTone(): StatusTone = when {
    requiresAction -> StatusTone.Urgent
    read -> StatusTone.Neutral
    else -> StatusTone.Pending
}

private fun AppNotification.icon() = when (type) {
    NotificationType.ALERT -> Icons.Outlined.PriorityHigh
    NotificationType.ORDER_UPDATE -> Icons.Outlined.Assignment
    NotificationType.COMPLIANCE -> Icons.Outlined.Rule
    NotificationType.SUPPORT -> Icons.Outlined.SupportAgent
    NotificationType.INFO -> Icons.Outlined.Inventory2
}
