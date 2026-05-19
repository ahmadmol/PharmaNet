package com.pharmalink.feature.admin.ui.audit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AuditLog
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
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

    private val logId: String = savedStateHandle[NavArgs.LOG_ID] ?: ""

    private val _uiState = MutableStateFlow<AuditLogDetailUiState>(AuditLogDetailUiState.Loading)
    val uiState: StateFlow<AuditLogDetailUiState> = _uiState.asStateFlow()

    init {
        if (logId.isBlank()) {
            _uiState.value = AuditLogDetailUiState.Error("معرف السجل غير صالح")
        } else {
            loadLogDetail()
        }
    }

    fun onAction(action: AuditLogDetailAction) {
        when (action) {
            AuditLogDetailAction.OnRetryClicked -> loadLogDetail()
        }
    }

    private fun loadLogDetail() {
        if (logId.isBlank()) {
            _uiState.value = AuditLogDetailUiState.Error("معرف السجل غير صالح")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuditLogDetailUiState.Loading
            pharmaRepository.getAuditLogById(logId)
                .onSuccess { log ->
                    _uiState.value = AuditLogDetailUiState.Success(log.toUiModel())
                }
                .onFailure { error ->
                    _uiState.value = AuditLogDetailUiState.Error(error.message ?: "خطأ غير متوقع")
                }
        }
    }

    private fun AuditLog.toUiModel(): AuditLogDetailModel {
        val formatter = DateTimeFormatter.ofPattern(
            "d MMMM yyyy - hh:mm a",
            Locale.forLanguageTag("ar"),
        )
        val formattedDateTime = createdAt.atZone(ZoneId.systemDefault()).format(formatter)
        return AuditLogDetailModel(
            actionLabel = actionLabel,
            isSuccess = isSuccess,
            adminName = adminName,
            formattedDateTime = formattedDateTime,
            targetEntityName = targetEntityName,
            targetWarehouseName = targetWarehouseName,
            targetSku = targetSku,
            oldValue = oldValue,
            newValue = newValue,
            ipAddress = ipAddress,
            userAgent = userAgent,
            transactionId = transactionId,
        )
    }
}
