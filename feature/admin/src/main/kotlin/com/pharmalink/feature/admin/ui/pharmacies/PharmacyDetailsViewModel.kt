package com.pharmalink.feature.admin.ui.pharmacies

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.Pharmacy
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

@HiltViewModel
class PharmacyDetailsViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val pharmacyId: String = savedStateHandle[NavArgs.PHARMACY_ID] ?: ""

    private val _state = MutableStateFlow(PharmacyDetailsUiState())
    val state: StateFlow<PharmacyDetailsUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<PharmacyDetailsEffect>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val effect: SharedFlow<PharmacyDetailsEffect> = _effect.asSharedFlow()

    init {
        if (pharmacyId.isEmpty()) {
            _state.update { it.copy(isLoading = false, contentError = "معرف الصيدلية مفقود") }
        } else {
            loadPharmacyDetails()
        }
    }

    fun onAction(action: PharmacyDetailsAction) {
        when (action) {
            PharmacyDetailsAction.OnRetryClicked -> loadPharmacyDetails()
            PharmacyDetailsAction.OnManageBranchClicked -> {
                viewModelScope.launch {
                    _effect.emit(PharmacyDetailsEffect.ShowMessage("إدارة الفرع: قيد التطوير"))
                }
            }
            PharmacyDetailsAction.OnViewOrdersClicked -> {
                viewModelScope.launch {
                    _effect.emit(PharmacyDetailsEffect.ShowMessage("عرض الطلبات: قيد التطوير"))
                }
            }
            PharmacyDetailsAction.OnEditClicked -> {
                viewModelScope.launch {
                    _effect.emit(PharmacyDetailsEffect.ShowMessage("تعديل الصيدلية: قيد التطوير"))
                }
            }
        }
    }

    private fun loadPharmacyDetails() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, contentError = "") }

            pharmaRepository.adminGetAllPharmacies()
                .onSuccess { pharmacies ->
                    val pharmacy = pharmacies.find { it.id == pharmacyId }
                    if (pharmacy != null) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                pharmacy = pharmacy.toDetailModel(),
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                contentError = "الصيدلية غير موجودة",
                            )
                        }
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            contentError = e.message ?: "فشل تحميل بيانات الصيدلية",
                        )
                    }
                }
        }
    }

    private fun Pharmacy.toDetailModel(): PharmacyDetailModel {
        // Note: Secondary stats (totalEmployees, totalOrders, totalCustomers, averageRating)
        // are not included because endpoints are not available yet
        return PharmacyDetailModel(
            id = id,
            name = name,
            location = location ?: "",
            contactNumber = contactNumber ?: "",
            licenseNumber = licenseNumber ?: "",
            isActive = isActive,
            createdAt = createdAt,
        )
    }
}
