package com.pharmalink.feature.cart.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pharmalink.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.designsystem.components.PharmaStateView
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.CartItem
import com.pharmalink.domain.model.DeliveryPreference
import com.pharmalink.domain.model.RequestPriority

/**
 * Cart Screen Component
 * Premium cart/review screen with all cart functionality
 */
@Composable
fun CartScreen(
    onBackClick: () -> Unit,
    onSubmitRequest: () -> Unit,
    onSaveDraft: () -> Unit,
    onBrowseResources: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val d = MaterialTheme.dimens
    val lazyListState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val bottomBarHeight = 80.dp // Approximate bottom navigation height
    
    LaunchedEffect(state.items) {
        // Auto-scroll to top when items change
        if (state.items.isNotEmpty()) {
            lazyListState.animateScrollToItem(0)
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            // Header
            CartHeader(
                onBackClick = onBackClick,
                onMoreClick = { /* TODO: Show cart options */ },
                modifier = Modifier.padding(horizontal = d.spaceL),
            )
            
            Spacer(Modifier.height(d.spaceM))
            
            when (val screenState = state.screenState) {
                is ScreenState.Loading -> PharmaStateView(
                    title = stringResource(R.string.cart_title_review),
                    subtitle = stringResource(R.string.cart_loading_subtitle),
                    tone = PharmaStateTone.Loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                
                is ScreenState.Error -> PharmaStateView(
                    title = stringResource(R.string.cart_error_title),
                    subtitle = screenState.message ?: stringResource(R.string.cart_error_fallback),
                    isError = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                
                is ScreenState.Offline -> PharmaStateView(
                    title = stringResource(R.string.cart_offline_title),
                    subtitle = screenState.message ?: stringResource(R.string.cart_offline_fallback),
                    modifier = Modifier.fillMaxWidth(),
                )
                
                is ScreenState.Empty -> PharmaStateView(
                    title = stringResource(R.string.cart_empty_title),
                    subtitle = stringResource(R.string.cart_empty_subtitle),
                    tone = PharmaStateTone.Neutral,
                    modifier = Modifier.fillMaxWidth(),
                )
                
                is ScreenState.Success -> {
                    // Main Content Area
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Cart Items List
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = d.spaceL,
                                vertical = d.spaceS,
                            ),
                            verticalArrangement = Arrangement.spacedBy(d.spaceM),
                        ) {
                            items(state.items, key = { it.id }) { item ->
                                CartItemCard(
                                    item = item,
                                    onQuantityChange = { newQuantity ->
                                        viewModel.updateItemQuantity(item.id, newQuantity)
                                    },
                                    onRemove = {
                                        viewModel.removeItem(item.id)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            
                            // Add spacing before bottom section
                            item {
                                Spacer(Modifier.height(d.spaceL))
                            }
                        }
                        
                        // Cart Summary
                        CartSummaryCard(
                            totalItems = state.totalItems,
                            selectedWarehouseCount = state.selectedWarehouseCount,
                            estimatedDeliveryTime = state.estimatedDeliveryTime,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = d.spaceL),
                        )
                    }
                }
            }
        }
        
        // Bottom Actions (Fixed at bottom)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        ) {
            if (state.items.isNotEmpty()) {
                CartBottomActions(
                    onSubmitRequest = onSubmitRequest,
                    onSaveDraft = onSaveDraft,
                    onContinueBrowsing = onBrowseResources,
                    isSubmitEnabled = state.items.all { it.stockStatus != com.pharmalink.domain.model.StockStatus.OUT_OF_STOCK },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
