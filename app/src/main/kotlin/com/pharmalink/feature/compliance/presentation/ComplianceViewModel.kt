package com.pharmalink.feature.compliance.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.ComplianceOverview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ComplianceUiState(
    val screenState: ScreenState<ComplianceOverview> = ScreenState.Loading,
)

@HiltViewModel
class ComplianceViewModel @Inject constructor(
    private val repository: PharmaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComplianceUiState())
    val uiState: StateFlow<ComplianceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCompliance().collect { overview ->
                _uiState.value = ComplianceUiState(
                    screenState = if (overview.documents.isEmpty()) {
                        ScreenState.Empty
                    } else {
                        ScreenState.Success(overview)
                    },
                )
            }
        }
    }
}
