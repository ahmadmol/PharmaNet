package com.pharmalink.feature.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pharmalink.designsystem.stitch.StitchTheme
import com.pharmalink.designsystem.stitch.components.StitchButton
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.PharmaBlue500
import com.pharmalink.designsystem.theme.PharmaNeutral100
import com.pharmalink.designsystem.theme.PharmaNeutral600
import com.pharmalink.designsystem.theme.PharmaSuccess
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.OrderStatus

private enum class OrdersTab(val label: String) {
    Completed("المكتملة"),
    Shipping("قيد الشحن"),
    Review("قيد المراجعة"),
}

@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel = hiltViewModel(),
    onOpenOrder: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(OrdersTab.Review) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        when {
            uiState.errorMessage != null -> {
                OrdersErrorState(
                    message = uiState.errorMessage.orEmpty(),
                    onRetry = viewModel::refreshOrders,
                )
            }
            uiState.isLoading && uiState.orders.isEmpty() -> {
                OrdersLoadingState()
            }
            uiState.orders.isEmpty() -> {
                OrdersEmptyScreen(onRefresh = viewModel::refreshOrders)
            }
            else -> {
                OrdersContent(
                    orders = uiState.orders,
                    isRefreshing = uiState.isLoading,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onRefresh = viewModel::refreshOrders,
                    onOpenOrder = onOpenOrder,
                )
            }
        }
    }
}

@Composable
private fun OrdersContent(
    orders: List<OrderItem>,
    isRefreshing: Boolean,
    selectedTab: OrdersTab,
    onTabSelected: (OrdersTab) -> Unit,
    onRefresh: () -> Unit,
    onOpenOrder: (String) -> Unit,
) {
    val d = MaterialTheme.dimens
    val filteredOrders = remember(orders, selectedTab) {
        orders.filter { it.matchesTab(selectedTab) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ClinicalCanvas),
        contentPadding = PaddingValues(bottom = d.spaceXL),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        item {
            OrdersHeader(onRefresh = onRefresh)
        }
        item {
            OrdersStatusTabs(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                modifier = Modifier.padding(horizontal = d.spaceL),
            )
        }
        item {
            OrdersFilterChips(modifier = Modifier.padding(horizontal = d.spaceL))
        }
        if (isRefreshing) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = d.spaceL),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            }
        }
        if (filteredOrders.isEmpty()) {
            item {
                OrdersEmptyHistorySection(
                    message = "لا توجد طلبات ضمن هذا التصنيف حالياً",
                    modifier = Modifier.padding(start = d.spaceL, top = d.spaceXL, end = d.spaceL),
                )
            }
        } else {
            items(filteredOrders, key = { it.id }) { order ->
                OrderCard(
                    order = order,
                    modifier = Modifier.padding(horizontal = d.spaceL),
                    onOpenOrder = onOpenOrder,
                )
            }
            item {
                OrdersEmptyHistorySection(
                    message = "يتم عرض أحدث الطلبات فقط",
                    modifier = Modifier.padding(start = d.spaceL, top = d.spaceM, end = d.spaceL),
                )
            }
        }
    }
}

@Composable
private fun OrdersHeader(onRefresh: () -> Unit) {
    val d = MaterialTheme.dimens

    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = d.spaceM, vertical = d.spaceS),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.orders_screen_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            HeaderIcon(icon = Icons.Outlined.Refresh, onClick = onRefresh)
        }
    }
}

@Composable
private fun HeaderIcon(icon: ImageVector, onClick: () -> Unit) {
    val d = MaterialTheme.dimens

    Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = PharmaNeutral100, contentColor = MaterialTheme.colorScheme.onSurface) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(d.iconS))
        }
    }
}

