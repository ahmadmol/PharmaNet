package com.pharmalink.feature.admin.ui.warehouses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.Warehouse
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
class AdminWarehousesViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminWarehousesUiState())
    val state: StateFlow<AdminWarehousesUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<AdminWarehousesEffect>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val effect: SharedFlow<AdminWarehousesEffect> = _effect.asSharedFlow()

    private val _allWarehouses = MutableStateFlow<List<WarehouseItemModel>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _sortBy = MutableStateFlow(WarehouseSortBy.NAME)

    init {
        loadWarehouses()
        observeFilteredWarehouses()
    }

    private fun observeFilteredWarehouses() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                _allWarehouses,
                _searchQuery,
                _sortBy
            ) { warehouses, query, sortBy ->
                var filtered = warehouses

                // Apply search filter
                if (query.isNotBlank()) {
                    filtered = filtered.filter { warehouse ->
                        warehouse.name.contains(query, ignoreCase = true) ||
                        warehouse.address.contains(query, ignoreCase = true)
                    }
                }

                // Apply sorting
                filtered = when (sortBy) {
                    WarehouseSortBy.NAME -> filtered.sortedBy { it.name }
                    WarehouseSortBy.LOCATION -> filtered.sortedBy { it.address }
                    WarehouseSortBy.DATE_ADDED -> filtered.sortedByDescending { it.lastUpdatedLabel }
                }

                filtered
            }.collect { filteredWarehouses ->
                _state.update { it.copy(warehouses = filteredWarehouses) }
            }
        }
    }

    fun onAction(action: AdminWarehousesAction) {
        when (action) {
            AdminWarehousesAction.OnRetryClicked -> loadWarehouses()
            AdminWarehousesAction.OnRefreshTriggered -> refreshWarehouses()
            AdminWarehousesAction.OnMenuClicked -> {
                viewModelScope.launch {
                    _effect.emit(AdminWarehousesEffect.ShowAdminMenu)
                }
            }
            is AdminWarehousesAction.OnSearchQueryChanged -> updateSearchQuery(action.query)
            is AdminWarehousesAction.OnSortByChanged -> updateSortBy(action.sortBy)
            is AdminWarehousesAction.OnWarehouseClicked -> navigateToDetail(action.warehouseId)
            is AdminWarehousesAction.OnManageInventoryClicked -> navigateToInventory(action.warehouseId)
            AdminWarehousesAction.OnAddWarehouseClicked -> {
                // Handled in UI - navigate to create facility
            }
        }
    }

    private fun loadWarehouses() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, contentError = "") }

            pharmaRepository.adminGetAllWarehouses()
                .onSuccess { warehouses ->
                    val warehouseModels = warehouses.map { it.toUiModel() }
                    
                    _allWarehouses.value = warehouseModels
                    _state.update {
                        it.copy(
                            isLoading = false,
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            contentError = WAREHOUSES_ERROR_MESSAGE,
                        )
                    }
                }
        }
    }

    private fun refreshWarehouses() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            
            pharmaRepository.adminGetAllWarehouses()
                .onSuccess { warehouses ->
                    val warehouseModels = warehouses.map { it.toUiModel() }
                    
                    _allWarehouses.value = warehouseModels
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isRefreshing = false) }
                    _effect.emit(AdminWarehousesEffect.ShowMessage("فشل تحديث المستودعات"))
                }
        }
    }

    private fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _state.update { it.copy(searchQuery = query) }
    }

    private fun updateSortBy(sortBy: WarehouseSortBy) {
        _sortBy.value = sortBy
        _state.update { it.copy(sortBy = sortBy) }
    }

    private fun navigateToDetail(warehouseId: String) {
        viewModelScope.launch {
            _effect.emit(AdminWarehousesEffect.NavigateToWarehouseDetail(warehouseId))
        }
    }

    private fun navigateToInventory(warehouseId: String) {
        viewModelScope.launch {
            _effect.emit(AdminWarehousesEffect.NavigateToInventoryManagement(warehouseId))
        }
    }

    private fun Warehouse.toUiModel(): WarehouseItemModel {
        val address = "$district، $city".trimStart('،').trim()
        val temperature = if (supportsColdChain) "2-8°C" else "عادي"

        return WarehouseItemModel(
            id = id,
            name = name,
            address = address.ifBlank { city },
            isActive = true,
            temperature = temperature,
            inventoryCount = inStockPercent, // real field: % of items in stock
            lastUpdatedLabel = lastUpdatedLabel ?: "",
        )
    }

    private companion object {
        private const val WAREHOUSES_ERROR_MESSAGE = "تعذر تحميل المستودعات. حاول مرة أخرى."
    }
}



