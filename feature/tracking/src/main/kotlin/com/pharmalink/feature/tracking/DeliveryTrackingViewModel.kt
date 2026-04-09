package com.pharmalink.feature.tracking

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.core.repository.DeliveryRepository
import com.pharmalink.domain.model.DeliveryStatus
import com.pharmalink.feature.tracking.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeliveryTrackingViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Nav argument name must match [NavArgs.ORDER_ID] on the `delivery_tracking/{orderId}` route. */
    private val orderId: String = savedStateHandle.get<String>(NavArgs.ORDER_ID).orEmpty()

    private val _uiState = MutableStateFlow(DeliveryTrackingUiState())
    val uiState: StateFlow<DeliveryTrackingUiState> = _uiState.asStateFlow()

    init {
        loadDeliveryTracking(isPullRefresh = false)
    }

    private fun loadDeliveryTracking(isPullRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update { current ->
                if (isPullRefresh) {
                    current.copy(isRefreshing = true)
                } else {
                    current.copy(screenState = ScreenState.Loading)
                }
            }

            val result = deliveryRepository.getDeliveryTracking(orderId)
            _uiState.update { currentState ->
                result.fold(
                    onSuccess = { tracking ->
                        currentState.copy(
                            screenState = ScreenState.Success(tracking),
                            canCallDelegate = tracking.delegate != null && tracking.delegate?.isActive == true,
                            isRefreshing = false,
                        )
                    },
                    onFailure = {
                        currentState.copy(
                            screenState = ScreenState.Error(
                                context.getString(R.string.tracking_error_load_failed),
                            ),
                            isRefreshing = false,
                        )
                    },
                )
            }
        }
    }

    fun refresh() {
        loadDeliveryTracking(isPullRefresh = true)
    }

    fun callDelegate() {
        viewModelScope.launch {
            _uiState.value.let { currentState ->
                if (currentState.screenState is ScreenState.Success) {
                    val tracking = currentState.screenState.data
                    tracking.delegate?.let { delegate ->
                        deliveryRepository.callDelegate(delegate.phone)
                    }
                }
            }
        }
    }

    fun getStatusText(status: DeliveryStatus): String {
        return when (status) {
            DeliveryStatus.PREPARING -> context.getString(R.string.delivery_status_preparing)
            DeliveryStatus.ASSIGNED -> context.getString(R.string.delivery_status_assigned)
            DeliveryStatus.PICKED_UP -> context.getString(R.string.delivery_status_picked_up)
            DeliveryStatus.IN_TRANSIT -> context.getString(R.string.delivery_status_in_transit)
            DeliveryStatus.ARRIVING -> context.getString(R.string.delivery_status_arriving)
            DeliveryStatus.DELIVERED -> context.getString(R.string.delivery_status_delivered)
            DeliveryStatus.FAILED -> context.getString(R.string.delivery_status_failed)
        }
    }
}
