package com.pharmalink.feature.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pharmalink.feature.auth.R

@Composable
fun LocationPickerComponent(
    selectedAddress: String,
    initialLatitude: Double?,
    initialLongitude: Double?,
    isResolvingLocation: Boolean,
    locationMessage: String?,
    locationMessageIsError: Boolean,
    onDismiss: () -> Unit,
    onUseCurrentLocation: () -> Unit,
) {
    val hasCoordinates = initialLatitude != null && initialLongitude != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.location_picker_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.location_picker_current_location_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (isResolvingLocation) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.location_picker_resolving_address),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                if (selectedAddress.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.location_picker_detected_address),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = selectedAddress,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else if (!isResolvingLocation) {
                    Text(
                        text = stringResource(R.string.location_picker_no_location_selected),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (hasCoordinates) {
                    Text(stringResource(R.string.location_picker_lat_lng_format, initialLatitude, initialLongitude))
                }

                locationMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (locationMessageIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onUseCurrentLocation,
                enabled = !isResolvingLocation,
            ) {
                Text(stringResource(R.string.location_picker_use_current_location))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.location_picker_cancel))
            }
        },
    )
}
