package com.pharmalink.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.PharmaNeutral100
import com.pharmalink.designsystem.theme.PharmaNeutral400
import com.pharmalink.designsystem.theme.PharmaNeutral600
import com.pharmalink.designsystem.theme.PharmaNeutral900
import com.pharmalink.designsystem.theme.PremiumPrimary
import com.pharmalink.designsystem.theme.PremiumSecondary
import com.pharmalink.designsystem.theme.PremiumAccent
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.designsystem.R as DsR
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.pharmalink.domain.model.Medicine

@Composable
fun MedicineSearchScreen(
    viewModel: MedicineSearchViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onMedicineSelected: (Medicine) -> Unit,
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val d = MaterialTheme.dimens

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = ClinicalCanvas,
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = d.spaceL),
                verticalArrangement = Arrangement.spacedBy(d.spaceM)
            ) {
                SearchHeader(onBackClick = onBackClick)
                
                MedicineSearchContent(
                    uiState = uiState,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onRetryClick = viewModel::loadMedicines,
                    onMedicineSelected = onMedicineSelected,
                )
            }
        }
    }
}

@Composable
private fun SearchHeader(onBackClick: () -> Unit) {
    val d = MaterialTheme.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .padding(vertical = d.spaceS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Leading: Back + Logo
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(d.spaceS)) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PremiumPrimary
                )
            }
            androidx.compose.foundation.Image(
                painter = painterResource(id = DsR.drawable.ic_app_logo),
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )
        }

        Text(
            text = stringResource(id = DsR.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = PremiumPrimary,
        )

        // Trailing: Profile
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = PharmaNeutral100,
            border = BorderStroke(1.dp, PharmaNeutral400.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.padding(10.dp),
                tint = PharmaNeutral600
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
) {
    val d = MaterialTheme.dimens

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(d.spaceL),
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "ابحث باسم الدواء...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PharmaNeutral400,
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = PharmaNeutral400,
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(d.radiusL),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = PharmaNeutral100,
                unfocusedBorderColor = PharmaNeutral100,
            ),
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = PremiumPrimary)
                }
            }

            uiState.searchQuery.isBlank() && uiState.allMedicines.isEmpty() -> {
                RecentSearchesSection()
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
                    modifier = Modifier.weight(1f),
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
                
                RecentSearchesSection()
            }
        }
    }
}

@Composable
private fun RecentSearchesSection() {
    val d = MaterialTheme.dimens
    val popularSearches = listOf("أسبيرين", "فيتامين سي", "أومينتين", "أنسولين")
    
    Column(verticalArrangement = Arrangement.spacedBy(d.spaceM)) {
        Text(
            text = "عمليات البحث الشائعة",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = PharmaNeutral900
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(d.spaceS)) {
            items(popularSearches) { search ->
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, PharmaNeutral100)
                ) {
                    Text(
                        text = search,
                        modifier = Modifier.padding(horizontal = d.spaceL, vertical = d.spaceS),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PharmaNeutral600
                    )
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
                // Medicine Image Placeholder
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(PharmaNeutral100, RoundedCornerShape(d.radiusM)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalPharmacy,
                        contentDescription = null,
                        tint = PharmaNeutral400,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PremiumSecondary.copy(alpha = 0.1f),
                        contentColor = PremiumSecondary
                    ) {
                        Text(
                            text = "متاح",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = "${medicine.name} (${medicine.brand})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PharmaNeutral900,
                    )
                    Text(
                        text = "العلامة التجارية: ${medicine.brand}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PharmaNeutral600,
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = PharmaNeutral100,
                    ) {
                        Text(
                            text = "${medicine.strength} - ${medicine.brand}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = PharmaNeutral600,
                        )
                    }
                }
            }

            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(d.radiusL),
                colors = ButtonDefaults.buttonColors(containerColor = PremiumPrimary)
            ) {
                Text(
                    text = "اختر هذا الدواء",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
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
