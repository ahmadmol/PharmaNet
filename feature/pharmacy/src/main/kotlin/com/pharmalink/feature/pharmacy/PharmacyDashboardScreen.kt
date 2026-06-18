package com.pharmalink.feature.pharmacy

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
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
import com.pharmalink.designsystem.theme.PharmaBlue100
import com.pharmalink.designsystem.theme.PharmaBlue900
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.PremiumPrimary
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
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(
                brush = PharmaGradients.headerBlueToGreen,
                shape = RoundedCornerShape(bottomStart = d.radiusXXL, bottomEnd = d.radiusXXL),
            ),
        color = Color.Transparent,
        contentColor = Color.White,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = d.spaceM),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = d.spaceL),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onNavigateToNotifications) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = stringResource(R.string.pharmacy_dashboard_notifications),
                        tint = Color.White,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                ) {
                    Icon(
                        painter = painterResource(id = DsR.drawable.ic_app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(d.iconM),
                        tint = Color.Unspecified,
                    )
                    Text(
                        text = stringResource(R.string.pharmacy_dashboard_home_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                Box(modifier = Modifier.size(48.dp))
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
            Column(verticalArrangement = Arrangement.spacedBy(d.spaceXS)) {
                Text(
                    text = "مساحة عمل الصيدلية",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "مرحباً بك في لوحة تحكم صيدليتك",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXL),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
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
                modifier = Modifier.size(d.iconL),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(d.spaceXXS),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "تنبيه هام",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = "حساب الصيدلية غير مرتبط بمنشأة حالياً.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = RoundedCornerShape(d.radiusL),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                shadowElevation = 1.dp,
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        Text(
            text = "ملخص النشاط",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        when {
            uiState.isLoading && !uiState.hasStats -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = d.spaceL),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
            uiState.errorMessage != null && !uiState.hasStats -> {
                Column(
                    modifier = Modifier.padding(vertical = d.spaceL),
                    verticalArrangement = Arrangement.spacedBy(d.spaceS),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(d.spaceM),
                ) {
                    DashboardStatTile(
                        label = "طلبات اليوم",
                        value = uiState.requestsTodayCount?.toString() ?: "0",
                    )
                    DashboardStatTile(
                        label = "بانتظار قرار",
                        value = uiState.pendingCustomerOrdersCount?.toString() ?: "0",
                    )
                    DashboardStatTile(
                        label = "إجمالي الطلبات",
                        value = uiState.totalCustomerOrdersCount?.toString() ?: "0",
                    )
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
        shape = RoundedCornerShape(d.radiusXL),
        color = PharmaBlue100,
        contentColor = PharmaBlue900,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceXS),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = PharmaBlue900,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = PharmaBlue100,
                    contentColor = PremiumPrimary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(d.iconL))
                    }
                }
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إدارة ${title} >",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = PremiumPrimary
                )
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
