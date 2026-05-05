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
                    _effect.emit(AdminUsersEffect.ShowMessage("القائمة: قيد التطوير"))
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
                // TODO: Navigate to add user
            }
            AdminUsersAction.OnDismissEditSheet -> {
                // Handled in UI
            }
            AdminUsersAction.OnConfirmDelete -> {
                // TODO: Implement delete
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
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            contentError = e.message ?: "فشل تحميل المستخدمين",
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
        viewModelScope.launch {
            _effect.emit(
                AdminUsersEffect.ShowEditUserSheet(
                    userId = user.id,
                    fullName = user.fullName,
                    accountType = user.accountType,
                    facilityId = user.facilityId,
                    isActive = user.isActive,
                )
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
        // Simulate monthly growth percentage
        return if (totalUsers > 0) (totalUsers * 0.12f) else 0f
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
}
