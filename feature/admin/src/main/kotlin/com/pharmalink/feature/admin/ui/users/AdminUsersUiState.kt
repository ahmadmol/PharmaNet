package com.pharmalink.feature.admin.ui.users

import androidx.compose.runtime.Immutable
import com.pharmalink.domain.model.AccountType

@Immutable
data class AdminUsersUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val contentError: String = "",
    val users: List<UserItemModel> = emptyList(),
    val searchQuery: String = "",
    val filterStatus: UserFilterStatus = UserFilterStatus.ALL,
    val sortBy: UserSortBy = UserSortBy.NAME,
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val monthlyGrowth: Float = 0f,
)

enum class UserFilterStatus {
    ALL,
    ACTIVE,
    INACTIVE
}

enum class UserSortBy {
    NAME,
    DATE_JOINED,
    ACCOUNT_TYPE
}

@Immutable
data class UserItemModel(
    val id: String = "",
    val fullName: String? = null,
    val email: String = "",
    val accountType: AccountType = AccountType.PUBLIC_USER,
    val facilityId: String? = null,
    val facilityName: String? = null,
    val isActive: Boolean = false,
    val avatarUrl: String = "",
    val createdAt: String = "",
)

sealed interface AdminUsersAction {
    data object OnRetryClicked : AdminUsersAction
    data object OnRefreshTriggered : AdminUsersAction
    data object OnMenuClicked : AdminUsersAction
    data class OnSearchQueryChanged(val query: String) : AdminUsersAction
    data object OnFilterClicked : AdminUsersAction
    data object OnSortClicked : AdminUsersAction
    data class OnFilterStatusChanged(val status: UserFilterStatus) : AdminUsersAction
    data class OnSortByChanged(val sortBy: UserSortBy) : AdminUsersAction
    data class OnEditUserClicked(val user: UserItemModel) : AdminUsersAction
    data class OnDeleteUserClicked(val userId: String) : AdminUsersAction
    data object OnAddUserClicked : AdminUsersAction
    data object OnDismissEditSheet : AdminUsersAction
    data object OnConfirmDelete : AdminUsersAction
    data object OnDismissDeleteDialog : AdminUsersAction
}

sealed interface AdminUsersEffect {
    data class ShowMessage(val message: String) : AdminUsersEffect
    data class ShowEditUserSheet(
        val userId: String,
        val fullName: String?,
        val accountType: AccountType,
        val facilityId: String?,
        val isActive: Boolean,
    ) : AdminUsersEffect
    data class ShowDeleteConfirmation(val userId: String, val userName: String?) : AdminUsersEffect
}
