package com.pharmalink.domain.model

sealed interface AuthSessionState {
    data object Loading : AuthSessionState

    data class Authenticated(val user: User) : AuthSessionState

    data object Unauthenticated : AuthSessionState
}

data class UserSnapshot(
    val userId: String,
    val phoneNumber: String,
    val email: String,
    // Compatibility carrier (legacy): kept during migration phases.
    val pharmacyId: String,
    val pharmacyName: String,
    // Role-native organization context (Phase 2 additive expansion).
    val warehouseId: String = "",
    val warehouseName: String = "",
    val accountType: AccountType,
    val displayName: String,
)
