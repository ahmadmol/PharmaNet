package com.pharmalink.feature.admin.ui.inventory

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.pharmalink.designsystem.theme.PharmaWarning
import com.pharmalink.designsystem.theme.StatusActive
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
import com.pharmalink.feature.admin.ui.components.AdminProfileAvatarIcon
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseInventoryScreen(
    onBackClick: () -> Unit,
    onAddMedicine: () -> Unit,
    onEditMedicine: (String) -> Unit,
    profileImageUrl: String? = null,
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
            is WarehouseInventoryEffect.NavigateToEditMedicine -> onEditMedicine(effect.medicineId)
        }
    }

    WarehouseInventoryContent(
        state = state,
        onAction = viewModel::onAction,
        profileImageUrl = profileImageUrl,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WarehouseInventoryContent(
    state: WarehouseInventoryUiState,
    onAction: (WarehouseInventoryAction) -> Unit,
    profileImageUrl: String? = null,
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
                    actions = {
                        AdminProfileAvatarIcon(
                            profileImageUrl = profileImageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            fallbackSize = 24.dp,
                            fallbackTint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(d.spaceM))
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
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
            state.totalItems == 0 -> EmptyContent(modifier = Modifier.padding(padding))
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(d.spaceS),
            ) {
                SearchField(
                    value = state.searchQuery,
                    onValueChange = { onAction(WarehouseInventoryAction.OnSearchQueryChanged(it)) },
                    placeholder = stringResource(R.string.warehouse_inventory_search_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                )
                InventoryFilterChips(
                    selectedFilter = state.selectedFilter,
                    onFilterSelected = { onAction(WarehouseInventoryAction.OnFilterSelected(it)) },
                )
            }
        }

        if (state.medicines.isEmpty()) {
            item {
                PharmaStateView(
                    title = "لا توجد منتجات مطابقة",
                    subtitle = "جرّب تغيير البحث أو اختيار فلتر آخر.",
                    tone = PharmaStateTone.Neutral,
                )
            }
        } else {
            items(
                items = state.medicines,
                key = { it.id },
            ) { medicine ->
                MedicineInventoryCard(
                    medicine = medicine,
                    onClick = { onAction(WarehouseInventoryAction.OnMedicineClicked(medicine.medicineId)) }
                )
            }
        }
    }
}

@Composable
private fun InventoryFilterChips(
    selectedFilter: InventoryProductFilter,
    onFilterSelected: (InventoryProductFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val filters = listOf(
        InventoryProductFilter.ALL to "الكل",
        InventoryProductFilter.AVAILABLE to "متوفر",
        InventoryProductFilter.LOW_STOCK to "منخفض",
        InventoryProductFilter.HIDDEN to "مخفي",
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(d.spaceS),
    ) {
        items(filters) { (filter, label) ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(d.radiusXL),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceM),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                verticalAlignment = Alignment.Top,
            ) {
                ProductImageThumb(
                    imageUrl = medicine.imageUrl,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(d.spaceXS),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = medicine.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = medicine.description.ifBlank { "\u0644\u0627 \u062a\u0648\u062c\u062f \u062a\u0641\u0627\u0635\u064a\u0644 \u0625\u0636\u0627\u0641\u064a\u0629" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                StockStatusChip(stockStatus = medicine.stockStatus)
            }

            ProductManagementMeta(
                priceLabel = medicine.priceLabel,
                isVisible = medicine.isVisible,
                isActive = medicine.isActive,
            )

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
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
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
                        StockStatus.IN_STOCK -> StatusActive
                        StockStatus.LOW_STOCK -> PharmaWarning
                        StockStatus.OUT_OF_STOCK -> MaterialTheme.colorScheme.error
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StockStatusChip(
    stockStatus: StockStatus,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val containerColor = when (stockStatus) {
        StockStatus.IN_STOCK -> StatusActive.copy(alpha = 0.12f)
        StockStatus.LOW_STOCK -> PharmaWarning.copy(alpha = 0.15f)
        StockStatus.OUT_OF_STOCK -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f)
    }
    val contentColor = when (stockStatus) {
        StockStatus.IN_STOCK -> StatusActive
        StockStatus.LOW_STOCK -> PharmaWarning
        StockStatus.OUT_OF_STOCK -> MaterialTheme.colorScheme.error
    }
    val label = when (stockStatus) {
        StockStatus.IN_STOCK -> "\u0645\u062a\u0648\u0641\u0631"
        StockStatus.LOW_STOCK -> stringResource(R.string.warehouse_inventory_low_stock)
        StockStatus.OUT_OF_STOCK -> stringResource(R.string.warehouse_inventory_out_of_stock)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(d.radiusM),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = d.spaceS, vertical = d.spaceXS),
        )
    }
}

@Composable
private fun ProductManagementMeta(
    priceLabel: String?,
    isVisible: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(d.spaceS),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            ProductMetaChip(
                label = priceLabel ?: "\u0627\u0644\u0633\u0639\u0631 \u063a\u064a\u0631 \u0645\u062d\u062f\u062f",
                modifier = Modifier.weight(1f),
            )
            ProductMetaChip(
                label = if (isVisible) "\u0638\u0627\u0647\u0631 \u0644\u0644\u0635\u064a\u062f\u0644\u064a\u0627\u062a" else "\u0645\u062e\u0641\u064a",
                modifier = Modifier.weight(1f),
            )
        }
        ProductMetaChip(
            label = if (isActive) "\u0646\u0634\u0637" else "\u063a\u064a\u0631 \u0646\u0634\u0637",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ProductMetaChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(d.radiusM),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = d.spaceS, vertical = d.spaceXS),
        )
    }
}

