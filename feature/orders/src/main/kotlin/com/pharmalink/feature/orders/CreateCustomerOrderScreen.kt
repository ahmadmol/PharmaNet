package com.pharmalink.feature.orders

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.outlined.LocationOn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaButtonStyle
import com.pharmalink.designsystem.components.PharmaTextField
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.CustomerRequestUrgency
import com.pharmalink.domain.model.FulfillmentType

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun CreateCustomerOrderScreen(
    medicine: MedicineSummaryUi,
    pharmacy: PharmacySummaryUi,
    onBackClick: () -> Unit,
    onOrderCreated: (String, FulfillmentType) -> Unit,
    viewModel: CreateCustomerOrderViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val activity = context.findActivity()
    val permissionDeniedPermanently = remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            viewModel.onImageSelected(uri)
        }
    )
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                permissionDeniedPermanently.value = false
                viewModel.detectDeliveryLocation()
            } else {
                val permanentlyDenied = activity?.let {
                    !ActivityCompat.shouldShowRequestPermissionRationale(
                        it,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    ) && !ActivityCompat.shouldShowRequestPermissionRationale(
                        it,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                } ?: true
                permissionDeniedPermanently.value = permanentlyDenied
                viewModel.onDeliveryLocationPermissionDenied(permanentlyDenied)
            }
        },
    )

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
        permissionDeniedPermanently = permissionDeniedPermanently.value,
        onDetectDeliveryLocationClick = {
            val hasPermission = context.hasLocationPermission()
            if (hasPermission) {
                viewModel.detectDeliveryLocation()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
        },
        onOpenAppSettingsClick = {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ),
            )
        },
        onOpenLocationSettingsClick = {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        },
        onDeliveryPhoneChange = viewModel::onDeliveryPhoneChange,
        onNotesChange = viewModel::onNotesChange,
        onAttachPrescriptionClick = { imagePickerLauncher.launch("image/*") },
        onRemovePrescriptionClick = { viewModel.onImageSelected(null) },
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
    permissionDeniedPermanently: Boolean,
    onDetectDeliveryLocationClick: () -> Unit,
    onOpenAppSettingsClick: () -> Unit,
    onOpenLocationSettingsClick: () -> Unit,
    onDeliveryPhoneChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAttachPrescriptionClick: () -> Unit,
    onRemovePrescriptionClick: () -> Unit,
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
                    PrescriptionAttachmentCard(
                        prescriptionUri = uiState.prescriptionUri,
                        onAttachClick = onAttachPrescriptionClick,
                        onRemoveClick = onRemovePrescriptionClick,
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
                        requestScope = uiState.requestScope,
                        deliveryAddress = uiState.deliveryAddress,
                        deliveryLatitude = uiState.deliveryLatitude,
                        deliveryLongitude = uiState.deliveryLongitude,
                        deliveryPhone = uiState.deliveryPhone,
                        deliveryAddressErrorMessage = uiState.deliveryAddressErrorMessage,
                        deliveryLocationErrorMessage = uiState.deliveryLocationErrorMessage,
                        deliveryPhoneErrorMessage = uiState.deliveryPhoneErrorMessage,
                        isDetectingLocation = uiState.isDetectingLocation,
                        permissionDeniedPermanently = permissionDeniedPermanently,
                        onDetectLocationClick = onDetectDeliveryLocationClick,
                        onOpenAppSettingsClick = onOpenAppSettingsClick,
                        onOpenLocationSettingsClick = onOpenLocationSettingsClick,
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
                        enabled = !uiState.isSubmitting && !uiState.isUploadingImage && !uiState.isDetectingLocation,
                    )
                }
                if (uiState.isSubmitting || uiState.isUploadingImage || uiState.isDetectingLocation) {
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
private fun PrescriptionAttachmentCard(
    prescriptionUri: android.net.Uri?,
    onAttachClick: () -> Unit,
    onRemoveClick: () -> Unit,
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
                text = stringResource(R.string.customer_order_attach_prescription),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (prescriptionUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sizeIn(maxHeight = 200.dp)
                ) {
                    AsyncImage(
                        model = prescriptionUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(d.spaceS),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        onClick = onRemoveClick
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.customer_order_remove_prescription),
                            modifier = Modifier.padding(d.spaceXS),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                PharmaButton(
                    text = stringResource(R.string.customer_order_attach_prescription),
                    onClick = onAttachClick,
                    style = PharmaButtonStyle.Outlined,
                    modifier = Modifier.fillMaxWidth(),
                )
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
                    text = if (pharmacy.name.isBlank()) {
                        stringResource(R.string.customer_order_scope_all)
                    } else {
                        pharmacy.name
                    },
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
            if (selectedUrgency == CustomerRequestUrgency.NORMAL) {
                Text(
                    text = stringResource(R.string.customer_order_scope_all_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
    requestScope: com.pharmalink.domain.model.CustomerRequestScope,
    deliveryAddress: String,
    deliveryLatitude: Double?,
    deliveryLongitude: Double?,
    deliveryPhone: String,
    deliveryAddressErrorMessage: String?,
    deliveryLocationErrorMessage: String?,
    deliveryPhoneErrorMessage: String?,
    isDetectingLocation: Boolean,
    permissionDeniedPermanently: Boolean,
    onDetectLocationClick: () -> Unit,
    onOpenAppSettingsClick: () -> Unit,
    onOpenLocationSettingsClick: () -> Unit,
    onDeliveryPhoneChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val isDelivery = fulfillmentType == FulfillmentType.DELIVERY
    val requiresLocation = isDelivery || requestScope == com.pharmalink.domain.model.CustomerRequestScope.ALL_PHARMACIES

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
            if (requiresLocation) {
                DeliveryLocationPicker(
                    deliveryAddress = deliveryAddress,
                    deliveryLatitude = deliveryLatitude,
                    deliveryLongitude = deliveryLongitude,
                    errorMessage = deliveryAddressErrorMessage ?: deliveryLocationErrorMessage,
                    isDetectingLocation = isDetectingLocation,
                    permissionDeniedPermanently = permissionDeniedPermanently,
                    onDetectLocationClick = onDetectLocationClick,
                    onOpenAppSettingsClick = onOpenAppSettingsClick,
                    onOpenLocationSettingsClick = onOpenLocationSettingsClick,
                )
                if (isDelivery) {
                    PharmaTextField(
                        value = deliveryPhone,
                        onValueChange = onDeliveryPhoneChange,
                        label = stringResource(R.string.customer_order_phone_label),
                        errorMessage = deliveryPhoneErrorMessage,
                    )
                }
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
private fun DeliveryLocationPicker(
    deliveryAddress: String,
    deliveryLatitude: Double?,
    deliveryLongitude: Double?,
    errorMessage: String?,
    isDetectingLocation: Boolean,
    permissionDeniedPermanently: Boolean,
    onDetectLocationClick: () -> Unit,
    onOpenAppSettingsClick: () -> Unit,
    onOpenLocationSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val hasCoordinates = deliveryLatitude != null && deliveryLongitude != null

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceM),
            verticalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(d.spaceS),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.customer_order_location_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (hasCoordinates) {
                    deliveryAddress
                } else {
                    stringResource(R.string.customer_order_location_not_selected)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (hasCoordinates) {
                Text(
                    text = stringResource(
                        R.string.customer_order_location_coordinates,
                        deliveryLatitude,
                        deliveryLongitude,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            PharmaButton(
                text = if (hasCoordinates) {
                    stringResource(R.string.customer_order_location_retry)
                } else {
                    stringResource(R.string.customer_order_location_use_current)
                },
                onClick = onDetectLocationClick,
                enabled = !isDetectingLocation,
                style = PharmaButtonStyle.Outlined,
                modifier = Modifier.fillMaxWidth(),
            )
            if (permissionDeniedPermanently) {
                PharmaButton(
                    text = stringResource(R.string.customer_order_location_open_app_settings),
                    onClick = onOpenAppSettingsClick,
                    enabled = !isDetectingLocation,
                    style = PharmaButtonStyle.Outlined,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            PharmaButton(
                text = stringResource(R.string.customer_order_location_open_gps_settings),
                onClick = onOpenLocationSettingsClick,
                enabled = !isDetectingLocation,
                style = PharmaButtonStyle.Outlined,
                modifier = Modifier.fillMaxWidth(),
            )
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

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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
