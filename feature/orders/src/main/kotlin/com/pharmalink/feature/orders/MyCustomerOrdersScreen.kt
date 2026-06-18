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
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.PharmaNeutral100
import com.pharmalink.designsystem.theme.PharmaNeutral400
import com.pharmalink.designsystem.theme.PharmaNeutral600
import com.pharmalink.designsystem.theme.PharmaNeutral900
import com.pharmalink.designsystem.theme.PharmaBlue100
import com.pharmalink.designsystem.theme.PremiumPrimary
import com.pharmalink.designsystem.theme.PremiumSecondary
import com.pharmalink.designsystem.theme.PremiumAccent
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.PharmaSuccess
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.designsystem.R as DsR
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.IconButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.Inventory2
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
    val d = MaterialTheme.dimens

    LaunchedEffect(refreshRequested) {
        if (!refreshRequested) return@LaunchedEffect
        viewModel.refreshOrders()
        onRefreshHandled()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = ClinicalCanvas,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = d.spaceL)
            ) {
                OrdersHeader()
                
                Text(
                    text = "طلباتي",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = PharmaNeutral900
                )
                Text(
                    text = "تتبع وإدارة طلبات الأدوية الخاصة بك",
                    style = MaterialTheme.typography.bodySmall,
                    color = PharmaNeutral600
                )
                
                Spacer(modifier = Modifier.height(d.spaceM))
                
                OrdersFilterTabs(
                    selectedTab = uiState.selectedFilter,
                    onTabSelected = viewModel::onFilterSelected
                )
                
                Spacer(modifier = Modifier.height(d.spaceM))

                when (val screenState = uiState.screenState) {
                    ScreenState.Loading -> {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PremiumPrimary)
                        }
                    }
                    ScreenState.Empty -> {
                        EmptyState(
                            onStartSearchClick = onStartSearchClick,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    }
                    is ScreenState.Error -> {
                        ErrorState(
                            message = screenState.message ?: stringResource(R.string.order_error_loading_failed),
                            onRetryClick = viewModel::refreshOrders,
                            title = stringResource(R.string.my_customer_orders_error_title),
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    }
                    is ScreenState.Success -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = d.spaceM),
                            verticalArrangement = Arrangement.spacedBy(d.spaceM),
                        ) {
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
                            onRetryClick = viewModel::refreshOrders,
                            title = stringResource(R.string.my_customer_orders_error_title),
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersHeader() {
    val d = MaterialTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = d.spaceM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = PharmaNeutral100,
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.padding(8.dp),
                tint = PharmaNeutral600
            )
        }

        Text(
            text = "PharmaNet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PremiumPrimary,
        )

        IconButton(onClick = { /* No-op per Rule 9 */ }) {
            Icon(
                painter = painterResource(id = DsR.drawable.ic_app_logo),
                contentDescription = null,
                modifier = Modifier.size(d.iconM),
                tint = Color.Unspecified
            )
        }
    }
}

@Composable
private fun OrdersFilterTabs(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
) {
    val d = MaterialTheme.dimens
    val tabs = listOf("الكل", "قيد المراجعة", "مؤكد", "مسلم")

    LazyRow(horizontalArrangement = Arrangement.spacedBy(d.spaceXL)) {
        items(tabs) { tab ->
            Column(
                modifier = Modifier.clickable { onTabSelected(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = tab,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (tab == selectedTab) FontWeight.Bold else FontWeight.Normal,
                    color = if (tab == selectedTab) PremiumPrimary else PharmaNeutral600
                )
                if (tab == selectedTab) {
                    Box(modifier = Modifier.width(20.dp).height(2.dp).background(PremiumPrimary, CircleShape))
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
        shape = RoundedCornerShape(d.radiusXL),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, PharmaNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Icon Placeholder
                Surface(
                    shape = CircleShape,
                    color = PharmaBlue50,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = PremiumPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = statusContainerColor(order.status).copy(alpha = 0.1f),
                            contentColor = statusContainerColor(order.status)
                        ) {
                            Text(
                                text = order.statusLabel,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = order.createdAtLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = PharmaNeutral400
                        )
                    }
                    
                    Text(
                        text = "${order.medicineName} (${order.quantity} عبوة)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PharmaNeutral900,
                    )
                    Text(
                        text = order.pharmacyName,
                        style = MaterialTheme.typography.bodySmall,
                        color = PharmaNeutral600,
                    )
                }
            }

            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(d.radiusL),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = PremiumPrimary
                ),
                border = BorderStroke(1.dp, PharmaNeutral100)
            ) {
                Text(
                    text = "عرض التفاصيل",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun statusContainerColor(status: com.pharmalink.domain.model.OrderStatus) = when (status) {
    com.pharmalink.domain.model.OrderStatus.PENDING -> PremiumAccent
    com.pharmalink.domain.model.OrderStatus.CONFIRMED -> PremiumSecondary
    com.pharmalink.domain.model.OrderStatus.DELIVERED -> PharmaSuccess
    com.pharmalink.domain.model.OrderStatus.CANCELLED,
    com.pharmalink.domain.model.OrderStatus.REJECTED -> PremiumUrgent
    else -> PremiumPrimary
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
