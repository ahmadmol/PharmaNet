package com.pharmalink.feature.orders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.validation.SyrianPhone
import com.pharmalink.core.location.FacilityLocationService
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.CustomerRequestScope
import com.pharmalink.domain.model.CustomerRequestUrgency
import com.pharmalink.domain.model.FulfillmentType
import com.pharmalink.feature.orders.usecase.CreateCustomerOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CreateCustomerOrderViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val pharmaRepository: com.pharmalink.data.repository.PharmaRepository,
    private val createCustomerOrderUseCase: CreateCustomerOrderUseCase,
    private val locationService: FacilityLocationService,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateCustomerOrderUiState())
    val uiState: StateFlow<CreateCustomerOrderUiState> = _uiState.asStateFlow()

    private var initialized = false

    fun initialize(
        medicine: MedicineSummaryUi,
        pharmacy: PharmacySummaryUi,
    ) {
        if (initialized) return
        initialized = true
        _uiState.update {
            it.copy(
                medicine = medicine,
                pharmacy = pharmacy,
                quantityInput = "1",
                fulfillmentType = if (pharmacy.supportsDelivery) {
                    FulfillmentType.DELIVERY
                } else {
                    FulfillmentType.PICKUP
                },
                requestScope = if (pharmacy.isAllPharmaciesRequest) {
                    CustomerRequestScope.ALL_PHARMACIES
                } else {
                    CustomerRequestScope.SPECIFIC_PHARMACY
                },
                urgency = if (pharmacy.isAllPharmaciesRequest) {
                    CustomerRequestUrgency.NORMAL
                } else {
                    CustomerRequestUrgency.URGENT
                },
            )
        }
    }

    fun onUrgencyChange(value: CustomerRequestUrgency) {
        _uiState.update {
            it.copy(
                urgency = value,
                requestScope = if (value == CustomerRequestUrgency.NORMAL) {
                    CustomerRequestScope.ALL_PHARMACIES
                } else {
                    CustomerRequestScope.SPECIFIC_PHARMACY
                },
                submitErrorMessage = null,
            )
        }
    }

    fun onQuantityChange(value: String) {
        val digits = value.filter(Char::isDigit)
        _uiState.update {
            it.copy(
                quantityInput = digits,
                quantityErrorMessage = null,
                submitErrorMessage = null,
            )
        }
    }

    fun onIncrementQuantity() {
        val quantity = (_uiState.value.quantityInput.toIntOrNull() ?: 0) + 1
        onQuantityChange(quantity.toString())
    }

    fun onDecrementQuantity() {
        val current = _uiState.value.quantityInput.toIntOrNull() ?: 1
        val next = if (current > 1) current - 1 else 1
        onQuantityChange(next.toString())
    }

    fun onFulfillmentTypeChange(value: FulfillmentType) {
        _uiState.update {
            it.copy(
                fulfillmentType = value,
                deliveryAddressErrorMessage = null,
                deliveryLocationErrorMessage = null,
                deliveryPhoneErrorMessage = null,
                submitErrorMessage = null,
            )
        }
    }

    fun detectDeliveryLocation() {
        if (_uiState.value.isDetectingLocation) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDetectingLocation = true,
                    deliveryAddressErrorMessage = null,
                    deliveryLocationErrorMessage = null,
                    submitErrorMessage = null,
                )
            }
            locationService.getFreshFacilityLocation().fold(
                onSuccess = { location ->
                    _uiState.update {
                        it.copy(
                            deliveryAddress = location.areaName,
                            deliveryLatitude = location.latitude,
                            deliveryLongitude = location.longitude,
                            isDetectingLocation = false,
                            deliveryAddressErrorMessage = null,
                            deliveryLocationErrorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDetectingLocation = false,
                            deliveryLocationErrorMessage = mapLocationErrorToMessage(error),
                        )
                    }
                },
            )
        }
    }

    fun onDeliveryLocationPermissionDenied(permanentlyDenied: Boolean) {
        _uiState.update {
            it.copy(
                deliveryLocationErrorMessage = context.getString(
                    if (permanentlyDenied) {
                        R.string.customer_order_location_permission_permanently_denied
                    } else {
                        R.string.customer_order_location_permission_denied
                    },
                ),
                submitErrorMessage = null,
            )
        }
    }

    fun onDeliveryAddressDetected(value: String, latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(
                deliveryAddress = value,
                deliveryLatitude = latitude,
                deliveryLongitude = longitude,
                deliveryAddressErrorMessage = null,
                deliveryLocationErrorMessage = null,
                submitErrorMessage = null,
            )
        }
    }

    fun onDeliveryPhoneChange(value: String) {
        _uiState.update {
            it.copy(
                deliveryPhone = value,
                deliveryPhoneErrorMessage = null,
                submitErrorMessage = null,
            )
        }
    }

    fun onNotesChange(value: String) {
        _uiState.update {
            it.copy(
                notes = value,
                submitErrorMessage = null,
            )
        }
    }

    fun onImageSelected(uri: android.net.Uri?) {
        _uiState.update {
            it.copy(
                prescriptionUri = uri,
                submitErrorMessage = null,
            )
        }
    }

    fun onSubmitClick() {
        val state = _uiState.value
        val validation = validate(state)
        if (validation != null) {
            _uiState.update {
                it.copy(
                    quantityErrorMessage = validation.quantityErrorMessage,
                    deliveryAddressErrorMessage = validation.deliveryAddressErrorMessage,
                    deliveryLocationErrorMessage = validation.deliveryLocationErrorMessage,
                    deliveryPhoneErrorMessage = validation.deliveryPhoneErrorMessage,
                    submitErrorMessage = validation.generalErrorMessage,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitErrorMessage = null) }

            val snapshot = authRepository.getUserSnapshot()
            if (snapshot?.accountType != AccountType.PUBLIC_USER) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        submitErrorMessage = context.getString(R.string.error_permission),
                    )
                }
                return@launch
            }

            // 1. Upload prescription if present
            var finalPrescriptionUrl: String? = null
            if (state.prescriptionUri != null) {
                _uiState.update { it.copy(isUploadingImage = true) }
                pharmaRepository.uploadPrescription(state.prescriptionUri).fold(
                    onSuccess = { url ->
                        finalPrescriptionUrl = url
                        _uiState.update { it.copy(isUploadingImage = false) }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                isUploadingImage = false,
                                submitErrorMessage = context.getString(R.string.customer_order_prescription_upload_failed),
                            )
                        }
                        return@launch
                    }
                )
            }

            val requiresLocation = state.requiresDeliveryLocation()
            val normalizedPhone = if (state.fulfillmentType == FulfillmentType.DELIVERY) {
                SyrianPhone.normalizeToE164Digits(state.deliveryPhone.trim())
            } else {
                null
            }

            // Ensure quantity is valid before calling UseCase
            val quantity = state.quantityInput.toIntOrNull() ?: 1
            val safeQuantity = if (quantity > 0) quantity else 1

            createCustomerOrderUseCase(
                medicineId = state.medicine.id,
                medicineName = state.medicine.name,
                quantity = safeQuantity,
                unit = state.medicine.strength.ifBlank {
                    state.medicine.brand.ifBlank { state.medicine.name }
                },
                pharmacyId = state.pharmacy.id.takeIf { state.requestScope == CustomerRequestScope.SPECIFIC_PHARMACY },
                urgency = state.urgency,
                requestScope = state.requestScope,
                fulfillmentType = state.fulfillmentType,
                deliveryAddress = state.deliveryAddress.trim().takeIf { requiresLocation },
                deliveryLatitude = state.deliveryLatitude.takeIf { requiresLocation },
                deliveryLongitude = state.deliveryLongitude.takeIf { requiresLocation },
                deliveryPhone = normalizedPhone,
                notes = state.notes.trim().takeIf { it.isNotBlank() },
                accountType = snapshot.accountType,
                prescriptionUrl = finalPrescriptionUrl,
            ).fold(
                onSuccess = { order ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            createdOrderId = order.id,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submitErrorMessage = mapErrorToMessage(error),
                        )
                    }
                },
            )
        }
    }

    fun consumeCreatedOrder() {
        _uiState.update { it.copy(createdOrderId = null) }
    }

    private fun validate(state: CreateCustomerOrderUiState): CreateCustomerOrderValidation? {
        val quantity = state.quantityInput.toIntOrNull()
        if (state.medicine.id.isBlank() ||
            (state.requestScope == CustomerRequestScope.SPECIFIC_PHARMACY && state.pharmacy.id.isBlank())
        ) {
            return CreateCustomerOrderValidation(
                generalErrorMessage = context.getString(R.string.customer_order_setup_error),
            )
        }
        if (quantity == null || quantity <= 0) {
            return CreateCustomerOrderValidation(
                quantityErrorMessage = context.getString(R.string.customer_order_quantity_error),
            )
        }
        if (state.requiresDeliveryLocation()) {
            if (state.deliveryAddress.isBlank()) {
                return CreateCustomerOrderValidation(
                    deliveryAddressErrorMessage = context.getString(R.string.customer_order_address_required),
                )
            }
            if (state.deliveryLatitude == null || state.deliveryLongitude == null) {
                return CreateCustomerOrderValidation(
                    deliveryLocationErrorMessage = context.getString(R.string.customer_order_location_required),
                )
            }
        }
        if (state.fulfillmentType == FulfillmentType.DELIVERY) {
            if (state.deliveryPhone.isBlank()) {
                return CreateCustomerOrderValidation(
                    deliveryPhoneErrorMessage = context.getString(R.string.customer_order_phone_required),
                )
            }
            if (!SyrianPhone.isValid(state.deliveryPhone.trim())) {
                return CreateCustomerOrderValidation(
                    deliveryPhoneErrorMessage = context.getString(R.string.customer_order_phone_invalid),
                )
            }
        }
        return null
    }

    private fun mapErrorToMessage(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("quantity", ignoreCase = true) ->
                context.getString(R.string.customer_order_quantity_error)
            message.contains("address", ignoreCase = true) ->
                context.getString(R.string.customer_order_address_required)
            message.contains("latitude", ignoreCase = true) ||
                message.contains("longitude", ignoreCase = true) ||
                message.contains("location", ignoreCase = true) ->
                context.getString(R.string.customer_order_location_required)
            message.contains("phone", ignoreCase = true) ->
                context.getString(R.string.customer_order_phone_required)
            message.contains("permission", ignoreCase = true) ||
                message.contains("public users", ignoreCase = true) ||
                message.contains("unauthorized", ignoreCase = true) ->
                context.getString(R.string.error_permission)
            message.contains("network", ignoreCase = true) ||
                message.contains("connection", ignoreCase = true) ->
                context.getString(R.string.error_network)
            else -> context.getString(R.string.customer_order_submit_failed)
        }
    }

    private fun mapLocationErrorToMessage(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("LOCATION_PERMISSION_DENIED", ignoreCase = true) ->
                context.getString(R.string.customer_order_location_permission_denied)
            message.contains("LOCATION_DISABLED", ignoreCase = true) ->
                context.getString(R.string.customer_order_location_disabled)
            message.contains("LOCATION_UNAVAILABLE", ignoreCase = true) ->
                context.getString(R.string.customer_order_location_unavailable)
            else -> context.getString(R.string.customer_order_location_unavailable)
        }
    }
}

