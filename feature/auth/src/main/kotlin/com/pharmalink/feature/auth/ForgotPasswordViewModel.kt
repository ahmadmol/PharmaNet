package com.pharmalink.feature.auth

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

data class ForgotPasswordUiState(
    val identifier: String = "",
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun updateIdentifier(value: String) {
        _uiState.update {
            it.copy(
                identifier = value,
                successMessage = null,
                errorMessage = null,
            )
        }
    }

    fun requestPasswordReset() {
        val identifier = _uiState.value.identifier.trim()
        if (identifier.isBlank()) {
            _uiState.update { it.copy(errorMessage = "ادخل البريد الإلكتروني المرتبط بالحساب أولا.") }
            return
        }

        if ("@" !in identifier) {
            _uiState.update {
                it.copy(errorMessage = "استعادة كلمة المرور متاحة عبر البريد الإلكتروني فقط في الإصدار الحالي.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, successMessage = null, errorMessage = null) }
            val result = authRepository.requestPasswordReset(identifier)
            _uiState.update { current ->
                result.fold(
                    onSuccess = {
                        current.copy(
                            isLoading = false,
                            successMessage = "تم إرسال تعليمات استعادة كلمة المرور إذا كان الحساب مرتبطا ببريد قابل للاستلام.",
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

    private fun mapError(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("synthetic-email", ignoreCase = true) ||
                message.contains("phone", ignoreCase = true) ->
                "استعادة كلمة المرور عبر رقم الهاتف غير متاحة حاليا مع نموذج البريد الداخلي. استخدم بريد الحساب أو تواصل مع الدعم."
            message.contains("valid email", ignoreCase = true) ||
                message.contains("Syrian phone", ignoreCase = true) ->
                "ادخل بريدا إلكترونيا صالحا مرتبطا بحسابك."
            message.contains("network", ignoreCase = true) ||
                message.contains("timeout", ignoreCase = true) ||
                message.contains("Unable to resolve host", ignoreCase = true) ->
                "تعذر الاتصال بالخادم. تحقق من الشبكة وحاول مرة أخرى."
            else -> message.ifBlank { "تعذر إرسال طلب الاستعادة الآن." }
        }
    }
}
