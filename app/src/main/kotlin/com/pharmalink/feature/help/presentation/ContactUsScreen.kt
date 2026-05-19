package com.pharmalink.feature.help.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmalink.R
import com.pharmalink.designsystem.components.PharmaScreenScaffold
import com.pharmalink.designsystem.components.PharmaTextField
import com.pharmalink.designsystem.theme.dimens

@Composable
fun ContactUsScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ContactUsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var subject by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    val d = MaterialTheme.dimens

    PharmaScreenScaffold(
        title = stringResource(R.string.contact_us_title),
        onBack = onBack,
        navigationContentDescription = stringResource(R.string.common_back),
        modifier = modifier,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            item {
                Text(
                    text = stringResource(R.string.contact_us_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                PharmaTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = stringResource(R.string.contact_us_subject),
                )
            }
            item {
                PharmaTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = stringResource(R.string.contact_us_category_optional),
                )
            }
            item {
                PharmaTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = stringResource(R.string.contact_us_message),
                    singleLine = false,
                )
            }
            item {
                Button(
                    onClick = {
                        viewModel.submit(
                            subject = subject,
                            category = category.ifBlank { null },
                            message = message,
                        )
                    },
                    enabled = !state.isSubmitting && subject.isNotBlank() && message.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.contact_us_send))
                }
            }
            state.submitError?.let { error ->
                item {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (state.submitSuccess) {
                item {
                    Text(
                        text = stringResource(R.string.contact_us_success),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                viewModel.consumeSuccess()
            }
            item {
                Column(modifier = Modifier.padding(top = d.spaceS)) {
                    Text(
                        text = stringResource(R.string.contact_us_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
