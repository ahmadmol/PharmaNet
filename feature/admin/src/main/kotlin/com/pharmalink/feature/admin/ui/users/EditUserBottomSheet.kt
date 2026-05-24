package com.pharmalink.feature.admin.ui.users

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaSwitch
import com.pharmalink.designsystem.components.PharmaTextField
import com.pharmalink.designsystem.theme.PharmaTheme
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.designsystem.utils.CollectEffect
import com.pharmalink.domain.model.AccountType
import com.pharmalink.feature.admin.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserBottomSheet(
    userId: String,
    fullName: String,
    accountType: AccountType,
    facilityId: String,
    isActive: Boolean,
    onDismiss: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: EditUserViewModel,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        viewModel.loadUser(userId, fullName, accountType, facilityId, isActive)
    }

    CollectEffect(effect = viewModel.effect) { effect ->
        when (effect) {
            EditUserEffect.Dismiss -> onDismiss()
            is EditUserEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            EditUserBottomSheetContent(
                state = state,
                onAction = viewModel::onAction,
            )
        }

        AnimatedVisibility(
            visible = state.showSensitiveChangeConfirmation,
            enter = fadeIn(animationSpec = tween(durationMillis = 140)) +
                slideInVertically(animationSpec = tween(durationMillis = 140)) { it / 12 },
            exit = fadeOut(animationSpec = tween(durationMillis = 100)),
        ) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.onAction(EditUserAction.OnDismissSensitiveConfirmation)
                },
                title = { Text("تأكيد تغيير حساس") },
                text = {
                    Column(
                        modifier = Modifier.animateContentSize(animationSpec = tween(durationMillis = 160)),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceS),
                    ) {
                        Text("هذه التغييرات تؤثر على الصلاحيات أو حالة الحساب:")
                        Crossfade(
                            targetState = state.sensitiveChangeWarning,
                            animationSpec = tween(durationMillis = 160),
                            label = "edit_user_sensitive_warning",
                        ) { warning ->
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.onAction(EditUserAction.OnConfirmSensitiveSave)
                        },
                    ) {
                        Text("تأكيد الحفظ")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.onAction(EditUserAction.OnDismissSensitiveConfirmation)
                        },
                    ) {
                        Text("مراجعة")
                    }
                },
            )
        }
    }
}

@Composable
private fun EditUserBottomSheetContent(
    state: EditUserUiState,
    onAction: (EditUserAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val showFacilityField = state.accountType == AccountType.PHARMACY ||
        state.accountType == AccountType.WAREHOUSE
    val facilityLabel = when (state.accountType) {
        AccountType.PHARMACY -> stringResource(R.string.edit_user_facility_id_label)
        AccountType.WAREHOUSE -> stringResource(R.string.edit_user_facility_id_label_warehouse)
        else -> ""
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = d.spaceL)
            .padding(bottom = d.spaceXXL),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.edit_user_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            IconButton(onClick = { onAction(EditUserAction.OnDismiss) }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.edit_user_close_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(d.spaceL))

        PharmaTextField(
            value = state.fullName,
            onValueChange = { onAction(EditUserAction.OnFullNameChanged(it)) },
            label = stringResource(R.string.edit_user_full_name_label),
            placeholder = stringResource(R.string.edit_user_full_name_placeholder),
            errorMessage = state.fullNameError,
            enabled = !state.isSaving,
        )

        Spacer(Modifier.height(d.spaceM))

        AccountTypeDropdown(
            selectedType = state.accountType,
            onTypeSelected = { onAction(EditUserAction.OnAccountTypeChanged(it)) },
            enabled = !state.isSaving,
        )

        Spacer(Modifier.height(d.spaceM))

        AnimatedVisibility(
            visible = showFacilityField,
            enter = fadeIn(animationSpec = tween(durationMillis = 180)) + expandVertically(),
            exit = fadeOut(animationSpec = tween(durationMillis = 140)) + shrinkVertically(),
        ) {
            PharmaTextField(
                value = state.facilityId,
                onValueChange = { onAction(EditUserAction.OnFacilityIdChanged(it)) },
                label = facilityLabel,
                placeholder = "550e8400-e29b-41d4-a716-446655440000",
                errorMessage = state.facilityIdError,
                enabled = !state.isSaving,
            )
        }
        AnimatedVisibility(
            visible = showFacilityField,
            enter = fadeIn(animationSpec = tween(durationMillis = 180)) + expandVertically(),
            exit = fadeOut(animationSpec = tween(durationMillis = 140)) + shrinkVertically(),
        ) {
            Spacer(Modifier.height(d.spaceL))
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(d.spaceL),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.edit_user_status_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(d.spaceXS))
                    Text(
                        text = stringResource(R.string.edit_user_status_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.width(d.spaceM))

                PharmaSwitch(
                    checked = state.isActive,
                    onCheckedChange = { onAction(EditUserAction.OnActiveToggled(it)) },
                    enabled = !state.isSaving,
                )
            }
        }

        Spacer(Modifier.height(d.spaceXL))

        PharmaButton(
            text = if (state.isSaving) {
                stringResource(R.string.edit_user_saving)
            } else {
                stringResource(R.string.edit_user_save_button)
            },
            onClick = { onAction(EditUserAction.OnSaveClicked) },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )

        AnimatedVisibility(
            visible = state.isSaving,
            enter = fadeIn(animationSpec = tween(durationMillis = 160)) + expandVertically(),
            exit = fadeOut(animationSpec = tween(durationMillis = 120)) + shrinkVertically(),
        ) {
            Column {
                Spacer(Modifier.height(d.spaceM))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountTypeDropdown(
    selectedType: AccountType,
    onTypeSelected: (AccountType) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.edit_user_account_type_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = d.spaceXS),
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = !expanded },
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = MaterialTheme.shapes.medium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(d.spaceM),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = accountTypeLabel(selectedType),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_user_account_type_pharmacy)) },
                    onClick = {
                        onTypeSelected(AccountType.PHARMACY)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_user_account_type_warehouse)) },
                    onClick = {
                        onTypeSelected(AccountType.WAREHOUSE)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_user_account_type_admin)) },
                    onClick = {
                        onTypeSelected(AccountType.ADMIN)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_user_account_type_public)) },
                    onClick = {
                        onTypeSelected(AccountType.PUBLIC_USER)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun accountTypeLabel(type: AccountType): String {
    return when (type) {
        AccountType.PHARMACY -> stringResource(R.string.edit_user_account_type_pharmacy)
        AccountType.WAREHOUSE -> stringResource(R.string.edit_user_account_type_warehouse)
        AccountType.ADMIN -> stringResource(R.string.edit_user_account_type_admin)
        AccountType.PUBLIC_USER -> stringResource(R.string.edit_user_account_type_public)
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun PreviewEditUserBottomSheet() {
    PharmaTheme {
        EditUserBottomSheetContent(
            state = EditUserUiState(
                userId = "user-123",
                fullName = "أحمد محمود",
                accountType = AccountType.PHARMACY,
                facilityId = "550e8400-e29b-41d4-a716-446655440000",
                isActive = true,
            ),
            onAction = {},
        )
    }
}
