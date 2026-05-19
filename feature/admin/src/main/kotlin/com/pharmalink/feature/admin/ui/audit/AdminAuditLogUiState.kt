package com.pharmalink.feature.admin.ui.audit

import androidx.compose.runtime.Immutable
import com.pharmalink.domain.model.AuditLog
import java.time.LocalDate

@Immutable
data class AdminAuditLogUiState(
    val isLoading: Boolean = false,
    val contentError: String = "",
    val logs: List<AuditLogGroup> = emptyList(),
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
)

@Immutable
data class AuditLogGroup(
    val dateLabel: String,
    val logs: List<AuditLogItemModel>,
)

@Immutable
data class AuditLogItemModel(
    val id: String,
    val iconType: AuditLogIconType,
    val actionTitle: String,
    val description: String,
    val relativeTime: String,
    val statusChip: String,
    val exactTimestamp: String,
    val borderColor: AuditLogBorderColor,
)

enum class AuditLogIconType {
    CREATE,
    UPDATE,
    DELETE,
    SECURITY,
}

enum class AuditLogBorderColor {
    GREEN,
    BLUE,
    RED,
    ORANGE,
}

sealed interface AdminAuditLogAction {
    data object OnRetryClicked : AdminAuditLogAction
    data object OnFilterClicked : AdminAuditLogAction
    data object OnExportClicked : AdminAuditLogAction
    data class OnLogClicked(val logId: String) : AdminAuditLogAction
    data class OnStartDateSelected(val date: LocalDate) : AdminAuditLogAction
    data class OnEndDateSelected(val date: LocalDate) : AdminAuditLogAction
}

sealed interface AdminAuditLogEffect {
    data class NavigateToDetail(val logId: String) : AdminAuditLogEffect
    data class ShowMessage(val message: String) : AdminAuditLogEffect
}
