package com.pharmalink.feature.orders.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrdersUiState(
    val screenState: ScreenState<List<Order>> = ScreenState.Loading,
    val pending: List<Order> = emptyList(),
    val approved: List<Order> = emptyList(),
    val rejected: List<Order> = emptyList(),
    val delivered: List<Order> = emptyList(),
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val repository: PharmaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OrdersUiState())
    val state: StateFlow<OrdersUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeOrders().collectLatest { orders ->
                // Active/Shipping statuses for "Approved" tab
                val activeStatuses = setOf(
                    OrderStatus.CONFIRMED,
                    OrderStatus.IN_PROGRESS,
                    OrderStatus.READY_FOR_PICKUP,
                    OrderStatus.OUT_FOR_DELIVERY
                )
                
                _state.value = OrdersUiState(
                    screenState = if (orders.isEmpty()) ScreenState.Empty else ScreenState.Success(orders),
                    pending = orders.filter { it.status == OrderStatus.PENDING },
                    approved = orders.filter { it.status in activeStatuses },
                    rejected = orders.filter { it.status == OrderStatus.REJECTED || it.status == OrderStatus.CANCELLED },
                    delivered = orders.filter { it.status == OrderStatus.DELIVERED },
                    isRefreshing = _state.value.isRefreshing,
                    isOffline = false,
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            delay(500)
            _state.update { it.copy(isRefreshing = false, isOffline = false) }
        }
    }
}
