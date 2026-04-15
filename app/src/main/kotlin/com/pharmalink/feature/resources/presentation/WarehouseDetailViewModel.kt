package com.pharmalink.feature.resources.presentation

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.R
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.Warehouse
import com.pharmalink.domain.model.WarehouseShipment
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WarehouseDetailContent(
    val warehouse: Warehouse,
    val shipments: List<WarehouseShipment>,
)

data class WarehouseDetailUiState(
    val screenState: ScreenState<WarehouseDetailContent> = ScreenState.Loading,
)

@HiltViewModel
class WarehouseDetailViewModel @Inject constructor(
    private val repository: PharmaRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WarehouseDetailUiState())
    val uiState: StateFlow<WarehouseDetailUiState> = _uiState.asStateFlow()

    private val warehouseId: String = savedStateHandle.get<String>(NavArgs.WAREHOUSE_ID).orEmpty()

    init {
        viewModelScope.launch {
            repository.getWarehouse(warehouseId).fold(
                onSuccess = { warehouse ->
                    if (warehouse == null) {
                        _uiState.value = WarehouseDetailUiState(
                            ScreenState.Error(context.getString(R.string.resources_error_warehouse_not_found)),
                        )
                        return@fold
                    }
                    repository.getWarehouseShipments(warehouseId).fold(
                        onSuccess = { shipments ->
                            _uiState.value = WarehouseDetailUiState(
                                screenState = ScreenState.Success(
                                    WarehouseDetailContent(
                                        warehouse = warehouse,
                                        shipments = shipments,
                                    ),
                                ),
                            )
                        },
                        onFailure = { e ->
                            _uiState.value = WarehouseDetailUiState(
                                ScreenState.Error(
                                    e.message ?: context.getString(R.string.resources_error_warehouse_not_found),
                                ),
                            )
                        },
                    )
                },
                onFailure = { e ->
                    _uiState.value = WarehouseDetailUiState(
                        ScreenState.Error(
                            e.message ?: context.getString(R.string.resources_error_warehouse_not_found),
                        ),
                    )
                },
            )
        }
    }
}
