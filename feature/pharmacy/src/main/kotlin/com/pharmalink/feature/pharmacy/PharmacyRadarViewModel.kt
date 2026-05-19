package com.pharmalink.feature.pharmacy

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.location.FacilityLocationService
import com.pharmalink.data.repository.PharmaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PharmacyRadarViewModel @Inject constructor(
    private val repository: PharmaRepository,
    private val locationService: FacilityLocationService,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PharmacyRadarUiState())
    val uiState: StateFlow<PharmacyRadarUiState> = _uiState.asStateFlow()

    fun refreshNearbyOrders(radiusKm: Double = 10.0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = context.getString(R.string.pharmacy_dashboard_loading_location),
            )

            val registeredLocation = repository.getCurrentPharmacyFacilityLocation().getOrNull()
            val location = registeredLocation ?: locationService.getCurrentFacilityLocation().getOrElse { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = mapLocationErrorToMessage(error),
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                currentLocationName = location.areaName,
                errorMessage = context.getString(R.string.pharmacy_dashboard_loading_orders),
            )

            repository.getNearbyOrders(
                lat = location.latitude,
                lng = location.longitude,
                radius = radiusKm,
            ).fold(
                onSuccess = { orders ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        nearbyOrders = orders,
                        errorMessage = null,
                    )
                },
                onFailure = { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.message?.takeIf { it.isNotBlank() }
                            ?: context.getString(R.string.pharmacy_dashboard_error_server),
                    )
                },
            )
        }
    }

    private fun mapLocationErrorToMessage(error: Throwable): String {
        return when (error.message) {
            "LOCATION_PERMISSION_DENIED" -> context.getString(R.string.pharmacy_dashboard_error_permission_denied)
            "LOCATION_DISABLED" -> context.getString(R.string.pharmacy_dashboard_error_location_disabled)
            "LOCATION_UNAVAILABLE" -> context.getString(R.string.pharmacy_dashboard_error_location_unavailable)
            else -> context.getString(R.string.pharmacy_dashboard_error_location_unavailable)
        }
    }
}
