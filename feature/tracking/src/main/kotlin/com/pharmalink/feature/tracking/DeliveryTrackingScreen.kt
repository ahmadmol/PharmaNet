package com.pharmalink.feature.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.pharmalink.designsystem.components.PharmaScreenScaffold
import com.pharmalink.designsystem.components.PharmaScreenState
import com.pharmalink.designsystem.components.PharmaStateSpec
import com.pharmalink.designsystem.components.PharmaStateTone
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.feature.tracking.components.DelegateInfoCard
import com.pharmalink.feature.tracking.components.DeliveryStatusHero
import com.pharmalink.feature.tracking.components.DeliveryTimeline
import com.pharmalink.feature.tracking.components.ExtraInfoCard
import com.pharmalink.feature.tracking.components.RouteInfoCard
import com.pharmalink.feature.tracking.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryTrackingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeliveryTrackingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PharmaScreenScaffold(
        title = stringResource(R.string.delivery_tracking_title),
        onBack = onBack,
        navigationContentDescription = stringResource(R.string.delivery_tracking_back),
        modifier = modifier,
    ) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            PharmaScreenState(
                screenState = state.screenState,
                loading = PharmaStateSpec(
                    title = stringResource(R.string.delivery_tracking_title),
                    subtitle = stringResource(R.string.delivery_tracking_loading),
                    tone = PharmaStateTone.Loading,
                ),
                empty = PharmaStateSpec(
                    title = stringResource(R.string.delivery_tracking_title),
                    subtitle = stringResource(R.string.delivery_tracking_empty),
                ),
                error = PharmaStateSpec(
                    title = stringResource(R.string.delivery_tracking_title),
                    subtitle = stringResource(R.string.delivery_tracking_error),
                    tone = PharmaStateTone.Error,
                ),
                offline = PharmaStateSpec(
                    title = stringResource(R.string.delivery_tracking_title),
                    subtitle = stringResource(R.string.delivery_tracking_offline),
                    tone = PharmaStateTone.Offline,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.dimens.spaceL),
            ) { tracking ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM),
                ) {
                    item {
                        DeliveryStatusHero(
                            status = tracking.currentStatus,
                            statusText = viewModel.getStatusText(tracking.currentStatus),
                            estimatedArrival = tracking.estimatedArrival,
                            lastUpdate = tracking.lastUpdate,
                            modifier = Modifier.padding(bottom = MaterialTheme.dimens.spaceS),
                        )
                    }

                    item {
                        DelegateInfoCard(
                            delegate = tracking.delegate,
                            onCallDelegate = { viewModel.callDelegate() },
                        )
                    }

                    item {
                        RouteInfoCard(
                            startPoint = tracking.startPoint,
                            destinationPoint = tracking.destinationPoint,
                        )
                    }

                    item {
                        DeliveryTimeline(
                            currentStatus = tracking.currentStatus,
                        )
                    }

                    item {
                        ExtraInfoCard(
                            orderNumber = tracking.orderNumber,
                            warehouseName = tracking.startPoint,
                            departureTime = tracking.departureTime,
                            deliveryNotes = tracking.deliveryNotes,
                        )
                    }

                    item {
                        Spacer(Modifier.height(MaterialTheme.dimens.spaceXL))
                    }
                }
            }
        }
    }
}
