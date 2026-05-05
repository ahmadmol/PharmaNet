package com.pharmalink.feature.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaButtonStyle
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.FulfillmentType

@Composable
fun CustomerOrderDetailScreen(
    onBackClick: () -> Unit,
    onOrderCancelled: () -> Unit,
    viewModel: CustomerOrderDetailViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(uiState.cancelCompleted) {
        if (!uiState.cancelCompleted) return@LaunchedEffect
        onOrderCancelled()
        viewModel.consumeCancelCompleted()
    }

    CustomerOrderDetailContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRetryClick = viewModel::refreshOrder,
        onCancelClick = viewModel::showCancelDialog,
        onDismissCancelDialog = viewModel::dismissCancelDialog,
        onConfirmCancelDialog = viewModel::confirmCancelOrder,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerOrderDetailContent(
    uiState: CustomerOrderDetailUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onCancelClick: () -> Unit,
    onDismissCancelDialog: () -> Unit,
    onConfirmCancelDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier,
            containerColor = ClinicalCanvas,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.customer_order_detail_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.customer_order_back),
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            when (val screenState = uiState.screenState) {
                ScreenState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is ScreenState.Error -> {
                    ErrorState(
                        message = screenState.message ?: stringResource(R.string.customer_order_detail_not_found),
                        onRetryClick = onRetryClick,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
                is ScreenState.Success -> {
                    val order = screenState.data
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(d.spaceL),
                        verticalArrangement = Arrangement.spacedBy(d.spaceL),
                    ) {
                        item {
                            StatusHeroCard(order = order)
                        }
                        item {
                            OrderSummaryCard(order = order)
                        }
                        item {
                            FulfillmentCard(order = order)
                        }
                        item {
                            PricingCard(order = order)
                        }
                        order.notes?.let {
                            item {
                                NotesCard(notes = it)
                            }
                        }
                        if (!uiState.actionErrorMessage.isNullOrBlank()) {
                            item {
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                ) {
                                    Text(
                                        text = uiState.actionErrorMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(d.spaceL),
                                    )
                                }
                            }
                        }
                        if (order.canCancel) {
                            item {
                                PharmaButton(
                                    text = stringResource(R.string.customer_order_cancel_action),
                                    onClick = onCancelClick,
                                    enabled = !uiState.isCancelling,
                                    style = PharmaButtonStyle.Outlined,
                                )
                            }
                        }
                        if (uiState.isCancelling) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
                ScreenState.Empty -> {
                    ErrorState(
                        message = stringResource(R.string.customer_order_detail_not_found),
                        onRetryClick = onRetryClick,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
                is ScreenState.Offline -> {
                    ErrorState(
                        message = stringResource(R.string.error_network),
                        onRetryClick = onRetryClick,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }

        if (uiState.isCancelDialogVisible) {
            AlertDialog(
                onDismissRequest = onDismissCancelDialog,
                title = {
                    Text(text = stringResource(R.string.customer_order_cancel_dialog_title))
                },
                text = {
                    Text(text = stringResource(R.string.customer_order_cancel_dialog_message))
                },
                confirmButton = {
                    TextButton(onClick = onConfirmCancelDialog) {
                        Text(text = stringResource(R.string.customer_order_cancel_dialog_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissCancelDialog) {
                        Text(text = stringResource(R.string.customer_order_cancel_dialog_dismiss))
                    }
                },
            )
        }
    }
}

@Composable
private fun StatusHeroCard(
    order: CustomerOrderDetailUi,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CustomerOrderStatusChip(
                status = order.status,
                label = order.statusLabel,
            )
            Text(
                text = order.statusLabel,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = order.statusSupportingText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun OrderSummaryCard(
    order: CustomerOrderDetailUi,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            SectionTitle(text = stringResource(R.string.customer_order_summary_section_title))
            OrderInfoRow(
                label = stringResource(R.string.customer_order_detail_order_id_label),
                value = order.id,
            )
            OrderInfoRow(
                label = stringResource(R.string.customer_order_success_medicine_label),
                value = order.medicineName,
            )
            OrderInfoRow(
                label = stringResource(R.string.customer_order_detail_quantity_label),
                value = stringResource(R.string.customer_order_quantity_value, order.quantity, order.unit),
            )
            OrderInfoRow(
                label = stringResource(R.string.customer_order_success_pharmacy_label),
                value = order.pharmacyName,
            )
            order.pharmacyLocation?.let { location ->
                OrderInfoRow(
                    label = stringResource(R.string.customer_order_pharmacy_location_label),
                    value = location,
                )
            }
            OrderInfoRow(
                label = stringResource(R.string.customer_order_request_type_title),
                value = order.urgencyLabel,
            )
            OrderInfoRow(
                label = stringResource(R.string.customer_order_request_scope_title),
                value = order.requestScopeLabel,
            )
            OrderInfoRow(
                label = stringResource(R.string.customer_order_detail_status_label),
                value = order.statusLabel,
            )
            Text(
                text = order.createdAtLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FulfillmentCard(
    order: CustomerOrderDetailUi,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            SectionTitle(text = stringResource(R.string.customer_order_fulfillment_section_title))
            OrderInfoRow(
                label = stringResource(R.string.customer_order_detail_fulfillment_label),
                value = order.fulfillmentLabel,
            )
            if (order.fulfillmentType == FulfillmentType.DELIVERY) {
                order.deliveryAddress?.let { address ->
                    OrderInfoRow(
                        label = stringResource(R.string.customer_order_detail_address_label),
                        value = address,
                    )
                }
                order.deliveryPhone?.let { phone ->
                    OrderInfoRow(
                        label = stringResource(R.string.customer_order_detail_phone_label),
                        value = phone,
                    )
                }
            }
        }
    }
}

@Composable
private fun PricingCard(
    order: CustomerOrderDetailUi,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            SectionTitle(text = stringResource(R.string.customer_order_pricing_section_title))
            OrderInfoRow(
                label = if (order.totalPriceLabel == null) {
                    stringResource(R.string.customer_order_pending_price_label)
                } else {
                    stringResource(R.string.customer_order_confirmed_price_label)
                },
                value = order.totalPriceLabel ?: stringResource(R.string.customer_order_pending_price_value),
            )
        }
    }
}

@Composable
private fun NotesCard(
    notes: String,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            SectionTitle(text = stringResource(R.string.customer_order_notes_section_title))
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
