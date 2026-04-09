package com.pharmalink.feature.resources.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.R
import com.pharmalink.designsystem.components.PharmaOutlinedTile
import com.pharmalink.designsystem.components.PharmaScreenScaffold
import com.pharmalink.designsystem.components.PharmaScreenState
import com.pharmalink.designsystem.components.PharmaSectionHeader
import com.pharmalink.designsystem.components.PharmaStateSpec
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.components.PharmaStateView
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.dimens

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
            WarehouseDetailsBody(
                content = content,
                onCreateRequest = onCreateRequest,
            )
        }
    }
}

@Composable
private fun WarehouseDetailsBody(
    content: WarehouseDetailContent,
    onCreateRequest: (() -> Unit)?,
) {
    val d = MaterialTheme.dimens
    val warehouse = content.warehouse

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(d.spaceL),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PharmaGradients.headerBlueToGreen)
                        .padding(d.spaceL),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Warehouse,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                        Text(
                            text = warehouse.name,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${warehouse.city} - ${warehouse.district}",
                            color = Color.White.copy(alpha = 0.92f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = stringResource(
                                R.string.warehouse_detail_distance_eta,
                                warehouse.distanceLabel,
                                warehouse.estimatedDeliveryLabel,
                            ),
                            color = Color.White.copy(alpha = 0.92f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        item {
            PharmaSectionHeader(
                title = stringResource(R.string.warehouse_stock_title),
                subtitle = stringResource(R.string.warehouse_detail_stock_subtitle),
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                PharmaOutlinedTile(
                    title = stringResource(R.string.warehouse_stock_in),
                    value = "${warehouse.inStockPercent}%",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Inventory2,
                )
                PharmaOutlinedTile(
                    title = stringResource(R.string.warehouse_stock_low),
                    value = warehouse.lowStockCount.toString(),
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Inventory2,
                )
                PharmaOutlinedTile(
                    title = stringResource(R.string.warehouse_stock_out),
                    value = warehouse.outOfStockCount.toString(),
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Inventory2,
                )
            }
        }
        item {
            PharmaSectionHeader(
                title = stringResource(R.string.warehouse_shipments_title),
                subtitle = stringResource(R.string.warehouse_detail_shipments_subtitle),
            )
        }
        if (content.shipments.isEmpty()) {
            item {
                PharmaStateView(
                    title = stringResource(R.string.warehouse_detail_no_shipments_title),
                    subtitle = stringResource(R.string.warehouse_detail_no_shipments_subtitle),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            items(content.shipments, key = { it.id }) { shipment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(d.spaceL),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Outlined.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                            Text(
                                text = shipment.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(d.spaceXS))
                            Text(
                                text = "${shipment.etaLabel} - ${shipment.statusLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = shipment.itemsCount.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    onClick = { onCreateRequest?.invoke() },
                ) {
                    Row(
                        modifier = Modifier.padding(d.spaceL),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.warehouse_action_request),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(d.spaceL),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        androidx.compose.foundation.layout.Column {
                            Text(
                                text = stringResource(R.string.warehouse_call_action),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = warehouse.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
