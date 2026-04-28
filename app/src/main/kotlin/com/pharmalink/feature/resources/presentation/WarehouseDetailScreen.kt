package com.pharmalink.feature.resources.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.R
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaButtonStyle
import com.pharmalink.designsystem.components.PharmaOutlinedTile
import com.pharmalink.designsystem.components.PharmaScreenScaffold
import com.pharmalink.designsystem.components.PharmaScreenState
import com.pharmalink.designsystem.components.PharmaSectionHeader
import com.pharmalink.designsystem.components.PharmaStateSpec
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.components.PharmaStateView
import com.pharmalink.designsystem.components.PharmaStatusChip
import com.pharmalink.designsystem.components.StatusTone
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.Warehouse
import com.pharmalink.domain.model.WarehouseShipment

@Composable
fun WarehouseDetailScreen(
    warehouseId: String,
    onBack: () -> Unit,
    onCreateRequest: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: WarehouseDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val d = MaterialTheme.dimens

    PharmaScreenScaffold(
        title = stringResource(R.string.warehouse_detail_title),
        onBack = onBack,
        navigationContentDescription = stringResource(R.string.common_back),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ClinicalCanvas),
        ) {
            PharmaScreenState(
                screenState = state.screenState,
                loading = PharmaStateSpec(
                    title = stringResource(R.string.warehouse_detail_title),
                    subtitle = stringResource(R.string.warehouse_detail_loading_subtitle),
                    tone = PharmaStateTone.Loading,
                ),
                empty = PharmaStateSpec(
                    title = stringResource(R.string.resources_empty_title),
                    subtitle = stringResource(R.string.resources_empty_subtitle),
                ),
                error = PharmaStateSpec(
                    title = stringResource(R.string.warehouse_detail_title),
                    subtitle = stringResource(R.string.warehouse_detail_error_subtitle),
                    tone = PharmaStateTone.Error,
                ),
                offline = PharmaStateSpec(
                    title = stringResource(R.string.warehouse_detail_title),
                    subtitle = stringResource(R.string.warehouse_detail_error_subtitle),
                    tone = PharmaStateTone.Offline,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = d.spaceL, vertical = d.spaceXL),
            ) { content ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(d.spaceL),
                    verticalArrangement = Arrangement.spacedBy(d.spaceL),
                ) {
                    item { WarehouseHeader(content.warehouse) }
                    item { WarehouseStatus(content.warehouse) }
                    item { WarehouseInfoCard(content.warehouse) }
                    item { WarehouseSupplierCard(content.warehouse) }
                    item { WarehouseShipments(content.shipments) }
                    if (onCreateRequest != null) {
                        item {
                            PharmaButton(
                                text = stringResource(R.string.warehouse_action_request),
                                onClick = onCreateRequest,
                                enabled = true,
                                style = PharmaButtonStyle.GradientAccent,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WarehouseHeader(warehouse: Warehouse) {
    val d = MaterialTheme.dimens
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PharmaGradients.headerBlueToGreen)
                .padding(d.spaceL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f), contentColor = Color.White) {
                Icon(Icons.Outlined.Warehouse, contentDescription = null, modifier = Modifier.padding(d.spaceM).size(36.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.spaceXS)) {
                Text(warehouse.name, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${warehouse.city} - ${warehouse.district}", color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodyMedium)
                Text(warehouse.lastUpdatedLabel, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall)
            }
            PharmaStatusChip(label = availabilityLabel(warehouse), tone = availabilityTone(warehouse))
        }
    }
}

@Composable
private fun WarehouseStatus(warehouse: Warehouse) {
    val d = MaterialTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
        PharmaSectionHeader(title = "Availability status", subtitle = "Live stock signals from this warehouse")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.spaceM)) {
            PharmaOutlinedTile(
                title = stringResource(R.string.warehouse_stock_in),
                value = "${warehouse.inStockPercent}%",
                icon = Icons.Outlined.Inventory2,
                modifier = Modifier.weight(1f),
            )
            PharmaOutlinedTile(
                title = stringResource(R.string.warehouse_stock_low),
                value = warehouse.lowStockCount.toString(),
                icon = Icons.Outlined.Inventory2,
                modifier = Modifier.weight(1f),
            )
            PharmaOutlinedTile(
                title = stringResource(R.string.warehouse_stock_out),
                value = warehouse.outOfStockCount.toString(),
                icon = Icons.Outlined.Inventory2,
                modifier = Modifier.weight(1f),
            )
        }
        if (warehouse.supportsColdChain) {
            InfoLine(icon = Icons.Outlined.AcUnit, title = "Cold chain", value = "Supported")
        }
    }
}

@Composable
private fun WarehouseInfoCard(warehouse: Warehouse) {
    val d = MaterialTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
        PharmaSectionHeader(title = "Warehouse info", subtitle = "Delivery time, location, and contact")
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(d.spaceL), verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
                InfoLine(Icons.Outlined.LocalShipping, "Delivery time", warehouse.estimatedDeliveryLabel)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                InfoLine(Icons.Outlined.LocationOn, "Location", "${warehouse.city} - ${warehouse.district} (${warehouse.distanceLabel})")
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                InfoLine(Icons.Outlined.Phone, "Phone", warehouse.phoneNumber)
            }
        }
    }
}

@Composable
private fun WarehouseSupplierCard(warehouse: Warehouse) {
    val d = MaterialTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
        PharmaSectionHeader(title = "Supplier details", subtitle = "Supplier-specific fields are not part of the warehouse model yet")
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(d.spaceL), verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
                InfoLine(Icons.Outlined.Store, "Warehouse partner", warehouse.name)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                InfoLine(Icons.Outlined.Inventory2, "Service scope", if (warehouse.supportsColdChain) "Standard and cold chain" else "Standard supply")
            }
        }
    }
}

@Composable
private fun WarehouseShipments(shipments: List<WarehouseShipment>) {
    val d = MaterialTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
        PharmaSectionHeader(title = stringResource(R.string.warehouse_shipments_title), subtitle = stringResource(R.string.warehouse_detail_shipments_subtitle))
        if (shipments.isEmpty()) {
            PharmaStateView(
                title = stringResource(R.string.warehouse_detail_no_shipments_title),
                subtitle = stringResource(R.string.warehouse_detail_no_shipments_subtitle),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            shipments.forEach { shipment ->
                ShipmentCard(shipment)
            }
        }
    }
}

@Composable
private fun ShipmentCard(shipment: WarehouseShipment) {
    val d = MaterialTheme.dimens
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(d.spaceL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), contentColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Outlined.LocalShipping, contentDescription = null, modifier = Modifier.padding(d.spaceM).size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.spaceXS)) {
                Text(shipment.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("${shipment.etaLabel} - ${shipment.statusLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(shipment.itemsCount.toString(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(value.ifBlank { "Not available" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun availabilityLabel(warehouse: Warehouse): String = when {
    warehouse.inStockPercent > 70 -> "Available"
    warehouse.inStockPercent > 30 -> "Low stock"
    else -> "Limited"
}

private fun availabilityTone(warehouse: Warehouse): StatusTone = when {
    warehouse.inStockPercent > 70 -> StatusTone.Success
    warehouse.inStockPercent > 30 -> StatusTone.Warning
    else -> StatusTone.Urgent
}
