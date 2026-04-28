package com.pharmalink.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaBlue500
import com.pharmalink.designsystem.theme.PharmaNeutral100
import com.pharmalink.designsystem.theme.PharmaNeutral600
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.Medicine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineSearchScreen(
    viewModel: MedicineSearchViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToWarehouseDetail: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val d = MaterialTheme.dimens

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "البحث عن دواء",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ClinicalCanvas,
                    ),
                )
            },
            containerColor = ClinicalCanvas,
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = d.spaceL),
            ) {
                // Search Input
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = d.spaceM),
                    placeholder = {
                        Text(
                            text = "ابحث عن اسم الدواء أو النوع...",
                            color = PharmaNeutral600,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = PharmaBlue500,
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(d.radiusXXL),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = PharmaBlue500,
                        unfocusedBorderColor = PharmaNeutral100,
                    ),
                )

                Spacer(modifier = Modifier.height(d.spaceM))

                // Results
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = PharmaBlue500)
                        }
                    }
                    uiState.errorMessage != null -> {
                        ErrorState(
                            message = uiState.errorMessage!!,
                            onRetry = viewModel::loadMedicines,
                        )
                    }
                    uiState.medicines.isEmpty() && uiState.searchQuery.isBlank() -> {
                        EmptyState(
                            message = "ابدأ بالبحث عن اسم الدواء",
                            subMessage = "ستظهر الأدوية المتوفرة في المستودعات",
                        )
                    }
                    uiState.medicines.isEmpty() -> {
                        EmptyState(
                            message = "لا توجد نتائج",
                            subMessage = "جرب اسم دواء آخر أو تحقق من الإملاء",
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = d.spaceM),
                            verticalArrangement = Arrangement.spacedBy(d.spaceM),
                        ) {
                            items(uiState.medicines) { medicine ->
                                MedicineCard(
                                    medicine = medicine,
                                    onClick = { onNavigateToWarehouseDetail(medicine.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicineCard(
    medicine: Medicine,
    onClick: () -> Unit,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(d.radiusXXL),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Medicine Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = PharmaBlue500.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(d.radiusM),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalPharmacy,
                    contentDescription = null,
                    tint = PharmaBlue500,
                    modifier = Modifier.size(24.dp),
                )
            }

            // Medicine Info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = medicine.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (medicine.brand.isNotBlank()) {
                    Text(
                        text = medicine.brand,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PharmaNeutral600,
                    )
                }
                if (medicine.strength.isNotBlank()) {
                    Text(
                        text = medicine.strength,
                        style = MaterialTheme.typography.bodySmall,
                        color = PharmaNeutral600,
                    )
                }
            }
        }

        // Availability Hint (no order button)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = PharmaBlue500.copy(alpha = 0.05f))
                .padding(horizontal = d.spaceL, vertical = d.spaceM),
        ) {
            Text(
                text = "متوفر في مستودعات →",
                style = MaterialTheme.typography.bodySmall,
                color = PharmaBlue500,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun EmptyState(
    message: String,
    subMessage: String,
) {
    val d = MaterialTheme.dimens

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = PharmaNeutral600,
                modifier = Modifier.size(64.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = PharmaNeutral600,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    val d = MaterialTheme.dimens

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "اضغط للمحاولة مرة أخرى",
                style = MaterialTheme.typography.bodyMedium,
                color = PharmaBlue500,
                modifier = Modifier.clickable(onClick = onRetry),
            )
        }
    }
}
