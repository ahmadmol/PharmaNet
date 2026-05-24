package com.pharmalink.feature.admin.ui.pharmacies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.Pharmacy
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
class AdminPharmaciesViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminPharmaciesUiState())
    val state: StateFlow<AdminPharmaciesUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<AdminPharmaciesEffect>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val effect: SharedFlow<AdminPharmaciesEffect> = _effect.asSharedFlow()

    private val _allPharmacies = MutableStateFlow<List<PharmacyItemModel>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _filterStatus = MutableStateFlow(PharmacyFilterStatus.ALL)
    private val _sortBy = MutableStateFlow(PharmacySortBy.NAME)

    init {
        loadPharmacies()
        observeFilteredPharmacies()
    }

    private fun observeFilteredPharmacies() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                _allPharmacies,
                _searchQuery,
                _filterStatus,
                _sortBy
            ) { pharmacies, query, status, sortBy ->
                var filtered = pharmacies

                // Apply search filter
                if (query.isNotBlank()) {
                    filtered = filtered.filter { pharmacy ->
                        pharmacy.name.contains(query, ignoreCase = true) ||
                        pharmacy.location.contains(query, ignoreCase = true)
                    }
                }

                // Apply status filter
                filtered = when (status) {
                    PharmacyFilterStatus.ALL -> filtered
                    PharmacyFilterStatus.ACTIVE -> filtered.filter { it.isActive }
                    PharmacyFilterStatus.INACTIVE -> filtered.filter { !it.isActive }
                }

                // Apply sorting
                filtered = when (sortBy) {
                    PharmacySortBy.NAME -> filtered.sortedBy { it.name }
                    PharmacySortBy.LOCATION -> filtered.sortedBy { it.location }
                    PharmacySortBy.DATE_ADDED -> filtered.sortedByDescending { it.createdAt }
                }

                filtered
            }.collect { filteredPharmacies ->
                _state.update { it.copy(pharmacies = filteredPharmacies) }
            }
        }
    }

    fun onAction(action: AdminPharmaciesAction) {
        when (action) {
            AdminPharmaciesAction.OnRetryClicked -> loadPharmacies()
            AdminPharmaciesAction.OnRefreshTriggered -> refreshPharmacies()
            AdminPharmaciesAction.OnMenuClicked -> {
                viewModelScope.launch {
                    _effect.emit(AdminPharmaciesEffect.ShowAdminMenu)
                }
            }
            is AdminPharmaciesAction.OnSearchQueryChanged -> updateSearchQuery(action.query)
            AdminPharmaciesAction.OnFilterClicked -> {
                // Filter UI is now always visible, no need for message
            }
            is AdminPharmaciesAction.OnFilterStatusChanged -> updateFilterStatus(action.status)
            is AdminPharmaciesAction.OnSortByChanged -> updateSortBy(action.sortBy)
            is AdminPharmaciesAction.OnPharmacyClicked -> navigateToDetail(action.pharmacyId)
            is AdminPharmaciesAction.OnManageBranchClicked -> navigateToBranch(action.pharmacyId)
            AdminPharmaciesAction.OnAddPharmacyClicked -> {
                // Handled in UI - navigate to create facility
            }
        }
    }

    private fun loadPharmacies() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, contentError = "") }

            pharmaRepository.adminGetAllPharmacies()
                .onSuccess { pharmacies ->
                    val pharmacyModels = pharmacies.map { it.toUiModel() }
                    
                    _allPharmacies.value = pharmacyModels
                    _state.update {
                        it.copy(
                            isLoading = false,
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            contentError = PHARMACIES_ERROR_MESSAGE,
                        )
                    }
                }
        }
    }

    private fun refreshPharmacies() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            
            pharmaRepository.adminGetAllPharmacies()
                .onSuccess { pharmacies ->
                    val pharmacyModels = pharmacies.map { it.toUiModel() }
                    
                    _allPharmacies.value = pharmacyModels
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isRefreshing = false) }
                    _effect.emit(AdminPharmaciesEffect.ShowMessage("فشل تحديث الصيدليات"))
                }
        }
    }

    private fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _state.update { it.copy(searchQuery = query) }
    }

    private fun updateFilterStatus(status: PharmacyFilterStatus) {
        _filterStatus.value = status
        _state.update { it.copy(filterStatus = status) }
    }

    private fun updateSortBy(sortBy: PharmacySortBy) {
        _sortBy.value = sortBy
        _state.update { it.copy(sortBy = sortBy) }
    }

    private fun navigateToDetail(pharmacyId: String) {
        viewModelScope.launch {
            _effect.emit(AdminPharmaciesEffect.NavigateToPharmacyDetail(pharmacyId))
        }
    }

    private fun navigateToBranch(pharmacyId: String) {
        viewModelScope.launch {
            _effect.emit(AdminPharmaciesEffect.NavigateToBranchManagement(pharmacyId))
        }
    }

    private fun Pharmacy.toUiModel(): PharmacyItemModel {
        return PharmacyItemModel(
            id = id,
            name = name,
            location = location ?: "",
            isActive = isActive,
            createdAt = createdAt ?: "",
        )
    }

    private companion object {
        private const val PHARMACIES_ERROR_MESSAGE = "تعذر تحميل الصيدليات. حاول مرة أخرى."
    }
}



