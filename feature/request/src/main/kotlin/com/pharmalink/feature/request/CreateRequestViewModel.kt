package com.pharmalink.feature.request

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.error.MissingPharmacyLinkageException
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AuthSessionState
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.RequestPriority
import com.pharmalink.domain.model.RequestStatus
import com.pharmalink.domain.model.UserSnapshot
import com.pharmalink.domain.model.Warehouse
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateRequestViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val pharmaRepository: PharmaRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateRequestUiState())
    val uiState: StateFlow<CreateRequestUiState> = _uiState.asStateFlow()
    private var currentUserSnapshot: UserSnapshot? = null

    init {
        observeCurrentUser()
        loadAvailableMedicines()
        loadAvailableWarehouses()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            authRepository.observeUserSnapshot().collect { snapshot ->
                currentUserSnapshot = snapshot
            }
        }
    }

    private fun loadAvailableWarehouses() {
        viewModelScope.launch {
            try {
                pharmaRepository.observeWarehouses().collect { warehouses ->
                    val warehouseOptions = warehouses.map { it.toWarehouseOption() }
                    val currentSelection = warehouseOptions.firstOrNull { it.id == _uiState.value.selectedWarehouseId }
                    val defaultWarehouse = warehouseOptions.singleOrNull()
                    _uiState.value = _uiState.value.copy(
                        warehouses = warehouseOptions,
                        selectedWarehouseId = currentSelection?.id
                            ?: _uiState.value.selectedWarehouseId.ifBlank { defaultWarehouse?.id.orEmpty() },
                        selectedWarehouseName = currentSelection?.name
                            ?: _uiState.value.selectedWarehouseName.ifBlank { defaultWarehouse?.name.orEmpty() },
                    )
                }
            } catch (e: Exception) {
                if (_uiState.value.selectedWarehouseId.isBlank()) {
                    _uiState.value = _uiState.value.copy(errorMessage = mapSupabaseErrorToUserMessage(e))
                }
            }
        }
    }

    private fun loadAvailableMedicines() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMedicines = true, medicineLoadError = null)
            
            pharmaRepository.fetchMedicines()
                .onSuccess { medicines ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMedicines = false,
                        medicines = medicines.map { it.toItem() }
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMedicines = false,
                        medicineLoadError = error.message ?: "فشل تحميل قائمة الأدوية"
                    )
                }
        }
    }

    fun onMedicineSelected(medicine: MedicineItem) {
        _uiState.value = _uiState.value.copy(
            selectedMedicine = medicine,
            errorMessage = null
        )
    }

    fun onQuantityChange(newQuantity: String) {
        _uiState.value = _uiState.value.copy(
            quantity = newQuantity,
            errorMessage = null
        )
    }

    fun onNotesChange(newNotes: String) {
        _uiState.value = _uiState.value.copy(
            notes = newNotes,
            errorMessage = null
        )
    }

    fun onWarehouseSelected(warehouseId: String, warehouseName: String) {
        _uiState.value = _uiState.value.copy(
            selectedWarehouseId = warehouseId,
            selectedWarehouseName = warehouseName,
            errorMessage = null
        )
    }

    fun sendRequest() {
        val currentState = _uiState.value
        val validationError = validateRequest(currentState)
        
        if (validationError != null) {
            _uiState.value = currentState.copy(errorMessage = validationError)
            return
        }
        
        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, errorMessage = null)
            
            try {
                val userSnapshot = resolveValidatedUserSnapshot()
                if (userSnapshot == null) {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        errorMessage = context.getString(R.string.error_permission),
                    )
                    return@launch
                }

                if (userSnapshot.accountType != com.pharmalink.domain.model.AccountType.PHARMACY) {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        errorMessage = "إنشاء الطلبات متاح فقط لحسابات الصيدليات",
                    )
                    return@launch
                }

                if (userSnapshot.pharmacyId.isBlank()) {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        errorMessage = "الملف الشخصي غير مكتمل. يرجى تسجيل الدخول من جديد لإعادة مزامنة بيانات الصيدلية.",
                    )
                    return@launch
                }
                
                val selectedMedicine = currentState.selectedMedicine!!
                val selectedWarehouse = currentState.warehouses.firstOrNull { it.id == currentState.selectedWarehouseId.trim() }
                if (selectedWarehouse == null) {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        errorMessage = context.getString(R.string.request_error_warehouse_invalid),
                    )
                    return@launch
                }
                val quantity = currentState.quantity.toIntOrNull() ?: 1
                
                if (quantity <= 0 || quantity > 1000) {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        errorMessage = context.getString(R.string.request_error_invalid_quantity)
                    )
                    return@launch
                }
                
                val request = Request(
                    id = "", // Will be generated by Supabase
                    medicineName = selectedMedicine.name.trim(),
                    medicineSubtitle = selectedMedicine.brand.trim(),
                    quantity = quantity,
                    unit = selectedMedicine.strength.trim(),
                    notes = currentState.notes.trim(),
                    priority = RequestPriority.NORMAL,
                    status = RequestStatus.SUBMITTED,
                    warehouseId = selectedWarehouse.id,
                    warehouseName = selectedWarehouse.name,
                    supplierName = selectedWarehouse.name,
                    createdAtLabel = "",
                    updatedAtLabel = "",
                )
                
                pharmaRepository.createRequest(request).fold(
                    onSuccess = { createdRequest ->
                        _uiState.value = CreateRequestUiState(
                            medicines = currentState.medicines,
                            warehouses = currentState.warehouses,
                            isSuccess = true,
                            createdRequestId = createdRequest.id,
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = currentState.copy(
                            isLoading = false,
                            errorMessage = mapSupabaseErrorToUserMessage(e),
                        )
                    },
                )
            } catch (e: Exception) {
                val userMessage = mapSupabaseErrorToUserMessage(e)
                _uiState.value = currentState.copy(
                    isLoading = false,
                    errorMessage = userMessage
                )
            }
        }
    }

    private suspend fun resolveValidatedUserSnapshot(): UserSnapshot? {
        val authState = authRepository.observeAuthState().first { state ->
            state !is AuthSessionState.Loading
        }
        val authenticatedUser = (authState as? AuthSessionState.Authenticated)?.user
            ?: return currentUserSnapshot ?: authRepository.getUserSnapshot()

        return authRepository.bootstrapAuthenticatedUser(authenticatedUser)
            .getOrElse { error ->
                throw error
            }
    }
    
    private fun validateRequest(state: CreateRequestUiState): String? {
        return when {
            state.selectedMedicine == null ->
                context.getString(R.string.request_error_medicine_required)
            state.selectedWarehouseId.isBlank() ->
                context.getString(R.string.request_error_warehouse_required)
            state.quantity.isBlank() ->
                context.getString(R.string.request_error_quantity_required)
            state.quantity.toIntOrNull() == null ->
                context.getString(R.string.request_error_invalid_quantity_format)
            state.quantity.toIntOrNull()!! <= 0 ->
                context.getString(R.string.request_error_quantity_zero)
            state.quantity.toIntOrNull()!! > 1000 ->
                context.getString(R.string.request_error_quantity_too_large)
            state.notes.length > 500 ->
                context.getString(R.string.request_error_notes_too_long)
            else -> null
        }
    }
    
    private fun mapSupabaseErrorToUserMessage(error: Throwable): String {
        return when {
            error is MissingPharmacyLinkageException ->
                if (error.message?.isNotBlank() == true) error.message!! else "إنشاء الطلبات متاح فقط لحسابات الصيدليات"
            error.message?.contains("duplicate key", ignoreCase = true) == true ||
            error.message?.contains("already exists", ignoreCase = true) == true ->
                context.getString(R.string.request_error_duplicate)
            error.message?.contains("foreign key constraint", ignoreCase = true) == true ||
            error.message?.contains("warehouse", ignoreCase = true) == true ->
                context.getString(R.string.request_error_warehouse_invalid)
            error.message?.contains("network", ignoreCase = true) == true ||
            error.message?.contains("connection", ignoreCase = true) == true ->
                context.getString(R.string.error_network)
            error.message?.contains("permission", ignoreCase = true) == true ||
            error.message?.contains("unauthorized", ignoreCase = true) == true ->
                context.getString(R.string.error_permission)
            else -> error.message ?: context.getString(R.string.request_error_creation_failed)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearRequestSent() {
        _uiState.value = _uiState.value.copy(
            isSuccess = false,
            createdRequestId = ""
        )
    }

    fun resetState() {
        val currentMedicines = _uiState.value.medicines
        val currentWarehouses = _uiState.value.warehouses
        _uiState.value = CreateRequestUiState(
            medicines = currentMedicines,
            warehouses = currentWarehouses
        )
    }

    private fun Warehouse.toWarehouseOption(): WarehouseOption = WarehouseOption(
        id = id,
        name = name,
        location = listOf(city, district).filter { it.isNotBlank() }.joinToString(" - "),
        statusLabel = when {
            inStockPercent > 70 -> "متوفر"
            inStockPercent > 30 -> "مخزون منخفض"
            else -> "غير متوفر"
        },
        deliveryLabel = estimatedDeliveryLabel,
        stockPercent = inStockPercent,
    )
}
