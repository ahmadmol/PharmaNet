package com.pharmalink.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun PharmaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    fullWidth: Boolean = true,
    errorMessage: String? = null,
    supportingText: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val isError = errorMessage != null
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = if (fullWidth) modifier.fillMaxWidth() else modifier,
        enabled = enabled,
        singleLine = singleLine,
        isError = isError,
        label = { Text(label) },
        supportingText = when {
            isError -> ({ Text(errorMessage!!, color = MaterialTheme.colorScheme.error) })
            supportingText != null -> ({ Text(supportingText) })
            else -> null
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            errorBorderColor = MaterialTheme.colorScheme.error,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
        trailingIcon = trailingIcon,
        shape = MaterialTheme.shapes.medium,
    )
}
