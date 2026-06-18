package com.pharmalink.feature.orders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.feature.orders.usecase.GetMyOrdersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyCustomerOrdersUiState(
    val screenState: ScreenState<List<CustomerOrderListItemUi>> = ScreenState.Loading,
    val isRefreshing: Boolean = false,
    val selectedFilter: String = "الكل",
    val allOrders: List<CustomerOrderListItemUi> = emptyList(),
)

@HiltViewModel
class MyCustomerOrdersViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getMyOrdersUseCase: GetMyOrdersUseCase,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyCustomerOrdersUiState())
    val uiState: StateFlow<MyCustomerOrdersUiState> = _uiState.asStateFlow()

    init {
        refreshOrders()
    }

    fun refreshOrders() {
        viewModelScope.launch {
            val currentState = _uiState.value.screenState
            _uiState.update {
                it.copy(
                    screenState = if (currentState is ScreenState.Success) currentState else ScreenState.Loading,
                    isRefreshing = currentState is ScreenState.Success,
                )
            }

            val snapshot = authRepository.getUserSnapshot()
            val customerId = snapshot?.userId.orEmpty()
            val accountType = snapshot?.accountType

            if (customerId.isBlank() || accountType != AccountType.PUBLIC_USER) {
                _uiState.update {
                    it.copy(
                        screenState = ScreenState.Error(context.getString(R.string.error_permission)),
                        isRefreshing = false,
                    )
                }
                return@launch
            }

            getMyOrdersUseCase(
                customerId = customerId,
                accountType = accountType,
            ).fold(
                onSuccess = { orders ->
                    val visibleOrders = orders
                        .asSequence()
                        .filter { it.isVisiblePublicUserOrder() }
                        .sortedByDescending { it.createdAt }
                        .map { it.toCustomerOrderListItemUi(context) }
                        .toList()

                    _uiState.update {
                        it.copy(
                            allOrders = visibleOrders,
                            screenState = if (visibleOrders.isEmpty()) {
                                ScreenState.Empty
                            } else {
                                ScreenState.Success(applyOrderFilter(visibleOrders, it.selectedFilter))
                            },
                            isRefreshing = false,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            screenState = ScreenState.Error(mapErrorToMessage(error)),
                            isRefreshing = false,
                        )
                    }
                },
            )
        }
    }

    fun onFilterSelected(filter: String) {
        _uiState.update { 
            it.copy(
                selectedFilter = filter,
                screenState = if (it.allOrders.isEmpty()) ScreenState.Empty else ScreenState.Success(applyOrderFilter(it.allOrders, filter))
            )
        }
    }

    private fun applyOrderFilter(orders: List<CustomerOrderListItemUi>, filter: String): List<CustomerOrderListItemUi> {
        return when (filter) {
            "الكل" -> orders
            "قيد المراجعة" -> orders.filter { it.status == com.pharmalink.domain.model.OrderStatus.PENDING }
            "مؤكد" -> orders.filter { it.status == com.pharmalink.domain.model.OrderStatus.CONFIRMED }
            "مسلم" -> orders.filter { it.status == com.pharmalink.domain.model.OrderStatus.DELIVERED }
            else -> orders
        }
    }

    private fun mapErrorToMessage(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("permission", ignoreCase = true) ||
                message.contains("unauthorized", ignoreCase = true) ->
                context.getString(R.string.error_permission)
            message.contains("network", ignoreCase = true) ||
                message.contains("connection", ignoreCase = true) ->
                context.getString(R.string.error_network)
            else -> context.getString(R.string.order_error_loading_failed)
        }
    }
}
