package com.pharmalink.feature.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaButtonStyle
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.FulfillmentType
import com.pharmalink.domain.model.OrderStatus
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PharmacyCustomerOrderDetailScreen(
    onBack: () -> Unit,
    viewModel: PharmacyCustomerOrderDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPriceDialog by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = ClinicalCanvas,
            topBar = {
                TopAppBar(
                    title = { Text("تفاصيل طلب العميل") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                )
            },
        ) { innerPadding ->
            when (val screenState = state.screenState) {
                ScreenState.Loading -> LoadingState(Modifier.padding(innerPadding))
                ScreenState.Empty -> MessageState("لم يتم العثور على الطلب", Modifier.padding(innerPadding))
                is ScreenState.Error -> MessageState(
                    screenState.message ?: "تعذر تحميل الطلب",
                    Modifier.padding(innerPadding),
                )
                is ScreenState.Success -> DetailContent(
                    order = screenState.data,
                    isActionInProgress = state.isActionInProgress,
                    actionErrorMessage = state.actionErrorMessage,
                    actionSuccessMessage = state.actionSuccessMessage,
                    onConfirm = { showPriceDialog = true },
                    onReject = viewModel::rejectOrder,
                    onReady = viewModel::markReadyForPickup,
                    onOutForDelivery = viewModel::markOutForDelivery,
                    onDelivered = viewModel::markDelivered,
                    modifier = Modifier.padding(innerPadding),
                )
                is ScreenState.Offline -> MessageState("تعذر الاتصال", Modifier.padding(innerPadding))
            }
        }
    }

    if (showPriceDialog) {
        PriceDialog(
            onDismiss = { showPriceDialog = false },
            onConfirm = { priceCents ->
                showPriceDialog = false
                viewModel.confirmOrder(priceCents)
            },
        )
    }
}

@Composable
private fun DetailContent(
    order: PharmacyCustomerOrderUi,
    isActionInProgress: Boolean,
    actionErrorMessage: String?,
    actionSuccessMessage: String?,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    onReady: () -> Unit,
    onOutForDelivery: () -> Unit,
    onDelivered: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.dimens.spaceL),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceL),
    ) {
        order.prescriptionUrl?.let { url ->
            PrescriptionImage(url = url)
        }

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
                CustomerOrderStatusChip(status = order.status, label = order.statusLabel)
                Text(order.medicineName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OrderInfoRow("العميل", order.customerName)
                OrderInfoRow("الكمية", order.quantityLabel)
                OrderInfoRow("طريقة الاستلام", order.fulfillmentLabel)
                OrderInfoRow("الأولوية", order.urgencyLabel)
                OrderInfoRow("السعر", order.priceLabel)
                order.deliveryAddress?.let { OrderInfoRow("عنوان التوصيل", it) }
                order.deliveryPhone?.let { OrderInfoRow("هاتف التوصيل", it) }
                order.notes?.let { OrderInfoRow("ملاحظات العميل", it) }
            }
        }

        if (!actionErrorMessage.isNullOrBlank()) {
            Text(actionErrorMessage, color = MaterialTheme.colorScheme.error)
        }
        if (!actionSuccessMessage.isNullOrBlank()) {
            Text(actionSuccessMessage, color = MaterialTheme.colorScheme.primary)
        }
        if (isActionInProgress) {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceS)) {
                CircularProgressIndicator()
                Text("جار تنفيذ الإجراء")
            }
        }

        ActionButtons(
            order = order,
            enabled = !isActionInProgress,
            onConfirm = onConfirm,
            onReject = onReject,
            onReady = onReady,
            onOutForDelivery = onOutForDelivery,
            onDelivered = onDelivered,
        )
    }
}

@Composable
private fun ActionButtons(
    order: PharmacyCustomerOrderUi,
    enabled: Boolean,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    onReady: () -> Unit,
    onOutForDelivery: () -> Unit,
    onDelivered: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM)) {
        when (order.status) {
            OrderStatus.QUOTE_PENDING,
            OrderStatus.PENDING -> {
                PharmaButton(
                    text = "تأكيد الطلب",
                    onClick = onConfirm,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                )
                PharmaButton(
                    text = "رفض الطلب",
                    onClick = onReject,
                    enabled = enabled,
                    style = PharmaButtonStyle.Outlined,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OrderStatus.CONFIRMED,
            OrderStatus.IN_PROGRESS -> {
                if (order.fulfillmentType == FulfillmentType.PICKUP) {
                    PharmaButton("جاهز للاستلام", onClick = onReady, enabled = enabled, modifier = Modifier.fillMaxWidth())
                } else {
                    PharmaButton("خرج للتوصيل", onClick = onOutForDelivery, enabled = enabled, modifier = Modifier.fillMaxWidth())
                }
            }
            OrderStatus.READY_FOR_PICKUP,
            OrderStatus.OUT_FOR_DELIVERY -> {
                PharmaButton("تم التسليم", onClick = onDelivered, enabled = enabled, modifier = Modifier.fillMaxWidth())
            }
            OrderStatus.DELIVERED,
            OrderStatus.REJECTED,
            OrderStatus.CANCELLED -> Unit
        }
    }
}

@Composable
private fun PriceDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var priceText by remember { mutableStateOf("") }
    val priceCents = priceText.toPriceCentsOrNull()
    val isValid = priceCents != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تأكيد الطلب") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceS)) {
                Text("أدخل السعر الإجمالي قبل تأكيد الطلب.")
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("السعر") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = { priceCents?.let(onConfirm) },
            ) {
                Text("تأكيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
    )
}

private fun String.toPriceCentsOrNull(): Long? {
    val normalized = trim()
    if (!normalized.matches(Regex("""\d+(\.\d{1,2})?"""))) return null

    return runCatching {
        BigDecimal(normalized)
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }.getOrNull()
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MessageState(message: String, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize().padding(MaterialTheme.dimens.spaceL),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Text(message, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PrescriptionImage(url: String) {
    var showZoomDialog by remember { mutableStateOf(false) }
    val d = MaterialTheme.dimens

    Column(verticalArrangement = Arrangement.spacedBy(d.spaceS)) {
        Text(
            text = "صورة الوصفة الطبية / الدواء",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clickable { showZoomDialog = true },
            shape = RoundedCornerShape(d.radiusL),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            AsyncImage(
                model = url,
                contentDescription = "وصفة طبية",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        
        Text(
            text = "إضغط لتكبير الصورة",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }

    if (showZoomDialog) {
        ZoomableImageDialog(
            url = url,
            onDismiss = { showZoomDialog = false }
        )
    }
}

@Composable
private fun ZoomableImageDialog(
    url: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ClinicalCanvas.copy(alpha = 0.9f))
        ) {
            AsyncImage(
                model = url,
                contentDescription = "وصفة طبية مكبرة",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "إغلاق",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
