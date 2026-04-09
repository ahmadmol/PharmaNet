package com.pharmalink.feature.orders.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaButtonStyle
import com.pharmalink.designsystem.components.PharmaScreenScaffold
import com.pharmalink.designsystem.components.PharmaScreenState
import com.pharmalink.designsystem.components.PharmaStateSpec
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.components.PharmaStatusChip
import com.pharmalink.designsystem.components.StatusTone
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
                verticalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                item {
                    OrderHeroCard(order = order)
                }
                item {
                    OrderFactsCard(order = order)
                }
                item {
                    PharmaButton(
                        text = stringResource(R.string.order_detail_open_request),
                        onClick = { onOpenRequest(order.requestId) },
                        style = PharmaButtonStyle.GradientAccent,
                    )
                }
                item {
                    PharmaButton(
                        text = stringResource(R.string.order_detail_track_delivery),
                        onClick = { onTrackDelivery(order.id) },
                        style = PharmaButtonStyle.Outlined,
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderHeroCard(order: Order) {
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
            verticalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = order.id,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.92f),
                )
                PharmaStatusChip(
                    label = orderStatusText(order.status),
                    tone = orderStatusTone(order.status),
                )
            }
            Text(
                text = order.medicineName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            if (order.isUrgent) {
                PharmaStatusChip(label = stringResource(R.string.order_detail_urgent), tone = StatusTone.Urgent)
            }
        }
    }
}

@Composable
private fun OrderFactsCard(order: Order) {
    val d = MaterialTheme.dimens
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(d.spaceL)) {
            DetailRow(
                label = stringResource(R.string.order_detail_label_quantity),
                value = "${order.quantity} ${order.unit}",
            )
            HorizontalDivider(Modifier.padding(vertical = d.spaceS))
            DetailRow(label = stringResource(R.string.order_detail_label_warehouse), value = order.warehouseName)
            HorizontalDivider(Modifier.padding(vertical = d.spaceS))
            DetailRow(label = stringResource(R.string.order_detail_label_supplier), value = order.supplierName)
            HorizontalDivider(Modifier.padding(vertical = d.spaceS))
            DetailRow(label = stringResource(R.string.order_detail_label_order_date), value = order.createdAtLabel)
            order.etaLabel?.let { eta ->
                HorizontalDivider(Modifier.padding(vertical = d.spaceS))
                DetailRow(label = stringResource(R.string.order_detail_label_eta), value = eta)
            }
            HorizontalDivider(Modifier.padding(vertical = d.spaceS))
            DetailRow(label = stringResource(R.string.order_detail_label_last_update), value = order.lastUpdateLabel)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
