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
import kotlinx.coroutines.flow.collectLatest
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
                // No registered location and GPS failed — mark location missing
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null,
                    isLocationMissing = true,
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                currentLocationName = location.areaName,
                errorMessage = context.getString(R.string.pharmacy_dashboard_loading_orders),
                isLocationMissing = false,
            )

            repository.observeNearbyOrdersRealtime(
                lat = location.latitude,
                lng = location.longitude,
                radius = radiusKm,
            ).collectLatest { orders ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    nearbyOrders = orders,
                    errorMessage = null,
                )
            }
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

    private fun mapBackendErrorToMessage(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("missing organizationId", ignoreCase = true) ||
                message.contains("linked pharmacy", ignoreCase = true) ||
                message.contains("Linked pharmacy not found", ignoreCase = true) ->
                context.getString(R.string.pharmacy_dashboard_error_missing_pharmacy_link)
            message.contains("coordinates", ignoreCase = true) ||
                message.contains("latitude", ignoreCase = true) ||
                message.contains("longitude", ignoreCase = true) ->
                context.getString(R.string.pharmacy_dashboard_error_missing_pharmacy_coordinates)
            message.contains("permission", ignoreCase = true) ||
                message.contains("unauthorized", ignoreCase = true) ->
                context.getString(R.string.pharmacy_dashboard_error_permission_denied)
            else -> context.getString(R.string.pharmacy_dashboard_error_server)
        }
    }
}