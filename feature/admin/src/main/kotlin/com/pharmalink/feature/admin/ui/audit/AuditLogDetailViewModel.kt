package com.pharmalink.feature.admin.ui.audit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AuditLog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AuditLogDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pharmaRepository: PharmaRepository,
) : ViewModel() {

    private val logId: String = checkNotNull(savedStateHandle[NavArgs.LOG_ID])

    private val _uiState = MutableStateFlow<AuditLogDetailUiState>(AuditLogDetailUiState.Loading)
    val uiState: StateFlow<AuditLogDetailUiState> = _uiState.asStateFlow()

    init {
        loadLogDetail()
    }

    fun retry() {
        loadLogDetail()
    }

    private fun loadLogDetail() {
        viewModelScope.launch {
            _uiState.value = AuditLogDetailUiState.Loading
            pharmaRepository.getAuditLogById(logId)
                .onSuccess { log ->
                    _uiState.value = AuditLogDetailUiState.Success(log)
                }
                .onFailure { error ->
                    _uiState.value = AuditLogDetailUiState.Error(error.message ?: "خطأ غير متوقع")
                }
        }
    }
}

sealed interface AuditLogDetailUiState {
    data object Loading : AuditLogDetailUiState
    data class Success(val log: AuditLog) : AuditLogDetailUiState
    data class Error(val message: String) : AuditLogDetailUiState
}
