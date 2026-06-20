package com.pharmalink.feature.admin.ui.inventory

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.Medicine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

private const val TAG = "EditMedicineViewModel"
private const val MISSING_WAREHOUSE_SAFE_ERROR = "تعذر تحديد المستودع"
private const val IMAGE_UPLOAD_SAFE_ERROR = "تعذر رفع صورة الدواء. يرجى المحاولة لاحقاً"
private const val UPDATE_MEDICINE_SAFE_ERROR = "تعذر تعديل الدواء. يرجى المحاولة لاحقاً"
private const val DELETE_MEDICINE_SAFE_ERROR = "تعذر حذف الدواء. يرجى المحاولة لاحقاً"
private const val INVALID_PRICE_SAFE_ERROR = "السعر غير صالح. اتركه فارغاً أو أدخل رقماً صحيحاً"

@HiltViewModel
class EditMedicineViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val warehouseId: String = savedStateHandle[NavArgs.WAREHOUSE_ID] ?: ""
    private val medicineId: String = savedStateHandle[NavArgs.MEDICINE_ID] ?: ""

    private val _state = MutableStateFlow(EditMedicineUiState())
    val state: StateFlow<EditMedicineUiState> = _state.asStateFlow()

    init {
        if (warehouseId.isBlank() || medicineId.isBlank()) {
            _state.update { it.copy(errorMessage = "معلومات الدواء ناقصة") }
        } else {
            loadMedicineData()
        }
    }

    private fun loadMedicineData() {
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true) }
            pharmaRepository.getWarehouseInventory(warehouseId).fold(
                onSuccess = { inventory ->
                    val item = inventory.firstOrNull { it.medicineId == medicineId }
                    if (item != null) {
                        _state.update {
                            it.copy(
                                name = item.medicineName,
                                description = item.description.orEmpty(),
                                price = item.priceAmount?.toString().orEmpty(),
                                stockQuantity = item.quantity.toString(),
                                isVisible = item.isVisible,
                                isActive = item.isActive,
                                existingImageUrl = item.medicineImageUrl,
                                isUploading = false
                            )
                        }
                    } else {
                        _state.update { it.copy(isUploading = false, errorMessage = "الدواء غير موجود في المخزون") }
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(isUploading = false, errorMessage = error.message) }
                }
            )
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value) }
    fun onBrandChange(value: String) = _state.update { it.copy(brand = value) }
    fun onStrengthChange(value: String) = _state.update { it.copy(strength = value) }
    fun onDescriptionChange(value: String) = _state.update { it.copy(description = value) }
    fun onSpecsChange(value: String) = _state.update { it.copy(specs = value) }
    fun onPriceChange(value: String) = _state.update { it.copy(price = value) }
    fun onStockQuantityChange(value: String) = _state.update { it.copy(stockQuantity = value) }
    fun onVisibilityChange(value: Boolean) = _state.update { it.copy(isVisible = value) }
    fun onActiveChange(value: Boolean) = _state.update { it.copy(isActive = value) }
    fun onImageSelected(uri: android.net.Uri?) = _state.update { it.copy(imageUri = uri) }

    fun submitMedicine() {
        if (warehouseId.isBlank()) {
            _state.update { it.copy(errorMessage = MISSING_WAREHOUSE_SAFE_ERROR) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isUploading = true, errorMessage = null) }
            val currentState = _state.value
            val optionalPrice = currentState.price.trim().takeIf { it.isNotBlank() }?.toDoubleOrNull()
            if (currentState.price.isNotBlank() && optionalPrice == null) {
                _state.update { it.copy(isUploading = false, errorMessage = INVALID_PRICE_SAFE_ERROR) }
                return@launch
            }

            var imageUrl = currentState.existingImageUrl
            currentState.imageUri?.let { uri ->
                pharmaRepository.uploadMedicineImage(uri).fold(
                    onSuccess = { imageUrl = it },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to upload medicine image", error)
                        _state.update {
                            it.copy(
                                isUploading = false,
                                errorMessage = IMAGE_UPLOAD_SAFE_ERROR,
                            )
                        }
                        return@launch
                    },
                )
            }

            pharmaRepository.updateMedicine(
                medicineId = medicineId,
                name = currentState.name.trim(),
                brand = currentState.brand.trim().takeIf { it.isNotBlank() },
                strength = currentState.strength.trim().takeIf { it.isNotBlank() },
                description = currentState.description.trim().takeIf { it.isNotBlank() },
                priceAmount = optionalPrice,
                stockQuantity = currentState.stockQuantity.toIntOrNull() ?: 0,
                imageUrl = imageUrl,
                isVisible = currentState.isVisible,
                isActive = currentState.isActive,
            ).fold(
                onSuccess = {
                    if (currentState.imageUri != null && !currentState.existingImageUrl.isNullOrBlank()) {
                        pharmaRepository.deleteMedicineImage(currentState.existingImageUrl)
                    }
                    _state.update { it.copy(isUploading = false, isSuccess = true) }
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to update medicine: ${error.message}", error)
                    _state.update {
                        it.copy(
                            isUploading = false,
                            errorMessage = UPDATE_MEDICINE_SAFE_ERROR,
                        )
                    }
                },
            )
        }
    }

    fun deleteMedicine() {
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, errorMessage = null) }
            val currentState = _state.value
            pharmaRepository.deleteMedicine(medicineId).fold(
                onSuccess = {
                    if (!currentState.existingImageUrl.isNullOrBlank()) {
                        pharmaRepository.deleteMedicineImage(currentState.existingImageUrl)
                    }
                    _state.update { it.copy(isDeleting = false, isDeleted = true) }
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to delete medicine: ${error.message}", error)
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = DELETE_MEDICINE_SAFE_ERROR,
                        )
                    }
                }
            )
        }
    }
}
