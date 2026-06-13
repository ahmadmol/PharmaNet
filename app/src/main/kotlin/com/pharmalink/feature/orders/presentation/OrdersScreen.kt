package com.pharmalink.feature.orders.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.R
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.designsystem.components.PharmaSectionHeader
import com.pharmalink.designsystem.components.PharmaStateView
import com.pharmalink.designsystem.components.PharmaStatusChip
import com.pharmalink.designsystem.components.StatusTone
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onOpenOrder: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OrdersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val d = MaterialTheme.dimens
    val pagerState = rememberPagerState(pageCount = { 4 }, initialPage = 0)
    val scope = rememberCoroutineScope()
    var urgentOnly by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClinicalCanvas),
    ) {
        Text(
            text = stringResource(R.string.orders_screen_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = d.spaceL, vertical = d.spaceL),
        )

        PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
            Tab(selected = pagerState.currentPage == 0, onClick = { scope.launch { pagerState.scrollToPage(0) } }, text = { Text(stringResource(R.string.orders_tab_pending)) })
            Tab(selected = pagerState.currentPage == 1, onClick = { scope.launch { pagerState.scrollToPage(1) } }, text = { Text(stringResource(R.string.orders_tab_approved)) })
            Tab(selected = pagerState.currentPage == 2, onClick = { scope.launch { pagerState.scrollToPage(2) } }, text = { Text(stringResource(R.string.orders_tab_rejected)) })
            Tab(selected = pagerState.currentPage == 3, onClick = { scope.launch { pagerState.scrollToPage(3) } }, text = { Text(stringResource(R.string.orders_tab_delivered)) })
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = d.spaceL, vertical = d.spaceM),
            horizontalArrangement = Arrangement.spacedBy(d.spaceS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = urgentOnly,
                onClick = { urgentOnly = !urgentOnly },
                label = { Text(stringResource(R.string.orders_filter_urgent)) },
                leadingIcon = { Icon(Icons.Outlined.PriorityHigh, contentDescription = null, tint = PremiumUrgent) },
            )
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text(stringResource(R.string.orders_filter_date)) },
                leadingIcon = { Icon(Icons.Outlined.CalendarToday, contentDescription = null) },
            )
        }

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = d.space6XL),
            ) { page ->
                val list = when (page) {
                    0 -> state.pending
                    1 -> state.approved
                    2 -> state.rejected
                    else -> state.delivered
                }.let { if (urgentOnly) it.filter { order -> order.isUrgent } else it }

                when (val screenState = state.screenState) {
                    ScreenState.Loading -> PharmaStateView(
                        title = stringResource(R.string.orders_loading_title),
                        subtitle = stringResource(R.string.orders_loading_subtitle),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    is ScreenState.Error -> PharmaStateView(
                        title = stringResource(R.string.orders_empty_title),
                        subtitle = screenState.message ?: stringResource(R.string.orders_error_load_fallback),
                        isError = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    is ScreenState.Offline -> PharmaStateView(
                        title = stringResource(R.string.orders_empty_title),
                        subtitle = screenState.message ?: stringResource(R.string.orders_offline_fallback),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ScreenState.Empty -> PharmaStateView(
                        title = stringResource(R.string.orders_empty_title),
                        subtitle = stringResource(R.string.orders_empty_subtitle),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    is ScreenState.Success -> {
                        if (list.isEmpty()) {
                            PharmaStateView(
                                title = stringResource(R.string.orders_empty_title),
                                subtitle = stringResource(R.string.orders_empty_subtitle),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(d.spaceL),
                                verticalArrangement = Arrangement.spacedBy(d.spaceM),
                            ) {
                                item { PharmaSectionHeader(title = stringResource(R.string.orders_section_operational)) }
                                items(list, key = { it.id }) { order ->
                                    OrderCard(order = order, onClick = { onOpenOrder(order.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    onClick: () -> Unit,
) {
    val d = MaterialTheme.dimens
    val tone = when (order.status) {
        OrderStatus.PENDING -> StatusTone.Pending
        OrderStatus.QUOTE_PENDING -> StatusTone.Pending
        OrderStatus.CONFIRMED,
        OrderStatus.IN_PROGRESS,
        OrderStatus.READY_FOR_PICKUP,
        OrderStatus.OUT_FOR_DELIVERY -> StatusTone.Success
        OrderStatus.REJECTED,
        OrderStatus.CANCELLED -> StatusTone.Warning
        OrderStatus.DELIVERED -> StatusTone.Neutral
    }
    val statusLabel = when (order.status) {
        OrderStatus.PENDING -> stringResource(R.string.order_status_pending)
        OrderStatus.QUOTE_PENDING -> "بانتظار موافقة الصيدلية"
        OrderStatus.CONFIRMED -> stringResource(R.string.order_status_approved)
        OrderStatus.IN_PROGRESS -> "قيد التجهيز"
        OrderStatus.READY_FOR_PICKUP -> "جاهز للاستلام"
        OrderStatus.OUT_FOR_DELIVERY -> "قيد التوصيل"
        OrderStatus.REJECTED -> stringResource(R.string.order_status_rejected)
        OrderStatus.CANCELLED -> "ملغي"
        OrderStatus.DELIVERED -> stringResource(R.string.order_status_delivered)
    }
    val trackBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(Modifier.padding(d.spaceL)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(order.medicineName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                PharmaStatusChip(label = statusLabel, tone = tone)
            }
            Spacer(Modifier.height(d.spaceS))
            Text(formatInstantToDisplay(order.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(d.spaceXS))
            Text("${order.quantity} ${order.unit} • ${order.warehouseName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(d.spaceXS))
            Text(order.requestId.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(d.spaceS))
            if (order.isUrgent) {
                Text(stringResource(R.string.orders_filter_urgent), color = PremiumUrgent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(d.spaceM))
            Row(
                modifier = Modifier.fillMaxWidth().background(trackBg, MaterialTheme.shapes.medium).padding(vertical = d.spaceS),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = d.spaceXS))
                Text(text = stringResource(R.string.order_track), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(d.spaceS))
            Text(formatInstantToDisplay(order.updatedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatInstantToDisplay(instant: java.time.Instant?): String {
    return instant?.let {
        val formatter = java.time.format.DateTimeFormatter
            .ofPattern("yyyy/MM/dd")
            .withZone(java.time.ZoneId.systemDefault())
        formatter.format(it)
    } ?: "-"
}
