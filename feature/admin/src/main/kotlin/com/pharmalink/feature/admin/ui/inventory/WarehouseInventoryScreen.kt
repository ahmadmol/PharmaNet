package com.pharmalink.feature.admin.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.designsystem.components.PharmaCard
import com.pharmalink.designsystem.components.PharmaSkeletonLine
import com.pharmalink.designsystem.components.PharmaStateView
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.theme.PharmaTheme
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.designsystem.utils.CollectEffect
import com.pharmalink.feature.admin.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseInventoryScreen(
    onBackClick: () -> Unit,
    onAddMedicine: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WarehouseInventoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    CollectEffect(effect = viewModel.effect) { effect ->
        when (effect) {
            is WarehouseInventoryEffect.ShowMessage -> {
                snackbarHostState.showSnackbar(effect.message)
            }
            WarehouseInventoryEffect.NavigateBack -> onBackClick()
            WarehouseInventoryEffect.NavigateToAddMedicine -> onAddMedicine()
            is WarehouseInventoryEffect.NavigateToMedicineDetail -> {
                snackbarHostState.showSnackbar("تفاصيل الدواء: قيد التطوير")
            }
            is WarehouseInventoryEffect.NavigateToEditInventory -> {
                snackbarHostState.showSnackbar("تعديل المخزون: قيد التطوير")
            }
            is WarehouseInventoryEffect.ShowDeleteConfirmation -> {
                snackbarHostState.showSnackbar("حذف ${effect.medicineName}: قيد التطوير")
            }
        }
    }

    WarehouseInventoryContent(
        state = state,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WarehouseInventoryContent(
    state: WarehouseInventoryUiState,
    onAction: (WarehouseInventoryAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.warehouse_inventory_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { onAction(WarehouseInventoryAction.OnBackClicked) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White,
                    ),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        floatingActionButton = {
            if (!state.isLoading && state.contentError.isEmpty()) {
                FloatingActionButton(
                    onClick = { onAction(WarehouseInventoryAction.OnAddMedicineClicked) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.warehouse_inventory_add_medicine_cd),
                    )
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingContent(modifier = Modifier.padding(padding))
            state.contentError.isNotEmpty() -> ErrorContent(
                message = state.contentError,
                onRetry = { onAction(WarehouseInventoryAction.OnRetryClicked) },
                modifier = Modifier.padding(padding),
            )
            state.medicines.isEmpty() -> EmptyContent(modifier = Modifier.padding(padding))
            else -> SuccessContent(
                state = state,
                onAction = onAction,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(d.spaceL),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        PharmaSkeletonLine(heightDp = 120f)
        PharmaSkeletonLine(heightDp = 60f)
        repeat(5) {
            PharmaSkeletonLine(heightDp = 140f)
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(d.spaceL),
    ) {
        PharmaStateView(
            title = stringResource(R.string.warehouse_inventory_error),
            subtitle = message,
            tone = PharmaStateTone.Error,
            actionLabel = stringResource(R.string.admin_users_retry),
            onAction = onRetry,
        )
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(d.spaceL),
    ) {
        PharmaStateView(
            title = stringResource(R.string.warehouse_inventory_empty),
            subtitle = stringResource(R.string.warehouse_inventory_empty_subtitle),
            tone = PharmaStateTone.Neutral,
        )
    }
}

@Composable
private fun SuccessContent(
    state: WarehouseInventoryUiState,
    onAction: (WarehouseInventoryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(d.spaceL),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        // Warehouse Summary
        item {
            WarehouseSummaryCard(
                warehouseName = state.warehouseName,
                totalItems = state.totalItems,
                capacityPercent = state.capacityPercent,
                lastUpdated = state.lastUpdated,
            )
        }

        // Search and Filter Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceS),
            ) {
                SearchField(
                    value = state.searchQuery,
                    onValueChange = { onAction(WarehouseInventoryAction.OnSearchQueryChanged(it)) },
                    placeholder = stringResource(R.string.warehouse_inventory_search_placeholder),
                    modifier = Modifier.weight(1f),
                )
                
                IconButton(
                    onClick = { onAction(WarehouseInventoryAction.OnFilterClicked) },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.admin_filter_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Medicine Cards
        items(
            items = state.medicines,
            key = { it.id },
        ) { medicine ->
            MedicineInventoryCard(
                medicine = medicine,
                onCardClick = { onAction(WarehouseInventoryAction.OnMedicineClicked(medicine.id)) },
            )
        }
    }
}

@Composable
private fun WarehouseSummaryCard(
    warehouseName: String,
    totalItems: Int,
    capacityPercent: Int,
    lastUpdated: String,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    PharmaCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        elevationDp = 2f,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(d.spaceXS),
                ) {
                    Text(
                        text = warehouseName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.warehouse_inventory_last_updated, lastUpdated),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceXL),
            ) {
                Column {
                    Text(
                        text = totalItems.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.warehouse_inventory_total_items),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                
                Column {
                    Text(
                        text = "$capacityPercent%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.warehouse_inventory_capacity),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(56.dp),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        singleLine = true,
    )
}

@Composable
private fun MedicineInventoryCard(
    medicine: MedicineInventoryModel,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onCardClick,
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(d.spaceXS),
                ) {
                    Text(
                        text = medicine.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = medicine.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                
                // Stock Status Badge
                if (medicine.stockStatus != StockStatus.IN_STOCK) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when (medicine.stockStatus) {
                            StockStatus.LOW_STOCK -> Color(0xFFFBBF24).copy(alpha = 0.15f)
                            StockStatus.OUT_OF_STOCK -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            else -> Color.Transparent
                        },
                    ) {
                        Text(
                            text = when (medicine.stockStatus) {
                                StockStatus.LOW_STOCK -> stringResource(R.string.warehouse_inventory_low_stock)
                                StockStatus.OUT_OF_STOCK -> stringResource(R.string.warehouse_inventory_out_of_stock)
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = when (medicine.stockStatus) {
                                StockStatus.LOW_STOCK -> Color(0xFFFBBF24)
                                StockStatus.OUT_OF_STOCK -> MaterialTheme.colorScheme.error
                                else -> Color.Transparent
                            },
                            modifier = Modifier.padding(
                                horizontal = d.spaceS,
                                vertical = d.spaceXS,
                            ),
                        )
                    }
                }
            }
            
            // Quantity and Progress
            Column(
                verticalArrangement = Arrangement.spacedBy(d.spaceS),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(
                            R.string.warehouse_inventory_quantity_format,
                            medicine.currentQuantity,
                            medicine.unit,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            R.string.warehouse_inventory_capacity_format,
                            medicine.capacity,
                            medicine.unit,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                
                LinearProgressIndicator(
                    progress = { 
                        if (medicine.capacity > 0) {
                            medicine.currentQuantity.toFloat() / medicine.capacity
                        } else {
                            0f
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = when (medicine.stockStatus) {
                        StockStatus.IN_STOCK -> Color(0xFF10B981)
                        StockStatus.LOW_STOCK -> Color(0xFFFBBF24)
                        StockStatus.OUT_OF_STOCK -> MaterialTheme.colorScheme.error
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun PreviewWarehouseInventoryScreen() {
    PharmaTheme {
        WarehouseInventoryContent(
            state = WarehouseInventoryUiState(
                warehouseName = "مستودع الرياض المركزي",
                totalItems = 1284,
                capacityPercent = 94,
                lastUpdated = "منذ دقيقتين",
                medicines = listOf(
                    MedicineInventoryModel(
                        id = "1",
                        name = "باراسيتامول 500 ملغ",
                        description = "مسكن للألم وخافض للحرارة",
                        currentQuantity = 850,
                        capacity = 1000,
                        unit = "علبة",
                        stockStatus = StockStatus.IN_STOCK,
                    ),
                    MedicineInventoryModel(
                        id = "2",
                        name = "أموكسيسيلين 250 ملغ",
                        description = "مضاد حيوي واسع الطيف",
                        currentQuantity = 120,
                        capacity = 500,
                        unit = "علبة",
                        stockStatus = StockStatus.LOW_STOCK,
                    ),
                    MedicineInventoryModel(
                        id = "3",
                        name = "أوميبرازول 20 ملغ",
                        description = "لعلاج حموضة المعدة",
                        currentQuantity = 0,
                        capacity = 300,
                        unit = "علبة",
                        stockStatus = StockStatus.OUT_OF_STOCK,
                    ),
                ),
            ),
            onAction = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
