package com.pharmalink.feature.request

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaButtonStyle
import com.pharmalink.designsystem.components.PharmaScreenScaffold
import com.pharmalink.designsystem.components.PharmaScreenState
import com.pharmalink.designsystem.components.PharmaStateSpec
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.theme.PharmaSuccess
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.RequestItem
import com.pharmalink.domain.model.RequestStatus
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun RequestDetailsScreen(
    onBack: () -> Unit,
    onOpenOrder: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RequestDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val d = MaterialTheme.dimens
    val deletedMessage = stringResource(R.string.request_error_deleted)

    LaunchedEffect(state.screenState) {
        val errorState = state.screenState as? com.pharmalink.core.common.ui.ScreenState.Error
        if (errorState?.message == deletedMessage) {
            onBack()
        }
    }

    PharmaScreenScaffold(
        title = stringResource(R.string.request_details_title),
        onBack = onBack,
        navigationContentDescription = stringResource(R.string.request_details_back),
        modifier = modifier,
    ) {
        PharmaScreenState(
            screenState = state.screenState,
            loading = PharmaStateSpec(
                title = stringResource(R.string.request_details_title),
                subtitle = stringResource(R.string.request_details_loading_subtitle),
                tone = PharmaStateTone.Loading,
            ),
            empty = PharmaStateSpec(
                title = stringResource(R.string.request_details_title),
                subtitle = stringResource(R.string.request_details_empty_subtitle),
            ),
            error = PharmaStateSpec(
                title = stringResource(R.string.request_details_title),
                subtitle = stringResource(R.string.request_details_error_subtitle),
                tone = PharmaStateTone.Error,
            ),
            offline = PharmaStateSpec(
                title = stringResource(R.string.request_details_title),
                subtitle = stringResource(R.string.request_details_offline_subtitle),
                tone = PharmaStateTone.Offline,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d.spaceL, vertical = d.spaceXL),
        ) { request ->
            RequestDetailsContent(
                request = request,
                onOpenOrder = onOpenOrder,
                accountType = state.accountType,
                isActionInProgress = state.isActionInProgress,
                actionErrorMessage = state.actionErrorMessage,
                relatedOrder = state.relatedOrder,
                onPharmacySubmit = viewModel::submitRequest,
                onPharmacyDelete = viewModel::deleteRequest,
                onPharmacyAcceptQuote = viewModel::acceptQuote,
                onPharmacyRejectQuote = viewModel::rejectQuote,
                onWarehouseAcceptPrice = viewModel::acceptRequest,
                onWarehouseAction = viewModel::updateRequestStatus,
                onDismissActionError = viewModel::clearActionError,
            )
        }
    }
}

