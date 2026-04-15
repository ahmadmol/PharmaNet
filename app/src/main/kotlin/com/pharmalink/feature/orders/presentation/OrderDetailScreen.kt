package com.pharmalink.feature.orders.presentation

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Store
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.R
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaButtonStyle
import com.pharmalink.designsystem.components.PharmaScreenScaffold
import com.pharmalink.designsystem.components.PharmaScreenState
import com.pharmalink.designsystem.components.PharmaSectionHeader
import com.pharmalink.designsystem.components.PharmaStateSpec
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.components.PharmaStatusChip
import com.pharmalink.designsystem.components.StatusTone
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus

@Composable
fun OrderDetailScreen(
    onBack: () -> Unit,
    onOpenRequest: (String) -> Unit,
    onTrackDelivery: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OrderDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val d = MaterialTheme.dimens

    PharmaScreenScaffold(
        title = stringResource(R.string.order_detail_title),
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
                    title = stringResource(R.string.order_detail_title),
                    subtitle = stringResource(R.string.order_detail_loading_subtitle),
                    tone = PharmaStateTone.Loading,
                ),
                empty = PharmaStateSpec(
                    title = stringResource(R.string.order_detail_title),
                    subtitle = stringResource(R.string.order_detail_empty_subtitle),
                ),
                error = PharmaStateSpec(
                    title = stringResource(R.string.order_detail_title),
                    subtitle = stringResource(R.string.order_detail_error_subtitle),
                    tone = PharmaStateTone.Error,
                ),
                offline = PharmaStateSpec(
                    title = stringResource(R.string.order_detail_title),
                    subtitle = stringResource(R.string.order_detail_offline_subtitle),
                    tone = PharmaStateTone.Offline,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = d.spaceL, vertical = d.spaceXL),
            ) { order ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(d.spaceL),
                    verticalArrangement = Arrangement.spacedBy(d.spaceL),
                ) {
                    item { OrderHeader(order = order) }
                    item { OrderTimeline(order = order) }
                    item {
                        OrderParties(
                            order = order,
                            onTrackDelivery = { onTrackDelivery(order.id) },
                        )
                    }
                    item { OrderProducts(order = order) }
                    item { OrderSummary(order = order) }
                    item {
                        OrderActions(
                            onOpenRequest = { onOpenRequest(order.requestId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderHeader(order: Order) {
    val d = MaterialTheme.dimens
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PharmaGradients.headerBlueToGreen)
                .padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Order ID", color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelMedium)
                    Text(order.id, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                PharmaStatusChip(label = orderStatusText(order.status), tone = orderStatusTone(order.status))
            }
            Text(order.medicineName, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Last update: ${order.lastUpdateLabel}", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun OrderTimeline(order: Order) {
    val d = MaterialTheme.dimens
    val steps = listOf(
        "Placed" to true,
        "Approved" to (order.status == OrderStatus.APPROVED || order.status == OrderStatus.DELIVERED),
        "Delivered" to (order.status == OrderStatus.DELIVERED),
    )

    Column(verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
        PharmaSectionHeader(title = "Progress tracker", subtitle = "Current status: ${orderStatusText(order.status)}")
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(d.spaceL),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                steps.forEachIndexed { index, step ->
                    TimelineStep(label = step.first, active = step.second)
                    if (index < steps.lastIndex) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(if (steps[index + 1].second) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineStep(label: String, active: Boolean) {
    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(modifier = Modifier.size(24.dp), shape = CircleShape, color = color.copy(alpha = 0.16f), contentColor = color) {
            Box(contentAlignment = Alignment.Center) {
                Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = color) {}
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OrderParties(
    order: Order,
    onTrackDelivery: () -> Unit,
) {
    val d = MaterialTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
        PharmaSectionHeader(title = "Pharmacy and supplier", subtitle = "Fulfillment partners for this order")
        Row(horizontalArrangement = Arrangement.spacedBy(d.spaceM), modifier = Modifier.fillMaxWidth()) {
            DetailCard(
                title = "Warehouse",
                value = order.warehouseName,
                detail = order.etaLabel?.let { "ETA: $it" } ?: "ETA not available",
                icon = Icons.Outlined.Store,
                modifier = Modifier.weight(1f),
            )
            DetailCard(
                title = "Supplier",
                value = order.supplierName,
                detail = if (order.isUrgent) "Urgent request" else "Standard request",
                icon = Icons.Outlined.LocalShipping,
                modifier = Modifier.weight(1f),
            )
        }
        PharmaButton(
            text = "Track delivery",
            onClick = onTrackDelivery,
            style = PharmaButtonStyle.Outlined,
        )
    }
}

@Composable
private fun OrderProducts(order: Order) {
    val d = MaterialTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
        PharmaSectionHeader(title = "Products", subtitle = "Items included in this order")
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(d.spaceL),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), contentColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Outlined.Inventory2, contentDescription = null, modifier = Modifier.padding(d.spaceM).size(24.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(order.medicineName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${order.quantity} ${order.unit}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun OrderSummary(order: Order) {
    val d = MaterialTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
        PharmaSectionHeader(title = "Pricing summary", subtitle = "Only available order fields are shown")
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(d.spaceL), verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
                SummaryRow("Request", order.requestId)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                SummaryRow("Quantity", "${order.quantity} ${order.unit}")
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                SummaryRow("Price", "Not provided by order data")
            }
        }
    }
}

@Composable
private fun OrderActions(onOpenRequest: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM)) {
        PharmaButton(
            text = "Repeat order",
            onClick = onOpenRequest,
            style = PharmaButtonStyle.GradientAccent,
        )
    }
}

@Composable
private fun DetailCard(
    title: String,
    value: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(d.spaceL), verticalArrangement = Arrangement.spacedBy(d.spaceS)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun orderStatusText(status: OrderStatus): String = when (status) {
    OrderStatus.PENDING -> stringResource(R.string.order_detail_status_pending)
    OrderStatus.APPROVED -> stringResource(R.string.order_detail_status_approved)
    OrderStatus.REJECTED -> stringResource(R.string.order_detail_status_rejected)
    OrderStatus.DELIVERED -> stringResource(R.string.order_detail_status_delivered)
}

private fun orderStatusTone(status: OrderStatus): StatusTone = when (status) {
    OrderStatus.PENDING -> StatusTone.Pending
    OrderStatus.APPROVED -> StatusTone.Success
    OrderStatus.REJECTED -> StatusTone.Urgent
    OrderStatus.DELIVERED -> StatusTone.Success
}
