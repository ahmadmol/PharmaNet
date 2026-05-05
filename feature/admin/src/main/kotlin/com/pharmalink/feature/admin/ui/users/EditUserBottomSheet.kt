package com.pharmalink.feature.admin.ui.users

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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaSwitch
import com.pharmalink.designsystem.components.PharmaTextField
import com.pharmalink.designsystem.theme.PharmaTheme
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.designsystem.utils.CollectEffect
import com.pharmalink.domain.model.AccountType

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
    modifier: Modifier = Modifier,
    viewModel: EditUserViewModel = hiltViewModel(),
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        viewModel.loadUser(userId, fullName, accountType, facilityId, isActive)
    }

    CollectEffect(effect = viewModel.effect) { effect ->
        when (effect) {
            EditUserEffect.Dismiss -> onDismiss()
            is EditUserEffect.ShowMessage -> {
                snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

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
        AccountType.PHARMACY -> "معرف الصيدلية"
        AccountType.WAREHOUSE -> "معرف المستودع"
        else -> ""
    }
    val facilityPlaceholder = when (state.accountType) {
        AccountType.PHARMACY,
        AccountType.WAREHOUSE -> "550e8400-e29b-41d4-a716-446655440000"
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
                text = "طھط¹ط¯ظٹظ„ ط¨ظٹط§ظ†ط§طھ ط§ظ„ظ…ط³طھط®ط¯ظ…",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            IconButton(onClick = { onAction(EditUserAction.OnDismiss) }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "ط¥ط؛ظ„ط§ظ‚",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(d.spaceL))

        PharmaTextField(
            value = state.fullName,
            onValueChange = { onAction(EditUserAction.OnFullNameChanged(it)) },
            label = "ط§ظ„ط§ط³ظ… ط§ظ„ظƒط§ظ…ظ„",
            placeholder = "ط£ط¯ط®ظ„ ط§ظ„ط§ط³ظ… ط§ظ„ظƒط§ظ…ظ„",
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

        if (showFacilityField) {
            PharmaTextField(
                value = state.facilityId,
                onValueChange = { onAction(EditUserAction.OnFacilityIdChanged(it)) },
                label = facilityLabel,
                placeholder = facilityPlaceholder,
                errorMessage = state.facilityIdError,
                enabled = !state.isSaving,
            )

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
                        text = "ط­ط§ظ„ط© ط§ظ„ط­ط³ط§ط¨",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(d.spaceXS))
                    Text(
                        text = "ط§ظ„ط³ظ…ط§ط­ ظ„ظ„ظ…ط³طھط®ط¯ظ… ط¨ط§ظ„ظˆطµظˆظ„ ط¥ظ„ظ‰ ط§ظ„ظ†ط¸ط§ظ…",
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
            text = if (state.isSaving) "ط¬ط§ط±ظٹ ط§ظ„ط­ظپط¸..." else "ط­ظپط¸ ط§ظ„طھط؛ظٹظٹط±ط§طھ",
            onClick = { onAction(EditUserAction.OnSaveClicked) },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.isSaving) {
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
            text = "ظ†ظˆط¹ ط§ظ„ط­ط³ط§ط¨",
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
                        text = when (selectedType) {
                            AccountType.PHARMACY -> "ظ…ط¯ظٹط± طµظٹط¯ظ„ظٹط©"
                            AccountType.WAREHOUSE -> "ظ…ط¯ظٹط± ظ…ط³طھظˆط¯ط¹"
                            AccountType.ADMIN -> "ظ…ط³ط¤ظˆظ„ ظ†ط¸ط§ظ…"
                            AccountType.PUBLIC_USER -> "ظ…ط³طھط®ط¯ظ… ط¹ط§ظ…"
                        },
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
                    text = { Text("ظ…ط¯ظٹط± طµظٹط¯ظ„ظٹط©") },
                    onClick = {
                        onTypeSelected(AccountType.PHARMACY)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
                DropdownMenuItem(
                    text = { Text("ظ…ط¯ظٹط± ظ…ط³طھظˆط¯ط¹") },
                    onClick = {
                        onTypeSelected(AccountType.WAREHOUSE)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
                DropdownMenuItem(
                    text = { Text("ظ…ط³ط¤ظˆظ„ ظ†ط¸ط§ظ…") },
                    onClick = {
                        onTypeSelected(AccountType.ADMIN)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
                DropdownMenuItem(
                    text = { Text("ظ…ط³طھط®ط¯ظ… ط¹ط§ظ…") },
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

@Preview(showBackground = true, locale = "ar")
@Composable
private fun PreviewEditUserBottomSheet() {
    PharmaTheme {
        EditUserBottomSheetContent(
            state = EditUserUiState(
                userId = "user-123",
                fullName = "ط£ط­ظ…ط¯ ظ…ط­ظ…ظˆط¯",
                accountType = AccountType.PHARMACY,
                facilityId = "550e8400-e29b-41d4-a716-446655440000",
                isActive = true,
            ),
            onAction = {},
        )
    }
}
