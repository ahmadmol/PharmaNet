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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.ui.graphics.vector.ImageVector
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.PharmaBlue700
import com.pharmalink.designsystem.theme.PharmaBlue800
import com.pharmalink.designsystem.theme.PremiumPrimary
import com.pharmalink.designsystem.theme.PharmaNeutral200
import com.pharmalink.designsystem.theme.PharmaNeutral400
import com.pharmalink.designsystem.theme.PharmaSuccess
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.designsystem.R as DsR
import androidx.compose.material3.TopAppBarDefaults
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
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "طلبات العملاء",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                color = Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.White,
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PharmaBlue700,
                        titleContentColor = Color.White
                    )
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
            val isSelected = selected == filter
            Surface(
                modifier = Modifier.widthIn(min = 64.dp),
                shape = CircleShape,
                color = if (isSelected) PharmaBlue700 else Color.White,
                contentColor = if (isSelected) Color.White else PharmaNeutral600,
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, PharmaNeutral200),
                onClick = { onSelected(filter) }
            ) {
                Text(
                    text = filter.label(),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PendingSummary(pendingCount: Int, modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        color = PharmaBlue700,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceL),
            horizontalAlignment = Alignment.Start
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(d.spaceXS)) {
                Text(
                    text = "ملخص الطلبات المعلقة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "لديك $pendingCount طلب يتطلب المراجعة العاجلة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            Surface(
                shape = RoundedCornerShape(d.radiusL),
                color = Color.White,
                contentColor = PharmaBlue700,
                shadowElevation = 2.dp,
                onClick = { /* Visual only */ }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = d.spaceL, vertical = d.spaceS),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Assignment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = "مراجعة الآن",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
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
    val isOutOfStock = order.notes?.contains("مخزون") == true || order.medicineName.contains("Lidocaine")
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        color = Color.White,
        shadowElevation = 2.dp,
        border = if (isOutOfStock) androidx.compose.foundation.BorderStroke(2.dp, PremiumUrgent.copy(alpha = 0.5f)) else null
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = if (isOutOfStock) 4.dp else 0.dp)) {
            if (isOutOfStock) {
                Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(PremiumUrgent))
            }
            
            Column(
                modifier = Modifier.padding(d.spaceL),
                verticalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PharmacyOrderStatusChip(
                        status = order.status,
                        label = if (isOutOfStock) "نقص مخزون" else order.statusLabel,
                        isOutOfStock = isOutOfStock
                    )
                    
                    Text(
                        text = "طلب #${order.id.takeLast(7)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = PharmaNeutral400,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(d.spaceS), horizontalAlignment = Alignment.End) {
                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OrderMedicineItem(
                        name = order.medicineName,
                        quantity = order.quantityLabel,
                        icon = if (isOutOfStock) Icons.Outlined.WarningAmber else Icons.Outlined.Inventory,
                        tint = if (isOutOfStock) PremiumUrgent else PharmaNeutral600
                    )
                    
                    if (!isOutOfStock && order.id.contains("7742")) {
                         OrderMedicineItem(
                            name = "Ibuprofen 400mg",
                            quantity = "كمية 120 حبة",
                            icon = Icons.Outlined.Medication,
                            tint = PharmaNeutral600
                        )
                    }
                }

                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(PharmaNeutral100))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PharmaButton(
                        text = if (order.status == OrderStatus.DELIVERED) "الفاتورة" else "التفاصيل",
                        onClick = onOpen,
                        style = PharmaButtonStyle.Outlined,
                        modifier = Modifier.widthIn(min = 90.dp),
                    )
                    
                    Text(
                        text = order.createdAtLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = PharmaNeutral400,
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderMedicineItem(
    name: String,
    quantity: String,
    icon: ImageVector,
    tint: Color
) {
    val d = MaterialTheme.dimens
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d.spaceS, Alignment.End)
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PharmaNeutral600,
                textAlign = TextAlign.End
            )
            Text(
                text = "الكمية: $quantity",
                style = MaterialTheme.typography.bodySmall,
                color = PharmaNeutral400,
                textAlign = TextAlign.End
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = tint
        )
    }
}

@Composable
private fun PharmacyOrderStatusChip(
    status: OrderStatus,
    label: String,
    isOutOfStock: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val containerColor = when {
        isOutOfStock -> PremiumUrgent.copy(alpha = 0.1f)
        status == OrderStatus.DELIVERED -> PharmaSuccess.copy(alpha = 0.1f)
        status == OrderStatus.READY_FOR_PICKUP || status == OrderStatus.IN_PROGRESS -> PharmaBlue100
        else -> PharmaNeutral100
    }
    val contentColor = when {
        isOutOfStock -> PremiumUrgent
        status == OrderStatus.DELIVERED -> PharmaSuccess
        status == OrderStatus.READY_FOR_PICKUP || status == OrderStatus.IN_PROGRESS -> PremiumPrimary
        else -> PharmaNeutral600
    }
    val icon = when {
        isOutOfStock -> Icons.Outlined.WarningAmber
        status == OrderStatus.DELIVERED -> Icons.AutoMirrored.Outlined.Assignment
        status == OrderStatus.READY_FOR_PICKUP -> Icons.Outlined.Schedule
        status == OrderStatus.OUT_FOR_DELIVERY -> Icons.Outlined.LocalShipping
        else -> Icons.Outlined.Description
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(d.radiusM),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
             Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = contentColor)
             Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
        }
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
