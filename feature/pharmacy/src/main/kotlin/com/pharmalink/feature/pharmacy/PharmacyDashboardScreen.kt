package com.pharmalink.feature.pharmacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.R as DsR
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.dimens

@Composable
fun PharmacyDashboardScreen(
    viewModel: PharmacyDashboardViewModel = hiltViewModel(),
    onNavigateToCustomerOrders: () -> Unit = {},
    onNavigateToRadar: () -> Unit = {},
    onNavigateToWarehouseRequests: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = ClinicalCanvas,
            topBar = {
                DashboardTopBar(onNavigateToNotifications = onNavigateToNotifications)
            },
        ) { innerPadding ->
            DashboardContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                uiState = uiState,
                onRetry = viewModel::load,
                onNavigateToCustomerOrders = onNavigateToCustomerOrders,
                onNavigateToRadar = onNavigateToRadar,
                onNavigateToWarehouseRequests = onNavigateToWarehouseRequests,
                onNavigateToNotifications = onNavigateToNotifications,
                onNavigateToProfile = onNavigateToProfile,
            )
        }
    }
}

@Composable
private fun DashboardTopBar(
    onNavigateToNotifications: () -> Unit,
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = d.cardElevation,
        shape = RoundedCornerShape(bottomStart = d.radiusXXL, bottomEnd = d.radiusXXL),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d.spaceL, vertical = d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(modifier = Modifier.size(48.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.spaceS)) {
                Icon(
                    painter = painterResource(id = DsR.drawable.sydaliti_logo_icon),
                    contentDescription = null,
                    modifier = Modifier.size(d.iconS),
                )
                Text(
                    text = stringResource(R.string.pharmacy_dashboard_home_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            IconButton(onClick = onNavigateToNotifications) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = stringResource(R.string.pharmacy_dashboard_notifications),
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    modifier: Modifier = Modifier,
    uiState: PharmacyDashboardUiState,
    onRetry: () -> Unit,
    onNavigateToCustomerOrders: () -> Unit,
    onNavigateToRadar: () -> Unit,
    onNavigateToWarehouseRequests: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
) {
    val d = MaterialTheme.dimens
    val isLinked = uiState.isPharmacyLinked

    // Gate linkage-dependent navigation (no-op until the pharmacy account is linked)
    val gatedCustomerOrders: () -> Unit = { if (isLinked) onNavigateToCustomerOrders() }
    val gatedRadar: () -> Unit = { if (isLinked) onNavigateToRadar() }
    val gatedWarehouseRequests: () -> Unit = { if (isLinked) onNavigateToWarehouseRequests() }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = d.spaceL, vertical = d.spaceL),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        item {
            Text(
                text = "مساحة عمل الصيدلية",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        item {
            DashboardStatsSection(
                uiState = uiState,
                onRetry = onRetry,
            )
        }

        // Show link blocking card when not linked
        if (!isLinked) {
            item {
                PharmacyLinkBlockingCard(onOpenProfile = onNavigateToProfile)
            }
        }

        item {
            WorkflowCard(
                title = "طلبات العملاء",
                body = "طلبات العملاء المرسلة مباشرة إلى صيدليتك للقبول أو التسعير أو التجهيز.",
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                onClick = gatedCustomerOrders,
            )
        }
        item {
            WorkflowCard(
                title = "الرادار",
                body = "يعرض الطلبات العامة القريبة المرسلة إلى كل الصيدليات فقط.",
                icon = Icons.Outlined.LocationOn,
                onClick = gatedRadar,
            )
        }
        item {
            WorkflowCard(
                title = "طلبات المستودعات",
                body = "تابع طلبات التوريد بين صيدليتك والمستودعات وحالة كل طلب.",
                icon = Icons.Outlined.Inventory2,
                onClick = gatedWarehouseRequests,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(d.spaceM)) {
                CompactWorkflowCard(
                    title = stringResource(R.string.pharmacy_dashboard_notifications),
                    icon = Icons.Outlined.NotificationsNone,
                    onClick = onNavigateToNotifications,
                    modifier = Modifier.weight(1f),
                )
                CompactWorkflowCard(
                    title = stringResource(R.string.pharmacy_dashboard_profile),
                    icon = Icons.Outlined.Person,
                    onClick = onNavigateToProfile,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PharmacyLinkBlockingCard(
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXL),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(d.iconM),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(d.spaceXS),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "حساب الصيدلية غير مرتبط بمنشأة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = "إدارة الطلبات والرادار وطلبات المستودعات متوقفة حتى يتم الربط.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Surface(
                shape = RoundedCornerShape(d.radiusL),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                onClick = onOpenProfile,
            ) {
                Text(
                    text = "الملف",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceS),
                )
            }
        }
    }
}

@Composable
private fun DashboardStatsSection(
    uiState: PharmacyDashboardUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXL),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = d.cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Text(
                text = "ملخص النشاط",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            when {
                uiState.isLoading && !uiState.hasStats -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
                uiState.errorMessage != null && !uiState.hasStats -> {
                    Column(verticalArrangement = Arrangement.spacedBy(d.spaceS)) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = onRetry) {
                            Text(text = "إعادة المحاولة")
                        }
                    }
                }
                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(d.spaceM)) {
                        DashboardStatTile(
                            label = "طلبات اليوم",
                            value = uiState.requestsTodayCount?.toString() ?: "—",
                            modifier = Modifier.weight(1f),
                        )
                        DashboardStatTile(
                            label = "بانتظار قرار",
                            value = uiState.pendingCustomerOrdersCount?.toString() ?: "—",
                            modifier = Modifier.weight(1f),
                        )
                        DashboardStatTile(
                            label = "إجمالي الطلبات",
                            value = uiState.totalCustomerOrdersCount?.toString() ?: "—",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(d.radiusL),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceM),
            verticalArrangement = Arrangement.spacedBy(d.spaceXS),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WorkflowCard(
    title: String,
    body: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXL),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = d.cardElevation,
    ) {
        Row(
            modifier = Modifier.padding(d.spaceL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(d.iconM))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(d.spaceXS)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CompactWorkflowCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(d.radiusXL),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = d.cardElevation,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(d.iconM))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}
