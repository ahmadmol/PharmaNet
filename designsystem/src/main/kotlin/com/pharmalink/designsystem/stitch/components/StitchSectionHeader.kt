package com.pharmalink.designsystem.stitch.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.R
import com.pharmalink.designsystem.stitch.StitchTheme
import com.pharmalink.designsystem.stitch.theme.dimens

@Composable
fun StitchSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    showViewAll: Boolean = false,
    onViewAllClick: (() -> Unit)? = null
) {
    val dimens = MaterialTheme.dimens
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            if (showViewAll && onViewAllClick != null) {
                TextButton(onClick = onViewAllClick) {
                    Text(
                        text = stringResource(R.string.view_all),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(dimens.spaceS))
    }
}

@Preview(showBackground = true)
@Composable
fun StitchSectionHeaderPreview() {
    StitchTheme {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.spaceM)) {
            StitchSectionHeader(title = "آخر النشاطات")
            StitchSectionHeader(title = "المستودعات المميزة", showViewAll = true, onViewAllClick = {})
        }
    }
}
