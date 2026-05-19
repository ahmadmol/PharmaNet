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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaButtonStyle
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.dimens
import kotlinx.coroutines.flow.collectLatest

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PharmacyCustomerOrdersScreen(
    onOpenOrder: (String) -> Unit,
    viewModel: PharmacyCustomerOrdersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.newOrderNotification.collectLatest { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = ClinicalCanvas,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("طلبات العملاء") },
                    actions = {
                        IconButton(onClick = viewModel::refreshOrders) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "تحديث")
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM),
            ) {
                FilterRow(
                    selected = state.selectedFilter,
                    onSelected = viewModel::selectFilter,
                    modifier = Modifier.padding(top = MaterialTheme.dimens.spaceS),
                )
                PendingSummary(
                    pendingCount = state.pendingCount,
                    modifier = Modifier.padding(horizontal = MaterialTheme.dimens.spaceL),
                )

                when (val screenState = state.screenState) {
                    ScreenState.Loading -> LoadingState()
                    ScreenState.Empty -> EmptyState()
                    is ScreenState.Error -> ErrorState(
                        message = screenState.message ?: "تعذر تحميل طلبات العملاء",
                        onRetry = viewModel::refreshOrders,
                    )
                    is ScreenState.Success -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = MaterialTheme.dimens.spaceL, vertical = MaterialTheme.dimens.spaceS),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM),
                        ) {
                            if (state.isRefreshing) {
                                item {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            items(screenState.data, key = { it.id }) { order ->
                                PharmacyCustomerOrderCard(
                                    order = order,
                                    onOpen = { onOpenOrder(order.id) },
                                )
                            }
                        }
                    }
                    is ScreenState.Offline -> ErrorState(
                        message = "تعذر الاتصال",
                        onRetry = viewModel::refreshOrders,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    selected: PharmacyCustomerOrderFilter,
    onSelected: (PharmacyCustomerOrderFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = MaterialTheme.dimens.spaceL),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceS),
    ) {
        items(PharmacyCustomerOrderFilter.entries, key = { it.name }) { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(filter.label()) },
            )
        }
    }
}

@Composable
private fun PendingSummary(pendingCount: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.dimens.spaceL),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceXS)) {
                Text("طلبات بانتظار قرار", style = MaterialTheme.typography.labelLarge)
                Text("راجع الطلبات الجديدة وحدد السعر أو الرفض", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = pendingCount.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PharmacyCustomerOrderCard(
    order: PharmacyCustomerOrderUi,
    onOpen: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = MaterialTheme.dimens.cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.dimens.spaceL),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceXS)) {
                    CustomerOrderStatusChip(status = order.status, label = order.statusLabel)
                    Text(order.medicineName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(order.customerName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(order.createdAtLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceS)) {
                SuggestionChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(order.fulfillmentLabel) },
                )
                SuggestionChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(order.urgencyLabel) },
                )
            }
            OrderInfoRow(label = "الكمية", value = order.quantityLabel)
            OrderInfoRow(label = "السعر", value = order.priceLabel)
            PharmaButton(
                text = "عرض التفاصيل",
                onClick = onOpen,
                style = PharmaButtonStyle.Outlined,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(MaterialTheme.dimens.spaceL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM),
        ) {
            Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("لا توجد طلبات عملاء ضمن هذا التصنيف", textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(MaterialTheme.dimens.spaceL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM),
        ) {
            Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            PharmaButton(text = "إعادة المحاولة", onClick = onRetry)
        }
    }
}
