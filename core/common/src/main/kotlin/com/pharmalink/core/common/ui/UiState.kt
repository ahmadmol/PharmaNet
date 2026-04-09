package com.pharmalink.core.common.ui

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun <T> UiState<T>.toScreenState(): ScreenState<T> = when (this) {
    UiState.Idle -> ScreenState.Empty
    UiState.Loading -> ScreenState.Loading
    is UiState.Success -> ScreenState.Success(data)
    is UiState.Error -> ScreenState.Error(message)
}
