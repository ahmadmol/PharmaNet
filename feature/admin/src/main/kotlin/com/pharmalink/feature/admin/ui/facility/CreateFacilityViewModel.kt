package com.pharmalink.feature.admin.ui.facility

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.validation.SyrianPhone
import com.pharmalink.core.location.FacilityLocationService
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.CreateFacilityRequest
import com.pharmalink.domain.model.FacilityType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CreateFacilityVM"
private const val CREATE_FACILITY_ERROR_MESSAGE =
    "\u062a\u0639\u0630\u0631 \u0625\u0646\u0634\u0627\u0621 \u0627\u0644\u0645\u0646\u0634\u0623\u0629. \u064a\u0631\u062c\u0649 \u0627\u0644\u0645\u062d\u0627\u0648\u0644\u0629 \u0644\u0627\u062d\u0642\u0627"

sealed interface CreateFacilityEffect {
    data class ShowSuccess(val message: String) : CreateFacilityEffect
    data class ShowError(val message: String) : CreateFacilityEffect
    data object NavigateBack : CreateFacilityEffect
}

@HiltViewModel
class CreateFacilityViewModel @Inject constructor(
    private val repository: PharmaRepository,
    private val locationService: FacilityLocationService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateFacilityUiState())
    val uiState: StateFlow<CreateFacilityUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<CreateFacilityEffect>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val effect: SharedFlow<CreateFacilityEffect> = _effect.asSharedFlow()

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

    fun requestCurrentLocation() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isResolvingLocation = true,
                    addressError = null,
                )
            }

            locationService.getCurrentFacilityLocation()
                .onSuccess { location ->
                    _uiState.update {
                        it.copy(
                            address = location.areaName,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            isResolvingLocation = false,
                            addressError = null,
                        )
                    }
                    Log.d(
                        TAG,
                        "Current location resolved: area=${location.areaName}, lat=${location.latitude}, lng=${location.longitude}",
                    )
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to resolve current location", error)
                    _uiState.update {
                        it.copy(
                            isResolvingLocation = false,
                        )
                    }
                    viewModelScope.launch {
                        _effect.emit(CreateFacilityEffect.ShowError(error.message ?: "تعذر تحديد موقع المنشأة حالياً"))
                    }
                }
        }
    }

    fun onLocationPermissionDenied() {
        viewModelScope.launch {
            _effect.emit(CreateFacilityEffect.ShowError("يرجى السماح بالوصول إلى الموقع لتحديد المنطقة تلقائياً"))
        }
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
                )
            }
            viewModelScope.launch {
                _effect.emit(CreateFacilityEffect.ShowError("يرجى تصحيح الأخطاء قبل المتابعة"))
            }
            return
        }

        // Check coordinates
        if (state.latitude == null || state.longitude == null) {
            viewModelScope.launch {
                _effect.emit(CreateFacilityEffect.ShowError("يرجى تحديد الموقع على الخريطة قبل المتابعة"))
            }
            return
        }

        // Submit request
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }

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
                        it.copy(isSubmitting = false)
                    }
                    val successMsg = when (state.facilityType) {
                        FacilityType.PHARMACY -> "تم إنشاء الصيدلية بنجاح"
                        FacilityType.WAREHOUSE -> "تم إنشاء المستودع بنجاح"
                    }
                    _effect.emit(CreateFacilityEffect.ShowSuccess(successMsg))
                    _effect.emit(CreateFacilityEffect.NavigateBack)
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to create facility", error)
                    _uiState.update {
                        it.copy(isSubmitting = false)
                    }
                    _effect.emit(CreateFacilityEffect.ShowError(CREATE_FACILITY_ERROR_MESSAGE))
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

        // Phone validation follows the same Syrian mobile rules used by auth flows.
        if (state.phone.isBlank()) {
            errors["phone"] = "رقم التواصل مطلوب"
        } else if (!SyrianPhone.isValid(state.phone)) {
            errors["phone"] = "رقم الجوال يجب أن يبدأ بـ 05 ويتكون من 10 أرقام"
        }

        if (state.phone.isNotBlank() && !SyrianPhone.isValid(state.phone)) {
            errors["phone"] = "أدخل رقم هاتف سوري صالح"
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
    val isResolvingLocation: Boolean = false,
    val isActive: Boolean = true,
    val isSubmitting: Boolean = false,
    val nameError: String? = null,
    val addressError: String? = null,
    val phoneError: String? = null,
    val licenseError: String? = null,
)
