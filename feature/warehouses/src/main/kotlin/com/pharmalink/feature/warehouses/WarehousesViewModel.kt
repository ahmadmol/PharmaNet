package com.pharmalink.feature.warehouses

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.Warehouse
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WarehousesViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(WarehousesUiState())
    val uiState: StateFlow<WarehousesUiState> = _uiState.asStateFlow()
    private var warehousesJob: Job? = null

    init {
        loadWarehouses()
    }

    private fun loadWarehouses() {
        warehousesJob?.cancel()
        warehousesJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            pharmaRepository.observeWarehouses()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = mapErrorToUserMessage(error),
                    )
                }
                .collect { warehouses ->
                    val currentWarehouses = _uiState.value.warehouses
                    val newWarehouseItems = warehouses.map { it.toWarehouseItem() }

                    if (currentWarehouses != newWarehouseItems) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            warehouses = newWarehouseItems,
                            errorMessage = null
                        )
                    } else {
                        if (_uiState.value.isLoading) {
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }
                    }
                }
        }
    }
    
    fun refreshWarehouses() {
        loadWarehouses()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    private fun mapErrorToUserMessage(error: Throwable): String {
        return when {
            error.message?.contains("network", ignoreCase = true) == true ||
            error.message?.contains("connection", ignoreCase = true) == true ->
                context.getString(R.string.error_network)
            error.message?.contains("permission", ignoreCase = true) == true ||
            error.message?.contains("unauthorized", ignoreCase = true) == true ->
                context.getString(R.string.error_permission)
            else -> error.message ?: context.getString(R.string.warehouse_error_loading_failed)
        }
    }
    
    private fun Warehouse.toWarehouseItem(): WarehouseItem {
        val statusType = when {
            inStockPercent > 70 -> StatusType.AVAILABLE
            inStockPercent > 30 -> StatusType.LOW_STOCK
            else -> StatusType.CLOSED
        }
        
        val status = when (statusType) {
            StatusType.AVAILABLE -> context.getString(R.string.warehouse_status_available)
            StatusType.LOW_STOCK -> context.getString(R.string.warehouse_status_low_stock)
            StatusType.CLOSED -> context.getString(R.string.warehouse_status_closed)
        }
        
        return WarehouseItem(
            id = id,
            name = name,
            address = "$city, $district",
            status = status,
            statusType = statusType,
            supportsColdChain = supportsColdChain,
            stockPercent = inStockPercent,
            distance = distanceLabel,
            estimatedDelivery = estimatedDeliveryLabel,
            phoneNumber = phoneNumber,
            lastUpdated = lastUpdatedLabel
        )
    }
}