@Composable
private fun OrdersStatusTabs(
    selectedTab: OrdersTab,
    onTabSelected: (OrdersTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(modifier = modifier.fillMaxWidth(), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)) {
        Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OrdersTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    contentColor = if (selected) PharmaBlue500 else MaterialTheme.colorScheme.onSurfaceVariant,
                    shadowElevation = if (selected) 1.dp else 0.dp,
                ) {
                    Box(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(),
                                onClick = { onTabSelected(tab) },
                            )
                            .padding(vertical = d.spaceS),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(tab.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersFilterChips(modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens
    val chips = listOf(
        "التاريخ" to Icons.Outlined.CalendarToday,
        "المستودع" to Icons.Outlined.Inventory2,
        "نوع الطلب" to Icons.Outlined.FilterList,
    )

    LazyRow(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.spaceS)) {
        items(chips) { (label, icon) ->
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                shadowElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceS),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(d.iconS))
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: OrderItem,
    modifier: Modifier = Modifier,
    onOpenOrder: (String) -> Unit,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)),
    ) {
        Column(modifier = Modifier.padding(d.spaceL), verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(d.spaceXXS)) {
                    OrderStatusChip(order)
                    Text(
                        text = "طلب رقم ${order.orderNumber}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.spaceXS)) {
                        Icon(Icons.Outlined.HomeWork, contentDescription = null, modifier = Modifier.size(d.iconS), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = order.warehouseName ?: order.supplierName ?: order.medicineName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(order.date, style = MaterialTheme.typography.labelSmall, color = PharmaNeutral600, textAlign = TextAlign.End)
            }

            OrderProgressTimeline(status = order.statusType)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.spaceS)) {
                StitchButton(
                    onClick = { onOpenOrder(order.id) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = PaddingValues(horizontal = d.spaceM, vertical = d.spaceS),
                ) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(d.iconS))
                    Spacer(Modifier.width(d.spaceS))
                    Text("عرض التفاصيل", fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Surface(
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Outlined.ChatBubble, contentDescription = null, modifier = Modifier.size(d.iconS))
                        Spacer(Modifier.width(d.spaceS))
                        Text("تواصل", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderStatusChip(order: OrderItem) {
    val color = when (order.statusType) {
        OrderStatus.PENDING -> PharmaNeutral600
        OrderStatus.QUOTE_PENDING -> PharmaNeutral600
        OrderStatus.CONFIRMED,
        OrderStatus.IN_PROGRESS,
        OrderStatus.READY_FOR_PICKUP,
        OrderStatus.OUT_FOR_DELIVERY -> PharmaBlue500
        OrderStatus.DELIVERED -> PharmaSuccess
        OrderStatus.REJECTED,
        OrderStatus.CANCELLED -> PremiumUrgent
    }

    Surface(shape = CircleShape, color = color.copy(alpha = 0.12f), contentColor = color) {
        Text(
            text = order.status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun OrderProgressTimeline(status: OrderStatus) {
    val d = MaterialTheme.dimens
    val activeIndex = when (status) {
        OrderStatus.PENDING,
        OrderStatus.QUOTE_PENDING,
        OrderStatus.REJECTED,
        OrderStatus.CANCELLED -> 0
        OrderStatus.CONFIRMED,
        OrderStatus.IN_PROGRESS,
        OrderStatus.READY_FOR_PICKUP,
        OrderStatus.OUT_FOR_DELIVERY -> 1
        OrderStatus.DELIVERED -> 2
    }
    val labels = listOf("قيد المراجعة", "قيد الشحن", "تم التوصيل")

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = d.spaceM)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {}
        Row(
            modifier = Modifier
                .fillMaxWidth((activeIndex + 1) / 3f)
                .align(Alignment.CenterEnd)
                .height(2.dp)
                .background(PharmaBlue500),
        ) {}
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { index, label ->
                val active = index <= activeIndex
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(d.spaceXS)) {
                    Surface(
                        modifier = Modifier.size(16.dp),
                        shape = CircleShape,
                        color = if (active) PharmaBlue500 else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(4.dp, MaterialTheme.colorScheme.surface),
                    ) {}
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (index == activeIndex) FontWeight.Bold else FontWeight.Medium,
                        color = if (index == activeIndex) PharmaBlue500 else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun OrdersEmptyHistorySection(
    message: String,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = d.spaceXL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = PharmaNeutral100, contentColor = MaterialTheme.colorScheme.outline) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.HistoryEdu, contentDescription = null, modifier = Modifier.size(d.iconL))
            }
        }
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Text("آخر الطلبات المتاحة", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = PharmaBlue500)
    }
}

@Composable
private fun OrdersLoadingState() {
    val d = MaterialTheme.dimens

    Box(modifier = Modifier.fillMaxSize().background(ClinicalCanvas), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(d.spaceS)) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PharmaBlue500, strokeWidth = 2.dp)
            Text(stringResource(R.string.order_loading_message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OrdersErrorState(message: String, onRetry: () -> Unit) {
    val d = MaterialTheme.dimens

    Box(modifier = Modifier.fillMaxSize().background(ClinicalCanvas).padding(d.spaceL), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(d.radiusXXL), color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
            Column(modifier = Modifier.padding(d.spaceL), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
                Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                StitchButton(onClick = onRetry) {
                    Text(stringResource(R.string.order_retry_loading))
                }
            }
        }
    }
}

@Composable
private fun OrdersEmptyScreen(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(ClinicalCanvas),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OrdersHeader(onRefresh = onRefresh)
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            OrdersEmptyHistorySection(message = stringResource(R.string.order_list_empty), modifier = Modifier.padding(horizontal = MaterialTheme.dimens.spaceL))
        }
    }
}

private fun OrderItem.matchesTab(tab: OrdersTab): Boolean = when (tab) {
    OrdersTab.Completed -> statusType == OrderStatus.DELIVERED
    OrdersTab.Shipping -> statusType == OrderStatus.CONFIRMED || 
                            statusType == OrderStatus.IN_PROGRESS ||
                            statusType == OrderStatus.READY_FOR_PICKUP ||
                            statusType == OrderStatus.OUT_FOR_DELIVERY
    OrdersTab.Review -> statusType == OrderStatus.PENDING || statusType == OrderStatus.REJECTED
}

@Preview(showBackground = true)
@Composable
fun OrdersScreenPreview() {
    StitchTheme {
        OrdersContent(
            orders = listOf(
                OrderItem(
                    id = "109",
                    requestId = "109",
                    orderNumber = "#109",
                    date = "٢٠٢٦/٠٤/١٢",
                    status = "قيد الشحن",
                    statusType = OrderStatus.CONFIRMED,
                    totalAmount = "250.00 ر.س",
                    warehouseName = "مستودع الأمل",
                    medicineName = "دواء",
                    quantity = 12,
                    unit = "علبة",
                ),
            ),
            isRefreshing = false,
            selectedTab = OrdersTab.Shipping,
            onTabSelected = {},
            onRefresh = {},
            onOpenOrder = {},
        )
    }
}
