package com.pharmalink.feature.admin.ui.warehouses

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaCard
import com.pharmalink.designsystem.components.PharmaSkeletonLine
import com.pharmalink.designsystem.components.PharmaStateView
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.theme.PharmaTheme
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.designsystem.utils.CollectEffect
import com.pharmalink.feature.admin.R

@Composable
fun WarehouseDetailsScreen(
    onBackClick: () -> Unit,
    onNavigateToInventory: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WarehouseDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    CollectEffect(effect = viewModel.effect) { effect ->
        when (effect) {
            is WarehouseDetailsEffect.ShowMessage -> {
                snackbarHostState.showSnackbar(effect.message)
            }
            is WarehouseDetailsEffect.NavigateToInventory -> {
                onNavigateToInventory(effect.warehouseId)
            }
            is WarehouseDetailsEffect.NavigateToShipments -> {
                snackbarHostState.showSnackbar("عرض الشحنات: قيد التطوير")
            }
        }
    }

    WarehouseDetailsContent(
        state = state,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WarehouseDetailsContent(
    state: WarehouseDetailsUiState,
    onAction: (WarehouseDetailsAction) -> Unit,
    onBackClick: () -> Unit,
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
                            text = stringResource(R.string.warehouse_details_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
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
                onRetry = { onAction(WarehouseDetailsAction.OnRetryClicked) },
                modifier = Modifier.padding(padding),
            )
            state.warehouse == null -> EmptyContent(modifier = Modifier.padding(padding))
            else -> SuccessContent(
                warehouse = state.warehouse,
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
        PharmaSkeletonLine(heightDp = 200f)
        Row(horizontalArrangement = Arrangement.spacedBy(d.spaceM)) {
            PharmaSkeletonLine(heightDp = 100f, modifier = Modifier.weight(1f))
            PharmaSkeletonLine(heightDp = 100f, modifier = Modifier.weight(1f))
        }
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
            title = stringResource(R.string.warehouse_details_error),
            subtitle = message,
            tone = PharmaStateTone.Error,
            actionLabel = stringResource(R.string.admin_warehouses_retry),
            onAction = onRetry,
        )
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(d.spaceL),
    ) {
        PharmaStateView(
            title = stringResource(R.string.warehouse_details_not_found),
            subtitle = stringResource(R.string.warehouse_details_not_found_subtitle),
            tone = PharmaStateTone.Neutral,
        )
    }
}

@Composable
private fun SuccessContent(
    warehouse: WarehouseDetailModel,
    onAction: (WarehouseDetailsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(d.spaceL),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        // Header Card
        item {
            HeaderCard(warehouse = warehouse)
        }

        // Statistics Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                StatCard(
                    title = stringResource(R.string.warehouse_details_inventory_items),
                    value = warehouse.totalInventoryItems.toString(),
                    modifier = Modifier.weight(1f),
                )
                
                StatCard(
                    title = stringResource(R.string.warehouse_details_active_shipments),
                    value = warehouse.activeShipments.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Stock Status Card
        item {
            StockStatusCard(warehouse = warehouse)
        }

        // Warehouse Information
        item {
            InfoCard(warehouse = warehouse)
        }

        // Actions
        item {
            ActionsCard(onAction = onAction)
        }
    }
}

@Composable
private fun HeaderCard(
    warehouse: WarehouseDetailModel,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    PharmaCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        elevationDp = 2f,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Warehouse,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(36.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(d.spaceXS),
            ) {
                Text(
                    text = warehouse.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )

                Text(
                    text = "${warehouse.city} - ${warehouse.district}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )

                if (warehouse.supportsColdChain) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(d.spaceXS),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AcUnit,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.warehouse_details_cold_chain),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF3B82F6),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    PharmaCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        elevationDp = 2f,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun StockStatusCard(
    warehouse: WarehouseDetailModel,
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
                text = stringResource(R.string.warehouse_details_stock_status),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            HorizontalDivider()

            // In Stock Progress
            Column(verticalArrangement = Arrangement.spacedBy(d.spaceS)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.warehouse_details_in_stock),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${warehouse.inStockPercent}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                    )
                }
                LinearProgressIndicator(
                    progress = { warehouse.inStockPercent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF10B981),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            // Low Stock Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.warehouse_details_low_stock),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = warehouse.lowStockCount.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B),
                )
            }

            // Out of Stock Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.warehouse_details_out_of_stock),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = warehouse.outOfStockCount.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    warehouse: WarehouseDetailModel,
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
                text = stringResource(R.string.warehouse_details_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            HorizontalDivider()

            InfoRow(
                icon = Icons.Outlined.LocationOn,
                label = stringResource(R.string.warehouse_details_location),
                value = "${warehouse.city}, ${warehouse.district}",
            )

            InfoRow(
                icon = Icons.Outlined.Phone,
                label = stringResource(R.string.warehouse_details_phone),
                value = warehouse.phoneNumber,
            )

            InfoRow(
                icon = Icons.Outlined.Inventory,
                label = stringResource(R.string.warehouse_details_distance),
                value = warehouse.distanceLabel,
            )

            InfoRow(
                icon = Icons.Outlined.Inventory,
                label = stringResource(R.string.warehouse_details_delivery_time),
                value = warehouse.estimatedDeliveryLabel,
            )

            InfoRow(
                icon = Icons.Outlined.Inventory,
                label = stringResource(R.string.warehouse_details_last_updated),
                value = warehouse.lastUpdatedLabel,
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ActionsCard(
    onAction: (WarehouseDetailsAction) -> Unit,
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
                text = stringResource(R.string.warehouse_details_actions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            HorizontalDivider()

            PharmaButton(
                text = stringResource(R.string.warehouse_details_manage_inventory),
                onClick = { onAction(WarehouseDetailsAction.OnManageInventoryClicked) },
                modifier = Modifier.fillMaxWidth(),
            )

            PharmaButton(
                text = stringResource(R.string.warehouse_details_view_shipments),
                onClick = { onAction(WarehouseDetailsAction.OnViewShipmentsClicked) },
                modifier = Modifier.fillMaxWidth(),
            )

            PharmaButton(
                text = stringResource(R.string.warehouse_details_edit),
                onClick = { onAction(WarehouseDetailsAction.OnEditClicked) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun PreviewWarehouseDetailsScreen() {
    PharmaTheme {
        WarehouseDetailsContent(
            state = WarehouseDetailsUiState(
                warehouse = WarehouseDetailModel(
                    id = "1",
                    name = "مستودع المركز الرئيسي",
                    city = "الرياض",
                    district = "المنطقة الصناعية",
                    phoneNumber = "0112345678",
                    supportsColdChain = true,
                    inStockPercent = 85,
                    lowStockCount = 12,
                    outOfStockCount = 3,
                    estimatedDeliveryLabel = "1-2 أيام",
                    distanceLabel = "5 كم",
                    lastUpdatedLabel = "منذ ساعة",
                    totalInventoryItems = 342,
                    activeShipments = 15,
                    completedOrders = 128,
                ),
            ),
            onAction = {},
            onBackClick = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
