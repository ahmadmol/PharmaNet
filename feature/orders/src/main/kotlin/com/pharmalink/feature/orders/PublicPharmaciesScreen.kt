package com.pharmalink.feature.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.pharmalink.designsystem.theme.dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicPharmaciesScreen(
    viewModel: PublicPharmaciesViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = ClinicalCanvas,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.public_pharmacies_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                )
            },
        ) { innerPadding ->
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.errorMessage != null -> {
                    PublicPharmaciesMessageState(
                        title = stringResource(R.string.public_pharmacies_error_title),
                        message = uiState.errorMessage,
                        actionLabel = stringResource(R.string.order_retry_loading),
                        onAction = viewModel::loadPharmacies,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(MaterialTheme.dimens.spaceL),
                    )
                }

                uiState.pharmacies.isEmpty() -> {
                    PublicPharmaciesMessageState(
                        title = stringResource(R.string.public_pharmacies_empty_title),
                        message = stringResource(R.string.public_pharmacies_empty_body),
                        actionLabel = stringResource(R.string.order_retry_loading),
                        onAction = viewModel::loadPharmacies,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(MaterialTheme.dimens.spaceL),
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(MaterialTheme.dimens.spaceL),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceL),
                    ) {
                        item {
                            InfoBanner(
                                title = stringResource(R.string.public_pharmacies_info_title),
                                body = stringResource(R.string.public_pharmacies_info_body),
                            )
                        }
                        item {
                            PharmacyFilterRow(
                                selectedFilter = uiState.selectedFilter,
                                onFilterSelected = viewModel::selectFilter,
                            )
                        }
                        if (uiState.visiblePharmacies.isEmpty()) {
                            item {
                                EmptyFilteredPharmaciesState()
                            }
                        }
                        items(uiState.visiblePharmacies, key = { it.pharmacyId }) { pharmacy ->
                            PublicPharmacyCard(pharmacy = pharmacy)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PharmacyFilterRow(
    selectedFilter: PublicPharmacyFilter,
    onFilterSelected: (PublicPharmacyFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceS),
    ) {
        PublicPharmacyFilterChip(
            label = stringResource(R.string.public_pharmacies_filter_all),
            selected = selectedFilter == PublicPharmacyFilter.ALL,
            onClick = { onFilterSelected(PublicPharmacyFilter.ALL) },
        )
        PublicPharmacyFilterChip(
            label = stringResource(R.string.public_pharmacies_filter_on_duty),
            selected = selectedFilter == PublicPharmacyFilter.ON_DUTY,
            onClick = { onFilterSelected(PublicPharmacyFilter.ON_DUTY) },
        )
        PublicPharmacyFilterChip(
            label = stringResource(R.string.public_pharmacies_filter_available),
            selected = selectedFilter == PublicPharmacyFilter.AVAILABLE,
            onClick = { onFilterSelected(PublicPharmacyFilter.AVAILABLE) },
        )
        PublicPharmacyFilterChip(
            label = stringResource(R.string.public_pharmacies_filter_nearby),
            selected = selectedFilter == PublicPharmacyFilter.NEARBY,
            onClick = { onFilterSelected(PublicPharmacyFilter.NEARBY) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublicPharmacyFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun PublicPharmacyCard(
    pharmacy: PublicPharmacyItemUi,
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
                Icon(
                    imageVector = Icons.Outlined.LocalPharmacy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(d.spaceXS),
                ) {
                    Text(
                        text = pharmacy.pharmacyName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (pharmacy.locationLabel.isNotBlank()) {
                        Text(
                            text = pharmacy.locationLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(d.spaceS)) {
                ServiceTag(
                    label = stringResource(R.string.customer_order_on_duty),
                    isVisible = pharmacy.isOnDuty,
                )
                ServiceTag(
                    label = stringResource(R.string.customer_order_pickup_option),
                    isVisible = pharmacy.supportsPickup,
                )
                ServiceTag(
                    label = stringResource(R.string.customer_order_delivery_available),
                    isVisible = pharmacy.supportsDelivery,
                )
            }
        }
    }
}

@Composable
private fun EmptyFilteredPharmaciesState(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = stringResource(R.string.customer_order_filtered_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(MaterialTheme.dimens.spaceL),
        )
    }
}

@Composable
private fun PublicPharmaciesMessageState(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Icon(
                imageVector = Icons.Outlined.LocalPharmacy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            PharmaButton(
                text = actionLabel,
                onClick = onAction,
            )
        }
    }
}
