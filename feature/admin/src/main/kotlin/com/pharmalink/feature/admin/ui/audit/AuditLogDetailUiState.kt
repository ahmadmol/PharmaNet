package com.pharmalink.feature.admin.ui.audit

import androidx.compose.runtime.Immutable

/**
 * UI model for the Audit Log Detail screen.
 * Maps from [com.pharmalink.domain.model.AuditLog] — no domain model exposed in UiState.
 */
@Immutable
data class AuditLogDetailModel(
    val actionLabel: String,
    val isSuccess: Boolean,
    val adminName: String,
    val formattedDateTime: String,
    val targetEntityName: String,
    val targetWarehouseName: String?,
    val targetSku: String?,
    val oldValue: String,
    val newValue: String,
    val ipAddress: String?,
    val userAgent: String?,
    val transactionId: String?,
)

sealed interface AuditLogDetailUiState {
    data object Loading : AuditLogDetailUiState
    data class Success(val log: AuditLogDetailModel) : AuditLogDetailUiState
    data class Error(val message: String) : AuditLogDetailUiState
}

sealed interface AuditLogDetailAction {
    data object OnRetryClicked : AuditLogDetailAction
}

// Effect is declared but currently unused — kept for future ShowMessage needs.
// If no effect is emitted, the SharedFlow is simply never collected.
// Removed: AuditLogDetailEffect — no effect is currently emitted by the VM.
