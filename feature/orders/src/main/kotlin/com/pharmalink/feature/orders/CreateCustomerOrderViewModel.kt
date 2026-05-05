package com.pharmalink.feature.orders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.validation.SyrianPhone
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
    private val createCustomerOrderUseCase: CreateCustomerOrderUseCase,
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
                deliveryPhoneErrorMessage = null,
                submitErrorMessage = null,
            )
        }
    }

    fun onDeliveryAddressChange(value: String) {
        _uiState.update {
            it.copy(
                deliveryAddress = value,
                deliveryAddressErrorMessage = null,
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

    fun onSubmitClick() {
        val state = _uiState.value
        val validation = validate(state)
        if (validation != null) {
            _uiState.update {
                it.copy(
                    quantityErrorMessage = validation.quantityErrorMessage,
                    deliveryAddressErrorMessage = validation.deliveryAddressErrorMessage,
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

            val normalizedPhone = if (state.fulfillmentType == FulfillmentType.DELIVERY) {
                SyrianPhone.normalizeToE164Digits(state.deliveryPhone.trim())
            } else {
                null
            }

            createCustomerOrderUseCase(
                medicineId = state.medicine.id,
                medicineName = state.medicine.name,
                quantity = state.quantityInput.toInt(),
                unit = state.medicine.strength.ifBlank {
                    state.medicine.brand.ifBlank { state.medicine.name }
                },
                pharmacyId = state.pharmacy.id.takeIf { state.requestScope == CustomerRequestScope.SPECIFIC_PHARMACY },
                urgency = state.urgency,
                requestScope = state.requestScope,
                fulfillmentType = state.fulfillmentType,
                deliveryAddress = state.deliveryAddress.trim().takeIf { state.fulfillmentType == FulfillmentType.DELIVERY },
                deliveryPhone = normalizedPhone,
                notes = state.notes.trim().takeIf { it.isNotBlank() },
                accountType = snapshot.accountType,
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
        if (state.fulfillmentType == FulfillmentType.DELIVERY) {
            if (state.deliveryAddress.isBlank()) {
                return CreateCustomerOrderValidation(
                    deliveryAddressErrorMessage = context.getString(R.string.customer_order_address_required),
                )
            }
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
}

data class CreateCustomerOrderUiState(
    val medicine: MedicineSummaryUi = MedicineSummaryUi(),
    val pharmacy: PharmacySummaryUi = PharmacySummaryUi(),
    val quantityInput: String = "1",
    val fulfillmentType: FulfillmentType = FulfillmentType.PICKUP,
    val urgency: CustomerRequestUrgency = CustomerRequestUrgency.URGENT,
    val requestScope: CustomerRequestScope = CustomerRequestScope.SPECIFIC_PHARMACY,
    val deliveryAddress: String = "",
    val deliveryPhone: String = "",
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val submitErrorMessage: String? = null,
    val quantityErrorMessage: String? = null,
    val deliveryAddressErrorMessage: String? = null,
    val deliveryPhoneErrorMessage: String? = null,
    val createdOrderId: String? = null,
)

private data class CreateCustomerOrderValidation(
    val quantityErrorMessage: String? = null,
    val deliveryAddressErrorMessage: String? = null,
    val deliveryPhoneErrorMessage: String? = null,
    val generalErrorMessage: String? = null,
)
