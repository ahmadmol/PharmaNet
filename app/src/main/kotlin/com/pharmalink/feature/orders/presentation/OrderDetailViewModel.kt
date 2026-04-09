package com.pharmalink.feature.orders.presentation

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.R
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.Order
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrderDetailUiState(
    val screenState: ScreenState<Order> = ScreenState.Loading,
)

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val repository: PharmaRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    private val orderId: String = savedStateHandle.get<String>(NavArgs.ORDER_ID).orEmpty()

    init {
        viewModelScope.launch {
            val order = repository.getOrder(orderId)
            _uiState.value = if (order == null) {
                OrderDetailUiState(ScreenState.Error(context.getString(R.string.orders_error_order_not_found)))
            } else {
                OrderDetailUiState(ScreenState.Success(order))
            }
        }
    }
}
