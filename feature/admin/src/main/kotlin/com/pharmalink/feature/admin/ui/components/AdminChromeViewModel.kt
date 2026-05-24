package com.pharmalink.feature.admin.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.data.repository.PharmaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminChromeUiState(
    val profileImageUrl: String? = null,
)

@HiltViewModel
class AdminChromeViewModel @Inject constructor(
    private val pharmaRepository: PharmaRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminChromeUiState())
    val state: StateFlow<AdminChromeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            pharmaRepository.observeProfile().collect { profile ->
                _state.update { it.copy(profileImageUrl = profile.avatarUrl) }
            }
        }
    }
}
