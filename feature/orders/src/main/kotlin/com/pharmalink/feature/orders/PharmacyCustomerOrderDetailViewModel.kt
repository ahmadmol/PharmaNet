package com.pharmalink.feature.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.FulfillmentType
import com.pharmalink.domain.model.OrderStatus
import com.pharmalink.domain.model.PharmacyCustomerOrder
import com.pharmalink.feature.orders.usecase.GetPharmacyCustomerOrderDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PharmacyCustomerOrderDetailUiState(
    val screenState: ScreenState<PharmacyCustomerOrderUi> = ScreenState.Loading,
    val isActionInProgress: Boolean = false,
    val actionErrorMessage: String? = null,
    val actionSuccessMessage: String? = null,
)

@HiltViewModel
class PharmacyCustomerOrderDetailViewModel @Inject constructor(
    private val getOrderDetail: GetPharmacyCustomerOrderDetailUseCase,
    private val repository: PharmaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PharmacyCustomerOrderDetailUiState())
    val uiState: StateFlow<PharmacyCustomerOrderDetailUiState> = _uiState.asStateFlow()

    private val orderId: String = savedStateHandle.get<String>(NavArgs.ORDER_ID).orEmpty()
    private var currentOrder: PharmacyCustomerOrder? = null
    private var attemptedRadarClaim = false

    init {
        loadOrder()
    }

    fun refreshOrder() {
        loadOrder()
    }

    fun confirmOrder(totalPriceCents: Long) {
        runAction("تم تأكيد الطلب") {
            repository.confirmCustomerOrder(orderId, totalPriceCents)
        }
    }

    fun rejectOrder() {
        runAction("تم رفض الطلب") {
            repository.rejectCustomerOrder(orderId)
        }
    }

    fun markReadyForPickup() {
        runAction("تم تحديد الطلب كجاهز للاستلام") {
            repository.markCustomerOrderReadyForPickup(orderId)
        }
    }

    fun markOutForDelivery() {
        runAction("تم تحديد الطلب كخارج للتوصيل") {
            repository.markCustomerOrderOutForDelivery(orderId)
        }
    }

    fun markDelivered() {
        runAction("تم تسليم الطلب") {
            repository.markCustomerOrderDelivered(orderId)
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionErrorMessage = null, actionSuccessMessage = null) }
    }

    fun canMarkReadyForPickup(): Boolean =
        currentOrder?.let {
            it.status in setOf(OrderStatus.CONFIRMED, OrderStatus.IN_PROGRESS) &&
                it.fulfillmentType == FulfillmentType.PICKUP
        } == true

    fun canMarkOutForDelivery(): Boolean =
        currentOrder?.let {
            it.status in setOf(OrderStatus.CONFIRMED, OrderStatus.IN_PROGRESS) &&
                it.fulfillmentType == FulfillmentType.DELIVERY
        } == true

    fun canMarkDelivered(): Boolean =
        currentOrder?.status in setOf(OrderStatus.READY_FOR_PICKUP, OrderStatus.OUT_FOR_DELIVERY)

    private fun loadOrder() {
        viewModelScope.launch {
            if (orderId.isBlank()) {
                _uiState.update { it.copy(screenState = ScreenState.Error("معرف الطلب غير صالح")) }
                return@launch
            }

            _uiState.update { it.copy(screenState = ScreenState.Loading, actionErrorMessage = null) }
            if (!attemptedRadarClaim) {
                attemptedRadarClaim = true
                repository.claimNearbyCustomerOrder(orderId)
            }
            getOrderDetail(orderId).fold(
                onSuccess = { order ->
                    currentOrder = order
                    _uiState.update {
                        it.copy(
                            screenState = ScreenState.Success(order.toPharmacyCustomerOrderUi()),
                            isActionInProgress = false,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            screenState = ScreenState.Error(error.message ?: "تعذر تحميل الطلب"),
                            isActionInProgress = false,
                        )
                    }
                },
            )
        }
    }

    private fun runAction(
        successMessage: String,
        action: suspend () -> Result<Unit>,
    ) {
        if (_uiState.value.isActionInProgress) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isActionInProgress = true,
                    actionErrorMessage = null,
                    actionSuccessMessage = null,
                )
            }
            action().fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            actionSuccessMessage = successMessage,
                        )
                    }
                    loadOrder()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            actionErrorMessage = error.message ?: "تعذر تنفيذ الإجراء",
                        )
                    }
                },
            )
        }
    }
}