@Composable
private fun ProductImageThumb(
    imageUrl: String,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.size(76.dp),
        shape = RoundedCornerShape(d.radiusL),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(d.iconL),
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "ar")
@Preview(showBackground = true)
@Composable
private fun PreviewWarehouseInventoryScreen() {
    PharmaTheme {
        WarehouseInventoryContent(
            state = WarehouseInventoryUiState(
                warehouseName = "ظ…ط³طھظˆط¯ط¹ ط§ظ„ط±ظٹط§ط¶ ط§ظ„ظ…ط±ظƒط²ظٹ",
                totalItems = 1284,
                capacityPercent = 94,
                lastUpdated = "ظ…ظ†ط° ط¯ظ‚ظٹظ‚طھظٹظ†",
                medicines = listOf(
                    MedicineInventoryModel(
                        id = "1",
                        name = "ط¨ط§ط±ط§ط³ظٹطھط§ظ…ظˆظ„ 500 ظ…ظ„ط؛",
                        description = "ظ…ط³ظƒظ† ظ„ظ„ط£ظ„ظ… ظˆط®ط§ظپط¶ ظ„ظ„ط­ط±ط§ط±ط©",
                        currentQuantity = 850,
                        capacity = 1000,
                        unit = "ط¹ظ„ط¨ط©",
                        stockStatus = StockStatus.IN_STOCK,
                    ),
                    MedicineInventoryModel(
                        id = "2",
                        name = "ط£ظ…ظˆظƒط³ظٹط³ظٹظ„ظٹظ† 250 ظ…ظ„ط؛",
                        description = "ظ…ط¶ط§ط¯ ط­ظٹظˆظٹ ظˆط§ط³ط¹ ط§ظ„ط·ظٹظپ",
                        currentQuantity = 120,
                        capacity = 500,
                        unit = "ط¹ظ„ط¨ط©",
                        stockStatus = StockStatus.LOW_STOCK,
                    ),
                    MedicineInventoryModel(
                        id = "3",
                        name = "ط£ظˆظ…ظٹط¨ط±ط§ط²ظˆظ„ 20 ظ…ظ„ط؛",
                        description = "ظ„ط¹ظ„ط§ط¬ ط­ظ…ظˆط¶ط© ط§ظ„ظ…ط¹ط¯ط©",
                        currentQuantity = 0,
                        capacity = 300,
                        unit = "ط¹ظ„ط¨ط©",
                        stockStatus = StockStatus.OUT_OF_STOCK,
                    ),
                ),
            ),
            onAction = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
