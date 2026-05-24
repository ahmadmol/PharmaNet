package com.pharmalink.feature.admin.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AdminUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AdminUsersViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminUsersUiState())
    val state: StateFlow<AdminUsersUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<AdminUsersEffect>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val effect: SharedFlow<AdminUsersEffect> = _effect.asSharedFlow()

    private val _allUsers = MutableStateFlow<List<UserItemModel>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _filterStatus = MutableStateFlow(UserFilterStatus.ALL)
    private val _sortBy = MutableStateFlow(UserSortBy.NAME)

    init {
        loadUsers()
        observeFilteredUsers()
    }

    private fun observeFilteredUsers() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                _allUsers,
                _searchQuery,
                _filterStatus,
                _sortBy
            ) { users, query, status, sortBy ->
                var filtered = users

                // Apply search filter
                if (query.isNotBlank()) {
                    filtered = filtered.filter { user ->
                        user.fullName?.contains(query, ignoreCase = true) == true ||
                        user.email.contains(query, ignoreCase = true) ||
                        user.facilityName.orEmpty().contains(query, ignoreCase = true)
                    }
                }

                // Apply status filter
                filtered = when (status) {
                    UserFilterStatus.ALL -> filtered
                    UserFilterStatus.ACTIVE -> filtered.filter { it.isActive }
                    UserFilterStatus.INACTIVE -> filtered.filter { !it.isActive }
                }

                // Apply sorting
                filtered = when (sortBy) {
                    UserSortBy.NAME -> filtered.sortedBy { it.fullName ?: it.email }
                    UserSortBy.DATE_JOINED -> filtered.sortedByDescending { it.createdAt }
                    UserSortBy.ACCOUNT_TYPE -> filtered.sortedBy { it.accountType.name }
                }

                filtered
            }.collect { filteredUsers ->
                _state.update { it.copy(users = filteredUsers) }
            }
        }
    }

    fun onAction(action: AdminUsersAction) {
        when (action) {
            AdminUsersAction.OnRetryClicked -> loadUsers()
            AdminUsersAction.OnRefreshTriggered -> refreshUsers()
            AdminUsersAction.OnMenuClicked -> {
                viewModelScope.launch {
                    _effect.emit(AdminUsersEffect.ShowAdminMenu)
                }
            }
            is AdminUsersAction.OnSearchQueryChanged -> updateSearchQuery(action.query)
            AdminUsersAction.OnFilterClicked -> {
                // Filter UI is now always visible, no need for message
            }
            AdminUsersAction.OnSortClicked -> {
                // Sort UI is now always visible, no need for message
            }
            is AdminUsersAction.OnFilterStatusChanged -> updateFilterStatus(action.status)
            is AdminUsersAction.OnSortByChanged -> updateSortBy(action.sortBy)
            is AdminUsersAction.OnEditUserClicked -> showEditSheet(action.user)
            is AdminUsersAction.OnDeleteUserClicked -> showDeleteConfirmation(action.userId)
            AdminUsersAction.OnAddUserClicked -> {
                // PRODUCTION: Add user feature not available - users self-register via auth
                // Admin manages existing users through Edit functionality only
                viewModelScope.launch {
                    _effect.emit(AdminUsersEffect.ShowMessage("إضافة مستخدم غير متاح - المستخدمون يسجلون ذاتياً"))
                }
            }
            AdminUsersAction.OnDismissEditSheet -> {
                _state.update {
                    it.copy(
                        isEditSheetVisible = false,
                        editSheetUserId = "",
                    )
                }
            }
            AdminUsersAction.OnConfirmDelete -> {
                // PRODUCTION: Destructive delete not available - use deactivation instead
                // Backend does not support user deletion for data integrity
                // Admin must use Edit User to deactivate accounts
                viewModelScope.launch {
                    _effect.emit(AdminUsersEffect.ShowMessage("الحذف غير متاح - استخدم تعطيل الحساب من خلال التعديل"))
                }
            }
            AdminUsersAction.OnDismissDeleteDialog -> {
                // Handled in UI
            }
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, contentError = "") }

            pharmaRepository.adminGetAllUsers()
                .onSuccess { users ->
                    val userModels = users.map { it.toUiModel() }
                    val activeCount = users.count { it.isActive }
                    
                    _allUsers.value = userModels
                    _state.update {
                        it.copy(
                            isLoading = false,
                            totalUsers = users.size,
                            activeUsers = activeCount,
                            monthlyGrowth = calculateGrowth(users.size),
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            contentError = USERS_LOAD_ERROR_MESSAGE,
                        )
                    }
                }
        }
    }

    private fun refreshUsers() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            
            pharmaRepository.adminGetAllUsers()
                .onSuccess { users ->
                    val userModels = users.map { it.toUiModel() }
                    val activeCount = users.count { it.isActive }
                    
                    _allUsers.value = userModels
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            totalUsers = users.size,
                            activeUsers = activeCount,
                            monthlyGrowth = calculateGrowth(users.size),
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isRefreshing = false) }
                    _effect.emit(AdminUsersEffect.ShowMessage("فشل تحديث المستخدمين"))
                }
        }
    }

    private fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _state.update { it.copy(searchQuery = query) }
    }

    private fun updateFilterStatus(status: UserFilterStatus) {
        _filterStatus.value = status
        _state.update { it.copy(filterStatus = status) }
    }

    private fun updateSortBy(sortBy: UserSortBy) {
        _sortBy.value = sortBy
        _state.update { it.copy(sortBy = sortBy) }
    }

    private fun showEditSheet(user: UserItemModel) {
        _state.update {
            it.copy(
                isEditSheetVisible = true,
                editSheetUserId = user.id,
                editSheetFullName = user.fullName ?: "",
                editSheetAccountType = user.accountType.name,
                editSheetFacilityId = user.facilityId ?: "",
                editSheetIsActive = user.isActive,
            )
        }
    }

    private fun showDeleteConfirmation(userId: String) {
        viewModelScope.launch {
            val user = _state.value.users.find { it.id == userId }
            if (user != null) {
                _effect.emit(
                    AdminUsersEffect.ShowDeleteConfirmation(
                        userId = userId,
                        userName = user.fullName,
                    )
                )
            }
        }
    }

    private fun calculateGrowth(totalUsers: Int): Float {
        return 0f
    }

    private fun AdminUser.toUiModel(): UserItemModel {
        return UserItemModel(
            id = id,
            fullName = fullName,
            email = email,
            accountType = accountType,
            facilityId = pharmacyId ?: warehouseId,
            facilityName = pharmacyName ?: warehouseName,
            isActive = isActive,
            avatarUrl = "",
            createdAt = createdAt,
        )
    }
    private companion object {
        private const val USERS_LOAD_ERROR_MESSAGE = "تعذر تحميل المستخدمين. حاول مرة أخرى."
    }
}



