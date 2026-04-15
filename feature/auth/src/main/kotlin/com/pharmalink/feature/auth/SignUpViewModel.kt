package com.pharmalink.feature.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.error.MissingPharmacyLinkageException
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.SignUpRequest
import com.pharmalink.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignUpUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val userId: String? = null,
    val emailConfirmationRequired: Boolean = false,
    // ط£آ¢ط¥â€œأ¢â‚¬آ¦ ط·آ·ط¢آ­ط·آ¸أ¢â‚¬ع‘ط·آ¸ط«â€ ط·آ¸أ¢â‚¬â€چ ط·آ·ط¢آ§ط·آ¸أ¢â‚¬â€چط·آ¸أ¢â€ڑآ¬ UI (ط·آ·ط¹آ¾ط·آ¸ط¹ث†ط·آ·ط¢آ­ط·آ·ط¢آ¯ط·آ¸ط¹ع©ط·آ¸أ¢â‚¬ع©ط·آ·ط¢آ« ط·آ¸أ¢â‚¬آ¦ط·آ¸أ¢â‚¬آ  ط·آ·ط¢آ§ط·آ¸أ¢â‚¬â€چط·آ·ط¢آ´ط·آ·ط¢آ§ط·آ·ط¢آ´ط·آ·ط¢آ©)
    val phoneNumber: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullName: String = "",
    val accountType: AccountType = AccountType.PUBLIC_USER,
    val pharmacyName: String = "",
    val pharmacyLocation: String = "",
    val warehouseName: String = "",
    val warehouseLocation: String = ""
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "SignUpVM"
    }

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    // ط£آ¢ط¥â€œأ¢â‚¬آ¦ ط·آ·ط¹آ¾ط·آ·ط¢آ­ط·آ·ط¢آ¯ط·آ¸ط¸آ¹ط·آ·ط¢آ« ط·آ·ط¢آ­ط·آ¸أ¢â‚¬ع‘ط·آ¸ط«â€ ط·آ¸أ¢â‚¬â€چ ط·آ·ط¢آ§ط·آ¸أ¢â‚¬â€چط·آ¸أ¢â€ڑآ¬ UI ط·آ¸أ¢â‚¬آ¦ط·آ¸أ¢â‚¬آ  ط·آ·ط¢آ§ط·آ¸أ¢â‚¬â€چط·آ·ط¢آ´ط·آ·ط¢آ§ط·آ·ط¢آ´ط·آ·ط¢آ©
    fun updatePhoneNumber(value: String) {
        _uiState.update { it.copy(phoneNumber = value) }
    }
    
    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value) }
    }
    
    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value) }
    }
    
    fun updateFullName(value: String) {
        _uiState.update { it.copy(fullName = value) }
    }
    
    fun updateAccountType(value: AccountType) {
        _uiState.update { it.copy(accountType = value) }
    }
    
    fun updatePharmacyName(value: String) {
        _uiState.update { it.copy(pharmacyName = value) }
    }
    
    fun updatePharmacyLocation(value: String) {
        _uiState.update { it.copy(pharmacyLocation = value) }
    }
    
    fun updateWarehouseName(value: String) {
        _uiState.update { it.copy(warehouseName = value) }
    }
    
    fun updateWarehouseLocation(value: String) {
        _uiState.update { it.copy(warehouseLocation = value) }
    }

    // ط£آ¢ط¥â€œأ¢â‚¬آ¦ ط·آ·ط¢آ§ط·آ¸أ¢â‚¬â€چط·آ·ط¢آ¯ط·آ·ط¢آ§ط·آ¸أ¢â‚¬â€چط·آ·ط¢آ© ط·آ·ط¢آ§ط·آ¸أ¢â‚¬â€چط·آ·ط¢آ±ط·آ·ط¢آ¦ط·آ¸ط¸آ¹ط·آ·ط¢آ³ط·آ¸ط¸آ¹ط·آ·ط¢آ© ط·آ¸أ¢â‚¬â€چط·آ¸أ¢â‚¬â€چط·آ·ط¹آ¾ط·آ·ط¢آ³ط·آ·ط¢آ¬ط·آ¸ط¸آ¹ط·آ¸أ¢â‚¬â€چ
    fun signUp() {
        Log.d(TAG, "=== SIGN UP BUTTON CLICKED ===")
        
        val currentState = _uiState.value
        Log.d(TAG, "Current state:")
        Log.d(TAG, "  - fullName: '${currentState.fullName}'")
        Log.d(TAG, "  - phoneNumber: '${currentState.phoneNumber}'")
        Log.d(TAG, "  - password: '${currentState.password}' (length: ${currentState.password.length})")
        Log.d(TAG, "  - confirmPassword: '${currentState.confirmPassword}' (length: ${currentState.confirmPassword.length})")
        Log.d(TAG, "  - accountType: ${currentState.accountType}")
        Log.d(TAG, "  - pharmacyName: '${currentState.pharmacyName}'")
        Log.d(TAG, "  - warehouseName: '${currentState.warehouseName}'")
        Log.d(TAG, "  - isLoading: ${currentState.isLoading}")
        Log.d(TAG, "  - isSuccess: ${currentState.isSuccess}")
        Log.d(TAG, "  - errorMessage: ${currentState.errorMessage}")
        
        // ط£آ¢ط¥â€œأ¢â‚¬آ¦ ط·آ·ط¹آ¾ط·آ·ط¢آ­ط·آ¸أ¢â‚¬ع‘ط·آ¸أ¢â‚¬ع‘ ط·آ¸أ¢â‚¬آ¦ط·آ·ط¢آ³ط·آ·ط¢آ¨ط·آ¸أ¢â‚¬ع‘ ط·آ¸أ¢â‚¬ع‘ط·آ·ط¢آ¨ط·آ¸أ¢â‚¬â€چ ط·آ·ط¢آ§ط·آ¸أ¢â‚¬â€چط·آ·ط¢آ¥ط·آ·ط¢آ±ط·آ·ط¢آ³ط·آ·ط¢آ§ط·آ¸أ¢â‚¬â€چ
        Log.d(TAG, "Starting validation...")
        val validationError = validateSignUp(currentState)
        if (validationError != null) {
            Log.e(TAG, "ط£آ¢أ¢â‚¬إ’ط¥â€™ VALIDATION FAILED: $validationError")
            _uiState.update { it.copy(errorMessage = validationError, isLoading = false) }
            return
        }
        
        Log.d(TAG, "ط£آ¢ط¥â€œأ¢â‚¬آ¦ VALIDATION PASSED")

        viewModelScope.launch {
            Log.d(TAG, "Starting coroutine for sign up...")
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            Log.d(TAG, "State updated: isLoading = true")
            
            // ط£آ¢ط¥â€œأ¢â‚¬آ¦ ط·آ·ط¢آ¨ط·آ¸أ¢â‚¬آ ط·آ·ط¢آ§ط·آ·ط·إ’ ط·آ·ط¢آ·ط·آ¸أ¢â‚¬â€چط·آ·ط¢آ¨ ط·آ·ط¢آ§ط·آ¸أ¢â‚¬â€چط·آ·ط¹آ¾ط·آ·ط¢آ³ط·آ·ط¢آ¬ط·آ¸ط¸آ¹ط·آ¸أ¢â‚¬â€چ
            val request = SignUpRequest(
                accountType = currentState.accountType,
                fullName = currentState.fullName,
                phoneNumber = currentState.phoneNumber,
                password = currentState.password,
                confirmPassword = currentState.confirmPassword,
                pharmacyName = currentState.pharmacyName,
                pharmacyLocation = currentState.pharmacyLocation,
                warehouseName = currentState.warehouseName,
                warehouseLocation = currentState.warehouseLocation
            )
            
            Log.d(TAG, "Built SignUpRequest:")
            Log.d(TAG, "  - accountType: ${request.accountType}")
            Log.d(TAG, "  - fullName: '${request.fullName}'")
            Log.d(TAG, "  - phoneNumber: '${request.phoneNumber}'")
            Log.d(TAG, "  - password: '${request.password}' (length: ${request.password.length})")
            Log.d(TAG, "  - pharmacyName: '${request.pharmacyName}'")
            Log.d(TAG, "  - warehouseName: '${request.warehouseName}'")
            
            Log.d(TAG, "Calling authRepository.signUp()...")
            
            val signUpResult = authRepository.signUp(request)

            signUpResult.fold(
                onSuccess = { user ->
                    completeIdentityBootstrap(user)
                },
                onFailure = { error ->
                    Log.e("AUTH_DEBUG", "Signup failed: ${error.message}", error)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = false,
                            errorMessage = mapAuthErrorToUserMessage(error),
                        )
                    }
                },
            )
        }
    }

    private suspend fun completeIdentityBootstrap(user: User) {
        val bootstrapResult = authRepository.bootstrapAuthenticatedUser(user)
        bootstrapResult.fold(
            onSuccess = {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        userId = user.id,
                        emailConfirmationRequired = false,
                        errorMessage = null,
                    )
                }
            },
            onFailure = { error ->
                Log.e("AUTH_DEBUG", "Identity bootstrap failed after signup: ${error.message}", error)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = false,
                        errorMessage = mapBootstrapError(error),
                    )
                }
            },
        )
    }

    private fun validateSignUp(state: SignUpUiState): String? {
        Log.d(TAG, "=== VALIDATION DEBUG ===")
        Log.d(TAG, "Password length: ${state.password.length} (min: 6)")
        Log.d(TAG, "Password match: ${state.password == state.confirmPassword}")
        Log.d(TAG, "Phone number: '${state.phoneNumber}'")
        Log.d(TAG, "Phone is valid Syrian: ${com.pharmalink.core.common.validation.SyrianPhone.normalizeToE164Digits(state.phoneNumber) != null}")
        Log.d(TAG, "Full name blank: ${state.fullName.isBlank()}")
        Log.d(TAG, "Account type: ${state.accountType}")
        Log.d(TAG, "Pharmacy name blank: ${state.pharmacyName.isBlank()}")
        Log.d(TAG, "Warehouse name blank: ${state.warehouseName.isBlank()}")
        
        val result = when {
            state.password.length < 6 -> {
                Log.e(TAG, "ط£آ¢أ¢â‚¬إ’ط¥â€™ VALIDATION: Password too short")
                context.getString(R.string.auth_error_password_short)
            }
            state.password != state.confirmPassword -> {
                Log.e(TAG, "ط£آ¢أ¢â‚¬إ’ط¥â€™ VALIDATION: Passwords don't match")
                context.getString(R.string.auth_error_password_mismatch)
            }
            com.pharmalink.core.common.validation.SyrianPhone.normalizeToE164Digits(state.phoneNumber) == null -> {
                Log.e(TAG, "ط£آ¢أ¢â‚¬إ’ط¥â€™ VALIDATION: Invalid Syrian phone")
                context.getString(R.string.auth_error_invalid_phone)
            }
            state.fullName.isBlank() -> {
                Log.e(TAG, "ط£آ¢أ¢â‚¬إ’ط¥â€™ VALIDATION: Full name is blank")
                context.getString(R.string.auth_error_name_required)
            }
            state.accountType == AccountType.PHARMACY && state.pharmacyName.isBlank() -> {
                Log.e(TAG, "ط£آ¢أ¢â‚¬إ’ط¥â€™ VALIDATION: Pharmacy name is blank for PHARMACY account")
                context.getString(R.string.auth_error_pharmacy_name_required)
            }
            state.accountType == AccountType.WAREHOUSE && state.warehouseName.isBlank() -> {
                Log.e(TAG, "ط£آ¢أ¢â‚¬إ’ط¥â€™ VALIDATION: Warehouse name is blank for WAREHOUSE account")
                context.getString(R.string.auth_error_warehouse_name_required)
            }
            state.accountType == AccountType.WAREHOUSE && state.warehouseLocation.isBlank() -> {
                Log.e(TAG, "ط£آ¢أ¢â‚¬إ’ط¥â€™ VALIDATION: Warehouse location is blank for WAREHOUSE account")
                context.getString(R.string.auth_error_warehouse_location_required)
            }
            else -> {
                Log.d(TAG, "ط£آ¢ط¥â€œأ¢â‚¬آ¦ VALIDATION: All checks passed")
                null
            }
        }
        
        Log.d(TAG, "Validation result: ${result ?: "SUCCESS"}")
        return result
    }

    // ط£آ¢ط¥â€œأ¢â‚¬آ¦ ط·آ·ط¹آ¾ط·آ·ط¢آ­ط·آ¸ط«â€ ط·آ¸ط¸آ¹ط·آ¸أ¢â‚¬â€چ ط·آ·ط¢آ£ط·آ·ط¢آ®ط·آ·ط¢آ·ط·آ·ط¢آ§ط·آ·ط·إ’ ط·آ·ط¢آ§ط·آ¸أ¢â‚¬â€چط·آ¸أ¢â‚¬آ ط·آ·ط¢آ¸ط·آ·ط¢آ§ط·آ¸أ¢â‚¬آ¦ ط·آ·ط¢آ¥ط·آ¸أ¢â‚¬â€چط·آ¸أ¢â‚¬آ° ط·آ·ط¢آ±ط·آ·ط¢آ³ط·آ·ط¢آ§ط·آ·ط¢آ¦ط·آ¸أ¢â‚¬â€چ ط·آ¸أ¢â‚¬آ¦ط·آ·ط¢آ³ط·آ·ط¹آ¾ط·آ·ط¢آ®ط·آ·ط¢آ¯ط·آ¸أ¢â‚¬آ¦ ط·آ¸ط«â€ ط·آ·ط¢آ§ط·آ·ط¢آ¶ط·آ·ط¢آ­ط·آ·ط¢آ©
    private fun mapAuthErrorToUserMessage(error: Throwable): String {
        val backendMessage = error.message.orEmpty()
        return when {
            backendMessage.contains("User already registered", ignoreCase = true) ||
                backendMessage.contains("already registered", ignoreCase = true) ->
                context.getString(R.string.auth_error_phone_exists)
            backendMessage.contains("Email not confirmed", ignoreCase = true) ||
                backendMessage.contains("not confirmed", ignoreCase = true) ->
                context.getString(R.string.auth_error_email_not_confirmed)
            backendMessage.contains("Password should be at least", ignoreCase = true) ||
                backendMessage.contains("weak password", ignoreCase = true) ->
                context.getString(R.string.auth_error_weak_password)
            backendMessage.contains("Network", ignoreCase = true) ||
                backendMessage.contains("Unable to resolve host", ignoreCase = true) ||
                backendMessage.contains("timeout", ignoreCase = true) ->
                context.getString(R.string.error_network)
            else -> context.getString(R.string.auth_error_signup_failed)
        }
    }

    private fun mapBootstrapError(error: Throwable): String =
        when (error) {
            is MissingPharmacyLinkageException -> error.message.orEmpty()
            else -> mapAuthErrorToUserMessage(error)
        }

    // ط£آ¢ط¥â€œأ¢â‚¬آ¦ ط·آ·ط¢آ¥ط·آ·ط¢آ¹ط·آ·ط¢آ§ط·آ·ط¢آ¯ط·آ·ط¢آ© ط·آ·ط¹آ¾ط·آ·ط¢آ¹ط·آ¸ط¸آ¹ط·آ¸ط¸آ¹ط·آ¸أ¢â‚¬آ  ط·آ·ط¢آ§ط·آ¸أ¢â‚¬â€چط·آ·ط¢آ­ط·آ·ط¢آ§ط·آ¸أ¢â‚¬â€چط·آ·ط¢آ© (ط·آ·ط¢آ¹ط·آ¸أ¢â‚¬آ ط·آ·ط¢آ¯ ط·آ·ط¢آ§ط·آ¸أ¢â‚¬â€چط·آ·ط¢آ®ط·آ·ط¢آ±ط·آ¸ط«â€ ط·آ·ط¢آ¬ ط·آ¸أ¢â‚¬آ¦ط·آ¸أ¢â‚¬آ  ط·آ·ط¢آ§ط·آ¸أ¢â‚¬â€چط·آ·ط¢آ´ط·آ·ط¢آ§ط·آ·ط¢آ´ط·آ·ط¢آ©)
    fun resetState() {
        _uiState.value = SignUpUiState()
    }
}
