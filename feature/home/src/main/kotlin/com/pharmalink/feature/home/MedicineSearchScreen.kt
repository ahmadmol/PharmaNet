package com.pharmalink.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.Medicine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineSearchScreen(
    viewModel: MedicineSearchViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onMedicineSelected: (Medicine) -> Unit,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.medicine_search_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.medicine_search_back),
                            )
                        }
                    },
                )
            },
            containerColor = ClinicalCanvas,
        ) { paddingValues ->
            MedicineSearchContent(
                uiState = uiState,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onRetryClick = viewModel::loadMedicines,
                onMedicineSelected = onMedicineSelected,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        }
    }
}

@Composable
private fun MedicineSearchContent(
    uiState: MedicineSearchUiState,
    onSearchQueryChange: (String) -> Unit,
    onRetryClick: () -> Unit,
    onMedicineSelected: (Medicine) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Column(
        modifier = modifier.padding(horizontal = d.spaceL),
        verticalArrangement = Arrangement.spacedBy(d.spaceL),
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = d.spaceM),
            placeholder = {
                Text(
                    text = stringResource(R.string.medicine_search_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )

        Text(
            text = stringResource(R.string.medicine_search_helper_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                SearchMessageState(
                    title = stringResource(R.string.medicine_search_error_title),
                    message = uiState.errorMessage,
                    actionLabel = stringResource(R.string.medicine_search_retry),
                    onAction = onRetryClick,
                )
            }

            uiState.searchQuery.isBlank() && uiState.allMedicines.isEmpty() -> {
                SearchMessageState(
                    title = stringResource(R.string.medicine_search_empty_initial_title),
                    message = stringResource(R.string.medicine_search_empty_initial_body),
                    actionLabel = stringResource(R.string.medicine_search_retry),
                    onAction = onRetryClick,
                )
            }

            uiState.searchQuery.isNotBlank() && uiState.medicines.isEmpty() -> {
                SearchMessageState(
                    title = stringResource(R.string.medicine_search_empty_results_title),
                    message = stringResource(R.string.medicine_search_empty_results_body),
                    actionLabel = stringResource(R.string.medicine_search_retry),
                    onAction = onRetryClick,
                )
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = d.spaceXL),
                    verticalArrangement = Arrangement.spacedBy(d.spaceM),
                ) {
                    items(uiState.medicines, key = { it.id }) { medicine ->
                        MedicineCard(
                            medicine = medicine,
                            onAction = { onMedicineSelected(medicine) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicineCard(
    medicine: Medicine,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = MaterialTheme.shapes.medium, color = PharmaBlue50) {
                    Box(
                        modifier = Modifier.padding(d.spaceM),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocalPharmacy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(d.spaceXS),
                ) {
                    Text(
                        text = medicine.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (medicine.brand.isNotBlank()) {
                        Text(
                            text = medicine.brand,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (medicine.strength.isNotBlank()) {
                        Text(
                            text = medicine.strength,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = stringResource(R.string.medicine_search_safe_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceS),
                )
            }

            PharmaButton(
                text = stringResource(R.string.medicine_search_select_action),
                onClick = onAction,
            )
        }
    }
}

@Composable
private fun SearchMessageState(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = d.cardElevation,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(d.spaceL),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.large,
                        )
                        .clickable(onClick = onAction)
                        .padding(horizontal = d.spaceL, vertical = d.spaceM),
                )
            }
        }
    }
}
