package com.pharmalink.feature.request

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.mapper.toUserIdentity
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.RequestStatus
import com.pharmalink.domain.model.RequestTransitions
import com.pharmalink.feature.request.usecase.DeleteRequestUseCase
import com.pharmalink.feature.request.usecase.SubmitRequestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class RequestDetailsUiState(
    val screenState: ScreenState<Request> = ScreenState.Loading,
    val accountType: AccountType? = null,
    val relatedOrder: Order? = null,
    val isActionInProgress: Boolean = false,
    val actionErrorMessage: String? = null,
)

@HiltViewModel
class RequestDetailsViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
    private val authRepository: AuthRepository,
    private val submitRequestUseCase: SubmitRequestUseCase,
    private val deleteRequestUseCase: DeleteRequestUseCase,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestDetailsUiState())
    val uiState: StateFlow<RequestDetailsUiState> = _uiState.asStateFlow()

    private val requestId: String = savedStateHandle.get<String>(NavArgs.REQUEST_ID).orEmpty()

    init {
        loadRequestDetails()
        observeNotifications()
    }
    
    private fun observeNotifications() {
        viewModelScope.launch {
            pharmaRepository.observeNotifications().collect { notifications ->
                notifications.forEach { notification ->
                    if (notification.type == com.pharmalink.domain.model.NotificationType.ORDER_UPDATE &&
                        notification.destination == com.pharmalink.domain.model.NotificationDestination.REQUEST &&
                        notification.destinationId == requestId) {
                        refreshRequest()
                    }
                }
            }
        }
    }
    
    private fun loadRequestDetails() {
        viewModelScope.launch {
            if (requestId.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    screenState = ScreenState.Error(context.getString(R.string.request_error_invalid_id)),
                )
                return@launch
            }
            
            try {
                val userSnapshot = authRepository.getUserSnapshot()
                val userIdentity = userSnapshot?.toUserIdentity()
                val accountType = userIdentity?.role ?: userSnapshot?.accountType
                _uiState.value = _uiState.value.copy(
                    screenState = ScreenState.Loading,
                    accountType = accountType,
                )
                
                pharmaRepository.getRequest(requestId).fold(
                    onSuccess = { request ->
                        _uiState.value = if (request != null) {
                            _uiState.value.copy(
                                screenState = ScreenState.Success(request),
                                relatedOrder = loadRelatedOrder(request),
                            )
                        } else {
                            _uiState.value.copy(
                                screenState = ScreenState.Error(context.getString(R.string.request_error_not_found)),
                                relatedOrder = null,
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            screenState = ScreenState.Error(mapErrorToUserMessage(e)),
                        )
                    },
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    screenState = ScreenState.Error(mapErrorToUserMessage(e)),
                )
            }
        }
    }
    
    fun refreshRequest() {
        loadRequestDetails()
    }

    fun acceptRequest(totalPriceCents: Long) {
        val currentRequest = (_uiState.value.screenState as? ScreenState.Success)?.data ?: return
        if (_uiState.value.isActionInProgress) return
        val role = _uiState.value.accountType
        if (role != AccountType.WAREHOUSE ||
            !RequestTransitions.canTransition(currentRequest.status, RequestStatus.QUOTE_PENDING, role)
        ) {
            _uiState.value = _uiState.value.copy(
                actionErrorMessage = context.getString(R.string.error_permission),
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isActionInProgress = true,
                actionErrorMessage = null,
            )

            pharmaRepository.warehouseAcceptB2bRequest(
                requestId = currentRequest.id,
                totalPriceCents = totalPriceCents,
            ).fold(
                onSuccess = { updatedRequest ->
                    val displayRequest = updatedRequest.withItemsFallback(currentRequest)
                    _uiState.value = _uiState.value.copy(
                        screenState = ScreenState.Success(displayRequest),
                        relatedOrder = loadRelatedOrder(displayRequest),
                        isActionInProgress = false,
                        actionErrorMessage = null,
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isActionInProgress = false,
                        actionErrorMessage = mapErrorToUserMessage(error),
                    )
                },
            )
        }
    }

    fun updateRequestStatus(targetStatus: RequestStatus) {
        val currentRequest = (_uiState.value.screenState as? ScreenState.Success)?.data ?: return
        if (_uiState.value.isActionInProgress) return
        val role = _uiState.value.accountType
        if (role != AccountType.WAREHOUSE) {
            _uiState.value = _uiState.value.copy(
                actionErrorMessage = context.getString(R.string.error_permission),
            )
            return
        }
        if (!RequestTransitions.canTransition(currentRequest.status, targetStatus, role)) {
            _uiState.value = _uiState.value.copy(
                actionErrorMessage = context.getString(R.string.error_permission),
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isActionInProgress = true,
                actionErrorMessage = null,
            )

            val result = when (targetStatus) {
                RequestStatus.QUOTE_PENDING -> Result.failure(
                    IllegalStateException("Quoting a B2B request requires a total price"),
                )
                RequestStatus.ACCEPTED -> Result.failure(
                    IllegalStateException("Accepting a B2B request requires a total price"),
                )
                RequestStatus.REJECTED -> pharmaRepository.warehouseRejectB2bRequest(currentRequest.id)
                RequestStatus.IN_PROGRESS -> pharmaRepository.warehouseStartB2bFulfillment(currentRequest.id)
                RequestStatus.FULFILLED -> pharmaRepository.warehouseMarkB2bDelivered(currentRequest.id)
                else -> Result.failure(
                    IllegalStateException("Unsupported warehouse B2B transition target: $targetStatus"),
                )
            }

            result.fold(
                onSuccess = { updatedRequest ->
                    val displayRequest = updatedRequest.withItemsFallback(currentRequest)
                    _uiState.value = _uiState.value.copy(
                        screenState = ScreenState.Success(displayRequest),
                        relatedOrder = loadRelatedOrder(displayRequest),
                        isActionInProgress = false,
                        actionErrorMessage = null,
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isActionInProgress = false,
                        actionErrorMessage = mapErrorToUserMessage(error),
                    )
                },
            )
        }
    }

    fun submitRequest() {
        val currentRequest = (_uiState.value.screenState as? ScreenState.Success)?.data ?: return
        val currentAccountType = _uiState.value.accountType ?: return
        if (_uiState.value.isActionInProgress) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isActionInProgress = true,
                actionErrorMessage = null,
            )

            submitRequestUseCase(
                request = currentRequest,
                accountType = currentAccountType,
            ).fold(
                onSuccess = { updatedRequest ->
                    val displayRequest = updatedRequest.withItemsFallback(currentRequest)
                    _uiState.value = _uiState.value.copy(
                        screenState = ScreenState.Success(displayRequest),
                        relatedOrder = loadRelatedOrder(displayRequest),
                        isActionInProgress = false,
                        actionErrorMessage = null,
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isActionInProgress = false,
                        actionErrorMessage = mapErrorToUserMessage(error),
                    )
                },
            )
        }
    }

    fun deleteRequest() {
        val currentRequest = (_uiState.value.screenState as? ScreenState.Success)?.data ?: return
        val currentAccountType = _uiState.value.accountType ?: return
        if (_uiState.value.isActionInProgress) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isActionInProgress = true,
                actionErrorMessage = null,
            )

            deleteRequestUseCase(
                request = currentRequest,
                accountType = currentAccountType,
            ).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        screenState = ScreenState.Error(context.getString(R.string.request_error_deleted)),
                        relatedOrder = null,
                        isActionInProgress = false,
                        actionErrorMessage = null,
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isActionInProgress = false,
                        actionErrorMessage = mapErrorToUserMessage(error),
                    )
                },
            )
        }
    }

    fun acceptQuote() {
        handlePharmacyQuoteDecision { request ->
            pharmaRepository.pharmacyAcceptB2bQuote(request.id)
        }
    }

    fun rejectQuote() {
        handlePharmacyQuoteDecision { request ->
            pharmaRepository.pharmacyRejectB2bQuote(request.id)
        }
    }

    fun clearActionError() {
        _uiState.value = _uiState.value.copy(actionErrorMessage = null)
    }

    private fun handlePharmacyQuoteDecision(
        action: suspend (Request) -> Result<Request>,
    ) {
        val currentRequest = (_uiState.value.screenState as? ScreenState.Success)?.data ?: return
        if (_uiState.value.isActionInProgress) return
        val role = _uiState.value.accountType
        if (role != AccountType.PHARMACY ||
            !RequestTransitions.canTransition(currentRequest.status, RequestStatus.ACCEPTED, role)
        ) {
            _uiState.value = _uiState.value.copy(
                actionErrorMessage = context.getString(R.string.error_permission),
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isActionInProgress = true,
                actionErrorMessage = null,
            )

            action(currentRequest).fold(
                onSuccess = { updatedRequest ->
                    val displayRequest = updatedRequest.withItemsFallback(currentRequest)
                    _uiState.value = _uiState.value.copy(
                        screenState = ScreenState.Success(displayRequest),
                        relatedOrder = loadRelatedOrder(displayRequest),
                        isActionInProgress = false,
                        actionErrorMessage = null,
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isActionInProgress = false,
                        actionErrorMessage = mapErrorToUserMessage(error),
                    )
                },
            )
        }
    }

    private suspend fun loadRelatedOrder(request: Request): Order? {
        val orderId = request.relatedOrderId ?: return null
        return pharmaRepository.getOrder(orderId).getOrNull()
    }
    
    private fun mapErrorToUserMessage(error: Throwable): String {
        return when {
            error.message?.contains("not found", ignoreCase = true) == true ||
            error.message?.contains("404", ignoreCase = true) == true ->
                context.getString(R.string.request_error_not_found)
            error.message?.contains("network", ignoreCase = true) == true ||
            error.message?.contains("connection", ignoreCase = true) == true ->
                context.getString(R.string.error_network)
            error.message?.contains("permission", ignoreCase = true) == true ||
            error.message?.contains("unauthorized", ignoreCase = true) == true ->
                context.getString(R.string.error_permission)
            error.message?.contains("deleted", ignoreCase = true) == true ->
                context.getString(R.string.request_error_deleted)
            else -> context.getString(R.string.request_error_action_failed)
        }
    }

    private fun Request.withItemsFallback(previous: Request): Request {
        if (items.isNotEmpty() || previous.items.isEmpty() || id != previous.id) return this
        val firstItem = previous.items.first()
        return copy(
            medicineId = firstItem.medicineId,
            medicineName = firstItem.medicineName,
            medicineSubtitle = firstItem.medicineSubtitle,
            quantity = firstItem.quantity,
            unit = firstItem.unit,
            items = previous.items,
        )
    }
}
