package com.pharmalink.feature.admin.ui.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AuditLog
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AdminAuditLogViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminAuditLogUiState())
    val state: StateFlow<AdminAuditLogUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<AdminAuditLogEffect>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val effect: SharedFlow<AdminAuditLogEffect> = _effect.asSharedFlow()

    init {
        loadLogs()
    }

    fun onAction(action: AdminAuditLogAction) {
        when (action) {
            AdminAuditLogAction.OnRetryClicked -> loadLogs()
            AdminAuditLogAction.OnFilterClicked -> applyFilter()
            AdminAuditLogAction.OnExportClicked -> exportLogs()
            is AdminAuditLogAction.OnLogClicked -> navigateToDetail(action.logId)
            is AdminAuditLogAction.OnStartDateSelected -> updateStartDate(action.date)
            is AdminAuditLogAction.OnEndDateSelected -> updateEndDate(action.date)
        }
    }

    private fun loadLogs() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, contentError = "") }

            pharmaRepository.adminGetAuditLogs()
                .onSuccess { logs ->
                    val grouped = groupLogsByDate(logs)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            logs = grouped,
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            contentError = AUDIT_LIST_ERROR_MESSAGE,
                        )
                    }
                }
        }
    }

    private fun applyFilter() {
        val startDate = _state.value.startDate
        val endDate = _state.value.endDate
        
        if (startDate == null && endDate == null) {
            // No filter applied, just reload all logs
            loadLogs()
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, contentError = "") }

            pharmaRepository.adminGetAuditLogs()
                .onSuccess { logs ->
                    val filtered = logs.filter { log ->
                        val logDate = log.createdAt.atZone(ZoneId.systemDefault()).toLocalDate()
                        val afterStart = startDate?.let { logDate >= it } ?: true
                        val beforeEnd = endDate?.let { logDate <= it } ?: true
                        afterStart && beforeEnd
                    }
                    
                    val grouped = groupLogsByDate(filtered)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            logs = grouped,
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            contentError = AUDIT_LIST_ERROR_MESSAGE,
                        )
                    }
                }
        }
    }

    private fun exportLogs() {
        // Export is not yet implemented — no backend RPC available.
        // Button is disabled in UI; this path should not be reached.
        viewModelScope.launch {
            _effect.emit(AdminAuditLogEffect.ShowMessage("تصدير السجل غير متاح حالياً"))
        }
    }

    private fun navigateToDetail(logId: String) {
        viewModelScope.launch {
            _effect.emit(AdminAuditLogEffect.NavigateToDetail(logId))
        }
    }

    private fun updateStartDate(date: LocalDate) {
        _state.update { it.copy(startDate = date) }
    }

    private fun updateEndDate(date: LocalDate) {
        _state.update { it.copy(endDate = date) }
    }

    private fun groupLogsByDate(logs: List<AuditLog>): List<AuditLogGroup> {
        val now = Instant.now()
        val today = now.atZone(ZoneId.systemDefault()).toLocalDate()
        val yesterday = today.minusDays(1)

        val grouped = logs
            .sortedByDescending { it.createdAt }
            .groupBy { log ->
                val logDate = log.createdAt.atZone(ZoneId.systemDefault()).toLocalDate()
                when (logDate) {
                    today -> "اليوم"
                    yesterday -> "أمس"
                    else -> formatDate(log.createdAt)
                }
            }

        return grouped.map { (dateLabel, groupLogs) ->
            AuditLogGroup(
                dateLabel = dateLabel,
                logs = groupLogs.map { it.toUiModel(now) },
            )
        }
    }

    private fun AuditLog.toUiModel(now: Instant): AuditLogItemModel {
        val iconType = when {
            action.contains("CREATE", ignoreCase = true) -> AuditLogIconType.CREATE
            action.contains("DELETE", ignoreCase = true) -> AuditLogIconType.DELETE
            action.contains("SECURITY", ignoreCase = true) || action.contains("LOGIN", ignoreCase = true) -> AuditLogIconType.SECURITY
            else -> AuditLogIconType.UPDATE
        }

        val borderColor = when (iconType) {
            AuditLogIconType.CREATE -> AuditLogBorderColor.GREEN
            AuditLogIconType.UPDATE -> AuditLogBorderColor.BLUE
            AuditLogIconType.DELETE -> AuditLogBorderColor.RED
            AuditLogIconType.SECURITY -> AuditLogBorderColor.ORANGE
        }

        return AuditLogItemModel(
            id = id,
            iconType = iconType,
            actionTitle = actionLabel,
            description = targetEntityName,
            relativeTime = formatRelativeTime(createdAt, now),
            adminName = adminName,
            ipAddress = ipAddress,
            statusChip = if (isSuccess) "نجح" else "فشل",
            exactTimestamp = formatExactTime(createdAt),
            borderColor = borderColor,
        )
    }

    private fun formatRelativeTime(instant: Instant, now: Instant): String {
        val minutes = ChronoUnit.MINUTES.between(instant, now)
        val hours = ChronoUnit.HOURS.between(instant, now)
        val days = ChronoUnit.DAYS.between(instant, now)

        return when {
            minutes < 1 -> "الآن"
            minutes < 60 -> "منذ $minutes دقيقة"
            hours < 24 -> "منذ $hours ساعة"
            days == 1L -> "أمس"
            days < 7 -> "منذ $days أيام"
            else -> formatDate(instant)
        }
    }

    private fun formatExactTime(instant: Instant): String {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.forLanguageTag("ar"))
        return instant.atZone(ZoneId.systemDefault()).format(formatter)
    }

    private fun formatDate(instant: Instant): String {
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ar"))
        return instant.atZone(ZoneId.systemDefault()).format(formatter)
    }

    private companion object {
        private const val AUDIT_LIST_ERROR_MESSAGE = "تعذر تحميل السجل. حاول مرة أخرى."
    }
}
