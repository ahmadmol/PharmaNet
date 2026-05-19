package com.pharmalink.feature.pharmacy

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class PharmacyDashboardViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PharmacyDashboardUiState())
    val uiState: StateFlow<PharmacyDashboardUiState> = _uiState.asStateFlow()
}