@Composable
private fun RequestDetailsContent(
    request: Request,
    onOpenOrder: (String) -> Unit,
    accountType: AccountType?,
    isActionInProgress: Boolean,
    actionErrorMessage: String?,
    relatedOrder: Order?,
    onPharmacySubmit: () -> Unit,
    onPharmacyDelete: () -> Unit,
    onPharmacyAcceptQuote: () -> Unit,
    onPharmacyRejectQuote: () -> Unit,
    onWarehouseAcceptPrice: (Long) -> Unit,
    onWarehouseAction: (RequestStatus) -> Unit,
    onDismissActionError: () -> Unit,
) {
    val d = MaterialTheme.dimens

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            item {
                RequestSummaryCard(request = request)
            }

            if (accountType == AccountType.WAREHOUSE) {
                item {
                    RequesterPharmacyCard(request = request)
                }
            }

            item {
                RequestItemsCard(request = request)
            }

            item {
                InvoiceSummaryCard(request = request)
            }

            item {
                PharmacyQuoteActionsCard(
                    accountType = accountType,
                    request = request,
                    relatedOrder = relatedOrder,
                    isActionInProgress = isActionInProgress,
                    actionErrorMessage = actionErrorMessage,
                    onAcceptQuote = onPharmacyAcceptQuote,
                    onRejectQuote = onPharmacyRejectQuote,
                    onDismissActionError = onDismissActionError,
                )
            }

            item {
                RequestStatusTimeline(
                    currentStatus = request.status,
                    modifier = Modifier.padding(vertical = d.spaceM),
                )
            }

            item {
                PharmacyDraftActionsCard(
                    accountType = accountType,
                    requestStatus = request.status,
                    isActionInProgress = isActionInProgress,
                    actionErrorMessage = actionErrorMessage,
                    onSubmit = onPharmacySubmit,
                    onDelete = onPharmacyDelete,
                    onDismissActionError = onDismissActionError,
                )
            }

            item {
                WarehouseLifecycleActionsCard(
                    accountType = accountType,
                    requestStatus = request.status,
                    isActionInProgress = isActionInProgress,
                    actionErrorMessage = actionErrorMessage,
                    onWarehouseAcceptPrice = onWarehouseAcceptPrice,
                    onWarehouseAction = onWarehouseAction,
                    onDismissActionError = onDismissActionError,
                )
            }

            item {
                WarehouseInfoCard(request = request)
            }

            item {
                StorageNotesCard(request = request)
            }

            item {
                EtaCard(request = request)
            }

            item {
                request.relatedOrderId?.let { orderId ->
                    PharmaButton(
                        text = stringResource(R.string.request_details_open_related_order),
                        onClick = { onOpenOrder(orderId) },
                        style = PharmaButtonStyle.GradientAccent,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PharmacyDraftActionsCard(
    accountType: AccountType?,
    requestStatus: RequestStatus,
    isActionInProgress: Boolean,
    actionErrorMessage: String?,
    onSubmit: () -> Unit,
    onDelete: () -> Unit,
    onDismissActionError: () -> Unit,
) {
    if (accountType != AccountType.PHARMACY || requestStatus != RequestStatus.DRAFT) return
    val d = MaterialTheme.dimens

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Text(
                text = stringResource(R.string.request_details_pharmacy_actions_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (isActionInProgress) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.request_action_updating),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            PharmaButton(
                text = stringResource(R.string.request_action_submit),
                onClick = onSubmit,
                enabled = !isActionInProgress,
                style = PharmaButtonStyle.GradientAccent,
                modifier = Modifier.fillMaxWidth(),
            )

            PharmaButton(
                text = stringResource(R.string.request_action_delete_draft),
                onClick = onDelete,
                enabled = !isActionInProgress,
                style = PharmaButtonStyle.Outlined,
                modifier = Modifier.fillMaxWidth(),
            )

            if (!actionErrorMessage.isNullOrBlank()) {
                Text(
                    text = actionErrorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                PharmaButton(
                    text = stringResource(R.string.request_dismiss_error),
                    onClick = onDismissActionError,
                    enabled = !isActionInProgress,
                    style = PharmaButtonStyle.Outlined,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun WarehouseLifecycleActionsCard(
    accountType: AccountType?,
    requestStatus: RequestStatus,
    isActionInProgress: Boolean,
    actionErrorMessage: String?,
    onWarehouseAcceptPrice: (Long) -> Unit,
    onWarehouseAction: (RequestStatus) -> Unit,
    onDismissActionError: () -> Unit,
) {
    if (accountType != AccountType.WAREHOUSE) return
    val actions = warehouseActionsForStatus(requestStatus)
    val isWaitingForPharmacy = requestStatus == RequestStatus.QUOTE_PENDING
    if (actions.isEmpty() && !isWaitingForPharmacy) return
    val d = MaterialTheme.dimens
    var showAcceptPriceDialog by remember { mutableStateOf(false) }
    var acceptPriceText by remember { mutableStateOf("") }
    var acceptPriceError by remember { mutableStateOf<String?>(null) }

    if (showAcceptPriceDialog) {
        val invalidPriceMessage = stringResource(R.string.request_accept_price_error)
        WarehouseAcceptPriceDialog(
            priceText = acceptPriceText,
            errorMessage = acceptPriceError,
            isActionInProgress = isActionInProgress,
            onPriceChange = {
                acceptPriceText = it
                acceptPriceError = null
            },
            onDismiss = {
                if (!isActionInProgress) {
                    showAcceptPriceDialog = false
                    acceptPriceError = null
                }
            },
            onConfirm = {
                val cents = acceptPriceText.toPriceCentsOrNull()
                if (cents == null) {
                    acceptPriceError = invalidPriceMessage
                } else {
                    showAcceptPriceDialog = false
                    acceptPriceError = null
                    onWarehouseAcceptPrice(cents)
                }
            },
        )
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Text(
                text = if (isWaitingForPharmacy) { "\u0628\u0627\u0646\u062A\u0638\u0627\u0631 \u0645\u0648\u0627\u0641\u0642\u0629 \u0627\u0644\u0635\u064A\u062F\u0644\u064A\u0629" } else { stringResource(R.string.request_details_warehouse_actions_title) },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (isWaitingForPharmacy) {
                Text(
                    text = "\u062A\u0645 \u0625\u0631\u0633\u0627\u0644 \u0639\u0631\u0636 \u0627\u0644\u0633\u0639\u0631. \u0627\u0644\u0645\u0633\u062A\u0648\u062F\u0639 \u0628\u0627\u0646\u062A\u0638\u0627\u0631 \u0645\u0648\u0627\u0641\u0642\u0629 \u0627\u0644\u0635\u064A\u062F\u0644\u064A\u0629.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (isActionInProgress) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.request_action_updating),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            actions.forEach { action ->
                PharmaButton(
                    text = stringResource(action.labelRes),
                    onClick = {
                        if (action.targetStatus == RequestStatus.QUOTE_PENDING) {
                            showAcceptPriceDialog = true
                        } else {
                            onWarehouseAction(action.targetStatus)
                        }
                    },
                    enabled = !isActionInProgress,
                    style = action.style,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (!actionErrorMessage.isNullOrBlank()) {
                Text(
                    text = actionErrorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                PharmaButton(
                    text = stringResource(R.string.request_dismiss_error),
                    onClick = onDismissActionError,
                    enabled = !isActionInProgress,
                    style = PharmaButtonStyle.Outlined,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PharmacyQuoteActionsCard(
    accountType: AccountType?,
    request: Request,
    relatedOrder: Order?,
    isActionInProgress: Boolean,
    actionErrorMessage: String?,
    onAcceptQuote: () -> Unit,
    onRejectQuote: () -> Unit,
    onDismissActionError: () -> Unit,
) {
    if (accountType != AccountType.PHARMACY || request.status != RequestStatus.QUOTE_PENDING) return
    val d = MaterialTheme.dimens
    val quotePriceCents = relatedOrder?.totalPriceCents ?: request.legacyTotalPriceCents()
    val quotePriceLabel = quotePriceCents?.toQuotePriceLabel(relatedOrder?.currency ?: "SAR")
        ?: "\u0628\u0627\u0646\u062A\u0638\u0627\u0631 \u0627\u0644\u0633\u0639\u0631"

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = "\u0639\u0631\u0636 \u0627\u0644\u0633\u0639\u0631",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )

            InvoiceRow(
                label = "\u0627\u0644\u0633\u0639\u0631 \u0627\u0644\u0645\u0639\u0631\u0648\u0636",
                value = quotePriceLabel,
                isGrandTotal = true,
            )

            Text(
                text = "\u0631\u0627\u062C\u0639 \u0627\u0644\u0633\u0639\u0631 \u0642\u0628\u0644 \u0627\u0644\u0645\u0648\u0627\u0641\u0642\u0629. \u0639\u0646\u062F \u0627\u0644\u0645\u0648\u0627\u0641\u0642\u0629 \u064A\u0645\u0643\u0646 \u0644\u0644\u0645\u0633\u062A\u0648\u062F\u0639 \u0628\u062F\u0621 \u062A\u062C\u0647\u064A\u0632 \u0627\u0644\u0637\u0644\u0628.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )

            if (isActionInProgress) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.request_action_updating),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            PharmaButton(
                text = "\u0627\u0644\u0645\u0648\u0627\u0641\u0642\u0629 \u0639\u0644\u0649 \u0627\u0644\u0639\u0631\u0636",
                onClick = onAcceptQuote,
                enabled = !isActionInProgress && quotePriceCents != null,
                style = PharmaButtonStyle.GradientAccent,
                modifier = Modifier.fillMaxWidth(),
            )

            PharmaButton(
                text = "\u0631\u0641\u0636 \u0627\u0644\u0639\u0631\u0636",
                onClick = onRejectQuote,
                enabled = !isActionInProgress,
                style = PharmaButtonStyle.Outlined,
                modifier = Modifier.fillMaxWidth(),
            )

            if (!actionErrorMessage.isNullOrBlank()) {
                Text(
                    text = actionErrorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
                PharmaButton(
                    text = stringResource(R.string.request_dismiss_error),
                    onClick = onDismissActionError,
                    enabled = !isActionInProgress,
                    style = PharmaButtonStyle.Outlined,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun WarehouseAcceptPriceDialog(
    priceText: String,
    errorMessage: String?,
    isActionInProgress: Boolean,
    onPriceChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.request_accept_price_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceS)) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = onPriceChange,
                    enabled = !isActionInProgress,
                    singleLine = true,
                    label = { Text(text = stringResource(R.string.request_accept_price_label)) },
                    supportingText = {
                        Text(text = stringResource(R.string.request_accept_price_supporting))
                    },
                    isError = !errorMessage.isNullOrBlank(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isActionInProgress,
            ) {
                Text(text = stringResource(R.string.request_accept_price_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isActionInProgress,
            ) {
                Text(text = stringResource(R.string.request_accept_price_cancel))
            }
        },
    )
}

private fun String.toPriceCentsOrNull(): Long? {
    val normalized = trim()
    if (!Regex("""\d+(\.\d{1,2})?""").matches(normalized)) return null
    return runCatching {
        BigDecimal(normalized)
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
            .takeIf { it >= 0L }
    }.getOrNull()
}

private fun Request.legacyTotalPriceCents(): Long? =
    totalPrice.takeIf { it > 0.0 }?.let { amount ->
        runCatching {
            BigDecimal.valueOf(amount)
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        }.getOrNull()
    }

private fun Long.toQuotePriceLabel(currency: String): String {
    val amount = BigDecimal.valueOf(this)
        .movePointLeft(2)
        .stripTrailingZeros()
        .toPlainString()
    val currencyLabel = when (currency.uppercase()) {
        "SAR" -> "ر.س"
        else -> currency
    }
    return "$amount $currencyLabel"
}

private data class WarehouseActionUi(
    val targetStatus: RequestStatus,
    val labelRes: Int,
    val style: PharmaButtonStyle,
)

@Composable
private fun warehouseActionsForStatus(status: RequestStatus): List<WarehouseActionUi> {
    return when (status) {
        RequestStatus.PENDING -> listOf(
            WarehouseActionUi(
                targetStatus = RequestStatus.QUOTE_PENDING,
                labelRes = R.string.request_action_accept,
                style = PharmaButtonStyle.GradientAccent,
            ),
            WarehouseActionUi(
                targetStatus = RequestStatus.REJECTED,
                labelRes = R.string.request_action_reject,
                style = PharmaButtonStyle.Outlined,
            ),
        )
        RequestStatus.ACCEPTED -> listOf(
            WarehouseActionUi(
                targetStatus = RequestStatus.IN_PROGRESS,
                labelRes = R.string.request_action_start_processing,
                style = PharmaButtonStyle.GradientAccent,
            ),
        )
        RequestStatus.IN_PROGRESS -> listOf(
            WarehouseActionUi(
                targetStatus = RequestStatus.FULFILLED,
                labelRes = R.string.request_action_mark_fulfilled,
                style = PharmaButtonStyle.GradientAccent,
            ),
        )
        RequestStatus.QUOTE_PENDING,
        RequestStatus.REJECTED,
        RequestStatus.FULFILLED,
        RequestStatus.CANCELLED,
        RequestStatus.DRAFT -> emptyList()
    }
}

@Composable
private fun RequesterPharmacyCard(request: Request) {
    val d = MaterialTheme.dimens
    val pharmacyName = request.pharmacyName.trim()
    val requesterTitle = if (pharmacyName.isBlank()) {
        "\u0627\u0644\u0637\u0644\u0628 \u0645\u0646 \u0635\u064A\u062F\u0644\u064A\u0629"
    } else {
        "\u0627\u0644\u0637\u0644\u0628 \u0645\u0646 \u0635\u064A\u062F\u0644\u064A\u0629 $pharmacyName"
    }
    val contactItems = listOfNotNull(
        request.pharmacyPhone.takeIf { it.isNotBlank() }?.let {
            Icons.Outlined.Call to it
        },
        request.pharmacyLocation.takeIf { it.isNotBlank() }?.let {
            Icons.Outlined.LocationOn to it
        },
    )

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = requesterTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )

            contactItems.forEach { (icon, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.End,
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestSummaryCard(request: Request) {
    val d = MaterialTheme.dimens
    val displayItems = request.displayItems()
    
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DetailsStatusPill(status = request.status)
                
                if (request.priority == com.pharmalink.domain.model.RequestPriority.URGENT) {
                    UrgencyBadge()
                }
            }
            
            Spacer(Modifier.height(d.spaceM))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                MedicineImagePlaceholder(
                    imageUrl = request.medicineImageUrl,
                    modifier = Modifier.padding(end = d.spaceM),
                )
                
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = displayItems.firstOrNull()?.medicineName ?: request.medicineName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    
                    if (request.medicineSubtitle.isNotEmpty()) {
                        Text(
                            text = request.medicineSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                            textAlign = TextAlign.End,
                        )
                    }
                    
                    Spacer(Modifier.height(d.spaceS))
                    
                    Text(
                        text = "\u0639\u062F\u062F \u0627\u0644\u0645\u0646\u062A\u062C\u0627\u062A: ${displayItems.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestItemsCard(request: Request) {
    val d = MaterialTheme.dimens
    val displayItems = request.displayItems()

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(d.spaceS, Alignment.End),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "\u0627\u0644\u0645\u0646\u062A\u062C\u0627\u062A \u0627\u0644\u0645\u0637\u0644\u0648\u0628\u0629 (${displayItems.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                )
            }

            displayItems.forEachIndexed { index, item ->
                RequestItemRow(
                    item = item,
                    displayLineNo = item.lineNo.takeIf { it > 0 } ?: (index + 1),
                )
                if (index != displayItems.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestItemRow(
    item: RequestItem,
    displayLineNo: Int,
) {
    val d = MaterialTheme.dimens

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = item.medicineName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
            if (item.medicineSubtitle.isNotBlank()) {
                Text(
                    text = item.medicineSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(d.radiusM),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Text(
                text = "${item.quantity} ${item.unit}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = d.spaceS, vertical = d.spaceXS),
            )
        }
    }
}

@Composable
private fun DetailsStatusPill(status: RequestStatus) {
    Surface(
        shape = CircleShape,
        color = when (status) {
            RequestStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
            RequestStatus.QUOTE_PENDING -> MaterialTheme.colorScheme.secondaryContainer
            RequestStatus.ACCEPTED -> MaterialTheme.colorScheme.primaryContainer
            RequestStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiaryContainer
            RequestStatus.FULFILLED -> MaterialTheme.colorScheme.primaryContainer
            RequestStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
            RequestStatus.CANCELLED, RequestStatus.DRAFT -> MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = when (status) {
            RequestStatus.PENDING -> MaterialTheme.colorScheme.onSecondaryContainer
            RequestStatus.QUOTE_PENDING -> MaterialTheme.colorScheme.onSecondaryContainer
            RequestStatus.ACCEPTED -> MaterialTheme.colorScheme.onPrimaryContainer
            RequestStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onTertiaryContainer
            RequestStatus.FULFILLED -> MaterialTheme.colorScheme.onPrimaryContainer
            RequestStatus.REJECTED -> MaterialTheme.colorScheme.onErrorContainer
            RequestStatus.CANCELLED, RequestStatus.DRAFT -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Text(
            text = requestStatusLabel(status),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun UrgencyBadge() {
    Surface(
        shape = RoundedCornerShape(50),
        color = PremiumUrgent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = stringResource(R.string.request_details_urgent_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun WarehouseInfoCard(request: Request) {
    val d = MaterialTheme.dimens
    
    InfoCard(
        title = stringResource(R.string.request_details_section_warehouse),
        icon = Icons.Outlined.LocalShipping,
        items = listOf(
            InfoItem(stringResource(R.string.request_details_label_warehouse), request.warehouseName),
            InfoItem(stringResource(R.string.request_details_label_supplier), request.supplierName),
            InfoItem(stringResource(R.string.request_details_label_last_updated), request.updatedAtLabel),
        ),
    )
}

@Composable
private fun StorageNotesCard(request: Request) {
    val d = MaterialTheme.dimens
    
    InfoCard(
        title = stringResource(R.string.request_details_section_storage),
        icon = Icons.Outlined.Notes,
        items = listOfNotNull(
            if (request.notes.isNotBlank()) {
                InfoItem(stringResource(R.string.request_details_label_order_notes), request.notes)
            } else {
                null
            },
            if (request.storageNotes.isNotBlank()) {
                InfoItem(stringResource(R.string.request_details_label_storage_notes), request.storageNotes)
            } else {
                null
            },
            InfoItem(
                stringResource(R.string.request_details_label_priority),
                if (request.priority == com.pharmalink.domain.model.RequestPriority.URGENT) {
                    stringResource(R.string.request_details_priority_urgent)
                } else {
                    stringResource(R.string.request_details_priority_normal)
                },
            ),
        ),
    )
}

@Composable
private fun EtaCard(request: Request) {
    val d = MaterialTheme.dimens
    
    if (request.etaLabel.isNotEmpty()) {
        InfoCard(
            title = stringResource(R.string.request_details_section_eta),
            icon = Icons.Outlined.Schedule,
            items = listOf(
                InfoItem(stringResource(R.string.request_details_label_expected_delivery), request.etaLabel),
            ),
        )
    }
}

@Composable
private fun ActionButtons(
    onContactWarehouse: () -> Unit,
    onReorder: () -> Unit,
    request: Request,
) {
    val d = MaterialTheme.dimens
    
    Column(
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        // Primary CTA - Contact Warehouse
        PharmaButton(
            text = stringResource(R.string.request_details_contact_warehouse),
            onClick = onContactWarehouse,
            style = PharmaButtonStyle.GradientAccent,
            modifier = Modifier.fillMaxWidth(),
        )
        
        // Secondary CTA - Reorder
        PharmaButton(
            text = stringResource(R.string.request_details_reorder_same),
            onClick = onReorder,
            style = PharmaButtonStyle.Outlined,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<InfoItem>,
) {
    val d = MaterialTheme.dimens
    
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(d.spaceS, Alignment.End),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = d.spaceM),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                )
            }
            
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = d.spaceXS),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun InvoiceSummaryCard(request: Request) {
    val d = MaterialTheme.dimens
    val urgentServiceFee = if (request.priority == com.pharmalink.domain.model.RequestPriority.URGENT) 10000.0 else 0.0
    val storedTotal = request.totalPrice.takeIf { it > 0.0 }
    val itemsTotal = storedTotal
        ?.minus(urgentServiceFee)
        ?.coerceAtLeast(0.0)
        ?: (request.quantity * 1000.0)
    val grandTotal = storedTotal ?: (itemsTotal + urgentServiceFee)

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL)
        ) {
            Text(
                text = stringResource(R.string.request_details_invoice_summary),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = d.spaceM)
            )

            InvoiceRow(
                label = stringResource(R.string.request_details_items_total),
                value = "${itemsTotal.toLong()} ل.س"
            )

            if (urgentServiceFee > 0) {
                InvoiceRow(
                    label = stringResource(R.string.request_details_urgent_service_fee),
                    value = "${urgentServiceFee.toLong()} ل.س",
                    isUrgent = true
                )
            }

            Spacer(modifier = Modifier.height(d.spaceS))

            // Separator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            Spacer(modifier = Modifier.height(d.spaceS))

            InvoiceRow(
                label = stringResource(R.string.request_details_grand_total),
                value = "${grandTotal.toLong()} ل.س",
                isGrandTotal = true
            )
        }
    }
}

@Composable
private fun InvoiceRow(label: String, value: String, isUrgent: Boolean = false, isGrandTotal: Boolean = false) {
    val d = MaterialTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = d.spaceXS),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isGrandTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isGrandTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isUrgent) PremiumUrgent else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
        Text(
            text = value,
            style = if (isGrandTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isGrandTotal) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isUrgent) PremiumUrgent else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun requestStatusLabel(status: RequestStatus): String = when (status) {
    RequestStatus.PENDING -> stringResource(R.string.request_status_submitted)
    RequestStatus.QUOTE_PENDING -> stringResource(R.string.request_status_quote_pending)
    RequestStatus.ACCEPTED -> stringResource(R.string.request_status_approved)
    RequestStatus.IN_PROGRESS -> stringResource(R.string.request_status_in_progress)
    RequestStatus.FULFILLED -> stringResource(R.string.request_status_completed)
    RequestStatus.REJECTED -> stringResource(R.string.request_status_rejected)
    RequestStatus.CANCELLED -> stringResource(R.string.request_status_cancelled)
    RequestStatus.DRAFT -> stringResource(R.string.request_status_draft)
}

private fun Request.displayItems(): List<RequestItem> =
    items.ifEmpty {
        listOf(
            RequestItem(
                lineNo = 1,
                medicineId = medicineId.orEmpty(),
                medicineName = medicineName,
                medicineSubtitle = medicineSubtitle,
                quantity = quantity,
                unit = unit,
            ),
        )
    }

private data class InfoItem(
    val label: String,
    val value: String,
)
