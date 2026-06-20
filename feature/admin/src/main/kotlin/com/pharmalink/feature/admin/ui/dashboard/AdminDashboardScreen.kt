package com.pharmalink.feature.admin.ui.dashboard

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pending
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.stitch.components.StitchButton
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Warehouse
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.PharmaBlue100
import com.pharmalink.designsystem.theme.PharmaBlue500
import com.pharmalink.designsystem.theme.PharmaBlue700
import com.pharmalink.designsystem.theme.PharmaBlue900
import com.pharmalink.designsystem.theme.PharmaNeutral100
import com.pharmalink.designsystem.theme.PharmaNeutral400
import com.pharmalink.designsystem.theme.PharmaNeutral600
import com.pharmalink.designsystem.theme.PharmaNeutral900
import com.pharmalink.designsystem.theme.PremiumPrimary
import com.pharmalink.designsystem.theme.PremiumAccent
import com.pharmalink.designsystem.theme.PharmaSuccess
import com.pharmalink.designsystem.theme.PharmaError
import com.pharmalink.designsystem.theme.PremiumUrgent
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.pharmalink.designsystem.theme.PharmaGradients
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.components.DashboardWelcomeCard
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaCard
import com.pharmalink.designsystem.components.PharmaButtonStyle
import com.pharmalink.designsystem.components.PharmaSkeletonLine
import com.pharmalink.designsystem.components.PharmaStateView
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.theme.PharmaTheme
import com.pharmalink.designsystem.theme.StatusActive
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.designsystem.utils.CollectEffect
import com.pharmalink.feature.admin.ui.components.AdminProfileAvatarButton
import com.pharmalink.feature.admin.R
import com.pharmalink.designsystem.R as DsR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToAddFacility: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToPharmacies: () -> Unit,
    onNavigateToWarehouses: () -> Unit,
    onNavigateToAuditLog: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToOrderDetail: (String) -> Unit,
    onShowAdminMenu: () -> Unit,
    onShowReportDialog: () -> Unit,
    profileImageUrl: String? = null,
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
            AdminDashboardEffect.NavigateToProfile -> onNavigateToProfile()
            AdminDashboardEffect.NavigateToUsers -> onNavigateToUsers()
            AdminDashboardEffect.NavigateToPharmacies -> onNavigateToPharmacies()
            AdminDashboardEffect.NavigateToWarehouses -> onNavigateToWarehouses()
            AdminDashboardEffect.NavigateToAllActivities -> onNavigateToAuditLog()
            AdminDashboardEffect.NavigateToOrders -> onNavigateToOrders()
            is AdminDashboardEffect.NavigateToOrderDetail -> onNavigateToOrderDetail(effect.orderId)
            AdminDashboardEffect.ShowAdminMenu -> onShowAdminMenu()
            AdminDashboardEffect.NavigateToReports -> onShowReportDialog()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AdminDashboardContent(
            state = state,
            onAction = viewModel::onAction,
            snackbarHostState = snackbarHostState,
            profileImageUrl = profileImageUrl,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminDashboardContent(
    state: AdminDashboardUiState,
    onAction: (AdminDashboardAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    profileImageUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ClinicalCanvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(DsR.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = PharmaBlue900,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    },
                    navigationIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(d.spaceM))
                            AdminProfileAvatarButton(
                                profileImageUrl = profileImageUrl,
                                contentDescription = stringResource(R.string.admin_profile_cd),
                                onClick = { onAction(AdminDashboardAction.OnProfileClicked) },
                                size = 36.dp
                            )
                            IconButton(onClick = { onAction(AdminDashboardAction.OnNotificationsClicked) }) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = stringResource(R.string.admin_notifications_cd),
                                    tint = PharmaNeutral600,
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { onAction(AdminDashboardAction.OnMenuClicked) }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.admin_menu_cd),
                                tint = PharmaNeutral600,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White,
                    ),
                )
                HorizontalDivider(color = PharmaNeutral100)
            }
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingContent(modifier = Modifier.padding(padding))
            state.contentError.isNotEmpty() -> ErrorContent(
                message = state.contentError.ifEmpty { stringResource(R.string.admin_dashboard_loading_failed_message) },
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
        // Welcome Text
        item {
            Column(verticalArrangement = Arrangement.spacedBy(d.spaceXXS)) {
                Text(
                    text = "أهلاً بك د. ${state.adminName.substringBefore(" ").ifEmpty { "أحمد" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PharmaNeutral600,
                )
            }
        }

        // Primary Actions
        item {
            PrimaryActionsRow(onAction = onAction)
        }

        // Banner Stats
        item {
            AdminStatsBanner(
                totalUsers = state.totalUsers,
                totalOrders = state.totalOrders,
                totalFacilities = state.totalPharmacies + state.totalWarehouses
            )
        }

        // Statistics Grid
        item {
            StatisticsGrid(
                totalUsers = state.totalUsers,
                totalPharmacies = state.totalPharmacies,
                totalWarehouses = state.totalWarehouses,
                totalOrders = state.totalOrders,
                onAction = onAction,
            )
        }

        // Orders Breakdown
        item {
            OrdersBreakdownSection(
                b2cOrders = state.b2cOrdersCount,
                b2bOrders = state.b2bOrdersCount,
                urgentOrders = state.urgentOrdersCount,
                pendingOrders = state.pendingOrdersCount,
                confirmedOrders = state.confirmedOrdersCount,
                deliveredOrders = state.deliveredOrdersCount,
                onAction = onAction,
            )
        }

        // System Health
        item {
            SystemHealthCard(
                healthPercent = state.systemHealthPercent,
                healthStatus = state.systemHealthStatus,
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
private fun AdminStatsBanner(
    totalUsers: Int,
    totalOrders: Int,
    totalFacilities: Int
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        color = PharmaBlue50,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceL)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp), horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "أهلاً بك في صيدليتي",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PremiumPrimary
                    )
                    Text(
                        text = "لوحة تحكم موحدة للمنشآت والطلبات",
                        style = MaterialTheme.typography.labelSmall,
                        color = PharmaNeutral600
                    )
                }
                Icon(
                    painter = painterResource(id = DsR.drawable.ic_sydaliti_mark),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(64.dp)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM)
            ) {
                BannerStatItem(label = "المستخدمون", value = formatNumber(totalUsers), modifier = Modifier.weight(1f))
                BannerStatItem(label = "الطلبات", value = formatNumber(totalOrders), modifier = Modifier.weight(1f))
                BannerStatItem(label = "المنشآت", value = formatNumber(totalFacilities), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BannerStatItem(label: String, value: String, modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d.spaceXXS, Alignment.Start)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = PharmaNeutral600)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PremiumPrimary)
    }
}

