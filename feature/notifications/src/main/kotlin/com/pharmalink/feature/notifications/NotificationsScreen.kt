package com.pharmalink.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.designsystem.components.PharmaScreenState
import com.pharmalink.designsystem.components.PharmaStateSpec
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.PharmaBlue900
import com.pharmalink.designsystem.theme.PharmaNeutral100
import com.pharmalink.designsystem.theme.PharmaNeutral400
import com.pharmalink.designsystem.theme.PharmaNeutral600
import com.pharmalink.designsystem.theme.PharmaNeutral900
import com.pharmalink.designsystem.theme.PremiumPrimary
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.StatusActive
import com.pharmalink.designsystem.theme.StatusInfo
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.AppNotification
import com.pharmalink.domain.model.NotificationCategory
import com.pharmalink.domain.model.NotificationDestination
import com.pharmalink.domain.model.NotificationType
import com.pharmalink.feature.notifications.R as NotifR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit = {},
    onNotificationOpen: (AppNotification) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val d = MaterialTheme.dimens

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.White,
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = "الإشعارات",
                                style = MaterialTheme.typography.titleLarge,
                                color = PharmaBlue900,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = PharmaNeutral600,
                                )
                            }
                        },
                        actions = {
                            Row {
                                if (state.unreadCount > 0) {
                                    TextButton(onClick = viewModel::markAllRead) {
                                        Text(
                                            text = "قراءة الكل",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PharmaNeutral600
                                        )
                                    }
                                }
                                if (state.totalCount > 0) {
                                    TextButton(onClick = viewModel::deleteAllNotifications) {
                                        Text(
                                            text = "حذف الكل",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PharmaNeutral600
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.White,
                        ),
                    )
                    HorizontalDivider(color = PharmaNeutral100)
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                FilterRow(
                    selected = state.selectedFilter,
                    counts = state.filterCounts,
                    onSelected = viewModel::selectFilter,
                    modifier = Modifier.padding(top = d.spaceM),
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
                        title = stringResource(NotifR.string.notifications_title),
                        subtitle = stringResource(NotifR.string.notifications_loading_subtitle),
                        tone = PharmaStateTone.Loading,
                    ),
                    empty = PharmaStateSpec(
                        title = stringResource(NotifR.string.notifications_empty_title),
                        subtitle = if (state.selectedFilter == NotificationFilter.All) {
                            stringResource(NotifR.string.notifications_empty_subtitle)
                        } else {
                            stringResource(NotifR.string.notifications_empty_filtered_subtitle)
                        },
                        actionLabel = if (state.selectedFilter != NotificationFilter.All) {
                            stringResource(NotifR.string.notifications_show_all)
                        } else {
                            null
                        },
                    ),
                    error = PharmaStateSpec(
                        title = stringResource(NotifR.string.notifications_title),
                        subtitle = stringResource(NotifR.string.notifications_error_subtitle),
                        tone = PharmaStateTone.Error,
                    ),
                    offline = PharmaStateSpec(
                        title = stringResource(NotifR.string.notifications_title),
                        subtitle = stringResource(NotifR.string.notifications_offline_subtitle),
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
                            items(section.items, key = { it.id }) { notification ->
                                NotificationCard(
                                    notification = notification,
                                    onMarkRead = { viewModel.markRead(notification.id) },
                                    onDelete = { viewModel.deleteNotification(notification.id) },
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
}

@Composable
private fun NotificationOverviewCard(
    state: NotificationsUiState,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, PharmaNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ملخص الإشعارات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PharmaNeutral900,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                OverviewStatBox(
                    label = "الكل",
                    value = state.totalCount.toString(),
                    color = StatusInfo,
                    modifier = Modifier.weight(1f),
                )
                OverviewStatBox(
                    label = "غير مقروءة",
                    value = state.unreadCount.toString(),
                    color = PremiumPrimary,
                    modifier = Modifier.weight(1f),
                )
                OverviewStatBox(
                    label = "تحتاج إجراء",
                    value = state.attentionCount.toString(),
                    color = PremiumUrgent,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun OverviewStatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(d.radiusL),
        color = color.copy(alpha = 0.05f),
    ) {
        Column(
            modifier = Modifier.padding(vertical = d.spaceM, horizontal = d.spaceS),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = PharmaNeutral600,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                textAlign = TextAlign.Center
            )
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
            val isSelected = filter == selected
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(d.radiusXXL))
                    .clickable { onSelected(filter) },
                color = if (isSelected) PremiumPrimary else Color.White,
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, PharmaNeutral100),
                shape = RoundedCornerShape(d.radiusXXL)
            ) {
                Text(
                    text = filter.label(counts[filter] ?: 0),
                    modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceS),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else PharmaNeutral600
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
) {
    val d = MaterialTheme.dimens
    val actionable = notification.destination != null
    val color = when {
        notification.requiresAction -> PremiumUrgent
        notification.type == NotificationType.ORDER_UPDATE -> StatusActive
        else -> PremiumPrimary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(d.radiusXXL),
        color = if (notification.read) Color.White else PharmaBlue50.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, PharmaNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PharmaNeutral900,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = notification.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = PharmaNeutral600,
                        textAlign = TextAlign.Start,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.2
                    )
                }

                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = color.copy(alpha = 0.1f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = notification.icon(),
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                        if (notification.requiresAction) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(8.dp),
                                shape = CircleShape,
                                color = PremiumUrgent,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                            ) {}
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS)
                ) {
                    if (actionable) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(d.radiusL))
                                .clickable(onClick = onOpen),
                            color = PremiumPrimary,
                            shape = RoundedCornerShape(d.radiusL)
                        ) {
                            Text(
                                text = notification.actionLabel(),
                                modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceS),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    
                    IconButton(onClick = onMarkRead) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (notification.read) StatusActive else PharmaNeutral400,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = PharmaNeutral400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = PharmaNeutral400,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = notification.createdAtLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = PharmaNeutral600
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(d.radiusL),
                        color = PharmaBlue50,
                    ) {
                        Text(
                            text = notification.categoryLabel(),
                            modifier = Modifier.padding(horizontal = d.spaceS, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = PremiumPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationFilter.label(count: Int): String {
    val base = when (this) {
        NotificationFilter.All -> "الكل"
        NotificationFilter.Attention -> "تحتاج إجراء"
        NotificationFilter.Requests -> "الطلبات"
        NotificationFilter.Orders -> "أوامر الشراء"
        NotificationFilter.Warehouses -> "المستودعات"
        NotificationFilter.Compliance -> "الامتثال"
        NotificationFilter.Support -> "الدعم"
    }
    return "$base ($count)"
}

@Composable
private fun AppNotification.categoryLabel(): String = when (category) {
    NotificationCategory.REQUESTS -> "الطلبات"
    NotificationCategory.ORDERS -> "أوامر الشراء"
    NotificationCategory.WAREHOUSES -> "المستودعات"
    NotificationCategory.COMPLIANCE -> "الامتثال"
    NotificationCategory.SUPPORT -> "الدعم"
}

@Composable
private fun AppNotification.actionLabel(): String = when (destination) {
    NotificationDestination.ORDER -> "فتح الطلب"
    NotificationDestination.PHARMACY_CUSTOMER_ORDER -> "فتح الطلب"
    NotificationDestination.REQUEST -> "فتح الطلب"
    NotificationDestination.WAREHOUSE -> "عرض المستودع"
    NotificationDestination.COMPLIANCE -> "فتح الامتثال"
    NotificationDestination.HELP -> "فتح الدعم"
    null -> "فتح"
}

private fun AppNotification.icon() = when (type) {
    NotificationType.ALERT -> Icons.Default.PriorityHigh
    NotificationType.ORDER_UPDATE -> Icons.Default.LocalShipping
    NotificationType.COMPLIANCE -> Icons.Outlined.Rule
    NotificationType.SUPPORT -> Icons.Outlined.SupportAgent
    NotificationType.INFO -> Icons.Outlined.Inventory2
}
