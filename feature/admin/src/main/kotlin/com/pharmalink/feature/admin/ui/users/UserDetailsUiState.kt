package com.pharmalink.feature.admin.ui.users

import androidx.compose.runtime.Immutable
import com.pharmalink.domain.model.AccountType

@Immutable
data class UserDetailsUiState(
    val isLoading: Boolean = false,
    val contentError: String = "",
    val user: UserDetailModel? = null,
)

@Immutable
data class UserDetailModel(
    val id: String = "",
    val fullName: String? = null,
    val email: String = "",
    val phoneNumber: String? = null,
    val accountType: AccountType = AccountType.PUBLIC_USER,
    val facilityId: String? = null,
    val facilityName: String = "",
    val isActive: Boolean = false,
    val createdAt: String = "",
    val totalOrders: Int = 0,
    val totalRequests: Int = 0,
    val lastLoginDate: String = "",
)

sealed interface UserDetailsAction {
    data object OnRetryClicked : UserDetailsAction
    data object OnEditClicked : UserDetailsAction
    data object OnDeactivateClicked : UserDetailsAction
    data object OnResetPasswordClicked : UserDetailsAction
}

sealed interface UserDetailsEffect {
    data class ShowMessage(val message: String) : UserDetailsEffect
    data class NavigateToEdit(val userId: String) : UserDetailsEffect
}