private fun CreateCustomerOrderUiState.requiresDeliveryLocation(): Boolean =
    fulfillmentType == FulfillmentType.DELIVERY || requestScope == CustomerRequestScope.ALL_PHARMACIES

data class CreateCustomerOrderUiState(
    val medicine: MedicineSummaryUi = MedicineSummaryUi(),
    val pharmacy: PharmacySummaryUi = PharmacySummaryUi(),
    val quantityInput: String = "1",
    val fulfillmentType: FulfillmentType = FulfillmentType.PICKUP,
    val urgency: CustomerRequestUrgency = CustomerRequestUrgency.URGENT,
    val requestScope: CustomerRequestScope = CustomerRequestScope.SPECIFIC_PHARMACY,
    val deliveryAddress: String = "",
    val deliveryLatitude: Double? = null,
    val deliveryLongitude: Double? = null,
    val deliveryPhone: String = "",
    val notes: String = "",
    val prescriptionUri: android.net.Uri? = null,
    val isSubmitting: Boolean = false,
    val isUploadingImage: Boolean = false,
    val isDetectingLocation: Boolean = false,
    val submitErrorMessage: String? = null,
    val quantityErrorMessage: String? = null,
    val deliveryAddressErrorMessage: String? = null,
    val deliveryLocationErrorMessage: String? = null,
    val deliveryPhoneErrorMessage: String? = null,
    val createdOrderId: String? = null,
)

private data class CreateCustomerOrderValidation(
    val quantityErrorMessage: String? = null,
    val deliveryAddressErrorMessage: String? = null,
    val deliveryLocationErrorMessage: String? = null,
    val deliveryPhoneErrorMessage: String? = null,
    val generalErrorMessage: String? = null,
)
