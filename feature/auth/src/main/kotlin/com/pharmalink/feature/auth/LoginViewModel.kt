package com.pharmalink.feature.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.error.MissingPharmacyLinkageException
import com.pharmalink.core.common.validation.SyrianPhone
import com.pharmalink.core.network.notifications.FcmTokenManager
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.domain.model.LoginRequest
import com.pharmalink.domain.model.LoginUiState
import com.pharmalink.domain.model.User
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
    private val fcmTokenManager: FcmTokenManager,
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
                onSuccess = { user ->
                    completeIdentityBootstrap(currentState, user)
                },
                onFailure = { exception ->
                    val backendMessage = exception.message.orEmpty()
                    val userMessage = when {
                        backendMessage.contains("Invalid login credentials", ignoreCase = true) ||
                            backendMessage.contains("Invalid credentials", ignoreCase = true) ->
                            context.getString(R.string.auth_error_invalid_credentials)
                        backendMessage.contains("Network", ignoreCase = true) ||
                            backendMessage.contains("timeout", ignoreCase = true) ->
                            context.getString(R.string.error_network)
                        else -> exception.message ?: context.getString(R.string.error_unknown)
                    }
                    
                    currentState.copy(
                        isLoading = false,
                        isLoginSuccessful = false,
                        errorMessage = userMessage,
                    )
                },
            )
        }
    }

    fun loginWithBiometrics() {
        Log.d("LoginDebug", "Biometric login triggered - implementing native biometric prompt")
        // Note: Real implementation would use BiometricPrompt
    }

    private suspend fun completeIdentityBootstrap(
        currentState: LoginUiState,
        user: User,
    ): LoginUiState {
        val bootstrapResult = authRepository.bootstrapAuthenticatedUser(user)
        return bootstrapResult.fold(
            onSuccess = {
                fcmTokenManager.refreshToken(reason = "login_success")
                currentState.copy(
                    isLoading = false,
                    isLoginSuccessful = true,
                    errorMessage = null,
                )
            },
            onFailure = { error ->
                currentState.copy(
                    isLoading = false,
                    isLoginSuccessful = false,
                    errorMessage = mapBootstrapError(error),
                )
            },
        )
    }

    private fun mapBootstrapError(error: Throwable): String =
        when (error) {
            is MissingPharmacyLinkageException -> error.message.orEmpty()
            else -> error.message ?: context.getString(R.string.error_unknown)
        }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
