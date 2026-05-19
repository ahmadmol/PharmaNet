package com.pharmalink.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordsVisible: Boolean = false,
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun updateCurrentPassword(value: String) = updateField { copy(currentPassword = value) }

    fun updateNewPassword(value: String) = updateField { copy(newPassword = value) }

    fun updateConfirmPassword(value: String) = updateField { copy(confirmPassword = value) }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordsVisible = !it.passwordsVisible) }
    }

    fun changePassword() {
        val state = _uiState.value
        val validationError = validate(state)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError, successMessage = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, successMessage = null, errorMessage = null) }
            val result = authRepository.changePassword(
                currentPassword = state.currentPassword,
                newPassword = state.newPassword,
            )
            _uiState.update { current ->
                result.fold(
                    onSuccess = {
                        current.copy(
                            currentPassword = "",
                            newPassword = "",
                            confirmPassword = "",
                            isLoading = false,
                            successMessage = "تم تغيير كلمة المرور بنجاح.",
                            errorMessage = null,
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isLoading = false,
                            successMessage = null,
                            errorMessage = mapError(error),
                        )
                    },
                )
            }
        }
    }

    private fun updateField(transform: ChangePasswordUiState.() -> ChangePasswordUiState) {
        _uiState.update {
            it.transform().copy(successMessage = null, errorMessage = null)
        }
    }

    private fun validate(state: ChangePasswordUiState): String? =
        when {
            state.currentPassword.isBlank() -> "أدخل كلمة المرور الحالية."
            state.newPassword.length < 8 -> "يجب أن تحتوي كلمة المرور الجديدة على 8 أحرف على الأقل."
            state.newPassword != state.confirmPassword -> "كلمة المرور الجديدة وتأكيدها غير متطابقين."
            state.currentPassword == state.newPassword -> "يجب أن تكون كلمة المرور الجديدة مختلفة عن الحالية."
            else -> null
        }

    private fun mapError(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("Invalid login credentials", ignoreCase = true) ||
                message.contains("invalid credentials", ignoreCase = true) ->
                "كلمة المرور الحالية غير صحيحة."
            message.contains("No active session", ignoreCase = true) ||
                message.contains("session", ignoreCase = true) ->
                "انتهت الجلسة. سجل الدخول مرة أخرى ثم أعد المحاولة."
            message.contains("same_password", ignoreCase = true) ||
                message.contains("same password", ignoreCase = true) ->
                "يجب أن تكون كلمة المرور الجديدة مختلفة عن الحالية."
            message.contains("weak", ignoreCase = true) ->
                "كلمة المرور الجديدة ضعيفة. اختر كلمة أقوى."
            message.contains("network", ignoreCase = true) ||
                message.contains("timeout", ignoreCase = true) ||
                message.contains("Unable to resolve host", ignoreCase = true) ->
                "تعذر الاتصال بالشبكة. تحقق من الإنترنت ثم أعد المحاولة."
            else -> message.ifBlank { "تعذر تغيير كلمة المرور." }
        }
    }
}
