package com.pharmalink.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.core.common.ui.ScreenState
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.components.PharmaStateView
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.feature.home.components.StitchActivityFeed
import com.pharmalink.feature.home.components.StitchHomeHeader
import com.pharmalink.feature.home.components.StitchHomeSearchBar
import com.pharmalink.feature.home.components.StitchKpiGrid
import com.pharmalink.feature.home.components.StitchQuickActionPills
import com.pharmalink.feature.home.components.StitchUrgentShortageBanner
import com.pharmalink.feature.home.components.StitchWarehousesPromoCard
import com.pharmalink.feature.home.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCreateRequest: () -> Unit,
    onSearchMedicine: () -> Unit,
    onOpenWarehouses: () -> Unit,
    onEmergencyRequest: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenRequest: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val d = MaterialTheme.dimens
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ClinicalCanvas),
    ) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = d.spaceL),
            ) {
                StitchHomeHeader(
                    notificationBadgeCount = state.notificationBadgeCount,
                    onOpenNotifications = onOpenNotifications,
                    modifier = Modifier.padding(top = d.spaceM),
                )
                StitchHomeSearchBar(
                    onSearchClick = onSearchMedicine,
                    modifier = Modifier.padding(top = d.spaceM),
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        top = d.spaceL,
                        bottom = d.space6XL,
                    ),
                    verticalArrangement = Arrangement.spacedBy(d.spaceL),
                ) {
                    when (val screenState = state.screenState) {
                        ScreenState.Loading -> {
                            item(key = "loading") {
                                PharmaStateView(
                                    title = stringResource(R.string.home_dashboard_title),
                                    subtitle = stringResource(R.string.home_loading_subtitle),
                                    tone = PharmaStateTone.Loading,
                                )
                            }
                        }
                        is ScreenState.Error -> {
                            item(key = "error") {
                                PharmaStateView(
                                    title = stringResource(R.string.home_empty_title),
                                    subtitle = screenState.message
                                        ?: stringResource(R.string.home_empty_subtitle),
                                    tone = PharmaStateTone.Error,
                                    actionLabel = stringResource(R.string.home_retry),
                                    onAction = viewModel::refresh,
                                )
                            }
                        }
                        is ScreenState.Offline -> {
                            item(key = "offline") {
                                PharmaStateView(
                                    title = stringResource(R.string.home_empty_title),
                                    subtitle = screenState.message
                                        ?: stringResource(R.string.home_empty_subtitle),
                                    tone = PharmaStateTone.Offline,
                                    actionLabel = stringResource(R.string.home_retry),
                                    onAction = viewModel::refresh,
                                )
                            }
                        }
                        ScreenState.Empty -> {
                            item(key = "empty") {
                                PharmaStateView(
                                    title = stringResource(R.string.home_empty_title),
                                    subtitle = stringResource(R.string.home_empty_subtitle),
                                    actionLabel = stringResource(R.string.home_quick_new),
                                    onAction = onCreateRequest,
                                )
                            }
                        }
                        is ScreenState.Success -> {
                            val content = screenState.data
                            if (content.urgentRequests > 0) {
                                item(key = "urgent") {
                                    StitchUrgentShortageBanner(
                                        urgentCount = content.urgentRequests,
                                        onEmergencyRequest = onEmergencyRequest,
                                    )
                                }
                            }
                            item(key = "kpi") {
                                StitchKpiGrid(
                                    activeOrders = content.activeOrders,
                                    urgentRequests = content.urgentRequests,
                                    completedToday = content.completedToday,
                                    lowStockAlerts = content.lowStockAlerts,
                                )
                            }
                            item(key = "quick") {
                                StitchQuickActionPills(
                                    onCreateRequest = onCreateRequest,
                                    onOpenWarehouses = onOpenWarehouses,
                                    onOpenOrders = onOpenOrders,
                                    onEmergencyRequest = onEmergencyRequest,
                                )
                            }
                            item(key = "activity") {
                                StitchActivityFeed(
                                    recentNotifications = content.recentNotifications,
                                    recentRequests = content.recentRequests,
                                    onOpenRequest = onOpenRequest,
                                    onOpenOrders = onOpenOrders,
                                    onOpenNotifications = onOpenNotifications,
                                )
                            }
                            item(key = "warehouses") {
                                StitchWarehousesPromoCard(onOpenWarehouses = onOpenWarehouses)
                            }
                        }
                    }
                }
            }
        }
    }
}
