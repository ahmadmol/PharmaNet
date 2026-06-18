package com.pharmalink.feature.orders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.PublicPharmacyAvailabilityStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PublicPharmaciesViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublicPharmaciesUiState())
    val uiState: StateFlow<PublicPharmaciesUiState> = _uiState.asStateFlow()

    init {
        loadPharmacies()
    }

    fun loadPharmacies() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            pharmaRepository.getPublicPharmacies()
                .onSuccess { pharmacies ->
                    val pharmacyItems = pharmacies.map { pharmacy ->
                        PublicPharmacyItemUi(
                            pharmacyId = pharmacy.pharmacyId,
                            pharmacyName = pharmacy.pharmacyName,
                            locationLabel = listOfNotNull(
                                pharmacy.location,
                                pharmacy.area,
                                pharmacy.city,
                                pharmacy.district
                            ).filter { it.isNotBlank() }.joinToString(", "),
                            supportsDelivery = pharmacy.supportsDelivery,
                            supportsPickup = pharmacy.supportsPickup,
                            isOnDuty = pharmacy.isOnDuty,
                            availabilityStatus = pharmacy.availabilityStatus,
                            distanceLabel = pharmacy.distanceLabel,
                            estimatedTimeLabel = pharmacy.estimatedTimeLabel,
                        )
                    }
                    _uiState.update {
                        it.copy(
                            pharmacies = pharmacyItems,
                            isLoading = false,
                        )
                    }
                    applyFilter()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "فشل تحميل الصيدليات",
                        )
                    }
                }
        }
    }

    fun selectFilter(filter: PublicPharmacyFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        applyFilter()
    }

    private fun applyFilter() {
        val currentState = _uiState.value
        val filtered = when (currentState.selectedFilter) {
            PublicPharmacyFilter.ALL -> currentState.pharmacies
            PublicPharmacyFilter.ON_DUTY -> currentState.pharmacies.filter { it.isOnDuty }
            PublicPharmacyFilter.AVAILABLE -> currentState.pharmacies.filter {
                it.availabilityStatus == PublicPharmacyAvailabilityStatus.AVAILABLE
            }
            PublicPharmacyFilter.NEARBY -> currentState.pharmacies.sortedBy { 
                it.distanceLabel?.filter { char -> char.isDigit() }?.toIntOrNull() ?: Int.MAX_VALUE 
            }
        }
        _uiState.update { it.copy(visiblePharmacies = filtered) }
    }
}

data class PublicPharmaciesUiState(
    val pharmacies: List<PublicPharmacyItemUi> = emptyList(),
    val visiblePharmacies: List<PublicPharmacyItemUi> = emptyList(),
    val selectedFilter: PublicPharmacyFilter = PublicPharmacyFilter.ALL,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class PublicPharmacyItemUi(
    val pharmacyId: String,
    val pharmacyName: String,
    val locationLabel: String,
    val supportsDelivery: Boolean,
    val supportsPickup: Boolean,
    val isOnDuty: Boolean,
    val availabilityStatus: PublicPharmacyAvailabilityStatus,
    val distanceLabel: String?,
    val estimatedTimeLabel: String?,
)

enum class PublicPharmacyFilter {
    ALL,
    ON_DUTY,
    AVAILABLE,
    NEARBY,
}
