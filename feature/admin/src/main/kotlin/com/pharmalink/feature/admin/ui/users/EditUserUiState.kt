package com.pharmalink.feature.admin.ui.users

import androidx.compose.runtime.Immutable
import com.pharmalink.domain.model.AccountType

@Immutable
data class EditUserUiState(
    val userId: String = "",
    val fullName: String = "",
    val accountType: AccountType = AccountType.PHARMACY,
    val facilityId: String = "",
    val isActive: Boolean = true,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String = "",
    val fullNameError: String = "",
    val facilityIdError: String = "",
)

sealed interface EditUserAction {
    data class OnFullNameChanged(val name: String) : EditUserAction
    data class OnAccountTypeChanged(val type: AccountType) : EditUserAction
    data class OnFacilityIdChanged(val id: String) : EditUserAction
    data class OnActiveToggled(val isActive: Boolean) : EditUserAction
    data object OnSaveClicked : EditUserAction
    data object OnDismiss : EditUserAction
}

sealed interface EditUserEffect {
    data object Dismiss : EditUserEffect
    data class ShowMessage(val message: String) : EditUserEffect
}
