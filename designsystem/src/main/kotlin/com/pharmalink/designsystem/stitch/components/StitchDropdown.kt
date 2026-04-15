package com.pharmalink.designsystem.stitch.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.stitch.StitchTheme
import com.pharmalink.designsystem.stitch.theme.dimens

@Composable
fun <T> StitchDropdown(
    selectedItem: T?,
    items: List<T>,
    onItemSelected: (T) -> Unit,
    itemLabel: (T) -> String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val d = MaterialTheme.dimens

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) // Ensures the Box takes minimum height needed
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(d.radiusM)
            )
            .clickable { expanded = true }
            .padding(d.spaceM)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedItem?.let(itemLabel) ?: placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = selectedItem?.let { MaterialTheme.colorScheme.onSurface } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.95f) // Adjust width to be similar to parent
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemLabel(item)) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StitchDropdownPreview() {
    StitchTheme {
        val items = listOf("خيار 1", "خيار 2", "خيار 3")
        var selectedItem by remember { mutableStateOf<String?>(null) }

        Column(modifier = Modifier.padding(MaterialTheme.dimens.spaceM)) {
            StitchDropdown(
                selectedItem = selectedItem,
                items = items,
                onItemSelected = { selectedItem = it },
                itemLabel = { it },
                placeholder = "اختر خياراً"
            )
        }
    }
}
