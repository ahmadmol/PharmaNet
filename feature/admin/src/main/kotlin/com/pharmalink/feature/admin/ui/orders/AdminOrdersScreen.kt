package com.pharmalink.feature.admin.ui.orders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.pharmalink.designsystem.theme.StatusActive
import com.pharmalink.designsystem.theme.StatusInfo
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaSkeletonLine
import com.pharmalink.designsystem.components.PharmaStateView
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.theme.PharmaTheme
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.designsystem.utils.CollectEffect
import com.pharmalink.feature.admin.ui.components.AdminProfileAvatarButton
import com.pharmalink.domain.model.AdminOrder
import com.pharmalink.domain.model.FulfillmentType
import com.pharmalink.domain.model.OrderStatus
import com.pharmalink.feature.admin.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(
    onBackClick: () -> Unit,
    onNavigateToOrderDetail: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    profileImageUrl: String? = null,
    modifier: Modifier = Modifier,
    viewModel: AdminOrdersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    CollectEffect(effect = viewModel.effect) { effect ->
        when (effect) {
            is AdminOrdersEffect.ShowMessage -> {
                snackbarHostState.showSnackbar(effect.message)
            }
            is AdminOrdersEffect.NavigateToOrderDetail -> {
                onNavigateToOrderDetail(effect.orderId)
                }
            }
    }

    AdminOrdersContent(
        state = state,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        onNavigateToProfile = onNavigateToProfile,
        profileImageUrl = profileImageUrl,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminOrdersContent(
    state: AdminOrdersUiState,
    onAction: (AdminOrdersAction) -> Unit,
    onBackClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    profileImageUrl: String? = null,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "جميع الطلبات",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    actions = {
                        AdminProfileAvatarButton(
                            profileImageUrl = profileImageUrl,
                            contentDescription = stringResource(R.string.admin_profile_cd),
                            onClick = onNavigateToProfile,
                        )
                        Spacer(Modifier.width(d.spaceM))
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))
            }
        },
    ) { padding ->
        when {
            state.isLoading && state.orders.isEmpty() -> LoadingContent(modifier = Modifier.padding(padding))
            state.contentError.isNotEmpty() && state.orders.isEmpty() -> ErrorContent(
                message = state.contentError,
                onRetry = { onAction(AdminOrdersAction.OnRetryClicked) },
                modifier = Modifier.padding(padding),
            )
            state.orders.isEmpty() -> EmptyContent(modifier = Modifier.padding(padding))
            else -> SuccessContent(
                state = state,
                onAction = onAction,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(d.spaceL),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        PharmaSkeletonLine(heightDp = 60f)
        PharmaSkeletonLine(heightDp = 50f)
        repeat(5) {
            PharmaSkeletonLine(heightDp = 140f)
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(d.spaceL),
    ) {
        PharmaStateView(
            title = "خطأ في تحميل الطلبات",
            subtitle = message,
            tone = PharmaStateTone.Error,
            actionLabel = "إعادة المحاولة",
            onAction = onRetry,
        )
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(d.spaceL),
    ) {
        PharmaStateView(
            title = "لا توجد طلبات",
            subtitle = "لم يتم العثور على أي طلبات",
            tone = PharmaStateTone.Neutral,
        )
    }
}

@Composable
private fun SuccessContent(
    state: AdminOrdersUiState,
    onAction: (AdminOrdersAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val listState = rememberLazyListState()

    // Pagination trigger
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= state.orders.size - 5
        }
    }

    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore }
            .collect { should ->
                if (should && state.hasMore && !state.isLoading) {
                    onAction(AdminOrdersAction.OnLoadMore)
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(d.spaceL),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        // Search Field
        item {
            SearchField(
                value = state.searchQuery,
                onValueChange = { onAction(AdminOrdersAction.OnSearchQueryChanged(it)) },
                placeholder = "البحث في اسم الدواء...",
            )
        }

        // Filter Chips
        item {
            Crossfade(
                targetState = state.selectedFilter,
                animationSpec = tween(durationMillis = 180),
                label = "admin_orders_filter_content",
            ) { selectedFilter ->
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                ) {
                item {
                    FilterChip(
                        selected = selectedFilter == OrderFilter.ALL,
                        onClick = { onAction(AdminOrdersAction.OnFilterSelected(OrderFilter.ALL)) },
                        label = { Text("الكل") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == OrderFilter.B2C,
                        onClick = { onAction(AdminOrdersAction.OnFilterSelected(OrderFilter.B2C)) },
                        label = { Text("B2C") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == OrderFilter.B2B,
                        onClick = { onAction(AdminOrdersAction.OnFilterSelected(OrderFilter.B2B)) },
                        label = { Text("B2B") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == OrderFilter.URGENT,
                        onClick = { onAction(AdminOrdersAction.OnFilterSelected(OrderFilter.URGENT)) },
                        label = { Text("مستعجل") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == OrderFilter.PENDING,
                        onClick = { onAction(AdminOrdersAction.OnFilterSelected(OrderFilter.PENDING)) },
                        label = { Text("معلق") }
                    )
                }
                }
            }
        }

        // Orders List
        items(
            items = state.orders,
            key = { it.id },
        ) { order ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                    slideInVertically(animationSpec = tween(durationMillis = 180)) { it / 12 },
                modifier = Modifier.animateItem(),
            ) {
                OrderCard(
                    order = order,
                    onClick = { onAction(AdminOrdersAction.OnOrderClicked(order.id)) },
                )
            }
        }

        // Loading More Indicator
        if (state.isLoading && state.orders.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(d.spaceM),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "جاري التحميل...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        singleLine = true,
    )
}

@Composable
private fun OrderCard(
    order: AdminOrder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "admin_order_card_press",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .animateContentSize(animationSpec = tween(durationMillis = 180))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Order Type Badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (order.orderType == "CUSTOMER_PHARMACY") {
                        StatusInfo.copy(alpha = 0.15f)
                    } else {
                        StatusActive.copy(alpha = 0.15f)
                    },
                ) {
                    Text(
                        text = if (order.orderType == "CUSTOMER_PHARMACY") "B2C" else "B2B",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (order.orderType == "CUSTOMER_PHARMACY") {
                            StatusInfo
                        } else {
                            StatusActive
                        },
                        modifier = Modifier.padding(
                            horizontal = d.spaceS,
                            vertical = d.spaceXS,
                        ),
                    )
                }

                // Status Badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (order.status) {
                        OrderStatus.PENDING -> MaterialTheme.colorScheme.primaryContainer
                        OrderStatus.CONFIRMED -> StatusActive.copy(alpha = 0.15f)
                        OrderStatus.DELIVERED -> StatusActive.copy(alpha = 0.15f)
                        OrderStatus.READY_FOR_PICKUP -> StatusActive.copy(alpha = 0.15f)
                        OrderStatus.OUT_FOR_DELIVERY -> StatusActive.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.errorContainer
                    },
                ) {
                    Text(
                        text = when (order.status) {
                            OrderStatus.PENDING -> "معلق"
                            OrderStatus.QUOTE_PENDING -> "بانتظار الموافقة"
                            OrderStatus.CONFIRMED -> "مؤكد"
                            OrderStatus.IN_PROGRESS -> "قيد التنفيذ"
                            OrderStatus.DELIVERED -> "مسلم"
                            OrderStatus.READY_FOR_PICKUP -> "جاهز للاستلام"
                            OrderStatus.OUT_FOR_DELIVERY -> "قيد التوصيل"
                            OrderStatus.CANCELLED -> "ملغي"
                            OrderStatus.REJECTED -> "مرفوض"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when (order.status) {
                            OrderStatus.PENDING -> MaterialTheme.colorScheme.primary
                            OrderStatus.CONFIRMED -> StatusActive
                            OrderStatus.DELIVERED -> StatusActive
                            OrderStatus.READY_FOR_PICKUP -> StatusActive
                            OrderStatus.OUT_FOR_DELIVERY -> StatusActive
                            else -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(
                            horizontal = d.spaceS,
                            vertical = d.spaceXS,
                        ),
                    )
                }
            }

            // Medicine Name
            Text(
                text = order.medicineName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Quantity
            Text(
                text = "${order.quantity} ${order.unit}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Pharmacy/Customer Info
            if (order.orderType == "CUSTOMER_PHARMACY") {
                order.customerName?.let {
                    Text(
                        text = "العميل: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                order.pharmacyName?.let {
                    Text(
                        text = "الصيدلية: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                order.pharmacyName?.let {
                    Text(
                        text = "الصيدلية: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                order.warehouseName?.let {
                    Text(
                        text = "المستودع: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Urgent Badge
            if (order.isUrgent) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = "⚡ مستعجل",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(
                            horizontal = d.spaceS,
                            vertical = d.spaceXS,
                        ),
                    )
                }
            }

            // Price
            order.totalPriceCents?.let { cents ->
                Text(
                    text = "${cents / 100.0} ${order.currency}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Date
            Text(
                text = order.createdAt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun PreviewAdminOrdersScreen() {
    PharmaTheme {
        AdminOrdersContent(
            state = AdminOrdersUiState(
                orders = listOf(
                    AdminOrder(
                        id = "1",
                        orderType = "CUSTOMER_PHARMACY",
                        status = OrderStatus.PENDING,
                        medicineName = "باراسيتامول 500 ملغ",
                        quantity = 2,
                        unit = "علبة",
                        pharmacyId = "ph1",
                        pharmacyName = "صيدلية النهدي",
                        warehouseId = null,
                        warehouseName = null,
                        customerId = "c1",
                        customerName = "أحمد محمد",
                        isUrgent = true,
                        totalPriceCents = 5000,
                        currency = "SAR",
                        fulfillmentType = FulfillmentType.DELIVERY,
                        createdAt = "2026-05-06 10:30",
                        updatedAt = "2026-05-06 10:30",
                        confirmedAt = null,
                        fulfilledAt = null,
                    ),
                ),
            ),
            onAction = {},
            onBackClick = {},
            onNavigateToProfile = {},
            profileImageUrl = null,
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
