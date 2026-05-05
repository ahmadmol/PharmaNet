package com.pharmalink.feature.admin.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaButtonStyle
import com.pharmalink.designsystem.components.PharmaCard
import com.pharmalink.designsystem.components.PharmaSkeletonLine
import com.pharmalink.designsystem.components.PharmaStateView
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.theme.PharmaTheme
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.designsystem.utils.CollectEffect
import com.pharmalink.feature.admin.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToAddFacility: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToPharmacies: () -> Unit,
    onNavigateToWarehouses: () -> Unit,
    onNavigateToAuditLog: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    CollectEffect(effect = viewModel.effect) { effect ->
        when (effect) {
            is AdminDashboardEffect.ShowMessage -> {
                snackbarHostState.showSnackbar(effect.message)
            }
            AdminDashboardEffect.NavigateToAddFacility -> onNavigateToAddFacility()
            AdminDashboardEffect.NavigateToNotifications -> onNavigateToNotifications()
            AdminDashboardEffect.NavigateToUsers -> onNavigateToUsers()
            AdminDashboardEffect.NavigateToPharmacies -> onNavigateToPharmacies()
            AdminDashboardEffect.NavigateToWarehouses -> onNavigateToWarehouses()
            AdminDashboardEffect.NavigateToAllActivities -> onNavigateToAuditLog()
            else -> {
                snackbarHostState.showSnackbar("قيد التطوير")
            }
        }
    }

    AdminDashboardContent(
        state = state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminDashboardContent(
    state: AdminDashboardUiState,
    onAction: (AdminDashboardAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.admin_dashboard_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { onAction(AdminDashboardAction.OnMenuClicked) }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.admin_menu_cd),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { onAction(AdminDashboardAction.OnNotificationsClicked) }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = stringResource(R.string.admin_notifications_cd),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                )
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.width(d.spaceM))
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White,
                    ),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingContent(modifier = Modifier.padding(padding))
            state.contentError.isNotEmpty() -> ErrorContent(
                message = state.contentError,
                onRetry = { onAction(AdminDashboardAction.OnRetryClicked) },
                modifier = Modifier.padding(padding),
            )
            else -> SuccessContent(
                state = state,
                onAction = onAction,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(d.spaceL),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        PharmaSkeletonLine(heightDp = 80f)
        PharmaSkeletonLine(heightDp = 120f)
        PharmaSkeletonLine(heightDp = 200f)
        PharmaSkeletonLine(heightDp = 150f)
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(d.spaceL),
    ) {
        PharmaStateView(
            title = stringResource(R.string.admin_dashboard_error),
            subtitle = message,
            tone = PharmaStateTone.Error,
            actionLabel = stringResource(R.string.admin_users_retry),
            onAction = onRetry,
        )
    }
}

