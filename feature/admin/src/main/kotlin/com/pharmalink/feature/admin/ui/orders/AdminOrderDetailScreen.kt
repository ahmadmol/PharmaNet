package com.pharmalink.feature.admin.ui.orders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.components.PharmaCard
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
fun AdminOrderDetailScreen(
    orderId: String,
    onBackClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    profileImageUrl: String? = null,
    modifier: Modifier = Modifier,
    viewModel: AdminOrderDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    CollectEffect(effect = viewModel.effect) { effect ->
        when (effect) {
            is AdminOrderDetailEffect.ShowMessage -> {
                snackbarHostState.showSnackbar(effect.message)
            }
            AdminOrderDetailEffect.NavigateBack -> onBackClick()
        }
    }

    AdminOrderDetailContent(
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
private fun AdminOrderDetailContent(
    state: AdminOrderDetailUiState,
    onAction: (AdminOrderDetailAction) -> Unit,
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
                            text = "تفاصيل الطلب",
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f))
            }
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingContent(modifier = Modifier.padding(padding))
            state.contentError.isNotEmpty() -> ErrorContent(
                message = state.contentError,
                onRetry = { onAction(AdminOrderDetailAction.OnRetryClicked) },
                modifier = Modifier.padding(padding),
            )
            state.order == null -> EmptyContent(modifier = Modifier.padding(padding))
            else -> SuccessContent(
                order = state.order,
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
        repeat(8) {
            PharmaSkeletonLine(heightDp = 100f)
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
            title = "خطأ في تحميل تفاصيل الطلب",
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
            title = "الطلب غير موجود",
            subtitle = "لم يتم العثور على تفاصيل هذا الطلب",
            tone = PharmaStateTone.Neutral,
        )
    }
}

@Composable
private fun SuccessContent(
    order: AdminOrder,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(d.spaceL),
        verticalArrangement = Arrangement.spacedBy(d.spaceL),
    ) {
        // Order ID & Type
        item {
            InfoCard(title = "معلومات الطلب") {
                InfoRow(label = "رقم الطلب", value = order.id)
                InfoRow(
                    label = "نوع الطلب",
                    value = if (order.orderType == "CUSTOMER_PHARMACY") "B2C - عميل → صيدلية" else "B2B - صيدلية → مستودع"
                )
                InfoRow(
                    label = "الحالة",
                    value = when (order.status) {
                        OrderStatus.PENDING -> "معلق"
                        OrderStatus.QUOTE_PENDING -> "بانتظار الموافقة"
                        OrderStatus.CONFIRMED -> "مؤكد"
                        OrderStatus.IN_PROGRESS -> "قيد التنفيذ"
                        OrderStatus.DELIVERED -> "مسلم"
                        OrderStatus.READY_FOR_PICKUP -> "جاهز للاستلام"
                        OrderStatus.OUT_FOR_DELIVERY -> "قيد التوصيل"
                        OrderStatus.CANCELLED -> "ملغي"
                        OrderStatus.REJECTED -> "مرفوض"
                    }
                )
                if (order.isUrgent) {
                    InfoRow(label = "الأولوية", value = "⚡ مستعجل", valueColor = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Medicine Info
        item {
            InfoCard(title = "معلومات الدواء") {
                InfoRow(label = "اسم الدواء", value = order.medicineName)
                InfoRow(label = "الكمية", value = "${order.quantity} ${order.unit}")
                InfoRow(
                    label = "نوع التسليم",
                    value = when (order.fulfillmentType) {
                        FulfillmentType.DELIVERY -> "توصيل"
                        FulfillmentType.PICKUP -> "استلام"
                    }
                )
            }
        }

        // Customer Info (B2C only)
        if (order.orderType == "CUSTOMER_PHARMACY") {
            val customerName = order.customerName
            if (customerName != null) {
                item {
                    InfoCard(title = "معلومات العميل") {
                        InfoRow(label = "اسم العميل", value = customerName)
                        InfoRow(label = "رقم العميل", value = order.customerId ?: "--")
                    }
                }
            }
        }

        // Pharmacy Info
        val pharmacyName = order.pharmacyName
        if (pharmacyName != null) {
            item {
                InfoCard(title = "معلومات الصيدلية") {
                    InfoRow(label = "اسم الصيدلية", value = pharmacyName)
                    InfoRow(label = "رقم الصيدلية", value = order.pharmacyId ?: "--")
                }
            }
        }

        // Warehouse Info (B2B only)
        if (order.orderType == "PHARMACY_WAREHOUSE") {
            val warehouseName = order.warehouseName
            if (warehouseName != null) {
                item {
                    InfoCard(title = "معلومات المستودع") {
                        InfoRow(label = "اسم المستودع", value = warehouseName)
                        InfoRow(label = "رقم المستودع", value = order.warehouseId ?: "--")
                    }
                }
            }
        }

        // Pricing Info
        val totalPriceCents = order.totalPriceCents
        if (totalPriceCents != null) {
            item {
                InfoCard(title = "معلومات السعر") {
                    InfoRow(
                        label = "السعر الإجمالي",
                        value = "${totalPriceCents / 100.0} ${order.currency}",
                        valueColor = MaterialTheme.colorScheme.primary,
                        valueFontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Timestamps
        item {
            InfoCard(title = "التواريخ") {
                InfoRow(label = "تاريخ الإنشاء", value = order.createdAt)
                InfoRow(label = "آخر تحديث", value = order.updatedAt)
                order.confirmedAt?.let {
                    InfoRow(label = "تاريخ التأكيد", value = it)
                }
                order.fulfilledAt?.let {
                    InfoRow(label = "تاريخ التسليم", value = it)
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val d = MaterialTheme.dimens

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
            slideInVertically(animationSpec = tween(durationMillis = 180)) { it / 12 },
        exit = fadeOut(animationSpec = tween(durationMillis = 120)),
    ) {
        PharmaCard(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(durationMillis = 180)),
            containerColor = MaterialTheme.colorScheme.surface,
            elevationDp = 2f,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))

                content()
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueFontWeight: FontWeight = FontWeight.Normal,
) {
    val d = MaterialTheme.dimens

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        
        Spacer(Modifier.width(d.spaceM))
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = valueFontWeight,
            color = valueColor,
            modifier = Modifier.weight(0.6f),
        )
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun PreviewAdminOrderDetailScreen() {
    PharmaTheme {
        AdminOrderDetailContent(
            state = AdminOrderDetailUiState(
                order = AdminOrder(
                    id = "550e8400-e29b-41d4-a716-446655440000",
                    orderType = "CUSTOMER_PHARMACY",
                    status = OrderStatus.CONFIRMED,
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
                    updatedAt = "2026-05-06 11:00",
                    confirmedAt = "2026-05-06 11:00",
                    fulfilledAt = null,
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
