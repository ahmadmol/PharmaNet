package com.pharmalink.feature.pharmacy

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.components.PharmaStateView
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseProductsScreen(
    onBack: () -> Unit,
    onAddToBasket: (WarehouseProductUiModel) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WarehouseProductsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = ClinicalCanvas,
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = "معرض منتجات المستودع",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "رجوع",
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            },
        ) { padding ->
            WarehouseProductsContent(
                uiState = uiState,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onAddToBasket = onAddToBasket,
                onRetry = viewModel::retry,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun WarehouseProductsContent(
    uiState: WarehouseProductsUiState,
    onSearchQueryChanged: (String) -> Unit,
    onAddToBasket: (WarehouseProductUiModel) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.errorMessage != null -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(d.spaceL),
            ) {
                PharmaStateView(
                    title = "تعذر تحميل المنتجات",
                    subtitle = uiState.errorMessage,
                    tone = PharmaStateTone.Error,
                    actionLabel = "إعادة المحاولة",
                    onAction = onRetry,
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(d.spaceL),
                verticalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("ابحث عن منتج") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(d.radiusL),
                    )
                }

                if (uiState.products.isEmpty()) {
                    item {
                        PharmaStateView(
                            title = "لا توجد منتجات متاحة",
                            subtitle = if (uiState.searchQuery.isBlank()) {
                                "لا توجد منتجات ظاهرة من هذا المستودع حالياً."
                            } else {
                                "لا توجد نتائج مطابقة للبحث."
                            },
                            tone = PharmaStateTone.Neutral,
                        )
                    }
                } else {
                    items(uiState.products, key = { it.id }) { product ->
                        WarehouseProductCard(
                            product = product,
                            onAddToBasket = { onAddToBasket(product) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WarehouseProductCard(
    product: WarehouseProductUiModel,
    onAddToBasket: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXL),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceM),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            verticalAlignment = Alignment.Top,
        ) {
            ProductImage(imageUrl = product.imageUrl)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(d.spaceXS),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = product.subtitle.ifBlank { "لا توجد تفاصيل إضافية" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(d.spaceXS))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProductInfoChip(label = "الكمية: ${product.stockQuantity}")
                    product.priceLabel?.let { price ->
                        ProductInfoChip(label = price)
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(d.radiusL),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = onAddToBasket,
                ) {
                    Text(
                        text = "إضافة للسلة",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceS),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.size(72.dp),
        shape = RoundedCornerShape(d.radiusL),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        if (!imageUrl.isNullOrBlank()) {
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
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(d.iconM),
                )
            }
        }
    }
}

@Composable
private fun ProductInfoChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(d.radiusM),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = d.spaceS, vertical = d.spaceXS),
            horizontalArrangement = Arrangement.spacedBy(d.spaceXS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(d.iconXS),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
