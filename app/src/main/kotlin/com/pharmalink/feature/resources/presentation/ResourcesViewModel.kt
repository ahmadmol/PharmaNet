package com.pharmalink.feature.resources.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.Warehouse
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WarehouseFilter {
    ALL,
    NEARBY,
    COLD_CHAIN,
}

enum class WarehouseSort {
    NEAREST,
    STOCK,
    LATEST,
}

data class ResourcesUiState(
    val query: String = "",
    val selectedFilter: WarehouseFilter = WarehouseFilter.ALL,
    val selectedSort: WarehouseSort = WarehouseSort.NEAREST,
    val screenState: ScreenState<List<Warehouse>> = ScreenState.Loading,
)

@HiltViewModel
class ResourcesViewModel @Inject constructor(
    private val repository: PharmaRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(WarehouseFilter.ALL)
    private val sort = MutableStateFlow(WarehouseSort.NEAREST)
    private val _uiState = MutableStateFlow(ResourcesUiState())
    val uiState: StateFlow<ResourcesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeWarehouses(),
                query,
                filter,
                sort,
            ) { warehouses, queryValue, filterValue, sortValue ->
                val filtered = warehouses
                    .filter { warehouse ->
                        val matchesText = queryValue.isBlank() ||
                            warehouse.name.contains(queryValue, ignoreCase = true) ||
                            warehouse.city.contains(queryValue, ignoreCase = true) ||
                            warehouse.district.contains(queryValue, ignoreCase = true)
                        val matchesFilter = when (filterValue) {
                            WarehouseFilter.ALL -> true
                            WarehouseFilter.NEARBY -> warehouse.distanceLabel.contains("4") || warehouse.distanceLabel.contains("7")
                            WarehouseFilter.COLD_CHAIN -> warehouse.supportsColdChain
                        }
                        matchesText && matchesFilter
                    }
                    .let { list ->
                        when (sortValue) {
                            WarehouseSort.NEAREST -> list.sortedBy { it.distanceLabel }
                            WarehouseSort.STOCK -> list.sortedByDescending { it.inStockPercent }
                            WarehouseSort.LATEST -> list.sortedBy { it.lastUpdatedLabel.length }
                        }
                    }

                ResourcesUiState(
                    query = queryValue,
                    selectedFilter = filterValue,
                    selectedSort = sortValue,
                    screenState = if (filtered.isEmpty()) ScreenState.Empty else ScreenState.Success(filtered),
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterSelected(value: WarehouseFilter) {
        filter.value = value
    }

    fun onSortSelected(value: WarehouseSort) {
        sort.value = value
    }
    
    fun refresh() {
        // Trigger refresh by updating query with same value
        query.value = query.value
    }
}
