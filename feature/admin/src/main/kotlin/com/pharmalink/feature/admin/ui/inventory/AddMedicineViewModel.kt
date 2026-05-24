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

private const val TAG = "AddMedicineViewModel"
private const val MISSING_WAREHOUSE_SAFE_ERROR =
    "\u062a\u0639\u0630\u0631 \u062a\u062d\u062f\u064a\u062f \u0627\u0644\u0645\u0633\u062a\u0648\u062f\u0639"
private const val IMAGE_UPLOAD_SAFE_ERROR =
    "\u062a\u0639\u0630\u0631 \u0631\u0641\u0639 \u0635\u0648\u0631\u0629 \u0627\u0644\u062f\u0648\u0627\u0621. \u064a\u0631\u062c\u0649 \u0627\u0644\u0645\u062d\u0627\u0648\u0644\u0629 \u0644\u0627\u062d\u0642\u0627"
private const val ADD_MEDICINE_SAFE_ERROR =
    "\u062a\u0639\u0630\u0631 \u0625\u0636\u0627\u0641\u0629 \u0627\u0644\u062f\u0648\u0627\u0621. \u064a\u0631\u062c\u0649 \u0627\u0644\u0645\u062d\u0627\u0648\u0644\u0629 \u0644\u0627\u062d\u0642\u0627"
private const val INVALID_PRICE_SAFE_ERROR =
    "\u0627\u0644\u0633\u0639\u0631 \u063a\u064a\u0631 \u0635\u0627\u0644\u062d. \u0627\u062a\u0631\u0643\u0647 \u0641\u0627\u0631\u063a\u0627 \u0623\u0648 \u0623\u062f\u062e\u0644 \u0631\u0642\u0645\u0627 \u0635\u062d\u064a\u062d\u0627"

@HiltViewModel
class AddMedicineViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val warehouseId: String = savedStateHandle[NavArgs.WAREHOUSE_ID] ?: ""

    private val _state = MutableStateFlow(AddMedicineUiState())
    val state: StateFlow<AddMedicineUiState> = _state.asStateFlow()

    fun onNameChange(value: String) = _state.update { it.copy(name = value) }
    fun onBrandChange(value: String) = _state.update { it.copy(brand = value) }
    fun onStrengthChange(value: String) = _state.update { it.copy(strength = value) }
    fun onDescriptionChange(value: String) = _state.update { it.copy(description = value) }
    fun onSpecsChange(value: String) = _state.update { it.copy(specs = value) }
    fun onPriceChange(value: String) = _state.update { it.copy(price = value) }
    fun onStockQuantityChange(value: String) = _state.update { it.copy(stockQuantity = value) }
    fun onVisibilityChange(value: Boolean) = _state.update { it.copy(isVisible = value) }
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

            var imageUrl: String? = null
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

            val medicine = Medicine(
                id = "",
                name = currentState.name.trim(),
                brand = currentState.brand.trim(),
                strength = currentState.strength.trim(),
                price = optionalPrice ?: 0.0,
                stockQuantity = currentState.stockQuantity.toIntOrNull() ?: 0,
                imageUrl = imageUrl,
                priceAmount = optionalPrice,
                description = currentState.description.trim().takeIf { it.isNotBlank() },
                specs = currentState.specs.trim().takeIf { it.isNotBlank() }?.let(::JsonPrimitive),
                isVisible = currentState.isVisible,
                isActive = true,
            )

            pharmaRepository.addMedicine(medicine, warehouseId).fold(
                onSuccess = {
                    _state.update { it.copy(isUploading = false, isSuccess = true) }
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to add medicine for warehouseId=$warehouseId: ${error.message}", error)
                    _state.update {
                        it.copy(
                            isUploading = false,
                            errorMessage = ADD_MEDICINE_SAFE_ERROR,
                        )
                    }
                },
            )
        }
    }
}
