package com.pharmalink.feature.request

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.error.MissingPharmacyLinkageException
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AuthSessionState
import com.pharmalink.domain.model.Request
import com.pharmalink.domain.model.RequestItem
import com.pharmalink.domain.model.RequestPriority
import com.pharmalink.domain.model.RequestStatus
import com.pharmalink.domain.mapper.toUserIdentity
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

private const val EMPTY_BASKET_ERROR = "\u064a\u0631\u062c\u0649 \u0625\u0636\u0627\u0641\u0629 \u062f\u0648\u0627\u0621 \u0648\u0627\u062d\u062f \u0639\u0644\u0649 \u0627\u0644\u0623\u0642\u0644 \u0625\u0644\u0649 \u0627\u0644\u0633\u0644\u0629"
private const val INVALID_BASKET_ERROR = "\u062a\u062d\u0642\u0642 \u0645\u0646 \u0643\u0645\u064a\u0627\u062a \u0648\u0648\u062d\u062f\u0627\u062a \u0639\u0646\u0627\u0635\u0631 \u0627\u0644\u0633\u0644\u0629"
private const val PHARMACY_ONLY_ERROR = "\u0625\u0646\u0634\u0627\u0621 \u0627\u0644\u0637\u0644\u0628\u0627\u062a \u0645\u062a\u0627\u062d \u0641\u0642\u0637 \u0644\u062d\u0633\u0627\u0628\u0627\u062a \u0627\u0644\u0635\u064a\u062f\u0644\u064a\u0627\u062a"
private const val MISSING_PHARMACY_PROFILE_ERROR = "\u0627\u0644\u0645\u0644\u0641 \u0627\u0644\u0634\u062e\u0635\u064a \u063a\u064a\u0631 \u0645\u0643\u062a\u0645\u0644. \u064a\u0631\u062c\u0649 \u062a\u0633\u062c\u064a\u0644 \u0627\u0644\u062f\u062e\u0648\u0644 \u0645\u0646 \u062c\u062f\u064a\u062f \u0644\u0625\u0639\u0627\u062f\u0629 \u0645\u0632\u0627\u0645\u0646\u0629 \u0628\u064a\u0627\u0646\u0627\u062a \u0627\u0644\u0635\u064a\u062f\u0644\u064a\u0629."
private const val MEDICINE_ID_MISSING_ERROR = "\u0645\u0639\u0631\u0641 \u0627\u0644\u062f\u0648\u0627\u0621 \u063a\u064a\u0631 \u0645\u062a\u0648\u0641\u0631. \u064a\u0631\u062c\u0649 \u0627\u062e\u062a\u064a\u0627\u0631 \u0627\u0644\u062f\u0648\u0627\u0621 \u0645\u0631\u0629 \u0623\u062e\u0631\u0649."
private const val WAREHOUSE_UNCLEAR_ERROR =
    "\u0627\u0644\u0645\u0633\u062A\u0648\u062F\u0639 \u063A\u064A\u0631 \u0645\u062D\u062F\u062F \u0628\u0648\u0636\u0648\u062D. \u064A\u0631\u062C\u0649 \u0627\u062E\u062A\u064A\u0627\u0631 \u0627\u0644\u0645\u0633\u062A\u0648\u062F\u0639 \u0645\u0631\u0629 \u0623\u062E\u0631\u0649."
private const val BASKET_WAREHOUSE_MISMATCH_ERROR =
    "\u0627\u0644\u0633\u0644\u0629 \u062A\u062D\u062A\u0648\u064A \u0645\u0646\u062A\u062C\u0627\u062A \u0645\u0646 \u0645\u0633\u062A\u0648\u062F\u0639 \u0645\u062E\u062A\u0644\u0641. \u064A\u0631\u062C\u0649 \u0627\u0633\u062A\u062E\u062F\u0627\u0645 \u0645\u0633\u062A\u0648\u062F\u0639 \u0648\u0627\u062D\u062F \u0644\u0643\u0644 \u0637\u0644\u0628."

