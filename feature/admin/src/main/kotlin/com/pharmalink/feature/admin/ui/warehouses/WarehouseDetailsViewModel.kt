package com.pharmalink.feature.admin.ui.warehouses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.Warehouse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WarehouseDetailsViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val warehouseId: String = savedStateHandle[NavArgs.WAREHOUSE_ID] ?: ""

    private val _state = MutableStateFlow(WarehouseDetailsUiState())
    val state: StateFlow<WarehouseDetailsUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<WarehouseDetailsEffect>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val effect: SharedFlow<WarehouseDetailsEffect> = _effect.asSharedFlow()

    init {
        if (warehouseId.isEmpty()) {
            _state.update { it.copy(isLoading = false, contentError = "معرف المستودع مفقود") }
        } else {
            loadWarehouseDetails()
        }
    }

    fun onAction(action: WarehouseDetailsAction) {
        when (action) {
            WarehouseDetailsAction.OnRetryClicked -> loadWarehouseDetails()
            WarehouseDetailsAction.OnManageInventoryClicked -> {
                viewModelScope.launch {
                    _effect.emit(WarehouseDetailsEffect.NavigateToInventory(warehouseId))
                }
            }
            WarehouseDetailsAction.OnViewShipmentsClicked -> {
                viewModelScope.launch {
                    _effect.emit(WarehouseDetailsEffect.ShowMessage("عرض الشحنات: قيد التطوير"))
                }
            }
            WarehouseDetailsAction.OnEditClicked -> {
                viewModelScope.launch {
                    _effect.emit(WarehouseDetailsEffect.ShowMessage("تعديل المستودع: قيد التطوير"))
                }
            }
        }
    }

    private fun loadWarehouseDetails() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, contentError = "") }

            pharmaRepository.adminGetAllWarehouses()
                .onSuccess { warehouses ->
                    val warehouse = warehouses.find { it.id == warehouseId }
                    if (warehouse != null) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                warehouse = warehouse.toDetailModel(),
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                contentError = "المستودع غير موجود",
                            )
                        }
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            contentError = e.message ?: "فشل تحميل بيانات المستودع",
                        )
                    }
                }
        }
    }

    private fun Warehouse.toDetailModel(): WarehouseDetailModel {
        // Note: Secondary stats (totalInventoryItems, activeShipments, completedOrders)
        // are not included because endpoints are not available yet
        return WarehouseDetailModel(
            id = id,
            name = name,
            city = city,
            district = district,
            phoneNumber = phoneNumber,
            supportsColdChain = supportsColdChain,
            inStockPercent = inStockPercent,
            lowStockCount = lowStockCount,
            outOfStockCount = outOfStockCount,
            estimatedDeliveryLabel = estimatedDeliveryLabel,
            distanceLabel = distanceLabel,
            lastUpdatedLabel = lastUpdatedLabel,
        )
    }
}
