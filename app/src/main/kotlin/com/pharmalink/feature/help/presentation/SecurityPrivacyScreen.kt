package com.pharmalink.feature.help.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pharmalink.R
import com.pharmalink.designsystem.components.PharmaScreenScaffold
import com.pharmalink.designsystem.theme.dimens

@Composable
fun SecurityPrivacyScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    PharmaScreenScaffold(
        title = stringResource(R.string.security_privacy_title),
        onBack = onBack,
        navigationContentDescription = stringResource(R.string.common_back),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Text(stringResource(R.string.security_privacy_intro), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.security_privacy_item_data), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.security_privacy_item_roles), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.security_privacy_item_location), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.security_privacy_item_notifications), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.security_privacy_item_photos), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.security_privacy_item_profile), style = MaterialTheme.typography.bodySmall)
        }
    }
}
