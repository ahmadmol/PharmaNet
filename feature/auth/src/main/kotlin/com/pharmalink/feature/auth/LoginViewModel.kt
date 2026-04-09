package com.pharmalink.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.validation.SyrianPhone
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.domain.model.LoginRequest
import com.pharmalink.domain.model.LoginUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Login ViewModel
 * Manages login screen state and business logic
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updatePhoneNumber(phoneNumber: String) {
        _uiState.value = _uiState.value.copy(
            phoneNumber = phoneNumber,
            errorMessage = null,
        )
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            errorMessage = null,
        )
    }

    fun login() {
        val currentState = _uiState.value

        if (currentState.phoneNumber.isBlank() || currentState.password.isBlank()) {
            _uiState.value = currentState.copy(
                errorMessage = context.getString(R.string.auth_error_fields_required),
            )
            return
        }

        if (!SyrianPhone.isValid(currentState.phoneNumber)) {
            _uiState.value = currentState.copy(
                errorMessage = context.getString(R.string.auth_error_phone_syrian),
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, errorMessage = null)

            val result = authRepository.login(
                LoginRequest(
                    phoneNumber = currentState.phoneNumber,
                    password = currentState.password,
                ),
            )

            _uiState.value = result.fold(
                onSuccess = {
                    currentState.copy(
                        isLoading = false,
                        isLoginSuccessful = true,
                        errorMessage = null,
                    )
                },
                onFailure = {
                    currentState.copy(
                        isLoading = false,
                        isLoginSuccessful = false,
                        errorMessage = context.getString(R.string.auth_error_login_failed),
                    )
                },
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
