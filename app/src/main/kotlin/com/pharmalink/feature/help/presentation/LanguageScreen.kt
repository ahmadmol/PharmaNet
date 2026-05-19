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
fun LanguageScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    PharmaScreenScaffold(
        title = stringResource(R.string.language_title),
        onBack = onBack,
        navigationContentDescription = stringResource(R.string.common_back),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            Text(
                text = stringResource(R.string.language_arabic_only_message),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.language_arabic_only_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
