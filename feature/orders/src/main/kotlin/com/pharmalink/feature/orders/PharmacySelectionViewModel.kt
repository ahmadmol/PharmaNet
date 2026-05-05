package com.pharmalink.feature.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.feature.orders.usecase.GetPublicPharmaciesForMedicineUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PharmacySelectionViewModel @Inject constructor(
    private val getPublicPharmaciesForMedicineUseCase: GetPublicPharmaciesForMedicineUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PharmacySelectionUiState())
    val uiState: StateFlow<PharmacySelectionUiState> = _uiState.asStateFlow()

    private var lastMedicineId: String? = null

    fun initialize(
        medicineId: String,
        medicineName: String,
        medicineBrand: String,
        medicineStrength: String,
    ) {
        if (lastMedicineId == medicineId && _uiState.value.medicine.name == medicineName) {
            return
        }
        lastMedicineId = medicineId
        _uiState.update {
            it.copy(
                medicine = MedicineSummaryUi(
                    id = medicineId,
                    name = medicineName,
                    brand = medicineBrand,
                    strength = medicineStrength,
                ),
            )
        }
        loadPharmacies(medicineId)
    }

    fun retry() {
        loadPharmacies(lastMedicineId.orEmpty())
    }

    fun selectFilter(filter: PharmacySelectionFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    private fun loadPharmacies(medicineId: String) {
        if (medicineId.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    pharmacies = emptyList(),
                    errorMessage = "missing_medicine",
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getPublicPharmaciesForMedicineUseCase(medicineId).fold(
                onSuccess = { pharmacies ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pharmacies = pharmacies.map { pharmacy -> pharmacy.toPharmacySelectionItemUi() },
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pharmacies = emptyList(),
                            errorMessage = error.message ?: "load_failed",
                        )
                    }
                },
            )
        }
    }
}

data class PharmacySelectionUiState(
    val medicine: MedicineSummaryUi = MedicineSummaryUi(),
    val pharmacies: List<PharmacySelectionItemUi> = emptyList(),
    val selectedFilter: PharmacySelectionFilter = PharmacySelectionFilter.NEARBY,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    val visiblePharmacies: List<PharmacySelectionItemUi>
        get() = when (selectedFilter) {
            PharmacySelectionFilter.NEARBY -> pharmacies.filter { !it.distanceLabel.isNullOrBlank() }.ifEmpty { pharmacies }
            PharmacySelectionFilter.ON_DUTY -> pharmacies.filter { it.isOnDuty }
            PharmacySelectionFilter.ALL -> pharmacies
        }
}
