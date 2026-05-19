package com.pharmalink.feature.help.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.data.repository.PharmaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ContactUsUiState(
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val submitError: String? = null,
)

@HiltViewModel
class ContactUsViewModel @Inject constructor(
    private val repository: PharmaRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ContactUsUiState())
    val uiState: StateFlow<ContactUsUiState> = _uiState.asStateFlow()

    fun submit(subject: String, message: String, category: String?) {
        viewModelScope.launch {
            _uiState.value = ContactUsUiState(isSubmitting = true)
            val result = repository.submitSupportRequest(
                subject = subject,
                message = message,
                category = category,
            )
            _uiState.value = if (result.isSuccess) {
                ContactUsUiState(submitSuccess = true)
            } else {
                ContactUsUiState(submitError = result.exceptionOrNull()?.message)
            }
        }
    }

    fun consumeSuccess() {
        _uiState.value = _uiState.value.copy(submitSuccess = false)
    }
}
