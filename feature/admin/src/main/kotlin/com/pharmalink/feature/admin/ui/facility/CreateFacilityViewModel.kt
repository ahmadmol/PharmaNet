package com.pharmalink.feature.admin.ui.facility

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.CreateFacilityRequest
import com.pharmalink.domain.model.FacilityType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CreateFacilityVM"

@HiltViewModel
class CreateFacilityViewModel @Inject constructor(
    private val repository: PharmaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateFacilityUiState())
    val uiState: StateFlow<CreateFacilityUiState> = _uiState.asStateFlow()

    fun onFacilityTypeChange(type: FacilityType) {
        _uiState.update { it.copy(facilityType = type) }
    }

    fun onNameChange(name: String) {
        _uiState.update {
            it.copy(
                name = name,
                nameError = null,
            )
        }
    }

    fun onAddressChange(address: String) {
        _uiState.update {
            it.copy(
                address = address,
                addressError = null,
            )
        }
    }

    fun onPhoneChange(phone: String) {
        _uiState.update {
            it.copy(
                phone = phone,
                phoneError = null,
            )
        }
    }

    fun onLicenseNumberChange(licenseNumber: String) {
        _uiState.update {
            it.copy(
                licenseNumber = licenseNumber,
                licenseError = null,
            )
        }
    }

    fun onMapPickerClick() {
        // This will be handled by the screen's navigation callback
        // The screen will navigate to map picker and receive coordinates back
        Log.d(TAG, "Map picker clicked - awaiting navigation to map picker screen")
    }

    fun onLocationSelected(latitude: Double, longitude: Double, address: String? = null) {
        _uiState.update {
            it.copy(
                latitude = latitude,
                longitude = longitude,
                address = address ?: it.address,
                addressError = null,
            )
        }
        Log.d(TAG, "Location selected: lat=$latitude, lng=$longitude, address=$address")
    }

    fun onActiveToggle(isActive: Boolean) {
        _uiState.update { it.copy(isActive = isActive) }
    }

    fun onCreateClick() {
        val state = _uiState.value

        // Validate all fields
        val validationErrors = validateFields(state)
        if (validationErrors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    nameError = validationErrors["name"],
                    addressError = validationErrors["address"],
                    phoneError = validationErrors["phone"],
                    licenseError = validationErrors["license"],
                    error = "يرجى تصحيح الأخطاء قبل المتابعة",
                )
            }
            return
        }

        // Check coordinates
        if (state.latitude == null || state.longitude == null) {
            _uiState.update {
                it.copy(error = "يرجى تحديد الموقع على الخريطة قبل المتابعة")
            }
            return
        }

        // Submit request
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }

            val request = CreateFacilityRequest(
                type = state.facilityType,
                name = state.name.trim(),
                address = state.address.trim(),
                phone = state.phone.trim(),
                licenseNumber = state.licenseNumber.trim(),
                latitude = state.latitude,
                longitude = state.longitude,
                isActive = state.isActive,
            )

            Log.d(TAG, "Creating facility: type=${request.type}, name=${request.name}")

            repository.createFacility(request)
                .onSuccess {
                    Log.d(TAG, "Facility created successfully")
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isSuccess = true,
                        )
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to create facility", error)
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = error.message ?: "حدث خطأ أثناء إنشاء المنشأة",
                        )
                    }
                }
        }
    }

    private fun validateFields(state: CreateFacilityUiState): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        // Name validation
        if (state.name.isBlank()) {
            errors["name"] = "اسم المنشأة مطلوب"
        } else if (state.name.length < 3) {
            errors["name"] = "اسم المنشأة يجب أن يكون 3 أحرف على الأقل"
        }

        // Address validation
        if (state.address.isBlank()) {
            errors["address"] = "العنوان مطلوب"
        } else if (state.address.length < 5) {
            errors["address"] = "العنوان يجب أن يكون 5 أحرف على الأقل"
        }

        // Phone validation (Saudi format: 05XXXXXXXX)
        if (state.phone.isBlank()) {
            errors["phone"] = "رقم التواصل مطلوب"
        } else if (!state.phone.matches(Regex("^05\\d{8}$"))) {
            errors["phone"] = "رقم الجوال يجب أن يبدأ بـ 05 ويتكون من 10 أرقام"
        }

        // License validation
        if (state.licenseNumber.isBlank()) {
            errors["license"] = "رقم الترخيص مطلوب"
        } else if (state.licenseNumber.length < 5) {
            errors["license"] = "رقم الترخيص يجب أن يكون 5 أحرف على الأقل"
        }

        return errors
    }
}

data class CreateFacilityUiState(
    val facilityType: FacilityType = FacilityType.PHARMACY,
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val licenseNumber: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isActive: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val nameError: String? = null,
    val addressError: String? = null,
    val phoneError: String? = null,
    val licenseError: String? = null,
)
