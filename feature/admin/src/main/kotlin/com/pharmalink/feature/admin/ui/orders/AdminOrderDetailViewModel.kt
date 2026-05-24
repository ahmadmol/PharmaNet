package com.pharmalink.feature.admin.ui.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.navigation.NavArgs
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
class AdminOrderDetailViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderId: String = savedStateHandle.get<String>(NavArgs.ORDER_ID) ?: ""

    private val _state = MutableStateFlow(AdminOrderDetailUiState())
    val state: StateFlow<AdminOrderDetailUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<AdminOrderDetailEffect>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val effect: SharedFlow<AdminOrderDetailEffect> = _effect.asSharedFlow()

    init {
        if (orderId.isBlank()) {
            _state.update { it.copy(contentError = "رقم الطلب غير صالح") }
        } else {
            loadOrderDetail()
        }
    }

    fun onAction(action: AdminOrderDetailAction) {
        when (action) {
            AdminOrderDetailAction.OnRetryClicked -> loadOrderDetail()
        }
    }

    private fun loadOrderDetail() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, contentError = "") }

            pharmaRepository.adminGetOrderDetail(orderId)
                .onSuccess { order ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            order = order,
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            contentError = ORDER_DETAIL_LOAD_ERROR_MESSAGE,
                        )
                    }
                }
        }
    }

    private companion object {
        private const val ORDER_DETAIL_LOAD_ERROR_MESSAGE = "تعذر تحميل تفاصيل الطلب. حاول مرة أخرى."
    }
}
