package com.pharmalink.feature.request

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.ui.UiState
import com.pharmalink.core.repository.RequestRepository
import com.pharmalink.core.repository.WarehouseRepository
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.RequestPriority
import com.pharmalink.domain.model.RequestStatus
import com.pharmalink.domain.model.Warehouse
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateRequestUiState(
    val medicineName: String = "",
    val notes: String = "",
    val urgent: Boolean = false,
    val imageUri: Uri? = null,
    val quantity: Int = 1,
    val unit: String = "",
    val availableWarehouses: List<Warehouse> = emptyList(),
    val selectedWarehouseId: String = "",
    val submitState: UiState<Request> = UiState.Idle,
    val medicineError: String? = null,
    val notesError: String? = null,
    val warehouseError: String? = null,
    val step: Int = 1,
    val showSuccess: Boolean = false,
)

@HiltViewModel
class CreateRequestViewModel @Inject constructor(
    private val requestRepository: RequestRepository,
    private val warehouseRepository: WarehouseRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val suggestionPool: List<String> by lazy {
        context.resources.getStringArray(R.array.request_medicine_suggestions).toList()
    }

    private val _state = MutableStateFlow(
        CreateRequestUiState(unit = context.getString(R.string.request_unit_box)),
    )
    val state: StateFlow<CreateRequestUiState> = _state.asStateFlow()

    fun unitOptions(): List<String> = listOf(
        context.getString(R.string.request_unit_box),
        context.getString(R.string.request_unit_bottle),
        context.getString(R.string.request_unit_pack),
    )

    init {
        viewModelScope.launch {
            warehouseRepository.getWarehouses().collectLatest { warehouses ->
                _state.update {
                    it.copy(
                        availableWarehouses = warehouses,
                        selectedWarehouseId = it.selectedWarehouseId.ifBlank { warehouses.firstOrNull()?.id.orEmpty() },
                    )
                }
            }
        }
    }

    fun suggestionsFor(query: String): List<String> {
        if (query.length < 2) return emptyList()
        return suggestionPool.filter { it.contains(query.trim(), ignoreCase = true) }.take(5)
    }

    fun onMedicineChange(value: String) {
        _state.update {
            it.copy(
                medicineName = value,
                medicineError = null,
                step = when {
                    value.isBlank() -> 1
                    it.selectedWarehouseId.isBlank() -> 2
                    else -> 3
                },
            )
        }
    }

    fun onNotesChange(value: String) {
        _state.update { it.copy(notes = value, notesError = null) }
    }

    fun onUrgentChange(urgent: Boolean) {
        _state.update { it.copy(urgent = urgent, notesError = null) }
    }

    fun onImagePicked(uri: Uri?) {
        _state.update { it.copy(imageUri = uri) }
    }

    fun updateQuantity(delta: Int) {
        val next = (_state.value.quantity + delta).coerceIn(1, 99)
        _state.update { it.copy(quantity = next, step = if (it.medicineName.isBlank()) 1 else 2) }
    }

    fun updateUnit(unit: String) {
        _state.update { it.copy(unit = unit) }
    }

    fun updateWarehouse(warehouseId: String) {
        _state.update {
            it.copy(
                selectedWarehouseId = warehouseId,
                warehouseError = null,
                step = if (it.medicineName.isBlank()) 1 else 3,
            )
        }
    }

    fun submit() {
        val validation = validate()
        if (validation != null) {
            _state.update {
                it.copy(
                    medicineError = validation.medicineError,
                    notesError = validation.notesError,
                    warehouseError = validation.warehouseError,
                    submitState = UiState.Error(validation.messageCode),
                )
            }
            return
        }

        val current = _state.value
        val selectedWarehouse = current.availableWarehouses.firstOrNull { it.id == current.selectedWarehouseId }
            ?: run {
                _state.update {
                    it.copy(
                        warehouseError = context.getString(R.string.request_validation_warehouse),
                        submitState = UiState.Error(MESSAGE_VALIDATION),
                    )
                }
                return
            }

        viewModelScope.launch {
            _state.update { it.copy(submitState = UiState.Loading) }

            val result = requestRepository.createRequest(
                Request(
                    id = "",
                    medicineName = current.medicineName.trim(),
                    quantity = current.quantity,
                    unit = current.unit,
                    notes = current.notes.trim(),
                    priority = if (current.urgent) RequestPriority.URGENT else RequestPriority.NORMAL,
                    status = RequestStatus.DRAFT,
                    warehouseId = selectedWarehouse.id,
                    warehouseName = selectedWarehouse.name,
                    supplierName = selectedWarehouse.name,
                    createdAtLabel = "الآن",
                    updatedAtLabel = "الآن",
                )
            )

            result.fold(
                onSuccess = { createdRequest ->
                    _state.update {
                        it.copy(
                            submitState = UiState.Success(createdRequest),
                            showSuccess = true,
                            step = 3,
                        )
                    }
                },
                onFailure = {
                    _state.update {
                        it.copy(
                            submitState = UiState.Error(context.getString(R.string.request_error_submit_failed)),
                        )
                    }
                },
            )
        }
    }

    fun consumeSubmitSuccess() {
        val warehouses = _state.value.availableWarehouses
        _state.value = CreateRequestUiState(
            availableWarehouses = warehouses,
            selectedWarehouseId = warehouses.firstOrNull()?.id.orEmpty(),
            unit = context.getString(R.string.request_unit_box),
        )
    }

    fun clearError() {
        if (_state.value.submitState is UiState.Error) {
            _state.update { it.copy(submitState = UiState.Idle) }
        }
    }

    private fun validate(): ValidationResult? {
        val current = _state.value
        val medicineError = when {
            current.medicineName.isBlank() -> context.getString(R.string.request_validation_medicine_required)
            current.medicineName.trim().length < 3 -> context.getString(R.string.request_validation_medicine_short)
            else -> null
        }
        val warehouseError = if (current.selectedWarehouseId.isBlank()) {
            context.getString(R.string.request_validation_warehouse)
        } else {
            null
        }
        val notesError = if (current.urgent && current.notes.trim().length < 10) {
            context.getString(R.string.request_validation_urgent_notes)
        } else {
            null
        }

        if (medicineError == null && warehouseError == null && notesError == null) return null
        return ValidationResult(medicineError, notesError, warehouseError, MESSAGE_VALIDATION)
    }

    private data class ValidationResult(
        val medicineError: String? = null,
        val notesError: String? = null,
        val warehouseError: String? = null,
        val messageCode: String,
    )

    private companion object {
        const val MESSAGE_VALIDATION = "validation"
    }
}
