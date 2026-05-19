package com.pharmalink.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.ComplianceDocumentStatus
import com.pharmalink.domain.model.ComplianceOverview
import com.pharmalink.domain.model.PharmacyProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ProfileContent(
    val profile: PharmacyProfile,
    val compliance: ComplianceOverview,
    val unreadNotifications: Int,
    val complianceAlertsCount: Int,
    val documentsNeedingAttentionCount: Int,
)

data class ProfileUiState(
    val isUpdatingNotifications: Boolean = false,
    val notificationsUpdateError: String? = null,
    val screenState: ScreenState<ProfileContent> = ScreenState.Loading,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: PharmaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeProfile(),
                repository.observeCompliance(),
                repository.observeNotifications(),
            ) { profile, compliance, notifications ->
                ProfileUiState(
                    isUpdatingNotifications = _uiState.value.isUpdatingNotifications,
                    notificationsUpdateError = _uiState.value.notificationsUpdateError,
                    screenState = ScreenState.Success(
                        ProfileContent(
                            profile = profile,
                            compliance = compliance,
                            unreadNotifications = if (profile.notificationsEnabled) {
                                notifications.count { !it.read }
                            } else {
                                0
                            },
                            complianceAlertsCount = compliance.alerts.size,
                            documentsNeedingAttentionCount = compliance.documents.count { document ->
                                document.status != ComplianceDocumentStatus.VALID
                            },
                        ),
                    ),
                )
            }
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        screenState = ScreenState.Error(
                            error.message ?: "تعذر تحميل الملف الشخصي حاليًا.",
                        ),
                    )
                }
                .collect { nextState ->
                    _uiState.value = nextState
                }
        }
    }

    fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdatingNotifications = true,
                notificationsUpdateError = null,
            )
            val result = repository.updateNotificationsPreference(enabled)
            _uiState.value = _uiState.value.copy(
                isUpdatingNotifications = false,
                notificationsUpdateError = result.exceptionOrNull()?.message,
            )
        }
    }
}
