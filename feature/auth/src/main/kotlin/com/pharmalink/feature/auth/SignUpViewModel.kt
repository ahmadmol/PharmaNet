package com.pharmalink.feature.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.repository.AuthRepository
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.SignUpRequest
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
    // ✅ حقول الـ UI (تُحدَّث من الشاشة)
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

    // ✅ تحديث حقول الـ UI من الشاشة
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

    // ✅ الدالة الرئيسية للتسجيل
    fun signUp() {
        val currentState = _uiState.value
        
        // ✅ تحقق مسبق قبل الإرسال
        val validationError = validateSignUp(currentState)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError, isLoading = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // ✅ بناء طلب التسجيل
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
            
            Log.d(TAG, "Submitting signup request for phone: ${request.phoneNumber}")
            
            authRepository.signUp(request)
                .onSuccess { user ->
                    Log.d(TAG, "Signup successful, userId: ${user.id}")
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            userId = user.id
                        )
                    }
                }
                .onFailure { error ->
                    val userMessage = mapAuthErrorToUserMessage(error)
                    Log.e(TAG, "Signup failed: $userMessage", error)
                    _uiState.update { 
                        it.copy(isLoading = false, errorMessage = userMessage)
                    }
                }
        }
    }

    // ✅ التحقق من صحة المدخلات
    private fun validateSignUp(state: SignUpUiState): String? {
        return when {
            state.password.length < 6 -> 
                context.getString(R.string.auth_error_password_short)
            state.password != state.confirmPassword -> 
                context.getString(R.string.auth_error_password_mismatch)
            com.pharmalink.core.common.validation.SyrianPhone.normalizeToE164Digits(state.phoneNumber) == null -> 
                context.getString(R.string.auth_error_invalid_phone)
            state.fullName.isBlank() -> 
                context.getString(R.string.auth_error_name_required)
            state.accountType == AccountType.PHARMACY && state.pharmacyName.isBlank() ->
                "يرجى إدخال اسم الصيدلية"
            state.accountType == AccountType.WAREHOUSE && state.warehouseName.isBlank() ->
                "يرجى إدخال اسم المستودع"
            else -> null
        }
    }

    // ✅ تحويل أخطاء النظام إلى رسائل مستخدم واضحة
    private fun mapAuthErrorToUserMessage(error: Throwable): String {
        return when {
            error.message?.contains("User already registered", ignoreCase = true) == true ->
                context.getString(R.string.auth_error_phone_exists)
            error.message?.contains("Invalid credentials", ignoreCase = true) == true ->
                context.getString(R.string.auth_error_weak_password)
            error.message?.contains("Network", ignoreCase = true) == true ->
                context.getString(R.string.error_network)
            else -> context.getString(R.string.error_unknown)
        }
    }

    // ✅ إعادة تعيين الحالة (عند الخروج من الشاشة)
    fun resetState() {
        _uiState.value = SignUpUiState()
    }
}
