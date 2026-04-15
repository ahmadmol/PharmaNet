package com.pharmalink.designsystem.stitch.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.stitch.StitchTheme
import com.pharmalink.designsystem.stitch.theme.dimens
import androidx.compose.foundation.layout.Spacer

enum class UrgencyLevel {
    NORMAL, URGENT
}

@Composable
fun StitchUrgencySelector(
    selectedUrgency: UrgencyLevel,
    onUrgencySelected: (UrgencyLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    val d = MaterialTheme.dimens

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = d.spaceS),
        horizontalArrangement = Arrangement.spacedBy(d.spaceS)
    ) {
        // Normal Button
        val normalButtonColors = if (selectedUrgency == UrgencyLevel.NORMAL) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        }

        val normalButtonBorder = if (selectedUrgency == UrgencyLevel.NORMAL) null else
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

        if (selectedUrgency == UrgencyLevel.NORMAL) {
            Button(
                onClick = { onUrgencySelected(UrgencyLevel.NORMAL) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(9999.dp), // Pill-shaped
                colors = normalButtonColors,
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text("عادي")
            }
        } else {
            OutlinedButton(
                onClick = { onUrgencySelected(UrgencyLevel.NORMAL) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(9999.dp), // Pill-shaped
                colors = normalButtonColors,
                border = normalButtonBorder
            ) {
                Text("عادي")
            }
        }

        // Urgent Button
        val urgentButtonColors = if (selectedUrgency == UrgencyLevel.URGENT) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary, // Red for urgent
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        } else {
            ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        }
        val urgentButtonBorder = if (selectedUrgency == UrgencyLevel.URGENT) null else
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

        if (selectedUrgency == UrgencyLevel.URGENT) {
            Button(
                onClick = { onUrgencySelected(UrgencyLevel.URGENT) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(9999.dp), // Pill-shaped
                colors = urgentButtonColors,
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text("عاجل")
            }
        } else {
            OutlinedButton(
                onClick = { onUrgencySelected(UrgencyLevel.URGENT) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(9999.dp), // Pill-shaped
                colors = urgentButtonColors,
                border = urgentButtonBorder
            ) {
                Text("عاجل")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StitchUrgencySelectorPreview() {
    StitchTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            var selected by remember { mutableStateOf(UrgencyLevel.NORMAL) }
            StitchUrgencySelector(selectedUrgency = selected, onUrgencySelected = { selected = it })
            Spacer(modifier = Modifier.height(16.dp))
            var selected2 by remember { mutableStateOf(UrgencyLevel.URGENT) }
            StitchUrgencySelector(selectedUrgency = selected2, onUrgencySelected = { selected2 = it })
        }
    }
}
