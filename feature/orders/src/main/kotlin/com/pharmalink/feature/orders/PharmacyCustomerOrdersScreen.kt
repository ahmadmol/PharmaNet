package com.pharmalink.feature.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaButtonStyle
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaBlue100
import com.pharmalink.designsystem.theme.PharmaBlue900
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.PharmaNeutral100
import com.pharmalink.designsystem.theme.PharmaNeutral600
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.StatusActive
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.OrderStatus
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
                label = {
                    Text(
                        text = filter.label(),
                        maxLines = 1,
                        softWrap = false,
                    )
                },
                modifier = Modifier.widthIn(min = 64.dp),
            )
        }
    }
}

@Composable
private fun PendingSummary(pendingCount: Int, modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .background(brush = PharmaGradients.headerBlueToGreen)
                .padding(d.spaceXXL),
            verticalArrangement = Arrangement.spacedBy(d.spaceL),
            horizontalAlignment = Alignment.Start
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(d.spaceXS)) {
                Text(
                    text = "ملخص الطلبات النشطة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "لديك $pendingCount طلب بانتظار قرارك أو التجهيز",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            Surface(
                shape = RoundedCornerShape(d.radiusL),
                color = Color.White,
                contentColor = MaterialTheme.colorScheme.primary,
                onClick = { /* Visual only */ }
            ) {
                Text(
                    text = "مراجعة الكل",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = d.spaceL, vertical = d.spaceS),
                )
            }
        }
    }
}

@Composable
private fun PharmacyCustomerOrderCard(
    order: PharmacyCustomerOrderUi,
    onOpen: () -> Unit,
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(d.radiusM),
                    color = PharmaNeutral100,
                ) {
                    Text(
                        text = "طلب #${order.id.takeLast(5)}",
                        modifier = Modifier.padding(horizontal = d.spaceM, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PharmaNeutral600,
                    )
                }
                PharmacyOrderStatusChip(
                    status = order.status,
                    label = order.statusLabel,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(d.spaceS)) {
                Text(
                    text = order.customerName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Medication,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = order.medicineName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = order.createdAtLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = PharmaNeutral600,
                )
                PharmaButton(
                    text = "التفاصيل",
                    onClick = onOpen,
                    style = PharmaButtonStyle.Outlined,
                    modifier = Modifier.widthIn(min = 100.dp),
                )
            }
        }
    }
}

@Composable
private fun PharmacyOrderStatusChip(
    status: OrderStatus,
    label: String,
    modifier: Modifier = Modifier,
) {
    val containerColor = when (status) {
        OrderStatus.DELIVERED -> StatusActive.copy(alpha = 0.12f)
        OrderStatus.REJECTED, OrderStatus.CANCELLED -> PremiumUrgent.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when (status) {
        OrderStatus.DELIVERED -> StatusActive
        OrderStatus.REJECTED, OrderStatus.CANCELLED -> PremiumUrgent
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
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.dimens.spaceM,
                vertical = MaterialTheme.dimens.spaceXS,
            ),
        )
    }
}

@Composable
private fun PharmacyOrderInfoRow(
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
            color = PharmaNeutral600,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