@HiltViewModel
class CreateRequestViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val pharmaRepository: PharmaRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val preselectedWarehouseId: String = savedStateHandle[NavArgs.WAREHOUSE_ID] ?: ""
    private val preselectedMedicineId: String = savedStateHandle[NavArgs.MEDICINE_ID] ?: ""
    private val preselectedMedicineName: String = savedStateHandle["medicineName"] ?: ""
    private val preselectedMedicineSubtitle: String = savedStateHandle["medicineSubtitle"] ?: ""
    private val preselectedUnit: String = savedStateHandle["unit"] ?: ""

    private val _uiState = MutableStateFlow(CreateRequestUiState())
    val uiState: StateFlow<CreateRequestUiState> = _uiState.asStateFlow()
    private var currentUserSnapshot: UserSnapshot? = null

    init {
        observeCurrentUser()
        applyCatalogPreselectIfPresent()
        loadAvailableMedicines()
        loadAvailableWarehouses()
    }

    private fun applyCatalogPreselectIfPresent() {
        val warehouseId = preselectedWarehouseId.normalizedId()
        if (warehouseId.isBlank() || preselectedMedicineId.isBlank() || preselectedMedicineName.isBlank()) {
            return
        }

        val unit = preselectedUnit.trim().ifBlank {
            preselectedMedicineSubtitle.trim().ifBlank { "\u0639\u0644\u0628\u0629" }
        }
        val item = CreateRequestBasketItem(
            medicineId = preselectedMedicineId,
            medicineName = preselectedMedicineName.trim(),
            medicineSubtitle = preselectedMedicineSubtitle.trim(),
            quantity = 1,
            unit = unit,
            warehouseId = warehouseId,
        )

        _uiState.value = _uiState.value.copy(
            selectedWarehouseId = warehouseId,
            items = listOf(item),
            selectedMedicine = null,
            quantity = "1",
            pendingUnit = "",
            errorMessage = null,
        )
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
                    val state = _uiState.value
                    val currentWarehouseId = state.selectedWarehouseId.normalizedId()
                    val currentSelection = warehouseOptions.firstOrNull { it.id == currentWarehouseId }
                    val defaultWarehouse = warehouseOptions.singleOrNull()
                    val resolvedWarehouseId = currentSelection?.id
                        ?: currentWarehouseId.ifBlank { defaultWarehouse?.id.orEmpty() }
                    val resolvedWarehouseName = currentSelection?.name
                        ?: state.selectedWarehouseName.ifBlank { defaultWarehouse?.name.orEmpty() }
                    _uiState.value = state.copy(
                        warehouses = warehouseOptions,
                        selectedWarehouseId = resolvedWarehouseId,
                        selectedWarehouseName = resolvedWarehouseName,
                        items = state.items.withWarehouseName(
                            warehouseId = resolvedWarehouseId,
                            warehouseName = resolvedWarehouseName,
                        ),
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
        val state = _uiState.value
        val medicineWarehouseId = medicine.warehouseId.normalizedId()
        val selectedWarehouseId = state.selectedWarehouseId.normalizedId()
        val basketWarehouseId = state.items.firstNotNullOfOrNull { item ->
            item.warehouseId.normalizedId().takeIf { it.isNotBlank() }
        }
        val resolvedWarehouseId = selectedWarehouseId.ifBlank { medicineWarehouseId }
        if (
            state.items.isNotEmpty() &&
            medicineWarehouseId.isNotBlank() &&
            basketWarehouseId != null &&
            medicineWarehouseId != basketWarehouseId
        ) {
            _uiState.value = state.copy(errorMessage = BASKET_WAREHOUSE_MISMATCH_ERROR)
            return
        }
        if (
            state.items.isNotEmpty() &&
            medicineWarehouseId.isNotBlank() &&
            selectedWarehouseId.isNotBlank() &&
            medicineWarehouseId != selectedWarehouseId
        ) {
            _uiState.value = state.copy(errorMessage = BASKET_WAREHOUSE_MISMATCH_ERROR)
            return
        }

        val resolvedWarehouseName = if (resolvedWarehouseId != selectedWarehouseId) {
            state.warehouses.firstOrNull { it.id == resolvedWarehouseId }?.name.orEmpty()
        } else {
            state.selectedWarehouseName
        }

        _uiState.value = state.copy(
            selectedMedicine = medicine,
            selectedWarehouseId = resolvedWarehouseId,
            selectedWarehouseName = resolvedWarehouseName.ifBlank { state.selectedWarehouseName },
            errorMessage = null
        )
    }

    fun onQuantityChange(newQuantity: String) {
        _uiState.value = _uiState.value.copy(
            quantity = newQuantity,
            errorMessage = null
        )
    }

    fun onPendingUnitChange(unit: String) {
        _uiState.value = _uiState.value.copy(
            pendingUnit = unit,
            errorMessage = null,
        )
    }

    fun addSelectedItemToBasket() {
        val state = _uiState.value
        val medicine = state.selectedMedicine
        if (medicine == null) {
            _uiState.value = state.copy(errorMessage = context.getString(R.string.request_error_medicine_required))
            return
        }
        if (medicine.id.isBlank()) {
            _uiState.value = state.copy(errorMessage = MEDICINE_ID_MISSING_ERROR)
            return
        }

        val quantity = state.quantity.toIntOrNull()
        if (quantity == null || quantity !in 1..1000) {
            _uiState.value = state.copy(errorMessage = context.getString(R.string.request_error_invalid_quantity))
            return
        }

        val unit = resolveUnit(
            pendingUnit = state.pendingUnit,
            fallbackUnit = medicine.strength,
        )
        if (unit.isBlank()) {
            _uiState.value = state.copy(errorMessage = INVALID_BASKET_ERROR)
            return
        }
        val medicineWarehouseId = medicine.warehouseId.normalizedId()
        val selectedWarehouseId = state.selectedWarehouseId.normalizedId().ifBlank { medicineWarehouseId }
        if (selectedWarehouseId.isBlank()) {
            _uiState.value = state.copy(errorMessage = WAREHOUSE_UNCLEAR_ERROR)
            return
        }
        if (medicineWarehouseId.isNotBlank() && medicineWarehouseId != selectedWarehouseId) {
            _uiState.value = state.copy(errorMessage = BASKET_WAREHOUSE_MISMATCH_ERROR)
            return
        }
        if (!state.items.matchWarehouse(selectedWarehouseId)) {
            _uiState.value = state.copy(errorMessage = BASKET_WAREHOUSE_MISMATCH_ERROR)
            return
        }
        val selectedWarehouseName = resolveWarehouseName(
            state = state,
            warehouseId = selectedWarehouseId,
        )

        val basketItem = CreateRequestBasketItem(
            medicineId = medicine.id,
            medicineName = medicine.name.trim(),
            medicineSubtitle = medicine.brand.trim(),
            quantity = quantity,
            unit = unit,
            warehouseId = selectedWarehouseId,
            warehouseName = selectedWarehouseName,
        )
        val updatedItems = state.items
            .filterNot { it.medicineId == basketItem.medicineId } + basketItem

        _uiState.value = state.copy(
            items = updatedItems,
            selectedWarehouseId = selectedWarehouseId,
            selectedWarehouseName = selectedWarehouseName,
            selectedMedicine = null,
            quantity = "1",
            pendingUnit = "",
            errorMessage = null,
        )
    }

    fun editBasketItem(medicineId: String, quantity: Int, unit: String) {
        val state = _uiState.value
        if (quantity !in 1..1000 || unit.isBlank()) {
            _uiState.value = state.copy(errorMessage = INVALID_BASKET_ERROR)
            return
        }

        _uiState.value = state.copy(
            items = state.items.map { item ->
                if (item.medicineId == medicineId) {
                    item.copy(quantity = quantity, unit = unit.trim())
                } else {
                    item
                }
            },
            errorMessage = null,
        )
    }

    fun removeBasketItem(medicineId: String) {
        val state = _uiState.value
        _uiState.value = state.copy(
            items = state.items.filterNot { it.medicineId == medicineId },
            errorMessage = null,
        )
    }

    fun clearBasket() {
        _uiState.value = _uiState.value.copy(
            items = emptyList(),
            errorMessage = null,
        )
    }

    fun onNotesChange(newNotes: String) {
        _uiState.value = _uiState.value.copy(
            notes = newNotes,
            errorMessage = null
        )
    }

    fun onUrgencyToggle(isUrgent: Boolean) {
        _uiState.value = _uiState.value.copy(
            isUrgent = isUrgent,
            errorMessage = null
        )
    }

    fun onWarehouseSelected(warehouseId: String, warehouseName: String) {
        val state = _uiState.value
        val normalizedWarehouseId = warehouseId.normalizedId()
        if (normalizedWarehouseId.isBlank()) {
            _uiState.value = state.copy(errorMessage = WAREHOUSE_UNCLEAR_ERROR)
            return
        }
        val basketWarehouseId = state.items.firstNotNullOfOrNull { item ->
            item.warehouseId.normalizedId().takeIf { it.isNotBlank() }
        }
        if (basketWarehouseId != null && basketWarehouseId != normalizedWarehouseId) {
            _uiState.value = state.copy(errorMessage = BASKET_WAREHOUSE_MISMATCH_ERROR)
            return
        }
        val normalizedWarehouseName = warehouseName.trim()
        _uiState.value = state.copy(
            selectedWarehouseId = normalizedWarehouseId,
            selectedWarehouseName = normalizedWarehouseName,
            items = state.items.withWarehouseName(
                warehouseId = normalizedWarehouseId,
                warehouseName = normalizedWarehouseName,
            ),
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

        val basketItems = resolveBasketItems(currentState)

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
                val userIdentity = userSnapshot.toUserIdentity()
                // Phase 3: role-native-first org resolution is centralized in UserIdentityMapper.
                val organizationId = userIdentity.organizationId.orEmpty()
                if (organizationId.isBlank()) {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        errorMessage = "الملف الشخصي غير مكتمل. يرجى تسجيل الدخول من جديد لإعادة مزامنة بيانات الصيدلية.",
                    )
                    return@launch
                }
                
                val selectedWarehouseId = currentState.selectedWarehouseId.normalizedId()
                if (selectedWarehouseId.isBlank()) {
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        errorMessage = WAREHOUSE_UNCLEAR_ERROR,
                    )
                    return@launch
                }
                val selectedWarehouseName = resolveWarehouseName(
                    state = currentState,
                    warehouseId = selectedWarehouseId,
                )

                val firstItem = basketItems.first()
                val requestItems = basketItems.mapIndexed { index, item ->
                    RequestItem(
                        lineNo = index + 1,
                        medicineId = item.medicineId,
                        medicineName = item.medicineName,
                        medicineSubtitle = item.medicineSubtitle,
                        quantity = item.quantity,
                        unit = item.unit,
                    )
                }
                val basePrice = basketItems.sumOf { item ->
                    val medicinePrice = currentState.medicines
                        .firstOrNull { it.id == item.medicineId }
                        ?.price
                        ?: 0.0
                    medicinePrice * item.quantity
                }
                val urgentFee = if (currentState.isUrgent) 10000.0 else 0.0
                val totalPrice = basePrice + urgentFee
                val priority = if (currentState.isUrgent) RequestPriority.URGENT else RequestPriority.NORMAL

                val request = Request(
                    id = "",
                    pharmacyId = organizationId,
                    medicineId = firstItem.medicineId,
                    medicineName = firstItem.medicineName,
                    medicineSubtitle = firstItem.medicineSubtitle,
                    quantity = firstItem.quantity,
                    unit = firstItem.unit,
                    notes = currentState.notes.trim(),
                    priority = priority,
                    totalPrice = totalPrice,
                    status = RequestStatus.DRAFT,
                    warehouseId = selectedWarehouseId,
                    warehouseName = selectedWarehouseName,
                    supplierName = selectedWarehouseName,
                    createdAtLabel = "",
                    updatedAtLabel = "",
                    items = requestItems,
                )

                pharmaRepository.createRequest(request).fold(
                    onSuccess = { createdRequest ->
                        pharmaRepository.submitPharmacyRequest(createdRequest.id).fold(
                            onSuccess = { submittedRequest ->
                                _uiState.value = currentState.copy(
                                    items = emptyList(),
                                    isLoading = false,
                                    selectedMedicine = null,
                                    quantity = "1",
                                    pendingUnit = "",
                                    notes = "",
                                    isUrgent = false,
                                    isSuccess = true,
                                    createdRequestId = submittedRequest.id,
                                    errorMessage = null,
                                )
                            },
                            onFailure = { e ->
                                _uiState.value = currentState.copy(
                                    isLoading = false,
                                    errorMessage = mapSupabaseErrorToUserMessage(e),
                                )
                            },
                        )
                    },
                    onFailure = { e ->
                        _uiState.value = currentState.copy(
                            isLoading = false,
                            errorMessage = mapSupabaseErrorToUserMessage(e),
                        )
                    },
                )
                return@launch

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
        val selectedWarehouseId = state.selectedWarehouseId.normalizedId()
        return when {
            selectedWarehouseId.isBlank() ->
                WAREHOUSE_UNCLEAR_ERROR
            state.notes.length > 500 ->
                context.getString(R.string.request_error_notes_too_long)
            state.items.isNotEmpty() && state.items.any { item ->
                item.medicineId.isBlank() || item.medicineName.isBlank() ||
                    item.quantity !in 1..1000 || item.unit.isBlank()
            } -> INVALID_BASKET_ERROR
            state.items.isNotEmpty() && !state.items.matchWarehouse(selectedWarehouseId) ->
                BASKET_WAREHOUSE_MISMATCH_ERROR
            state.items.isNotEmpty() -> null
            state.selectedMedicine == null ->
                context.getString(R.string.request_error_medicine_required)
            state.selectedMedicine.id.isBlank() ->
                MEDICINE_ID_MISSING_ERROR
            state.selectedMedicine.warehouseId.normalizedId().let { medicineWarehouseId ->
                medicineWarehouseId.isNotBlank() && medicineWarehouseId != selectedWarehouseId
            } -> BASKET_WAREHOUSE_MISMATCH_ERROR
            state.quantity.isBlank() ->
                context.getString(R.string.request_error_quantity_required)
            state.quantity.toIntOrNull() == null ->
                context.getString(R.string.request_error_invalid_quantity_format)
            state.quantity.toIntOrNull()!! <= 0 ->
                context.getString(R.string.request_error_quantity_zero)
            state.quantity.toIntOrNull()!! > 1000 ->
                context.getString(R.string.request_error_quantity_too_large)
            resolveUnit(state.pendingUnit, state.selectedMedicine.strength).isBlank() ->
                INVALID_BASKET_ERROR
            else -> null
        }
    }

    private fun resolveBasketItems(state: CreateRequestUiState): List<CreateRequestBasketItem> {
        if (state.items.isNotEmpty()) return state.items

        val selectedMedicine = state.selectedMedicine ?: return emptyList()
        val quantity = state.quantity.toIntOrNull() ?: return emptyList()
        val unit = resolveUnit(
            pendingUnit = state.pendingUnit,
            fallbackUnit = selectedMedicine.strength,
        )
        if (selectedMedicine.id.isBlank() || quantity !in 1..1000 || unit.isBlank()) {
            return emptyList()
        }
        val warehouseId = state.selectedWarehouseId.normalizedId()
        val warehouseName = resolveWarehouseName(state, warehouseId)

        return listOf(
            CreateRequestBasketItem(
                medicineId = selectedMedicine.id,
                medicineName = selectedMedicine.name.trim(),
                medicineSubtitle = selectedMedicine.brand.trim(),
                quantity = quantity,
                unit = unit,
                warehouseId = warehouseId,
                warehouseName = warehouseName,
            ),
        )
    }

    private fun resolveUnit(pendingUnit: String, fallbackUnit: String): String =
        pendingUnit.trim().ifBlank { fallbackUnit.trim() }

    private fun resolveWarehouseName(state: CreateRequestUiState, warehouseId: String): String {
        val normalizedWarehouseId = warehouseId.normalizedId()
        return state.warehouses.firstOrNull { it.id == normalizedWarehouseId }?.name
            ?: state.selectedWarehouseName.trim().ifBlank {
                state.items.firstOrNull { it.warehouseId.normalizedId() == normalizedWarehouseId }
                    ?.warehouseName
                    .orEmpty()
            }
    }

    private fun List<CreateRequestBasketItem>.matchWarehouse(warehouseId: String): Boolean {
        val normalizedWarehouseId = warehouseId.normalizedId()
        if (normalizedWarehouseId.isBlank()) return false
        return all { item ->
            val itemWarehouseId = item.warehouseId.normalizedId()
            itemWarehouseId.isBlank() || itemWarehouseId == normalizedWarehouseId
        }
    }

    private fun List<CreateRequestBasketItem>.withWarehouseName(
        warehouseId: String,
        warehouseName: String,
    ): List<CreateRequestBasketItem> {
        val normalizedWarehouseId = warehouseId.normalizedId()
        val normalizedWarehouseName = warehouseName.trim()
        if (normalizedWarehouseId.isBlank()) return this
        return map { item ->
            val itemWarehouseId = item.warehouseId.normalizedId()
            if (itemWarehouseId.isBlank() || itemWarehouseId == normalizedWarehouseId) {
                item.copy(
                    warehouseId = normalizedWarehouseId,
                    warehouseName = item.warehouseName.ifBlank { normalizedWarehouseName },
                )
            } else {
                item
            }
        }
    }

    private fun String.normalizedId(): String = trim()
    
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

