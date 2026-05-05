package com.pharmalink.designsystem.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

@Composable
fun <T> CollectEffect(
    effect: Flow<T>,
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    onEffect: suspend (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEffect by rememberUpdatedState(onEffect)

    LaunchedEffect(effect, lifecycleOwner, lifecycleState) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(lifecycleState) {
            effect.collect { currentOnEffect(it) }
        }
    }
}
