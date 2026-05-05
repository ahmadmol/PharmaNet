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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.pharmalink.domain.model.OrderStatus

@Composable
fun MyCustomerOrdersScreen(
    onBackClick: () -> Unit,
    onStartSearchClick: () -> Unit,
    onOpenOrderDetail: (String) -> Unit,
    refreshRequested: Boolean,
    onRefreshHandled: () -> Unit,
    viewModel: MyCustomerOrdersViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(refreshRequested) {
        if (!refreshRequested) return@LaunchedEffect
        viewModel.refreshOrders()
        onRefreshHandled()
    }

    MyCustomerOrdersContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onStartSearchClick = onStartSearchClick,
        onRetryClick = viewModel::refreshOrders,
        onOpenOrderDetail = onOpenOrderDetail,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyCustomerOrdersContent(
    uiState: MyCustomerOrdersUiState,
    onBackClick: () -> Unit,
    onStartSearchClick: () -> Unit,
    onRetryClick: () -> Unit,
    onOpenOrderDetail: (String) -> Unit,
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
                            text = stringResource(R.string.my_customer_orders_title),
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
                    LoadingState(modifier = Modifier.padding(innerPadding))
                }
                ScreenState.Empty -> {
                    EmptyState(
                        onStartSearchClick = onStartSearchClick,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
                is ScreenState.Error -> {
                    ErrorState(
                        message = screenState.message ?: stringResource(R.string.order_error_loading_failed),
                        onRetryClick = onRetryClick,
                        title = stringResource(R.string.my_customer_orders_error_title),
                        modifier = Modifier.padding(innerPadding),
                    )
                }
                is ScreenState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(d.spaceL),
                        verticalArrangement = Arrangement.spacedBy(d.spaceL),
                    ) {
                        if (uiState.isRefreshing) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        items(screenState.data, key = { it.id }) { order ->
                            CustomerOrderListCard(
                                order = order,
                                onAction = { onOpenOrderDetail(order.id) },
                            )
                        }
                    }
                }
                is ScreenState.Offline -> {
                    ErrorState(
                        message = stringResource(R.string.error_network),
                        onRetryClick = onRetryClick,
                        title = stringResource(R.string.my_customer_orders_error_title),
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerOrderListCard(
    order: CustomerOrderListItemUi,
    onAction: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(d.spaceXS),
                ) {
                    CustomerOrderStatusChip(status = order.status, label = order.statusLabel)
                    Text(
                        text = order.medicineName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(d.spaceXS),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocalPharmacy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = order.pharmacyName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = order.createdAtLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }

            OrderInfoRow(
                label = stringResource(R.string.customer_order_detail_quantity_label),
                value = stringResource(R.string.customer_order_quantity_value, order.quantity, order.unit),
            )
            OrderInfoRow(
                label = stringResource(R.string.customer_order_request_type_title),
                value = order.urgencyLabel,
            )
            OrderInfoRow(
                label = stringResource(R.string.customer_order_request_scope_title),
                value = order.requestScopeLabel,
            )
            OrderInfoRow(
                label = stringResource(R.string.customer_order_detail_fulfillment_label),
                value = order.fulfillmentLabel,
            )
            OrderInfoRow(
                label = stringResource(R.string.customer_order_detail_status_label),
                value = order.statusSupportingText,
            )
            OrderInfoRow(
                label = if (order.totalPriceLabel == null) {
                    stringResource(R.string.customer_order_pending_price_label)
                } else {
                    stringResource(R.string.customer_order_confirmed_price_label)
                },
                value = order.totalPriceLabel ?: stringResource(R.string.customer_order_pending_price_value),
            )

            PharmaButton(
                text = stringResource(R.string.customer_order_view_details_action),
                onClick = onAction,
                style = PharmaButtonStyle.Outlined,
            )
        }
    }
}

@Composable
internal fun CustomerOrderStatusChip(
    status: OrderStatus,
    label: String,
    modifier: Modifier = Modifier,
) {
    val containerColor = when (status) {
        OrderStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
        OrderStatus.CONFIRMED,
        OrderStatus.READY_FOR_PICKUP,
        OrderStatus.OUT_FOR_DELIVERY -> MaterialTheme.colorScheme.primaryContainer
        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.tertiaryContainer
        OrderStatus.REJECTED,
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when (status) {
        OrderStatus.REJECTED,
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.onErrorContainer
        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.onTertiaryContainer
        OrderStatus.PENDING -> MaterialTheme.colorScheme.onSecondaryContainer
        OrderStatus.CONFIRMED,
        OrderStatus.READY_FOR_PICKUP,
        OrderStatus.OUT_FOR_DELIVERY -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.dimens.spaceM,
                vertical = MaterialTheme.dimens.spaceXS,
            ),
        )
    }
}

@Composable
internal fun OrderInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceXXS),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(
    onStartSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(d.spaceL),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.my_customer_orders_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.my_customer_orders_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            PharmaButton(
                text = stringResource(R.string.my_customer_orders_empty_action),
                onClick = onStartSearchClick,
            )
        }
    }
}

@Composable
internal fun ErrorState(
    message: String,
    onRetryClick: () -> Unit,
    title: String? = null,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(d.spaceL),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.errorContainer,
        ) {
            Column(
                modifier = Modifier.padding(d.spaceL),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                )
                PharmaButton(
                    text = stringResource(R.string.order_retry_loading),
                    onClick = onRetryClick,
                )
            }
        }
    }
}
