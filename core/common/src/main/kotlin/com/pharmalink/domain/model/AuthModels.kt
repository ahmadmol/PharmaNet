package com.pharmalink.domain.model

/**
 * Authentication Data Models
 */

data class LoginRequest(
    val phoneNumber: String,
    val password: String,
)

data class SignUpRequest(
    val accountType: AccountType,
    val fullName: String,
    val phoneNumber: String,
    val password: String,
    val confirmPassword: String,
    val pharmacyName: String = "",
    val pharmacyLocation: String = "",
    val warehouseName: String = "",
    val warehouseLocation: String = "",
)

data class User(
    val id: String,
    val fullName: String,
    val pharmacyName: String,
    val phoneNumber: String,
    val email: String = "",
    val isActive: Boolean = true,
    val accountType: AccountType = AccountType.PHARMACY,
    val pharmacyLocation: String = "",
    val warehouseName: String = "",
    val warehouseLocation: String = "",
)

/**
 * Authentication UI States
 */
data class LoginUiState(
    val phoneNumber: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccessful: Boolean = false,
)


data class SplashUiState(
    val isLoading: Boolean = true,
    val shouldNavigateToLogin: Boolean = false,
    val shouldNavigateToMain: Boolean = false,
)

/**
 * Helper functions for authentication models
 */

// ✅ امتداد للتحقق من صحة طلب التسجيل
fun SignUpRequest.isValid(): Boolean {
    return password.length >= 6 &&
           password == confirmPassword &&
           fullName.isNotBlank() &&
           com.pharmalink.core.common.validation.SyrianPhone.normalizeToE164Digits(phoneNumber) != null
}

// ✅ امتداد لتحويل طلب التسجيل إلى بيانات الملف الشخصي
fun SignUpRequest.toProfileData(): Map<String, String> = buildMap {
    put("full_name", fullName)
    put("phone_number", phoneNumber)
    put("account_type", accountType.name)
    
    when (accountType) {
        AccountType.PHARMACY -> {
            put("pharmacy_name", pharmacyName)
            put("pharmacy_location", pharmacyLocation)
        }
        AccountType.WAREHOUSE -> {
            put("warehouse_name", warehouseName)
            put("warehouse_location", warehouseLocation)
        }
        AccountType.PUBLIC_USER -> {
            // لا حقول إضافية
        }
    }
}

// ✅ امتداد للتحقق السريع من طلب الدخول
fun LoginRequest.isValid(): Boolean {
    return password.length >= 6 &&
           com.pharmalink.core.common.validation.SyrianPhone.normalizeToE164Digits(phoneNumber) != null
}
