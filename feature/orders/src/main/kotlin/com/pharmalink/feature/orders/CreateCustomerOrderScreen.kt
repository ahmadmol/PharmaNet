package com.pharmalink.feature.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Storefront
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
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaButtonStyle
import com.pharmalink.designsystem.components.PharmaTextField
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.CustomerRequestUrgency
import com.pharmalink.domain.model.FulfillmentType

@Composable
fun CreateCustomerOrderScreen(
    medicine: MedicineSummaryUi,
    pharmacy: PharmacySummaryUi,
    onBackClick: () -> Unit,
    onOrderCreated: (String, FulfillmentType) -> Unit,
    viewModel: CreateCustomerOrderViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(medicine, pharmacy) {
        viewModel.initialize(
            medicine = medicine,
            pharmacy = pharmacy,
        )
    }

    LaunchedEffect(uiState.createdOrderId) {
        val orderId = uiState.createdOrderId ?: return@LaunchedEffect
        onOrderCreated(orderId, uiState.fulfillmentType)
        viewModel.consumeCreatedOrder()
    }

    CreateCustomerOrderContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onQuantityChange = viewModel::onQuantityChange,
        onIncrementQuantity = viewModel::onIncrementQuantity,
        onDecrementQuantity = viewModel::onDecrementQuantity,
        onUrgencyChange = viewModel::onUrgencyChange,
        onFulfillmentTypeChange = viewModel::onFulfillmentTypeChange,
        onDeliveryAddressChange = viewModel::onDeliveryAddressChange,
        onDeliveryPhoneChange = viewModel::onDeliveryPhoneChange,
        onNotesChange = viewModel::onNotesChange,
        onSubmitClick = viewModel::onSubmitClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCustomerOrderContent(
    uiState: CreateCustomerOrderUiState,
    onBackClick: () -> Unit,
    onQuantityChange: (String) -> Unit,
    onIncrementQuantity: () -> Unit,
    onDecrementQuantity: () -> Unit,
    onUrgencyChange: (CustomerRequestUrgency) -> Unit,
    onFulfillmentTypeChange: (FulfillmentType) -> Unit,
    onDeliveryAddressChange: (String) -> Unit,
    onDeliveryPhoneChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier,
            containerColor = ClinicalCanvas,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.customer_order_create_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.customer_order_back),
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(d.spaceL),
                verticalArrangement = Arrangement.spacedBy(d.spaceL),
            ) {
                item {
                    OrderSummaryCard(
                        medicine = uiState.medicine,
                        pharmacy = uiState.pharmacy,
                    )
                }
                item {
                    QuantityStepperCard(
                        quantity = uiState.quantityInput,
                        errorMessage = uiState.quantityErrorMessage,
                        onQuantityChange = onQuantityChange,
                        onIncrementQuantity = onIncrementQuantity,
                        onDecrementQuantity = onDecrementQuantity,
                    )
                }
                item {
                    RequestTypeSelectorCard(
                        selectedUrgency = uiState.urgency,
                        canSelectUrgent = uiState.pharmacy.id.isNotBlank(),
                        onUrgencyChange = onUrgencyChange,
                    )
                }
                item {
                    FulfillmentSelectorCard(
                        selectedType = uiState.fulfillmentType,
                        pickupEnabled = uiState.pharmacy.supportsPickup,
                        deliveryEnabled = uiState.pharmacy.supportsDelivery,
                        onFulfillmentTypeChange = onFulfillmentTypeChange,
                    )
                }
                item {
                    DeliverySection(
                        fulfillmentType = uiState.fulfillmentType,
                        deliveryAddress = uiState.deliveryAddress,
                        deliveryPhone = uiState.deliveryPhone,
                        deliveryAddressErrorMessage = uiState.deliveryAddressErrorMessage,
                        deliveryPhoneErrorMessage = uiState.deliveryPhoneErrorMessage,
                        onDeliveryAddressChange = onDeliveryAddressChange,
                        onDeliveryPhoneChange = onDeliveryPhoneChange,
                    )
                }
                item {
                    NotesSection(
                        notes = uiState.notes,
                        onNotesChange = onNotesChange,
                    )
                }
                item {
                    InfoBanner(
                        title = stringResource(R.string.customer_order_confirmation_notice),
                        body = stringResource(R.string.customer_order_price_hidden_notice),
                    )
                }
                if (uiState.submitErrorMessage != null) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Text(
                                text = uiState.submitErrorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(d.spaceL),
                            )
                        }
                    }
                }
                item {
                    PharmaButton(
                        text = stringResource(R.string.customer_order_submit),
                        onClick = onSubmitClick,
                        enabled = !uiState.isSubmitting,
                    )
                }
                if (uiState.isSubmitting) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderSummaryCard(
    medicine: MedicineSummaryUi,
    pharmacy: PharmacySummaryUi,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Text(
                text = medicine.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (medicine.brand.isNotBlank() || medicine.strength.isNotBlank()) {
                Text(
                    text = listOf(medicine.brand, medicine.strength).filter { it.isNotBlank() }.joinToString(" - "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(d.spaceXS),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = pharmacy.name.ifBlank { stringResource(R.string.customer_order_scope_all) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RequestTypeSelectorCard(
    selectedUrgency: CustomerRequestUrgency,
    canSelectUrgent: Boolean,
    onUrgencyChange: (CustomerRequestUrgency) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Text(
                text = stringResource(R.string.customer_order_request_type_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(d.spaceM)) {
                FulfillmentOptionCard(
                    title = stringResource(R.string.customer_order_urgency_urgent),
                    selected = selectedUrgency == CustomerRequestUrgency.URGENT,
                    enabled = canSelectUrgent,
                    onAction = { onUrgencyChange(CustomerRequestUrgency.URGENT) },
                    modifier = Modifier.weight(1f),
                )
                FulfillmentOptionCard(
                    title = stringResource(R.string.customer_order_urgency_normal),
                    selected = selectedUrgency == CustomerRequestUrgency.NORMAL,
                    enabled = true,
                    onAction = { onUrgencyChange(CustomerRequestUrgency.NORMAL) },
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = if (selectedUrgency == CustomerRequestUrgency.URGENT) {
                    stringResource(R.string.customer_order_scope_specific)
                } else {
                    stringResource(R.string.customer_order_scope_all)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuantityStepperCard(
    quantity: String,
    errorMessage: String?,
    onQuantityChange: (String) -> Unit,
    onIncrementQuantity: () -> Unit,
    onDecrementQuantity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Text(
                text = stringResource(R.string.customer_order_quantity_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepperActionButton(
                    icon = Icons.Outlined.Remove,
                    onAction = onDecrementQuantity,
                )
                PharmaTextField(
                    value = quantity,
                    onValueChange = onQuantityChange,
                    label = stringResource(R.string.customer_order_quantity_label),
                    modifier = Modifier.weight(1f),
                    errorMessage = errorMessage,
                )
                StepperActionButton(
                    icon = Icons.Outlined.Add,
                    onAction = onIncrementQuantity,
                )
            }
        }
    }
}

@Composable
private fun StepperActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = PharmaBlue50,
        onClick = onAction,
    ) {
        Box(
            modifier = Modifier.padding(d.spaceM),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun FulfillmentSelectorCard(
    selectedType: FulfillmentType,
    pickupEnabled: Boolean,
    deliveryEnabled: Boolean,
    onFulfillmentTypeChange: (FulfillmentType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Text(
                text = stringResource(R.string.customer_order_fulfillment_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(d.spaceM)) {
                if (pickupEnabled) {
                    FulfillmentOptionCard(
                        title = stringResource(R.string.customer_order_pickup_option),
                        selected = selectedType == FulfillmentType.PICKUP,
                        enabled = true,
                        onAction = { onFulfillmentTypeChange(FulfillmentType.PICKUP) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (deliveryEnabled) {
                    FulfillmentOptionCard(
                        title = stringResource(R.string.customer_order_delivery_option),
                        selected = selectedType == FulfillmentType.DELIVERY,
                        enabled = true,
                        onAction = { onFulfillmentTypeChange(FulfillmentType.DELIVERY) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FulfillmentOptionCard(
    title: String,
    selected: Boolean,
    enabled: Boolean,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = backgroundColor,
        enabled = enabled,
        onClick = onAction,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) contentColor else contentColor.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d.spaceM, vertical = d.spaceL),
        )
    }
}

@Composable
private fun DeliverySection(
    fulfillmentType: FulfillmentType,
    deliveryAddress: String,
    deliveryPhone: String,
    deliveryAddressErrorMessage: String?,
    deliveryPhoneErrorMessage: String?,
    onDeliveryAddressChange: (String) -> Unit,
    onDeliveryPhoneChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val isDelivery = fulfillmentType == FulfillmentType.DELIVERY

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Text(
                text = stringResource(R.string.customer_order_delivery_section_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (isDelivery) {
                PharmaTextField(
                    value = deliveryAddress,
                    onValueChange = onDeliveryAddressChange,
                    label = stringResource(R.string.customer_order_address_label),
                    singleLine = false,
                    errorMessage = deliveryAddressErrorMessage,
                )
                PharmaTextField(
                    value = deliveryPhone,
                    onValueChange = onDeliveryPhoneChange,
                    label = stringResource(R.string.customer_order_phone_label),
                    errorMessage = deliveryPhoneErrorMessage,
                )
            } else {
                Text(
                    text = stringResource(R.string.customer_order_pickup_no_delivery_fields),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NotesSection(
    notes: String,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Text(
                text = stringResource(R.string.customer_order_optional_notes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            PharmaTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = stringResource(R.string.customer_order_optional_notes),
                singleLine = false,
            )
        }
    }
}

@Composable
internal fun InfoBanner(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceXS),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
