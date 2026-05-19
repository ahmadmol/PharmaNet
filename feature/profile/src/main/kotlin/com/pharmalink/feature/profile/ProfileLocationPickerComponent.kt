package com.pharmalink.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun ProfileLocationPickerComponent(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onDismiss: () -> Unit,
    onPickLocation: (lat: Double, lng: Double, address: String?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.location_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("إدخال العنوان اليدوي متاح حالياً. تم تعطيل اختيار الموقع من الخريطة في هذه المرحلة.")
                if (initialLatitude != null && initialLongitude != null) {
                    Text(stringResource(R.string.location_picker_lat_lng_format, initialLatitude, initialLongitude))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.location_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.location_picker_cancel))
            }
        },
    )
}
