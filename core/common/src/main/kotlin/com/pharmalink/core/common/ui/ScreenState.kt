package com.pharmalink.core.common.ui

sealed interface ScreenState<out T> {
    data object Loading : ScreenState<Nothing>
    data object Empty : ScreenState<Nothing>
    data class Success<T>(val data: T) : ScreenState<T>
    data class Error(val message: String? = null) : ScreenState<Nothing> {
        companion object
    }
    data class Offline(val message: String? = null) : ScreenState<Nothing> {
        companion object
    }
}
