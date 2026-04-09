package com.pharmalink.core.common.ui

interface ScreenContract<STATE, ACTION : UiAction, EVENT : UiEvent> {
    val state: STATE

    fun onAction(action: ACTION)
}