private fun formatNumber(number: Int): String {
    return if (number >= 1000) {
        "%,d".format(number)
    } else {
        number.toString()
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
        StitchButton(
            onClick = { onAction(AdminDashboardAction.OnAddFacilityClicked) },
            modifier = Modifier.weight(1.3f).height(48.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(d.spaceS))
            Text(stringResource(R.string.admin_dashboard_add_facility), fontWeight = FontWeight.Bold)
        }
        
        PharmaButton(
            text = "التقارير قريبًا",
            onClick = {},
            enabled = false,
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
                title = "إجمالي المستخدمين",
                value = formatNumber(totalUsers),
                modifier = Modifier.weight(1f),
                onClick = { onAction(AdminDashboardAction.OnUsersCardClicked) },
            )
            
            StatCard(
                title = "إجمالي الصيدليات",
                value = formatNumber(totalPharmacies),
                modifier = Modifier.weight(1f),
                onClick = { onAction(AdminDashboardAction.OnPharmaciesCardClicked) },
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            StatCard(
                title = "إجمالي الطلبات",
                value = formatNumber(totalOrders),
                modifier = Modifier.weight(1f),
                onClick = { onAction(AdminDashboardAction.OnOrdersCardClicked) },
            )
            
            StatCard(
                title = "إجمالي المستودعات",
                value = formatNumber(totalWarehouses),
                modifier = Modifier.weight(1f),
                onClick = { onAction(AdminDashboardAction.OnWarehousesCardClicked) },
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
    
    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(d.radiusXL),
        color = PharmaBlue50.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, PharmaBlue100)
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = PharmaNeutral600,
                textAlign = TextAlign.Start
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = PremiumPrimary,
                textAlign = TextAlign.Start
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
                text = stringResource(R.string.admin_dashboard_view_all),
                style = MaterialTheme.typography.labelLarge,
                color = PharmaBlue500,
                modifier = Modifier.clickable {
                    onAction(AdminDashboardAction.OnViewAllRequestsClicked)
                },
            )
            Text(
                text = stringResource(R.string.admin_dashboard_pending_requests),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PharmaNeutral900,
            )
        }
        
        requests.take(2).forEach { request ->
            PendingRequestCard(
                request = request,
                onClick = {
                    onAction(AdminDashboardAction.OnPendingRequestClicked(request.id, request.type))
                },
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
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(d.radiusXL),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, PharmaNeutral100)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(d.radiusL),
                color = when (request.type) {
                    RequestType.FACILITY -> PharmaBlue50
                    RequestType.ORDER -> Color(0xFFE3F2FD)
                    else -> PharmaNeutral100
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (request.type) {
                            RequestType.FACILITY -> if (request.title.contains("صيدلية")) 
                                Icons.Outlined.LocalPharmacy else Icons.Outlined.Warehouse
                            RequestType.ORDER -> Icons.Outlined.Inventory2
                            RequestType.USER -> Icons.Outlined.Person
                            else -> Icons.Default.Pending
                        },
                        contentDescription = null,
                        tint = when (request.type) {
                            RequestType.FACILITY -> PremiumPrimary
                            RequestType.ORDER -> PharmaBlue500
                            else -> PharmaNeutral600
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = request.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = PharmaNeutral900,
                    textAlign = TextAlign.Start
                )
                Text(
                    text = "${request.subtitle} • ${request.timestamp}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PharmaNeutral600,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = PharmaNeutral400,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SystemHealthCard(
    healthPercent: Int,
    healthStatus: String,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val animatedProgress by animateFloatAsState(
        targetValue = (healthPercent.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = 500),
        label = "admin_system_health_progress",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, PharmaNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(d.radiusM),
                    color = PharmaSuccess.copy(alpha = 0.12f),
                    contentColor = PharmaSuccess
                ) {
                    Text(
                        text = healthStatus,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceXXS)
                    )
                }
                Text(
                    text = stringResource(R.string.admin_dashboard_system_health),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PharmaNeutral900,
                )
            }
            
            Text(
                text = "$healthPercent%",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = PremiumPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = PremiumPrimary,
                trackColor = PharmaNeutral100,
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
                text = stringResource(R.string.admin_dashboard_view_all),
                style = MaterialTheme.typography.labelLarge,
                color = PharmaBlue500,
                modifier = Modifier.clickable {
                    onAction(AdminDashboardAction.OnViewAllActivitiesClicked)
                },
            )
            Text(
                text = stringResource(R.string.admin_dashboard_recent_activities),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PharmaNeutral900,
            )
        }
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(d.radiusXXL),
            color = Color.White,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, PharmaNeutral100)
        ) {
            Column {
                activities.take(3).forEachIndexed { index, activity ->
                    ActivityRow(activity = activity)
                    if (index < activities.take(3).lastIndex) {
                        HorizontalDivider(color = PharmaNeutral100)
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
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = when (activity.status) {
                ActivityStatus.SUCCESS -> PharmaSuccess.copy(alpha = 0.12f)
                ActivityStatus.FAILED -> PharmaError.copy(alpha = 0.12f)
                ActivityStatus.PENDING -> PharmaBlue100
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when (activity.status) {
                        ActivityStatus.SUCCESS -> Icons.Default.CheckCircle
                        ActivityStatus.FAILED -> Icons.Default.Error
                        ActivityStatus.PENDING -> Icons.Default.Pending
                    },
                    contentDescription = null,
                    tint = when (activity.status) {
                        ActivityStatus.SUCCESS -> StatusActive
                        ActivityStatus.FAILED -> PharmaError
                        ActivityStatus.PENDING -> PharmaBlue500
                    },
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(d.spaceXS),
        ) {
            Text(
                text = activity.action,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = PharmaNeutral900,
            )
            Text(
                text = activity.user,
                style = MaterialTheme.typography.labelSmall,
                color = PharmaNeutral600,
            )
        }
        
        Text(
            text = activity.timestamp,
            style = MaterialTheme.typography.labelSmall,
            color = PharmaNeutral400,
        )
    }
}

@Composable
private fun OrdersBreakdownSection(
    b2cOrders: Int,
    b2bOrders: Int,
    urgentOrders: Int,
    pendingOrders: Int,
    confirmedOrders: Int,
    deliveredOrders: Int,
    onAction: (AdminDashboardAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, PharmaNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceL)
        ) {
            Text(
                text = "تقسيم الطلبات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PharmaNeutral900,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
                OrderBreakdownRow(
                    label1 = "B2C", value1 = formatNumber(b2cOrders),
                    label2 = "B2B", value2 = formatNumber(b2bOrders)
                )
                HorizontalDivider(color = PharmaNeutral100)
                OrderBreakdownRow(
                    label1 = "مستعجل", value1 = formatNumber(urgentOrders),
                    label2 = "معلّق", value2 = formatNumber(pendingOrders)
                )
                HorizontalDivider(color = PharmaNeutral100)
                OrderBreakdownRow(
                    label1 = "مؤكد", value1 = formatNumber(confirmedOrders),
                    label2 = "مسلّم", value2 = formatNumber(deliveredOrders)
                )
            }
        }
    }
}

@Composable
private fun OrderBreakdownRow(
    label1: String, value1: String,
    label2: String, value2: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        OrderBreakdownItem(label = label1, value = value1, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(1.dp).height(24.dp).background(PharmaNeutral100))
        OrderBreakdownItem(label = label2, value = value2, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun OrderBreakdownItem(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PharmaNeutral900)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = PharmaNeutral600)
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
                // activeConnections removed - not available
            ),
            onAction = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

