package com.pharmalink.feature.orders

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
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import com.pharmalink.domain.model.PublicPharmacyAvailabilityStatus

@Composable
fun PharmacySelectionScreen(
    medicineId: String,
    medicineName: String,
    medicineBrand: String,
    medicineStrength: String,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onSelectPharmacy: (PharmacySummaryUi) -> Unit,
    onSearchAllPharmacies: () -> Unit,
    viewModel: PharmacySelectionViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(medicineId, medicineName, medicineBrand, medicineStrength) {
        viewModel.initialize(
            medicineId = medicineId,
            medicineName = medicineName,
            medicineBrand = medicineBrand,
            medicineStrength = medicineStrength,
        )
    }

    PharmacySelectionContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRetryClick = {
            viewModel.retry()
            onRetryClick()
        },
        onSelectPharmacy = onSelectPharmacy,
        onSearchAllPharmacies = onSearchAllPharmacies,
        onFilterSelected = viewModel::selectFilter,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PharmacySelectionContent(
    uiState: PharmacySelectionUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onSelectPharmacy: (PharmacySummaryUi) -> Unit,
    onSearchAllPharmacies: () -> Unit,
    onFilterSelected: (PharmacySelectionFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier,
            containerColor = ClinicalCanvas,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.customer_order_pharmacy_selection_question),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.customer_order_back),
                            )
                        }
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
                    OrderMessageState(
                        title = stringResource(R.string.customer_order_generic_error_title),
                        message = stringResource(R.string.customer_order_generic_error_body),
                        actionLabel = stringResource(R.string.order_retry_loading),
                        onAction = onRetryClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(d.spaceL),
                    )
                }

                uiState.pharmacies.isEmpty() -> {
                    OrderMessageState(
                        title = stringResource(R.string.customer_order_pharmacy_empty_title),
                        message = stringResource(R.string.customer_order_pharmacy_empty_body),
                        actionLabel = stringResource(R.string.order_retry_loading),
                        onAction = onRetryClick,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(d.spaceL),
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(d.spaceL),
                        verticalArrangement = Arrangement.spacedBy(d.spaceL),
                    ) {
                        item {
                            MedicineSummaryCard(medicine = uiState.medicine)
                        }
                        item {
                            InfoBanner(
                                title = stringResource(R.string.customer_order_select_pharmacy_title),
                                body = stringResource(R.string.customer_order_confirmation_notice),
                            )
                        }
                        item {
                            PharmacyFilterRow(
                                selectedFilter = uiState.selectedFilter,
                                onFilterSelected = onFilterSelected,
                            )
                        }
                        item {
                            PharmaButton(
                                text = stringResource(R.string.customer_order_search_all_action),
                                onClick = onSearchAllPharmacies,
                            )
                        }
                        if (uiState.visiblePharmacies.isEmpty()) {
                            item {
                                EmptyFilteredPharmacies()
                            }
                        }
                        items(uiState.visiblePharmacies, key = { it.pharmacyId }) { pharmacy ->
                            PharmacySelectionCard(
                                pharmacy = pharmacy,
                                onAction = {
                                    onSelectPharmacy(
                                        PharmacySummaryUi(
                                            id = pharmacy.pharmacyId,
                                            name = pharmacy.pharmacyName,
                                            locationLabel = pharmacy.locationLabel,
                                            supportsPickup = pharmacy.supportsPickup,
                                            supportsDelivery = pharmacy.supportsDelivery,
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PharmacyFilterRow(
    selectedFilter: PharmacySelectionFilter,
    onFilterSelected: (PharmacySelectionFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceS),
    ) {
        PharmacyFilterChip(
            label = stringResource(R.string.customer_order_filter_nearby),
            selected = selectedFilter == PharmacySelectionFilter.NEARBY,
            onClick = { onFilterSelected(PharmacySelectionFilter.NEARBY) },
        )
        PharmacyFilterChip(
            label = stringResource(R.string.customer_order_filter_on_duty),
            selected = selectedFilter == PharmacySelectionFilter.ON_DUTY,
            onClick = { onFilterSelected(PharmacySelectionFilter.ON_DUTY) },
        )
        PharmacyFilterChip(
            label = stringResource(R.string.customer_order_filter_all),
            selected = selectedFilter == PharmacySelectionFilter.ALL,
            onClick = { onFilterSelected(PharmacySelectionFilter.ALL) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PharmacyFilterChip(
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
private fun MedicineSummaryCard(
    medicine: MedicineSummaryUi,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = d.cardElevation,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = PharmaBlue50) {
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
    }
}

@Composable
private fun PharmacySelectionCard(
    pharmacy: PharmacySelectionItemUi,
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
            Column(verticalArrangement = Arrangement.spacedBy(d.spaceXS)) {
                Text(
                    text = pharmacy.pharmacyName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(d.spaceXS),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = pharmacy.locationLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!pharmacy.distanceLabel.isNullOrBlank() || !pharmacy.estimatedTimeLabel.isNullOrBlank()) {
                    Text(
                        text = listOfNotNull(pharmacy.distanceLabel, pharmacy.estimatedTimeLabel).joinToString(" - "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                ServiceTag(
                    label = pharmacy.availabilityStatus.toCustomerAvailabilityLabel(),
                    isVisible = true,
                )
            }
            PharmaButton(
                text = stringResource(R.string.customer_order_select_pharmacy_action),
                onClick = onAction,
            )
        }
    }
}

@Composable
private fun PublicPharmacyAvailabilityStatus.toCustomerAvailabilityLabel(): String = when (this) {
    PublicPharmacyAvailabilityStatus.AVAILABLE -> stringResource(R.string.customer_order_availability_available)
    PublicPharmacyAvailabilityStatus.NEEDS_CONFIRMATION -> stringResource(R.string.customer_order_availability_needs_confirmation)
    PublicPharmacyAvailabilityStatus.UNKNOWN -> stringResource(R.string.customer_order_availability_unknown)
}

@Composable
private fun EmptyFilteredPharmacies(modifier: Modifier = Modifier) {
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
internal fun ServiceTag(
    label: String,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return

    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceS),
        )
    }
}

@Composable
private fun OrderMessageState(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Box(
        modifier = modifier.background(ClinicalCanvas),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
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
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}