@Composable
private fun SuccessContent(
    state: AdminDashboardUiState,
    onAction: (AdminDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(d.spaceL),
        verticalArrangement = Arrangement.spacedBy(d.spaceL),
    ) {
        // Welcome Section
        item {
            WelcomeSection(
                adminName = state.adminName,
            )
        }

        // Primary Actions
        item {
            PrimaryActionsRow(onAction = onAction)
        }

        // Statistics Cards
        item {
            StatisticsGrid(
                totalUsers = state.totalUsers,
                totalPharmacies = state.totalPharmacies,
                totalWarehouses = state.totalWarehouses,
                totalOrders = state.totalOrders,
                onAction = onAction,
            )
        }

        // Pending Requests
        if (state.pendingRequests.isNotEmpty()) {
            item {
                PendingRequestsSection(
                    requests = state.pendingRequests,
                    onAction = onAction,
                )
            }
        }

        // System Health
        item {
            SystemHealthCard(
                healthPercent = state.systemHealthPercent,
                healthStatus = state.systemHealthStatus,
                activeConnections = state.activeConnections,
            )
        }

        // Recent Activities
        if (state.recentActivities.isNotEmpty()) {
            item {
                RecentActivitiesSection(
                    activities = state.recentActivities,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun WelcomeSection(
    adminName: String,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(d.spaceS),
    ) {
        Text(
            text = stringResource(R.string.admin_dashboard_welcome),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = adminName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun PrimaryActionsRow(
    onAction: (AdminDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        PharmaButton(
            text = stringResource(R.string.admin_dashboard_add_facility),
            onClick = { onAction(AdminDashboardAction.OnAddFacilityClicked) },
            modifier = Modifier.weight(1f),
        )
        
        PharmaButton(
            text = stringResource(R.string.admin_dashboard_generate_report),
            onClick = { onAction(AdminDashboardAction.OnGenerateReportClicked) },
            modifier = Modifier.weight(1f),
            style = PharmaButtonStyle.Outlined,
        )
    }
}

@Composable
private fun StatisticsGrid(
    totalUsers: Int,
    totalPharmacies: Int,
    totalWarehouses: Int,
    totalOrders: Int,
    onAction: (AdminDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            StatCard(
                title = stringResource(R.string.admin_dashboard_total_users),
                value = totalUsers.toString(),
                modifier = Modifier.weight(1f),
                onClick = { onAction(AdminDashboardAction.OnUsersCardClicked) },
            )
            
            StatCard(
                title = stringResource(R.string.admin_dashboard_total_pharmacies),
                value = totalPharmacies.toString(),
                modifier = Modifier.weight(1f),
                onClick = { onAction(AdminDashboardAction.OnPharmaciesCardClicked) },
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            StatCard(
                title = stringResource(R.string.admin_dashboard_total_warehouses),
                value = totalWarehouses.toString(),
                modifier = Modifier.weight(1f),
                onClick = { onAction(AdminDashboardAction.OnWarehousesCardClicked) },
            )
            
            StatCard(
                title = stringResource(R.string.admin_dashboard_total_orders),
                value = formatLargeNumber(totalOrders),
                modifier = Modifier.weight(1f),
                onClick = { onAction(AdminDashboardAction.OnOrdersCardClicked) },
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val d = MaterialTheme.dimens

    PharmaCard(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            onClick = onClick,
        ),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        elevationDp = 2f,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PendingRequestsSection(
    requests: List<PendingRequestModel>,
    onAction: (AdminDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.admin_dashboard_pending_requests),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            
            Text(
                text = stringResource(R.string.admin_dashboard_view_all),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    onAction(AdminDashboardAction.OnViewAllRequestsClicked)
                },
            )
        }
        
        requests.forEach { request ->
            PendingRequestCard(
                request = request,
                onClick = { onAction(AdminDashboardAction.OnPendingRequestClicked(request.id)) },
            )
        }
    }
}

@Composable
private fun PendingRequestCard(
    request: PendingRequestModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Pending,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(d.spaceXS),
            ) {
                Text(
                    text = request.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = request.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = request.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SystemHealthCard(
    healthPercent: Int,
    healthStatus: String,
    activeConnections: Int,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    PharmaCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        elevationDp = 2f,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Text(
                text = stringResource(R.string.admin_dashboard_system_health),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$healthPercent%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                
                Text(
                    text = healthStatus,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF10B981),
                )
            }
            
            LinearProgressIndicator(
                progress = { healthPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF10B981),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            
            Text(
                text = stringResource(R.string.admin_dashboard_active_connections, activeConnections),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentActivitiesSection(
    activities: List<ActivityModel>,
    onAction: (AdminDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.admin_dashboard_recent_activities),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            
            Text(
                text = stringResource(R.string.admin_dashboard_view_all),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    onAction(AdminDashboardAction.OnViewAllActivitiesClicked)
                },
            )
        }
        
        PharmaCard(
            containerColor = MaterialTheme.colorScheme.surface,
            elevationDp = 2f,
        ) {
            Column {
                activities.forEachIndexed { index, activity ->
                    ActivityRow(activity = activity)
                    if (index < activities.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(
    activity: ActivityModel,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(d.spaceL),
        horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (activity.status) {
                ActivityStatus.SUCCESS -> Icons.Default.CheckCircle
                ActivityStatus.FAILED -> Icons.Default.Error
                ActivityStatus.PENDING -> Icons.Default.Pending
            },
            contentDescription = null,
            tint = when (activity.status) {
                ActivityStatus.SUCCESS -> Color(0xFF10B981)
                ActivityStatus.FAILED -> MaterialTheme.colorScheme.error
                ActivityStatus.PENDING -> MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.size(20.dp),
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(d.spaceXS),
        ) {
            Text(
                text = activity.action,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = activity.user,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        
        Text(
            text = activity.timestamp,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatLargeNumber(number: Int): String {
    return when {
        number >= 1000 -> "${number / 1000}.${(number % 1000) / 100}K"
        else -> number.toString()
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun PreviewAdminDashboardScreen() {
    PharmaTheme {
        AdminDashboardContent(
            state = AdminDashboardUiState(
                adminName = "مدير النظام",
                totalUsers = 1284,
                totalPharmacies = 452,
                totalWarehouses = 86,
                totalOrders = 15400,
                pendingRequests = listOf(
                    PendingRequestModel(
                        id = "1",
                        title = "طلب إنشاء صيدلية جديدة",
                        subtitle = "صيدلية النهدي - فرع الرياض الشمالي",
                        timestamp = "منذ ساعتين",
                        type = RequestType.FACILITY,
                    ),
                ),
                recentActivities = listOf(
                    ActivityModel(
                        id = "1",
                        action = "تعديل بيانات مستخدم",
                        user = "سارة الأحمدي",
                        timestamp = "منذ 10 دقائق",
                        status = ActivityStatus.SUCCESS,
                    ),
                ),
                systemHealthPercent = 94,
                systemHealthStatus = "ممتاز",
                activeConnections = 127,
            ),
            onAction = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
