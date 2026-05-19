package com.pharmalink.feature.admin.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.data.repository.PharmaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AdminOrdersViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminOrdersUiState())
    val state: StateFlow<AdminOrdersUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<AdminOrdersEffect>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val effect: SharedFlow<AdminOrdersEffect> = _effect.asSharedFlow()

    private var currentOffset = 0
    private val pageSize = 50

    init {
        loadOrders()
    }

    fun onAction(action: AdminOrdersAction) {
        when (action) {
            AdminOrdersAction.OnRetryClicked -> loadOrders(reset = true)
            AdminOrdersAction.OnRefreshTriggered -> loadOrders(reset = true)
            is AdminOrdersAction.OnSearchQueryChanged -> {
                _state.update { it.copy(searchQuery = action.query) }
                loadOrders(reset = true)
            }
            is AdminOrdersAction.OnFilterSelected -> {
                _state.update { it.copy(selectedFilter = action.filter) }
                loadOrders(reset = true)
            }
            is AdminOrdersAction.OnOrderClicked -> {
                viewModelScope.launch {
                    _effect.emit(AdminOrdersEffect.NavigateToOrderDetail(action.orderId))
                }
            }
            AdminOrdersAction.OnLoadMore -> {
                if (_state.value.hasMore && !_state.value.isLoading) {
                    loadOrders(reset = false)
                }
            }
        }
    }

    private fun loadOrders(reset: Boolean = false) {
        viewModelScope.launch {
            if (reset) {
                currentOffset = 0
                _state.update { it.copy(isLoading = true, contentError = "", orders = emptyList()) }
            } else {
                _state.update { it.copy(isLoading = true) }
            }

            val filter = _state.value.selectedFilter
            val search = _state.value.searchQuery

            // Map filter to parameters
            val orderType = when (filter) {
                OrderFilter.B2C -> "CUSTOMER_PHARMACY"
                OrderFilter.B2B -> "PHARMACY_WAREHOUSE"
                else -> null
            }
            val status = when (filter) {
                OrderFilter.PENDING -> "PENDING"
                else -> null
            }
            val isUrgent = when (filter) {
                OrderFilter.URGENT -> true
                else -> null
            }

            pharmaRepository.adminGetAllOrders(
                orderType = orderType,
                status = status,
                isUrgent = isUrgent,
                search = search.takeIf { it.isNotBlank() },
                limit = pageSize,
                offset = currentOffset
            )
                .onSuccess { newOrders ->
                    val updatedOrders = if (reset) {
                        newOrders
                    } else {
                        _state.value.orders + newOrders
                    }
                    
                    _state.update {
                        it.copy(
                            isLoading = false,
                            orders = updatedOrders,
                            hasMore = newOrders.size == pageSize,
                        )
                    }
                    
                    if (!reset) {
                        currentOffset += newOrders.size
                    } else {
                        currentOffset = newOrders.size
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            contentError = e.message ?: "فشل تحميل الطلبات",
                        )
                    }
                }
        }
    }
}
