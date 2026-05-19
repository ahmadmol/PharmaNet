package com.pharmalink.feature.admin.ui.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.Medicine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddMedicineViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val warehouseId: String = savedStateHandle[NavArgs.WAREHOUSE_ID] ?: ""

    private val _state = MutableStateFlow(AddMedicineUiState())
    val state: StateFlow<AddMedicineUiState> = _state.asStateFlow()

    fun onNameChange(value: String) = _state.update { it.copy(name = value) }
    fun onBrandChange(value: String) = _state.update { it.copy(brand = value) }
    fun onStrengthChange(value: String) = _state.update { it.copy(strength = value) }
    fun onPriceChange(value: String) = _state.update { it.copy(price = value) }
    fun onStockQuantityChange(value: String) = _state.update { it.copy(stockQuantity = value) }
    fun onImageSelected(uri: android.net.Uri?) = _state.update { it.copy(imageUri = uri) }

    fun submitMedicine() {
        if (warehouseId.isBlank()) {
            _state.update { it.copy(errorMessage = "معرف المستودع مفقود") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isUploading = true, errorMessage = null) }

            var imageUrl: String? = null
            _state.value.imageUri?.let { uri ->
                pharmaRepository.uploadMedicineImage(uri).fold(
                    onSuccess = { imageUrl = it },
                    onFailure = { e ->
                        _state.update { it.copy(isUploading = false, errorMessage = "فشل رفع الصورة: ${e.message}") }
                        return@launch
                    }
                )
            }

            val medicine = Medicine(
                id = "",
                name = _state.value.name,
                brand = _state.value.brand,
                strength = _state.value.strength,
                price = _state.value.price.toDoubleOrNull() ?: 0.0,
                stockQuantity = _state.value.stockQuantity.toIntOrNull() ?: 0,
                imageUrl = imageUrl
            )

            pharmaRepository.addMedicine(medicine, warehouseId).fold(
                onSuccess = {
                    _state.update { it.copy(isUploading = false, isSuccess = true) }
                },
                onFailure = { e ->
                    _state.update { it.copy(isUploading = false, errorMessage = "فشل إضافة الدواء: ${e.message}") }
                }
            )
        }
    }
}
