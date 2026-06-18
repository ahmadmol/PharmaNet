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
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.PharmaNeutral100
import com.pharmalink.designsystem.theme.PharmaNeutral400
import com.pharmalink.designsystem.theme.PharmaNeutral600
import com.pharmalink.designsystem.theme.PharmaNeutral900
import com.pharmalink.designsystem.theme.PharmaBlue100
import com.pharmalink.designsystem.theme.PremiumPrimary
import com.pharmalink.designsystem.theme.PremiumSecondary
import com.pharmalink.designsystem.theme.PremiumAccent
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.designsystem.R as DsR
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.IconButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember

@Composable
fun PublicPharmaciesScreen(
    viewModel: PublicPharmaciesViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val d = MaterialTheme.dimens

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = ClinicalCanvas,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { /* No-op per Rule 9 */ },
                    containerColor = PremiumPrimary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Map, contentDescription = "Map")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = d.spaceL)
            ) {
                PharmaciesHeader()
                
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = PremiumPrimary)
                        }
                    }

                    uiState.errorMessage != null -> {
                        PublicPharmaciesMessageState(
                            title = stringResource(R.string.public_pharmacies_error_title),
                            message = uiState.errorMessage,
                            actionLabel = stringResource(R.string.order_retry_loading),
                            onAction = viewModel::loadPharmacies,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = d.spaceM),
                            verticalArrangement = Arrangement.spacedBy(d.spaceM),
                        ) {
                            item {
                                PharmaciesHeroBanner()
                            }
                            item {
                                PharmacyFilterRow(
                                    selectedFilter = uiState.selectedFilter,
                                    onFilterSelected = viewModel::selectFilter,
                                )
                            }
                            if (uiState.visiblePharmacies.isEmpty() && uiState.pharmacies.isNotEmpty()) {
                                item {
                                    EmptyFilteredPharmaciesState()
                                }
                            } else if (uiState.pharmacies.isEmpty()) {
                                item {
                                    PublicPharmaciesMessageState(
                                        title = stringResource(R.string.public_pharmacies_empty_title),
                                        message = stringResource(R.string.public_pharmacies_empty_body),
                                        actionLabel = stringResource(R.string.order_retry_loading),
                                        onAction = viewModel::loadPharmacies,
                                    )
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
}

@Composable
private fun PharmaciesHeader() {
    val d = MaterialTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = d.spaceM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = PharmaNeutral100,
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.padding(8.dp),
                tint = PharmaNeutral600
            )
        }

        Text(
            text = "PharmaNet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PremiumPrimary,
        )

        IconButton(onClick = { /* No-op per Rule 9 */ }) {
            Icon(
                painter = painterResource(id = DsR.drawable.ic_app_logo),
                contentDescription = null,
                modifier = Modifier.size(d.iconM),
                tint = Color.Unspecified
            )
        }
    }
}

@Composable
private fun PharmaciesHeroBanner() {
    val d = MaterialTheme.dimens
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(d.radiusXXL))
            .background(PharmaGradients.headerBlueToGreen)
            .padding(d.spaceL),
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "الصيدليات المتاحة",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "اعثر على أقرب صيدلية لخدمتكم الآن",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
        
        Icon(
            imageVector = Icons.Outlined.LocalPharmacy,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.15f),
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.CenterEnd)
                .graphicsLayer { translationX = 20f }
        )
    }
}

@Composable
private fun PharmacyFilterRow(
    selectedFilter: PublicPharmacyFilter,
    onFilterSelected: (PublicPharmacyFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = listOf(
        PublicPharmacyFilter.ALL to "الكل",
        PublicPharmacyFilter.ON_DUTY to "المناوبة",
        PublicPharmacyFilter.AVAILABLE to "المتاحة",
        PublicPharmacyFilter.NEARBY to "القريبة",
    )
    
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceS),
    ) {
        items(filters) { (filter, label) ->
            Surface(
                onClick = { onFilterSelected(filter) },
                shape = CircleShape,
                color = if (selectedFilter == filter) PremiumPrimary.copy(alpha = 0.1f) else Color.White,
                border = BorderStroke(1.dp, if (selectedFilter == filter) PremiumPrimary else PharmaNeutral100),
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedFilter == filter) PremiumPrimary else PharmaNeutral600,
                    fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
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
        shape = RoundedCornerShape(d.radiusXL),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, PharmaNeutral100)
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Pharmacy Icon Placeholder
                Surface(
                    shape = RoundedCornerShape(d.radiusM),
                    color = PharmaBlue50,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.LocalPharmacy,
                            contentDescription = null,
                            tint = PremiumPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = (if (pharmacy.isOnDuty) PremiumSecondary else PremiumUrgent).copy(alpha = 0.1f),
                        contentColor = if (pharmacy.isOnDuty) PremiumSecondary else PremiumUrgent
                    ) {
                        Text(
                            text = if (pharmacy.isOnDuty) "مناوب" else "مغلق حالياً",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = pharmacy.pharmacyName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PharmaNeutral900,
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = PharmaNeutral400)
                        Text(
                            text = pharmacy.locationLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = PharmaNeutral600,
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp), tint = PremiumAccent)
                        Text(
                            text = "4.8",
                            style = MaterialTheme.typography.labelSmall,
                            color = PharmaNeutral600,
                        )
                        pharmacy.distanceLabel?.let {
                            Text(
                                text = "• تبعد $it",
                                style = MaterialTheme.typography.labelSmall,
                                color = PharmaNeutral600,
                            )
                        }
                        pharmacy.estimatedTimeLabel?.let {
                            Text(
                                text = "• $it",
                                style = MaterialTheme.typography.labelSmall,
                                color = PharmaNeutral600,
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { /* No-op per Rule 9 */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(d.radiusL),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumPrimary)
            ) {
                Text(
                    text = "عرض المنتجات",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
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
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = PharmaNeutral400,
                modifier = Modifier.size(72.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PharmaNeutral900,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = PharmaNeutral600,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(d.radiusL),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumPrimary)
            ) {
                Text(actionLabel, modifier = Modifier.padding(horizontal = d.spaceL))
            }
        }
    }
}
