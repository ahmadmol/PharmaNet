package com.pharmalink.feature.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.common.error.MissingPharmacyLinkageException
import com.pharmalink.core.location.FacilityLocationService
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.SignUpRequest
import com.pharmalink.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignUpUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val userId: String? = null,
    val emailConfirmationRequired: Boolean = false,
    val requireManualLogin: Boolean = false,
    val successMessage: String? = null,
    val navigateToLogin: Boolean = false,
    val phoneNumber: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullName: String = "",
    val accountType: AccountType = AccountType.PUBLIC_USER,
    val pharmacyName: String = "",
    val pharmacyLocation: String = "",
    val warehouseName: String = "",
    val warehouseLocation: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isResolvingLocation: Boolean = false,
    val locationMessage: String? = null,
    val locationMessageIsError: Boolean = false,
    val locationSettingsAction: SignUpLocationSettingsAction? = null,
    val agreedToTerms: Boolean = false,
)

enum class SignUpLocationSettingsAction {
    APP_SETTINGS,
    LOCATION_SETTINGS,
}

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val locationService: FacilityLocationService,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    companion object {
        private const val TAG = "SignUpVM"
        private const val SENSITIVE_SIGNUP_REDIRECT_DELAY_MS = 1500L
    }

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

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

    fun updateAgreedToTerms(value: Boolean) {
        _uiState.update { it.copy(agreedToTerms = value) }
    }

    fun updateAccountType(value: AccountType) {
        _uiState.update { it.copy(accountType = value) }
    }

    fun updatePharmacyName(value: String) {
        _uiState.update { it.copy(pharmacyName = value) }
    }

    fun updatePharmacyLocation(value: String) {
        _uiState.update {
            it.copy(
                pharmacyLocation = value,
                latitude = if (it.accountType == AccountType.PHARMACY) null else it.latitude,
                longitude = if (it.accountType == AccountType.PHARMACY) null else it.longitude,
            )
        }
    }

    fun updateWarehouseName(value: String) {
        _uiState.update { it.copy(warehouseName = value) }
    }

    fun updateWarehouseLocation(value: String) {
        _uiState.update {
            it.copy(
                warehouseLocation = value,
                latitude = if (it.accountType == AccountType.WAREHOUSE) null else it.latitude,
                longitude = if (it.accountType == AccountType.WAREHOUSE) null else it.longitude,
            )
        }
    }

    fun onPickLocation(lat: Double, lng: Double, address: String?) {
        _uiState.update {
            it.copy(
                latitude = lat,
                longitude = lng,
                pharmacyLocation = if (it.accountType == AccountType.PHARMACY) {
                    address ?: it.pharmacyLocation
                } else {
                    it.pharmacyLocation
                },
                warehouseLocation = if (it.accountType == AccountType.WAREHOUSE) {
                    address ?: it.warehouseLocation
                } else {
                    it.warehouseLocation
                },
            )
        }
    }

    fun requestCurrentLocation() {
        val accountType = _uiState.value.accountType
        if (accountType != AccountType.WAREHOUSE && accountType != AccountType.PHARMACY) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isResolvingLocation = true,
                    latitude = null,
                    longitude = null,
                    pharmacyLocation = if (accountType == AccountType.PHARMACY) "" else it.pharmacyLocation,
                    warehouseLocation = if (accountType == AccountType.WAREHOUSE) "" else it.warehouseLocation,
                    locationMessage = null,
                    locationMessageIsError = false,
                    locationSettingsAction = null,
                )
            }

            locationService.getFreshFacilityLocation()
                .onSuccess { location ->
                    _uiState.update { state ->
                        state.copy(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            pharmacyLocation = if (state.accountType == AccountType.PHARMACY) {
                                location.areaName
                            } else {
                                state.pharmacyLocation
                            },
                            warehouseLocation = if (state.accountType == AccountType.WAREHOUSE) {
                                location.areaName
                            } else {
                                state.warehouseLocation
                            },
                            isResolvingLocation = false,
                            locationMessage = if (state.accountType == AccountType.WAREHOUSE) {
                                context.getString(R.string.location_picker_success_warehouse)
                            } else {
                                context.getString(R.string.location_picker_success_pharmacy)
                            },
                            locationMessageIsError = false,
                            locationSettingsAction = null,
                        )
                    }
                }
                .onFailure { error ->
                    val action = when (error.message) {
                        "LOCATION_PERMISSION_DENIED" -> SignUpLocationSettingsAction.APP_SETTINGS
                        "LOCATION_DISABLED" -> SignUpLocationSettingsAction.LOCATION_SETTINGS
                        else -> null
                    }
                    val message = when (error.message) {
                        "LOCATION_PERMISSION_DENIED" -> context.getString(R.string.location_error_permission_settings)
                        "LOCATION_DISABLED" -> context.getString(R.string.location_error_gps_disabled)
                        "LOCATION_UNAVAILABLE" -> context.getString(R.string.location_error_unavailable)
                        else -> error.message ?: context.getString(R.string.location_error_generic)
                    }
                    _uiState.update {
                        it.copy(
                            isResolvingLocation = false,
                            latitude = null,
                            longitude = null,
                            pharmacyLocation = if (accountType == AccountType.PHARMACY) "" else it.pharmacyLocation,
                            warehouseLocation = if (accountType == AccountType.WAREHOUSE) "" else it.warehouseLocation,
                            locationMessage = message,
                            locationMessageIsError = true,
                            locationSettingsAction = action,
                        )
                    }
                }
        }
    }

    fun onLocationPermissionDenied(permanentlyDenied: Boolean) {
        val accountType = _uiState.value.accountType
        _uiState.update {
            it.copy(
                isResolvingLocation = false,
                latitude = null,
                longitude = null,
                pharmacyLocation = if (accountType == AccountType.PHARMACY) "" else it.pharmacyLocation,
                warehouseLocation = if (accountType == AccountType.WAREHOUSE) "" else it.warehouseLocation,
                locationMessage = if (permanentlyDenied) {
                    context.getString(R.string.location_error_permission_settings)
                } else {
                    context.getString(R.string.location_error_permission_required)
                },
                locationMessageIsError = true,
                locationSettingsAction = if (permanentlyDenied) {
                    SignUpLocationSettingsAction.APP_SETTINGS
                } else {
                    null
                },
            )
        }
    }

    fun signUp() {
        val currentState = _uiState.value
        Log.d(TAG, "Sign up requested for type=${currentState.accountType}")

        val validationError = validateSignUp(currentState)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError, isLoading = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isSuccess = false,
                    errorMessage = null,
                    successMessage = null,
                    requireManualLogin = false,
                    navigateToLogin = false,
                )
            }

            val request = SignUpRequest(
                accountType = currentState.accountType,
                fullName = currentState.fullName,
                phoneNumber = currentState.phoneNumber,
                password = currentState.password,
                confirmPassword = currentState.confirmPassword,
                pharmacyName = currentState.pharmacyName,
                pharmacyLocation = currentState.pharmacyLocation,
                warehouseName = currentState.warehouseName,
                warehouseLocation = currentState.warehouseLocation,
                latitude = currentState.latitude,
                longitude = currentState.longitude,
            )

            authRepository.signUp(request).fold(
                onSuccess = { signUpResult ->
                    if (signUpResult.requiresManualLogin) {
                        handleManualLoginRequired(signUpResult.user)
                    } else {
                        completeIdentityBootstrap(signUpResult.user)
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Signup failed: ${error.message}", error)
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

    private suspend fun handleManualLoginRequired(user: User) {
        val successMessage = context.getString(R.string.signup_success_created) + "\n" +
            context.getString(R.string.signup_success_login_required)

        _uiState.update {
            it.copy(
                isLoading = false,
                isSuccess = true,
                userId = user.id,
                emailConfirmationRequired = false,
                requireManualLogin = true,
                successMessage = successMessage,
                errorMessage = null,
            )
        }

        delay(SENSITIVE_SIGNUP_REDIRECT_DELAY_MS)
        _uiState.update { current ->
            if (current.isSuccess && current.requireManualLogin) {
                current.copy(navigateToLogin = true)
            } else {
                current
            }
        }
    }

    private suspend fun completeIdentityBootstrap(user: User) {
        authRepository.bootstrapAuthenticatedUser(user).fold(
            onSuccess = {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        userId = user.id,
                        emailConfirmationRequired = false,
                        requireManualLogin = false,
                        successMessage = null,
                        errorMessage = null,
                    )
                }
            },
            onFailure = { error ->
                Log.e(TAG, "Identity bootstrap failed after signup: ${error.message}", error)
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

    fun onLoginNavigationConsumed() {
        _uiState.update { it.copy(navigateToLogin = false) }
    }

    private fun validateSignUp(state: SignUpUiState): String? {
        return when {
            state.password.length < 6 -> context.getString(R.string.auth_error_password_short)
            state.password != state.confirmPassword -> context.getString(R.string.auth_error_password_mismatch)
            com.pharmalink.core.common.validation.SyrianPhone.normalizeToE164Digits(state.phoneNumber) == null ->
                context.getString(R.string.auth_error_invalid_phone)
            state.fullName.isBlank() -> context.getString(R.string.auth_error_name_required)
            state.accountType == AccountType.PHARMACY && state.pharmacyName.isBlank() ->
                context.getString(R.string.auth_error_pharmacy_name_required)
            state.accountType == AccountType.PHARMACY &&
                (state.pharmacyLocation.isBlank() || state.latitude == null || state.longitude == null) ->
                context.getString(R.string.auth_error_pharmacy_map_location_required)
            state.accountType == AccountType.WAREHOUSE && state.warehouseName.isBlank() ->
                context.getString(R.string.auth_error_warehouse_name_required)
            state.accountType == AccountType.WAREHOUSE &&
                (state.warehouseLocation.isBlank() || state.latitude == null || state.longitude == null) ->
                context.getString(R.string.auth_error_warehouse_map_location_required)
            else -> null
        }
    }

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

    fun resetState() {
        _uiState.value = SignUpUiState()
    }
}
