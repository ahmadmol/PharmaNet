package com.pharmalink.feature.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaButtonStyle
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.FulfillmentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerOrderSuccessScreen(
    medicineName: String,
    pharmacyName: String,
    fulfillmentType: FulfillmentType,
    showPrimaryAction: Boolean,
    onGoToMyOrdersClick: () -> Unit,
    onBackToSearchClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val fulfillmentLabel = if (fulfillmentType == FulfillmentType.DELIVERY) {
        stringResource(R.string.customer_order_delivery_option)
    } else {
        stringResource(R.string.customer_order_pickup_option)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier,
            containerColor = ClinicalCanvas,
            topBar = {
                TopAppBar(
                    title = {},
                    actions = {
                        IconButton(onClick = onCloseClick) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.customer_order_close),
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(d.spaceL),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(d.spaceL),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.customer_order_success_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.customer_order_success_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.customer_order_confirmation_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = d.cardElevation,
                ) {
                    Column(
                        modifier = Modifier.padding(d.spaceL),
                        verticalArrangement = Arrangement.spacedBy(d.spaceM),
                    ) {
                        SuccessSummaryRow(
                            label = stringResource(R.string.customer_order_success_medicine_label),
                            value = medicineName,
                        )
                        SuccessSummaryRow(
                            label = stringResource(R.string.customer_order_success_pharmacy_label),
                            value = pharmacyName,
                        )
                        SuccessSummaryRow(
                            label = stringResource(R.string.customer_order_success_fulfillment_label),
                            value = fulfillmentLabel,
                        )
                    }
                }
                if (showPrimaryAction) {
                    PharmaButton(
                        text = stringResource(R.string.customer_order_success_primary_action),
                        onClick = onGoToMyOrdersClick,
                    )
                }
                PharmaButton(
                    text = stringResource(R.string.customer_order_success_secondary_action),
                    onClick = onBackToSearchClick,
                    style = PharmaButtonStyle.Outlined,
                )
            }
        }
    }
}

@Composable
private fun SuccessSummaryRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceXS),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
