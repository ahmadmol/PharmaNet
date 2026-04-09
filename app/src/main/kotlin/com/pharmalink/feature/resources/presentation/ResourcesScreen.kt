package com.pharmalink.feature.resources.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pharmalink.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.components.PharmaStateView
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.Warehouse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(
    onWarehouseClick: (warehouseId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResourcesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val d = MaterialTheme.dimens
    val scope = rememberCoroutineScope()
    var pullRefreshing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClinicalCanvas),
    ) {
        ResourcesHeader(
            onFilterClick = { },
            onSortClick = { },
            modifier = Modifier.padding(horizontal = d.spaceL, vertical = d.spaceM),
        )

        Spacer(Modifier.height(d.spaceS))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d.spaceL),
            shape = RoundedCornerShape(d.radiusL),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(d.spaceM)) {
                Text(
                    text = stringResource(R.string.resources_search_start_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(d.spaceS))
                ResourcesSearchBar(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    onSearch = { },
                    onClear = { viewModel.onQueryChange("") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(d.spaceM))

        ResourcesFilterRow(
            selectedFilter = state.selectedFilter.toResourceFilter(),
            onFilterSelected = { filter ->
                viewModel.onFilterSelected(filter.toWarehouseFilter())
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(d.spaceM))

        PullToRefreshBox(
            isRefreshing = pullRefreshing,
            onRefresh = {
                scope.launch {
                    pullRefreshing = true
                    viewModel.refresh()
                    delay(600)
                    pullRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val screenState = state.screenState) {
                ScreenState.Loading -> {
                    PharmaStateView(
                        title = stringResource(R.string.resources_loading_title),
                        subtitle = stringResource(R.string.resources_loading_subtitle),
                        tone = PharmaStateTone.Loading,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                is ScreenState.Error -> {
                    PharmaStateView(
                        title = stringResource(R.string.resources_error_title),
                        subtitle = screenState.message ?: stringResource(R.string.resources_error_fallback),
                        tone = PharmaStateTone.Error,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                is ScreenState.Offline -> {
                    PharmaStateView(
                        title = stringResource(R.string.resources_offline_title),
                        subtitle = screenState.message ?: stringResource(R.string.resources_offline_fallback),
                        tone = PharmaStateTone.Offline,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                ScreenState.Empty -> {
                    ResourcesEmptyState(
                        onResetFilters = {
                            viewModel.onQueryChange("")
                            viewModel.onFilterSelected(WarehouseFilter.ALL)
                        },
                        onRetrySearch = { viewModel.refresh() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = d.spaceL),
                    )
                }
                is ScreenState.Success -> {
                    val warehouses = screenState.data
                    if (warehouses.isEmpty()) {
                        ResourcesEmptyState(
                            onResetFilters = {
                                viewModel.onQueryChange("")
                                viewModel.onFilterSelected(WarehouseFilter.ALL)
                            },
                            onRetrySearch = { viewModel.refresh() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = d.spaceL),
                        )
                    } else {
                        WarehouseListContent(
                            warehouses = warehouses,
                            onWarehouseClick = onWarehouseClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WarehouseListContent(
    warehouses: List<Warehouse>,
    onWarehouseClick: (warehouseId: String) -> Unit,
) {
    val d = MaterialTheme.dimens
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = d.spaceL, vertical = d.spaceS),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        item {
            Text(
                text = stringResource(R.string.resources_recommended_heading),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = d.spaceXS),
            )
        }
        items(warehouses.take(2), key = { it.id }) { warehouse ->
            EnhancedWarehouseCard(
                warehouse = warehouse,
                onClick = { onWarehouseClick(warehouse.id) },
                isRecommended = true,
            )
        }
        item {
            Text(
                text = stringResource(R.string.resources_all_warehouses_heading),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = d.spaceM, bottom = d.spaceXS),
            )
        }
        items(warehouses.drop(2), key = { it.id }) { warehouse ->
            EnhancedWarehouseCard(
                warehouse = warehouse,
                onClick = { onWarehouseClick(warehouse.id) },
                isRecommended = false,
            )
        }
    }
}
