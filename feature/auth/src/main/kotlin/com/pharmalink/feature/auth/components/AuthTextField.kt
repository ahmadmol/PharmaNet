package com.pharmalink.feature.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.feature.auth.R

/**
 * Auth TextField Component
 * Premium text field with icon and soft styling for auth screens
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
        leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    errorMessage: String? = null,
    readOnly: Boolean = false,
) {
    val d = MaterialTheme.dimens
    var isPasswordVisible by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            label = { 
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
                        leadingIcon = leadingIcon?.let { icon ->
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(
                        onClick = { isPasswordVisible = !isPasswordVisible },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible) {
                                Icons.Outlined.Visibility
                            } else {
                                Icons.Outlined.VisibilityOff
                            },
                            contentDescription = if (isPasswordVisible) {
                                stringResource(R.string.auth_password_hide_full)
                            } else {
                                stringResource(R.string.auth_password_show_full)
                            },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !isPasswordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
            ),
            keyboardActions = keyboardActions,
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                errorTextColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        )
        
        // Error Message
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = d.spaceS, top = 4.dp),
            )
        }
    }
}

// Pre-configured Auth TextFields
@Composable
fun EmailTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = label ?: stringResource(R.string.auth_email_label),
        leadingIcon = Icons.Outlined.Email,
        keyboardType = KeyboardType.Email,
        isError = isError,
        errorMessage = errorMessage,
        modifier = modifier,
    )
}

@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = label ?: stringResource(R.string.auth_password_label),
        leadingIcon = Icons.Outlined.Lock,
        isPassword = true,
        keyboardType = KeyboardType.Password,
        isError = isError,
        errorMessage = errorMessage,
        modifier = modifier,
    )
}

@Composable
fun NameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = label ?: stringResource(R.string.auth_full_name_label),
        leadingIcon = Icons.Outlined.Person,
        keyboardType = KeyboardType.Text,
        isError = isError,
        errorMessage = errorMessage,
        modifier = modifier,
    )
}

@Composable
fun PharmacyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = label ?: stringResource(R.string.auth_pharmacy_name_label),
        leadingIcon = Icons.Outlined.Store,
        keyboardType = KeyboardType.Text,
        isError = isError,
        errorMessage = errorMessage,
        modifier = modifier,
    )
}

@Composable
fun PhoneTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = label ?: stringResource(R.string.auth_phone_label),
        leadingIcon = Icons.Outlined.Phone,
        keyboardType = KeyboardType.Phone,
        isError = isError,
        errorMessage = errorMessage,
        modifier = modifier,
    )
}

@Composable
fun LocationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    readOnly: Boolean = false,
) {
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = label ?: stringResource(R.string.auth_location_label),
        leadingIcon = Icons.Outlined.Place,
        keyboardType = KeyboardType.Text,
        isError = isError,
        errorMessage = errorMessage,
        modifier = modifier,
        readOnly = readOnly,
    )
}

@Composable
fun WarehouseNameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = label ?: stringResource(R.string.auth_warehouse_name_label),
        leadingIcon = Icons.Outlined.Inventory,
        keyboardType = KeyboardType.Text,
        isError = isError,
        errorMessage = errorMessage,
        modifier = modifier,
    )
}
