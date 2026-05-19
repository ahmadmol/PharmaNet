package com.pharmalink.feature.orders

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.feature.orders.usecase.AcceptCustomerOrderPriceUseCase
import com.pharmalink.feature.orders.usecase.CancelCustomerOrderUseCase
import com.pharmalink.feature.orders.usecase.GetMyOrdersUseCase
import com.pharmalink.feature.orders.usecase.RejectCustomerOrderPriceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerOrderDetailUiState(
    val screenState: ScreenState<CustomerOrderDetailUi> = ScreenState.Loading,
    val isCancelDialogVisible: Boolean = false,
    val isCancelling: Boolean = false,
    val isAcceptingPrice: Boolean = false,
    val isRejectingPrice: Boolean = false,
    val actionErrorMessage: String? = null,
    val actionSuccessMessage: String? = null,
    val cancelCompleted: Boolean = false,
)

@HiltViewModel
class CustomerOrderDetailViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getMyOrdersUseCase: GetMyOrdersUseCase,
    private val cancelCustomerOrderUseCase: CancelCustomerOrderUseCase,
    private val acceptCustomerOrderPriceUseCase: AcceptCustomerOrderPriceUseCase,
    private val rejectCustomerOrderPriceUseCase: RejectCustomerOrderPriceUseCase,
    @param:ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerOrderDetailUiState())
    val uiState: StateFlow<CustomerOrderDetailUiState> = _uiState.asStateFlow()

    private val orderId: String = savedStateHandle.get<String>(NavArgs.ORDER_ID).orEmpty()

    init {
        loadOrder()
    }

    fun refreshOrder() {
        loadOrder()
    }

    fun showCancelDialog() {
        _uiState.update { it.copy(isCancelDialogVisible = true, actionErrorMessage = null) }
    }

    fun dismissCancelDialog() {
        _uiState.update { it.copy(isCancelDialogVisible = false) }
    }

    fun confirmCancelOrder() {
        if (_uiState.value.isCancelling) return

        viewModelScope.launch {
            val snapshot = authRepository.getUserSnapshot()
            val customerId = snapshot?.userId.orEmpty()
            val accountType = snapshot?.accountType

            if (customerId.isBlank() || accountType != AccountType.PUBLIC_USER) {
                _uiState.update {
                    it.copy(
                        isCancelDialogVisible = false,
                        isCancelling = false,
                        actionErrorMessage = context.getString(R.string.error_permission),
                    )
                }
                return@launch
            }

            val currentOrder = (_uiState.value.screenState as? ScreenState.Success)?.data
                ?: run {
                    _uiState.update {
                        it.copy(
                            isCancelDialogVisible = false,
                            actionErrorMessage = context.getString(R.string.customer_order_detail_not_found),
                        )
                    }
                    return@launch
                }

            val matchingOrder = getMyOrdersUseCase(
                customerId = customerId,
                accountType = accountType,
            ).getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isCancelDialogVisible = false,
                        actionErrorMessage = mapErrorToMessage(error),
                    )
                }
                return@launch
            }.firstOrNull { it.id == currentOrder.id && it.isVisiblePublicUserOrder() }

            if (matchingOrder == null) {
                _uiState.update {
                    it.copy(
                        isCancelDialogVisible = false,
                        actionErrorMessage = context.getString(R.string.customer_order_detail_not_found),
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isCancelDialogVisible = false,
                    isCancelling = true,
                    actionErrorMessage = null,
                )
            }

            cancelCustomerOrderUseCase(
                order = matchingOrder,
                accountType = accountType,
                customerId = customerId,
            ).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            cancelCompleted = true,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            actionErrorMessage = mapErrorToMessage(error),
                        )
                    }
                },
            )
        }
    }

    fun consumeCancelCompleted() {
        _uiState.update { it.copy(cancelCompleted = false) }
    }

    fun clearActionError() {
        _uiState.update { it.copy(actionErrorMessage = null) }
    }

    fun acceptPrice() {
        if (_uiState.value.isAcceptingPrice || _uiState.value.isRejectingPrice) return

        viewModelScope.launch {
            val snapshot = authRepository.getUserSnapshot()
            val customerId = snapshot?.userId.orEmpty()
            val accountType = snapshot?.accountType

            if (customerId.isBlank() || accountType != AccountType.PUBLIC_USER) {
                _uiState.update {
                    it.copy(
                        actionErrorMessage = context.getString(R.string.error_permission),
                    )
                }
                return@launch
            }

            val currentOrder = (_uiState.value.screenState as? ScreenState.Success)?.data
                ?: run {
                    _uiState.update {
                        it.copy(
                            actionErrorMessage = context.getString(R.string.customer_order_detail_not_found),
                        )
                    }
                    return@launch
                }

            val matchingOrder = getMyOrdersUseCase(
                customerId = customerId,
                accountType = accountType,
            ).getOrElse { error ->
                _uiState.update {
                    it.copy(
                        actionErrorMessage = mapErrorToMessage(error),
                    )
                }
                return@launch
            }.firstOrNull { it.id == currentOrder.id && it.isVisiblePublicUserOrder() }

            if (matchingOrder == null) {
                _uiState.update {
                    it.copy(
                        actionErrorMessage = context.getString(R.string.customer_order_detail_not_found),
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isAcceptingPrice = true,
                    actionErrorMessage = null,
                    actionSuccessMessage = null,
                )
            }

            acceptCustomerOrderPriceUseCase(
                order = matchingOrder,
                accountType = accountType,
                customerId = customerId,
            ).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isAcceptingPrice = false,
                            actionSuccessMessage = context.getString(R.string.customer_order_accept_price_success),
                        )
                    }
                    // Refresh order to show new status
                    loadOrder()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isAcceptingPrice = false,
                            actionErrorMessage = mapPriceActionErrorToMessage(error, isAccept = true),
                        )
                    }
                },
            )
        }
    }

    fun rejectPrice() {
        if (_uiState.value.isAcceptingPrice || _uiState.value.isRejectingPrice) return

        viewModelScope.launch {
            val snapshot = authRepository.getUserSnapshot()
            val customerId = snapshot?.userId.orEmpty()
            val accountType = snapshot?.accountType

            if (customerId.isBlank() || accountType != AccountType.PUBLIC_USER) {
                _uiState.update {
                    it.copy(
                        actionErrorMessage = context.getString(R.string.error_permission),
                    )
                }
                return@launch
            }

            val currentOrder = (_uiState.value.screenState as? ScreenState.Success)?.data
                ?: run {
                    _uiState.update {
                        it.copy(
                            actionErrorMessage = context.getString(R.string.customer_order_detail_not_found),
                        )
                    }
                    return@launch
                }

            val matchingOrder = getMyOrdersUseCase(
                customerId = customerId,
                accountType = accountType,
            ).getOrElse { error ->
                _uiState.update {
                    it.copy(
                        actionErrorMessage = mapErrorToMessage(error),
                    )
                }
                return@launch
            }.firstOrNull { it.id == currentOrder.id && it.isVisiblePublicUserOrder() }

            if (matchingOrder == null) {
                _uiState.update {
                    it.copy(
                        actionErrorMessage = context.getString(R.string.customer_order_detail_not_found),
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isRejectingPrice = true,
                    actionErrorMessage = null,
                    actionSuccessMessage = null,
                )
            }

            rejectCustomerOrderPriceUseCase(
                order = matchingOrder,
                accountType = accountType,
                customerId = customerId,
            ).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isRejectingPrice = false,
                            actionSuccessMessage = context.getString(R.string.customer_order_reject_price_success),
                        )
                    }
                    // Refresh order to show new status
                    loadOrder()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isRejectingPrice = false,
                            actionErrorMessage = mapPriceActionErrorToMessage(error, isAccept = false),
                        )
                    }
                },
            )
        }
    }

    private fun mapPriceActionErrorToMessage(error: Throwable, isAccept: Boolean): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("confirmed", ignoreCase = true) ->
                context.getString(R.string.customer_order_cancel_pending_only) // Reuse similar message
            message.contains("permission", ignoreCase = true) ||
                message.contains("public users", ignoreCase = true) ||
                message.contains("unauthorized", ignoreCase = true) ->
                context.getString(R.string.error_permission)
            message.contains("network", ignoreCase = true) ||
                message.contains("connection", ignoreCase = true) ->
                context.getString(R.string.error_network)
            message.contains("not found", ignoreCase = true) ->
                context.getString(R.string.customer_order_detail_not_found)
            else -> if (isAccept) {
                context.getString(R.string.customer_order_accept_price_failed)
            } else {
                context.getString(R.string.customer_order_reject_price_failed)
            }
        }
    }

    private fun loadOrder() {
        viewModelScope.launch {
            if (orderId.isBlank()) {
                _uiState.update {
                    it.copy(
                        screenState = ScreenState.Error(context.getString(R.string.customer_order_detail_not_found)),
                        isCancelling = false,
                    )
                }
                return@launch
            }

            val snapshot = authRepository.getUserSnapshot()
            val customerId = snapshot?.userId.orEmpty()
            val accountType = snapshot?.accountType

            if (customerId.isBlank() || accountType != AccountType.PUBLIC_USER) {
                _uiState.update {
                    it.copy(
                        screenState = ScreenState.Error(context.getString(R.string.error_permission)),
                        isCancelling = false,
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    screenState = ScreenState.Loading,
                    isCancelling = false,
                    isCancelDialogVisible = false,
                    actionErrorMessage = null,
                    actionSuccessMessage = null,
                )
            }

            getMyOrdersUseCase(
                customerId = customerId,
                accountType = accountType,
            ).fold(
                onSuccess = { orders ->
                    val order = orders
                        .firstOrNull { it.id == orderId && it.isVisiblePublicUserOrder() }

                    _uiState.update {
                        it.copy(
                            screenState = if (order == null) {
                                ScreenState.Error(context.getString(R.string.customer_order_detail_not_found))
                            } else {
                                ScreenState.Success(order.toCustomerOrderDetailUi(context))
                            },
                            isCancelling = false,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            screenState = ScreenState.Error(mapErrorToMessage(error)),
                            isCancelling = false,
                        )
                    }
                },
            )
        }
    }

    private fun mapErrorToMessage(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("pending", ignoreCase = true) ->
                context.getString(R.string.customer_order_cancel_pending_only)
            message.contains("permission", ignoreCase = true) ||
                message.contains("public users", ignoreCase = true) ||
                message.contains("unauthorized", ignoreCase = true) ->
                context.getString(R.string.error_permission)
            message.contains("network", ignoreCase = true) ||
                message.contains("connection", ignoreCase = true) ->
                context.getString(R.string.error_network)
            message.contains("not found", ignoreCase = true) ->
                context.getString(R.string.customer_order_detail_not_found)
            else -> context.getString(R.string.customer_order_cancel_failed)
        }
    }
}
